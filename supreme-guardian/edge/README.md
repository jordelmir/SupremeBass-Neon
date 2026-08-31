# Supreme Guardian — Edge Module

The Edge Module is the runtime brain that connects physical devices to the incident engine.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Edge Module                           │
│                                                             │
│  ┌─────────────┐    ┌─────────────────┐    ┌────────────┐  │
│  │   Device     │    │   Observation   │    │   Edge     │  │
│  │   Manager    │───►│   Collector     │───►│   Agent    │  │
│  │             │    │                 │    │            │  │
│  └─────────────┘    └─────────────────┘    └────────────┘  │
│         │                   │                     │         │
│         ▼                   ▼                     ▼         │
│  ┌─────────────┐    ┌─────────────────┐    ┌────────────┐  │
│  │  Cameras    │    │   Thermal       │    │  Incident  │  │
│  │  Sensors    │    │   Processor     │    │  Engine    │  │
│  └─────────────┘    └─────────────────┘    └────────────┘  │
│                                                     │       │
│                                                     ▼       │
│                                             ┌────────────┐  │
│                                             │  Evidence  │  │
│                                             │  Recorder  │  │
│                                             └────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## Components

### DeviceManager
- Discovers cameras and sensors on the network
- Maintains connections to each device
- Tracks device health (connected/disconnected/failed)
- Routes frames from cameras to Edge Agent
- Handles disconnections with automatic reconnection

### ObservationCollector
- Collects thermal observations from cameras
- Collects sensor readings (smoke, heat, flame, CO)
- Buffers observations within time window (5s)
- Emits aggregated observations for fusion engine
- Tracks observation quality and completeness

### EdgeAgent
- Receives thermal frames from cameras
- Processes observations through ThermalObservationProcessor
- Runs DeterministicSensorFusion for multi-sensor analysis
- Evaluates FireDetectionRules for state transitions
- Triggers actuators based on state
- Records evidence for every transition

### IncidentOrchestrator
- Coordinates the full incident lifecycle
- Connects Edge Agent to Incident Engine
- Records evidence for every transition
- Triggers actuators based on state
- Manages suppression requests
- Notifies UI of state changes

## Data Flow

1. **Camera** → Thermal Frame → **DeviceManager**
2. **DeviceManager** → Thermal Frame → **EdgeAgent**
3. **EdgeAgent** → ThermalObservationProcessor → **SensorObservation**
4. **SensorObservation** → **DeterministicSensorFusion** → **FusionResult**
5. **FusionResult** → **IncidentEngine** → **Incident**
6. **Incident** → **IncidentOrchestrator** → **EvidenceRecorder**
7. **Incident** → **ActuatorController** → **Physical Suppression**

## Safety

### Safety Doctrine
1. **NEVER**: AI → direct unrestricted valve
2. **ALWAYS**: Safety interlocks + human approval for critical actions
3. **SAFETY PLANE**: Deterministic (fire panel, PLC, hardwired I/O)
4. **INTELLIGENCE PLANE**: Flexible (AI, thermal CV, analytics)

### Evidence Chain
Every state transition must be recorded with:
- Timestamp
- Source (which sensor/actor)
- Confidence level
- Cryptographic hash chain (tamper-evident)

## Configuration

### Device Discovery
- Scan interval: 60 seconds
- Health check interval: 10 seconds
- Reconnect delay: 5 seconds (exponential backoff)
- Max reconnect attempts: 5

### Observation Collection
- Collection window: 5 seconds
- Max observations per camera: 10
- Buffer size: Unlimited (cleared after collection)

### Fusion Engine
- Fusion interval: 5 seconds
- Minimum observations for confirmation: 2 sensors
- Multi-sensor bonus: 5% per additional sensor
