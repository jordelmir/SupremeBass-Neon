# SupremeBass Neon — Audio DSP Platform

> **Status**: Active Development | **Last Updated**: 2026-09-01 | **Commits**: 10 since P0 Truth Freeze

## Overview

SupremeBass is a professional-grade audio DSP (Digital Signal Processing) platform for Android. It provides real-time audio enhancement with background audio boost, advanced signal analysis, device diagnostics, and AI-powered tuning capabilities.

**Key Feature**: Background audio boost that works system-wide — YouTube, Spotify, any audio app — even with screen off.

---

## Quick Start

### Install (Debug)
```bash
# Build
cd supreme && ./gradlew :apps:android:assembleDebug

# Install on connected device
adb install -r supreme/apps/android/build/outputs/apk/debug/android-debug.apk

# Launch
adb shell am start -n com.supreme.android/.SupremeActivity
```

### Requirements
- **Android**: 8.0+ (API 26)
- **Target SDK**: 35 (Android 15)
- **Build**: Kotlin DSL, AGP 8.3.2, Kotlin 1.9.20, Compose BOM 2024.02.02

---

## Architecture

### Project Structure
```
SupremeBass/
├── supreme/                          # Main monorepo
│   ├── apps/android/                 # Android application
│   │   └── src/main/java/com/supreme/android/
│   │       ├── AudioService.kt       # Foreground service for background boost
│   │       ├── AudioStatePersistence.kt  # SharedPreferences state
│   │       ├── LegacyEffectsEngine.kt    # Android AudioEffect API (global boost)
│   │       ├── SupremeActivity.kt    # Main activity + bottom navigation
│   │       ├── SupremeApplication.kt # Application class
│   │       ├── audio/                # Audio backends
│   │       │   ├── backend/          # AndroidAudioTrack, Oboe, NativeDsp
│   │       │   └── safety/           # AcousticSafetyController, AudioRouteMonitor
│   │       ├── core/                 # Error handling, logging, Result type
│   │       ├── cv/                   # VisualAnalyzer (CameraX)
│   │       ├── data/                 # Room databases, repositories
│   │       ├── di/                   # AppContainer (manual DI)
│   │       ├── domain/model/         # 11 domain models
│   │       ├── dsp/                  # DSP engine (9 files)
│   │       ├── experiment/           # ExperimentRunner, FlameSafetyController
│   │       ├── infrastructure/       # TelemetryExporter
│   │       ├── navigation/           # SupremeNavGraph (12 routes)
│   │       ├── permissions/          # PermissionHelper
│   │       ├── signal/               # SignalEngine, SignalEngineState
│   │       ├── ui/                   # 15 screen packages
│   │       │   ├── boost/            # BoostScreen (main volume control)
│   │       │   ├── camerahub/        # CameraHubScreen
│   │       │   ├── components/       # AudioVisualizer, BreathingText, MatrixRain, NeonSwitch
│   │       │   ├── cv/               # VisualLabScreen
│   │       │   ├── device/           # DeviceLabScreen + ViewModel
│   │       │   ├── experiment/       # ExperimentLabScreen, FlameLabScreen
│   │       │   ├── find/             # FindScreen
│   │       │   ├── fix/              # FixScreen
│   │       │   ├── home/             # HomeScreen, AssetsScreen, SettingsScreen
│   │       │   ├── homehub/          # HomeHubScreen
│   │       │   ├── maintenance/      # MaintenanceScreen
│   │       │   ├── network/          # NetworkScreen
│   │       │   ├── noise/            # NoiseScreen
│   │       │   ├── settings/         # SettingsScreen
│   │       │   ├── signal/           # SignalLabScreen + ViewModel
│   │       │   ├── vibration/        # VibrationScreen
│   │       │   └── warranty/         # WarrantyScreen
│   │       ├── viewmodel/            # 9 ViewModels
│   │       └── ui/theme/             # Visual identity system
│   │           ├── TitanColors.kt    # Neon color palette
│   │           ├── Typography.kt     # JetBrains Mono + Orbitron
│   │           ├── GlassModifiers.kt # premiumGlass, neonGlass, reactorGlass, etc.
│   │           ├── NeonComponents.kt # NeonButton, NeonCard, NeonPresetButton, NeonSlider
│   │           └── Theme.kt         # SupremeTheme (dark mode)
│   ├── core/                         # Shared modules
│   │   ├── universal-model/          # PhysicalGraph, TimeMachine, AdapterContracts
│   │   ├── device-abstraction/       # Device abstraction layer
│   │   └── truth/                    # TruthAuthority, Observation, CommandPhase
│   └── modules/                      # Feature modules
│       ├── fix-ai/                   # AI-powered device diagnostics
│       ├── maintenance-os/           # Maintenance scheduling
│       ├── warranty-vault/           # Warranty tracking
│       ├── network-doctor/           # Network diagnostics
│       ├── noise-doctor/             # Noise analysis
│       ├── vibration-doctor/         # Vibration analysis
│       ├── camera-hub/               # Camera utilities
│       ├── find/                     # Device finder
│       ├── home-hub/                 # Smart home integration
│       ├── utilities/                # Utility tools
│       ├── inventory/                # Asset inventory
│       ├── vehicle-hub/              # Vehicle diagnostics
│       ├── leak-watch/               # Leak detection
│       └── emergency/                # Emergency features
├── .github/                          # GitHub config
│   ├── workflows/build.yml           # CI/CD pipeline
│   └── branch-protection.md          # Branch protection rules
└── README.md
```

### Build System
- **Gradle**: 8.6 with Kotlin DSL
- **AGP**: 8.3.2
- **Kotlin**: 1.9.20
- **Compose BOM**: 2024.02.02
- **Compose Compiler**: 1.5.4
- **compileSdk/targetSdk**: 35
- **minSdk**: 26

### Dependencies
- **Core**: androidx.core, lifecycle, activity-compose
- **UI**: Jetpack Compose (Material3, Material Icons Extended)
- **Navigation**: navigation-compose 2.7.6
- **Camera**: CameraX 1.3.1
- **ML**: ML Kit (barcode, text recognition, document scanner)
- **Database**: Room 2.6.1
- **Coroutines**: kotlinx-coroutines-android 1.7.3
- **Location**: play-services-location 21.1.0

---

## Features

### 1. Audio Boost (Background Service)
**Status**: ✅ Fully Functional

- **Global audio boost** via Android `LoudnessEnhancer` (session 0)
- **Background operation** — works with screen off, YouTube, any app
- **Persistent notification** with percentage display + Stop button
- **WakeLock** prevents CPU sleep
- **START_STICKY** ensures service restart if killed
- **Auto-recovery** when Android kills effects (polling every 300ms)
- **YouTube detection** — detects video changes via `playbackCallback`
- **Headphone detection** — detects plug/unplug via `deviceCallback`

**How it works**:
1. User enables boost via BoostScreen slider/presets
2. `AudioService` starts as foreground service
3. `LegacyEffectsEngine` creates `LoudnessEnhancer` + `Equalizer` on global session 0
4. Effects modify all audio output system-wide
5. Service survives app dismissal, screen off, and app switching

### 2. DSP Engine
**Status**: ✅ Fully Implemented (9 files)

| Module | File | Description |
|--------|------|-------------|
| BiquadFilter | `dsp/BiquadFilter.kt` | IIR filter implementation |
| ParametricEQ | `dsp/ParametricEQ.kt` | Multi-band equalizer |
| BassBoost | `dsp/BassBoost.kt` | Low-frequency enhancement |
| Virtualizer | `dsp/Virtualizer.kt` | Spatial audio virtualization |
| Limiter | `dsp/Limiter.kt` | Dynamic range limiting |
| Oscillator | `dsp/Oscillator.kt` | Signal generation |
| SignalGenerator | `dsp/SignalGenerator.kt` | Test signal generation |
| SweepEngine | `dsp/SweepEngine.kt` | Frequency sweep |
| AudioDSPChain | `dsp/AudioDSPChain.kt` | DSP pipeline orchestration |
| EqualizerPreset | `dsp/eq/EqualizerPreset.kt` | Preset configurations |

### 3. Visual Identity (Neon Theme)
**Status**: ✅ Applied Across All Screens

| Component | File | Description |
|-----------|------|-------------|
| TitanColors | `ui/theme/TitanColors.kt` | Neon palette (NeonCyan, NeonPink, etc.) |
| Typography | `ui/theme/Typography.kt` | JetBrains Mono + Orbitron fonts |
| GlassModifiers | `ui/theme/GlassModifiers.kt` | premiumGlass, neonGlass, reactorGlass, pulsingNeonBorder, breathingGlow, scanLineOverlay |
| NeonComponents | `ui/theme/NeonComponents.kt` | NeonButton, NeonCard, NeonPresetButton, NeonSlider |
| Theme | `ui/theme/Theme.kt` | SupremeTheme (dark mode) |

### 4. UI Screens (15 screens)
**Status**: ✅ All Implemented

| Screen | Route | Description |
|--------|-------|-------------|
| BoostScreen | `home` | Main volume boost control (MatrixRain, presets, slider) |
| HomeScreen | `modules` | Module hub (Fix AI, Tools, Assets, etc.) |
| SignalLabScreen | `signal` | Signal analysis and DSP controls |
| FixScreen | `fix` | AI-powered device diagnostics |
| DeviceLabScreen | `device` | Device information and testing |
| ExperimentLabScreen | `experiment` | Audio experiments |
| FlameLabScreen | `flame` | Flame analysis |
| VisualLabScreen | `visual` | Computer vision lab |
| SettingsScreen | `settings` | App settings |
| NetworkScreen | `tools/network` | Network diagnostics |
| NoiseScreen | `tools/noise` | Noise analysis |
| VibrationScreen | `tools/vibration` | Vibration analysis |
| AssetsScreen | `assets` | Asset inventory |
| MaintenanceScreen | `maintenance` | Maintenance scheduling |
| WarrantyScreen | `warranty` | Warranty tracking |

### 5. UI Components
**Status**: ✅ All Implemented

| Component | File | Description |
|-----------|------|-------------|
| AudioVisualizer | `components/AudioVisualizer.kt` | Real-time audio visualization |
| BreathingText | `components/BreathingText.kt` | Animated text with breathing effect |
| MatrixRain | `components/MatrixRain.kt` | Matrix-style rain background |
| NeonSwitch | `components/NeonSwitch.kt` | Toggle switch with neon glow |
| SharedComponents | `components/SharedComponents.kt` | Reusable UI components |

### 6. P1 Foundations (Multiplicative Systems)
**Status**: ✅ Implemented

| System | File | Description |
|--------|------|-------------|
| Physical Knowledge Graph | `core/universal-model/PhysicalGraph.kt` | Device relationship mapping |
| Time Machine | `core/universal-model/TimeMachine.kt` | Event sourcing |
| Adapter Contracts | `core/universal-model/AdapterContracts.kt` | DeviceAdapter SDK |

### 7. Truth Core
**Status**: ✅ Implemented

| Component | File | Description |
|-----------|------|-------------|
| TruthAuthority | `core/truth/TruthAuthority.kt` | Ground truth validation |
| Observation | `core/truth/Observation.kt` | Typed observation wrapper |
| CommandPhase | `core/truth/CommandPhase.kt` | Command lifecycle |
| MeasurementUnits | `core/truth/MeasurementUnits.kt` | Standardized units |
| Money | `core/truth/Money.kt` | Financial type safety |

---

## What's NOT Implemented

### 1. Oboe Backend (Stub Only)
**File**: `audio/backend/AudioOutputBackendOboe.kt`
- Stub implementation, never opens real `oboe::AudioStream`
- Would provide low-latency native audio output
- **Status**: Not started

### 2. Native DSP (JNI Bridge)
**File**: `audio/backend/NativeDsp.kt`
- JNI bridge to native C++ DSP code
- Currently a stub, no actual native code
- **Status**: Not started

### 3. CI/CD Pipeline
**File**: `.github/workflows/build.yml`
- Exists but is in wrong location (`supreme/.github/` instead of root `/.github/`)
- Needs to be moved to repo root
- **Status**: Needs fix

### 4. Branch Protection
**File**: `.github/branch-protection.md`
- Documented but not enforced via GitHub API
- **Status**: Manual only

### 5. Guardian Standalone
**Directory**: `supreme-guardian/`
- No Gradle wrapper, not compiled via main build
- Separate project, not integrated
- **Status**: Independent

### 6. Automated Testing
- Unit tests exist but coverage is minimal
- No integration tests
- No UI tests
- **Status**: Basic only

### 7. ProGuard Rules
- Release build has `isMinifyEnabled = true`
- ProGuard rules file exists but may need tuning
- **Status**: Basic

### 8. Release Signing
- Signing config reads from environment variables
- No keystore committed (correct)
- **Status**: CI/CD only

---

## Navigation

### Bottom Tabs
```
[Boost] [Modules] [Signal] [Fix] [Device] [Lab] [Settings]
```

### Routes
| Route | Screen | Parent |
|-------|--------|--------|
| `home` | BoostScreen | — |
| `modules` | HomeScreen | — |
| `fix` | FixScreen | — |
| `signal` | SignalLabScreen | — |
| `device` | DeviceLabScreen | — |
| `experiment` | ExperimentLabScreen | — |
| `flame` | FlameLabScreen | Lab |
| `visual` | VisualLabScreen | Lab |
| `settings` | SettingsScreen | — |
| `tools` | ToolsHubScreen | Modules |
| `tools/network` | NetworkScreen | Tools |
| `tools/noise` | NoiseScreen | Tools |
| `tools/vibration` | VibrationScreen | Tools |
| `assets` | AssetsScreen | Modules |
| `assets/{id}` | AssetDetailScreen | Assets |
| `maintenance` | MaintenanceScreen | Modules |
| `warranty` | WarrantyScreen | Modules |
| `homehub` | HomeHubScreen | Modules |
| `camerahub` | CameraHubScreen | Modules |
| `find` | FindScreen | Modules |

---

## Devices

### Connected via Wireless ADB
| Device | Model | ADB ID |
|--------|-------|--------|
| Honor VER_N49 | Honor | `adb-A2VQ024305000780-SoFCiE._adb-tls-connect._tcp` |
| Xiaomi M2101K6R | Redmi Note 11 Pro | `adb-cea87d5f-Vwjy6n._adb-tls-connect._tcp` |

### Tested On
- ✅ Honor VER_N49 — All features working
- ✅ Xiaomi M2101K6R — All features working (currently disconnected)

---

## Git History (Last 10 Commits)

| Hash | Message | Date |
|------|---------|------|
| `bd60932` | feat: add Modules tab to bottom navigation | 2026-09-01 |
| `3952d97` | feat: background audio boost + restored original UI | 2026-09-01 |
| `dd260ae` | feat: add Boost screen — main volume boost percentage control | 2026-09-01 |
| `3f71356` | chore: cleanup temp files | 2026-09-01 |
| `82953ee` | feat: restore all original SupremeBass DSP functions | 2026-09-01 |
| `1e807f3` | feat: SupremeBass visual identity — neon theme across entire APK | 2026-09-01 |
| `8aa30cf` | P1 foundations: Physical Knowledge Graph, Time Machine, Adapter Contracts | 2026-09-01 |
| `90c4e22` | P0 completion: CI fix, Truth Core module, Guardian truth cleanup | 2026-09-01 |
| `0be16bb` | P0 Truth Freeze: eliminate all synthetic/fake data across 10 files | 2026-09-01 |
| `ebb5d4a` | fix: resolve all compilation errors - BUILD SUCCESSFUL | 2026-09-01 |

---

## Build Commands

```bash
# Build debug APK
cd supreme && ./gradlew :apps:android:assembleDebug

# Build release APK
cd supreme && ./gradlew :apps:android:assembleRelease

# Install on device
adb install -r supreme/apps/android/build/outputs/apk/debug/android-debug.apk

# Launch app
adb shell am start -n com.supreme.android/.SupremeActivity

# Stop app
adb shell am force-stop com.supreme.android

# Check logs
adb logcat -s SupremeBass_Service:V SupremeBass_Engine:V SupremeBass_Audio:V
```

---

## Permissions (22 total)

| Permission | Purpose |
|------------|---------|
| CAMERA | CameraX for visual analysis |
| RECORD_AUDIO | Audio input for analysis |
| ACCESS_FINE_LOCATION | Location-based features |
| ACCESS_COARSE_LOCATION | Location-based features |
| ACCESS_WIFI_STATE | WiFi diagnostics |
| ACCESS_NETWORK_STATE | Network diagnostics |
| INTERNET | API calls |
| BLUETOOTH_* | Bluetooth audio routing |
| UWB_RANGING | UWB device ranging |
| NFC | NFC tag reading |
| HIGH_SAMPLING_RATE_SENSORS | High-frequency sensor data |
| CALL_PHONE | Emergency features |
| READ_PHONE_STATE | Device information |
| SEND_SMS | Emergency features |
| FLASHLIGHT | Camera flash control |
| VIBRATE | Haptic feedback |
| WAKE_LOCK | Background processing |
| FOREGROUND_SERVICE | Background audio boost |
| FOREGROUND_SERVICE_MEDIA_PLAYBACK | Audio boost service type |
| FOREGROUND_SERVICE_CONNECTED_DEVICE | Connected device service |
| READ_EXTERNAL_STORAGE | File access (legacy) |
| WRITE_EXTERNAL_STORAGE | File access (legacy) |
| CHANGE_WIFI_STATE | WiFi management |

---

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Commit Convention
- `feat:` — New feature
- `fix:` — Bug fix
- `chore:` — Maintenance
- `docs:` — Documentation
- `refactor:` — Code refactoring
- `test:` — Adding tests

---

## License

Private — All rights reserved.

---

## Support

- **GitHub Issues**: [Report bugs](https://github.com/jordelmir/SupremeBass-Neon/issues)
- **Documentation**: This file
- **Build Status**: Check GitHub Actions

---

**SupremeBass Neon** — Professional Audio DSP for Android
