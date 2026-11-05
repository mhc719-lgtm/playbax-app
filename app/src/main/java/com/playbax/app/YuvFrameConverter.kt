package com.playbax.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

/**
 * Converts CameraX YUV_420_888 ImageProxy frames into upright, aspect-correct,
 * memory-compact JPEG byte arrays on the background analysis executor.
 */
object YuvFrameConverter {

    private const val JPEG_QUALITY = 72
    private const val MAX_DIMENSION_PX = 560

    fun imageProxyToUprightJpeg(
        image: ImageProxy,
        mirrorHorizontally: Boolean
    ): ByteArray? {
        return try {
            val nv21 = yuv420888ToNv21(image) ?: return null
            val width = image.width
            val height = image.height
            val rawOut = ByteArrayOutputStream(32768)
            val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
            yuvImage.compressToJpeg(Rect(0, 0, width, height), 80, rawOut)
            val rawJpeg = rawOut.toByteArray()

            val sampleSize = if (maxOf(width, height) > MAX_DIMENSION_PX * 2) 2 else 1
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val rawBitmap = BitmapFactory.decodeByteArray(rawJpeg, 0, rawJpeg.size, opts) ?: return null

            val rotation = image.imageInfo.rotationDegrees
            val matrix = Matrix()
            if (rotation != 0) {
                matrix.postRotate(rotation.toFloat())
            }
            if (mirrorHorizontally) {
                matrix.postScale(-1f, 1f)
            }

            val maxDim = maxOf(rawBitmap.width, rawBitmap.height)
            if (maxDim > MAX_DIMENSION_PX) {
                val scale = MAX_DIMENSION_PX.toFloat() / maxDim.toFloat()
                matrix.postScale(scale, scale)
            }

            val uprightBitmap = Bitmap.createBitmap(
                rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true
            )
            if (uprightBitmap !== rawBitmap) {
                rawBitmap.recycle()
            }

            val finalOut = ByteArrayOutputStream(24576)
            uprightBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, finalOut)
            uprightBitmap.recycle()
            finalOut.toByteArray()
        } catch (_: Exception) {
            null
        }
    }

    private fun yuv420888ToNv21(image: ImageProxy): ByteArray? {
        val width = image.width
        val height = image.height
        val ySize = width * height
        val uvSize = width * height / 2
        val nv21 = ByteArray(ySize + uvSize)

        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        yBuffer.rewind()
        uBuffer.rewind()
        vBuffer.rewind()

        val yRowStride = yPlane.rowStride
        var pos = 0
        if (yRowStride == width) {
            yBuffer.get(nv21, 0, ySize)
            pos = ySize
        } else {
            for (row in 0 until height) {
                yBuffer.position(row * yRowStride)
                yBuffer.get(nv21, pos, width)
                pos += width
            }
        }

        val uvRowStride = uPlane.rowStride
        val uvPixelStride = uPlane.pixelStride
        val uvWidth = width / 2
        val uvHeight = height / 2

        for (row in 0 until uvHeight) {
            val rowOffset = row * uvRowStride
            for (col in 0 until uvWidth) {
                val uvIndex = rowOffset + col * uvPixelStride
                nv21[pos++] = vBuffer.get(uvIndex)
                nv21[pos++] = uBuffer.get(uvIndex)
            }
        }
        return nv21
    }
}
