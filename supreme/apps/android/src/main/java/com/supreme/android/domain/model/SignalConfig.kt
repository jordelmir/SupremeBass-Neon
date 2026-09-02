package com.supreme.android.domain.model

data class SignalConfig(
    val frequencyHz: Double,
    val amplitude: Float,
    val waveform: Waveform,
    val durationMs: Long = 0,
    val phaseRadians: Double = 0.0,
    val modulation: Modulation? = null,
    val envelope: Envelope? = null,
    val chirpEndHz: Double? = null,
    val noiseLowHz: Double? = null,
    val noiseHighHz: Double? = null
) {
    init {
        require(frequencyHz > 0.0) { "frequencyHz must be > 0, was $frequencyHz" }
        require(frequencyHz.isFinite()) { "frequencyHz must be finite, was $frequencyHz" }
        require(amplitude in 0.0f..1.0f) { "amplitude must be in 0.0..1.0, was $amplitude" }
        require(durationMs >= 0) { "durationMs must be >= 0, was $durationMs" }
        require(phaseRadians >= 0.0) { "phaseRadians must be >= 0, was $phaseRadians" }
    }

    fun withNyquistGuard(sampleRate: Int): SignalConfig {
        val nyquist = sampleRate / 2.0
        return if (frequencyHz >= nyquist) {
            copy(frequencyHz = nyquist * 0.99)
        } else {
            this
        }
    }
}

data class Modulation(
    val type: ModulationType,
    val rateHz: Double,
    val depth: Float
) {
    init {
        require(rateHz > 0.0) { "modulation rateHz must be > 0" }
        require(depth in 0.0f..1.0f) { "modulation depth must be in 0.0..1.0" }
    }
}

enum class ModulationType {
    AM,
    FM,
    PM
}

sealed interface Envelope {
    data class Ramp(
        val attackMs: Long,
        val releaseMs: Long
    ) : Envelope {
        init {
            require(attackMs >= 0) { "attackMs must be >= 0" }
            require(releaseMs >= 0) { "releaseMs must be >= 0" }
        }
    }

    data class Burst(
        val onMs: Long,
        val offMs: Long
    ) : Envelope {
        init {
            require(onMs > 0) { "onMs must be > 0" }
            require(offMs >= 0) { "offMs must be >= 0" }
        }
    }
}
