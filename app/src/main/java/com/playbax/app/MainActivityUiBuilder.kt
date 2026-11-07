package com.playbax.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivityViews(
    val rootFrame: FrameLayout,
    val homeContainer: LinearLayout,
    val badgeCircle: View,
    val setupBodyCol: LinearLayout,
    val clipsBodyCol: LinearLayout,
    val delayRingGauge: DelayRingGaugeView,
    val screensTile: PastelBentoTileRefs,
    val layoutTile: PastelBentoTileRefs,
    val captureTile: PastelBentoTileRefs,
    val lensTile: PastelBentoTileRefs,
    val resetDefaultsBtn: TextView,
    val applyLaunchBtn: TextView,
    val navSetupTab: HealthBottomNavTabRefs,
    val navClipsTab: HealthBottomNavTabRefs,
    val cameraStageContainer: FrameLayout,
    val multiScreenStage: PlayBaxMultiScreenStage,
    val lowVisControlPill: LinearLayout,
    val recordActionBtn: TextView,
    val freezeToggleBtn: TextView,
    val stopToHomeBtn: TextView,
    val splashOverlay: PlayBaxSplashView
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
            setPadding(dp(context, 16), dp(context, 44), dp(context, 16), dp(context, 18))
        }

        val topHeader = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(context, 14) }
        }
        val badgeCircle = ImageView(context).apply {
            setImageResource(R.drawable.ic_menu_hamburger)
            setPadding(dp(context, 8), dp(context, 8), dp(context, 8), dp(context, 8))
            background = GoogleStyleWidgets.roundedDrawable(context, GoogleStyleWidgets.SOFT_PEACH_PILL, 999)
            layoutParams = LinearLayout.LayoutParams(dp(context, 38), dp(context, 38))
        }
        topHeader.addView(GoogleStyleWidgets.createBrandHeaderRow(context))
        topHeader.addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 1f) })
        topHeader.addView(badgeCircle)
        homeCol.addView(topHeader)

        val setupBodyCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val clipsBodyCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }

        // Top Centered Interactive 360° Rotary Finger Dial (1s–30s with detent clicks & haptics)
        val delayRingGauge = DelayRingGaugeView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(context, 292), dp(context, 292)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(context, 2)
                bottomMargin = dp(context, 14)
            }
        }
        setupBodyCol.addView(delayRingGauge)

        // Symmetric 2x2 Bento Grid Below the Dial (Strict 1f : 1f 50%/50% width, 68dp card height, 10dp center gutter)
        val bentoRow1 = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(context, 10) }
        }
        val screensTile = GoogleStyleWidgets.createPastelBentoTile(
            context, "#FF9E80", "▣", "#FF3D00", "Playback screens", "1 Screen", "+5s"
        ) {}
        val layoutTile = GoogleStyleWidgets.createPastelBentoTile(
            context, "#EEF1F6", "⇅", "#1B1B1F", "Screen layout", "Auto (Stack)", "Portrait / Landscape"
        ) {}
        bentoRow1.addView(screensTile.card, LinearLayout.LayoutParams(0, dp(context, 68), 1f).apply { rightMargin = dp(context, 5) })
        bentoRow1.addView(layoutTile.card, LinearLayout.LayoutParams(0, dp(context, 68), 1f).apply { leftMargin = dp(context, 5) })
        setupBodyCol.addView(bentoRow1)

        val bentoRow2 = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(context, 18) }
        }
        val captureTile = GoogleStyleWidgets.createPastelBentoTile(
            context, "#FCE4EC", "●", "#FF1744", "Capture mode", "Memory Only", "Unrecorded RAM"
        ) {}
        val lensTile = GoogleStyleWidgets.createPastelBentoTile(
            context, "#FF9E80", "◎", "#FF3D00", "Camera lens", "1.0x Rear", "Tap to switch lens"
        ) {}
        bentoRow2.addView(captureTile.card, LinearLayout.LayoutParams(0, dp(context, 68), 1f).apply { rightMargin = dp(context, 5) })
        bentoRow2.addView(lensTile.card, LinearLayout.LayoutParams(0, dp(context, 68), 1f).apply { leftMargin = dp(context, 5) })
        setupBodyCol.addView(bentoRow2)

        // Symmetric 50% / 50% Action Buttons
        val actionRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, -2)
        }
        val resetDefaultsBtn = createPillButton(context, "↺  Reset", GoogleStyleWidgets.SOFT_PEACH_PILL, "#B71C1C", 52)
        val applyLaunchBtn = createPillButton(context, "▶  Start Camera", GoogleStyleWidgets.BRAND_ORANGE_PRIMARY, "#FFFFFF", 52)
        actionRow.addView(resetDefaultsBtn, LinearLayout.LayoutParams(0, dp(context, 52), 1f).apply { rightMargin = dp(context, 5) })
        actionRow.addView(applyLaunchBtn, LinearLayout.LayoutParams(0, dp(context, 52), 1f).apply { leftMargin = dp(context, 5) })
        setupBodyCol.addView(actionRow)

        homeCol.addView(setupBodyCol)
        homeCol.addView(clipsBodyCol)
        homeScroll.addView(homeCol)
        homeContainer.addView(homeScroll)

        // Google Health Style Bottom Navigation Bar (Tonal Background + Top 64x32dp Icon Pill + Bottom 12sp Label)
        val navDivider = View(context).apply {
            setBackgroundColor(Color.parseColor("#E6D6D0"))
            layoutParams = LinearLayout.LayoutParams(-1, dp(context, 1))
        }
        val bottomNavBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(context, 24), dp(context, 8), dp(context, 24), dp(context, 10))
            setBackgroundColor(Color.parseColor(GoogleStyleWidgets.BOTTOM_NAV_BG))
        }
        val navSetupTab = GoogleStyleWidgets.createHealthBottomNavTab(context, R.drawable.ic_nav_home, "Home")
        val navClipsTab = GoogleStyleWidgets.createHealthBottomNavTab(context, R.drawable.ic_nav_clips, "Saved Clips")
        bottomNavBar.addView(navSetupTab.rootTab, LinearLayout.LayoutParams(0, -2, 1f))
        bottomNavBar.addView(navClipsTab.rootTab, LinearLayout.LayoutParams(0, -2, 1f))
        homeContainer.addView(navDivider)
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

        val splashOverlay = PlayBaxSplashView(context).apply { visibility = View.GONE }

        return MainActivityViews(
            root, homeContainer, badgeCircle, setupBodyCol, clipsBodyCol,
            delayRingGauge, screensTile, layoutTile, captureTile, lensTile,
            resetDefaultsBtn, applyLaunchBtn, navSetupTab, navClipsTab,
            cameraStageContainer, stage, lowVisControlPill, recordActionBtn,
            freezeToggleBtn, stopToHomeBtn, splashOverlay
        )
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

    private fun dp(context: Context, value: Int): Int = GoogleStyleWidgets.dp(context, value)
}
