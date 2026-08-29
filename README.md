# Supreme Acoustics

**Android bass booster with signal generator and safety features.**

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg)](https://developer.android.com/about/versions/oreo)

---

## Download

**[Download Latest APK](https://github.com/jordelmir/SupremeBass-Neon/releases/latest)**

> Requires Android 8.0 (API 26) or higher.

---

## What It Does

### Bass Booster
- Gain slider from 100% to 400% (0-30 dB boost)
- 11 presets (Flat, Warm Bass, Deep Sub, Punchy Mids, Vocal Clarity, etc.)
- Boost persists across screen-off and app switches
- Real-time audio visualizer
- Auto-shutoff after 30 minutes

### Signal Generator
- Generate test tones: Sine, Square, Triangle, Sawtooth, Chirp, Noise Band
- Frequency range: 20 Hz – 20 kHz
- Configurable amplitude and duration
- Automated frequency sweeps

### Safety Features
- Route interlock (blocks unsafe audio routes)
- Amplitude limiting
- Duration limits with auto-shutoff
- First-run safety disclaimer
- Gain drift detection

---

## Screenshots

| Main Screen | Signal Lab | Device Lab | Experiments | Flame Lab | Settings |
|:-----------:|:----------:|:----------:|:-----------:|:---------:|:--------:|
| ![Main](docs/screenshots/01_main.png) | ![Signal](docs/screenshots/02_signal.png) | ![Device](docs/screenshots/03_device.png) | ![Experiment](docs/screenshots/04_experiment.png) | ![Flame](docs/screenshots/05_flame.png) | ![Settings](docs/screenshots/06_settings.png) |

---

## Tech Stack

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
| `SupremeBass_Engine` | Audio effects |
| `SupremeBass_Persistence` | SharedPreferences state |

---

## Permissions

| Permission | Purpose |
|-----------|---------|
| `MODIFY_AUDIO_SETTINGS` | Audio effects processing |
| `FOREGROUND_SERVICE` | Persistent boost service |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Audio boost while backgrounded |
| `POST_NOTIFICATIONS` | Boost status notification (Android 13+) |
| `WAKE_LOCK` | Keep CPU active during boost |

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
