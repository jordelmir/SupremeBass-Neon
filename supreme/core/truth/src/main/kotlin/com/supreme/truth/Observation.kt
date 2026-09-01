package com.supreme.truth

import java.time.Instant

/**
 * Observation<T> — a single data point with full provenance.
 *
 * This is the atomic unit of truth in Supreme.
 * Every sensor reading, user input, device report, and derived value
 * is wrapped in Observation to enforce traceability.
 */
data class Observation<T>(
    val value: T,
    val unit: MeasurementUnit,
    val authority: TruthAuthority,
    val source: String,
    val timestamp: Instant,
    val rawPayload: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val uncertainty: Double? = null,
    val calibrationId: String? = null,
    val lastValidated: Instant = timestamp,
    val confidence: Double? = null,
    val methodVersion: String = "1.0"
) {
    val isPhysical: Boolean get() = authority.isTrustedForPhysical

    fun isStale(maxAgeMs: Long): Boolean {
        return System.currentTimeMillis() - timestamp.toEpochMilli() > maxAgeMs
    }

    fun <R> derive(
        newValue: R,
        newUnit: MeasurementUnit,
        derivationMethod: String
    ): Observation<R> = Observation(
        value = newValue,
        unit = newUnit,
        authority = TruthAuthority.DERIVED,
        source = "$source → $derivationMethod",
        timestamp = timestamp,
        metadata = metadata + ("derivedFrom" to source),
        methodVersion = methodVersion
    )
}

typealias SensorObservation = Observation<Double>
typealias StateObservation = Observation<Boolean>
typealias TextObservation = Observation<String>
