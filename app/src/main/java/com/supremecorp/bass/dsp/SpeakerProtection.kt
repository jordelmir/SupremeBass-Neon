package com.supremecorp.bass.dsp

import com.supremecorp.bass.core.logging.AppLogger
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.math.ln
import kotlin.math.max

class SpeakerProtection {

    companion object {
        private const val TAG = "SpeakerProtection"
        private const val THERMAL_TIME_CONSTANT = 2.0
        private const val MAX_THERMAL_POWER = 0.8
        private const val EXCURSION_LIMIT = 0.9
        private const val EXCURSION_RELEASE_RATE = 0.05
    }

    enum class ProtectionStatus { SAFE, WARNING, REDUCING, PROTECTED }

    data class ProtectionState(
        val status: ProtectionStatus,
        val thermalPower: Double,
        val thermalLimit: Double,
        val excursion: Double,
        val excursionLimit: Double,
        val gainReductionDb: Float,
        val message: String
    )

    private var thermalPwr: Double = 0.0
    private var thermalGain: Double = 0.0
    private var excursionLvl: Double = 0.0
    private var excursionGain: Double = 0.0
    private var lastUpdate: Long = System.currentTimeMillis()

    fun reset() {
        thermalPwr = 0.0
        thermalGain = 0.0
        excursionLvl = 0.0
        excursionGain = 0.0
        lastUpdate = System.currentTimeMillis()
    }

    fun update(
        buffer: FloatArray,
        frameCount: Int,
        channelCount: Int,
        sampleRate: Int
    ): ProtectionState {
        val now = System.currentTimeMillis()
        val dt = (now - lastUpdate) / 1000.0
        lastUpdate = now

        var sumSquares = 0.0
        var peak = 0.0
        for (i in 0 until frameCount) {
            for (ch in 0 until channelCount) {
                val idx = i * channelCount + ch
                if (idx < buffer.size) {
                    val s = buffer[idx].toDouble()
                    sumSquares += s * s
                    val a = abs(s)
                    if (a > peak) peak = a
                }
            }
        }

        val rms = sqrt(sumSquares / (frameCount * channelCount))
        thermalPwr = thermalPwr * (1.0 - dt / THERMAL_TIME_CONSTANT) + rms * (dt / THERMAL_TIME_CONSTANT)
        excursionLvl = max(excursionLvl * (1.0 - EXCURSION_RELEASE_RATE * dt), peak)

        thermalGain = if (thermalPwr > MAX_THERMAL_POWER * 0.7) {
            ((thermalPwr - MAX_THERMAL_POWER * 0.7) / (MAX_THERMAL_POWER * 0.3)).coerceIn(0.0, 1.0)
        } else 0.0

        excursionGain = if (excursionLvl > EXCURSION_LIMIT * 0.7) {
            ((excursionLvl - EXCURSION_LIMIT * 0.7) / (EXCURSION_LIMIT * 0.3)).coerceIn(0.0, 1.0)
        } else 0.0

        val totalReduction = max(thermalGain, excursionGain)
        val status = when {
            totalReduction > 0.5 -> ProtectionStatus.PROTECTED
            totalReduction > 0.1 -> ProtectionStatus.REDUCING
            thermalPwr > MAX_THERMAL_POWER * 0.5 || excursionLvl > EXCURSION_LIMIT * 0.5 -> ProtectionStatus.WARNING
            else -> ProtectionStatus.SAFE
        }

        val reductionDb = if (totalReduction > 0.0) {
            (20.0 * ln(1.0 - totalReduction) / ln(10.0)).toFloat()
        } else 0f

        val msg = when (status) {
            ProtectionStatus.SAFE -> "Operating normally"
            ProtectionStatus.WARNING -> "Approaching limits"
            ProtectionStatus.REDUCING -> "Reducing gain for protection"
            ProtectionStatus.PROTECTED -> "Protection active"
        }

        return ProtectionState(status, thermalPwr, MAX_THERMAL_POWER, excursionLvl, EXCURSION_LIMIT, reductionDb, msg)
    }

    fun applyProtection(buffer: FloatArray, frameCount: Int, channelCount: Int, state: ProtectionState) {
        if (state.status == ProtectionStatus.SAFE || state.status == ProtectionStatus.WARNING) return
        val totalReduction = max(thermalGain, excursionGain)
        val gain = (1.0 - totalReduction).coerceIn(0.1, 1.0).toFloat()
        for (i in 0 until frameCount * channelCount) {
            if (i < buffer.size) buffer[i] *= gain
        }
    }
}
