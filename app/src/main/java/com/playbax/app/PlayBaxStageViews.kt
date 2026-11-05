package com.playbax.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View
import android.widget.LinearLayout
import java.util.Locale
import kotlin.math.max

/**
 * Hardware-accelerated delayed video panel rendering its assigned delay slot (+1x, +2x, +3x)
 * with a soft Google Material 3 pill badge and warmup progress bar.
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
    private var isPlaybackActive: Boolean = false

    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    private val srcRect = Rect()
    private val dstRect = RectF()
    private val badgeRect = RectF()
    private val progressBgRect = RectF()
    private val progressFillRect = RectF()

    private val bgPaint = Paint().apply { color = Color.parseColor("#0E1013") }
    private val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D9131314")
        style = Paint.Style.FILL
    }
    private val badgeStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#38A8C7FA")
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F1F3F4")
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        textSize = dp(12.5f)
    }
    private val progressTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4D2C3038")
    }
    private val progressActivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#669DF6")
    }

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    fun updateFrameState(
        bitmap: Bitmap?,
        targetDelaySec: Int,
        active: Boolean,
        warmingUp: Boolean,
        remainingSec: Float,
        progress: Float
    ) {
        this.frameBitmap = bitmap
        this.delayTargetSeconds = targetDelaySec
        this.isPlaybackActive = active
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

        canvas.drawRect(0f, 0f, w, h, bgPaint)

        val bmp = frameBitmap
        if (bmp != null && !bmp.isRecycled && bmp.width > 0 && bmp.height > 0) {
            srcRect.set(0, 0, bmp.width, bmp.height)
            val scale = max(w / bmp.width.toFloat(), h / bmp.height.toFloat())
            val scaledW = bmp.width * scale
            val scaledH = bmp.height * scale
            val left = (w - scaledW) * 0.5f
            val top = (h - scaledH) * 0.5f
            dstRect.set(left, top, left + scaledW, top + scaledH)
            canvas.drawBitmap(bmp, srcRect, dstRect, bitmapPaint)
        }

        drawDelayBadge(canvas, w)
    }

    private fun drawDelayBadge(canvas: Canvas, viewWidth: Float) {
        val label: String
        val dotColor: Int
        when {
            !isPlaybackActive -> {
                label = "Screen $slotNumber • +${delayTargetSeconds}s Ready"
                dotColor = Color.parseColor("#A8C7FA")
            }
            isWarmingUp -> {
                val remStr = String.format(Locale.US, "%.1fs", remainingWarmupSec)
                label = "Screen $slotNumber • +${delayTargetSeconds}s in $remStr"
                dotColor = Color.parseColor("#FDD663")
            }
            else -> {
                label = "Screen $slotNumber • -${delayTargetSeconds}s DELAY"
                dotColor = Color.parseColor("#81C995")
            }
        }

        val padH = dp(12f)
        val dotRadius = dp(4f)
        val dotGap = dp(8f)
        val textWidth = textPaint.measureText(label)
        val badgeW = (padH * 2f + dotRadius * 2f + dotGap + textWidth).coerceAtMost(viewWidth - dp(24f))
        val badgeH = dp(30f)

        val left = dp(14f)
        val top = if (isTopEdgeSlot) dp(92f) else dp(12f)
        badgeRect.set(left, top, left + badgeW, top + badgeH)

        val corner = badgeH * 0.5f
        canvas.drawRoundRect(badgeRect, corner, corner, badgeBgPaint)
        canvas.drawRoundRect(badgeRect, corner, corner, badgeStrokePaint)

        dotPaint.color = dotColor
        val dotCx = left + padH + dotRadius
        val dotCy = top + badgeH * 0.5f
        canvas.drawCircle(dotCx, dotCy, dotRadius, dotPaint)

        val textX = dotCx + dotRadius + dotGap
        val textY = dotCy - (textPaint.descent() + textPaint.ascent()) * 0.5f
        canvas.drawText(label, textX, textY, textPaint)

        if (isPlaybackActive && isWarmingUp) {
            val barTop = badgeRect.bottom + dp(4f)
            val barH = dp(3.5f)
            progressBgRect.set(left + dp(4f), barTop, left + badgeW - dp(4f), barTop + barH)
            canvas.drawRoundRect(progressBgRect, barH, barH, progressTrackPaint)

            val fillW = progressBgRect.width() * warmupProgress.coerceIn(0.05f, 1f)
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
 * Multi-screen container managing 1, 2, or 3 DelayedScreenPanelViews in either
 * Portrait Stacked (VERTICAL) or Landscape Side-by-Side (HORIZONTAL) orientation.
 */
class PlayBaxMultiScreenStage(context: Context) : LinearLayout(context) {

    val panels = listOf(
        DelayedScreenPanelView(context, 1),
        DelayedScreenPanelView(context, 2),
        DelayedScreenPanelView(context, 3)
    )
    private val dividers = listOf(
        View(context).apply { setBackgroundColor(Color.parseColor("#1E1F23")) },
        View(context).apply { setBackgroundColor(Color.parseColor("#1E1F23")) }
    )

    init {
        setBackgroundColor(Color.parseColor("#0E1013"))
        applyStageLayout(screenCount = 1, sideBySide = false)
    }

    fun applyStageLayout(screenCount: Int, sideBySide: Boolean) {
        val clampedCount = screenCount.coerceIn(1, 3)
        orientation = if (sideBySide) HORIZONTAL else VERTICAL
        removeAllViews()

        val divPx = (2f * resources.displayMetrics.density).toInt().coerceAtLeast(2)
        for (i in 0 until clampedCount) {
            val panel = panels[i]
            panel.isTopEdgeSlot = sideBySide || (i == 0)
            val lp = if (sideBySide) {
                LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
            } else {
                LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
            }
            addView(panel, lp)

            if (i < clampedCount - 1) {
                val div = dividers[i]
                val divLp = if (sideBySide) {
                    LayoutParams(divPx, LayoutParams.MATCH_PARENT)
                } else {
                    LayoutParams(LayoutParams.MATCH_PARENT, divPx)
                }
                addView(div, divLp)
            }
        }
    }
}
