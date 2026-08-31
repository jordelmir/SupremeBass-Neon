# Supreme — Everyday Intelligence Platform

The intelligence layer for the physical things around you.

## Vision

Not a collection of tools, but a platform that:

```
OBSERVE → UNDERSTAND → DIAGNOSE → RECOMMEND → ACT → VERIFY → REMEMBER/PREDICT
```

## Architecture

```
supreme/
├── core/
│   ├── universal-model/    — The ontology (Asset, Device, Observation, Anomaly, etc.)
│   └── device-abstraction/ — SupremeDevice interface (polymorphic device gateway)
│
├── modules/
│   ├── fix-ai/            — "What's wrong with this?" (camera + mic + vibration)
│   ├── maintenance-os/    — "When should I maintain this?" (scheduling + reminders)
│   ├── warranty-vault/    — "Does this still have warranty?" (OCR + document scanning)
│   ├── home-hub/          — Matter/Google Home integration
│   ├── network-doctor/    — Wi-Fi/Internet diagnosis
│   ├── noise-doctor/      — Sound analysis and diagnostics
│   ├── vibration-doctor/  — Vibration analysis
│   ├── camera-hub/        — ONVIF/RTSP camera management
│   ├── find/              — BLE/UWB object finding
│   ├── utilities/         — Water/electric/gas metering
│   ├── inventory/         — Home inventory management
│   ├── vehicle-hub/       — OBD vehicle diagnostics
│   ├── leak-watch/        — Water leak detection + auto shutoff
│   └── emergency/         — Emergency tools + safety
│
├── apps/
│   └── android/           — Android app (Compose UI)
│
└── hardware/
    ├── supreme-tag/       — BLE/UWB tracking tag
    ├── supreme-sensor/    — Temperature/humidity/motion sensor
    └── supreme-hub/       — Matter/BLE gateway
```

## The Universal Ontology

Every module speaks this language:

```kotlin
ASSET       — anything the user owns
DEVICE      — anything that produces observations
OBSERVATION — a reading from a device or user input
ANOMALY     — something abnormal detected
ACTION      — something done or recommended
EVIDENCE    — proof that something happened
MAINTENANCE — a task scheduled or completed
COST        — money spent or estimated
DIAGNOSIS   — AI-powered cause analysis
WARRANTY    — warranty information
```

## Example: Washing Machine Lifecycle

```
ASSET: Washing Machine
├── DEVICE: Noise sensor
│   └── OBSERVATION: 31.4 Hz dominant, harmonics at 62.7, 94.1
├── DEVICE: Vibration sensor
│   └── OBSERVATION: 0.81 g RMS (+161% from baseline)
├── DEVICE: Power monitor
│   └── OBSERVATION: 450W draw (+20% from baseline)
│
├── ANOMALY: Bearing wear (confidence: 78%)
├── DIAGNOSIS: Bearing wear → Inspect → Replace
├── ACTION: Replace bearing (₡18,000)
├── EVIDENCE: before.wav, after.wav, invoice.pdf
├── MAINTENANCE: Next in 6 months
└── COST: ₡18,000 (₡42,000 total lifetime)
```

Now Supreme **learns the physical life** of the user's things.

## Commercial Tiers

### Supreme Free
- Phone-only tools
- Signal analysis, vibration, network
- Basic inventory, maintenance reminders
- Goal: **millions of users**

### Supreme Pro ($3.99–$7.99/month)
- AI diagnosis
- Unlimited assets
- Warranty intelligence
- Advanced measurement
- Historical analytics

### Supreme Home ($5–$15/month/home)
- Matter integration
- Camera management
- Sensor networks
- Home automations
- Safety dashboard

### Supreme Hardware
- Supreme Tag (BLE/UWB)
- Supreme Sensor (temp/humidity/motion)
- Supreme Hub (Matter/BLE gateway)

### Supreme Guardian (B2B)
- Commercial/industrial safety
- Installation + license

## Priority Order

1. **Fix AI + Maintenance OS** — diagnose + maintain anything
2. **Warranty + Inventory Vault** — documents + tracking
3. **Supreme Home / Matter** — smart home control
4. **Noise + Vibration Doctor** — reuse DSP engine
5. **Cameras + Guardian Home** — existing cameras + intelligence

## Tech Stack

- Android (Kotlin, Jetpack Compose)
- BLE/UWB (Android APIs)
- Matter/Google Home APIs
- CameraX / ML Kit
- SupremeBass DSP engine
- ONVIF/RTSP adapters
- Coroutines + Flow
- Room (local database)
