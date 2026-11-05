package com.playbax.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.SeekBar
import android.widget.TextView

/**
 * Shared Google Material 3 Dark + Soft Luminous Gradient UI components for PlayBax.
 */
object GoogleStyleWidgets {

    const val M3_DARK_SHEET = "#F5131314"
    const val M3_DARK_CARD = "#1E1F23"
    const val M3_DARK_TRACK = "#2C3038"
    const val M3_TONAL_BTN_BG = "#232F42"
    const val M3_SPINNER_BG = "#1D2A3B"
    const val M3_SPINNER_BORDER = "#33A8C7FA"
    const val M3_ACCENT_LIGHT = "#A8C7FA"
    const val M3_TEXT_PRIMARY = "#E3E3E3"
    const val GOOGLE_GREEN = "#81C995"
    const val GOOGLE_RED = "#F28B82"
    const val GOOGLE_YELLOW = "#FDD663"

    fun createBrandWordmarkRow(
        context: Context,
        iconSizeDp: Int = 26,
        titleSp: Float = 18f
    ): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val logo = ImageView(context).apply {
                setImageResource(R.drawable.ic_playbax_launcher)
                layoutParams = LinearLayout.LayoutParams(dp(context, iconSizeDp), dp(context, iconSizeDp)).apply {
                    rightMargin = dp(context, 10)
                }
            }
            val brandTitle = TextView(context).apply {
                text = "PlayBax"
                setTextColor(Color.parseColor(M3_TEXT_PRIMARY))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, titleSp)
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                letterSpacing = 0.01f
            }
            addView(logo)
            addView(brandTitle)
        }
    }

    fun styleThickGoogleSlider(context: Context, seekBar: SeekBar) {
        val trackHeight = dp(context, 12)
        val radius = dp(context, 999).toFloat()

        val bgTrack = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(Color.parseColor(M3_DARK_TRACK))
            setSize(-1, trackHeight)
        }

        val activeProgressRect = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(
                Color.parseColor("#1A73E8"),
                Color.parseColor("#4285F4"),
                Color.parseColor("#A8C7FA")
            )
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setSize(-1, trackHeight)
        }
        val progressClip = ClipDrawable(activeProgressRect, Gravity.START, ClipDrawable.HORIZONTAL)

        val layerDrawable = LayerDrawable(arrayOf(bgTrack, progressClip)).apply {
            setId(0, android.R.id.background)
            setId(1, android.R.id.progress)
        }

        val thumbSize = dp(context, 24)
        val thumbDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setSize(thumbSize, thumbSize)
            setColor(Color.parseColor(M3_ACCENT_LIGHT))
            setStroke(dp(context, 3), Color.parseColor("#131314"))
        }

        seekBar.progressDrawable = layerDrawable
        seekBar.thumb = thumbDrawable
        seekBar.splitTrack = false
        seekBar.setPadding(dp(context, 12), dp(context, 6), dp(context, 12), dp(context, 6))
    }

    fun createStepperButton(
        context: Context,
        symbol: String,
        sizeDp: Int = 38,
        onClick: () -> Unit
    ): TextView {
        return TextView(context).apply {
            text = symbol
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor(M3_ACCENT_LIGHT))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor(M3_TONAL_BTN_BG))
            }
            layoutParams = LinearLayout.LayoutParams(dp(context, sizeDp), dp(context, sizeDp))
            setOnClickListener { onClick() }
        }
    }

    fun createGoogleSpinnerChip(
        context: Context,
        label: String,
        heightDp: Int = 40,
        textSp: Float = 12.5f,
        onClick: (View) -> Unit
    ): TextView {
        return TextView(context).apply {
            text = formatSpinnerText(label)
            gravity = Gravity.CENTER
            setPadding(dp(context, 10), 0, dp(context, 10), 0)
            setTextColor(Color.parseColor("#C2E7FF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, textSp)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(context, 12).toFloat()
                setColor(Color.parseColor(M3_SPINNER_BG))
                setStroke(dp(context, 1), Color.parseColor(M3_SPINNER_BORDER))
            }
            layoutParams = LinearLayout.LayoutParams(0, dp(context, heightDp), 1f)
            setOnClickListener { onClick(this) }
        }
    }

    fun formatSpinnerText(label: String): String = "✓  $label  ▾"

    fun showGooglePopupMenu(
        context: Context,
        anchor: View,
        options: List<String>,
        selectedIndex: Int,
        onSelected: (Int) -> Unit
    ) {
        try {
            val popup = PopupMenu(context, anchor)
            options.forEachIndexed { idx, title ->
                val prefix = if (idx == selectedIndex) "✓  " else "     "
                popup.menu.add(0, idx, idx, "$prefix$title")
            }
            popup.setOnMenuItemClickListener { item ->
                onSelected(item.itemId)
                true
            }
            popup.show()
        } catch (_: Exception) {
            val next = (selectedIndex + 1) % options.size
            onSelected(next)
        }
    }

    fun gradientPillDrawable(): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(
                Color.parseColor("#1A73E8"),
                Color.parseColor("#4285F4"),
                Color.parseColor("#669DF6")
            )
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 999f
        }
    }

    fun roundedCardDrawable(
        context: Context,
        bgHex: String,
        radiusDp: Int = 20,
        strokeHex: String? = null
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(context, radiusDp).toFloat()
            setColor(Color.parseColor(bgHex))
            if (strokeHex != null) {
                setStroke(dp(context, 1), Color.parseColor(strokeHex))
            }
        }
    }

    fun dp(context: Context, value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }
}
