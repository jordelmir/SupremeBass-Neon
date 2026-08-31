package com.supremeguardian.edge

import com.supremeguardian.core.building.ZoneId
import com.supremeguardian.core.incident.Incident
import com.supremeguardian.core.shared.*
import com.supremeguardian.engine.IncidentEngine
import com.supremeguardian.engine.FireDetectionRules
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Incident Orchestrator — coordinates the full incident lifecycle.
 *
 * Responsibilities:
 *   1. Connect Edge Agent to Incident Engine
 *   2. Record evidence for every transition
 *   3. Trigger actuators based on state
 *   4. Notify UI of state changes
 *   5. Manage suppression requests
 *
 * Flow:
 *
 *   Sensor Data → Edge Agent → Incident Engine → Incident
 *                                        │
 *                                        ▼
 *                                   Evidence Recorder
 *                                        │
 *                                        ▼
 *                                   Actuator Controller
 *                                        │
 *                                        ▼
 *                                   UI Notification
 */
class IncidentOrchestrator(
    private val edgeAgent: EdgeAgent
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val running = AtomicBoolean(false)

    // Components
    private val evidenceRecorder = EvidenceRecorder()
    private val actuatorController = ActuatorController()

    // State
    private val _state = MutableStateFlow(OrchestratorState())
    val state: StateFlow<OrchestratorState> = _state.asStateFlow()

    private val _incidents = MutableSharedFlow<Incident>(replay = 1)
    val incidents: SharedFlow<Incident> = _incidents.asSharedFlow()

    /**
     * Start the orchestrator.
     */
    fun start() {
        if (running.getAndSet(true)) return

        // Collect incidents from edge agent
        scope.launch {
            edgeAgent.incidents.collect { incident ->
                handleIncident(incident)
            }
        }

        // Collect events from edge agent
        scope.launch {
            edgeAgent.events.collect { event ->
                handleAgentEvent(event)
            }
        }
    }

    /**
     * Stop the orchestrator.
     */
    fun stop() {
        if (!running.getAndSet(false)) return

        scope.cancel()
        evidenceRecorder.stop()
        actuatorController.stop()
    }

    /**
     * Request suppression for a zone.
     */
    suspend fun requestSuppression(zoneId: ZoneId, reason: String): SuppressionResult {
        val incident = _state.value.currentIncident

        if (incident == null) {
            return SuppressionResult(false, "No active incident")
        }

        // Record suppression request as evidence
        evidenceRecorder.recordEvidence(
            incidentId = incident.id,
            evidence = com.supremeguardian.core.incident.Evidence(
                type = com.supremeguardian.core.incident.EvidenceType.ACTOR_ACTION,
                source = EvidenceSource("orchestrator", "IncidentOrchestrator"),
                timestamp = GuardianTimestamp(),
                data = mapOf(
                    "action" to "suppression_requested",
                    "zone_id" to zoneId.value,
                    "reason" to reason
                ),
                confidence = Confidence(1.0, ObservationAuthority.SYSTEM_GENERATED)
            )
        )

        // Trigger actuator
        val result = actuatorController.activateSuppression(zoneId, incident.id)

        // Record result
        evidenceRecorder.recordEvidence(
            incidentId = incident.id,
            evidence = com.supremeguardian.core.incident.Evidence(
                type = com.supremeguardian.core.incident.EvidenceType.ACTOR_ACTION,
                source = EvidenceSource("actuator", "ActuatorController"),
                timestamp = GuardianTimestamp(),
                data = mapOf(
                    "action" to "suppression_activated",
                    "success" to result.success.toString(),
                    "message" to result.message
                ),
                confidence = Confidence(1.0, ObservationAuthority.SYSTEM_GENERATED)
            )
        )

        return result
    }

    /**
     * Get evidence for an incident.
     */
    fun getEvidence(incidentId: IncidentId): List<com.supremeguardian.core.incident.Evidence> {
        return evidenceRecorder.getEvidence(incidentId)
    }

    /**
     * Get incident timeline.
     */
    fun getTimeline(incidentId: IncidentId): List<com.supremeguardian.core.incident.TimelineEntry> {
        return evidenceRecorder.getTimeline(incidentId)
    }

    private suspend fun handleIncident(incident: Incident) {
        _state.update { it.copy(
            currentIncident = incident,
            activeIncidents = 1,
            lastIncidentTime = GuardianTimestamp()
        )}

        // Record incident creation
        evidenceRecorder.recordEvidence(
            incidentId = incident.id,
            evidence = com.supremeguardian.core.incident.Evidence(
                type = com.supremeguardian.core.incident.EvidenceType.OBSERVATION,
                source = EvidenceSource("engine", "IncidentEngine"),
                timestamp = GuardianTimestamp(),
                data = mapOf(
                    "action" to "incident_created",
                    "state" to incident.state.name,
                    "zone" to incident.currentZone.value
                ),
                confidence = Confidence(1.0, ObservationAuthority.SYSTEM_GENERATED)
            )
        )

        _incidents.tryEmit(incident)
    }

    private suspend fun handleAgentEvent(event: AgentEvent) {
        when (event) {
            is AgentEvent.IncidentCreated -> {
                _state.update { it.copy(
                    lastIncidentTime = GuardianTimestamp()
                )}
            }
            is AgentEvent.SuppressionRequested -> {
                // Handle suppression request from agent
                val result = actuatorController.activateSuppression(
                    event.zoneId,
                    event.incidentId
                )
                // Record result
                evidenceRecorder.recordEvidence(
                    incidentId = event.incidentId,
                    evidence = com.supremeguardian.core.incident.Evidence(
                        type = com.supremeguardian.core.incident.EvidenceType.ACTOR_ACTION,
                        source = EvidenceSource("agent", "EdgeAgent"),
                        timestamp = GuardianTimestamp(),
                        data = mapOf(
                            "action" to "suppression_activated",
                            "success" to result.success.toString()
                        ),
                        confidence = Confidence(1.0, ObservationAuthority.SYSTEM_GENERATED)
                    )
                )
            }
            is AgentEvent.ThermalEvent -> {
                // Record thermal event
                _state.update { it.copy(
                    lastThermalEventTime = GuardianTimestamp()
                )}
            }
            else -> { /* Other events */ }
        }
    }
}

/**
 * Orchestrator state.
 */
data class OrchestratorState(
    val activeIncidents: Int = 0,
    val currentIncident: Incident? = null,
    val lastIncidentTime: GuardianTimestamp? = null,
    val lastThermalEventTime: GuardianTimestamp? = null
)

/**
 * Evidence Recorder — records evidence for every state transition.
 */
class EvidenceRecorder {
    private val evidence = mutableMapOf<IncidentId, MutableList<com.supremeguardian.core.incident.Evidence>>()
    private val timeline = mutableMapOf<IncidentId, MutableList<com.supremeguardian.core.incident.TimelineEntry>>()

    fun recordEvidence(incidentId: IncidentId, evidence: com.supremeguardian.core.incident.Evidence) {
        this.evidence.getOrPut(incidentId) { mutableListOf() }.add(evidence)
    }

    fun getEvidence(incidentId: IncidentId): List<com.supremeguardian.core.incident.Evidence> {
        return evidence[incidentId]?.toList() ?: emptyList()
    }

    fun getTimeline(incidentId: IncidentId): List<com.supremeguardian.core.incident.TimelineEntry> {
        return timeline[incidentId]?.toList() ?: emptyList()
    }

    fun stop() {
        evidence.clear()
        timeline.clear()
    }
}

/**
 * Actuator Controller — controls physical actuators (valves, speakers, alarms).
 */
class ActuatorController {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val running = AtomicBoolean(false)

    fun start() {
        running.set(true)
    }

    fun stop() {
        running.set(false)
        scope.cancel()
    }

    /**
     * Activate suppression for a zone.
     */
    suspend fun activateSuppression(zoneId: ZoneId, incidentId: IncidentId): SuppressionResult {
        if (!running.get()) {
            return SuppressionResult(false, "Controller not running")
        }

        // TODO: Send command to physical suppression system
        // This would involve:
        // 1. Verify safety interlocks
        // 2. Send command to PLC/relay
        // 3. Wait for confirmation
        // 4. Return result

        return SuppressionResult(true, "Suppression activated for ${zoneId.value}")
    }

    /**
     * Deactivate suppression for a zone.
     */
    suspend fun deactivateSuppression(zoneId: ZoneId): SuppressionResult {
        if (!running.get()) {
            return SuppressionResult(false, "Controller not running")
        }

        // TODO: Send command to physical suppression system

        return SuppressionResult(true, "Suppression deactivated for ${zoneId.value}")
    }

    /**
     * Test an actuator.
     */
    suspend fun testActuator(actuatorId: String): Boolean {
        // TODO: Send test command to actuator
        return true
    }
}

/**
 * Suppression result.
 */
data class SuppressionResult(
    val success: Boolean,
    val message: String
)
