package com.supremeguardian.core.safety

import com.supremeguardian.core.shared.*

/**
 * Safety command — for controlling actuators (suppression, alarms, etc.)
 *
 * Every command must be:
 *   - Requested by an authorized entity
 *   - Validated against safety interlocks
 *   - Acknowledged by the actuator
 *   - Physically verified (if possible)
 *
 * NEVER allow: AI → direct unrestricted valve
 * ALWAYS require: safety interlocks + human approval for critical actions
 */
data class ActuatorCommand(
    val commandId: CommandId,
    val timestamp: GuardianTimestamp,
    val action: ActuatorAction,
    val zoneId: ZoneId,
    val requestedBy: String,
    val reason: String,
    val evidenceIds: List<EvidenceId>,
    val policyVersion: Int,
    val signature: String? = null, // Cryptographic signature for critical commands
    val state: CommandState = CommandState.REQUESTED
)

/**
 * Command lifecycle states.
 */
enum class CommandState {
    REQUESTED,
    AUTHORIZED,
    COMMAND_SENT,
    ACTUATOR_ACKNOWLEDGED,
    PHYSICAL_EFFECT_OBSERVED,
    PHYSICALLY_VERIFIED,
    FAILED,
    REJECTED,
    CANCELLED
}

/**
 * Available actuator actions.
 */
enum class ActuatorAction {
    // Alarm
    TRIGGER_ALARM,
    SILENCE_ALARM,
    EVACUATION_SIGNAL,

    // Suppression
    ARM_SUPPRESSION,
    ACTIVATE_SUPPRESSION,
    DEACTIVATE_SUPPRESSION,
    EMERGENCY_STOP_SUPPRESSION,

    // Electrical
    ISOLATE_ELECTRICAL,
    RESTORE_ELECTRICAL,

    // HVAC
    SHUTDOWN_HVAC,
    RESTORE_HVAC,

    // Acoustic
    ACTIVATE_ACOUSTIC_ARRAY,
    DEACTIVATE_ACOUSTIC_ARRAY,

    // Water Mist
    ACTIVATE_WATER_MIST,
    DEACTIVATE_WATER_MIST,

    // Monitoring
    INCREASE_MONITORING,
    RESTORE_NORMAL_MONITORING,

    // Notification
    NOTIFY_RESPONDERS,
    NOTIFY_BUILDING_MANAGER,
    NOTIFY_FIRE_SERVICE
}

/**
 * Suppression system state.
 */
data class SuppressionState(
    val type: SuppressionType,
    val armed: Boolean,
    val active: Boolean,
    val targetZones: List<ZoneId>,
    val activatedAt: GuardianTimestamp?,
    val activationCount: Int = 0
)

enum class SuppressionType {
    ACOUSTIC,
    WATER_MIST,
    SPRINKLER,
    GAS,
    FOAM,
    HYBRID
}

/**
 * Safety interlock — prevents unsafe command execution.
 */
data class SafetyInterlock(
    val id: String,
    val name: String,
    val description: String,
    val check: (ActuatorCommand, SystemState) -> InterlockResult
)

sealed class InterlockResult {
    data object Pass : InterlockResult()
    data class Fail(val reason: String) : InterlockResult()
    data class RequireApproval(val approvers: List<String>) : InterlockResult()
}

/**
 * System state for interlock evaluation.
 */
data class SystemState(
    val activeIncidents: Int,
    val suppressionActive: Boolean,
    val alarmActive: Boolean,
    val electricalIsolated: Boolean,
    val evacuationActive: Boolean,
    val manualOverrideActive: Boolean,
    val lastCommandTime: GuardianTimestamp?
)
