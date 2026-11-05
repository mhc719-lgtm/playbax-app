package com.playbax.app

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

/**
 * Hardware-friendly frame converter using CameraX 1.3+ native JNI ImageProxy.toBitmap()
 * and direct PreviewView Bitmap compression with zero manual YUV buffer stride loops.
 */
object YuvFrameConverter {

    private const val JPEG_QUALITY = 74
    private const val MAX_DIMENSION_PX = 520

    fun imageProxyToUprightJpeg(
        image: ImageProxy,
        mirrorHorizontally: Boolean
    ): ByteArray? {
        return try {
            val rawBitmap = image.toBitmap()
            val rotation = image.imageInfo.rotationDegrees
            val jpeg = bitmapToJpegBytes(rawBitmap, rotation, mirrorHorizontally)
            if (!rawBitmap.isRecycled) {
                rawBitmap.recycle()
            }
            jpeg
        } catch (_: Exception) {
            null
        }
    }

    fun bitmapToJpegBytes(
        source: Bitmap,
        rotationDegrees: Int,
        mirrorHorizontally: Boolean
    ): ByteArray? {
        return try {
            val w = source.width
            val h = source.height
            if (w <= 0 || h <= 0) return null

            val matrix = Matrix()
            if (rotationDegrees != 0) {
                matrix.postRotate(rotationDegrees.toFloat())
            }
            if (mirrorHorizontally) {
                matrix.postScale(-1f, 1f)
            }

            val maxDim = maxOf(w, h)
            if (maxDim > MAX_DIMENSION_PX) {
                val scale = MAX_DIMENSION_PX.toFloat() / maxDim.toFloat()
                matrix.postScale(scale, scale)
            }

            val transformed = Bitmap.createBitmap(source, 0, 0, w, h, matrix, true)
            val out = ByteArrayOutputStream(24576)
            transformed.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            if (transformed !== source && !transformed.isRecycled) {
                transformed.recycle()
            }
            out.toByteArray()
        } catch (_: Exception) {
            null
        }
    }
}
