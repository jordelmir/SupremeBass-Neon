# Supreme Bass — Comprehensive Codebase Audit

**Date:** 2026-07-04
**App Version:** 1.0 (versionCode 1)
**Package:** `com.supremecorp.bass`
**Audit Scope:** Full source tree, build config, manifests, resources, architecture, dependencies, permissions, privacy, and Play Store readiness.

---

## Table of Contents

1. [Stack Overview](#1-stack-overview)
2. [Architecture](#2-architecture)
3. [Complete File Inventory](#3-complete-file-inventory)
4. [Audio Flow Analysis](#4-audio-flow-analysis)
5. [Current Dependencies](#5-current-dependencies)
6. [Permissions](#6-permissions)
7. [Network Usage](#7-network-usage)
8. [Storage Usage](#8-storage-usage)
9. [Privacy Policy](#9-privacy-policy)
10. [Analytics / Crash / Ads](#10-analytics--crash--ads)
11. [Build Variants](#11-build-variants)
12. [Risks Identified](#12-risks-identified)
13. [Proposed Changes](#13-proposed-changes)
14. [New Dependencies Needed](#14-new-dependencies-needed)
15. [Privacy Impact Assessment](#15-privacy-impact-assessment)
16. [Performance Impact](#16-performance-impact)
17. [Play Store Checklist](#17-play-store-checklist)

---

## 1. Stack Overview

| Layer | Technology | Version / Detail |
|---|---|---|
| Language | Kotlin | 1.9.20 |
| UI Framework | Jetpack Compose + XML hybrid | Compose BOM 2024.02.02, Kotlin Compiler Extension 1.5.4 |
| Material | Material 3 (Compose) + MaterialComponents (XML) | Material 3 via BOM; `com.google.android.material:material:1.11.0` for XML |
| Build System | Gradle KTS | Gradle 8.6, AGP 8.3.2 |
| minSdk | 26 | Android 8.0 Oreo |
| targetSdk | 34 | Android 14 |
| compileSdk | 34 | Android 14 |
| JVM Target | 17 | |
| Java Home | `/opt/homebrew/opt/openjdk@17` | Homebrew OpenJDK 17 |
| Signing | Release keystore present | `release-key.jks` with hardcoded password |

---

## 2. Architecture

### 2.1 Pattern
- **Single Activity** (`MainActivity`) with Compose-based UI via `setContent {}`.
- One foreground **Service** (`AudioService`) for persistent audio boost.
- One custom **View** (`MatrixRainView`) used in the XML layout (legacy/unused in practice since Compose takes over).
- **Object singleton** (`AdsManager`) for AdMob lifecycle management.
- **Engine class** (`AudioEngine`) wrapping `LoudnessEnhancer`.

### 2.2 Package Structure (Monolithic)
```
com.supremecorp.bass/
├── AdsManager.kt          # AdMob singleton (banner, interstitial, rewarded)
├── AudioEngine.kt          # LoudnessEnhancer wrapper with health-check loop
├── AudioService.kt         # Foreground service with WakeLock
├── MainActivity.kt         # Single Activity + all Compose UI (660 lines)
├── MatrixRainView.kt       # Custom View for matrix rain (XML layout reference)
└── ui/
    ├── components/
    │   ├── AudioVisualizer.kt   # Canvas-based audio visualizer
    │   ├── BreathingText.kt     # Animated text with glow
    │   ├── MatrixRain.kt        # Compose Canvas matrix rain
    │   └── NeonSwitch.kt        # Custom toggle switch
    └── theme/
        ├── GlassModifiers.kt    # Compose Modifier extensions (glass, glow, scan)
        ├── TitanColors.kt       # Color palette constants
        ├── TitanTheme.kt        # Material3 darkColorScheme
        └── TitanTypography.kt   # Typography definitions
```

### 2.3 UI Hybrid Note
The project contains **both** an XML layout (`activity_main.xml`) with ConstraintLayout, SeekBar, Buttons, CardView, SwitchMaterial, and a Compose UI tree in `MainActivity.kt`. The Compose UI is the active path — `setContent { TitanTheme { SupremeBassScreen() } }`. The XML layout is **not inflated** by the Compose Activity and appears to be a leftover from a previous iteration. `MatrixRainView.kt` (the XML custom View) is also not used at runtime since the Compose `MatrixRain` component replaces it.

---

## 3. Complete File Inventory

### 3.1 Kotlin Source Files

| File | Lines | Purpose |
|---|---|---|
| `AdsManager.kt` | 149 | AdMob singleton: initializes SDK, preloads interstitial + rewarded ads, provides banner creation and show methods. Ad unit IDs are placeholder (`XXXXXXXXXXXXXXXX`). |
| `AudioEngine.kt` | 104 | Wraps `android.media.audiofx.LoudnessEnhancer` on global session 0. Manages lifecycle (create/start/stop), 5-second health-check heartbeat, gain multiplication by 60. |
| `AudioService.kt` | 86 | Foreground service (`FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK`). Creates `AudioEngine`, acquires `PARTIAL_WAKE_LOCK` (1 hour max), shows persistent notification, handles `GAIN` intent extra. Returns `START_STICKY`. |
| `MainActivity.kt` | 660 | Single Activity. Calls `AdsManager.initialize()`. Full Compose UI: BreathingText title, NeonSwitch toggle, volume display, AudioVisualizer, preset grid (10 presets via LazyVerticalGrid), Slider (0–300), WarningCard, BannerAd at bottom. Contains utility functions `getWarningColor`, `getStatusText`, `getPresetColor`, `noRippleClickable`. |
| `MatrixRainView.kt` | 132 | Custom `View` subclass for Matrix-style rain animation. 35 drops, Japanese katakana + symbols, neon colors, continuous `invalidate()`. **Not used at runtime** (Compose `MatrixRain` replaces it). |
| `ui/components/AudioVisualizer.kt` | 105 | `@Composable` Canvas-based 32-bar visualizer. Uses sine-wave math for bar heights, color shifts based on gain level, glow overlay for high-intensity bars. Purely cosmetic (not connected to real audio data). |
| `ui/components/BreathingText.kt` | 98 | `@Composable` text with animated scale, glow alpha, and glow radius. Renders two layers (outer glow + main) for neon effect. |
| `ui/components/MatrixRain.kt` | 114 | `@Composable` Canvas-based matrix rain. 45 columns, 65 rows, 45ms tick delay, 18-char trail per column. Uses `NativePaint` for text rendering with glow. |
| `ui/components/NeonSwitch.kt` | 203 | `@Composable` custom toggle with animated thumb, breathing scale, color transition, pulsing indicator dot. |
| `ui/theme/GlassModifiers.kt` | 304 | Compose `Modifier` extensions: `premiumGlass`, `reactorGlass`, `neonGlass`, `pulsingNeonBorder`, `breathingGlow`, `scanLineOverlay`. All use `composed {}` with infinite animations. |
| `ui/theme/TitanColors.kt` | 53 | Color constants: AbsoluteBlack, NeonCyan, NeonRed, RadioactiveGreen, etc. Gradient factory methods. |
| `ui/theme/TitanTheme.kt` | 28 | `TitanTheme` composable wrapping `MaterialTheme` with custom `darkColorScheme`. |
| `ui/theme/TitanTypography.kt` | 34 | Custom `Typography` definition with 4 text styles. |

### 3.2 Build & Config Files

| File | Purpose |
|---|---|
| `app/build.gradle.kts` | App module build config: SDK versions, signing config, Compose, dependencies |
| `build.gradle.kts` | Root build file: AGP 8.3.2, Kotlin 1.9.20 |
| `settings.gradle.kts` | Repository config, root project name, module include |
| `gradle.properties` | JVM args (4GB), parallel build, configuration cache, daemon, incremental Kotlin, AndroidX, Jetifier, Homebrew JDK 17 |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle 8.6 distribution |
| `app/proguard-rules.pro` | ProGuard keep rules for AdMob, Compose, Kotlin |

### 3.3 Manifest

| File | Key Details |
|---|---|
| `AndroidManifest.xml` | 7 permissions, 1 Activity (launcher, exported), 1 Service (foreground mediaPlayback), AdMob test App ID, `allowBackup=true`, single-locale theme |

### 3.4 Resources

| File | Purpose |
|---|---|
| `res/layout/activity_main.xml` | Legacy XML layout (NOT used by Compose Activity). Contains MatrixRainView, SeekBar, preset Buttons, warning CardView. |
| `res/values/colors.xml` | 11 color definitions including neon_cyan, neon_red, glass_grey |
| `res/values/themes.xml` | `Theme.SupremeBass` (MaterialComponents.DayNight.NoActionBar) + `GlassPanel` style |
| `res/values/dimens.xml` | 8 dimension values for default screen size |
| `res/values-sw360dp/dimens.xml` | Dimension overrides for small screens |
| `res/values-sw600dp/dimens.xml` | Dimension overrides for tablets |
| `res/drawable/bg_glass.xml` | Shape drawable: grey solid, cyan border, 16dp corners |
| `res/drawable/ic_bass_logo.xml` | Vector drawable: 108dp app icon with cyan circle, red center, white crosshairs |

---

## 4. Audio Flow Analysis

### 4.1 Architecture
```
MainActivity (Compose UI)
    │
    ├─ Toggle ON  → startService(AudioService) + Intent("GAIN", gainValue)
    ├─ Gain Change → startService(AudioService) + Intent("GAIN", newGain)
    └─ Toggle OFF → stopService(AudioService)
         │
         ▼
AudioService (Foreground, START_STICKY)
    ├─ acquireWakeLock(PARTIAL, 1 hour)
    ├─ createNotification("Supreme Bass Active")
    ├─ AudioEngine(context).startSession()
    └─ AudioEngine.setGain(gain) on each onStartCommand
         │
         ▼
AudioEngine
    ├─ LoudnessEnhancer(SESSION_ID=0)  ← GLOBAL SESSION
    ├─ enabled = true
    ├─ setTargetGain(gainValue * 60)
    ├─ 5-second health-check loop (re-create if dead)
    └─ stopSession() → release()
```

### 4.2 Session 0 Critical Detail
- **`SESSION_ID = 0`** means the `LoudnessEnhancer` attaches to the **global audio output session**.
- This affects ALL audio playing on the device, not just the app's own audio.
- The app does not produce any audio itself — it boosts the entire system output.
- **This is the core functionality and the primary risk factor.**

### 4.3 Gain Calculation
- UI slider range: `0f..300f`
- Presets: 100, 125, 150, 175, 200, 225, 250, 275, 300, 400 (displayed as volume %)
- Internal gain: `(preset - 100) * 60` → `setTargetGain(gainValue * 60)`
- At 300% boost: gain = 200 * 60 = **12,000 millibels** (~30 dB boost)
- At 400% (slider max 300 + base 100): gain = 300 * 60 = **18,000 millibels** (~45 dB boost)

### 4.4 Health-Check Loop
- `Handler.postDelayed(healthCheckRunnable, 5000)` — runs every 5 seconds
- Checks if `LoudnessEnhancer` is still valid by doing a no-op assignment
- If dead, recreates the enhancer and re-applies gain
- Runs on `Looper.getMainLooper()` (main thread)

### 4.5 WakeLock
- `PARTIAL_WAKE_LOCK` acquired with 1-hour timeout
- Tag: `"SupremeBass::AudioEngineWakeLock"`
- Released in `onDestroy()`
- Acquire failure is silently caught

### 4.6 Notification
- Channel: `"SupremeBassChannel"`, importance `LOW`
- Title: `"Supreme Bass Active"`
- Text: `"Hyper-Drive Audio Boost is Engine On."`
- Icon: `android.R.drawable.ic_media_play` (system default)

---

## 5. Current Dependencies

### 5.1 App Module (`app/build.gradle.kts`)

| Dependency | Version | Purpose |
|---|---|---|
| `androidx.core:core-ktx` | 1.12.0 | Kotlin extensions for Android core |
| `androidx.appcompat:appcompat` | 1.6.1 | Backward-compatible Activity support |
| `androidx.activity:activity-compose` | 1.8.1 | Compose integration with ComponentActivity |
| `androidx.compose:compose-bom` | 2024.02.02 | Compose Bill of Materials |
| `androidx.compose.ui:ui` | (from BOM) | Compose UI core |
| `androidx.compose.ui:ui-graphics` | (from BOM) | Compose graphics |
| `androidx.compose.ui:ui-tooling-preview` | (from BOM) | Compose preview support |
| `androidx.compose.material3:material3` | (from BOM) | Material 3 components |
| `androidx.compose.material:material-icons-extended` | (from BOM) | Full Material icon set |
| `com.google.android.material:material` | 1.11.0 | Material Components for XML views |
| `com.google.android.gms:play-services-ads` | 23.1.0 | Google AdMob SDK |

### 5.2 Root Module (`build.gradle.kts`)

| Plugin | Version |
|---|---|
| `com.android.application` | 8.3.2 |
| `org.jetbrains.kotlin.android` | 1.9.20 |

### 5.3 Gradle

| Component | Version |
|---|---|
| Gradle | 8.6 |
| AGP | 8.3.2 |
| Kotlin | 1.9.20 |
| Compose Compiler | 1.5.4 |

---

## 6. Permissions

### 6.1 Declared Permissions (AndroidManifest.xml)

| Permission | Purpose | Risk Level |
|---|---|---|
| `MODIFY_AUDIO_SETTINGS` | Required for `LoudnessEnhancer` to modify audio output | Medium — affects all audio |
| `FOREGROUND_SERVICE` | Required to run foreground service | Low — standard |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Required for Android 14+ foreground service type | Low — standard |
| `POST_NOTIFICATIONS` | Required for Android 13+ notification display | Low — standard |
| `WAKE_LOCK` | Keeps CPU running for audio processing | Medium — battery impact |
| `INTERNET` | Required for AdMob ad loading | Medium — network access |
| `ACCESS_NETWORK_STATE` | Required for AdMob network status checks | Low — standard |

### 6.2 Permission Analysis
- **No dangerous permissions** declared (no camera, microphone, location, contacts, etc.).
- `MODIFY_AUDIO_SETTINGS` is a **normal permission** — auto-granted.
- `INTERNET` is a **normal permission** — auto-granted.
- `POST_NOTIFICATIONS` is a **dangerous permission** on Android 13+ — requires runtime request. **Not handled in code** — `MainActivity.kt` does not call `requestPermission()`.

---

## 7. Network Usage

### 7.1 Network Calls
- **AdMob SDK** makes HTTP/S requests to load ads (banner, interstitial, rewarded).
- No direct network calls in app code.
- Network calls are entirely managed by `com.google.android.gms:play-services-ads:23.1.0`.

### 7.2 Data Sent
- Device advertising ID (GAID) to Google AdMob.
- Standard ad request metadata (device model, OS version, locale).
- No user-generated data sent from app code.

### 7.3 Data Received
- Ad content (HTML/images/video) from AdMob servers.
- Ad configuration and targeting data.

---

## 8. Storage Usage

### 8.1 Disk Writes
- **No SharedPreferences** usage in app code.
- **No database** (Room, SQLite, etc.).
- **No file I/O** beyond AdMob SDK internal caching.
- **No downloads** or media storage.

### 8.2 Disk Reads
- No asset files read.
- No configuration files read.

### 8.3 Backup
- `android:allowBackup="true"` in manifest — system backup includes app data (minimal since no local data is stored).

---

## 9. Privacy Policy

**STATUS: NONE**

- No privacy policy exists in the codebase.
- No privacy policy URL is referenced.
- **REQUIRED FOR PLAY STORE** — Google Play requires a privacy policy URL for apps that:
  - Access network (`INTERNET` permission) ✓
  - Use advertising SDKs ✓
  - Access device identifiers (AdMob GAID) ✓

---

## 10. Analytics / Crash / Ads

### 10.1 Analytics
**NONE** — No Firebase Analytics, Mixpanel, Amplitude, or any analytics SDK present.

### 10.2 Crash Reporting
**NONE** — No Firebase Crashlytics, Sentry, Bugsnag, or any crash reporting SDK present.

### 10.3 Ads
**YES — AdMob SDK integrated but NOT production-ready:**
- `AdsManager.kt` contains AdMob code for banner, interstitial, and rewarded ads.
- **All ad unit IDs are placeholders:** `ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX`
- **App ID in manifest is a TEST ID:** `ca-app-pub-3940256099942544~3347511713` (Google's sample test ID)
- `AdsManager.initialize()` is called in `MainActivity.onCreate()`.
- Banner ad is displayed at the bottom of the screen via `AndroidView` in Compose.
- Interstitial ads shown every 3 preset changes.
- Rewarded ad support exists but is **never called** from UI code.
- **Will not serve real ads without production ad unit IDs.**

---

## 11. Build Variants

### 11.1 Configured Build Types

| Type | Minify | ProGuard | Signing |
|---|---|---|---|
| `debug` | No (default) | No | Debug key |
| `release` | Yes (`isMinifyEnabled = true`) | `proguard-android-optimize.txt` + `proguard-rules.pro` | Release keystore |

### 11.2 Signing Config
- **Store file:** `release-key.jks`
- **Store password:** `supreme2026` (hardcoded in build script)
- **Key alias:** `supremebass`
- **Key password:** `supreme2026` (hardcoded in build script)

### 11.3 ProGuard Rules
- Keeps AdMob classes (`com.google.android.gms.ads.**`)
- Keeps Google Ads classes (`com.google.ads.**`)
- Keeps Google Play Services common (`com.google.android.gms.common.**`)
- Suppresses Compose warnings
- Suppresses Kotlin warnings, keeps `kotlin.Metadata`

---

## 12. Risks Identified

### 12.1 CRITICAL — Signing Credentials Hardcoded
- **File:** `app/build.gradle.kts:21-27`
- **Issue:** Release keystore passwords (`supreme2026`) are hardcoded in plaintext in the build script.
- **Impact:** Anyone with repo access can sign release builds. Credentials are in version control history.
- **Severity:** CRITICAL

### 12.2 CRITICAL — Global Audio Session 0
- **File:** `AudioEngine.kt:11`
- **Issue:** `SESSION_ID = 0` applies gain to ALL system audio output.
- **Impact:** Boosts media, notifications, calls, alarms — not just music. Can cause speaker damage at high levels.
- **Severity:** CRITICAL

### 12.3 HIGH — Extreme Gain Without Warning
- **File:** `MainActivity.kt:377`
- **Issue:** Slider allows up to 300% gain (18,000 millibels / ~45 dB). No user-facing safety warning on first use.
- **Impact:** Potential hearing damage, speaker damage. WarningCard appears only after gain > 100 and is purely cosmetic.
- **Severity:** HIGH

### 12.4 HIGH — No Privacy Policy
- **File:** N/A
- **Issue:** App uses INTERNET + AdMob (collects GAID) but has no privacy policy.
- **Impact:** Google Play will reject the listing.
- **Severity:** HIGH

### 12.5 HIGH — POST_NOTIFICATIONS Not Requested at Runtime
- **File:** `MainActivity.kt`
- **Issue:** Android 13+ requires runtime permission for notifications. No `requestPermission()` call.
- **Impact:** Foreground service notification may not display on Android 13+ without user manually enabling it.
- **Severity:** HIGH

### 12.6 MEDIUM — AdMob Test IDs
- **File:** `AdsManager.kt:27-29`, `AndroidManifest.xml:23`
- **Issue:** All ad unit IDs are placeholders. Manifest uses Google's test App ID.
- **Impact:** No real ads will serve. App will not generate revenue.
- **Severity:** MEDIUM

### 12.7 MEDIUM — WakeLock Without Timeout Strategy
- **File:** `AudioService.kt:53-57`
- **Issue:** WakeLock acquired for 1 hour but `START_STICKY` service can run indefinitely.
- **Impact:** Battery drain if user forgets to disable boost. No auto-shutoff mechanism.
- **Severity:** MEDIUM

### 12.8 MEDIUM — AudioVisualizer Is Fake
- **File:** `ui/components/AudioVisualizer.kt`
- **Issue:** Visualizer uses sine-wave math, not real audio analysis. Does not reflect actual audio output.
- **Impact:** User may trust visual feedback as accurate audio representation.
- **Severity:** MEDIUM

### 12.9 MEDIUM — Matrix Rain Performance
- **File:** `ui/components/MatrixRain.kt:43-48`
- **Issue:** `LaunchedEffect` runs `while(true)` with 45ms delay, updating 45 column states. Canvas redraws continuously.
- **Impact:** Continuous GPU/CPU usage for background animation even when not visible.
- **Severity:** MEDIUM

### 12.10 MEDIUM — GlassModifiers Heavy Animations
- **File:** `ui/theme/GlassModifiers.kt`
- **Issue:** Every `composed {}` modifier creates its own `InfiniteTransition`. The main screen uses `reactorGlass`, `neonGlass`, `breathingGlow`, and `scanLineOverlay` simultaneously.
- **Impact:** Multiple concurrent infinite animations consuming CPU/GPU resources.
- **Severity:** MEDIUM

### 12.11 LOW — Duplicate Matrix Rain Implementations
- **Files:** `MatrixRainView.kt` (custom View, 132 lines) and `ui/components/MatrixRain.kt` (Compose, 114 lines)
- **Issue:** Two implementations of the same effect. The View version is dead code.
- **Impact:** Confusion, bloated APK, maintenance overhead.
- **Severity:** LOW

### 12.12 LOW — Unused XML Layout
- **File:** `res/layout/activity_main.xml`
- **Issue:** 83-line XML layout never inflated by Compose Activity.
- **Impact:** Dead resource in APK.
- **Severity:** LOW

### 12.13 LOW — `android.enableJetifier=true`
- **File:** `gradle.properties:20`
- **Issue:** Jetifier is enabled but all dependencies are AndroidX-native. No support library dependencies exist.
- **Impact:** Unnecessary build step, slower builds.
- **Severity:** LOW

### 12.14 LOW — No Unit Tests
- **Issue:** No test files exist in the project. `testInstrumentationRunner` is configured but no tests written.
- **Impact:** No regression protection.
- **Severity:** LOW

### 12.15 INFO — Rewarded Ad Never Triggered
- **File:** `AdsManager.kt:138-148`
- **Issue:** `showRewardedIfReady()` exists but is never called from any UI code.
- **Impact:** Dead code path.
- **Severity:** INFO

---

## 13. Proposed Changes

### 13.1 Must-Fix (Pre-Launch)

| # | Change | Files | Priority |
|---|---|---|---|
| 1 | **Move signing credentials to environment variables or `local.properties`** | `app/build.gradle.kts` | CRITICAL |
| 2 | **Add privacy policy URL** to Play Store listing and in-app disclosure | `AndroidManifest.xml` (new meta-data), new `privacy_policy.html` or hosted URL | CRITICAL |
| 3 | **Request POST_NOTIFICATIONS at runtime** on Android 13+ | `MainActivity.kt` | HIGH |
| 4 | **Replace AdMob test IDs with production IDs** | `AdsManager.kt`, `AndroidManifest.xml` | HIGH |
| 5 | **Add first-run safety disclaimer** explaining speaker/hearing risks | `MainActivity.kt` | HIGH |

### 13.2 Should-Fix (Quality)

| # | Change | Files | Priority |
|---|---|---|---|
| 6 | **Remove dead code**: `MatrixRainView.kt`, `activity_main.xml` | Delete files | MEDIUM |
| 7 | **Add auto-shutoff timer** (e.g., 30 min max) or battery-aware shutdown | `AudioService.kt`, `MainActivity.kt` | MEDIUM |
| 8 | **Consolidate infinite transitions** — share a single `InfiniteTransition` where possible | `GlassModifiers.kt`, `MainActivity.kt` | MEDIUM |
| 9 | **Add `android:exported="false"` intent filter documentation** or remove unused intent filters | `AndroidManifest.xml` | LOW |
| 10 | **Remove Jetifier** | `gradle.properties` | LOW |

### 13.3 Nice-to-Have (Post-Launch)

| # | Change | Files | Priority |
|---|---|---|---|
| 11 | **Add unit tests** for `AudioEngine` gain calculation logic | New test files | LOW |
| 12 | **Connect AudioVisualizer to real audio metering** (requires `Visualizer` API + `RECORD_AUDIO` permission) | `AudioEngine.kt`, `AudioVisualizer.kt` | LOW |
| 13 | **Add equalizer presets** (bass boost, treble, etc.) using `Equalizer` FX | New `EqualizerEngine.kt` | LOW |
| 14 | **Consider separate audio session** instead of global session 0 for safer operation | `AudioEngine.kt` | LOW |

---

## 14. New Dependencies Needed

### 14.1 Required for Launch

| Dependency | Purpose | When |
|---|---|---|
| None strictly required | Privacy policy is a URL, not a dependency | Pre-launch |

### 14.2 Recommended

| Dependency | Purpose | Priority |
|---|---|---|
| `androidx.compose.material3:material3-window-size-class` | Better responsive layout for tablets/foldables | Medium |
| `androidx.lifecycle:lifecycle-runtime-compose` | `collectAsStateWithLifecycle` for safer state collection | Medium |
| `androidx.core:core-ktx:1.13+` | Latest AndroidX core with notification permission helpers | Medium |
| Firebase Crashlytics (optional) | Crash reporting for production monitoring | Low |
| Firebase Analytics (optional) | Usage analytics for product decisions | Low |

### 14.3 NOT Needed
- Room / SQLite (no local data)
- Retrofit / OkHttp (no API calls beyond AdMob)
- Hilt / Koin (single-activity, no DI complexity)
- Navigation (single screen)

---

## 15. Privacy Impact Assessment

### 15.1 Data Collection

| Data Type | Collected? | Sent? | By Whom |
|---|---|---|---|
| Advertising ID (GAID) | Yes | Yes | AdMob SDK |
| Device info (model, OS) | Yes | Yes | AdMob SDK |
| IP Address | Yes | Yes | AdMob SDK (standard HTTP) |
| Audio data | No | No | — |
| User input | No | No | — |
| Location | No | No | — |
| Contacts | No | No | — |
| Camera/Microphone | No | No | — |

### 15.2 Privacy Requirements
- [ ] Privacy policy URL must be provided to Google Play
- [ ] Privacy policy must disclose AdMob data collection
- [ ] Privacy policy must disclose GAID usage
- [ ] Privacy policy must disclose network access purpose
- [ ] Data safety form must be filled in Google Play Console
- [ ] Consent for EU users (GDPR) — AdMob provides built-in consent form via UMP SDK

### 15.3 GDPR Considerations
- AdMob serves personalized ads by default in EEA/UK.
- **Without UMP (User Messaging Platform) SDK**, the app cannot obtain GDPR consent.
- **Recommended:** Add `com.google.android.ump:user-messaging-platform` for EU compliance.

### 15.4 CCPA Considerations
- California users must be able to opt out of data sale.
- AdMob provides `setRequestConfiguration` to set `setTagForUnderAgeOfConsent` and `setRequestAgent`.
- Should implement "Do Not Sell" option if targeting California users.

---

## 16. Performance Impact

### 16.1 CPU/GPU

| Component | Impact | Detail |
|---|---|---|
| MatrixRain Composable | HIGH | 45ms tick loop, 45 columns, continuous Canvas redraw |
| AudioVisualizer | MEDIUM | 32-bar Canvas, sine-wave math every frame |
| GlassModifiers (6+ instances) | MEDIUM | Multiple `InfiniteTransition` instances |
| BreathingText | LOW | Simple scale/alpha animation |
| NeonSwitch | LOW | Single toggle animation |

### 16.2 Memory

| Component | Impact | Detail |
|---|---|---|
| AdMob SDK | HIGH | ~15-30 MB baseline for ad loading |
| Compose runtime | MEDIUM | State management for animations |
| LoudnessEnhancer | LOW | System-level audio effect, minimal app memory |
| MatrixRain state | LOW | 45 mutable state objects |

### 16.3 Battery

| Component | Impact | Detail |
|---|---|---|
| PARTIAL_WAKE_LOCK | HIGH | Prevents CPU sleep for 1 hour (extendable via START_STICKY) |
| Continuous animations | MEDIUM | GPU rendering even when screen is on but app is backgrounded |
| AdMob network polling | LOW | Periodic ad refresh requests |
| Foreground service | LOW | Persistent notification (minor overhead) |

### 16.4 Network

| Component | Impact | Detail |
|---|---|---|
| AdMob ad requests | LOW-MEDIUM | Banner refresh every ~60s, interstitial preloaded on-demand |
| No other network usage | — | — |

### 16.5 Build Performance
- Gradle configured aggressively: 4GB JVM, parallel builds, configuration cache, build cache.
- Jetifier adds ~2-5s to builds unnecessarily.
- No significant build issues identified.

---

## 17. Play Store Checklist

### 17.1 Store Listing Requirements

| Requirement | Status | Notes |
|---|---|---|
| App name | ✅ "Supreme Bass" | |
| Icon | ✅ `ic_bass_logo.xml` (vector) | Vector drawable works for store listing |
| Feature graphic | ❌ Missing | Required for store listing |
| Screenshots | ❌ Missing | At least 2 required, 8 recommended |
| Short description | ❌ Missing | 80 char max |
| Full description | ❌ Missing | 4000 char max |
| Category | ❌ Not set | Suggest: Music & Audio or Tools |
| Privacy policy URL | ❌ Missing | **REQUIRED** — app uses internet + ads |
| Content rating | ❌ Not set | Must complete IARC questionnaire |
| Data safety form | ❌ Not set | Must declare AdMob data collection |
| Target audience | ❌ Not set | Must declare if targeting children (COPPA) |

### 17.2 Technical Requirements

| Requirement | Status | Notes |
|---|---|---|
| targetSdk >= 33 (Android 13) | ✅ targetSdk = 34 | Exceeds requirement |
| 64-bit native libs | ✅ N/A | Pure Kotlin/Java, no native code |
| App Bundle (AAB) | ⚠️ Not confirmed | Must build as AAB for Play Store |
| ProGuard/R8 for release | ✅ `isMinifyEnabled = true` | |
| No hardcoded test IDs in release | ❌ Test AdMob App ID in manifest | Must replace with production ID |
| Signed with release key | ⚠️ Configured but credentials in plaintext | Must secure |

### 17.3 Policy Compliance

| Policy | Status | Notes |
|---|---|---|
| Audio manipulation disclosure | ❌ Missing | App modifies system audio — must disclose |
| Speaker/hearing safety warning | ❌ Missing | High gain levels pose risk |
| Advertising disclosure | ❌ Missing | Must clearly indicate ad-supported nature |
| Interstitial ad frequency | ⚠️ Every 3 preset changes | May be flagged as aggressive |
| Rewarded ad transparency | ✅ Not shown to users | Rewarded path exists but unused |

### 17.4 Pre-Launch Checklist

- [ ] Replace AdMob test IDs with production IDs
- [ ] Create and host privacy policy
- [ ] Add privacy policy URL to Play Store listing
- [ ] Fill in Data Safety form in Play Console
- [ ] Complete IARC content rating questionnaire
- [ ] Create feature graphic (1024x500 px)
- [ ] Take screenshots (min 2, phone + tablet recommended)
- [ ] Write short and full descriptions
- [ ] Secure signing credentials (move to CI/CD or env vars)
- [ ] Add runtime permission request for POST_NOTIFICATIONS
- [ ] Add first-run safety disclaimer
- [ ] Build and upload AAB to Play Console
- [ ] Test on multiple screen sizes and API levels
- [ ] Verify ProGuard doesn't break AdMob (test release build)
- [ ] Consider adding UMP SDK for GDPR consent
- [ ] Consider adding auto-shutoff timer for battery safety

---

## Appendix A: Signing Credential Exposure (Critical)

The following credentials are hardcoded in `app/build.gradle.kts`:

```
storeFile = file("release-key.jks")
storePassword = "supreme2026"
keyAlias = "supremebass"
keyPassword = "supreme2026"
```

**Immediate remediation:**
1. Move to `local.properties` (gitignored) or environment variables.
2. Use `System.getenv("KEYSTORE_PASSWORD")` or `project.findProperty()` with `gradle.properties` (local, gitignored).
3. Rotate the keystore password if the repository has been shared publicly.

---

## Appendix B: Gain Safety Table

| Displayed % | Gain (mB) | Approx dB | Risk Level |
|---|---|---|---|
| 100% | 0 | 0 dB | Safe |
| 125% | 1,500 | ~3.7 dB | Low |
| 150% | 3,000 | ~7.5 dB | Low-Medium |
| 175% | 4,500 | ~11.2 dB | Medium |
| 200% | 6,000 | ~15 dB | Medium-High |
| 225% | 7,500 | ~18.7 dB | High |
| 250% | 9,000 | ~22.5 dB | High |
| 275% | 10,500 | ~26.2 dB | Very High |
| 300% | 12,000 | ~30 dB | Dangerous |
| 400% (slider max) | 18,000 | ~45 dB | Extreme |

---

*End of audit.*
