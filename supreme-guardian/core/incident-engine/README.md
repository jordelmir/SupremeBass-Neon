# Supreme Guardian — Incident Engine

The Incident Engine is the runtime brain of Supreme Guardian.

## Architecture

```
Sensor Observations
       │
       ▼
┌─────────────────┐
│ Incident Engine │
│                 │
│ ┌─────────────┐ │
│ │ Rules Engine│ │
│ └─────────────┘ │
│                 │
│ ┌─────────────┐ │
│ │   Evidence   │ │
│ │   Recorder   │ │
│ └─────────────┘ │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Incident State  │
│    Machine      │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  UI / Actuators │
└─────────────────┘
```

## Components

### IncidentEngine
- Processes sensor observations
- Evaluates rules for state transitions
- Executes validated transitions
- Records evidence for every transition
- Emits state changes

### FireDetectionRules
- Deterministic rules for fire detection
- Temperature thresholds (50°C, 70°C, 90°C, 120°C)
- Rate-of-rise thresholds (5, 10, 20 °C/min)
- Multi-sensor confirmation (≥2 sensors)
- Smoke, flame, CO, electrical anomaly detection

### ThermalObservationProcessor
- Extracts temperature measurements from thermal frames
- Detects hotspots
- Calculates rate-of-rise
- Generates ThermalEvent for significant changes
- Converts to SensorObservation for fusion

### DeterministicSensorFusion
- Combines multiple sensor inputs
- Groups observations by zone
- Applies deterministic rules
- Outputs FusionResult with fire confidence
- No ML inference — all rules are explicit

## Evidence Chain

Every state transition must be recorded with:
- Timestamp
- Source (which sensor/actor)
- Confidence level
- Cryptographic hash chain (tamper-evident)

## Safety Doctrine

1. **NEVER**: AI → direct unrestricted valve
2. **ALWAYS**: Safety interlocks + human approval for critical actions
3. **SAFETY PLANE**: Deterministic (fire panel, PLC, hardwired I/O)
4. **INTELLIGENCE PLANE**: Flexible (AI, thermal CV, analytics)

## State Machine

```
NORMAL → WATCH → THERMAL_ANOMALY → SUSPECT → MULTISENSOR_VERIFYING
→ CONFIRMED_INCIDENT → ALARM_ACTIVE → SUPPRESSION_PREPARED
→ SUPPRESSION_ACTIVE → VERIFYING_RESPONSE
→ EXTINGUISHED → REIGNITION_WATCH → RECOVERED
```

Every transition is validated and recorded with evidence.
