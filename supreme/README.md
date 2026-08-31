# Supreme — Everyday Intelligence Platform

The intelligence layer for the physical things around you.

## Your home. Your devices. Your vehicle. Your utilities. Your documents. Your cameras. Your maintenance. Your safety.

---

## Architecture

```
supreme/
├── core/
│   ├── universal-model/     — The ontology (Asset, Device, Observation, etc.)
│   └── device-abstraction/  — SupremeDevice interface
│
├── modules/
│   ├── fix-ai/             — "What's wrong with this?" (camera + mic + vibration)
│   ├── maintenance-os/     — "When should I maintain this?" (scheduling)
│   ├── warranty-vault/     — "Does this still have warranty?" (OCR)
│   ├── network-doctor/     — "Why is my Internet bad?" (Wi-Fi analysis)
│   ├── noise-doctor/       — "What is this sound?" (FFT + harmonics)
│   ├── vibration-doctor/   — "Is this vibrating normally?" (accelerometer)
│   ├── camera-hub/         — "Show me all my cameras" (ONVIF/RTSP)
│   ├── find/               — "Where are my keys?" (BLE/UWB)
│   ├── home-hub/           — "Control my home" (Matter/Google Home)
│   ├── utilities/          — "Track my consumption" (water/electric/gas)
│   ├── inventory/          — "What do I own?" (barcode/QR/NFC/OCR)
│   ├── vehicle-hub/        — "How is my car?" (OBD2 diagnostics)
│   ├── leak-watch/         — "Stop the leak" (sensors + valve)
│   └── emergency/          — "I need help NOW" (flashlight/SOS/contacts)
│
├── apps/
│   └── android/            — Android app (Compose UI)
│
└── supreme-guardian/       — B2B safety platform (separate)
```

## The Cycle

```
OBSERVE → UNDERSTAND → DIAGNOSE → RECOMMEND → ACT → VERIFY → REMEMBER/PREDICT
```

## The Universal Ontology

Every module speaks this language:

| Entity | Purpose |
|--------|---------|
| ASSET | Anything the user owns |
| DEVICE | Anything that produces observations |
| OBSERVATION | A reading from a device or user input |
| ANOMALY | Something abnormal detected |
| ACTION | Something done or recommended |
| EVIDENCE | Proof that something happened |
| MAINTENANCE | A task scheduled or completed |
| COST | Money spent or estimated |
| DIAGNOSIS | AI-powered cause analysis |
| WARRANTY | Warranty information |

## Example: Washing Machine Lifecycle

```
ASSET: Washing Machine
├── DEVICE: Noise sensor → OBSERVATION: 31.4 Hz, harmonics 62.7, 94.1
├── DEVICE: Vibration sensor → OBSERVATION: 0.81 g RMS (+161%)
├── DEVICE: Power monitor → OBSERVATION: 450W (+20%)
│
├── ANOMALY: Bearing wear (78% confidence)
├── DIAGNOSIS: Bearing wear → Inspect → Replace
├── ACTION: Replace bearing (₡18,000)
├── EVIDENCE: before.wav, after.wav, invoice.pdf
├── MAINTENANCE: Next in 6 months
└── COST: ₡18,000 (₡42,000 lifetime)
```

## Commercial Tiers

### Supreme Free
- Phone-only tools
- Signal, noise, vibration, network analysis
- Basic inventory, maintenance reminders
- Goal: **millions of users**

### Supreme Pro ($3.99–$7.99/month)
- AI diagnosis
- Unlimited assets
- Warranty intelligence
- Advanced measurement
- Historical analytics
- Cloud backup

### Supreme Home ($5–$15/month/home)
- Matter integration
- Camera management
- Sensor networks
- Home automations
- Safety dashboard

### Supreme Hardware
- Supreme Tag (BLE/UWB) — $15-25
- Supreme Sensor (temp/humidity/motion) — $20-30
- Supreme Hub (Matter/BLE gateway) — $50-80

### Supreme Guardian (B2B)
- Commercial/industrial safety
- Installation + license
- Higher ticket

## Tech Stack

- Android (Kotlin, Jetpack Compose)
- Material3 with dynamic color
- BLE/UWB (Android APIs)
- Matter/Google Home APIs
- CameraX / ML Kit (OCR, barcode, document scanner)
- ONVIF/RTSP (camera integration)
- Coroutines + Flow
- Room (local database)
- SupremeBass DSP engine (FFT, harmonics, spectral analysis)

## Priority Order (Completed)

1. **Fix AI + Maintenance OS** ✅
2. **Warranty + Inventory Vault** ✅
3. **Network + Noise + Vibration Doctors** ✅
4. **Camera Hub + Find** ✅
5. **Home Hub + Utilities** ✅
6. **Vehicle Hub + Leak Watch + Emergency** ✅

## Files

```
Total: ~15,000 lines of Kotlin
Modules: 14
Screens: 10
Unit tests: 66+
```
