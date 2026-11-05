package com.playbax.app

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.provider.MediaStore
import androidx.camera.core.Camera
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Decoupled CameraX engine managing single-consumer ImageAnalysis (in-memory delayed ring buffer)
 * and optional MediaStore MP4 VideoCapture recording.
 */
class PlayBaxCameraEngine(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val ringBuffer: PlayBaxFrameRingBuffer,
    private val onStatusToast: (String) -> Unit,
    private val onRecordingStateChanged: (Boolean) -> Unit
) {

    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private var activeCamera: Camera? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private var boundLensMode: Int = -1

    @Volatile
    var maxRetentionMs: Long = 15_000L

    fun bindCameraIfNeeded(forceRebind: Boolean = false) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val desiredLens = PlayBaxPreferences.getLensMode(context)
        if (!forceRebind && activeCamera != null && boundLensMode == desiredLens) {
            CameraLensHelper.applyZoomForPreference(activeCamera, desiredLens)
            return
        }

        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val provider = future.get()
                cameraProvider = provider
                stopRecordingIfActive(silent = true)
                provider.unbindAll()

                val selector = CameraLensHelper.buildCameraSelector(context, provider, desiredLens)
                val isFrontMirror = (desiredLens == PlayBaxPreferences.LENS_FRONT_MIRROR)

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()

                imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                    try {
                        val nowMs = SystemClock.elapsedRealtime()
                        if (ringBuffer.shouldAcceptFrame(nowMs)) {
                            val jpeg = YuvFrameConverter.imageProxyToUprightJpeg(
                                image = imageProxy,
                                mirrorHorizontally = isFrontMirror
                            )
                            if (jpeg != null) {
                                ringBuffer.pushFrame(jpeg, nowMs, maxRetentionMs)
                            }
                        }
                    } finally {
                        imageProxy.close()
                    }
                }

                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HD))
                    .build()
                val videoCap = VideoCapture.withOutput(recorder)
                videoCapture = videoCap

                activeCamera = try {
                    provider.bindToLifecycle(lifecycleOwner, selector, imageAnalysis, videoCap)
                } catch (_: Exception) {
                    // Fallback if device only supports single analysis stream with current lens
                    videoCapture = null
                    provider.bindToLifecycle(lifecycleOwner, selector, imageAnalysis)
                }

                boundLensMode = desiredLens
                CameraLensHelper.applyZoomForPreference(activeCamera, desiredLens)
                activeCamera?.cameraInfo?.zoomState?.observe(lifecycleOwner) {
                    CameraLensHelper.applyZoomForPreference(activeCamera, desiredLens)
                }
            } catch (_: Exception) {
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun startRecordingIfNeeded() {
        if (!PlayBaxPreferences.isRecordEnabled(context)) {
            stopRecordingIfActive(silent = true)
            return
        }
        if (activeRecording != null) return
        val capture = videoCapture ?: return

        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "PlayBax_$timestamp"
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/PlayBax")
                }
            }

            val outputOptions = MediaStoreOutputOptions.Builder(
                context.contentResolver,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            ).setContentValues(contentValues).build()

            var pending = capture.output.prepareRecording(context, outputOptions)
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
            ) {
                pending = pending.withAudioEnabled()
            }

            activeRecording = pending.start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        onRecordingStateChanged(true)
                    }
                    is VideoRecordEvent.Finalize -> {
                        activeRecording = null
                        onRecordingStateChanged(false)
                        if (!event.hasError()) {
                            onStatusToast("Saved MP4 to Gallery (Movies/PlayBax)")
                        }
                    }
                }
            }
        } catch (_: Exception) {
            activeRecording = null
            onRecordingStateChanged(false)
        }
    }

    fun stopRecordingIfActive(silent: Boolean = false) {
        val rec = activeRecording ?: return
        activeRecording = null
        try {
            rec.stop()
        } catch (_: Exception) {
        }
        if (silent) {
            onRecordingStateChanged(false)
        }
    }

    fun isCurrentlyRecording(): Boolean = activeRecording != null

    fun shutdown() {
        stopRecordingIfActive(silent = true)
        try {
            cameraProvider?.unbindAll()
        } catch (_: Exception) {
        }
        analysisExecutor.shutdownNow()
    }
}
