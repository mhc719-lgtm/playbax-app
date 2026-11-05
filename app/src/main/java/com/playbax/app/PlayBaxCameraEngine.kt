package com.playbax.app

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import androidx.camera.core.Camera
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Decoupled CameraX engine with PreviewView hardware anchor, background ImageAnalysis +
 * PreviewView fallback frame capture, background 24fps multi-screen decoder, and MP4 Recorder.
 */
class PlayBaxCameraEngine(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    private val ringBuffer: PlayBaxFrameRingBuffer,
    private val onFramesDecoded: (List<DelayedFrameResult>, Int) -> Unit,
    private val onStatusToast: (String) -> Unit,
    private val onRecordingStateChanged: (Boolean) -> Unit
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private val playbackExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    private var cameraProvider: ProcessCameraProvider? = null
    private var activeCamera: Camera? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private var boundLensMode: Int = -1

    @Volatile var isStageRunning: Boolean = false
    @Volatile var isFrozen: Boolean = false

    private val slotPacketIds = LongArray(3) { -1L }
    private val slotBitmaps = arrayOfNulls<android.graphics.Bitmap>(3)

    private val previewFallbackRunnable = object : Runnable {
        override fun run() {
            if (isStageRunning && !isFrozen) {
                val nowMs = SystemClock.elapsedRealtime()
                if (nowMs - ringBuffer.getLastPushedTimestampMs() > 90L) {
                    val bmp = try { previewView.bitmap } catch (_: Exception) { null }
                    if (bmp != null) {
                        val delaySec = PlayBaxPreferences.getDelaySeconds(context)
                        val count = PlayBaxPreferences.getScreenCount(context)
                        val maxMs = count * delaySec * 1000L
                        analysisExecutor.execute {
                            val jpeg = YuvFrameConverter.bitmapToJpegBytes(bmp, 0, false)
                            if (!bmp.isRecycled) bmp.recycle()
                            if (jpeg != null) {
                                ringBuffer.pushFrame(jpeg, SystemClock.elapsedRealtime(), maxMs)
                            }
                        }
                    }
                }
            }
            if (isStageRunning) {
                mainHandler.postDelayed(this, 60L)
            }
        }
    }

    init {
        playbackExecutor.scheduleAtFixedRate({ tickBackgroundPlayback() }, 45L, 42L, TimeUnit.MILLISECONDS)
    }

    fun startCameraSession(forceRebind: Boolean = false) {
        isStageRunning = true
        mainHandler.removeCallbacks(previewFallbackRunnable)
        mainHandler.postDelayed(previewFallbackRunnable, 120L)

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) return

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

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                    try {
                        val nowMs = SystemClock.elapsedRealtime()
                        if (isStageRunning && !isFrozen && ringBuffer.shouldAcceptFrame(nowMs)) {
                            val jpeg = YuvFrameConverter.imageProxyToUprightJpeg(imageProxy, isFrontMirror)
                            if (jpeg != null) {
                                val d = PlayBaxPreferences.getDelaySeconds(context)
                                val c = PlayBaxPreferences.getScreenCount(context)
                                ringBuffer.pushFrame(jpeg, nowMs, c * d * 1000L)
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
                    provider.bindToLifecycle(lifecycleOwner, selector, preview, imageAnalysis, videoCap)
                } catch (_: Exception) {
                    try {
                        provider.bindToLifecycle(lifecycleOwner, selector, preview, videoCap)
                    } catch (_: Exception) {
                        videoCapture = null
                        provider.bindToLifecycle(lifecycleOwner, selector, preview, imageAnalysis)
                    }
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

    private fun tickBackgroundPlayback() {
        if (!isStageRunning || isFrozen) return
        try {
            val delaySec = PlayBaxPreferences.getDelaySeconds(context)
            val screenCount = PlayBaxPreferences.getScreenCount(context)
            val nowMs = SystemClock.elapsedRealtime()

            val results = ArrayList<DelayedFrameResult>(screenCount)
            for (i in 0 until screenCount) {
                val slotDelayMs = (i + 1) * delaySec * 1000L
                val res = ringBuffer.decodeFrameForDelay(
                    nowMs = nowMs,
                    delayMs = slotDelayMs,
                    lastPacketId = slotPacketIds[i],
                    currentBitmap = slotBitmaps[i]
                )
                slotPacketIds[i] = res.packetId
                slotBitmaps[i] = res.bitmap
                results.add(res)
            }
            mainHandler.post {
                onFramesDecoded(results, delaySec)
            }
        } catch (_: Exception) {
        }
    }

    fun startRecordingIfNeeded() {
        if (activeRecording != null) return
        val capture = videoCapture ?: return
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "PlayBax_$timestamp")
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
                    is VideoRecordEvent.Start -> onRecordingStateChanged(true)
                    is VideoRecordEvent.Finalize -> {
                        activeRecording = null
                        onRecordingStateChanged(false)
                        if (!event.hasError()) onStatusToast("Saved MP4 to Movies/PlayBax")
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
        try { rec.stop() } catch (_: Exception) {}
        if (silent) onRecordingStateChanged(false)
    }

    fun stopStageSession() {
        isStageRunning = false
        mainHandler.removeCallbacks(previewFallbackRunnable)
        stopRecordingIfActive(silent = true)
    }

    fun shutdown() {
        stopStageSession()
        try { cameraProvider?.unbindAll() } catch (_: Exception) {}
        playbackExecutor.shutdownNow()
        analysisExecutor.shutdownNow()
    }
}
