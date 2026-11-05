package com.playbax.app

import android.hardware.camera2.CameraCharacteristics
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraInfo
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

    fun applyZoomForPreference(camera: Camera?, lensMode: Int) {
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
    }
}
