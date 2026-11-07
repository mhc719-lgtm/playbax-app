package com.playbax.app

import android.app.AlertDialog
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.text.InputType
import android.text.TextUtils
import android.text.format.DateFormat
import android.util.TypedValue
import android.view.Gravity
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.util.Date

class SavedClipItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val dateAddedSec: Long
)

object PlayBaxClipsHelper {

    private const val CLIP_NAMES_PREFS = "playbax_clip_names"

    fun querySavedClips(context: Context): List<SavedClipItem> {
        val list = ArrayList<SavedClipItem>()
        val namePrefs = context.getSharedPreferences(CLIP_NAMES_PREFS, Context.MODE_PRIVATE)
        try {
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_ADDED
            )
            val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ? OR ${MediaStore.Video.Media.DISPLAY_NAME} LIKE ?"
            } else {
                "${MediaStore.Video.Media.DISPLAY_NAME} LIKE ?"
            }
            val selectionArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                arrayOf("%Movies/PlayBax%", "PlayBax_%")
            } else {
                arrayOf("PlayBax_%")
            }
            val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                while (cursor.moveToNext() && list.size < 30) {
                    val id = cursor.getLong(idCol)
                    val mediaName = cursor.getString(nameCol) ?: "PlayBax_Clip.mp4"
                    val customName = namePrefs.getString(id.toString(), null)
                    val finalName = if (!customName.isNullOrBlank()) customName else mediaName
                    val size = cursor.getLong(sizeCol)
                    val dateSec = cursor.getLong(dateCol)
                    val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    list.add(SavedClipItem(id, uri, finalName, size, dateSec))
                }
            }
        } catch (_: Exception) {}
        return list
    }

    fun populateClipsContainer(
        context: Context,
        container: LinearLayout,
        onStartCameraClick: () -> Unit
    ) {
        container.removeAllViews()
        val clips = querySavedClips(context)

        val headerTv = TextView(context).apply {
            text = "Your PlayBax (${clips.size})"
            setTextColor(Color.parseColor(GoogleStyleWidgets.TEXT_DARK))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(context, 12) }
        }
        container.addView(headerTv)

        if (clips.isEmpty()) {
            val emptyCard = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(context, 24), dp(context, 32), dp(context, 24), dp(context, 32))
                background = GoogleStyleWidgets.roundedDrawable(context, "#FFFFFF", 24, "#F2E1DA")
            }
            val badge = TextView(context).apply {
                text = "▶"
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor(GoogleStyleWidgets.BRAND_ORANGE_PRIMARY))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
                background = GoogleStyleWidgets.roundedDrawable(context, GoogleStyleWidgets.SOFT_PEACH_PILL, 999)
                layoutParams = LinearLayout.LayoutParams(dp(context, 56), dp(context, 56)).apply { bottomMargin = dp(context, 12) }
            }
            val emptyTitle = TextView(context).apply {
                text = "No saved recordings yet"
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor(GoogleStyleWidgets.TEXT_DARK))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            }
            val emptyDesc = TextView(context).apply {
                text = "PlayBax streams in unrecorded RAM by default. Tap ● Rec on the live playback screen whenever you want to save a 1, 2, or 3-screen MP4 clip."
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor(GoogleStyleWidgets.TEXT_MUTED))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding(0, dp(context, 6), 0, dp(context, 16))
            }
            val launchBtn = TextView(context).apply {
                text = "▶  Open Live Playback"
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(dp(context, 22), dp(context, 12), dp(context, 22), dp(context, 12))
                background = GoogleStyleWidgets.roundedDrawable(context, GoogleStyleWidgets.BRAND_ORANGE_PRIMARY, 999)
                setOnClickListener { onStartCameraClick() }
            }
            emptyCard.addView(badge); emptyCard.addView(emptyTitle); emptyCard.addView(emptyDesc); emptyCard.addView(launchBtn)
            container.addView(emptyCard)
            return
        }

        clips.forEach { clip ->
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(context, 14), dp(context, 12), dp(context, 12), dp(context, 12))
                background = GoogleStyleWidgets.roundedDrawable(context, "#FFFFFF", 20, "#F2E1DA")
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(context, 10) }
            }
            val textCol = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(context, 8) }
            }
            val title = TextView(context).apply {
                text = clip.displayName
                isSingleLine = true
                ellipsize = TextUtils.TruncateAt.MIDDLE
                setTextColor(Color.parseColor(GoogleStyleWidgets.TEXT_DARK))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            }
            val mb = String.format("%.1f MB", clip.sizeBytes / (1024f * 1024f))
            val dateStr = DateFormat.format("MMM d, h:mm a", Date(clip.dateAddedSec * 1000L)).toString()
            val meta = TextView(context).apply {
                text = "$dateStr • $mb"
                setTextColor(Color.parseColor(GoogleStyleWidgets.TEXT_MUTED))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(0, dp(context, 2), 0, 0)
            }
            textCol.addView(title); textCol.addView(meta)

            val playBtn = createCircleIconBtn(context, "▶", GoogleStyleWidgets.BRAND_ORANGE_PRIMARY, "#FFFFFF") {
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(clip.uri, "video/mp4")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {
                    Toast.makeText(context, "Unable to open video player", Toast.LENGTH_SHORT).show()
                }
            }
            val editBtn = createCircleIconBtn(context, "✎", GoogleStyleWidgets.SOFT_PEACH_PILL, "#BF360C") {
                showRenameClipDialog(context, clip) {
                    populateClipsContainer(context, container, onStartCameraClick)
                }
            }
            val delBtn = createCircleIconBtn(context, "✕", "#F5EBE7", "#6E5850") {
                try {
                    context.contentResolver.delete(clip.uri, null, null)
                    context.getSharedPreferences(CLIP_NAMES_PREFS, Context.MODE_PRIVATE).edit().remove(clip.id.toString()).apply()
                    populateClipsContainer(context, container, onStartCameraClick)
                } catch (_: Exception) {}
            }

            row.addView(textCol); row.addView(playBtn); row.addView(editBtn); row.addView(delBtn)
            container.addView(row)
        }
    }

    private fun showRenameClipDialog(context: Context, clip: SavedClipItem, onRenamed: () -> Unit) {
        val currentBase = clip.displayName.removeSuffix(".mp4").removeSuffix(".MP4")
        val input = EditText(context).apply {
            setText(currentBase)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setSelection(text.length)
            setTextColor(Color.parseColor(GoogleStyleWidgets.TEXT_DARK))
        }
        val wrap = FrameLayout(context).apply {
            setPadding(dp(context, 20), dp(context, 8), dp(context, 20), 0)
            addView(input)
        }
        AlertDialog.Builder(context)
            .setTitle("Rename Clip")
            .setView(wrap)
            .setPositiveButton("Save") { _, _ ->
                val raw = input.text?.toString()?.trim().orEmpty()
                if (raw.isNotEmpty()) {
                    val finalName = if (raw.endsWith(".mp4", ignoreCase = true)) raw else "$raw.mp4"
                    context.getSharedPreferences(CLIP_NAMES_PREFS, Context.MODE_PRIVATE)
                        .edit().putString(clip.id.toString(), finalName).apply()
                    try {
                        val values = ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, finalName) }
                        context.contentResolver.update(clip.uri, values, null, null)
                    } catch (_: Exception) {}
                    onRenamed()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createCircleIconBtn(context: Context, icon: String, bgHex: String, fgHex: String, onClick: () -> Unit): TextView {
        return TextView(context).apply {
            text = icon
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor(fgHex))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GoogleStyleWidgets.roundedDrawable(context, bgHex, 999)
            layoutParams = LinearLayout.LayoutParams(dp(context, 38), dp(context, 38)).apply { leftMargin = dp(context, 6) }
            setOnClickListener {
                GoogleStyleWidgets.performClickHaptic(this)
                onClick()
            }
        }
    }

    fun showGuideAndPrivacyDialog(context: Context) {
        val message = """
            PlayBax • Live Delayed Video Playback
            
            1. Tripod Delayed Playback (1s–30s):
            Select 1, 2, or 3 cascading screens (+1×D, +2×D, +3×D) and tap Start Camera. Perform your shot or drill and watch your form immediately at your chosen delay.
            
            2. Native Camera Hardware & Gestures:
            • 1080p 16:9 Full-HD with Optical & Electronic Anti-Shake (OIS + EIS)
            • Continuous Video Autofocus & 30fps Indoor Lock
            • Pinch-to-Zoom & Tap-to-Focus directly on the live playback stage
            
            3. Privacy & Data Safety Disclosure:
            • Memory-Only Default: Frames are held strictly in volatile device RAM and automatically pruned.
            • On-Demand MP4 Recording: Video is saved to Movies/PlayBax ONLY when you explicitly tap ● Rec during playback. Zero data is transmitted off-device.
        """.trimIndent()

        AlertDialog.Builder(context)
            .setTitle("PlayBax Guide & Privacy")
            .setMessage(message)
            .setPositiveButton("Got It", null)
            .show()
    }

    fun showPermissionRationaleDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("Camera Access Required")
            .setMessage("PlayBax requires Camera permission to stream live delayed playback on your screen. All video stays 100% on your device.")
            .setPositiveButton("Open App Settings") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                    context.startActivity(intent)
                } catch (_: Exception) {}
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun dp(context: Context, value: Int): Int = GoogleStyleWidgets.dp(context, value)
}
