package com.supremecorp.bass.dsp

import kotlin.math.log10
import kotlin.math.sqrt

/**
 * SPL (Sound Pressure Level) estimator.
 *
 * CALIBRATION DOCTRINE:
 * ─────────────────────
 * Without a calibrated measurement microphone, this provides:
 *   - dBFS (decibels relative to full scale) — hardware-dependent
 *   - Relative level comparisons between recordings
 *   - Loudness trends over time
 *
 * This does NOT provide:
 *   - Absolute dB SPL (sound pressure level in Pascals)
 *   - Calibrated acoustic measurements
 *
 * For absolute dB SPL, you need:
 *   1. Calibrated measurement microphone (e.g., Class 1 or Class 2)
 *   2. Known microphone sensitivity (V/Pa)
 *   3. Calibration reference (e.g., 94 dB SPL @ 1 kHz acoustic calibrator)
 *   4. Transfer function: mic → preamp → ADC → digital
 *
 * The relationship is:
 *   dB SPL = dBFS + microphone_sensitivity_dBFS_per_Pa + calibration_offset
 *
 * Until calibration is performed, report as:
 *   "Relative Level: X dBFS" (NOT "SPL: X dB")
 */
class SPLEstimator {

    companion object {
        private const val TAG = "SPLEstimator"
    }

    /**
     * Result of SPL estimation.
     *
     * IMPORTANT: splDb is RELATIVE ONLY — not absolute dB SPL.
     * For absolute SPL, a calibrated microphone and reference are required.
     */
    data class SPLResult(
        val relativeLevelDb: Double,   // Relative level in dB (NOT absolute SPL)
        val splDbFS: Double,           // Level in dBFS (full scale)
        val peakDb: Double,            // Peak level in dBFS
        val rmsDb: Double,             // RMS level in dBFS
        val crestFactorDb: Double,     // Peak to RMS ratio
        val loudnessLUFS: Double,      // Integrated loudness (EBU R128)
        val dynamicRangeDb: Double     // Dynamic range
    )

    /**
     * Estimate level from audio samples.
     *
     * @param samples Audio samples (normalized -1.0 to 1.0)
     * @param sampleRate Sample rate in Hz
     * @return Level estimation result (relative, not absolute SPL)
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

        // Relative level — NO arbitrary offset
        // This is dBFS, not dB SPL. To convert to dB SPL, use:
        //   dB_SPL = dBFS + mic_sensitivity + cal_offset
        val relativeLevel = rmsDbFS

        // Crest factor (headroom)
        val crestFactorDb = peakDbFS - rmsDbFS

        // Simple loudness estimation (EBU R128 inspired)
        val loudness = calculateIntegratedLoudness(samples, sampleRate)

        // Dynamic range estimation
        val dynamicRange = estimateDynamicRange(samples, sampleRate)

        return SPLResult(
            relativeLevelDb = relativeLevel.coerceIn(-100.0, 0.0),
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
     * Get a descriptive label for the relative level.
     *
     * NOTE: These are rough approximations based on typical recording levels.
     * Actual SPL depends on microphone sensitivity and gain staging.
     */
    fun getLevelDescription(relativeLevelDb: Double): String {
        return when {
            relativeLevelDb < -60 -> "Very quiet (near noise floor)"
            relativeLevelDb < -40 -> "Quiet (low-level signal)"
            relativeLevelDb < -20 -> "Moderate (typical speech level)"
            relativeLevelDb < -10 -> "Loud (strong signal)"
            relativeLevelDb < -6 -> "Very loud (near clipping)"
            else -> "Extremely loud (approaching full scale)"
        }
    }

    /**
     * Get calibration instructions for converting to absolute SPL.
     */
    fun getCalibrationInstructions(): String {
        return """
            To convert relative dBFS to absolute dB SPL:
            
            1. Use a calibrated measurement microphone (Class 1 or Class 2)
            2. Connect to a 94 dB SPL acoustic calibrator (1 kHz tone)
            3. Record the calibrator output
            4. Measure the dBFS level of the recorded calibrator tone
            5. Calculate: offset = 94.0 - measured_dBFS
            6. Apply: dB_SPL = dBFS + offset
            
            Example:
              - Calibrator: 94 dB SPL @ 1 kHz
              - Recorded level: -26 dBFS
              - Offset: 94 - (-26) = 120
              - Future measurement at -20 dBFS = -20 + 120 = 100 dB SPL
            
            Reference: IEC 61672-1 (Sound Level Meters)
        """.trimIndent()
    }
}
