# Supreme Acoustics

**Professional bass booster with acoustic laboratory, signal generator, and device characterization.**

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg)](https://developer.android.com/about/versions/oreo)
[![Build](https://img.shields.io/badge/Build-Passing-brightgreen)]()

---

## Download

**[Download Latest APK](https://github.com/jordelmir/SupremeBass-Neon/releases/latest)**

> Requires Android 8.0 (API 26) or higher.

---

## Features

### Bass Booster & Sound Enhancer
- Precision bass boost with smooth, responsive slider (100%–400%)
- Works with all audio apps — music, videos, podcasts, games
- 10 preset levels (100%–400%) with color-coded warnings
- Real-time audio visualizer
- Auto-shutoff timer (30 minutes max)

### Signal Generator
- Generate precise audio signals for testing and calibration
- Waveforms: Sine, Square, Triangle, Sawtooth, Chirp, Noise Band
- Frequency range: 20 Hz – 20 kHz
- Configurable amplitude and modulation

### Acoustic Sweep Engine
- Automated frequency sweeps (Linear, Logarithmic, Stepped)
- Configurable dwell time and step count
- Full audible spectrum coverage

### Device Lab
- Characterize device frequency response
- Save multiple device profiles with measured points
- Export telemetry data as JSON
- Track response across output routes (speaker, headphones, Bluetooth)

### Experiment Engine
- Run controlled acoustic experiments
- Frequency response and distortion profiling
- Configurable variables, dwell time, repeats
- Experiment history with Room database persistence

### Flame Lab (Experimental)
- Acoustic flame research with safety interlocks
- Headphone detection (blocks wired/Bluetooth/USB)
- Duration limits, cooldown timers, amplitude restrictions
- First-run safety disclaimer

### Native C++ DSP Engine
- Powered by Oboe for ultra-low latency audio
- C++17 oscillator and limiter with sub-millisecond latency
- JNI bridge for Kotlin integration
- Hardware-accelerated processing

### Safety First
- Route interlock blocks unsafe audio routes
- Amplitude limiting prevents speaker damage
- Duration limits with auto-shutoff
- First-run safety disclaimer
- Gain drift detection and auto-correction
- Audio boost persists across screen-off and app switches

---

## Screenshots

| Main Screen | Signal Lab | Device Lab | Experiments | Flame Lab | Settings |
|:-----------:|:----------:|:----------:|:-----------:|:---------:|:--------:|
| ![Main](docs/screenshots/01_main.png) | ![Signal](docs/screenshots/02_signal.png) | ![Device](docs/screenshots/03_device.png) | ![Experiment](docs/screenshots/04_experiment.png) | ![Flame](docs/screenshots/05_flame.png) | ![Settings](docs/screenshots/06_settings.png) |

---

## Architecture

```
app/src/main/java/com/supremecorp/bass/
├── audio/
│   ├── backend/          # AudioTrack + Oboe backends
│   └── safety/           # AcousticSafetyController, AudioRouteMonitor
├── core/logging/         # AppLogger (structured logging)
├── cv/                   # VisualAnalyzer (CameraX)
├── data/
│   ├── ads/              # AdUnitProvider (debug/prod auto-detect)
│   ├── device/           # Room DB, DAOs, Repository
│   └── experiment/       # Room DB, DAOs, Repository
├── domain/model/         # SignalConfig, Waveform, Experiment, etc.
├── dsp/                  # Oscillator, SignalGenerator, Limiter, SweepEngine
├── experiment/           # ExperimentRunner, FlameSafetyController
├── infrastructure/       # TelemetryExporter
├── signal/               # SignalEngine, SignalEngineState
├── ui/
│   ├── cv/               # VisualLabScreen
│   ├── device/           # DeviceLabScreen, DeviceLabViewModel
│   ├── experiment/       # ExperimentLabScreen, FlameLabScreen
│   ├── navigation/       # 6-tab navigation
│   ├── settings/         # SettingsScreen (Oboe toggle, safety)
│   └── signal/           # SignalLabScreen, SignalLabViewModel
└── MainActivity.kt       # Main entry point
```

### Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin 1.9.20 |
| UI | Jetpack Compose (BOM 2024.02.02) |
| Architecture | Clean Architecture |
| Database | Room 2.6.1 |
| Native DSP | C++17 via Oboe 1.8.0 |
| Build | Gradle 8.6, AGP 8.3.2 |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 (Android 15) |

---

## Build

### Prerequisites
- Android Studio Hedgehog+ 
- JDK 17
- Android SDK 35

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
Requires keystore environment variables:
```bash
export SUPREME_KEYSTORE_PASSWORD=your_password
export SUPREME_KEY_ALIAS=supremebass
export SUPREME_KEY_PASSWORD=your_password

./gradlew bundleRelease
```

### Run Tests
```bash
./gradlew testDebugUnitTest
```

---

## Logging

Filter all app logs:
```bash
adb logcat -s SupremeBass_*
```

| TAG | Module |
|-----|--------|
| `SupremeBass_MainActivity` | Activity lifecycle, permissions |
| `SupremeBass_Service` | Foreground service state |
| `SupremeBass_Engine` | Audio effects (LoudnessEnhancer, Equalizer) |
| `SupremeBass_Persistence` | SharedPreferences state |
| `SupremeBass_SignalEngine` | Signal generation, sessions |
| `SupremeBass_DeviceLab` | Characterization sweep |
| `SupremeBass_ExperimentRunner` | Experiment lifecycle |
| `SupremeBass_ExperimentVM` | Experiment ViewModel |
| `SupremeBass_FlameSafety` | Safety checks, violations |
| `SupremeBass_AdUnitProvider` | AdMob ID resolution |
| `SupremeBass_VisualAnalyzer` | Camera frame analysis |

---

## Permissions

| Permission | Purpose |
|-----------|---------|
| `MODIFY_AUDIO_SETTINGS` | Audio effects processing |
| `FOREGROUND_SERVICE` | Persistent boost service |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Audio boost while backgrounded |
| `POST_NOTIFICATIONS` | Boost status notification (Android 13+) |
| `WAKE_LOCK` | Keep CPU active during boost |
| `INTERNET` | AdMob advertising |
| `ACCESS_NETWORK_STATE` | AdMob connectivity check |

---

## Privacy

All audio processing occurs **locally on your device**. No audio data, recordings, or sound profiles are transmitted. Camera analysis is processed on-device only.

**[Full Privacy Policy](docs/privacy/index.html)**

---

## Play Store

| Document | Location |
|----------|----------|
| Store Listing | [docs/playstore/store_listing.md](docs/playstore/store_listing.md) |
| Content Rating | [docs/playstore/content_rating_notes.md](docs/playstore/content_rating_notes.md) |
| Data Safety | [docs/playstore/data_safety_mapping.md](docs/playstore/data_safety_mapping.md) |
| Ads Declaration | [docs/playstore/ads_declaration.md](docs/playstore/ads_declaration.md) |
| Feature Graphic | [docs/playstore/feature_graphic.html](docs/playstore/feature_graphic.html) |
| Screenshot Guide | [docs/playstore/screenshot_guide.md](docs/playstore/screenshot_guide.md) |

---

## License

```
Copyright 2026 Supreme Corp

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
