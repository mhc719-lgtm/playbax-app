package com.playbax.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.cos
import kotlin.math.sin

class PlayBaxSplashView(context: Context) : View(context) {

    private var progress: Float = 0f
    private val arcRect = RectF()
    private val pillRect = RectF()
    private val trianglePath = Path()
    private val arrowPath = Path()

    private val bgPaint = Paint().apply { color = Color.parseColor("#FFFDFC") }
    private val ripplePaint1 = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val ripplePaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val badgeFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val replayArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val whiteShapePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val wordmarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1B1B1F")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
    }
    private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#7A3E34")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    init {
        isClickable = true
        isFocusable = true
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    fun playSplashAnimation(onFinished: () -> Unit) {
        visibility = VISIBLE
        alpha = 1f
        val mainAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1350L
            interpolator = DecelerateInterpolator(1.3f)
            addUpdateListener { anim ->
                progress = anim.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    animate().alpha(0f).setDuration(250L).withEndAction {
                        visibility = GONE
                        onFinished()
                    }.start()
                }
            })
        }
        setOnClickListener {
            mainAnim.cancel()
            animate().alpha(0f).setDuration(150L).withEndAction {
                visibility = GONE
                onFinished()
            }.start()
        }
        mainAnim.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        canvas.drawRect(0f, 0f, w, h, bgPaint)

        val d = resources.displayMetrics.density
        val cx = w * 0.5f
        val cy = h * 0.44f
        val baseRadius = 56f * d

        // 1. Expanding Lottie-style kinetic ripple rings
        val ripplePhase1 = ((progress - 0.1f) / 0.85f).coerceIn(0f, 1f)
        if (ripplePhase1 > 0f) {
            ripplePaint1.strokeWidth = 3f * d * (1f - ripplePhase1 * 0.5f)
            ripplePaint1.color = Color.argb(((1f - ripplePhase1) * 75).toInt(), 255, 61, 0)
            canvas.drawCircle(cx, cy, baseRadius * (1.05f + ripplePhase1 * 0.95f), ripplePaint1)
        }
        val ripplePhase2 = ((progress - 0.28f) / 0.72f).coerceIn(0f, 1f)
        if (ripplePhase2 > 0f) {
            ripplePaint2.strokeWidth = 2.5f * d * (1f - ripplePhase2 * 0.5f)
            ripplePaint2.color = Color.argb(((1f - ripplePhase2) * 60).toInt(), 255, 109, 0)
            canvas.drawCircle(cx, cy, baseRadius * (1.02f + ripplePhase2 * 0.65f), ripplePaint2)
        }

        // 2. Elastic spring scale for the circular Electric Orange-Red emblem
        val scaleProgress = (progress / 0.42f).coerceIn(0f, 1f)
        val springScale = if (scaleProgress < 1f) {
            val t = scaleProgress - 1f
            1f + 2.4f * t * t * t + 1.4f * t * t
        } else 1f
        val r = baseRadius * springScale.coerceAtLeast(0.05f)

        badgeFillPaint.shader = LinearGradient(
            cx - r, cy - r, cx + r, cy + r,
            intArrayOf(
                Color.parseColor("#FF1744"),
                Color.parseColor("#FF3D00"),
                Color.parseColor("#FF6D00"),
                Color.parseColor("#FF9100")
            ),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r, badgeFillPaint)

        // 3. Animated SVG vector replay arc sweeping 0° -> 295° inside the circular emblem
        val arcProgress = ((progress - 0.12f) / 0.58f).coerceIn(0f, 1f)
        if (arcProgress > 0f) {
            val ringR = r * 0.58f
            arcRect.set(cx - ringR, cy - ringR, cx + ringR, cy + ringR)
            replayArcPaint.strokeWidth = r * 0.145f
            val startDeg = -90f + (1f - arcProgress) * 90f
            val sweepDeg = 295f * arcProgress
            canvas.drawArc(arcRect, startDeg, sweepDeg, false, replayArcPaint)

            // Rotating arrowhead at the start of the replay loop
            val tipRad = Math.toRadians(startDeg.toDouble())
            val tipX = cx + cos(tipRad).toFloat() * ringR
            val tipY = cy + sin(tipRad).toFloat() * ringR
            val arrowSize = r * 0.19f * arcProgress
            arrowPath.reset()
            arrowPath.moveTo(tipX - arrowSize * 0.9f, tipY)
            arrowPath.lineTo(tipX + arrowSize * 0.45f, tipY - arrowSize * 0.75f)
            arrowPath.lineTo(tipX + arrowSize * 0.45f, tipY + arrowSize * 0.75f)
            arrowPath.close()
            canvas.drawPath(arrowPath, whiteShapePaint)
        }

        // 4. Central Play Triangle spring pop
        val playProgress = ((progress - 0.26f) / 0.40f).coerceIn(0f, 1f)
        if (playProgress > 0f) {
            val triR = r * 0.25f * playProgress
            trianglePath.reset()
            trianglePath.moveTo(cx - triR * 0.55f, cy - triR * 0.85f)
            trianglePath.lineTo(cx + triR * 0.95f, cy)
            trianglePath.lineTo(cx - triR * 0.55f, cy + triR * 0.85f)
            trianglePath.close()
            canvas.drawPath(trianglePath, whiteShapePaint)
        }

        // 5. Kinetic Wordmark & Subtitle slide-in (matching App Menu branding)
        val textPhase = ((progress - 0.25f) / 0.50f).coerceIn(0f, 1f)
        if (textPhase > 0f) {
            val yOffset = (1f - textPhase) * 18f * d
            wordmarkPaint.textSize = 32f * d
            wordmarkPaint.alpha = (textPhase * 255).toInt()
            val titleY = cy + baseRadius + 46f * d + yOffset
            canvas.drawText("PlayBax", cx, titleY, wordmarkPaint)

            subtitlePaint.alpha = (textPhase * 255).toInt()
            subtitlePaint.textSize = 14.5f * d
            canvas.drawText("Live Delayed Video Playback", cx, titleY + 26f * d, subtitlePaint)
        }
    }
}
