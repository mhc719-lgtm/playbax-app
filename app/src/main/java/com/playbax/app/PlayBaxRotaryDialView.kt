package com.playbax.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.SoundEffectConstants
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

class DelayRingGaugeView(context: Context) : View(context) {

    var onDelaySelected: ((Int) -> Unit)? = null

    private var delaySec: Int = 5
    private var screenCount: Int = 1
    private var isDraggingDial = false
    private var pulseScale: Float = 1f
    private var lastTouchDeg: Float = 0f
    private var accumDeg: Float = 60f

    private val arcRect = RectF()

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E8ECF2")
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val activeArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val thumbHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#40FF3D00")
        style = Paint.Style.FILL
    }
    private val thumbFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF3D00")
        style = Paint.Style.FILL
    }
    private val thumbStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
    }
    private val thumbDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val topLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4E525A")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val centerValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1B1B1F")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
    }
    private val bottomSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF3D00")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#7A808C")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    init {
        isClickable = true
        isFocusable = true
        isHapticFeedbackEnabled = true
    }

    fun updateState(delaySeconds: Int, screens: Int) {
        val clamped = delaySeconds.coerceIn(1, 30)
        this.delaySec = clamped
        this.screenCount = screens.coerceIn(1, 3)
        if (!isDraggingDial) {
            this.accumDeg = clamped * 12f
        }
        invalidate()
    }

    private fun triggerIntervalDetentFeedback() {
        GoogleStyleWidgets.performClickHaptic(this)
        try {
            playSoundEffect(SoundEffectConstants.CLICK)
        } catch (_: Exception) {}

        ValueAnimator.ofFloat(1.09f, 1.0f).apply {
            duration = 110L
            addUpdateListener { anim ->
                pulseScale = anim.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val cx = width * 0.5f
        val cy = height * 0.5f
        val dx = event.x - cx
        val dy = event.y - cy
        val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        val size = minOf(width, height).toFloat()

        // Angle in degrees clockwise from 12 o'clock (0..360)
        val rawClockwiseDeg = ((Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f) + 360f) % 360f

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (dist < size * 0.14f) return false
                parent?.requestDisallowInterceptTouchEvent(true)
                isDraggingDial = true
                lastTouchDeg = rawClockwiseDeg

                // Snap immediately to nearest interval (1s..30s, 12 deg each) on touch
                val touchedSec = angleToIntervalSec(rawClockwiseDeg)
                accumDeg = touchedSec * 12f
                val changed = (touchedSec != delaySec)
                applyUserDelaySelection(touchedSec)
                if (!changed) {
                    // Always give a tactile click when first grabbing the current dial position
                    GoogleStyleWidgets.performClickHaptic(this)
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isDraggingDial) return false
                var delta = rawClockwiseDeg - lastTouchDeg
                if (delta > 180f) delta -= 360f
                if (delta < -180f) delta += 360f
                lastTouchDeg = rawClockwiseDeg

                accumDeg += delta
                // Wrap continuously around 1s..30s (360 deg = 30 intervals of 12 deg)
                val wrappedDeg = ((accumDeg - 6f) % 360f + 360f) % 360f
                val stepSec = ((wrappedDeg / 12f).toInt() + 1).coerceIn(1, 30)
                applyUserDelaySelection(stepSec)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDraggingDial) {
                    isDraggingDial = false
                    accumDeg = delaySec * 12f
                    parent?.requestDisallowInterceptTouchEvent(false)
                    invalidate()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun angleToIntervalSec(clockwiseDegFromTop: Float): Int {
        val slot = (clockwiseDegFromTop / 12f).roundToInt()
        return if (slot <= 0) 30 else slot.coerceIn(1, 30)
    }

    private fun applyUserDelaySelection(targetSec: Int) {
        val clamped = targetSec.coerceIn(1, 30)
        if (clamped != delaySec) {
            delaySec = clamped
            triggerIntervalDetentFeedback()
            onDelaySelected?.invoke(clamped)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val size = minOf(w, h)
        val stroke = size * 0.125f
        trackPaint.strokeWidth = stroke
        activeArcPaint.strokeWidth = stroke

        val pad = stroke * 0.95f
        val cx = w * 0.5f
        val cy = h * 0.5f
        val radius = (size * 0.5f) - pad
        arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius)

        activeArcPaint.shader = LinearGradient(
            arcRect.left, arcRect.top, arcRect.right, arcRect.bottom,
            intArrayOf(
                Color.parseColor("#FF1744"),
                Color.parseColor("#FF3D00"),
                Color.parseColor("#FF6D00"),
                Color.parseColor("#FF9100")
            ),
            null, Shader.TileMode.CLAMP
        )

        // 1. Full 360° circular track
        canvas.drawOval(arcRect, trackPaint)

        // 2. Active sweep: exact 12° per second from 1s (12°) to 30s (360°) starting at 12 o'clock (-90°)
        val sweepDeg = delaySec * 12f
        canvas.drawArc(arcRect, -90f, sweepDeg, false, activeArcPaint)

        // 3. 30 Discrete Circular Dot Interval Stops around the outer dial ring
        val dotOrbitR = radius + stroke * 0.70f
        for (i in 1..30) {
            val isMajor = (i % 5 == 0)
            val isReached = (i <= delaySec)
            val dotRadius = if (isMajor) size * 0.0095f else size * 0.0062f
            tickPaint.color = when {
                i == delaySec -> Color.parseColor("#FF1744")
                isReached -> Color.parseColor("#FF6D00")
                isMajor -> Color.parseColor("#9AA4B2")
                else -> Color.parseColor("#CBD2DC")
            }
            val angleRad = Math.toRadians((-90.0 + i * 12.0))
            val dotX = cx + cos(angleRad).toFloat() * dotOrbitR
            val dotY = cy + sin(angleRad).toFloat() * dotOrbitR
            canvas.drawCircle(dotX, dotY, dotRadius, tickPaint)
        }

        // 4. Tactile Rotary Thumb Handle at exact stop angle (-90° + delaySec * 12°)
        val stopRad = Math.toRadians((-90.0 + sweepDeg.toDouble()))
        val thumbX = cx + cos(stopRad).toFloat() * radius
        val thumbY = cy + sin(stopRad).toFloat() * radius
        val thumbR = stroke * 0.56f * if (isDraggingDial) 1.12f else 1.0f

        canvas.drawCircle(thumbX, thumbY, thumbR * 1.45f, thumbHaloPaint)
        canvas.drawCircle(thumbX, thumbY, thumbR, thumbFillPaint)
        thumbStrokePaint.strokeWidth = size * 0.014f
        canvas.drawCircle(thumbX, thumbY, thumbR, thumbStrokePaint)
        canvas.drawCircle(thumbX, thumbY, thumbR * 0.32f, thumbDotPaint)

        // 5. Center Readout with Detent Pulse
        topLabelPaint.textSize = size * 0.066f
        centerValuePaint.textSize = size * 0.25f * pulseScale
        bottomSubPaint.textSize = size * 0.066f

        canvas.drawText("INTERVAL", cx, cy - radius * 0.38f, topLabelPaint)
        canvas.drawText("$delaySec", cx, cy + (size * 0.25f) * 0.28f, centerValuePaint)
        canvas.drawText("seconds", cx, cy + radius * 0.44f, bottomSubPaint)
    }
}
