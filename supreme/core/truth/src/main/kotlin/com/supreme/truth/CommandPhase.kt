package com.supreme.truth

import java.time.Instant

/**
 * Command Lifecycle — tracks the full lifecycle of a physical command.
 *
 * Enforces: COMMAND_REQUESTED ≠ COMMAND_SENT ≠ ACKNOWLEDGED ≠ EFFECT_OBSERVED ≠ PHYSICALLY_VERIFIED
 */
data class CommandLifecycle(
    val commandId: String,
    val type: String,
    val requestedAt: Instant,
    val requestedBy: String,
    val transportAck: Boolean? = null,
    val sentAt: Instant? = null,
    val protocolAck: Boolean? = null,
    val acknowledgedAt: Instant? = null,
    val physicalState: String? = null,
    val observedAt: Instant? = null,
    val physicalEvidence: Map<String, String> = emptyMap(),
    val physicallyVerified: Boolean? = null,
    val verifiedAt: Instant? = null,
    val failureReason: String? = null,
    val phase: CommandPhase = CommandPhase.REQUESTED
) {
    fun sent(transportAck: Boolean): CommandLifecycle = copy(
        transportAck = transportAck,
        sentAt = Instant.now(),
        phase = if (transportAck) CommandPhase.SENT else CommandPhase.TRANSPORT_FAILED
    )

    fun acknowledged(protocolAck: Boolean): CommandLifecycle = copy(
        protocolAck = protocolAck,
        acknowledgedAt = Instant.now(),
        phase = if (protocolAck) CommandPhase.ACKNOWLEDGED else CommandPhase.PROTOCOL_FAILED
    )

    fun observed(
        physicalState: String,
        vararg evidence: Pair<String, String>
    ): CommandLifecycle = copy(
        physicalState = physicalState,
        observedAt = Instant.now(),
        physicalEvidence = physicalEvidence + evidence.toMap(),
        phase = CommandPhase.OBSERVED
    )

    fun verified(verified: Boolean, reason: String? = null): CommandLifecycle = copy(
        physicallyVerified = verified,
        verifiedAt = Instant.now(),
        failureReason = if (!verified) reason else null,
        phase = if (verified) CommandPhase.PHYSICALLY_VERIFIED else CommandPhase.PHYSICAL_EFFECT_FAILED
    )

    val isCompleted: Boolean get() = phase in setOf(
        CommandPhase.PHYSICALLY_VERIFIED,
        CommandPhase.PHYSICAL_EFFECT_FAILED,
        CommandPhase.TRANSPORT_FAILED,
        CommandPhase.PROTOCOL_FAILED,
        CommandPhase.CANCELLED
    )

    val isPhysicalSuccess: Boolean get() = phase == CommandPhase.PHYSICALLY_VERIFIED
}

enum class CommandPhase {
    REQUESTED,
    SENT,
    TRANSPORT_FAILED,
    ACKNOWLEDGED,
    PROTOCOL_FAILED,
    OBSERVED,
    PHYSICALLY_VERIFIED,
    PHYSICAL_EFFECT_FAILED,
    CANCELLED
}
