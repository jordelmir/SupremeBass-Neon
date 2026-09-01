package com.supreme.core

import java.time.Instant
import java.util.UUID

/**
 * Time Machine — immutable event log for the physical world.
 *
 * Every state change in Supreme is recorded as an immutable event.
 * State at time t = reduce(events[0..t]).
 *
 * This enables:
 *   - "How was this car on June 3?"
 *   - "When did vibration start increasing?"
 *   - "What changed before the failure?"
 *   - "What was the board temperature 30 min before the incident?"
 *   - Complete incident reconstruction from event log
 *   - Before/after repair comparison
 *
 * Events are append-only, hash-chained, and optionally signed.
 * This is the write-ahead log for the physical world.
 */

// ─────────────────────────────────────────────────────────────
// EVENT TYPES
// ─────────────────────────────────────────────────────────────

/**
 * Base interface for all events in the Time Machine.
 * Every event is immutable, timestamped, and carries a sequence number.
 */
interface PhysicalEvent {
    val eventId: String
    val aggregateId: String
    val timestamp: Instant
    val sequenceNumber: Long
    val eventType: String
    val actor: String
    val metadata: Map<String, String>
    val previousHash: String?
    val hash: String
}

/**
 * All event types in the Supreme Time Machine.
 */
sealed class SupremeEvent : PhysicalEvent {

    // ── Asset Lifecycle ──

    data class AssetCreated(
        override val eventId: String = UUID.randomUUID().toString(),
        override val aggregateId: String,
        override val timestamp: Instant = Instant.now(),
        override val sequenceNumber: Long = 0,
        override val actor: String = "system",
        override val metadata: Map<String, String> = emptyMap(),
        override val previousHash: String? = null,
        override val hash: String = "",
        val asset: Asset
    ) : SupremeEvent() { override val eventType = "ASSET_CREATED" }

    data class AssetUpdated(
        override val eventId: String = UUID.randomUUID().toString(),
        override val aggregateId: String,
        override val timestamp: Instant = Instant.now(),
        override val sequenceNumber: Long = 0,
        override val actor: String = "system",
        override val metadata: Map<String, String> = emptyMap(),
        override val previousHash: String? = null,
        override val hash: String = "",
        val field: String,
        val oldValue: String?,
        val newValue: String?
    ) : SupremeEvent() { override val eventType = "ASSET_UPDATED" }

    // ── Device Lifecycle ──

    data class DeviceConnected(
        override val eventId: String = UUID.randomUUID().toString(),
        override val aggregateId: String,
        override val timestamp: Instant = Instant.now(),
        override val sequenceNumber: Long = 0,
        override val actor: String = "system",
        override val metadata: Map<String, String> = emptyMap(),
        override val previousHash: String? = null,
        override val hash: String = "",
        val deviceId: DeviceId,
        val protocol: DeviceProtocol
    ) : SupremeEvent() { override val eventType = "DEVICE_CONNECTED" }

    data class DeviceDisconnected(
        override val eventId: String = UUID.randomUUID().toString(),
        override val aggregateId: String,
        override val timestamp: Instant = Instant.now(),
        override val sequenceNumber: Long = 0,
        override val actor: String = "system",
        override val metadata: Map<String, String> = emptyMap(),
        override val previousHash: String? = null,
        override val hash: String = "",
        val deviceId: DeviceId,
        val reason: String
    ) : SupremeEvent() { override val eventType = "DEVICE_DISCONNECTED" }

    // ── Observations ──

    data class ObservationRecorded(
        override val eventId: String = UUID.randomUUID().toString(),
        override val aggregateId: String,
        override val timestamp: Instant = Instant.now(),
        override val sequenceNumber: Long = 0,
        override val actor: String = "system",
        override val metadata: Map<String, String> = emptyMap(),
        override val previousHash: String? = null,
        override val hash: String = "",
        val observation: Observation
    ) : SupremeEvent() { override val eventType = "OBSERVATION_RECORDED" }

    // ── Anomalies ──

    data class AnomalyDetected(
        override val eventId: String = UUID.randomUUID().toString(),
        override val aggregateId: String,
        override val timestamp: Instant = Instant.now(),
        override val sequenceNumber: Long = 0,
        override val actor: String = "system",
        override val metadata: Map<String, String> = emptyMap(),
        override val previousHash: String? = null,
        override val hash: String = "",
        val anomaly: Anomaly
    ) : SupremeEvent() { override val eventType = "ANOMALY_DETECTED" }

    data class AnomalyResolved(
        override val eventId: String = UUID.randomUUID().toString(),
        override val aggregateId: String,
        override val timestamp: Instant = Instant.now(),
        override val sequenceNumber: Long = 0,
        override val actor: String = "system",
        override val metadata: Map<String, String> = emptyMap(),
        override val previousHash: String? = null,
        override val hash: String = "",
        val anomalyId: AnomalyId,
        val resolution: String
    ) : SupremeEvent() { override val eventType = "ANOMALY_RESOLVED" }

    // ── Commands ──

    data class CommandRequested(
        override val eventId: String = UUID.randomUUID().toString(),
        override val aggregateId: String,
        override val timestamp: Instant = Instant.now(),
        override val sequenceNumber: Long = 0,
        override val actor: String = "user",
        override val metadata: Map<String, String> = emptyMap(),
        override val previousHash: String? = null,
        override val hash: String = "",
        val commandId: String,
        val commandType: String,
        val targetDeviceId: DeviceId
    ) : SupremeEvent() { override val eventType = "COMMAND_REQUESTED" }

    data class CommandSent(
        override val eventId: String = UUID.randomUUID().toString(),
        override val aggregateId: String,
        override val timestamp: Instant = Instant.now(),
        override val sequenceNumber: Long = 0,
        override val actor: String = "system",
        override val metadata: Map<String, String> = emptyMap(),
        override val previousHash: String? = null,
        override val hash: String = "",
        val commandId: String,
        val transportAck: Boolean
    ) : SupremeEvent() { override val eventType = "COMMAND_SENT" }

    data class CommandAcknowledged(
        override val eventId: String = UUID.randomUUID().toString(),
        override val aggregateId: String,
        override val timestamp: Instant = Instant.now(),
        override val sequenceNumber: Long = 0,
        override val actor: String = "system",
        override val metadata: Map<String, String> = emptyMap(),
        override val previousHash: String? = null,
        override val hash: String = "",
        val commandId: String,
        val protocolAck: Boolean
    ) : SupremeEvent() { override val eventType = "COMMAND_ACKNOWLEDGED" }

    data class PhysicalEffectObserved(
        override val eventId: String = UUID.randomUUID().toString(),
        override val aggregateId: String,
        override val timestamp: Instant = Instant.now(),
        override val sequenceNumber: Long = 0,
        override val actor: String = "system",
        override val metadata: Map<String, String> = emptyMap(),
        override val previousHash: String? = null,
        override val hash: String = "",
        val commandId: String,
        val physicalState: String,
        val evidence: Map<String, String>
    ) : SupremeEvent() { override val eventType = "PHYSICAL_EFFECT_OBSERVED" }

    data class PhysicalEffectVerified(
        override val eventId: String = UUID.randomUUID().toString(),
        override val aggregateId: String,
        override val timestamp: Instant = Instant.now(),
        override val sequenceNumber: Long = 0,
        override val actor: String = "system",
        override val metadata: Map<String, String> = emptyMap(),
        override val previousHash: String? = null,
        override val hash: String = "",
        val commandId: String,
        val verified: Boolean,
        val reason: String? = null
    ) : SupremeEvent() { override val eventType = "PHYSICAL_EFFECT_VERIFIED" }

    // ── Maintenance ──

    data class MaintenanceScheduled(
        override val eventId: String = UUID.randomUUID().toString(),
        override val aggregateId: String,
        override val timestamp: Instant = Instant.now(),
        override val sequenceNumber: Long = 0,
        override val actor: String = "system",
        override val metadata: Map<String, String> = emptyMap(),
        override val previousHash: String? = null,
        override val hash: String = "",
        val task: MaintenanceTask
    ) : SupremeEvent() { override val eventType = "MAINTENANCE_SCHEDULED" }

    data class MaintenanceCompleted(
        override val eventId: String = UUID.randomUUID().toString(),
        override val aggregateId: String,
        override val timestamp: Instant = Instant.now(),
        override val sequenceNumber: Long = 0,
        override val actor: String = "user",
        override val metadata: Map<String, String> = emptyMap(),
        override val previousHash: String? = null,
        override val hash: String = "",
        val taskId: MaintenanceId,
        val cost: Double? = null,
        val notes: String? = null
    ) : SupremeEvent() { override val eventType = "MAINTENANCE_COMPLETED" }

    // ── Incidents ──

    data class IncidentCreated(
        override val eventId: String = UUID.randomUUID().toString(),
        override val aggregateId: String,
        override val timestamp: Instant = Instant.now(),
        override val sequenceNumber: Long = 0,
        override val actor: String = "system",
        override val metadata: Map<String, String> = emptyMap(),
        override val previousHash: String? = null,
        override val hash: String = "",
        val incidentId: String,
        val severity: Severity,
        val description: String,
        val sensorIds: List<String>
    ) : SupremeEvent() { override val eventType = "INCIDENT_CREATED" }

    data class IncidentResolved(
        override val eventId: String = UUID.randomUUID().toString(),
        override val aggregateId: String,
        override val timestamp: Instant = Instant.now(),
        override val sequenceNumber: Long = 0,
        override val actor: String = "user",
        override val metadata: Map<String, String> = emptyMap(),
        override val previousHash: String? = null,
        override val hash: String = "",
        val incidentId: String,
        val resolution: String
    ) : SupremeEvent() { override val eventType = "INCIDENT_RESOLVED" }
}

// ─────────────────────────────────────────────────────────────
// EVENT STORE INTERFACE
// ─────────────────────────────────────────────────────────────

/**
 * Append-only event store for the Time Machine.
 * All implementations MUST guarantee:
 *   - Events are immutable after writing
 *   - Sequence numbers are monotonic per aggregate
 *   - Hash chain is maintained
 *   - No event is ever modified or deleted
 */
interface EventStore {
    /** Append events to the store. Events must be in sequence order. */
    suspend fun append(events: List<PhysicalEvent>)

    /** Read all events for an aggregate, in order. */
    suspend fun readAggregate(aggregateId: String): List<PhysicalEvent>

    /** Read events from a timestamp. */
    suspend fun readFrom(from: Instant): List<PhysicalEvent>

    /** Read events in a time range. */
    suspend fun readRange(from: Instant, to: Instant): List<PhysicalEvent>

    /** Read the N most recent events for an aggregate. */
    suspend fun readRecent(aggregateId: String, count: Int): List<PhysicalEvent>

    /** Get the current sequence number for an aggregate. */
    suspend fun currentSequence(aggregateId: String): Long

    /** Verify the hash chain integrity for an aggregate. */
    suspend fun verifyIntegrity(aggregateId: String): IntegrityResult
}

data class IntegrityResult(
    val valid: Boolean,
    val totalEvents: Long,
    val firstBrokenSequence: Long? = null,
    val error: String? = null
)

// ─────────────────────────────────────────────────────────────
// STATE RECONSTRUCTION
// ─────────────────────────────────────────────────────────────

/**
 * Reconstructs state from events.
 * State(t) = reduce(events[0..t])
 */
interface StateReconstructor<T> {
    /** Initial empty state. */
    fun empty(): T

    /** Apply an event to the current state. */
    fun apply(state: T, event: PhysicalEvent): T

    /** Reconstruct state at a specific time. */
    suspend fun stateAt(store: EventStore, aggregateId: String, timestamp: Instant): T {
        val events = store.readAggregate(aggregateId)
            .filter { it.timestamp <= timestamp }
        return events.fold(empty()) { state, event -> apply(state, event) }
    }

    /** Reconstruct current state. */
    suspend fun currentState(store: EventStore, aggregateId: String): T {
        val events = store.readAggregate(aggregateId)
        return events.fold(empty()) { state, event -> apply(state, event) }
    }
}
