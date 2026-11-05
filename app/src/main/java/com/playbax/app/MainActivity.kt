package com.playbax.app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.Choreographer
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity(), Choreographer.FrameCallback {

    private lateinit var ui: MainActivityViews
    private val ringBuffer = PlayBaxFrameRingBuffer()
    private lateinit var cameraEngine: PlayBaxCameraEngine

    private var isDelayedStreamActive = false
    private var isVideoRecordingNow = false
    private var lastAppliedScreenCount = -1
    private var lastAppliedSideBySide = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        syncUiState()
        if (hasCameraPermission()) cameraEngine.bindCameraIfNeeded(forceRebind = true)
    }

    private val prefsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = syncUiState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        ui = MainActivityUiBuilder.build(this, PlayBaxMultiScreenStage(this))
        setContentView(ui.rootFrame)

        cameraEngine = PlayBaxCameraEngine(
            context = this,
            lifecycleOwner = this,
            ringBuffer = ringBuffer,
            onStatusToast = { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() },
            onRecordingStateChanged = { recording ->
                isVideoRecordingNow = recording
                syncUiState()
            }
        )

        wireControls()
        registerPrefsReceiver()
        syncUiState()

        if (!hasCameraPermission()) requestCameraPermissions()
        else cameraEngine.bindCameraIfNeeded(forceRebind = true)
    }

    private fun wireControls() {
        ui.multiScreenStage.panels.forEach { panel ->
            panel.setOnClickListener {
                PlayBaxPreferences.setControlsMinimized(this, !PlayBaxPreferences.isControlsMinimized(this))
            }
        }
        ui.dragHandleContainer.setOnClickListener { PlayBaxPreferences.setControlsMinimized(this, true) }
        ui.minimizedTripodPill.setOnClickListener { PlayBaxPreferences.setControlsMinimized(this, false) }

        ui.minusBtn.setOnClickListener {
            PlayBaxPreferences.setDelaySeconds(this, PlayBaxPreferences.getDelaySeconds(this) - 1)
        }
        ui.plusBtn.setOnClickListener {
            PlayBaxPreferences.setDelaySeconds(this, PlayBaxPreferences.getDelaySeconds(this) + 1)
        }

        ui.delaySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    PlayBaxPreferences.setDelaySeconds(
                        this@MainActivity,
                        progress + PlayBaxPreferences.MIN_DELAY_SECONDS
                    )
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        ui.screenSpinnerChip.setOnClickListener { anchor ->
            val d = PlayBaxPreferences.getDelaySeconds(this)
            val idx = (PlayBaxPreferences.getScreenCount(this) - 1).coerceIn(0, 2)
            GoogleStyleWidgets.showGooglePopupMenu(this, anchor, PlayBaxPreferences.getScreenMenuOptions(d), idx) {
                PlayBaxPreferences.setScreenCount(this, it + 1)
            }
        }

        ui.recordSpinnerChip.setOnClickListener { anchor ->
            val idx = if (PlayBaxPreferences.isRecordEnabled(this)) 1 else 0
            GoogleStyleWidgets.showGooglePopupMenu(this, anchor, PlayBaxPreferences.getRecordMenuOptions(), idx) {
                val enableRec = (it == 1)
                PlayBaxPreferences.setRecordEnabled(this, enableRec)
                if (isDelayedStreamActive) {
                    if (enableRec) cameraEngine.startRecordingIfNeeded()
                    else cameraEngine.stopRecordingIfActive()
                }
            }
        }

        ui.lensQuickChip.setOnClickListener { anchor ->
            val idx = PlayBaxPreferences.getLensMode(this)
            GoogleStyleWidgets.showGooglePopupMenu(this, anchor, PlayBaxPreferences.getLensMenuOptions(), idx) {
                PlayBaxPreferences.setLensMode(this, it)
                ringBuffer.clear()
                cameraEngine.bindCameraIfNeeded(forceRebind = true)
            }
        }

        ui.layoutQuickChip.setOnClickListener { anchor ->
            val idx = PlayBaxPreferences.getLayoutMode(this)
            GoogleStyleWidgets.showGooglePopupMenu(this, anchor, PlayBaxPreferences.getLayoutMenuOptions(), idx) {
                PlayBaxPreferences.setLayoutMode(this, it)
            }
        }

        ui.grantCameraBtn.setOnClickListener { requestCameraPermissions() }

        ui.startDelayBtn.setOnClickListener {
            if (!hasCameraPermission()) {
                requestCameraPermissions()
                return@setOnClickListener
            }
            ringBuffer.clear()
            isDelayedStreamActive = true
            if (PlayBaxPreferences.isRecordEnabled(this)) cameraEngine.startRecordingIfNeeded()
            syncUiState()
        }

        ui.tripodViewBtn.setOnClickListener { PlayBaxPreferences.setControlsMinimized(this, true) }
        ui.resetBufferBtn.setOnClickListener {
            ringBuffer.clear()
            Toast.makeText(this, "Delay buffer reset", Toast.LENGTH_SHORT).show()
        }
        ui.pauseDelayBtn.setOnClickListener {
            isDelayedStreamActive = false
            cameraEngine.stopRecordingIfActive()
            ringBuffer.clear()
            syncUiState()
        }
    }

    override fun doFrame(frameTimeNanos: Long) {
        val delaySec = PlayBaxPreferences.getDelaySeconds(this)
        val screenCount = PlayBaxPreferences.getScreenCount(this)
        val nowMs = SystemClock.elapsedRealtime()
        cameraEngine.maxRetentionMs = (screenCount * delaySec * 1000L)

        for (i in 0 until screenCount) {
            val panel = ui.multiScreenStage.panels[i]
            val slotDelaySec = (i + 1) * delaySec
            val res = ringBuffer.decodeFrameForDelay(
                nowMs = nowMs,
                delayMs = slotDelaySec * 1000L,
                reusableBitmap = panel.frameBitmap,
                streamActive = isDelayedStreamActive
            )
            panel.updateFrameState(
                bitmap = res.bitmap,
                targetDelaySec = slotDelaySec,
                active = isDelayedStreamActive,
                warmingUp = res.isWarmingUp,
                remainingSec = res.remainingWarmupSeconds,
                progress = res.bufferFillProgress
            )
        }
        Choreographer.getInstance().postFrameCallback(this)
    }

    private fun syncUiState() {
        val delaySec = PlayBaxPreferences.getDelaySeconds(this)
        val screenCount = PlayBaxPreferences.getScreenCount(this)
        val layoutMode = PlayBaxPreferences.getLayoutMode(this)
        val recordEnabled = PlayBaxPreferences.isRecordEnabled(this)
        val lensMode = PlayBaxPreferences.getLensMode(this)
        val minimized = PlayBaxPreferences.isControlsMinimized(this)
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        val sideBySide = when (layoutMode) {
            PlayBaxPreferences.LAYOUT_PORTRAIT_STACKED -> false
            PlayBaxPreferences.LAYOUT_LANDSCAPE_SIDE -> true
            else -> isLandscape
        }
        if (lastAppliedScreenCount != screenCount || lastAppliedSideBySide != sideBySide) {
            ui.multiScreenStage.applyStageLayout(screenCount, sideBySide)
            lastAppliedScreenCount = screenCount
            lastAppliedSideBySide = sideBySide
        }

        val targetProgress = delaySec - PlayBaxPreferences.MIN_DELAY_SECONDS
        if (ui.delaySlider.progress != targetProgress) ui.delaySlider.progress = targetProgress

        val cascadeSummary = when (screenCount) {
            1 -> "+${delaySec}s"
            2 -> "+${delaySec}s / +${delaySec * 2}s"
            else -> "+${delaySec}s / +${delaySec * 2}s / +${delaySec * 3}s"
        }
        val modeTag = if (recordEnabled) "MP4 Rec" else "Memory"
        ui.readoutBadge.text = "$cascadeSummary • $modeTag"

        ui.screenSpinnerChip.text = GoogleStyleWidgets.formatSpinnerText(
            PlayBaxPreferences.getScreenSpinnerLabel(screenCount, delaySec)
        )
        ui.recordSpinnerChip.text = GoogleStyleWidgets.formatSpinnerText(
            PlayBaxPreferences.getRecordSpinnerLabel(recordEnabled)
        )
        ui.lensQuickChip.text = "✓ ${PlayBaxPreferences.getLensChipLabel(lensMode)} ▾"
        ui.layoutQuickChip.text = "✓ ${PlayBaxPreferences.getLayoutChipLabel(layoutMode, isLandscape)} ▾"
        ui.minimizedPillText.text = "▲  PlayBax ($cascadeSummary) • Tap for Controls"

        if (isDelayedStreamActive) {
            val dotHex = if (isVideoRecordingNow) GoogleStyleWidgets.GOOGLE_RED else GoogleStyleWidgets.GOOGLE_GREEN
            ui.statusDot.background = MainActivityUiBuilder.pillDrawable(dotHex)
            ui.statusText.text = if (isVideoRecordingNow) "REC • Delay $cascadeSummary" else "Live Delay ($cascadeSummary)"
        } else {
            ui.statusDot.background = MainActivityUiBuilder.pillDrawable(GoogleStyleWidgets.M3_ACCENT_LIGHT)
            ui.statusText.text = "PlayBax Ready ($cascadeSummary)"
        }

        val camGranted = hasCameraPermission()
        ui.grantCameraBtn.visibility = if (camGranted) View.GONE else View.VISIBLE
        ui.mainActionSection.visibility = if (camGranted) View.VISIBLE else View.GONE
        ui.startDelayBtn.visibility = if (isDelayedStreamActive) View.GONE else View.VISIBLE
        ui.activeButtonsRow.visibility = if (isDelayedStreamActive) View.VISIBLE else View.GONE

        ui.bottomDock.visibility = if (minimized) View.GONE else View.VISIBLE
        ui.minimizedTripodPill.visibility = if (minimized) View.VISIBLE else View.GONE
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        syncUiState()
    }

    override fun onResume() {
        super.onResume()
        syncUiState()
        if (hasCameraPermission()) cameraEngine.bindCameraIfNeeded()
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun onPause() {
        Choreographer.getInstance().removeFrameCallback(this)
        super.onPause()
    }

    override fun onDestroy() {
        Choreographer.getInstance().removeFrameCallback(this)
        try { unregisterReceiver(prefsReceiver) } catch (_: Exception) {}
        cameraEngine.shutdown()
        super.onDestroy()
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun requestCameraPermissions() {
        permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
    }

    private fun registerPrefsReceiver() {
        val filter = IntentFilter(PlayBaxPreferences.ACTION_PREFS_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(prefsReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(prefsReceiver, filter)
        }
    }
}
