package com.playbax.app

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * Builds the slide-open Google Health style User Menu sheet (matching Screenshot 3)
 * with top identity card, standalone action pills, and segmented option groups.
 */
object PlayBaxUserMenuSheet {

    private const val MENU_BG = "#FCEAE6"
    private const val ICON_TINT = "#7A3E34"
    private const val TEXT_MAIN = "#2B1612"

    fun buildUserMenuOverlay(
        context: Context,
        onClose: () -> Unit,
        onStartCamera: () -> Unit,
        onOpenClips: () -> Unit,
        onResetSettings: () -> Unit
    ): ScrollView {
        val scroll = ScrollView(context).apply {
            isFillViewport = true
            visibility = View.GONE
            setBackgroundColor(Color.parseColor(MENU_BG))
        }
        val col = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 18), dp(context, 42), dp(context, 18), dp(context, 32))
        }

        // Top-right close "✕" row
        val closeRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(context, 8) }
        }
        val closeBtn = TextView(context).apply {
            text = "✕"
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor(ICON_TINT))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setPadding(dp(context, 12), dp(context, 6), dp(context, 12), dp(context, 6))
            setOnClickListener { onClose() }
        }
        closeRow.addView(closeBtn)
        col.addView(closeRow)

        // Top Profile / App Identity Card (matching Screenshot 3 top card)
        val profileCard = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(context, 18), dp(context, 18), dp(context, 18), dp(context, 18))
            background = GoogleStyleWidgets.roundedDrawable(context, "#FFFFFF", 28)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(context, 14) }
        }
        val avatarCircle = TextView(context).apply {
            text = "PB"
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GoogleStyleWidgets.roundedDrawable(context, GoogleStyleWidgets.BRAND_CRIMSON, 999, "#FF6D00")
            layoutParams = LinearLayout.LayoutParams(dp(context, 64), dp(context, 64)).apply { rightMargin = dp(context, 16) }
        }
        val profileTextCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        val nameTv = TextView(context).apply {
            text = "PlayBax Studio"
            setTextColor(Color.parseColor(TEXT_MAIN))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }
        val subTv = TextView(context).apply {
            text = "Live Delayed Video Coach"
            setTextColor(Color.parseColor(ICON_TINT))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
            setPadding(0, dp(context, 2), 0, dp(context, 6))
        }
        val proPill = TextView(context).apply {
            text = "1080p HD • OIS+EIS"
            setTextColor(Color.parseColor(TEXT_MAIN))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(context, 10), dp(context, 3), dp(context, 10), dp(context, 3))
            background = GoogleStyleWidgets.roundedDrawable(context, "#FFE0D6", 999, "#FF6D00")
            layoutParams = LinearLayout.LayoutParams(-2, -2)
        }
        profileTextCol.addView(nameTv)
        profileTextCol.addView(subTv)
        profileTextCol.addView(proPill)
        profileCard.addView(avatarCircle)
        profileCard.addView(profileTextCol)
        col.addView(profileCard)

        // Standalone Action Pills
        col.addView(createStandalonePillRow(context, "▶", "Start Live Delayed Camera") {
            onClose(); onStartCamera()
        })
        col.addView(createStandalonePillRow(context, "🎬", "Open Saved Clips Library") {
            onClose(); onOpenClips()
        })

        // Segmented Group 1: Studio Features
        val group1 = createSegmentedGroupCard(context)
        group1.addView(createSegmentedRow(context, "⏱", "Delay & screen presets") {
            onResetSettings(); onClose()
        })
        group1.addView(createGroupDivider(context))
        group1.addView(createSegmentedRow(context, "📷", "Camera hardware pipeline") {
            showInfoDialog(
                context,
                "Camera Hardware Pipeline",
                "• Native 16:9 Full-HD (1920×1080) sensor stream\n• Optical & Electronic Anti-Shake (OIS + EIS)\n• Continuous Video Autofocus & 30fps Indoor Lock\n• Pinch-to-Zoom & Tap-to-Focus on live playback"
            )
        })
        group1.addView(createGroupDivider(context))
        group1.addView(createSegmentedRow(context, "🔄", "Multi-screen stage modes") {
            showInfoDialog(
                context,
                "Multi-Screen Stage Modes",
                "• 1 Screen (+1×D full-screen delayed stream)\n• 2 Screens (+1×D and +2×D cascading delay)\n• 3 Screens (+1×D, +2×D, and +3×D cascading delay)\n• Automatically flips between Portrait Stacked and Landscape Side-by-Side."
            )
        })
        col.addView(group1)

        // Segmented Group 2: Data, Settings, Help & Feedback (matching Screenshot 3 bottom group)
        val group2 = createSegmentedGroupCard(context)
        group2.addView(createSegmentedRow(context, "🛡", "Your data in PlayBax") {
            showInfoDialog(
                context,
                "Your Data & Privacy in PlayBax",
                "• 100% On-Device RAM Processing: Delayed video frames are held strictly in volatile device RAM (up to 192 MB) and automatically pruned.\n• Manual Recording Only: MP4 video is saved to Movies/PlayBax ONLY when you explicitly tap ● Rec during playback.\n• Zero Cloud Uploads: No video or personal data ever leaves your device."
            )
        })
        group2.addView(createGroupDivider(context))
        group2.addView(createSegmentedRow(context, "⚙", "PlayBax settings") {
            onResetSettings()
            Toast.makeText(context, "All PlayBax settings restored to defaults", Toast.LENGTH_SHORT).show()
        })
        group2.addView(createGroupDivider(context))
        group2.addView(createSegmentedRow(context, "❓", "Help & Tripod guide") {
            PlayBaxClipsHelper.showGuideAndPrivacyDialog(context)
        })
        group2.addView(createGroupDivider(context))
        group2.addView(createSegmentedRow(context, "💬", "Feedback") {
            showInfoDialog(
                context,
                "PlayBax v1.0.6",
                "PlayBax Athletic Video Delay Coach (v1.0.6).\nThank you for testing PlayBax!"
            )
        })
        col.addView(group2)

        scroll.addView(col)
        return scroll
    }

    private fun createStandalonePillRow(context: Context, icon: String, title: String, onClick: () -> Unit): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(context, 18), dp(context, 15), dp(context, 18), dp(context, 15))
            background = GoogleStyleWidgets.roundedDrawable(context, "#FFFFFF", 999)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(context, 10) }
            setOnClickListener { onClick() }
            addView(createRowIcon(context, icon))
            addView(createRowTitle(context, title))
        }
    }

    private fun createSegmentedGroupCard(context: Context): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GoogleStyleWidgets.roundedDrawable(context, "#FFFFFF", 24)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(context, 14) }
        }
    }

    private fun createSegmentedRow(context: Context, icon: String, title: String, onClick: () -> Unit): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(context, 18), dp(context, 16), dp(context, 18), dp(context, 16))
            setOnClickListener { onClick() }
            addView(createRowIcon(context, icon))
            addView(createRowTitle(context, title))
        }
    }

    private fun createRowIcon(context: Context, icon: String): TextView = TextView(context).apply {
        text = icon; gravity = Gravity.CENTER
        setTextColor(Color.parseColor(ICON_TINT))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
        layoutParams = LinearLayout.LayoutParams(dp(context, 28), -2).apply { rightMargin = dp(context, 14) }
    }

    private fun createRowTitle(context: Context, title: String): TextView = TextView(context).apply {
        text = title
        setTextColor(Color.parseColor(TEXT_MAIN))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f)
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    private fun createGroupDivider(context: Context): View = View(context).apply {
        setBackgroundColor(Color.parseColor(MENU_BG))
        layoutParams = LinearLayout.LayoutParams(-1, dp(context, 2))
    }

    private fun showInfoDialog(context: Context, title: String, msg: String) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun dp(context: Context, value: Int): Int = GoogleStyleWidgets.dp(context, value)
}
