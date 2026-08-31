package com.supremeguardian.engine

import com.supremeguardian.core.building.ZoneId
import com.supremeguardian.core.evidence.*
import com.supremeguardian.core.incident.*
import com.supremeguardian.core.sensor.SensorObservation
import com.supremeguardian.core.shared.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Incident Engine — the runtime brain of Supreme Guardian.
 *
 * Responsibilities:
 *   1. Process sensor observations
 *   2. Evaluate rules for state transitions
 *   3. Execute validated transitions
 *   4. Record evidence for every transition
 *   5. Emit state changes for UI/actuators
 *
 * This is a DETERMINISTIC engine — no ML inference here.
 * All decisions are based on explicit rules with evidence.
 */
class IncidentEngine {

    private val _activeIncidents = MutableStateFlow<Map<ZoneId, Incident>>(emptyMap())
    val activeIncidents: StateFlow<Map<ZoneId, Incident>> = _activeIncidents.asStateFlow()

    private val _incidentHistory = MutableStateFlow<List<Incident>>(emptyMap())
    val incidentHistory: StateFlow<List<Incident>> = _incidentHistory.asStateFlow()

    private val ruleEngine = FireDetectionRules()
    private val evidenceRecorder = EvidenceRecorder()

    /**
     * Process a new observation from any sensor.
     * This is the main entry point for all sensor data.
     */
    fun processObservation(observation: SensorObservation) {
        val zoneId = observation.zoneId
        val currentIncident = _activeIncidents.value[zoneId]

        if (currentIncident == null) {
            // No active incident — check if we should start one
            processNewIncidentCheck(observation)
        } else {
            // Active incident — evaluate rules for state transition
            processActiveIncident(currentIncident, observation)
        }
    }

    private fun processNewIncidentCheck(observation: SensorObservation) {
        // Evaluate rules to see if we should transition from NORMAL to WATCH
        val decision = ruleEngine.evaluateNewIncident(observation)

        if (decision != null) {
            val zoneId = observation.zoneId
            val evidence = evidenceRecorder.recordObservation(observation)

            val incident = Incident(
                id = IncidentId(),
                buildingId = BuildingId("unknown"), // TODO: resolve from zone
                currentZone = zoneId,
                state = decision.newState,
                createdAt = GuardianTimestamp(),
                timeline = listOf(
                    TimelineEntry(
                        timestamp = GuardianTimestamp(),
                        fromState = "NORMAL",
                        toState = decision.newState.name,
                        evidenceIds = listOf(evidence.id),
                        actor = "system:incident-engine",
                        reason = decision.reason
                    )
                ),
                auditLog = listOf(
                    AuditEntry(
                        timestamp = GuardianTimestamp(),
                        action = "INCIDENT_CREATED",
                        actor = "system:incident-engine",
                        details = mapOf("zone" to zoneId.value, "reason" to decision.reason)
                    )
                )
            )

            _activeIncidents.update { current ->
                current + (zoneId to incident)
            }

            println("[IncidentEngine] NEW INCIDENT: ${incident.id.value} in zone ${zoneId.value} → ${decision.newState.name}")
        }
    }

    private fun processActiveIncident(incident: Incident, observation: SensorObservation) {
        val evidence = evidenceRecorder.recordObservation(observation)

        val decision = ruleEngine.evaluateTransition(
            currentState = incident.state,
            observation = observation,
            incident = incident
        )

        if (decision != null) {
            val newIncident = executeTransition(incident, decision, evidence)

            _activeIncidents.update { current ->
                if (newIncident.isActive) {
                    current + (newIncident.currentZone to newIncident)
                } else {
                    // Incident reached terminal state — move to history
                    current - newIncident.currentZone
                }
            }

            if (!newIncident.isActive) {
                _incidentHistory.update { history ->
                    history + newIncident
                }
            }

            println("[IncidentEngine] TRANSITION: ${incident.id.value} ${incident.state.name} → ${decision.newState.name}")
        }
    }

    private fun executeTransition(
        incident: Incident,
        decision: TransitionDecision,
        evidence: EvidenceEntry
    ): Incident {
        val newState = decision.newState
        val timelineEntry = TimelineEntry(
            timestamp = GuardianTimestamp(),
            fromState = incident.state.name,
            toState = newState.name,
            evidenceIds = listOf(evidence.id),
            actor = decision.actor,
            reason = decision.reason
        )

        val auditEntry = AuditEntry(
            timestamp = GuardianTimestamp(),
            action = "STATE_TRANSITION",
            actor = decision.actor,
            details = mapOf(
                "from" to incident.state.name,
                "to" to newState.name,
                "reason" to decision.reason
            ),
            evidenceIds = listOf(evidence.id)
        )

        return incident.copy(
            state = newState,
            timeline = incident.timeline + timelineEntry,
            auditLog = incident.auditLog + auditEntry
        )
    }

    /**
     * Manually acknowledge an incident (operator action).
     */
    fun acknowledgeIncident(zoneId: ZoneId, actor: String, reason: String) {
        val incident = _activeIncidents.value[zoneId] ?: return

        val auditEntry = AuditEntry(
            timestamp = GuardianTimestamp(),
            action = "INCIDENT_ACKNOWLEDGED",
            actor = actor,
            details = mapOf("reason" to reason)
        )

        val updatedIncident = incident.copy(
            auditLog = incident.auditLog + auditEntry
        )

        _activeIncidents.update { current ->
            current + (zoneId to updatedIncident)
        }
    }

    /**
     * Get incident for a specific zone.
     */
    fun getIncident(zoneId: ZoneId): Incident? = _activeIncidents.value[zoneId]

    /**
     * Get all active incidents.
     */
    fun getAllActive(): List<Incident> = _activeIncidents.value.values.toList()

    /**
     * Get incident history.
     */
    fun getHistory(): List<Incident> = _incidentHistory.value
}

/**
 * Transition decision — what the rules engine decided.
 */
data class TransitionDecision(
    val newState: IncidentState,
    val reason: String,
    val actor: String = "system:incident-engine"
)

/**
 * Evidence recorder — creates evidence entries for all observations.
 */
class EvidenceRecorder {
    private var sequence = 0L

    fun recordObservation(observation: SensorObservation): EvidenceEntry {
        sequence++
        return EvidenceEntry(
            id = EvidenceId(),
            timestamp = observation.timestamp,
            type = when (observation.sensorType) {
                com.supremeguardian.core.sensor.SensorType.THERMAL_CAMERA -> EvidenceType.THERMAL_MEASUREMENT
                com.supremeguardian.core.sensor.SensorType.RGB_CAMERA -> EvidenceType.RGB_IMAGE
                com.supremeguardian.core.sensor.SensorType.SMOKE_DETECTOR -> EvidenceType.SMOKE_DETECTION
                com.supremeguardian.core.sensor.SensorType.HEAT_DETECTOR -> EvidenceType.HEAT_DETECTION
                com.supremeguardian.core.sensor.SensorType.FLAME_DETECTOR -> EvidenceType.FLAME_DETECTION
                com.supremeguardian.core.sensor.SensorType.GAS_DETECTOR -> EvidenceType.GAS_DETECTION
                com.supremeguardian.core.sensor.SensorType.CO_DETECTOR -> EvidenceType.GAS_DETECTION
                com.supremeguardian.core.sensor.SensorType.ELECTRICAL_SENSOR -> EvidenceType.ELECTRICAL_ANOMALY
                com.supremeguardian.core.sensor.SensorType.ACOUSTIC_SENSOR -> EvidenceType.ACOUSTIC_EVENT
                else -> EvidenceType.SENSOR_READING
            },
            source = "${observation.sensorType.name.lowercase()}:${observation.sensorId.value}",
            description = "Sensor reading from ${observation.sensorType.name} in zone ${observation.zoneId.value}",
            data = observation.readings.mapValues { it.value.toString() },
            confidence = observation.confidence
        )
    }

    fun recordCommand(command: com.supremeguardian.core.safety.ActuatorCommand): EvidenceEntry {
        sequence++
        return EvidenceEntry(
            id = EvidenceId(),
            timestamp = command.timestamp,
            type = EvidenceType.COMMAND_EXECUTED,
            source = "command:${command.commandId.value}",
            description = "Command ${command.action.name} for zone ${command.zoneId.value}",
            data = mapOf(
                "action" to command.action.name,
                "requestedBy" to command.requestedBy,
                "reason" to command.reason
            ),
            confidence = Confidence(1.0, ObservationAuthority.HARDWARE_MEASURED)
        )
    }

    fun recordSystemEvent(event: String, details: Map<String, String> = emptyMap()): EvidenceEntry {
        sequence++
        return EvidenceEntry(
            id = EvidenceId(),
            timestamp = GuardianTimestamp(),
            type = EvidenceType.SYSTEM_EVENT,
            source = "system:incident-engine",
            description = event,
            data = details,
            confidence = Confidence(1.0, ObservationAuthority.HARDWARE_MEASURED)
        )
    }
}
