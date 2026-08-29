# SupremeBass Release Configuration

## Keystore Setup

For release builds, you need to:

1. Generate a release keystore:
```bash
keytool -genkey -v -keystore release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias supremebass
```

2. Set environment variables:
```bash
export SUPREME_KEYSTORE_PASSWORD="your_password"
export SUPREME_KEY_ALIAS="supremebass"
export SUPREME_KEY_PASSWORD="your_password"
```

3. Place `release-key.jks` in the `app/` directory.

## Play Store Metadata

### App Description
```
SupremeBass - Professional Audio Enhancement & Measurement

Transform your Android device into a professional audio workstation with real DSP processing.

Features:
• 10-Band Parametric Equalizer with ISO frequencies
• Bass Boost with low shelf filter
• Stereo Virtualizer with spatial enhancement
• Real-time Spectrum Analyzer
• Speaker Diagnostics (frequency response, THD)
• Acoustic Measurement (RT60, SPL estimation)
• 7 Professional EQ Presets
• Speaker Protection System

Technical Details:
• Real-time DSP processing at 48kHz
• Biquad filters (Audio EQ Cookbook)
• FFT-based spectrum analysis
• Safety ramps for speaker protection
• Route interlock for headphones/Bluetooth

Note: Acoustic measurements are relative without calibrated microphone.
```

### Categories
- Primary: Music & Audio
- Tags: equalizer, bass boost, audio, dsp, spectrum analyzer

### Permissions Used
- `RECORD_AUDIO` - For acoustic measurements (optional)
- `MODIFY_AUDIO_SETTINGS` - For audio routing
- `WAKE_LOCK` - For background audio processing
- `INTERNET` - For ads (can be removed)

### Privacy Policy
This app processes audio locally on your device. No audio data is transmitted or stored externally. Microphone access is only used for acoustic measurements and only when you explicitly start a measurement.
