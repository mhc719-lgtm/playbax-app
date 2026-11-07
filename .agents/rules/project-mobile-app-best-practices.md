---
trigger: always_on
description: Project-specific mobile app best practices, multi-screen delayed video ring-buffer architecture, toolchain commands, and Google Drive/GitHub APK delivery rules for PlayBax App (com.playbax.app), extending Global Mobile App Best Practices.
---

# Project Mobile App Best Practices

## 1. Global Governance Alignment & Core Product Architecture (`com.playbax.app`)
This project inherits all quality gates, `<300 LOC` modularity rules, modern Google soft gradient adaptive icon standards, Material 3 mobile ergonomics, and the 4-step pipeline from **Global Mobile App Best Practices** (`global-mobile-app-best-practices.md`).
- **Application Name & Package**: **PlayBax** (`com.playbax.app`), targeting `minSdk = 26`, `targetSdk = 34`, `compileSdk = 34`.
- **Core Delayed Video Playback Architecture**:
  1. **1–30s Base Delay + 1 to 3 Cascading Delayed Screens (`PlayBaxStageViews.kt`)**:
     - Supports `1 Screen` (`+1×D`, e.g., `+5s`), `2 Screens` (`+1×D` and `+2×D`, e.g., `+5s / +10s`), or `3 Screens` (`+1×D`, `+2×D`, and `+3×D`, e.g., `+5s / +10s / +15s`).
     - Supports **Portrait Stacked** (`VERTICAL`), **Landscape Side-by-Side** (`HORIZONTAL`), and **Auto Orientation** switching seamlessly (`android:configChanges="orientation|screenSize|screenLayout|smallestScreenSize|uiMode"`) without dropping the active frame buffer.
  2. **Full-HD `ARGB_8888` Ring Buffer, Hardware `OIS + EIS` & Manual Composite Stage Recording (`PlayBaxFrameRingBuffer.kt`, `CameraLensHelper.kt`, `PlayBaxStageRecorder.kt`)**:
     - Captures `16:9` (`1920×1080`) `YUV_420_888` frames on a background executor (`YuvFrameConverter.kt`) into an in-memory `ARGB_8888` ring buffer (`96 MB` cap) with hardware `OIS + EIS` stabilization, `CONTINUOUS_VIDEO` autofocus, and `30fps` AE lock.
     - Never auto-records on camera entry; tapping `● Rec` in the live feed starts `PlayBaxStageRecorder.kt` (`MediaCodec` H.264 surface compositor + `MediaMuxer`), recording the exact 1, 2, or 3 delayed screens visible on stage to `Movies/PlayBax`.
  3. **Electric Orange-Red Google Health Home UI & Distraction-Free Live Stage (`MainActivityUiBuilder.kt`, `PlayBaxUserMenuSheet.kt`, `PlayBaxStageViews.kt`)**:
     - **Home Screen**: High-energy Electric Orange-Red (`#FF3D00` / `#FF1744` / `#FF6D00`) theme with a `38dp` circular brand icon + `25sp` `PlayBax` wordmark, non-clickable `1080p • OIS+EIS` status badge, `38dp` `PB` avatar opening a slide-open Google Health User Menu, strict `1f : 1f` (`50% / 50%`) symmetric `220dp` gauge + `68dp` Bento tiles, and a `#F4EAE6` tonal 2-tab bottom navigation bar (`Home` and `Saved Clips` with monochrome vector icons).
     - **Live Stage**: Unlocks `SCREEN_ORIENTATION_FULL_SENSOR` + `OrientationEventListener` for seamless Portrait/Landscape rotation, supports pinch-to-zoom and tap-to-focus, and renders only a single low-visibility (`alpha = 0.36f`) `● Rec | ⏸ Pause | ■ Stop` pill.

## 2. Portable Toolchain & Build Commands
- **Workspace Path**: `c:\Users\Home1\.gemini\antigravity\scratch\playbax-app`
- **Portable JDK 17 Path**: `c:\Users\Home1\.gemini\antigravity\scratch\overplay-app\.local-sdk\jdk17\bin\java.exe`
- **Portable Android SDK 34 Path**: `c:\Users\Home1\.gemini\antigravity\scratch\overplay-app\.local-sdk\android-sdk` (configured in `local.properties`).
- **Canonical Build Command (`BypassSandbox: true`)**:
  ```powershell
  c:\Users\Home1\.gemini\antigravity\scratch\overplay-app\.local-sdk\jdk17\bin\java.exe -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain assembleDebug assembleRelease --no-daemon
  ```
- **Canonical APK Staging Command (`BypassSandbox: false`)**:
  ```powershell
  Copy-Item -Path "app\build\outputs\apk\release\app-release.apk" -Destination "PlayBax.apk" -Force
  Copy-Item -Path "app\build\outputs\apk\release\app-release.apk" -Destination "PlayBax-v<versionName>.apk" -Force
  ```

## 3. Delivery & Repository Governance (`playbax-app`)
- **Target Google Drive Folder (`playbax-app`)**:
  - **Folder ID**: `1lIXm_e4GIPygjcsQXzBmAeWjfqKsS_wY` (`https://drive.google.com/drive/folders/1lIXm_e4GIPygjcsQXzBmAeWjfqKsS_wY`)
  - **Canonical In-Place `PlayBax.apk` File ID**: `1ZTgp6XJ0gKL1GqLTjLt5LOwrk0lV1W2L` (`https://drive.google.com/file/d/1ZTgp6XJ0gKL1GqLTjLt5LOwrk0lV1W2L/view`)
- **Canonical Google Drive Upload Commands (`BypassSandbox: true`)**:
  1. Update `PlayBax.apk` in-place (preserves stable download URL):
     ```powershell
     gdrive mutate update 1ZTgp6XJ0gKL1GqLTjLt5LOwrk0lV1W2L PlayBax.apk
     ```
  2. Upload versioned `PlayBax-v<versionName>.apk` into the `playbax-app` folder:
     ```powershell
     gdrive mutate upload PlayBax-v<versionName>.apk --parent 1lIXm_e4GIPygjcsQXzBmAeWjfqKsS_wY
     ```
- **Target GitHub Repository**: `https://github.com/mhc719-lgtm/playbax-app.git`
- **`0.0.1` Version & Commit Convention**: On every build or commit, increment `versionCode` by `+1` and `versionName` by `0.0.1` (rolling `.9` over to the next minor `.0`, e.g., `1.0.0` → `1.0.1`, `1.0.9` → `1.1.0`) in `app/build.gradle.kts`, and prefix every Git commit message with the new `<versionName>` (`git commit -m "<versionName> <summary>"`).
