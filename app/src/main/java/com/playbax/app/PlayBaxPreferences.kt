package com.playbax.app

import android.content.Context
import android.content.Intent

object PlayBaxPreferences {
    private const val PREFS_NAME = "playbax_prefs"
    const val ACTION_PREFS_CHANGED = "com.playbax.app.ACTION_PREFS_CHANGED"

    private const val KEY_DELAY_SECONDS = "delay_seconds"
    private const val KEY_SCREEN_COUNT = "screen_count"
    private const val KEY_LAYOUT_MODE = "layout_mode"
    private const val KEY_RECORD_ENABLED = "record_enabled"
    private const val KEY_LENS_MODE = "lens_mode"
    private const val KEY_CONTROLS_MINIMIZED = "controls_minimized"

    const val MIN_DELAY_SECONDS = 1
    const val MAX_DELAY_SECONDS = 30
    const val DEFAULT_DELAY_SECONDS = 5

    const val LAYOUT_AUTO = 0
    const val LAYOUT_PORTRAIT_STACKED = 1
    const val LAYOUT_LANDSCAPE_SIDE = 2

    const val LENS_REAR_WIDE = 0
    const val LENS_REAR_ULTRA = 1
    const val LENS_FRONT_MIRROR = 2

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDelaySeconds(context: Context): Int =
        prefs(context).getInt(KEY_DELAY_SECONDS, DEFAULT_DELAY_SECONDS)
            .coerceIn(MIN_DELAY_SECONDS, MAX_DELAY_SECONDS)

    fun setDelaySeconds(context: Context, seconds: Int) {
        val clamped = seconds.coerceIn(MIN_DELAY_SECONDS, MAX_DELAY_SECONDS)
        prefs(context).edit().putInt(KEY_DELAY_SECONDS, clamped).apply()
        notifyChanged(context)
    }

    fun getScreenCount(context: Context): Int =
        prefs(context).getInt(KEY_SCREEN_COUNT, 1).coerceIn(1, 3)

    fun setScreenCount(context: Context, count: Int) {
        val clamped = count.coerceIn(1, 3)
        prefs(context).edit().putInt(KEY_SCREEN_COUNT, clamped).apply()
        notifyChanged(context)
    }

    fun getLayoutMode(context: Context): Int =
        prefs(context).getInt(KEY_LAYOUT_MODE, LAYOUT_AUTO).coerceIn(0, 2)

    fun setLayoutMode(context: Context, mode: Int) {
        val clamped = mode.coerceIn(0, 2)
        prefs(context).edit().putInt(KEY_LAYOUT_MODE, clamped).apply()
        notifyChanged(context)
    }

    fun isRecordEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_RECORD_ENABLED, false)

    fun setRecordEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_RECORD_ENABLED, enabled).apply()
        notifyChanged(context)
    }

    fun getLensMode(context: Context): Int =
        prefs(context).getInt(KEY_LENS_MODE, LENS_REAR_WIDE).coerceIn(0, 2)

    fun setLensMode(context: Context, mode: Int) {
        val clamped = mode.coerceIn(0, 2)
        prefs(context).edit().putInt(KEY_LENS_MODE, clamped).apply()
        notifyChanged(context)
    }

    fun isControlsMinimized(context: Context): Boolean =
        prefs(context).getBoolean(KEY_CONTROLS_MINIMIZED, false)

    fun setControlsMinimized(context: Context, minimized: Boolean) {
        prefs(context).edit().putBoolean(KEY_CONTROLS_MINIMIZED, minimized).apply()
        notifyChanged(context)
    }

    fun getScreenSpinnerLabel(screenCount: Int, delaySec: Int): String {
        return when (screenCount.coerceIn(1, 3)) {
            1 -> "1 Screen (+${delaySec}s)"
            2 -> "2 Screens (+${delaySec}s • +${delaySec * 2}s)"
            else -> "3 Screens (+${delaySec}s • +${delaySec * 2}s • +${delaySec * 3}s)"
        }
    }

    fun getScreenMenuOptions(delaySec: Int): List<String> = listOf(
        "1 Screen (+${delaySec}s Full)",
        "2 Screens (+${delaySec}s & +${delaySec * 2}s)",
        "3 Screens (+${delaySec}s, +${delaySec * 2}s, +${delaySec * 3}s)"
    )

    fun getRecordSpinnerLabel(recordEnabled: Boolean): String =
        if (recordEnabled) "Record MP4 On" else "Memory Only"

    fun getRecordMenuOptions(): List<String> = listOf(
        "Memory Only (Unrecorded Stream)",
        "Record MP4 to Device Gallery"
    )

    fun getLayoutChipLabel(layoutMode: Int, isDeviceLandscape: Boolean): String {
        return when (layoutMode) {
            LAYOUT_PORTRAIT_STACKED -> "Stacked"
            LAYOUT_LANDSCAPE_SIDE -> "Side-by-Side"
            else -> if (isDeviceLandscape) "Auto (Side)" else "Auto (Stack)"
        }
    }

    fun getLayoutMenuOptions(): List<String> = listOf(
        "Auto (Portrait Stacked / Landscape Side)",
        "Always Portrait Stacked (Vertical)",
        "Always Side-by-Side (Horizontal)"
    )

    fun getLensChipLabel(lensMode: Int): String {
        return when (lensMode) {
            LENS_REAR_ULTRA -> "0.5x Wide"
            LENS_FRONT_MIRROR -> "Front Cam"
            else -> "1.0x Rear"
        }
    }

    fun getLensMenuOptions(): List<String> = listOf(
        "1.0x Standard Rear Camera",
        "0.5x Ultra-Wide Rear Camera",
        "Front Mirrored Self-Coach Camera"
    )

    private fun notifyChanged(context: Context) {
        val intent = Intent(ACTION_PREFS_CHANGED).apply {
            setPackage(context.packageName)
        }
        context.sendBroadcast(intent)
    }
}
