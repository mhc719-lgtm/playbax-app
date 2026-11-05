package com.playbax.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView

class MainActivityViews(
    val rootFrame: FrameLayout,
    val homeSetupScroll: ScrollView,
    val topModePill: TextView,
    val delayRingGauge: DelayRingGaugeView,
    val screensTile: PastelBentoTileRefs,
    val layoutTile: PastelBentoTileRefs,
    val captureTile: PastelBentoTileRefs,
    val lensTile: PastelBentoTileRefs,
    val cascadeTile: PastelBentoTileRefs,
    val minusBtn: TextView,
    val delaySlider: SeekBar,
    val plusBtn: TextView,
    val presetButtons: List<Pair<Int, TextView>>,
    val resetDefaultsBtn: TextView,
    val applyLaunchBtn: TextView,
    val cameraStageContainer: FrameLayout,
    val multiScreenStage: PlayBaxMultiScreenStage,
    val lowVisControlPill: LinearLayout,
    val recordActionBtn: TextView,
    val freezeToggleBtn: TextView,
    val stopToHomeBtn: TextView
)

object MainActivityUiBuilder {

    fun build(context: Context, stage: PlayBaxMultiScreenStage): MainActivityViews {
        val root = FrameLayout(context).apply {
            setBackgroundColor(Color.parseColor(GoogleStyleWidgets.LIGHT_BG))
        }

        // =====================================================================
        // 1. Branded Google Light UI Home Setup Screen (Fitbit / Play Store Style)
        // =====================================================================
        val homeScroll = ScrollView(context).apply {
            isFillViewport = true
            setBackgroundColor(Color.parseColor(GoogleStyleWidgets.LIGHT_BG))
        }
        val homeCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 18), dp(context, 46), dp(context, 18), dp(context, 28))
        }

        val topHeader = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(context, 18) }
        }
        val brandRow = GoogleStyleWidgets.createBrandHeaderRow(context)
        val topModePill = TextView(context).apply {
            text = "Memory Stream"
            setTextColor(Color.parseColor(GoogleStyleWidgets.TEXT_DARK))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(dp(context, 14), dp(context, 7), dp(context, 14), dp(context, 7))
            background = GoogleStyleWidgets.roundedDrawable(context, GoogleStyleWidgets.LIGHT_SURFACE_PILL, 999)
        }
        val badgeCircle = TextView(context).apply {
            text = "PB"
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GoogleStyleWidgets.roundedDrawable(context, "#0B57D0", 999, "#26C6DA")
            layoutParams = LinearLayout.LayoutParams(dp(context, 36), dp(context, 36)).apply {
                leftMargin = dp(context, 10)
            }
        }
        topHeader.addView(brandRow)
        topHeader.addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 1f) })
        topHeader.addView(topModePill)
        topHeader.addView(badgeCircle)
        homeCol.addView(topHeader)

        val heroBentoRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(context, 10) }
        }
        val delayRingGauge = DelayRingGaugeView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, dp(context, 224), 0.94f).apply {
                rightMargin = dp(context, 10)
            }
        }
        val rightStack = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, -2, 1.06f)
        }
        val screensTile = GoogleStyleWidgets.createPastelBentoTile(
            context, "#9CF2E6", "▣", "#00796B", "Playback screens", "1 Screen", "Tap to switch 1–3"
        ) {}
        val layoutTile = GoogleStyleWidgets.createPastelBentoTile(
            context, "#E6EAEE", "⇅", "#1F1F1F", "Screen layout", "Auto Rotate", "Stacked / Side"
        ) {}
        val captureTile = GoogleStyleWidgets.createPastelBentoTile(
            context, "#F3E8FF", "●", "#6A1B9A", "Capture mode", "Memory Only", "Unrecorded RAM"
        ) {}

        rightStack.addView(screensTile.card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(context, 8) })
        rightStack.addView(layoutTile.card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(context, 8) })
        rightStack.addView(captureTile.card, LinearLayout.LayoutParams(-1, -2))

        heroBentoRow.addView(delayRingGauge)
        heroBentoRow.addView(rightStack)
        homeCol.addView(heroBentoRow)

        val secondBentoRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(context, 14) }
        }
        val lensTile = GoogleStyleWidgets.createPastelBentoTile(
            context, "#E6EAEE", "◎", "#0B57D0", "Camera lens", "1.0x Rear", "Tap to switch lens"
        ) {}
        val cascadeTile = GoogleStyleWidgets.createPastelBentoTile(
            context, "#ADEFE4", "⏱", "#00695C", "Delay cascade", "+5s behind", "1–30s memory loop"
        ) {}
        secondBentoRow.addView(lensTile.card, weightLp(context, 1f, 8))
        secondBentoRow.addView(cascadeTile.card, weightLp(context, 1f, 0))
        homeCol.addView(secondBentoRow)

        val sliderSection = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 16), dp(context, 14), dp(context, 16), dp(context, 14))
            background = GoogleStyleWidgets.roundedDrawable(context, "#FFFFFF", 24, "#E0E6ED")
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(context, 16) }
        }
        val sliderRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(context, 10) }
        }
        val minusBtn = GoogleStyleWidgets.createLightStepperButton(context, "−") {}
        val delaySlider = SeekBar(context).apply {
            max = PlayBaxPreferences.MAX_DELAY_SECONDS - PlayBaxPreferences.MIN_DELAY_SECONDS
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply {
                leftMargin = dp(context, 6); rightMargin = dp(context, 6)
            }
        }
        GoogleStyleWidgets.styleLightGoogleSlider(context, delaySlider)
        val plusBtn = GoogleStyleWidgets.createLightStepperButton(context, "+") {}
        sliderRow.addView(minusBtn)
        sliderRow.addView(delaySlider)
        sliderRow.addView(plusBtn)
        sliderSection.addView(sliderRow)

        val presetsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(-1, -2)
        }
        val presetValues = listOf(3, 5, 10, 15, 30)
        val presetButtons = presetValues.mapIndexed { idx, sec ->
            val btn = TextView(context).apply {
                text = "${sec}s"
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(0, dp(context, 8), 0, dp(context, 8))
            }
            val rightMargin = if (idx < presetValues.size - 1) 6 else 0
            presetsRow.addView(btn, weightLp(context, 1f, rightMargin))
            sec to btn
        }
        sliderSection.addView(presetsRow)
        homeCol.addView(sliderSection)

        val actionRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, -2)
        }
        val resetDefaultsBtn = createPillButton(context, "↺  Reset", GoogleStyleWidgets.SOFT_SKY_PILL, "#001D35", 52)
        val applyLaunchBtn = createPillButton(context, "▶  Apply & Start Camera", GoogleStyleWidgets.GOOGLE_BLUE_PRIMARY, "#FFFFFF", 52)
        actionRow.addView(resetDefaultsBtn, weightLp(context, 0.72f, 10, heightDp = 52))
        actionRow.addView(applyLaunchBtn, weightLp(context, 1.58f, 0, heightDp = 52))
        homeCol.addView(actionRow)

        homeScroll.addView(homeCol)
        root.addView(homeScroll, FrameLayout.LayoutParams(-1, -1))

        // =====================================================================
        // 2. Distraction-Free Live Delayed Playback Screen
        //    (Pure Video + Single Low-Visibility Record / Pause / Stop Pill)
        // =====================================================================
        val cameraStageContainer = FrameLayout(context).apply {
            visibility = View.GONE
            setBackgroundColor(Color.BLACK)
        }
        cameraStageContainer.addView(stage, FrameLayout.LayoutParams(-1, -1))

        val lowVisControlPill = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            alpha = 0.36f
            setPadding(dp(context, 8), dp(context, 5), dp(context, 8), dp(context, 5))
            background = GoogleStyleWidgets.roundedDrawable(context, "#66000000", 999, "#33FFFFFF")
        }
        val recordActionBtn = createLowVisControlItem(context, "● Rec")
        val freezeToggleBtn = createLowVisControlItem(context, "⏸ Pause")
        val stopToHomeBtn = createLowVisControlItem(context, "■ Stop")

        lowVisControlPill.addView(recordActionBtn)
        lowVisControlPill.addView(createHairlineDivider(context))
        lowVisControlPill.addView(freezeToggleBtn)
        lowVisControlPill.addView(createHairlineDivider(context))
        lowVisControlPill.addView(stopToHomeBtn)

        val pillLp = FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
            bottomMargin = dp(context, 20)
        }
        cameraStageContainer.addView(lowVisControlPill, pillLp)
        root.addView(cameraStageContainer, FrameLayout.LayoutParams(-1, -1))

        return MainActivityViews(
            root, homeScroll, topModePill, delayRingGauge, screensTile, layoutTile,
            captureTile, lensTile, cascadeTile, minusBtn, delaySlider, plusBtn,
            presetButtons, resetDefaultsBtn, applyLaunchBtn, cameraStageContainer,
            stage, lowVisControlPill, recordActionBtn, freezeToggleBtn, stopToHomeBtn
        )
    }

    private fun createLowVisControlItem(context: Context, label: String): TextView {
        return TextView(context).apply {
            text = label
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(dp(context, 12), dp(context, 6), dp(context, 12), dp(context, 6))
        }
    }

    private fun createHairlineDivider(context: Context): View {
        return View(context).apply {
            setBackgroundColor(Color.parseColor("#4DFFFFFF"))
            layoutParams = LinearLayout.LayoutParams(dp(context, 1), dp(context, 14))
        }
    }

    private fun createPillButton(
        context: Context,
        label: String,
        bgHex: String,
        fgHex: String,
        heightDp: Int
    ): TextView {
        return TextView(context).apply {
            text = label
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor(fgHex))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GoogleStyleWidgets.roundedDrawable(context, bgHex, 999)
            layoutParams = LinearLayout.LayoutParams(-1, dp(context, heightDp))
        }
    }

    private fun weightLp(context: Context, weight: Float, rightMarginDp: Int, heightDp: Int = -2): LinearLayout.LayoutParams {
        val h = if (heightDp > 0) dp(context, heightDp) else heightDp
        return LinearLayout.LayoutParams(0, h, weight).apply { rightMargin = dp(context, rightMarginDp) }
    }

    private fun dp(context: Context, value: Int): Int = GoogleStyleWidgets.dp(context, value)
}
