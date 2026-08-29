package com.supremecorp.bass.domain.model

import java.util.UUID

enum class FlameType {
    CANDLE,
    PROPANE_TORCH,
    BUTANE_LIGHTER,
    WAX_WARMING_LAMP,
    ALCOHOL_LAMP,
    CUSTOM
}

data class FlameExperimentConfig(
    val flameType: FlameType,
    val distanceMeters: Double,
    val durationSeconds: Int,
    val waveform: Waveform,
    val frequencyHz: Double,
    val amplitude: Double,
    val testNotes: String = ""
) {
    init {
        require(distanceMeters in 0.1..5.0) { "Distance must be 0.1-5.0 meters" }
        require(durationSeconds in 1..300) { "Duration must be 1-300 seconds" }
        require(frequencyHz in 20.0..20000.0) { "Frequency must be 20-20000 Hz" }
        require(amplitude in 0.0..1.0) { "Amplitude must be 0.0-1.0" }
    }
}

data class FlameExperimentResult(
    val id: String = UUID.randomUUID().toString(),
    val config: FlameExperimentConfig,
    val outputRoute: OutputRoute,
    val measuredPeakAmplitude: Double,
    val measuredRmsAmplitude: Double,
    val observationNotes: String,
    val completedAtMs: Long = System.currentTimeMillis()
)

enum class FlameSafetyViolation {
    HEADPHONES_DETECTED,
    DURATION_EXCEEDED,
    COOLDOWN_ACTIVE,
    AMPLITUDE_TOO_HIGH,
    FREQUENCY_OUT_OF_RANGE,
    DISTANCE_TOO_CLOSE,
    DISTANCE_TOO_FAR
}

data class FlameSafetyState(
    val isSafe: Boolean = true,
    val violations: List<FlameSafetyViolation> = emptyList(),
    val lastExperimentMs: Long? = null,
    val cooldownRemainingMs: Long = 0
) {
    val canStart: Boolean get() = isSafe && violations.isEmpty()
}
