package com.playbax.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

class MainActivityViews(
    val rootFrame: FrameLayout,
    val multiScreenStage: PlayBaxMultiScreenStage,
    val statusDot: View,
    val statusText: TextView,
    val lensQuickChip: TextView,
    val layoutQuickChip: TextView,
    val minimizedTripodPill: LinearLayout,
    val minimizedPillText: TextView,
    val bottomDock: LinearLayout,
    val dragHandleContainer: LinearLayout,
    val readoutBadge: TextView,
    val minusBtn: TextView,
    val delaySlider: SeekBar,
    val plusBtn: TextView,
    val screenSpinnerChip: TextView,
    val recordSpinnerChip: TextView,
    val grantCameraBtn: TextView,
    val mainActionSection: LinearLayout,
    val startDelayBtn: TextView,
    val activeButtonsRow: LinearLayout,
    val tripodViewBtn: TextView,
    val resetBufferBtn: TextView,
    val pauseDelayBtn: TextView
)

object MainActivityUiBuilder {

    fun build(context: Context, stage: PlayBaxMultiScreenStage): MainActivityViews {
        val root = FrameLayout(context).apply { setBackgroundColor(Color.parseColor("#0E1013")) }
        root.addView(stage, FrameLayout.LayoutParams(-1, -1))

        // Top Floating Status & Camera/Layout Quick Chips
        val topBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(context, 14), dp(context, 44), dp(context, 14), dp(context, 10))
        }
        val statusPill = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(context, 12), dp(context, 8), dp(context, 12), dp(context, 8))
            background = pillDrawable("#D9131314")
        }
        val statusDot = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(context, 8), dp(context, 8)).apply { rightMargin = dp(context, 7) }
            background = pillDrawable(GoogleStyleWidgets.GOOGLE_GREEN)
        }
        val statusText = TextView(context).apply {
            text = "PlayBax Ready"
            setTextColor(Color.parseColor(GoogleStyleWidgets.M3_TEXT_PRIMARY))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        statusPill.addView(statusDot)
        statusPill.addView(statusText)

        val lensQuickChip = createTopQuickChip(context, "✓ 1.0x Rear ▾").apply {
            layoutParams = LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(context, 8) }
        }
        val layoutQuickChip = createTopQuickChip(context, "✓ Auto Layout ▾")

        topBar.addView(statusPill)
        topBar.addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 1f) })
        topBar.addView(lensQuickChip)
        topBar.addView(layoutQuickChip)
        root.addView(topBar, FrameLayout.LayoutParams(-1, -2, Gravity.TOP))

        // Minimized Tripod Floating Pill (When 3-Row Control Sheet is Collapsed)
        val minimizedTripodPill = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(context, 18), dp(context, 10), dp(context, 18), dp(context, 10))
            background = GoogleStyleWidgets.roundedCardDrawable(context, "#E6131314", 999, "#40A8C7FA")
            visibility = View.GONE
        }
        val minimizedPillText = TextView(context).apply {
            text = "▲  PlayBax (+5s) • Tap to Show Controls"
            setTextColor(Color.parseColor("#C2E7FF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        minimizedTripodPill.addView(minimizedPillText)
        val minLp = FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
            bottomMargin = dp(context, 22)
        }
        root.addView(minimizedTripodPill, minLp)

        // Ultra-Clean 3-Row Google Material 3 Dark Sheet (#131314)
        val bottomDock = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 20), dp(context, 8), dp(context, 20), dp(context, 24))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                val r = dp(context, 28).toFloat()
                cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
                setColor(Color.parseColor(GoogleStyleWidgets.M3_DARK_SHEET))
            }
        }
        val dragHandleContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(context, 4), 0, dp(context, 10))
            layoutParams = LinearLayout.LayoutParams(-1, -2)
        }
        dragHandleContainer.addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(context, 48), dp(context, 5))
            background = pillDrawable("#668E918F")
        })
        bottomDock.addView(dragHandleContainer)

        // Row 1: Minimalist Brand & Live Readout Header
        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(context, 12) }
        }
        val brandWordmark = GoogleStyleWidgets.createBrandWordmarkRow(context, 26, 18f).apply {
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        val readoutBadge = TextView(context).apply {
            text = "5s Delay • Memory"
            setTextColor(Color.parseColor(GoogleStyleWidgets.M3_ACCENT_LIGHT))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(dp(context, 12), dp(context, 6), dp(context, 12), dp(context, 6))
            background = pillDrawable("#1D2A3B")
        }
        headerRow.addView(brandWordmark)
        headerRow.addView(readoutBadge)
        bottomDock.addView(headerRow)

        // Row 2: [ − ] Thick Luminous Gradient Delay Slider (1s..30s) [ + ]
        val sliderCard = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10))
            background = GoogleStyleWidgets.roundedCardDrawable(context, GoogleStyleWidgets.M3_DARK_CARD, 20)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(context, 12) }
        }
        val minusBtn = GoogleStyleWidgets.createStepperButton(context, "−", sizeDp = 38) {}
        val delaySlider = SeekBar(context).apply {
            max = PlayBaxPreferences.MAX_DELAY_SECONDS - PlayBaxPreferences.MIN_DELAY_SECONDS
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply {
                leftMargin = dp(context, 6); rightMargin = dp(context, 6)
            }
        }
        GoogleStyleWidgets.styleThickGoogleSlider(context, delaySlider)
        val plusBtn = GoogleStyleWidgets.createStepperButton(context, "+", sizeDp = 38) {}
        sliderCard.addView(minusBtn)
        sliderCard.addView(delaySlider)
        sliderCard.addView(plusBtn)
        bottomDock.addView(sliderCard)

        // Row 3: Two Tonal Dropdown Spinners (Screens Cascade & Recording Mode)
        val spinnerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(context, 14) }
        }
        val screenSpinnerChip = GoogleStyleWidgets.createGoogleSpinnerChip(context, "1 Screen (+5s)") {}
        val recordSpinnerChip = GoogleStyleWidgets.createGoogleSpinnerChip(context, "Memory Only") {}
        spinnerRow.addView(screenSpinnerChip, weightLp(context, 1.15f, 10, heightDp = 40))
        spinnerRow.addView(recordSpinnerChip, weightLp(context, 0.85f, 0, heightDp = 40))
        bottomDock.addView(spinnerRow)

        val grantCameraBtn = createGradientActionPill(context, "Allow Camera Access").apply {
            visibility = View.GONE
        }
        bottomDock.addView(grantCameraBtn)

        val mainActionSection = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val startDelayBtn = createGradientActionPill(context, "Start Live Delayed Playback")
        val activeButtonsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            visibility = View.GONE
        }
        val tripodViewBtn = createGradientActionPill(context, "Tripod Fullscreen")
        val resetBufferBtn = createSolidActionPill(context, "Reset", "#1E2A3A", "#A8C7FA")
        val pauseDelayBtn = createSolidActionPill(context, "Pause", "#3B1F23", "#F2B8B5")
        activeButtonsRow.addView(tripodViewBtn, weightLp(context, 1.35f, 8, heightDp = 48))
        activeButtonsRow.addView(resetBufferBtn, weightLp(context, 0.65f, 8, heightDp = 48))
        activeButtonsRow.addView(pauseDelayBtn, weightLp(context, 0.65f, 0, heightDp = 48))

        mainActionSection.addView(startDelayBtn)
        mainActionSection.addView(activeButtonsRow)
        bottomDock.addView(mainActionSection)

        root.addView(bottomDock, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))

        return MainActivityViews(
            root, stage, statusDot, statusText, lensQuickChip, layoutQuickChip,
            minimizedTripodPill, minimizedPillText, bottomDock, dragHandleContainer,
            readoutBadge, minusBtn, delaySlider, plusBtn, screenSpinnerChip,
            recordSpinnerChip, grantCameraBtn, mainActionSection, startDelayBtn,
            activeButtonsRow, tripodViewBtn, resetBufferBtn, pauseDelayBtn
        )
    }

    private fun createTopQuickChip(context: Context, textLabel: String): TextView {
        return TextView(context).apply {
            text = textLabel
            setTextColor(Color.parseColor("#C2E7FF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(dp(context, 12), dp(context, 8), dp(context, 12), dp(context, 8))
            background = GoogleStyleWidgets.roundedCardDrawable(context, "#D9131314", 999, "#33A8C7FA")
        }
    }

    fun pillDrawable(colorHex: String): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 999f
            setColor(Color.parseColor(colorHex))
        }
    }

    private fun createGradientActionPill(context: Context, label: String): TextView {
        return TextView(context).apply {
            text = label
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            background = GoogleStyleWidgets.gradientPillDrawable()
            layoutParams = LinearLayout.LayoutParams(-1, dp(context, 48))
        }
    }

    private fun createSolidActionPill(
        context: Context,
        label: String,
        bgHex: String,
        fgHex: String,
        heightDp: Int = 48
    ): TextView {
        return TextView(context).apply {
            text = label
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor(fgHex))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            background = pillDrawable(bgHex)
            layoutParams = LinearLayout.LayoutParams(-1, dp(context, heightDp))
        }
    }

    private fun weightLp(context: Context, weight: Float, rightMarginDp: Int, heightDp: Int = -2): LinearLayout.LayoutParams {
        val h = if (heightDp > 0) dp(context, heightDp) else heightDp
        return LinearLayout.LayoutParams(0, h, weight).apply { rightMargin = dp(context, rightMarginDp) }
    }

    private fun dp(context: Context, value: Int): Int = GoogleStyleWidgets.dp(context, value)
}
