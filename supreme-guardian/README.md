# Supreme Guardian

**Distributed Physical Safety Platform**

Detect hazardous conditions before humans, understand where they're occurring, coordinate physical response systems, and verify through sensors whether the response actually worked.

## Architecture

```
SUPREME GUARDIAN
═══════════════════════════════════════

Human & Infrastructure Protection OS

            │
            ├── Supreme Vision
            ├── Supreme Thermal
            ├── Supreme Sense
            ├── Supreme Audio Engine
            ├── Supreme Fire Intelligence
            ├── Supreme Suppression
            ├── Supreme Building
            ├── Supreme Evidence
            └── Supreme Guardian Console
                     Android
                     Web
                     Desktop
```

## Module Structure

```
supreme-guardian/
│
├── apps/
│   ├── android-console/      # SupremeBass → Supreme Guardian HMI
│   ├── web-console/          # Web-based monitoring console
│   └── desktop-console/      # Desktop monitoring application
│
├── core/
│   ├── domain/               # Domain models (shared across all modules)
│   │   ├── incident/         # Incident state machine
│   │   ├── thermal/          # Thermal observation models
│   │   ├── sensor/           # Sensor fusion models
│   │   ├── building/         # Building digital twin
│   │   ├── evidence/         # Evidence chain
│   │   └── safety/           # Safety interlocks & commands
│   ├── incident-engine/      # Incident lifecycle management
│   ├── rules-engine/         # Deterministic safety rules
│   └── digital-twin/         # Building digital twin runtime
│
├── edge/
│   ├── guardian-agent/       # Edge computing agent
│   ├── vision-engine/        # RGB video analysis
│   ├── thermal-engine/       # Thermal video analysis
│   └── sensor-fusion/        # Multi-sensor fusion
│
├── integrations/
│   ├── thermal/
│   │   ├── flir/             # FLIR A70 Advanced adapter
│   │   ├── axis/             # AXIS Q2101-TE adapter
│   │   ├── hikvision/        # Hikvision thermal adapter
│   │   └── onvif/            # ONVIF generic adapter
│   ├── onvif/                # ONVIF Profile M client
│   ├── bacnet/               # BACnet building automation
│   ├── modbus/               # Modbus TCP/RTU
│   ├── mqtt/                 # MQTT broker integration
│   └── fire-panel/           # Fire alarm panel integration
│
├── audio/
│   ├── supreme-dsp/          # DSP library (extracted from SupremeBass)
│   ├── array-controller/     # Multi-speaker array control
│   ├── measurement-engine/   # Acoustic measurement
│   └── hardware-drivers/     # Audio hardware abstraction
│
├── suppression/
│   ├── acoustic/             # Acoustic suppression control
│   ├── water-mist/           # Water mist system control
│   └── interlocks/           # Safety interlock logic
│
├── hardware/
│   ├── zone-controller/      # Per-zone hardware controller
│   └── sensor-node/          # Sensor node firmware
│
├── simulation/
│   └── fire-digital-twin/    # Fire simulation for testing
│
└── docs/
    ├── architecture/         # Architecture decision records
    ├── protocols/            # Protocol specifications
    ├── safety-case/          # Safety case documentation
    ├── verification/         # Test & verification plans
    └── compliance/           # Regulatory compliance
```

## Core Domain Model

### Incident State Machine

```
NORMAL → WATCH → THERMAL_ANOMALY → SUSPECT → MULTISENSOR_VERIFYING
→ CONFIRMED_INCIDENT → ALARM_ACTIVE → SUPPRESSION_PREPARED
→ SUPPRESSION_ACTIVE → VERIFYING_RESPONSE
→ EXTINGUISHED → REIGNITION_WATCH → RECOVERED
```

### Evidence Chain

Every state transition must be recorded with:
- Timestamp
- Source
- Confidence level
- Cryptographic hash chain (tamper-evident)

### Safety Doctrine

1. **NEVER**: AI → direct unrestricted valve
2. **ALWAYS**: Safety interlocks + human approval for critical actions
3. **SAFETY PLANE**: Deterministic (fire panel, PLC, hardwired I/O)
4. **INTELLIGENCE PLANE**: Flexible (AI, thermal CV, analytics)

## Hardware Integration

### Supported Thermal Cameras

| Camera | Protocol | Features |
|--------|----------|----------|
| FLIR A70 Advanced | RTSP, REST, MQTT, Modbus | Radiometric, 640×480, PoE |
| AXIS Q2101-TE | ONVIF, VAPIX, MQTT | Fire detection analytics |
| Hikvision Thermal | ONVIF, ISAPI | Radiometric, dual-spectrum |

### Sensor Fusion

Thermal + RGB + Smoke + Heat + CO + Electrical telemetry

### Suppression Systems

- Acoustic (research phase)
- Water mist (NFPA 750 compliant)
- Conventional sprinkler

## Safety Standards

- NFPA 25: Inspection and Testing of Water-Based Systems
- NFPA 72: National Fire Alarm and Signaling Code
- NFPA 750: Standard on Water Mist Fire Protection Systems
- IEC 61672-1: Sound Level Meters

## Status

**Phase 1**: Core domain models ✅
**Phase 2**: Thermal camera adapters (FLIR A70, AXIS Q2101-TE) 🔨
**Phase 3**: Edge agent + sensor fusion ⏳
**Phase 4**: Building digital twin runtime ⏳
**Phase 5**: Suppression control ⏳
**Phase 6**: Certification track ⏳
