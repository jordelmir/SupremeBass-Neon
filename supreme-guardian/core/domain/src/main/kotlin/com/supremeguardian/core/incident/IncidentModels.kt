package com.supremeguardian.core.incident

import com.supremeguardian.core.evidence.EvidenceChain
import com.supremeguardian.core.safety.ActuatorCommand
import com.supremeguardian.core.safety.SuppressionState
import com.supremeguardian.core.sensor.SensorObservation
import com.supremeguardian.core.shared.*
import java.time.Instant

/**
 * Incident state machine.
 *
 * States:
 *   NORMAL → WATCH → THERMAL_ANOMALY → SUSPECT → MULTISENSOR_VERIFYING
 *   → CONFIRMED_INCIDENT → ALARM_ACTIVE → SUPPRESSION_PREPARED
 *   → SUPPRESSION_ACTIVE → VERIFYING_RESPONSE
 *   → EXTINGUISHED → REIGNITION_WATCH → RECOVERED
 *   → ESCALATED → FIRE_SERVICE
 *
 * Every transition must be recorded with evidence.
 */
sealed class IncidentState {
    abstract val name: String
    abstract val enteredAt: GuardianTimestamp
    abstract val evidenceChain: EvidenceChain

    /** Normal operation — no anomalies detected */
    data class Normal(
        override val enteredAt: GuardianTimestamp,
        override val evidenceChain: EvidenceChain = EvidenceChain()
    ) : IncidentState() {
        override val name: String = "NORMAL"
    }

    /** Watch mode — single sensor anomaly, monitoring */
    data class Watch(
        override val enteredAt: GuardianTimestamp,
        override val evidenceChain: EvidenceChain,
        val triggerObservation: SensorObservation,
        val reason: String
    ) : IncidentState() {
        override val name: String = "WATCH"
    }

    /** Thermal anomaly detected — elevated temperature or rate-of-rise */
    data class ThermalAnomaly(
        override val enteredAt: GuardianTimestamp,
        override val evidenceChain: EvidenceChain,
        val thermalObservationId: EvidenceId,
        val maxCelsius: Double,
        val rateOfRise: Double?
    ) : IncidentState() {
        override val name: String = "THERMAL_ANOMALY"
    }

    /** Suspected fire — multiple indicators but not yet confirmed */
    data class Suspect(
        override val enteredAt: GuardianTimestamp,
        override val evidenceChain: EvidenceChain,
        val observations: List<EvidenceId>,
        val indicators: List<String>
    ) : IncidentState() {
        override val name: String = "SUSPECT"
    }

    /** Multi-sensor verification in progress */
    data class MultisensorVerifying(
        override val enteredAt: GuardianTimestamp,
        override val evidenceChain: EvidenceChain,
        val verificationStart: GuardianTimestamp,
        val sensorsChecking: List<SensorId>
    ) : IncidentState() {
        override val name: String = "MULTISENSOR_VERIFYING"
    }

    /** Fire confirmed by multiple independent sensors */
    data class ConfirmedIncident(
        override val enteredAt: GuardianTimestamp,
        override val evidenceChain: EvidenceChain,
        val confirmations: List<EvidenceId>,
        val confidence: Double,
        val affectedZones: List<ZoneId>
    ) : IncidentState() {
        override val name: String = "CONFIRMED_INCIDENT"
    }

    /** Alarm activated — notification and evacuation initiated */
    data class AlarmActive(
        override val enteredAt: GuardianTimestamp,
        override val evidenceChain: EvidenceChain,
        val alarmId: CommandId,
        val notificationsSent: List<String>
    ) : IncidentState() {
        override val name: String = "ALARM_ACTIVE"
    }

    /** Suppression system prepared and armed */
    data class SuppressionPrepared(
        override val enteredAt: GuardianTimestamp,
        override val evidenceChain: EvidenceChain,
        val suppressionType: String,
        val targetZones: List<ZoneId>
    ) : IncidentState() {
        override val name: String = "SUPPRESSION_PREPARED"
    }

    /** Suppression actively engaged */
    data class SuppressionActive(
        override val enteredAt: GuardianTimestamp,
        override val evidenceChain: EvidenceChain,
        val commandId: CommandId,
        val suppressionState: SuppressionState
    ) : IncidentState() {
        override val name: String = "SUPPRESSION_ACTIVE"
    }

    /** Verifying if suppression was effective */
    data class VerifyingResponse(
        override val enteredAt: GuardianTimestamp,
        override val evidenceChain: EvidenceChain,
        val verificationStart: GuardianTimestamp,
        val sensorsMonitoring: List<SensorId>
    ) : IncidentState() {
        override val name: String = "VERIFYING_RESPONSE"
    }

    /** Fire extinguished — monitoring for reignition */
    data class Extinguished(
        override val enteredAt: GuardianTimestamp,
        override val evidenceChain: EvidenceChain,
        val extinguishedAt: GuardianTimestamp
    ) : IncidentState() {
        override val name: String = "EXTINGUISHED"
    }

    /** Reignition watch period */
    data class ReignitionWatch(
        override val enteredAt: GuardianTimestamp,
        override val evidenceChain: EvidenceChain,
        val watchUntil: GuardianTimestamp
    ) : IncidentState() {
        override val name: String = "REIGNITION_WATCH"
    }

    /** Fully recovered — incident closed */
    data class Recovered(
        override val enteredAt: GuardianTimestamp,
        override val evidenceChain: EvidenceChain,
        val recoveredAt: GuardianTimestamp,
        val duration: Long // ms from incident start to recovery
    ) : IncidentState() {
        override val name: String = "RECOVERED"
    }

    /** Escalated to higher authority */
    data class Escalated(
        override val enteredAt: GuardianTimestamp,
        override val evidenceChain: EvidenceChain,
        val escalatedTo: String,
        val reason: String
    ) : IncidentState() {
        override val name: String = "ESCALATED"
    }

    /** Fire service notified */
    data class FireService(
        override val enteredAt: GuardianTimestamp,
        override val evidenceChain: EvidenceChain,
        val fireServiceId: String,
        val notifiedAt: GuardianTimestamp
    ) : IncidentState() {
        override val name: String = "FIRE_SERVICE"
    }
}

/**
 * Incident — the core domain object for a safety event.
 */
data class Incident(
    val id: IncidentId,
    val buildingId: BuildingId,
    val currentZone: ZoneId,
    val state: IncidentState,
    val createdAt: GuardianTimestamp,
    val timeline: List<TimelineEntry>,
    val auditLog: List<AuditEntry>
) {
    val isActive: Boolean
        get() = state !is IncidentState.Recovered && state !is IncidentState.FireService

    val isSuppressionRequired: Boolean
        get() = state is IncidentState.ConfirmedIncident ||
                state is IncidentState.AlarmActive ||
                state is IncidentState.SuppressionPrepared ||
                state is IncidentState.SuppressionActive

    val durationMs: Long
        get() {
            val now = if (isActive) GuardianTimestamp() else state.enteredAt
            return now.elapsedSince(createdAt)
        }
}

/**
 * Timeline entry — records every state transition.
 */
data class TimelineEntry(
    val timestamp: GuardianTimestamp,
    val fromState: String,
    val toState: String,
    val evidenceIds: List<EvidenceId>,
    val actor: String, // "system", "user:id", "automation:rule"
    val reason: String
)

/**
 * Incident state transitions — validates and executes transitions.
 */
object IncidentStateMachine {

    /**
     * Valid transitions from each state.
     * Safety: only explicitly allowed transitions are permitted.
     */
    private val validTransitions: Map<String, List<String>> = mapOf(
        "NORMAL" to listOf("WATCH", "RECOVERED"),
        "WATCH" to listOf("NORMAL", "THERMAL_ANOMALY", "SUSPECT"),
        "THERMAL_ANOMALY" to listOf("NORMAL", "WATCH", "SUSPECT", "CONFIRMED_INCIDENT"),
        "SUSPECT" to listOf("NORMAL", "WATCH", "MULTISENSOR_VERIFYING", "CONFIRMED_INCIDENT"),
        "MULTISENSOR_VERIFYING" to listOf("NORMAL", "WATCH", "CONFIRMED_INCIDENT"),
        "CONFIRMED_INCIDENT" to listOf("ALARM_ACTIVE", "SUPPRESSION_PREPARED", "ESCALATED"),
        "ALARM_ACTIVE" to listOf("SUPPRESSION_PREPARED", "ESCALATED"),
        "SUPPRESSION_PREPARED" to listOf("SUPPRESSION_ACTIVE", "ESCALATED"),
        "SUPPRESSION_ACTIVE" to listOf("VERIFYING_RESPONSE", "ESCALATED"),
        "VERIFYING_RESPONSE" to listOf("EXTINGUISHED", "SUPPRESSION_ACTIVE", "ESCALATED"),
        "EXTINGUISHED" to listOf("REIGNITION_WATCH", "CONFIRMED_INCIDENT"),
        "REIGNITION_WATCH" to listOf("RECOVERED", "CONFIRMED_INCIDENT"),
        "RECOVERED" to listOf("NORMAL"),
        "ESCALATED" to listOf("FIRE_SERVICE", "CONFIRMED_INCIDENT"),
        "FIRE_SERVICE" to listOf("RECOVERED")
    )

    /**
     * Check if a transition is valid.
     */
    fun isValidTransition(from: String, to: String): Boolean {
        return validTransitions[from]?.contains(to) == true
    }

    /**
     * Get valid next states from current state.
     */
    fun getValidTransitions(currentState: String): List<String> {
        return validTransitions[currentState] ?: emptyList()
    }

    /**
     * Validate a transition. Returns null if valid, error message if invalid.
     */
    fun validateTransition(from: IncidentState, to: String): String? {
        if (!isValidTransition(from.name, to)) {
            return "Invalid transition: ${from.name} → $to"
        }
        return null
    }
}
