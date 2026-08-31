package com.supremeguardian.edge

import com.supremeguardian.core.building.ZoneId
import com.supremeguardian.core.incident.Incident
import com.supremeguardian.core.shared.*
import com.supremeguardian.core.thermal.ThermalFrame
import com.supremeguardian.engine.DeterministicSensorFusion
import com.supremeguardian.engine.FireDetectionRules
import com.supremeguardian.engine.IncidentEngine
import com.supremeguardian.engine.ThermalObservationProcessor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Edge Agent — the brain that connects physical devices to the incident engine.
 *
 * Responsibilities:
 *   1. Receive thermal frames from cameras
 *   2. Process observations
 *   3. Run fusion engine
 *   4. Evaluate rules
 *   5. Trigger actuators
 *   6. Record evidence
 *   7. Emit state changes for UI
 *
 * Architecture:
 *
 *   ThermalFrame ──► ThermalObservationProcessor ──► SensorObservation
 *                                                        │
 *   SensorObservation ───────────────────────────────► DeterministicSensorFusion
 *                                                        │
 *                                                        ▼
 *                                                   FusionResult
 *                                                        │
 *                                                        ▼
 *                                                   IncidentEngine
 *                                                        │
 *                                                        ▼
 *                                                   Incident
 *                                                        │
 *                                                   ┌────┴────┐
 *                                                   │         │
 *                                               UI/Console  Actuators
 */
class EdgeAgent(
    private val incidentEngine: IncidentEngine = IncidentEngine(FireDetectionRules()),
    private val fusionEngine: DeterministicSensorFusion = DeterministicSensorFusion(),
    private val thermalProcessor: ThermalObservationProcessor = ThermalObservationProcessor()
) {
    companion object {
        private const val FUSION_INTERVAL_MS = 5000L // Fuse every 5 seconds
        private const val HEALTH_CHECK_INTERVAL_MS = 30_000L // Health check every 30s
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val running = AtomicBoolean(false)
    private val observations = ConcurrentHashMap<CameraId, MutableList<ThermalObservation>>()

    // State flows
    private val _state = MutableStateFlow(AgentState())
    val state: StateFlow<AgentState> = _state.asStateFlow()

    private val _incidents = MutableSharedFlow<Incident>(replay = 1)
    val incidents: SharedFlow<Incident> = _incidents.asSharedFlow()

    private val _events = MutableSharedFlow<AgentEvent>(replay = 10)
    val events: SharedFlow<AgentEvent> = _events.asSharedFlow()

    /**
     * Start the edge agent.
     */
    fun start() {
        if (running.getAndSet(true)) return

        emitEvent(AgentEvent.Started)

        // Start fusion loop
        scope.launch {
            while (running.get()) {
                try {
                    fuseAndEvaluate()
                } catch (e: Exception) {
                    emitEvent(AgentEvent.Error("Fusion failed: ${e.message}"))
                }
                delay(FUSION_INTERVAL_MS)
            }
        }

        // Start health check loop
        scope.launch {
            while (running.get()) {
                try {
                    emitEvent(AgentEvent.HealthCheck(AgentHealth(
                        uptimeMs = System.currentTimeMillis(),
                        observationsCount = observations.values.sumOf { it.size },
                        activeIncidents = _state.value.activeIncidents,
                        memoryUsageMb = Runtime.getRuntime().let {
                            (it.totalMemory() - it.freeMemory()) / 1024 / 1024
                        }
                    )))
                } catch (e: Exception) {
                    // Health check failure is non-fatal
                }
                delay(HEALTH_CHECK_INTERVAL_MS)
            }
        }

        emitEvent(AgentEvent.Started)
    }

    /**
     * Stop the edge agent.
     */
    fun stop() {
        if (!running.getAndSet(false)) return

        scope.cancel()
        observations.clear()
        emitEvent(AgentEvent.Stopped)
    }

    /**
     * Ingest a thermal frame from a camera.
     */
    fun ingestThermalFrame(frame: ThermalFrame) {
        if (!running.get()) return

        val zoneId = frame.zoneId
        val cameraId = frame.cameraId

        // Process through thermal processor
        val result = thermalProcessor.processFrame(frame)

        // Store observation
        observations.getOrPut(cameraId) { mutableListOf() }.add(
            ThermalObservation(
                cameraId = cameraId,
                zoneId = zoneId,
                timestamp = frame.timestamp,
                maxCelsius = frame.maxCelsius,
                minCelsius = frame.minCelsius,
                averageCelsius = frame.averageCelsius,
                hotspot = result.hotspot,
                rateOfRise = result.rateOfRise
            )
        )

        // Emit thermal events
        result.events.forEach { event ->
            emitEvent(AgentEvent.ThermalEvent(event))
        }
    }

    /**
     * Ingest a sensor reading.
     */
    fun ingestSensorReading(reading: com.supremeguardian.core.sensor.SensorObservation) {
        if (!running.get()) return

        observations.getOrPut(SensorId(reading.sensorId.value)) { mutableListOf() }
            .add(
                ThermalObservation(
                    cameraId = CameraId(reading.sensorId.value),
                    zoneId = reading.zoneId,
                    timestamp = reading.timestamp,
                    maxCelsius = reading.readings["temperature"] ?: 0.0,
                    minCelsius = reading.readings["temperature"] ?: 0.0,
                    averageCelsius = reading.readings["temperature"] ?: 0.0,
                    hotspot = null,
                    rateOfRise = reading.readings["rate_of_rise"]
                )
            )
    }

    /**
     * Trigger a suppression action.
     */
    suspend fun triggerSuppression(zoneId: ZoneId): SuppressionResult {
        if (!running.get()) {
            return SuppressionResult(false, "Agent not running")
        }

        val incident = _state.value.currentIncident
        if (incident == null) {
            return SuppressionResult(false, "No active incident")
        }

        // Emit suppression request
        emitEvent(AgentEvent.SuppressionRequested(
            incidentId = incident.id,
            zoneId = zoneId,
            reason = "Agent triggered suppression"
        ))

        return SuppressionResult(true, "Suppression request emitted")
    }

    private suspend fun fuseAndEvaluate() {
        val allObservations = observations.values.flatten()

        if (allObservations.isEmpty()) return

        // Run fusion engine
        val fusionResult = fusionEngine.fuse(
            allObservations.map {
                com.supremeguardian.core.sensor.SensorObservation(
                    sensorId = com.supremeguardian.core.sensor.SensorId(it.cameraId.value),
                    sensorType = com.supremeguardian.core.sensor.SensorType.THERMAL_CAMERA,
                    zoneId = it.zoneId,
                    timestamp = it.timestamp,
                    readings = mapOf(
                        "max_celsius" to it.maxCelsius,
                        "min_celsius" to it.minCelsius,
                        "average_celsius" to it.averageCelsius
                    ),
                    confidence = com.supremeguardian.core.sensor.Confidence(
                        value = 0.9,
                        authority = com.supremeguardian.core.sensor.ObservationAuthority.HARDWARE_MEASURED
                    ),
                    authority = com.supremeguardian.core.sensor.ObservationAuthority.HARDWARE_MEASURED
                )
            }
        )

        // Process through incident engine
        val incident = incidentEngine.processObservation(
            sensorId = com.supremeguardian.core.sensor.SensorId("fusion"),
            sensorType = com.supremeguardian.core.sensor.SensorType.THERMAL_CAMERA,
            zoneId = fusionResult.zoneId,
            confidence = fusionResult.fireConfidence,
            readings = fusionResult.fusedReading,
            reason = fusionResult.reasoning.joinToString("; ")
        )

        // Update state
        _state.update { it.copy(
            activeIncidents = if (incident != null) 1 else 0,
            currentIncident = incident,
            lastFusionTime = GuardianTimestamp(),
            lastFusionResult = fusionResult
        )}

        // Emit incident if new
        if (incident != null) {
            _incidents.tryEmit(incident)
            emitEvent(AgentEvent.IncidentCreated(incident.id))
        }

        // Clear processed observations
        observations.clear()
    }

    private fun emitEvent(event: AgentEvent) {
        _events.tryEmit(event)
    }
}

/**
 * Agent state.
 */
data class AgentState(
    val activeIncidents: Int = 0,
    val currentIncident: Incident? = null,
    val lastFusionTime: GuardianTimestamp? = null,
    val lastFusionResult: com.supremeguardian.core.sensor.FusionResult? = null
)

/**
 * Agent events.
 */
sealed class AgentEvent {
    data object Started : AgentEvent()
    data object Stopped : AgentEvent()
    data class Error(val message: String) : AgentEvent()
    data class HealthCheck(val health: AgentHealth) : AgentEvent()
    data class ThermalEvent(val event: com.supremeguardian.core.thermal.ThermalEvent) : AgentEvent()
    data class IncidentCreated(val incidentId: IncidentId) : AgentEvent()
    data class SuppressionRequested(
        val incidentId: IncidentId,
        val zoneId: ZoneId,
        val reason: String
    ) : AgentEvent()
}

/**
 * Agent health.
 */
data class AgentHealth(
    val uptimeMs: Long,
    val observationsCount: Int,
    val activeIncidents: Int,
    val memoryUsageMb: Long
)

/**
 * Suppression result.
 */
data class SuppressionResult(
    val success: Boolean,
    val message: String
)
