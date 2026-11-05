package com.playbax.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.SeekBar
import android.widget.TextView

class DelayRingGaugeView(context: Context) : View(context) {

    private var delaySec: Int = 5
    private var screenCount: Int = 1
    private val arcRect = RectF()

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E8ECF2")
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
    }
    private val activeArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
    }
    private val topLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4E525A"); textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val centerValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1B1B1F"); textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }
    private val bottomSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF3D00"); textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    }

    fun updateState(delaySeconds: Int, screens: Int) {
        this.delaySec = delaySeconds.coerceIn(1, 30)
        this.screenCount = screens.coerceIn(1, 3)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val size = minOf(w, h)
        val stroke = size * 0.14f
        trackPaint.strokeWidth = stroke; activeArcPaint.strokeWidth = stroke

        val pad = stroke * 0.85f
        val cx = w * 0.5f; val cy = h * 0.5f
        val radius = (size * 0.5f) - pad
        arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius)

        activeArcPaint.shader = LinearGradient(
            arcRect.left, arcRect.top, arcRect.right, arcRect.bottom,
            intArrayOf(Color.parseColor("#FF1744"), Color.parseColor("#FF3D00"), Color.parseColor("#FF6D00")),
            null, Shader.TileMode.CLAMP
        )

        canvas.drawOval(arcRect, trackPaint)
        val sweep = (delaySec / 30f).coerceIn(0.08f, 1f) * 300f
        canvas.drawArc(arcRect, 120f, sweep, false, activeArcPaint)

        topLabelPaint.textSize = size * 0.082f
        centerValuePaint.textSize = size * 0.24f
        bottomSubPaint.textSize = size * 0.078f

        canvas.drawText("Delay interval", cx, cy - radius * 0.36f, topLabelPaint)
        canvas.drawText("${delaySec}s", cx, cy + centerValuePaint.textSize * 0.28f, centerValuePaint)

        val cascade = when (screenCount) {
            1 -> "+${delaySec}s"
            2 -> "+${delaySec}s • +${delaySec * 2}s"
            else -> "+${delaySec}s•+${delaySec * 2}s•+${delaySec * 3}s"
        }
        canvas.drawText(cascade, cx, cy + radius * 0.56f, bottomSubPaint)
    }
}

class PastelBentoTileRefs(val card: LinearLayout, val valueText: TextView, val subtitleText: TextView)
class HealthBottomNavTabRefs(val rootTab: LinearLayout, val iconPill: TextView, val labelText: TextView)

object GoogleStyleWidgets {

    const val LIGHT_BG = "#FFFDFC"
    const val LIGHT_SURFACE_PILL = "#EEF1F6"
    const val TEXT_DARK = "#1B1B1F"
    const val TEXT_MUTED = "#4E525A"
    const val BRAND_ORANGE_PRIMARY = "#FF3D00"
    const val BRAND_CRIMSON = "#FF1744"
    const val SOFT_PEACH_PILL = "#FFE0D6"

    fun createBrandHeaderRow(context: Context): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        val logo = ImageView(context).apply {
            setImageResource(R.drawable.ic_playbax_launcher)
            layoutParams = LinearLayout.LayoutParams(dp(context, 30), dp(context, 30)).apply { rightMargin = dp(context, 10) }
        }
        val title = TextView(context).apply {
            text = "PlayBax"; setTextColor(Color.parseColor(TEXT_DARK))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 21f)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }
        addView(logo); addView(title)
    }

    fun createPastelBentoTile(
        context: Context, bgHex: String, iconSymbol: String, iconColorHex: String,
        title: String, initialValue: String, initialSub: String, onClick: (View) -> Unit
    ): PastelBentoTileRefs {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(context, 10), dp(context, 8), dp(context, 12), dp(context, 8))
            background = roundedDrawable(context, bgHex, 24)
            setOnClickListener {
                try { performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) } catch (_: Exception) {}
                onClick(this)
            }
        }
        val iconPill = TextView(context).apply {
            text = iconSymbol; gravity = Gravity.CENTER
            setTextColor(Color.parseColor(iconColorHex)); setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = roundedDrawable(context, "#FFFFFF", 999)
            layoutParams = LinearLayout.LayoutParams(dp(context, 34), dp(context, 48)).apply { rightMargin = dp(context, 10) }
        }
        val textCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        val titleTv = TextView(context).apply {
            text = title; isSingleLine = true; ellipsize = TextUtils.TruncateAt.END
            setTextColor(Color.parseColor("#2B2D31")); setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        val valueTv = TextView(context).apply {
            text = initialValue; isSingleLine = true; ellipsize = TextUtils.TruncateAt.END
            setTextColor(Color.parseColor(TEXT_DARK)); setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }
        val subTv = TextView(context).apply {
            text = initialSub; isSingleLine = true; ellipsize = TextUtils.TruncateAt.END
            setTextColor(Color.parseColor("#3C3F46")); setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
        }
        textCol.addView(titleTv); textCol.addView(valueTv); textCol.addView(subTv)
        card.addView(iconPill); card.addView(textCol)
        return PastelBentoTileRefs(card, valueTv, subTv)
    }

    fun createHealthBottomNavTab(context: Context, iconSymbol: String, label: String): HealthBottomNavTabRefs {
        val rootTab = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(0, dp(context, 4), 0, dp(context, 4))
        }
        val iconPill = TextView(context).apply {
            text = iconSymbol; gravity = Gravity.CENTER; setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(dp(context, 64), dp(context, 32)).apply { bottomMargin = dp(context, 3) }
        }
        val labelText = TextView(context).apply {
            text = label; gravity = Gravity.CENTER; setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        rootTab.addView(iconPill); rootTab.addView(labelText)
        return HealthBottomNavTabRefs(rootTab, iconPill, labelText)
    }

    fun styleHealthNavTab(context: Context, tab: HealthBottomNavTabRefs, active: Boolean) {
        tab.iconPill.background = roundedDrawable(context, if (active) SOFT_PEACH_PILL else "#00000000", 999)
        tab.iconPill.setTextColor(Color.parseColor(if (active) BRAND_ORANGE_PRIMARY else TEXT_MUTED))
        tab.labelText.setTextColor(Color.parseColor(if (active) BRAND_ORANGE_PRIMARY else TEXT_MUTED))
        tab.labelText.typeface = Typeface.create("sans-serif-medium", if (active) Typeface.BOLD else Typeface.NORMAL)
    }

    fun styleLightGoogleSlider(context: Context, seekBar: SeekBar) {
        val trackHeight = dp(context, 12); val radius = dp(context, 999).toFloat()
        val bgTrack = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE; cornerRadius = radius
            setColor(Color.parseColor("#E8ECF2")); setSize(-1, trackHeight)
        }
        val activeProgressRect = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(Color.parseColor("#FF1744"), Color.parseColor("#FF3D00"), Color.parseColor("#FF6D00"))
        ).apply { shape = GradientDrawable.RECTANGLE; cornerRadius = radius; setSize(-1, trackHeight) }

        val progressClip = ClipDrawable(activeProgressRect, Gravity.START, ClipDrawable.HORIZONTAL)
        val layerDrawable = LayerDrawable(arrayOf(bgTrack, progressClip)).apply {
            setId(0, android.R.id.background); setId(1, android.R.id.progress)
        }
        val thumbSize = dp(context, 24)
        val thumbDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL; setSize(thumbSize, thumbSize)
            setColor(Color.parseColor(BRAND_ORANGE_PRIMARY)); setStroke(dp(context, 3), Color.WHITE)
        }
        seekBar.progressDrawable = layerDrawable; seekBar.thumb = thumbDrawable
        seekBar.splitTrack = false
        seekBar.setPadding(dp(context, 12), dp(context, 6), dp(context, 12), dp(context, 6))
    }

    fun createLightStepperButton(context: Context, symbol: String, onClick: () -> Unit): TextView = TextView(context).apply {
        text = symbol; gravity = Gravity.CENTER; setTextColor(Color.parseColor(BRAND_ORANGE_PRIMARY))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        background = roundedDrawable(context, SOFT_PEACH_PILL, 999)
        layoutParams = LinearLayout.LayoutParams(dp(context, 38), dp(context, 38))
        setOnClickListener {
            try { performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) } catch (_: Exception) {}
            onClick()
        }
    }

    fun showGooglePopupMenu(context: Context, anchor: View, options: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit) {
        try {
            val popup = PopupMenu(context, anchor)
            options.forEachIndexed { idx, title -> popup.menu.add(0, idx, idx, "${if (idx == selectedIndex) "✓  " else "     "}$title") }
            popup.setOnMenuItemClickListener { item -> onSelected(item.itemId); true }
            popup.show()
        } catch (_: Exception) { onSelected((selectedIndex + 1) % options.size) }
    }

    fun roundedDrawable(context: Context, bgHex: String, radiusDp: Int, strokeHex: String? = null): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE; cornerRadius = dp(context, radiusDp).toFloat()
        setColor(Color.parseColor(bgHex))
        if (strokeHex != null) setStroke(dp(context, 1), Color.parseColor(strokeHex))
    }

    fun dp(context: Context, value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
}
