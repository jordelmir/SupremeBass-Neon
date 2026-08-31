package com.supremeguardian.core.shared

import java.time.Instant
import java.util.UUID

/**
 * Unique identifiers for Supreme Guardian domain objects.
 * Type-safe wrappers prevent mixing IDs across domains.
 */
@JvmInline
value class IncidentId(val value: String = UUID.randomUUID().toString())

@JvmInline
value class ZoneId(val value: String)

@JvmInline
value class CameraId(val value: String)

@JvmInline
value class SensorId(val value: String)

@JvmInline
value class NodeId(val value: String)

@JvmInline
value class CommandId(val value: String = UUID.randomUUID().toString())

@JvmInline
value class EvidenceId(val value: String = UUID.randomUUID().toString())

@JvmInline
value class BuildingId(val value: String)

@JvmInline
value class FloorId(val value: String)

/**
 * Observation authority — distinguishes measured data from derived/inferred data.
 */
enum class ObservationAuthority {
    /** Direct hardware measurement (radiometric, calibrated) */
    HARDWARE_MEASURED,
    /** Derived from other observations (sensor fusion) */
    DERIVED,
    /** Computed/inferred by AI model */
    AI_INFERRED,
    /** User-reported */
    USER_REPORTED,
    /** Digital simulation */
    SIMULATED
}

/**
 * Confidence level for observations.
 */
data class Confidence(
    val value: Double, // 0.0 to 1.0
    val authority: ObservationAuthority
) {
    init {
        require(value in 0.0..1.0) { "Confidence must be 0.0..1.0, was $value" }
    }
}

/**
 * Physical coordinates in 3D space.
 */
data class PhysicalCoordinates(
    val xMeters: Double,
    val yMeters: Double,
    val zMeters: Double
)

/**
 * Bounding box in 2D space (for camera FOV, zone boundaries, etc.)
 */
data class BoundingBox2D(
    val xMin: Double,
    val yMin: Double,
    val xMax: Double,
    val yMax: Double
)

/**
 * Timestamp wrapper with ISO-8601 serialization.
 */
data class GuardianTimestamp(
    val instant: Instant = Instant.now()
) {
    val iso8601: String get() = instant.toString()
    val epochMillis: Long get() = instant.toEpochMilli()

    fun elapsedSince(other: GuardianTimestamp): Long = instant.toEpochMilli() - other.instant.toEpochMilli()
}

/**
 * Audit entry — every significant action must be auditable.
 */
data class AuditEntry(
    val timestamp: GuardianTimestamp,
    val action: String,
    val actor: String,
    val details: Map<String, String> = emptyMap(),
    val evidenceIds: List<EvidenceId> = emptyList()
)
