package com.playbax.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Size
import android.view.OrientationEventListener
import android.view.Surface
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Decoupled HD CameraX engine (1280x960 ImageAnalysis + PreviewView), live OrientationEventListener
 * rotation sync, and composite multi-screen MP4 recording via PlayBaxStageRecorder.
 */
class PlayBaxCameraEngine(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    private val ringBuffer: PlayBaxFrameRingBuffer,
    private val onFramesDecoded: (List<DelayedFrameResult>, Int) -> Unit,
    private val onOrientationChangedCallback: () -> Unit,
    private val onStatusToast: (String) -> Unit,
    private val onRecordingStateChanged: (Boolean) -> Unit
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private val playbackExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private val isUiUpdatePending = AtomicBoolean(false)

    private val stageRecorder = PlayBaxStageRecorder(
        context = context,
        onStatusToast = onStatusToast,
        onRecordingStateChanged = onRecordingStateChanged
    )

    private var cameraProvider: ProcessCameraProvider? = null
    private var activeCamera: Camera? = null
    private var previewUseCase: Preview? = null
    private var imageAnalysisUseCase: ImageAnalysis? = null

    @Volatile var isStageRunning: Boolean = false
    @Volatile var isFrozen: Boolean = false
    @Volatile private var currentTargetRotation: Int = Surface.ROTATION_0

    private val slotPacketIds = LongArray(3) { -1L }
    private val slotBitmaps = arrayOfNulls<Bitmap>(3)

    private val orientationListener = object : OrientationEventListener(context) {
        override fun onOrientationChanged(orientation: Int) {
            if (orientation == ORIENTATION_UNKNOWN || !isStageRunning) return
            val sensorRot = when (orientation) {
                in 55..125 -> Surface.ROTATION_270
                in 145..215 -> Surface.ROTATION_180
                in 235..305 -> Surface.ROTATION_90
                in 325..360, in 0..35 -> Surface.ROTATION_0
                else -> currentTargetRotation
            }
            applyRotationIfChanged(sensorRot)
        }
    }

    init {
        playbackExecutor.scheduleAtFixedRate({ tickBackgroundPlayback() }, 50L, 42L, TimeUnit.MILLISECONDS)
    }

    fun onDisplayRotationChanged() {
        val displayRot = previewView.display?.rotation ?: return
        applyRotationIfChanged(displayRot)
    }

    private fun applyRotationIfChanged(newRot: Int) {
        if (newRot == currentTargetRotation) return
        val wasLandscape = (currentTargetRotation == Surface.ROTATION_90 || currentTargetRotation == Surface.ROTATION_270)
        val nowLandscape = (newRot == Surface.ROTATION_90 || newRot == Surface.ROTATION_270)
        currentTargetRotation = newRot
        try {
            previewUseCase?.targetRotation = newRot
            imageAnalysisUseCase?.targetRotation = newRot
        } catch (_: Exception) {}

        if (wasLandscape != nowLandscape) {
            ringBuffer.clear()
            for (i in 0..2) {
                slotPacketIds[i] = -1L
                slotBitmaps[i] = null
            }
            mainHandler.post { onOrientationChangedCallback() }
        }
    }

    fun startCameraSession() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) return

        isStageRunning = true
        try {
            if (orientationListener.canDetectOrientation()) orientationListener.enable()
        } catch (_: Exception) {}

        val desiredLens = PlayBaxPreferences.getLensMode(context)
        val isFrontMirror = (desiredLens == PlayBaxPreferences.LENS_FRONT_MIRROR)
        val initialRot = previewView.display?.rotation ?: Surface.ROTATION_0
        currentTargetRotation = initialRot

        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (!isStageRunning) return@addListener
            try {
                val provider = future.get()
                cameraProvider = provider
                provider.unbindAll()

                val selector = CameraLensHelper.buildCameraSelector(provider, desiredLens)
                val preview = Preview.Builder()
                    .setTargetRotation(currentTargetRotation)
                    .build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                previewUseCase = preview

                val resSelector = ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(1280, 960),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                    )
                    .build()

                val imageAnalysis = ImageAnalysis.Builder()
                    .setResolutionSelector(resSelector)
                    .setTargetRotation(currentTargetRotation)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                imageAnalysisUseCase = imageAnalysis

                imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                    try {
                        val nowMs = SystemClock.elapsedRealtime()
                        if (isStageRunning && !isFrozen && ringBuffer.shouldAcceptFrame(nowMs)) {
                            val c = PlayBaxPreferences.getScreenCount(context)
                            val maxDim = if (c == 1) 960 else 768
                            val jpeg = YuvFrameConverter.imageProxyToUprightJpeg(
                                image = imageProxy,
                                mirrorHorizontally = isFrontMirror,
                                maxDimensionPx = maxDim
                            )
                            if (jpeg != null && jpeg.isNotEmpty()) {
                                val d = PlayBaxPreferences.getDelaySeconds(context)
                                ringBuffer.pushFrame(jpeg, nowMs, c * d * 1000L)
                            }
                        }
                    } catch (_: Exception) {
                    } finally {
                        imageProxy.close()
                    }
                }

                activeCamera = try {
                    provider.bindToLifecycle(lifecycleOwner, selector, preview, imageAnalysis)
                } catch (_: Exception) {
                    val fallback = if (isFrontMirror) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                    provider.bindToLifecycle(lifecycleOwner, fallback, preview, imageAnalysis)
                }

                CameraLensHelper.applyZoomForPreference(activeCamera, desiredLens)
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

            if (stageRecorder.isRecording) {
                val layoutMode = PlayBaxPreferences.getLayoutMode(context)
                val isLand = isCurrentlyLandscape()
                val sideBySide = when (layoutMode) {
                    PlayBaxPreferences.LAYOUT_PORTRAIT_STACKED -> false
                    PlayBaxPreferences.LAYOUT_LANDSCAPE_SIDE -> true
                    else -> isLand
                }
                stageRecorder.recordStageFrame(slotBitmaps, screenCount, sideBySide)
            }

            if (isUiUpdatePending.compareAndSet(false, true)) {
                mainHandler.post {
                    try {
                        if (isStageRunning) {
                            onFramesDecoded(results, delaySec)
                        }
                    } finally {
                        isUiUpdatePending.set(false)
                    }
                }
            }
        } catch (_: Exception) {
            isUiUpdatePending.set(false)
        }
    }

    fun isCurrentlyLandscape(): Boolean {
        val rot = currentTargetRotation
        if (rot == Surface.ROTATION_90 || rot == Surface.ROTATION_270) return true
        return context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    fun startRecordingIfNeeded() {
        if (stageRecorder.isRecording || !isStageRunning) return
        val isLand = isCurrentlyLandscape()
        playbackExecutor.execute {
            stageRecorder.startRecording(isLandscape = isLand)
        }
    }

    fun stopRecordingIfActive(silent: Boolean = false) {
        if (!stageRecorder.isRecording) return
        playbackExecutor.execute {
            stageRecorder.stopRecording(silent = silent)
        }
    }

    fun stopStageSession(saveActiveRecording: Boolean = true) {
        isStageRunning = false
        isUiUpdatePending.set(false)
        try { orientationListener.disable() } catch (_: Exception) {}
        stopRecordingIfActive(silent = !saveActiveRecording)
        try { cameraProvider?.unbindAll() } catch (_: Exception) {}
    }

    fun shutdown() {
        stopStageSession(saveActiveRecording = false)
        playbackExecutor.shutdownNow()
        analysisExecutor.shutdownNow()
    }
}
