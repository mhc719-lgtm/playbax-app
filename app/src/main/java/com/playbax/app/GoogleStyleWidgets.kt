package com.playbax.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView

class PastelBentoTileRefs(val card: LinearLayout, val valueText: TextView, val subtitleText: TextView)
class HealthBottomNavTabRefs(val rootTab: LinearLayout, val iconPill: FrameLayout, val iconView: ImageView, val labelText: TextView)

object GoogleStyleWidgets {

    const val LIGHT_BG = "#FFFDFC"
    const val BOTTOM_NAV_BG = "#F4EAE6"
    const val LIGHT_SURFACE_PILL = "#EEF1F6"
    const val TEXT_DARK = "#1B1B1F"
    const val TEXT_MUTED = "#4E525A"
    const val BRAND_ORANGE_PRIMARY = "#FF3D00"
    const val BRAND_CRIMSON = "#FF1744"
    const val SOFT_PEACH_PILL = "#FFE0D6"
    const val ACTIVE_NAV_PILL = "#FFCCBC"

    fun createBrandHeaderRow(context: Context): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        val logo = ImageView(context).apply {
            setImageResource(R.drawable.ic_playbax_launcher)
            layoutParams = LinearLayout.LayoutParams(dp(context, 38), dp(context, 38)).apply { rightMargin = dp(context, 11) }
        }
        val title = TextView(context).apply {
            text = "PlayBax"; setTextColor(Color.parseColor(TEXT_DARK))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 25f)
            letterSpacing = -0.02f
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
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
                performClickHaptic(this)
                onClick(this)
            }
        }
        val iconPill = object : View(context) {
            private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor(iconColorHex)
                style = Paint.Style.STROKE
                strokeWidth = dp(context, 2).toFloat() * 1.15f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor(iconColorHex)
                style = Paint.Style.FILL
            }
            private val rect = RectF()
            private val path = Path()

            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                val cx = width * 0.5f
                val cy = height * 0.5f
                val r = dp(context, 10).toFloat()
                when (iconSymbol) {
                    "▣" -> {
                        // Cascading / Stacked Video Screens icon
                        val sw = r * 1.45f
                        val sh = r * 1.05f
                        // Back cascading screen
                        path.reset()
                        path.moveTo(cx - sw * 0.25f, cy - sh * 0.68f)
                        path.lineTo(cx + sw * 0.72f, cy - sh * 0.68f)
                        path.lineTo(cx + sw * 0.72f, cy + sh * 0.35f)
                        canvas.drawPath(path, strokePaint)
                        // Front primary screen with play triangle
                        rect.set(cx - sw * 0.72f, cy - sh * 0.32f, cx + sw * 0.38f, cy + sh * 0.68f)
                        canvas.drawRoundRect(rect, dp(context, 3).toFloat(), dp(context, 3).toFloat(), strokePaint)
                        path.reset()
                        val px = rect.centerX()
                        val py = rect.centerY()
                        val pr = r * 0.28f
                        path.moveTo(px - pr * 0.7f, py - pr)
                        path.lineTo(px + pr, py)
                        path.lineTo(px - pr * 0.7f, py + pr)
                        path.close()
                        canvas.drawPath(path, fillPaint)
                    }
                    "⇅" -> {
                        // Dual opposing orientation arrows
                        val dx = r * 0.46f
                        val topY = cy - r * 0.92f
                        val botY = cy + r * 0.92f
                        val aw = r * 0.38f
                        path.reset()
                        path.moveTo(cx - dx, botY); path.lineTo(cx - dx, topY)
                        path.moveTo(cx - dx - aw, topY + aw); path.lineTo(cx - dx, topY); path.lineTo(cx - dx + aw, topY + aw)
                        path.moveTo(cx + dx, topY); path.lineTo(cx + dx, botY)
                        path.moveTo(cx + dx - aw, botY - aw); path.lineTo(cx + dx, botY); path.lineTo(cx + dx + aw, botY - aw)
                        canvas.drawPath(path, strokePaint)
                    }
                    "●" -> {
                        // Video Camcorder silhouette (body + side lens cone)
                        rect.set(cx - r * 1.02f, cy - r * 0.64f, cx + r * 0.36f, cy + r * 0.64f)
                        canvas.drawRoundRect(rect, dp(context, 3).toFloat(), dp(context, 3).toFloat(), strokePaint)
                        path.reset()
                        path.moveTo(cx + r * 0.38f, cy - r * 0.24f)
                        path.lineTo(cx + r * 1.02f, cy - r * 0.56f)
                        path.lineTo(cx + r * 1.02f, cy + r * 0.56f)
                        path.lineTo(cx + r * 0.38f, cy + r * 0.24f)
                        path.close()
                        canvas.drawPath(path, fillPaint)
                        canvas.drawCircle(rect.centerX(), rect.centerY(), r * 0.22f, fillPaint)
                    }
                    else -> {
                        // SLR Camera Body with top prism hump + center lens
                        rect.set(cx - r * 1.02f, cy - r * 0.55f, cx + r * 1.02f, cy + r * 0.78f)
                        canvas.drawRoundRect(rect, dp(context, 3).toFloat(), dp(context, 3).toFloat(), strokePaint)
                        path.reset()
                        path.moveTo(cx - r * 0.45f, cy - r * 0.55f)
                        path.lineTo(cx - r * 0.26f, cy - r * 0.88f)
                        path.lineTo(cx + r * 0.26f, cy - r * 0.88f)
                        path.lineTo(cx + r * 0.45f, cy - r * 0.55f)
                        canvas.drawPath(path, strokePaint)
                        canvas.drawCircle(cx, cy + r * 0.12f, r * 0.36f, fillPaint)
                    }
                }
            }
        }.apply {
            background = roundedDrawable(context, "#FFFFFF", 999)
            layoutParams = LinearLayout.LayoutParams(dp(context, 38), dp(context, 48)).apply { rightMargin = dp(context, 10) }
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

    fun createHealthBottomNavTab(context: Context, iconResId: Int, label: String): HealthBottomNavTabRefs {
        val rootTab = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(0, dp(context, 4), 0, dp(context, 4))
        }
        val iconPill = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(context, 64), dp(context, 32)).apply { bottomMargin = dp(context, 3) }
        }
        val iconView = ImageView(context).apply {
            setImageResource(iconResId)
            layoutParams = FrameLayout.LayoutParams(dp(context, 22), dp(context, 22), Gravity.CENTER)
        }
        iconPill.addView(iconView)
        val labelText = TextView(context).apply {
            text = label; gravity = Gravity.CENTER; setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        rootTab.addView(iconPill); rootTab.addView(labelText)
        return HealthBottomNavTabRefs(rootTab, iconPill, iconView, labelText)
    }

    fun styleHealthNavTab(context: Context, tab: HealthBottomNavTabRefs, active: Boolean) {
        tab.iconPill.background = roundedDrawable(context, if (active) ACTIVE_NAV_PILL else "#00000000", 999)
        tab.iconView.setColorFilter(Color.parseColor(if (active) BRAND_ORANGE_PRIMARY else TEXT_MUTED))
        tab.labelText.setTextColor(Color.parseColor(if (active) BRAND_ORANGE_PRIMARY else TEXT_MUTED))
        tab.labelText.typeface = Typeface.create("sans-serif-medium", if (active) Typeface.BOLD else Typeface.NORMAL)
    }

    fun showGooglePopupMenu(context: Context, anchor: View, options: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit) {
        try {
            val popup = PopupMenu(context, anchor)
            options.forEachIndexed { idx, title -> popup.menu.add(0, idx, idx, "${if (idx == selectedIndex) "✓  " else "     "}$title") }
            popup.setOnMenuItemClickListener { item -> onSelected(item.itemId); true }
            popup.show()
        } catch (_: Exception) { onSelected((selectedIndex + 1) % options.size) }
    }

    @Suppress("DEPRECATION")
    fun performClickHaptic(view: View) {
        try {
            view.isHapticFeedbackEnabled = true
            view.performHapticFeedback(
                HapticFeedbackConstants.KEYBOARD_TAP,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING or HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        } catch (_: Exception) {}
        try {
            val ctx = view.context
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val attrs = VibrationAttributes.Builder().setUsage(VibrationAttributes.USAGE_HARDWARE_FEEDBACK).build()
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK), attrs)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } else {
                    vibrator.vibrate(VibrationEffect.createOneShot(14L, 180))
                }
            }
        } catch (_: Exception) {}
    }

    fun roundedDrawable(context: Context, bgHex: String, radiusDp: Int, strokeHex: String? = null): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE; cornerRadius = dp(context, radiusDp).toFloat()
        setColor(Color.parseColor(bgHex))
        if (strokeHex != null) setStroke(dp(context, 1), Color.parseColor(strokeHex))
    }

    fun dp(context: Context, value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
}
