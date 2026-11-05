package com.playbax.app

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider

object CameraLensHelper {

    fun buildCameraSelector(
        context: Context,
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

        val widestBackCameraId = findWidestBackCameraId(context)
        if (widestBackCameraId != null) {
            return CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .addCameraFilter { cameraInfos ->
                    val filtered = cameraInfos.filter { info ->
                        info.toString().contains("CameraId=$widestBackCameraId") ||
                            info.toString().contains("id=$widestBackCameraId")
                    }
                    filtered.ifEmpty { cameraInfos }
                }
                .build()
        }
        return CameraSelector.DEFAULT_BACK_CAMERA
    }

    fun applyZoomForPreference(camera: Camera?, lensMode: Int) {
        if (camera == null) return
        try {
            val zoomState = camera.cameraInfo.zoomState.value
            val minZoom = zoomState?.minZoomRatio ?: 1.0f
            val targetZoom = if (lensMode == PlayBaxPreferences.LENS_REAR_ULTRA) {
                if (minZoom < 1.0f) minZoom else 1.0f
            } else {
                1.0f.coerceAtLeast(minZoom)
            }
            camera.cameraControl.setZoomRatio(targetZoom)
        } catch (_: Exception) {
        }
    }

    private fun findWidestBackCameraId(context: Context): String? {
        return try {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            var bestId: String? = null
            var minFocalLength = Float.MAX_VALUE

            for (cameraId in manager.cameraIdList) {
                val chars = manager.getCameraCharacteristics(cameraId)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (facing != CameraCharacteristics.LENS_FACING_BACK) continue

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val zoomRange = chars.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
                    if (zoomRange != null && zoomRange.lower < 0.85f) {
                        return cameraId
                    }
                }

                val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                if (focalLengths != null && focalLengths.isNotEmpty()) {
                    val minFocal = focalLengths.minOrNull() ?: Float.MAX_VALUE
                    if (minFocal < minFocalLength) {
                        minFocalLength = minFocal
                        bestId = cameraId
                    }
                }
            }
            bestId
        } catch (_: Exception) {
            null
        }
    }
}
