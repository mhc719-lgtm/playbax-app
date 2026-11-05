package com.playbax.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.camera.view.PreviewView
import java.util.Locale
import kotlin.math.max

/**
 * Hardware-accelerated rounded video card rendering its assigned delayed stream (+1x, +2x, +3x)
 * with a clean Google soft pill badge and warmup progress bar.
 */
class DelayedScreenPanelView(
    context: Context,
    private val slotNumber: Int
) : View(context) {

    var frameBitmap: Bitmap? = null
    var isTopEdgeSlot: Boolean = (slotNumber == 1)
    private var delayTargetSeconds: Int = 5
    private var isWarmingUp: Boolean = true
    private var remainingWarmupSec: Float = 5f
    private var warmupProgress: Float = 0f

    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    private val srcRect = Rect()
    private val dstRect = RectF()
    private val cardRect = RectF()
    private val clipPath = Path()
    private val badgeRect = RectF()
    private val progressBgRect = RectF()
    private val progressFillRect = RectF()

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#33FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = dp(1.5f)
    }
    private val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E6FFFFFF")
        style = Paint.Style.FILL
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1F1F1F")
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        textSize = dp(12f)
    }
    private val progressTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#66FFFFFF")
    }
    private val progressActivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00BFA5")
    }

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    fun updateFrameState(
        bitmap: Bitmap?,
        targetDelaySec: Int,
        warmingUp: Boolean,
        remainingSec: Float,
        progress: Float
    ) {
        this.frameBitmap = bitmap
        this.delayTargetSeconds = targetDelaySec
        this.isWarmingUp = warmingUp
        this.remainingWarmupSec = remainingSec
        this.warmupProgress = progress
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val corner = dp(18f)
        cardRect.set(dp(4f), dp(4f), w - dp(4f), h - dp(4f))

        val bmp = frameBitmap
        if (bmp != null && !bmp.isRecycled && bmp.width > 0 && bmp.height > 0) {
            val saveCount = canvas.save()
            clipPath.reset()
            clipPath.addRoundRect(cardRect, corner, corner, Path.Direction.CW)
            canvas.clipPath(clipPath)

            srcRect.set(0, 0, bmp.width, bmp.height)
            val scale = max(cardRect.width() / bmp.width.toFloat(), cardRect.height() / bmp.height.toFloat())
            val scaledW = bmp.width * scale
            val scaledH = bmp.height * scale
            val left = cardRect.centerX() - scaledW * 0.5f
            val top = cardRect.centerY() - scaledH * 0.5f
            dstRect.set(left, top, left + scaledW, top + scaledH)
            canvas.drawBitmap(bmp, srcRect, dstRect, bitmapPaint)
            canvas.restoreToCount(saveCount)
        }

        canvas.drawRoundRect(cardRect, corner, corner, borderPaint)
        drawDelayBadge(canvas, w)
    }

    private fun drawDelayBadge(canvas: Canvas, viewWidth: Float) {
        val label: String
        val dotColor: Int
        if (isWarmingUp) {
            val remStr = String.format(Locale.US, "%.1fs", remainingWarmupSec)
            label = "Screen $slotNumber • +${delayTargetSeconds}s in $remStr"
            dotColor = Color.parseColor("#F29900")
        } else {
            label = "Screen $slotNumber • -${delayTargetSeconds}s DELAY"
            dotColor = Color.parseColor("#00A884")
        }

        val padH = dp(12f)
        val dotRadius = dp(4.5f)
        val dotGap = dp(7f)
        val textWidth = textPaint.measureText(label)
        val badgeW = (padH * 2f + dotRadius * 2f + dotGap + textWidth).coerceAtMost(viewWidth - dp(28f))
        val badgeH = dp(30f)

        val left = dp(14f)
        val top = if (isTopEdgeSlot) dp(88f) else dp(14f)
        badgeRect.set(left, top, left + badgeW, top + badgeH)

        val corner = badgeH * 0.5f
        canvas.drawRoundRect(badgeRect, corner, corner, badgeBgPaint)

        dotPaint.color = dotColor
        val dotCx = left + padH + dotRadius
        val dotCy = top + badgeH * 0.5f
        canvas.drawCircle(dotCx, dotCy, dotRadius, dotPaint)

        val textX = dotCx + dotRadius + dotGap
        val textY = dotCy - (textPaint.descent() + textPaint.ascent()) * 0.5f
        canvas.drawText(label, textX, textY, textPaint)

        if (isWarmingUp) {
            val barTop = badgeRect.bottom + dp(4f)
            val barH = dp(4f)
            progressBgRect.set(left + dp(6f), barTop, left + badgeW - dp(6f), barTop + barH)
            canvas.drawRoundRect(progressBgRect, barH, barH, progressTrackPaint)

            val fillW = progressBgRect.width() * warmupProgress.coerceIn(0.06f, 1f)
            progressFillRect.set(
                progressBgRect.left,
                barTop,
                progressBgRect.left + fillW,
                barTop + barH
            )
            canvas.drawRoundRect(progressFillRect, barH, barH, progressActivePaint)
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}

/**
 * Multi-screen camera stage holding both the hardware PreviewView anchor and
 * 1, 2, or 3 DelayedScreenPanelView tiles in Portrait Stacked or Landscape Side-by-Side.
 */
class PlayBaxMultiScreenStage(context: Context) : FrameLayout(context) {

    val previewView = PreviewView(context).apply {
        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        scaleType = PreviewView.ScaleType.FILL_CENTER
    }

    private val tilesGrid = LinearLayout(context)
    val panels = listOf(
        DelayedScreenPanelView(context, 1),
        DelayedScreenPanelView(context, 2),
        DelayedScreenPanelView(context, 3)
    )

    init {
        setBackgroundColor(Color.parseColor("#111316"))
        addView(previewView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        val pad = (6f * resources.displayMetrics.density).toInt()
        tilesGrid.setPadding(pad, pad, pad, pad)
        addView(tilesGrid, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        applyStageLayout(screenCount = 1, sideBySide = false)
    }

    fun applyStageLayout(screenCount: Int, sideBySide: Boolean) {
        val clampedCount = screenCount.coerceIn(1, 3)
        tilesGrid.orientation = if (sideBySide) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
        tilesGrid.removeAllViews()

        for (i in 0 until clampedCount) {
            val panel = panels[i]
            panel.isTopEdgeSlot = sideBySide || (i == 0)
            val lp = if (sideBySide) {
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            } else {
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            }
            tilesGrid.addView(panel, lp)
        }
    }
}
