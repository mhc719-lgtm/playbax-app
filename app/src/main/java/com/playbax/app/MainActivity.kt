package com.playbax.app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var ui: MainActivityViews
    private val ringBuffer = PlayBaxFrameRingBuffer()
    private lateinit var cameraEngine: PlayBaxCameraEngine

    private var isInCameraStage = false
    private var isShowingClipsTab = false
    private var isVideoRecordingNow = false
    private var pendingLaunchAfterPermission = false
    private var lastAppliedScreenCount = -1
    private var lastAppliedSideBySide = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && pendingLaunchAfterPermission) {
            pendingLaunchAfterPermission = false
            enterCameraStage()
        } else if (!granted) {
            pendingLaunchAfterPermission = false
            PlayBaxClipsHelper.showPermissionRationaleDialog(this)
            syncUiState()
        } else syncUiState()
    }

    private val prefsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = syncUiState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        val stage = PlayBaxMultiScreenStage(this)
        ui = MainActivityUiBuilder.build(this, stage)
        setContentView(ui.rootFrame)

        cameraEngine = PlayBaxCameraEngine(
            context = this,
            lifecycleOwner = this,
            previewView = stage.previewView,
            ringBuffer = ringBuffer,
            onFramesDecoded = { results, _ ->
                if (!isInCameraStage) return@PlayBaxCameraEngine
                for (i in results.indices) ui.multiScreenStage.panels[i].updateFrameState(results[i].bitmap)
            },
            onOrientationChangedCallback = {
                if (isInCameraStage) {
                    ui.multiScreenStage.clearPanelBitmaps()
                    syncUiState()
                }
            },
            onStatusToast = { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() },
            onRecordingStateChanged = { recording -> isVideoRecordingNow = recording; syncUiState() }
        )

        stage.onPinchZoomDelta = { factor -> cameraEngine.handlePinchZoom(factor) }
        stage.onTapToFocusPoint = { x, y, _, _ -> cameraEngine.handleTapToFocus(x, y) }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    isInCameraStage -> returnToHomeSettings()
                    isShowingClipsTab -> { isShowingClipsTab = false; syncUiState() }
                    else -> { isEnabled = false; onBackPressedDispatcher.onBackPressed() }
                }
            }
        })
        wireHomeControls()
        wireCameraControls()
        registerPrefsReceiver()
        syncUiState()
    }

    private fun wireHomeControls() {
        ui.badgeCircle.setOnClickListener { PlayBaxClipsHelper.showGuideAndPrivacyDialog(this) }
        ui.topModePill.setOnClickListener { PlayBaxClipsHelper.showGuideAndPrivacyDialog(this) }
        ui.navSetupTab.setOnClickListener { isShowingClipsTab = false; syncUiState() }
        ui.navClipsTab.setOnClickListener {
            isShowingClipsTab = true
            PlayBaxClipsHelper.populateClipsContainer(this, ui.clipsBodyCol) { isShowingClipsTab = false; triggerStartCamera() }
            syncUiState()
        }
        ui.navGuideTab.setOnClickListener { PlayBaxClipsHelper.showGuideAndPrivacyDialog(this) }

        ui.screensTile.card.setOnClickListener { anchor ->
            val d = PlayBaxPreferences.getDelaySeconds(this)
            val idx = (PlayBaxPreferences.getScreenCount(this) - 1).coerceIn(0, 2)
            GoogleStyleWidgets.showGooglePopupMenu(this, anchor, PlayBaxPreferences.getScreenMenuOptions(d), idx) { PlayBaxPreferences.setScreenCount(this, it + 1) }
        }
        ui.layoutTile.card.setOnClickListener { anchor ->
            val idx = PlayBaxPreferences.getLayoutMode(this)
            GoogleStyleWidgets.showGooglePopupMenu(this, anchor, PlayBaxPreferences.getLayoutMenuOptions(), idx) { PlayBaxPreferences.setLayoutMode(this, it) }
        }
        ui.captureTile.card.setOnClickListener { anchor ->
            val idx = if (PlayBaxPreferences.isRecordEnabled(this)) 1 else 0
            GoogleStyleWidgets.showGooglePopupMenu(this, anchor, PlayBaxPreferences.getRecordMenuOptions(), idx) { PlayBaxPreferences.setRecordEnabled(this, it == 1) }
        }
        ui.lensTile.card.setOnClickListener { anchor ->
            val idx = PlayBaxPreferences.getLensMode(this)
            GoogleStyleWidgets.showGooglePopupMenu(this, anchor, PlayBaxPreferences.getLensMenuOptions(), idx) { PlayBaxPreferences.setLensMode(this, it) }
        }
        ui.cascadeTile.card.setOnClickListener { PlayBaxPreferences.setScreenCount(this, (PlayBaxPreferences.getScreenCount(this) % 3) + 1) }
        ui.minusBtn.setOnClickListener { PlayBaxPreferences.setDelaySeconds(this, PlayBaxPreferences.getDelaySeconds(this) - 1) }
        ui.plusBtn.setOnClickListener { PlayBaxPreferences.setDelaySeconds(this, PlayBaxPreferences.getDelaySeconds(this) + 1) }

        ui.delaySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) PlayBaxPreferences.setDelaySeconds(this@MainActivity, progress + PlayBaxPreferences.MIN_DELAY_SECONDS)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        ui.presetButtons.forEach { (sec, btn) ->
            btn.setOnClickListener {
                try { btn.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) } catch (_: Exception) {}
                PlayBaxPreferences.setDelaySeconds(this, sec)
            }
        }
        ui.resetDefaultsBtn.setOnClickListener {
            PlayBaxPreferences.setDelaySeconds(this, 5)
            PlayBaxPreferences.setScreenCount(this, 1)
            PlayBaxPreferences.setLayoutMode(this, PlayBaxPreferences.LAYOUT_AUTO)
            PlayBaxPreferences.setRecordEnabled(this, false)
            PlayBaxPreferences.setLensMode(this, PlayBaxPreferences.LENS_REAR_WIDE)
            Toast.makeText(this, "Reset to default settings", Toast.LENGTH_SHORT).show()
        }
        ui.applyLaunchBtn.setOnClickListener { triggerStartCamera() }
    }

    private fun triggerStartCamera() {
        if (!hasCameraPermission()) {
            pendingLaunchAfterPermission = true
            permissionLauncher.launch(Manifest.permission.CAMERA)
        } else enterCameraStage()
    }

    private fun wireCameraControls() {
        ui.recordActionBtn.setOnClickListener {
            try { it.performHapticFeedback(HapticFeedbackConstants.CONFIRM) } catch (_: Exception) {}
            if (isVideoRecordingNow) cameraEngine.stopRecordingIfActive(silent = false)
            else cameraEngine.startRecordingIfNeeded()
            syncUiState()
        }
        ui.freezeToggleBtn.setOnClickListener {
            cameraEngine.isFrozen = !cameraEngine.isFrozen
            syncUiState()
        }
        ui.stopToHomeBtn.setOnClickListener { returnToHomeSettings() }
    }

    private fun enterCameraStage() {
        isInCameraStage = true
        isVideoRecordingNow = false
        PlayBaxPreferences.setRecordEnabled(this, false)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        cameraEngine.isFrozen = false
        ringBuffer.clear()
        ui.multiScreenStage.clearPanelBitmaps()
        syncUiState()
        cameraEngine.startCameraSession()
    }

    private fun returnToHomeSettings() {
        isInCameraStage = false
        cameraEngine.isFrozen = false
        cameraEngine.stopStageSession(saveActiveRecording = true)
        ringBuffer.clear()
        ui.multiScreenStage.clearPanelBitmaps()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        if (isShowingClipsTab) PlayBaxClipsHelper.populateClipsContainer(this, ui.clipsBodyCol) { triggerStartCamera() }
        syncUiState()
    }

    private fun syncUiState() {
        val delaySec = PlayBaxPreferences.getDelaySeconds(this)
        val screenCount = PlayBaxPreferences.getScreenCount(this)
        val layoutMode = PlayBaxPreferences.getLayoutMode(this)
        val recordEnabled = PlayBaxPreferences.isRecordEnabled(this)
        val lensMode = PlayBaxPreferences.getLensMode(this)
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE ||
            (isInCameraStage && ::cameraEngine.isInitialized && cameraEngine.isCurrentlyLandscape())

        ui.delayRingGauge.updateState(delaySec, screenCount)
        ui.topModePill.text = "1080p • OIS+EIS"
        ui.screensTile.valueText.text = if (screenCount == 1) "1 Screen" else "$screenCount Screens"

        val cascadeStr = when (screenCount) {
            1 -> "+${delaySec}s"
            2 -> "+${delaySec}s • +${delaySec * 2}s"
            else -> "+${delaySec}s • +${delaySec * 2}s • +${delaySec * 3}s"
        }
        ui.screensTile.subtitleText.text = cascadeStr
        ui.layoutTile.valueText.text = PlayBaxPreferences.getLayoutChipLabel(layoutMode, isLandscape)
        ui.layoutTile.subtitleText.text = if (layoutMode == PlayBaxPreferences.LAYOUT_AUTO) "Portrait / Landscape" else "Locked orientation"
        ui.captureTile.valueText.text = if (recordEnabled) "Manual ● Rec" else "Memory Only"
        ui.captureTile.subtitleText.text = if (recordEnabled) "Tap ● Rec in feed" else "Unrecorded RAM"
        ui.lensTile.valueText.text = PlayBaxPreferences.getLensChipLabel(lensMode)
        ui.cascadeTile.valueText.text = cascadeStr

        val targetProgress = delaySec - PlayBaxPreferences.MIN_DELAY_SECONDS
        if (ui.delaySlider.progress != targetProgress) ui.delaySlider.progress = targetProgress

        ui.presetButtons.forEach { (sec, btn) ->
            val sel = (sec == delaySec)
            btn.background = GoogleStyleWidgets.roundedDrawable(this, if (sel) GoogleStyleWidgets.BRAND_ORANGE_PRIMARY else "#F5EBE7", 999)
            btn.setTextColor(Color.parseColor(if (sel) "#FFFFFF" else GoogleStyleWidgets.TEXT_DARK))
        }

        ui.setupBodyCol.visibility = if (isShowingClipsTab) View.GONE else View.VISIBLE
        ui.clipsBodyCol.visibility = if (isShowingClipsTab) View.VISIBLE else View.GONE
        ui.navSetupTab.background = GoogleStyleWidgets.roundedDrawable(this, if (!isShowingClipsTab) GoogleStyleWidgets.SOFT_PEACH_PILL else "#FFFFFF", 999)
        ui.navSetupTab.setTextColor(Color.parseColor(if (!isShowingClipsTab) "#BF360C" else GoogleStyleWidgets.TEXT_MUTED))
        ui.navClipsTab.background = GoogleStyleWidgets.roundedDrawable(this, if (isShowingClipsTab) GoogleStyleWidgets.SOFT_PEACH_PILL else "#FFFFFF", 999)
        ui.navClipsTab.setTextColor(Color.parseColor(if (isShowingClipsTab) "#BF360C" else GoogleStyleWidgets.TEXT_MUTED))
        ui.navGuideTab.background = GoogleStyleWidgets.roundedDrawable(this, "#FFFFFF", 999)
        ui.navGuideTab.setTextColor(Color.parseColor(GoogleStyleWidgets.TEXT_MUTED))

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

        ui.homeContainer.visibility = if (isInCameraStage) View.GONE else View.VISIBLE
        ui.cameraStageContainer.visibility = if (isInCameraStage) View.VISIBLE else View.GONE

        if (isInCameraStage) {
            ui.freezeToggleBtn.text = if (cameraEngine.isFrozen) "▶ Play" else "⏸ Pause"
            ui.recordActionBtn.text = if (isVideoRecordingNow) "● Rec On" else "● Rec"
            ui.recordActionBtn.setTextColor(Color.parseColor(if (isVideoRecordingNow) "#FF7043" else "#FFFFFF"))
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (::cameraEngine.isInitialized) {
            cameraEngine.onDisplayRotationChanged()
            ui.rootFrame.postDelayed({ cameraEngine.onDisplayRotationChanged(); syncUiState() }, 120L)
        }
        syncUiState()
    }

    override fun onResume() {
        super.onResume()
        syncUiState()
        if (isInCameraStage && hasCameraPermission() && !cameraEngine.isStageRunning) cameraEngine.startCameraSession()
    }

    override fun onPause() {
        if (isInCameraStage) cameraEngine.stopStageSession(saveActiveRecording = true)
        super.onPause()
    }

    override fun onDestroy() {
        try { unregisterReceiver(prefsReceiver) } catch (_: Exception) {}
        cameraEngine.shutdown()
        super.onDestroy()
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun registerPrefsReceiver() {
        val filter = IntentFilter(PlayBaxPreferences.ACTION_PREFS_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) registerReceiver(prefsReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else registerReceiver(prefsReceiver, filter)
    }
}
