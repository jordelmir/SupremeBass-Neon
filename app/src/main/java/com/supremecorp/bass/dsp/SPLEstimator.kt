package com.supremecorp.bass.dsp

import kotlin.math.log10
import kotlin.math.sqrt

/**
 * SPL (Sound Pressure Level) estimator.
 *
 * Note: Without a calibrated microphone, this provides RELATIVE SPL values only.
 * The values represent relative loudness, not absolute dB SPL.
 *
 * For absolute SPL measurement, a calibrated microphone and reference signal are required.
 */
class SPLEstimator {

    companion object {
        private const val TAG = "SPLEstimator"

        // Reference values (relative, not absolute SPL)
        private const val REFERENCE_PRESSURE = 1.0 // Normalized reference
    }

    data class SPLResult(
        val splDb: Double,           // Relative SPL in dB
        val splDbFS: Double,         // Level in dBFS (full scale)
        val peakDb: Double,          // Peak level
        val rmsDb: Double,           // RMS level
        val crestFactorDb: Double,   // Peak to RMS ratio
        val loudnessLUFS: Double,    // Integrated loudness (EBU R128)
        val dynamicRangeDb: Double   // Dynamic range
    )

    /**
     * Estimate SPL from audio samples.
     *
     * @param samples Audio samples (normalized -1.0 to 1.0)
     * @param sampleRate Sample rate in Hz
     * @return SPL estimation result
     */
    fun estimate(samples: FloatArray, sampleRate: Int): SPLResult {
        val n = samples.size

        // Calculate RMS
        var sumSquares = 0.0
        var peak = 0.0

        for (s in samples) {
            val abs = kotlin.math.abs(s).toDouble()
            if (abs > peak) peak = abs
            sumSquares += s.toDouble() * s
        }

        val rms = sqrt(sumSquares / n)

        // Convert to dB (relative to full scale)
        val rmsDbFS = if (rms > 1e-20) 20.0 * log10(rms) else -200.0
        val peakDbFS = if (peak > 1e-20) 20.0 * log10(peak) else -200.0

        // Relative SPL (shifted so typical speech is ~60-70 dB)
        // This is NOT absolute SPL - just relative loudness
        val splDb = rmsDbFS + 94.0 // Offset to approximate typical SPL range

        // Crest factor (headroom)
        val crestFactorDb = peakDbFS - rmsDbFS

        // Simple loudness estimation (EBU R128 inspired)
        val loudness = calculateIntegratedLoudness(samples, sampleRate)

        // Dynamic range estimation
        val dynamicRange = estimateDynamicRange(samples, sampleRate)

        return SPLResult(
            splDb = splDb.coerceIn(0.0, 140.0),
            splDbFS = rmsDbFS.coerceIn(-100.0, 0.0),
            peakDb = peakDbFS.coerceIn(-100.0, 0.0),
            rmsDb = rmsDbFS.coerceIn(-100.0, 0.0),
            crestFactorDb = crestFactorDb.coerceIn(0.0, 30.0),
            loudnessLUFS = loudness.coerceIn(-70.0, 0.0),
            dynamicRangeDb = dynamicRange.coerceIn(0.0, 60.0)
        )
    }

    /**
     * Calculate integrated loudness (simplified EBU R128).
     */
    private fun calculateIntegratedLoudness(samples: FloatArray, sampleRate: Int): Double {
        val blockSize = (sampleRate * 0.1).toInt() // 100ms blocks
        val blocks = mutableListOf<Double>()

        var pos = 0
        while (pos + blockSize <= samples.size) {
            var sumSquares = 0.0
            for (i in 0 until blockSize) {
                val s = samples[pos + i].toDouble()
                sumSquares += s * s
            }
            val rms = sqrt(sumSquares / blockSize)
            blocks.add(rms)
            pos += blockSize
        }

        if (blocks.isEmpty()) return -70.0

        // Gated loudness (simplified)
        val sorted = blocks.sorted()
        val gateThreshold = sorted[(sorted.size * 0.1).toInt()] // 10th percentile

        val gatedBlocks = blocks.filter { it > gateThreshold }
        if (gatedBlocks.isEmpty()) return -70.0

        val meanSquare = gatedBlocks.map { it * it }.average()
        return if (meanSquare > 1e-20) {
            -0.691 + 10.0 * log10(meanSquare)
        } else {
            -70.0
        }
    }

    /**
     * Estimate dynamic range (difference between loud and quiet sections).
     */
    private fun estimateDynamicRange(samples: FloatArray, sampleRate: Int): Double {
        val blockSize = (sampleRate * 0.5).toInt() // 500ms blocks
        val blockLevels = mutableListOf<Double>()

        var pos = 0
        while (pos + blockSize <= samples.size) {
            var sumSquares = 0.0
            for (i in 0 until blockSize) {
                val s = samples[pos + i].toDouble()
                sumSquares += s * s
            }
            val rms = sqrt(sumSquares / blockSize)
            val db = if (rms > 1e-20) 20.0 * log10(rms) else -200.0
            blockLevels.add(db)
            pos += blockSize
        }

        if (blockLevels.size < 2) return 0.0

        val sorted = blockLevels.sorted()
        val p95 = sorted[(sorted.size * 0.95).toInt()]
        val p5 = sorted[(sorted.size * 0.05).toInt()]

        return (p95 - p5).coerceIn(0.0, 60.0)
    }

    /**
     * Get a descriptive label for the SPL level.
     */
    fun getSPLDescription(splDb: Double): String {
        return when {
            splDb < 30 -> "Very quiet (whisper)"
            splDb < 50 -> "Quiet (quiet room)"
            splDb < 65 -> "Moderate (normal conversation)"
            splDb < 80 -> "Loud (busy traffic)"
            splDb < 90 -> "Very loud (power tools)"
            splDb < 100 -> "Extremely loud (motorcycle)"
            splDb < 110 -> "Pain threshold (rock concert)"
            splDb < 120 -> "Painful (jackhammer)"
            splDb < 130 -> "Threshold of pain"
            else -> "Dangerous (hearing damage risk)"
        }
    }

    /**
     * Check if SPL level poses hearing risk.
     */
    fun isHearingRisk(splDb: Double, durationSeconds: Double): Boolean {
        // OSHA/NIOSH exposure limits (simplified)
        return when {
            splDb >= 115 -> true // Always risky
            splDb >= 100 && durationSeconds > 15 * 60 -> true // 15 minutes
            splDb >= 95 && durationSeconds > 45 * 60 -> true // 45 minutes
            splDb >= 90 && durationSeconds > 8 * 3600 -> true // 8 hours
            splDb >= 85 && durationSeconds > 8 * 3600 -> true // 8 hours
            else -> false
        }
    }

    /**
     * Get maximum safe exposure time for given SPL.
     */
    fun getMaxExposureTime(splDb: Double): Double {
        return when {
            splDb >= 115 -> 0.0 // Immediate danger
            splDb >= 100 -> 15.0 * 60 // 15 minutes
            splDb >= 95 -> 45.0 * 60 // 45 minutes
            splDb >= 90 -> 8.0 * 3600 // 8 hours
            splDb >= 85 -> 8.0 * 3600 // 8 hours
            else -> Double.MAX_VALUE // Safe indefinitely
        }
    }
}
