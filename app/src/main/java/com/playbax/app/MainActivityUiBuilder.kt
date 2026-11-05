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
    val homeContainer: LinearLayout,
    val topModePill: TextView,
    val badgeCircle: TextView,
    val setupBodyCol: LinearLayout,
    val clipsBodyCol: LinearLayout,
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
    val navSetupTab: TextView,
    val navClipsTab: TextView,
    val navGuideTab: TextView,
    val cameraStageContainer: FrameLayout,
    val multiScreenStage: PlayBaxMultiScreenStage,
    val lowVisControlPill: LinearLayout,
    val recordActionBtn: TextView,
    val freezeToggleBtn: TextView,
    val stopToHomeBtn: TextView
)

object MainActivityUiBuilder {

    fun build(context: Context, stage: PlayBaxMultiScreenStage): MainActivityViews {
        val root = FrameLayout(context).apply { setBackgroundColor(Color.parseColor(GoogleStyleWidgets.LIGHT_BG)) }
        val homeContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(GoogleStyleWidgets.LIGHT_BG))
        }
        val homeScroll = ScrollView(context).apply {
            isFillViewport = true
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        }
        val homeCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 18), dp(context, 44), dp(context, 18), dp(context, 20))
        }

        val topHeader = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(context, 16) }
        }
        val topModePill = TextView(context).apply {
            text = "1080p • OIS+EIS"
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
            background = GoogleStyleWidgets.roundedDrawable(context, GoogleStyleWidgets.BRAND_ORANGE_PRIMARY, 999, "#FF7043")
            layoutParams = LinearLayout.LayoutParams(dp(context, 36), dp(context, 36)).apply { leftMargin = dp(context, 10) }
        }
        topHeader.addView(GoogleStyleWidgets.createBrandHeaderRow(context))
        topHeader.addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 1f) })
        topHeader.addView(topModePill)
        topHeader.addView(badgeCircle)
        homeCol.addView(topHeader)

        val setupBodyCol = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val clipsBodyCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }

        val heroBentoRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(context, 10) }
        }
        val delayRingGauge = DelayRingGaugeView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, dp(context, 220), 0.94f).apply { rightMargin = dp(context, 10) }
        }
        val rightStack = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, -2, 1.06f)
        }
        val screensTile = GoogleStyleWidgets.createPastelBentoTile(context, "#FFDAD0", "▣", "#BF360C", "Playback screens", "1 Screen", "Tap to switch 1–3") {}
        val layoutTile = GoogleStyleWidgets.createPastelBentoTile(context, "#F5EBE7", "⇅", "#3E2723", "Screen layout", "Auto Rotate", "Stacked / Side") {}
        val captureTile = GoogleStyleWidgets.createPastelBentoTile(context, "#FCE4EC", "●", "#B71C1C", "Capture mode", "Memory Only", "Unrecorded RAM") {}

        rightStack.addView(screensTile.card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(context, 8) })
        rightStack.addView(layoutTile.card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(context, 8) })
        rightStack.addView(captureTile.card, LinearLayout.LayoutParams(-1, -2))
        heroBentoRow.addView(delayRingGauge)
        heroBentoRow.addView(rightStack)
        setupBodyCol.addView(heroBentoRow)

        val secondBentoRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(context, 12) }
        }
        val lensTile = GoogleStyleWidgets.createPastelBentoTile(context, "#FFF0E5", "◎", "#D84315", "Camera lens", "1.0x Rear", "Tap to switch lens") {}
        val cascadeTile = GoogleStyleWidgets.createPastelBentoTile(context, "#FFE4D6", "⏱", "#B71C1C", "Delay cascade", "+5s behind", "1–30s memory loop") {}
        secondBentoRow.addView(lensTile.card, weightLp(context, 1f, 8))
        secondBentoRow.addView(cascadeTile.card, weightLp(context, 1f, 0))
        setupBodyCol.addView(secondBentoRow)

        val sliderSection = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 16), dp(context, 12), dp(context, 16), dp(context, 12))
            background = GoogleStyleWidgets.roundedDrawable(context, "#FFFFFF", 24, "#F2E1DA")
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(context, 14) }
        }
        val sliderRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(context, 10) }
        }
        val minusBtn = GoogleStyleWidgets.createLightStepperButton(context, "−") {}
        val delaySlider = SeekBar(context).apply {
            max = PlayBaxPreferences.MAX_DELAY_SECONDS - PlayBaxPreferences.MIN_DELAY_SECONDS
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(context, 6); rightMargin = dp(context, 6) }
        }
        GoogleStyleWidgets.styleLightGoogleSlider(context, delaySlider)
        val plusBtn = GoogleStyleWidgets.createLightStepperButton(context, "+") {}
        sliderRow.addView(minusBtn); sliderRow.addView(delaySlider); sliderRow.addView(plusBtn)
        sliderSection.addView(sliderRow)

        val presetsRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        val presetValues = listOf(3, 5, 10, 15, 30)
        val presetButtons = presetValues.mapIndexed { idx, sec ->
            val btn = TextView(context).apply {
                text = "${sec}s"; gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(0, dp(context, 8), 0, dp(context, 8))
            }
            presetsRow.addView(btn, weightLp(context, 1f, if (idx < presetValues.size - 1) 6 else 0))
            sec to btn
        }
        sliderSection.addView(presetsRow)
        setupBodyCol.addView(sliderSection)

        val actionRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val resetDefaultsBtn = createPillButton(context, "↺  Reset", GoogleStyleWidgets.SOFT_PEACH_PILL, "#3E2723", 50)
        val applyLaunchBtn = createPillButton(context, "▶  Apply & Start Camera", GoogleStyleWidgets.BRAND_ORANGE_PRIMARY, "#FFFFFF", 50)
        actionRow.addView(resetDefaultsBtn, weightLp(context, 0.72f, 10, heightDp = 50))
        actionRow.addView(applyLaunchBtn, weightLp(context, 1.58f, 0, heightDp = 50))
        setupBodyCol.addView(actionRow)

        homeCol.addView(setupBodyCol)
        homeCol.addView(clipsBodyCol)
        homeScroll.addView(homeCol)
        homeContainer.addView(homeScroll)

        val bottomNavBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(context, 16), dp(context, 8), dp(context, 16), dp(context, 14))
            background = GoogleStyleWidgets.roundedDrawable(context, "#FFFFFF", 0, "#F2E1DA")
        }
        val navSetupTab = createBottomNavTab(context, "⚙  Setup")
        val navClipsTab = createBottomNavTab(context, "🎬  Saved Clips")
        val navGuideTab = createBottomNavTab(context, "ⓘ  Guide & Privacy")
        bottomNavBar.addView(navSetupTab, weightLp(context, 1f, 6))
        bottomNavBar.addView(navClipsTab, weightLp(context, 1f, 6))
        bottomNavBar.addView(navGuideTab, weightLp(context, 1f, 0))
        homeContainer.addView(bottomNavBar, LinearLayout.LayoutParams(-1, -2))
        root.addView(homeContainer, FrameLayout.LayoutParams(-1, -1))

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

        val pillLp = FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = dp(context, 20) }
        cameraStageContainer.addView(lowVisControlPill, pillLp)
        root.addView(cameraStageContainer, FrameLayout.LayoutParams(-1, -1))

        return MainActivityViews(
            root, homeContainer, topModePill, badgeCircle, setupBodyCol, clipsBodyCol,
            delayRingGauge, screensTile, layoutTile, captureTile, lensTile, cascadeTile,
            minusBtn, delaySlider, plusBtn, presetButtons, resetDefaultsBtn, applyLaunchBtn,
            navSetupTab, navClipsTab, navGuideTab, cameraStageContainer, stage,
            lowVisControlPill, recordActionBtn, freezeToggleBtn, stopToHomeBtn
        )
    }

    private fun createBottomNavTab(context: Context, label: String): TextView = TextView(context).apply {
        text = label; gravity = Gravity.CENTER
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        setPadding(dp(context, 8), dp(context, 9), dp(context, 8), dp(context, 9))
    }

    private fun createLowVisControlItem(context: Context, label: String): TextView = TextView(context).apply {
        text = label; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        setPadding(dp(context, 12), dp(context, 6), dp(context, 12), dp(context, 6))
    }

    private fun createHairlineDivider(context: Context): View = View(context).apply {
        setBackgroundColor(Color.parseColor("#4DFFFFFF"))
        layoutParams = LinearLayout.LayoutParams(dp(context, 1), dp(context, 14))
    }

    private fun createPillButton(context: Context, label: String, bgHex: String, fgHex: String, heightDp: Int): TextView = TextView(context).apply {
        text = label; gravity = Gravity.CENTER; setTextColor(Color.parseColor(fgHex))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        background = GoogleStyleWidgets.roundedDrawable(context, bgHex, 999)
        layoutParams = LinearLayout.LayoutParams(-1, dp(context, heightDp))
    }

    private fun weightLp(context: Context, weight: Float, rightMarginDp: Int, heightDp: Int = -2): LinearLayout.LayoutParams {
        val h = if (heightDp > 0) dp(context, heightDp) else heightDp
        return LinearLayout.LayoutParams(0, h, weight).apply { rightMargin = dp(context, rightMarginDp) }
    }

    private fun dp(context: Context, value: Int): Int = GoogleStyleWidgets.dp(context, value)
}
