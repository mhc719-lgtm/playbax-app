package com.playbax.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.camera.view.PreviewView
import kotlin.math.max

/**
 * Pure edge-to-edge hardware-accelerated delayed video panel free of UI badges or borders.
 */
class DelayedScreenPanelView(context: Context) : View(context) {

    var frameBitmap: Bitmap? = null
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    private val srcRect = Rect()
    private val dstRect = RectF()

    fun updateFrameState(bitmap: Bitmap?) {
        this.frameBitmap = bitmap
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val bmp = frameBitmap
        if (bmp != null && !bmp.isRecycled && bmp.width > 0 && bmp.height > 0) {
            canvas.drawColor(Color.BLACK)
            srcRect.set(0, 0, bmp.width, bmp.height)
            val scale = max(w / bmp.width.toFloat(), h / bmp.height.toFloat())
            val scaledW = bmp.width * scale
            val scaledH = bmp.height * scale
            val left = (w - scaledW) * 0.5f
            val top = (h - scaledH) * 0.5f
            dstRect.set(left, top, left + scaledW, top + scaledH)
            canvas.drawBitmap(bmp, srcRect, dstRect, bitmapPaint)
        }
    }
}

/**
 * Multi-screen camera stage holding the live PreviewView anchor and 1, 2, or 3
 * full-bleed DelayedScreenPanelView viewports in Portrait Stacked or Landscape Side-by-Side.
 */
class PlayBaxMultiScreenStage(context: Context) : FrameLayout(context) {

    val previewView = PreviewView(context).apply {
        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        scaleType = PreviewView.ScaleType.FILL_CENTER
    }

    private val tilesGrid = LinearLayout(context)
    val panels = listOf(
        DelayedScreenPanelView(context),
        DelayedScreenPanelView(context),
        DelayedScreenPanelView(context)
    )
    private val dividers = listOf(
        View(context).apply { setBackgroundColor(Color.parseColor("#26FFFFFF")) },
        View(context).apply { setBackgroundColor(Color.parseColor("#26FFFFFF")) }
    )

    init {
        setBackgroundColor(Color.BLACK)
        addView(previewView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(tilesGrid, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        applyStageLayout(screenCount = 1, sideBySide = false)
    }

    fun clearPanelBitmaps() {
        panels.forEach { it.updateFrameState(null) }
    }

    fun applyStageLayout(screenCount: Int, sideBySide: Boolean) {
        val clampedCount = screenCount.coerceIn(1, 3)
        tilesGrid.orientation = if (sideBySide) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
        tilesGrid.removeAllViews()

        val divPx = (1f * resources.displayMetrics.density).toInt().coerceAtLeast(1)
        for (i in 0 until clampedCount) {
            val panel = panels[i]
            val lp = if (sideBySide) {
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            } else {
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            }
            tilesGrid.addView(panel, lp)

            if (i < clampedCount - 1) {
                val div = dividers[i]
                val divLp = if (sideBySide) {
                    LinearLayout.LayoutParams(divPx, LinearLayout.LayoutParams.MATCH_PARENT)
                } else {
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, divPx)
                }
                tilesGrid.addView(div, divLp)
            }
        }
    }
}
