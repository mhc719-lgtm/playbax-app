package com.playbax.app

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.util.Range
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider

object CameraLensHelper {

    @OptIn(ExperimentalCamera2Interop::class)
    fun buildCameraSelector(
        cameraProvider: ProcessCameraProvider,
        lensMode: Int
    ): CameraSelector {
        if (lensMode == PlayBaxPreferences.LENS_FRONT_MIRROR) {
            return if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
        }
        if (lensMode == PlayBaxPreferences.LENS_REAR_WIDE) {
            return CameraSelector.DEFAULT_BACK_CAMERA
        }

        return CameraSelector.Builder()
            .addCameraFilter { cameraInfos ->
                val backCameras = cameraInfos.filter { info ->
                    try {
                        val facing = Camera2CameraInfo.from(info)
                            .getCameraCharacteristic(CameraCharacteristics.LENS_FACING)
                        facing == CameraCharacteristics.LENS_FACING_BACK
                    } catch (_: Exception) {
                        false
                    }
                }
                if (backCameras.isEmpty()) {
                    cameraInfos
                } else {
                    val zoomCapableBack = backCameras.firstOrNull { info ->
                        (info.zoomState.value?.minZoomRatio ?: 1.0f) < 0.95f
                    }
                    if (zoomCapableBack != null) {
                        listOf(zoomCapableBack)
                    } else {
                        val widest = backCameras.minByOrNull { info ->
                            try {
                                Camera2CameraInfo.from(info)
                                    .getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                                    ?.minOrNull() ?: Float.MAX_VALUE
                            } catch (_: Exception) {
                                Float.MAX_VALUE
                            }
                        }
                        if (widest != null) listOf(widest) else backCameras
                    }
                }
            }
            .build()
    }

    /**
     * Applies single-shot lens zoom and unlocks native Camera2 hardware video optimizations:
     * Continuous Video AF, Optical + Electronic Anti-Shake (OIS + EIS), 30fps AE target range,
     * 50/60Hz gym lighting anti-banding, and hardware ISP fast denoise + edge sharpening.
     */
    @OptIn(ExperimentalCamera2Interop::class)
    fun applyHardwareVideoOptimizations(camera: Camera?, lensMode: Int) {
        if (camera == null) return
        try {
            val minZoom = camera.cameraInfo.zoomState.value?.minZoomRatio ?: 1.0f
            val targetZoom = if (lensMode == PlayBaxPreferences.LENS_REAR_ULTRA && minZoom < 1.0f) {
                minZoom
            } else {
                1.0f
            }
            camera.cameraControl.setZoomRatio(targetZoom)
        } catch (_: Exception) {
        }

        try {
            val cam2Info = Camera2CameraInfo.from(camera.cameraInfo)
            val cam2Control = Camera2CameraControl.from(camera.cameraControl)
            val opts = CaptureRequestOptions.Builder()

            // 1. Smooth Continuous Video Autofocus (prevents focus-hunting pulses on moving athletes)
            val afModes = cam2Info.getCameraCharacteristic(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)
            if (afModes != null && afModes.contains(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)) {
                opts.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO
                )
            }

            // 2. Hardware Optical Image Stabilization (OIS Anti-Shake)
            val oisModes = cam2Info.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
            if (oisModes != null && oisModes.contains(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON)) {
                opts.setCaptureRequestOption(
                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON
                )
            }

            // 3. Hardware Electronic Video Stabilization (EIS / Preview Anti-Shake)
            val eisModes = cam2Info.getCameraCharacteristic(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)
            if (eisModes != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    eisModes.contains(CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION)
                ) {
                    opts.setCaptureRequestOption(
                        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                        CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION
                    )
                } else if (eisModes.contains(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON)) {
                    opts.setCaptureRequestOption(
                        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON
                    )
                }
            }

            // 4. Lock crisp 30fps AE target range (prevents 15fps indoor motion blur)
            val fpsRanges = cam2Info.getCameraCharacteristic(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
            val best30Range = pickBest30FpsRange(fpsRanges)
            if (best30Range != null) {
                opts.setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, best30Range)
            }

            // 5. Indoor gym LED/fluorescent anti-banding + hardware ISP fast denoise & edge clarity
            val abModes = cam2Info.getCameraCharacteristic(CameraCharacteristics.CONTROL_AE_AVAILABLE_ANTIBANDING_MODES)
            if (abModes != null && abModes.contains(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_AUTO)) {
                opts.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_ANTIBANDING_MODE,
                    CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_AUTO
                )
            }
            val nrModes = cam2Info.getCameraCharacteristic(CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES)
            if (nrModes != null && nrModes.contains(CaptureRequest.NOISE_REDUCTION_MODE_FAST)) {
                opts.setCaptureRequestOption(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_FAST)
            }
            val edgeModes = cam2Info.getCameraCharacteristic(CameraCharacteristics.EDGE_AVAILABLE_EDGE_MODES)
            if (edgeModes != null && edgeModes.contains(CaptureRequest.EDGE_MODE_FAST)) {
                opts.setCaptureRequestOption(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_FAST)
            }

            cam2Control.setCaptureRequestOptions(opts.build())
        } catch (_: Exception) {
        }
    }

    private fun pickBest30FpsRange(ranges: Array<Range<Int>>?): Range<Int>? {
        if (ranges.isNullOrEmpty()) return null
        val exact30 = ranges.firstOrNull { it.lower == 30 && it.upper == 30 }
        if (exact30 != null) return exact30
        return ranges
            .filter { it.upper == 30 }
            .maxByOrNull { it.lower }
    }
}
