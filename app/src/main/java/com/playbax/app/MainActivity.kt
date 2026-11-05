package com.playbax.app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
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
    private var isOverlayHidden = false
    private var isVideoRecordingNow = false
    private var pendingLaunchAfterPermission = false
    private var lastAppliedScreenCount = -1
    private var lastAppliedSideBySide = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        if (hasCameraPermission() && pendingLaunchAfterPermission) {
            pendingLaunchAfterPermission = false
            enterCameraStage()
        } else syncUiState()
    }

    private val prefsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = syncUiState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val stage = PlayBaxMultiScreenStage(this)
        ui = MainActivityUiBuilder.build(this, stage)
        setContentView(ui.rootFrame)

        cameraEngine = PlayBaxCameraEngine(
            context = this, lifecycleOwner = this, previewView = stage.previewView, ringBuffer = ringBuffer,
            onFramesDecoded = { results, delaySec ->
                if (!isInCameraStage) return@PlayBaxCameraEngine
                for (i in results.indices) {
                    val res = results[i]
                    ui.multiScreenStage.panels[i].updateFrameState(
                        res.bitmap, (i + 1) * delaySec, res.isWarmingUp, res.remainingWarmupSeconds, res.bufferFillProgress
                    )
                }
            },
            onStatusToast = { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() },
            onRecordingStateChanged = { recording -> isVideoRecordingNow = recording; syncUiState() }
        )

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isInCameraStage) returnToHomeSettings()
                else { isEnabled = false; onBackPressedDispatcher.onBackPressed() }
            }
        })
        wireHomeControls()
        wireCameraControls()
        registerPrefsReceiver()
        syncUiState()
    }

    private fun wireHomeControls() {
        ui.screensTile.card.setOnClickListener { anchor ->
            val d = PlayBaxPreferences.getDelaySeconds(this)
            val idx = (PlayBaxPreferences.getScreenCount(this) - 1).coerceIn(0, 2)
            GoogleStyleWidgets.showGooglePopupMenu(this, anchor, PlayBaxPreferences.getScreenMenuOptions(d), idx) {
                PlayBaxPreferences.setScreenCount(this, it + 1)
            }
        }
        ui.layoutTile.card.setOnClickListener { anchor ->
            val idx = PlayBaxPreferences.getLayoutMode(this)
            GoogleStyleWidgets.showGooglePopupMenu(this, anchor, PlayBaxPreferences.getLayoutMenuOptions(), idx) {
                PlayBaxPreferences.setLayoutMode(this, it)
            }
        }
        ui.captureTile.card.setOnClickListener { anchor ->
            val idx = if (PlayBaxPreferences.isRecordEnabled(this)) 1 else 0
            GoogleStyleWidgets.showGooglePopupMenu(this, anchor, PlayBaxPreferences.getRecordMenuOptions(), idx) {
                PlayBaxPreferences.setRecordEnabled(this, it == 1)
            }
        }
        ui.lensTile.card.setOnClickListener { anchor ->
            val idx = PlayBaxPreferences.getLensMode(this)
            GoogleStyleWidgets.showGooglePopupMenu(this, anchor, PlayBaxPreferences.getLensMenuOptions(), idx) {
                PlayBaxPreferences.setLensMode(this, it)
            }
        }
        ui.cascadeTile.card.setOnClickListener {
            PlayBaxPreferences.setScreenCount(this, (PlayBaxPreferences.getScreenCount(this) % 3) + 1)
        }
        ui.minusBtn.setOnClickListener {
            PlayBaxPreferences.setDelaySeconds(this, PlayBaxPreferences.getDelaySeconds(this) - 1)
        }
        ui.plusBtn.setOnClickListener {
            PlayBaxPreferences.setDelaySeconds(this, PlayBaxPreferences.getDelaySeconds(this) + 1)
        }
        ui.delaySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) PlayBaxPreferences.setDelaySeconds(this@MainActivity, progress + PlayBaxPreferences.MIN_DELAY_SECONDS)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        ui.presetButtons.forEach { (sec, btn) ->
            btn.setOnClickListener { PlayBaxPreferences.setDelaySeconds(this, sec) }
        }
        ui.resetDefaultsBtn.setOnClickListener {
            PlayBaxPreferences.setDelaySeconds(this, 5)
            PlayBaxPreferences.setScreenCount(this, 1)
            PlayBaxPreferences.setLayoutMode(this, PlayBaxPreferences.LAYOUT_AUTO)
            PlayBaxPreferences.setRecordEnabled(this, false)
            PlayBaxPreferences.setLensMode(this, PlayBaxPreferences.LENS_REAR_WIDE)
            Toast.makeText(this, "Reset to default settings", Toast.LENGTH_SHORT).show()
        }
        ui.applyLaunchBtn.setOnClickListener {
            if (!hasCameraPermission()) {
                pendingLaunchAfterPermission = true
                permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
            } else {
                enterCameraStage()
            }
        }
    }

    private fun wireCameraControls() {
        ui.backToHomeBtn.setOnClickListener { returnToHomeSettings() }
        ui.freezeToggleBtn.setOnClickListener {
            cameraEngine.isFrozen = !cameraEngine.isFrozen
            syncUiState()
        }
        ui.recordActionBtn.setOnClickListener {
            if (isVideoRecordingNow) cameraEngine.stopRecordingIfActive()
            else {
                PlayBaxPreferences.setRecordEnabled(this, true)
                cameraEngine.startRecordingIfNeeded()
            }
            syncUiState()
        }
        ui.fullscreenToggleBtn.setOnClickListener {
            isOverlayHidden = true
            syncUiState()
        }
        ui.multiScreenStage.panels.forEach { panel ->
            panel.setOnClickListener {
                isOverlayHidden = !isOverlayHidden
                syncUiState()
            }
        }
    }

    private fun enterCameraStage() {
        isInCameraStage = true
        isOverlayHidden = false
        cameraEngine.isFrozen = false
        ringBuffer.clear()
        syncUiState()
        cameraEngine.startCameraSession(forceRebind = true)
        if (PlayBaxPreferences.isRecordEnabled(this)) {
            ui.rootFrame.postDelayed({ if (isInCameraStage) cameraEngine.startRecordingIfNeeded() }, 600L)
        }
    }

    private fun returnToHomeSettings() {
        isInCameraStage = false
        isOverlayHidden = false
        cameraEngine.isFrozen = false
        cameraEngine.stopStageSession()
        ringBuffer.clear()
        syncUiState()
    }

    private fun syncUiState() {
        val delaySec = PlayBaxPreferences.getDelaySeconds(this)
        val screenCount = PlayBaxPreferences.getScreenCount(this)
        val layoutMode = PlayBaxPreferences.getLayoutMode(this)
        val recordEnabled = PlayBaxPreferences.isRecordEnabled(this)
        val lensMode = PlayBaxPreferences.getLensMode(this)
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        ui.delayRingGauge.updateState(delaySec, screenCount)
        ui.topModePill.text = if (recordEnabled) "● MP4 Capture" else "Memory Stream"
        ui.screensTile.valueText.text = if (screenCount == 1) "1 Screen" else "$screenCount Screens"

        val cascadeStr = when (screenCount) {
            1 -> "+${delaySec}s"
            2 -> "+${delaySec}s • +${delaySec * 2}s"
            else -> "+${delaySec}s • +${delaySec * 2}s • +${delaySec * 3}s"
        }
        ui.screensTile.subtitleText.text = cascadeStr
        ui.layoutTile.valueText.text = PlayBaxPreferences.getLayoutChipLabel(layoutMode, isLandscape)
        ui.layoutTile.subtitleText.text = if (layoutMode == PlayBaxPreferences.LAYOUT_AUTO) "Portrait / Landscape" else "Locked orientation"
        ui.captureTile.valueText.text = if (recordEnabled) "Record MP4" else "Memory Only"
        ui.captureTile.subtitleText.text = if (recordEnabled) "Saves to Gallery" else "Unrecorded RAM"
        ui.lensTile.valueText.text = PlayBaxPreferences.getLensChipLabel(lensMode)
        ui.cascadeTile.valueText.text = cascadeStr

        val targetProgress = delaySec - PlayBaxPreferences.MIN_DELAY_SECONDS
        if (ui.delaySlider.progress != targetProgress) ui.delaySlider.progress = targetProgress

        ui.presetButtons.forEach { (sec, btn) ->
            val sel = (sec == delaySec)
            btn.background = GoogleStyleWidgets.roundedDrawable(this, if (sel) "#0B57D0" else "#E6EAEE", 999)
            btn.setTextColor(Color.parseColor(if (sel) "#FFFFFF" else "#1F1F1F"))
        }

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

        ui.homeSetupScroll.visibility = if (isInCameraStage) View.GONE else View.VISIBLE
        ui.cameraStageContainer.visibility = if (isInCameraStage) View.VISIBLE else View.GONE

        if (isInCameraStage) {
            ui.cameraTopBar.visibility = if (isOverlayHidden) View.GONE else View.VISIBLE
            ui.cameraBottomBar.visibility = if (isOverlayHidden) View.GONE else View.VISIBLE
            ui.freezeToggleBtn.text = if (cameraEngine.isFrozen) "▶ Resume" else "⏸ Freeze"

            if (isVideoRecordingNow) {
                ui.cameraStatusPill.text = "● REC ($cascadeStr)"
                ui.cameraStatusPill.background = GoogleStyleWidgets.roundedDrawable(this, "#FFDAD6", 999)
                ui.recordActionBtn.text = "■ Stop Rec"
                ui.recordActionBtn.background = GoogleStyleWidgets.roundedDrawable(this, "#B3261E", 999)
            } else {
                ui.cameraStatusPill.text = if (cameraEngine.isFrozen) "⏸ FROZEN ($cascadeStr)" else "● LIVE ($cascadeStr)"
                ui.cameraStatusPill.background = GoogleStyleWidgets.roundedDrawable(this, "#E6A8F5EC", 999)
                ui.recordActionBtn.text = "● Record MP4"
                ui.recordActionBtn.background = GoogleStyleWidgets.roundedDrawable(this, "#0B57D0", 999)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        syncUiState()
    }

    override fun onResume() {
        super.onResume()
        syncUiState()
        if (isInCameraStage && hasCameraPermission()) cameraEngine.startCameraSession()
    }

    override fun onPause() {
        if (isInCameraStage) cameraEngine.stopStageSession()
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(prefsReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(prefsReceiver, filter)
        }
    }
}
