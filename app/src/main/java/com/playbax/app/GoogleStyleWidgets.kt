package com.playbax.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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

    fun roundedDrawable(context: Context, bgHex: String, radiusDp: Int, strokeHex: String? = null): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE; cornerRadius = dp(context, radiusDp).toFloat()
        setColor(Color.parseColor(bgHex))
        if (strokeHex != null) setStroke(dp(context, 1), Color.parseColor(strokeHex))
    }

    fun dp(context: Context, value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
}
