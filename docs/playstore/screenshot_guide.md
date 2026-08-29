# Supreme Acoustics — Screenshot Guide

## How to Take Screenshots for Play Store

### Requirements
- **Minimum:** 2 screenshots
- **Recommended:** 4-8 screenshots
- **Aspect ratio:** 16:9 (portrait) or 9:16 (landscape)
- **Min dimension:** 320px, max 3840px
- **Format:** PNG or JPEG

### Screenshot List

| # | Screen | What to Capture | Tips |
|---|--------|-----------------|------|
| 1 | **Main Bass Boost** | Neon UI with slider at 200%, visualizer active | Show the dramatic glow effect |
| 2 | **Signal Lab** | Waveform selector visible, frequency set to 440Hz | Show professional UI |
| 3 | **Device Lab** | Frequency response chart or measurement results | Show data visualization |
| 4 | **Experiment Lab** | Running experiment with results | Show the scientific interface |
| 5 | **Flame Lab** | Safety controls visible, flame mode active | Show safety interlocks |
| 6 | **Settings** | Oboe backend toggle, safety settings visible | Show technical depth |

### How to Capture

#### Method 1: ADB (Recommended)
```bash
# Connect device via USB, then:
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png ./screenshots/

# Or use the keyboard shortcut:
# Power + Volume Down (simultaneously)
```

#### Method 2: Android Emulator
```bash
# In Android Studio, use the screenshot button in the emulator toolbar
# Or use: adb emu screenrecord
```

#### Method 3: Device Screenshot
- Press **Power + Volume Down** simultaneously
- Screenshots save to `Pictures/Screenshots/`

### Naming Convention
```
screenshots/
├── 01_main_bass_boost.png
├── 02_signal_lab.png
├── 03_device_lab.png
├── 04_experiment_lab.png
├── 05_flame_lab.png
└── 06_settings.png
```

### Play Store Upload
1. Go to Play Console → Store listing
2. Scroll to "Screenshots"
3. Upload all screenshots
4. Drag to reorder if needed

---

*Guide version: 1.0 | Date: August 29, 2026*
