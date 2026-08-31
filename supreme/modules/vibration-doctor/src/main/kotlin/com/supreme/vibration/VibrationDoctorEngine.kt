package com.supreme.vibration

import com.supreme.core.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.time.Instant
import kotlin.math.*

/**
 * Supreme Vibration Doctor — "Is this vibrating normally?"
 *
 * Uses the phone's accelerometer to measure vibration of equipment.
 * Place the phone on top of the machine and record for 10-30 seconds.
 *
 * Detects:
 * - Vibration level (RMS, peak)
 * - Dominant frequency
 * - Changes from baseline
 * - Anomalous patterns
 *
 * Works with:
 * - Washing machines, motors, pumps, generators
 * - Fridges, vehicle dashboards, power tools
 */

class VibrationDoctorEngine {

    private val baselines = mutableMapOf<String, VibrationBaseline>()
    private val _analyses = MutableSharedFlow<VibrationAnalysis>(replay = 1)
    val analyses: SharedFlow<VibrationAnalysis> = _analyses

    /**
     * Analyze vibration data from accelerometer.
     */
    suspend fun analyze(
        x: FloatArray,
        y: FloatArray,
        z: FloatArray,
        sampleRate: Float = 100f, // Default 100 Hz
        assetId: String? = null,
        assetName: String? = null,
        assetCategory: AssetCategory? = null
    ): VibrationAnalysis {
        // 1. Calculate RMS for each axis
        val rmsX = calculateRMS(x)
        val rmsY = calculateRMS(y)
        val rmsZ = calculateRMS(z)

        // 2. Calculate total RMS
        val totalRMS = sqrt(rmsX * rmsX + rmsY * rmsY + rmsZ * rmsZ)

        // 3. Calculate peak values
        val peakX = x.maxOrNull()?.let { abs(it) } ?: 0f
        val peakY = y.maxOrNull()?.let { abs(it) } ?: 0f
        val peakZ = z.maxOrNull()?.let { abs(it) } ?: 0f
        val totalPeak = maxOf(peakX, peakY, peakZ)

        // 4. FFT on dominant axis (usually Z for vertical machines)
        val dominantAxis = when {
            rmsZ >= rmsX && rmsZ >= rmsY -> z
            rmsY >= rmsX -> y
            else -> x
        }
        val fftResult = performFFT(dominantAxis, sampleRate)

        // 5. Detect dominant frequency
        val dominantFreq = detectDominantFrequency(fftResult)

        // 6. Detect harmonics
        val harmonics = detectHarmonics(dominantFreq, fftResult)

        // 7. Calculate spectral features
        val spectralFeatures = calculateSpectralFeatures(fftResult, sampleRate)

        // 8. Compare with baseline
        val baseline = assetId?.let { baselines[it] }
        val deviation = baseline?.let { calculateDeviation(totalRMS, dominantFreq, it) }

        // 9. Classify vibration level
        val vibrationLevel = classifyVibrationLevel(totalRMS, assetCategory)

        // 10. Generate diagnosis
        val diagnosis = generateDiagnosis(
            totalRMS, dominantFreq, harmonics, spectralFeatures,
            deviation, vibrationLevel, assetCategory
        )

        val analysis = VibrationAnalysis(
            timestamp = Instant.now(),
            rmsG = totalRMS,
            peakG = totalPeak.toDouble(),
            rmsX = rmsX,
            rmsY = rmsY,
            rmsZ = rmsZ,
            dominantFrequency = dominantFreq,
            harmonics = harmonics,
            spectralCentroid = spectralFeatures.centroid,
            spectralRolloff = spectralFeatures.rolloff,
            spectralBandwidth = spectralFeatures.bandwidth,
            vibrationLevel = vibrationLevel,
            diagnosis = diagnosis,
            baselineDeviation = deviation,
            assetId = assetId,
            assetName = assetName,
            sampleRate = sampleRate,
            durationMs = (x.size.toLong() / sampleRate.toLong()) * 1000
        )

        _analyses.tryEmit(analysis)
        return analysis
    }

    /**
     * Set baseline for an asset.
     */
    fun setBaseline(assetId: String, analysis: VibrationAnalysis) {
        baselines[assetId] = VibrationBaseline(
            rms = analysis.rmsG,
            dominantFrequency = analysis.dominantFrequency,
            timestamp = Instant.now()
        )
    }

    /**
     * Get baseline for an asset.
     */
    fun getBaseline(assetId: String): VibrationBaseline? {
        return baselines[assetId]
    }

    /**
     * Compare two vibration measurements.
     */
    suspend fun compare(before: VibrationAnalysis, after: VibrationAnalysis): VibrationComparison {
        val rmsChange = if (before.rmsG > 0) {
            ((after.rmsG - before.rmsG) / before.rmsG * 100)
        } else 0.0

        val freqChange = if (before.dominantFrequency > 0) {
            ((after.dominantFrequency - before.dominantFrequency) / before.dominantFrequency * 100)
        } else 0.0

        val improvement = rmsChange < -10

        return VibrationComparison(
            before = before,
            after = after,
            rmsChange = rmsChange,
            frequencyChange = freqChange,
            improvementDetected = improvement,
            summary = if (improvement) {
                "Vibration reduced after intervention"
            } else if (rmsChange > 10) {
                "Vibration increased — may indicate worsening condition"
            } else {
                "No significant change detected"
            }
        )
    }

    // ─────────────────────────────────────────────────────────────
    // ANALYSIS
    // ─────────────────────────────────────────────────────────────

    private fun calculateRMS(data: FloatArray): Double {
        if (data.isEmpty()) return 0.0
        val sumOfSquares = data.sumOf { it.toDouble().pow(2) }
        return sqrt(sumOfSquares / data.size)
    }

    private fun performFFT(data: FloatArray, sampleRate: Float): FFTResult {
        val n = data.size
        val spectrum = FloatArray(n / 2)

        for (k in 0 until n / 2) {
            var real = 0.0
            var imag = 0.0
            for (j in 0 until n) {
                val angle = 2.0 * PI * k * j / n
                real += data[j] * cos(angle).toFloat()
                imag -= data[j] * sin(angle).toFloat()
            }
            spectrum[k] = sqrt(real * real + imag * imag).toFloat() / n
        }

        return FFTResult(
            spectrum = spectrum,
            bins = n / 2,
            binWidth = sampleRate / n
        )
    }

    private fun detectDominantFrequency(fft: FFTResult): Double {
        var maxMagnitude = 0f
        var maxBin = 0

        for (k in 1 until fft.bins) {
            if (fft.spectrum[k] > maxMagnitude) {
                maxMagnitude = fft.spectrum[k]
                maxBin = k
            }
        }

        return maxBin * fft.binWidth.toDouble()
    }

    private fun detectHarmonics(fundamental: Double, fft: FFTResult): List<VibrationHarmonic> {
        if (fundamental <= 0) return emptyList()

        val harmonics = mutableListOf<VibrationHarmonic>()
        for (multiplier in 2..5) {
            val expectedFreq = fundamental * multiplier
            val bin = (expectedFreq / fft.binWidth).toInt()
            if (bin < fft.bins) {
                val magnitude = fft.spectrum[bin].toDouble()
                harmonics.add(VibrationHarmonic(
                    frequency = expectedFreq,
                    multiplier = multiplier,
                    magnitude = magnitude,
                    relativeMagnitude = if (fft.spectrum[1] > 0) {
                        magnitude / fft.spectrum[1].toDouble()
                    } else 0.0
                ))
            }
        }
        return harmonics
    }

    private fun calculateSpectralFeatures(fft: FFTResult, sampleRate: Float): SpectralFeatures {
        var weightedSum = 0.0
        var magnitudeSum = 0.0

        for (k in 1 until fft.bins) {
            val freq = k * fft.binWidth
            val mag = fft.spectrum[k].toDouble()
            weightedSum += freq * mag
            magnitudeSum += mag
        }

        val centroid = if (magnitudeSum > 0) weightedSum / magnitudeSum else 0.0

        val totalEnergy = fft.spectrum.drop(1).sumOf { it.toDouble() }
        val threshold = totalEnergy * 0.85
        var cumulative = 0.0
        var rolloffBin = fft.bins - 1
        for (k in 1 until fft.bins) {
            cumulative += fft.spectrum[k].toDouble()
            if (cumulative >= threshold) {
                rolloffBin = k
                break
            }
        }
        val rolloff = rolloffBin * fft.binWidth

        var bandwidthSum = 0.0
        for (k in 1 until fft.bins) {
            val freq = k * fft.binWidth
            val mag = fft.spectrum[k].toDouble()
            bandwidthSum += mag * (freq - centroid).pow(2)
        }
        val bandwidth = if (magnitudeSum > 0) sqrt(bandwidthSum / magnitudeSum) else 0.0

        return SpectralFeatures(
            centroid = centroid,
            rolloff = rolloff.toDouble(),
            bandwidth = bandwidth
        )
    }

    private fun calculateDeviation(
        currentRMS: Double,
        currentFreq: Double,
        baseline: VibrationBaseline
    ): DeviationResult {
        val rmsDeviation = if (baseline.rms > 0) {
            ((currentRMS - baseline.rms) / baseline.rms * 100)
        } else 0.0

        val freqDeviation = if (baseline.dominantFrequency > 0) {
            ((currentFreq - baseline.dominantFrequency) / baseline.dominantFrequency * 100)
        } else 0.0

        val isAnomalous = rmsDeviation > 50 || freqDeviation > 20

        return DeviationResult(
            rmsDeviation = rmsDeviation,
            frequencyDeviation = freqDeviation,
            isAnomalous = isAnomalous,
            severity = when {
                rmsDeviation > 100 -> Severity.CRITICAL
                rmsDeviation > 50 -> Severity.HIGH
                rmsDeviation > 25 -> Severity.MEDIUM
                rmsDeviation > 10 -> Severity.LOW
                else -> Severity.INFO
            }
        )
    }

    private fun classifyVibrationLevel(rmsG: Double, category: AssetCategory?): VibrationLevel {
        return when {
            rmsG > 2.0 -> VibrationLevel.DANGEROUS
            rmsG > 1.0 -> VibrationLevel.HIGH
            rmsG > 0.5 -> VibrationLevel.ELEVATED
            rmsG > 0.2 -> VibrationLevel.NORMAL
            else -> VibrationLevel.LOW
        }
    }

    // ─────────────────────────────────────────────────────────────
    // DIAGNOSIS
    // ─────────────────────────────────────────────────────────────

    private fun generateDiagnosis(
        rmsG: Double,
        dominantFreq: Double,
        harmonics: List<VibrationHarmonic>,
        spectral: SpectralFeatures,
        deviation: DeviationResult?,
        vibrationLevel: VibrationLevel,
        category: AssetCategory?
    ): VibrationDiagnosis {
        val causes = mutableListOf<VibrationCause>()

        when {
            vibrationLevel == VibrationLevel.DANGEROUS -> {
                causes.add(VibrationCause("Critical mechanical failure", 0.50, "Dangerously high vibration"))
                causes.add(VibrationCause("Severe imbalance", 0.30, "Extreme RMS level"))
                causes.add(VibrationCause("Structural damage", 0.20, "Immediate inspection required"))
            }
            vibrationLevel == VibrationLevel.HIGH -> {
                causes.add(VibrationCause("Imbalance", 0.35, "High RMS vibration"))
                causes.add(VibrationCause("Bearing wear", 0.30, "High frequency components"))
                causes.add(VibrationCause("Loose mounting", 0.20, "Low frequency oscillation"))
                causes.add(VibrationCause("Normal", 0.15, "Within tolerance"))
            }
            vibrationLevel == VibrationLevel.ELEVATED -> {
                causes.add(VibrationCause("Early wear", 0.30, "Slightly elevated vibration"))
                causes.add(VibrationCause("Minor imbalance", 0.25, "Moderate RMS"))
                causes.add(VibrationCause("Normal variation", 0.30, "Within acceptable range"))
                causes.add(VibrationCause("Other", 0.15, "Need more data"))
            }
            else -> {
                causes.add(VibrationCause("Normal operation", 0.60, "Within normal range"))
                causes.add(VibrationCause("Other", 0.40, "No issues detected"))
            }
        }

        // Add frequency-specific causes
        if (dominantFreq > 0 && harmonics.isNotEmpty()) {
            val harmonicRatio = harmonics.firstOrNull()?.relativeMagnitude ?: 0.0
            if (harmonicRatio > 0.3) {
                causes.add(0, VibrationCause("Mechanical looseness", 0.40,
                    "Strong harmonics at ${dominantFreq.toInt()} Hz"))
            }
        }

        val recommendations = mutableListOf<String>()
        when (vibrationLevel) {
            VibrationLevel.DANGEROUS -> {
                recommendations.add("STOP operation immediately")
                recommendations.add("Inspect for damage")
                recommendations.add("Contact maintenance")
            }
            VibrationLevel.HIGH -> {
                recommendations.add("Schedule inspection")
                recommendations.add("Check mounting bolts")
                recommendations.add("Balance rotating parts")
            }
            VibrationLevel.ELEVATED -> {
                recommendations.add("Monitor closely")
                recommendations.add("Schedule preventive maintenance")
            }
            else -> {
                recommendations.add("Continue normal operation")
                recommendations.add("Record baseline for comparison")
            }
        }

        return VibrationDiagnosis(
            vibrationLevel = vibrationLevel,
            causes = causes.sortedByDescending { it.probability },
            dominantFrequency = dominantFreq,
            harmonicsCount = harmonics.size,
            hasAnomaly = deviation?.isAnomalous ?: false,
            anomalySeverity = deviation?.severity,
            recommendations = recommendations,
            confidence = calculateConfidence(rmsG, dominantFreq, harmonics, deviation)
        )
    }

    private fun calculateConfidence(
        rmsG: Double,
        dominantFreq: Double,
        harmonics: List<VibrationHarmonic>,
        deviation: DeviationResult?
    ): Double {
        var confidence = 0.3

        if (rmsG > 0.1) confidence += 0.2
        if (dominantFreq > 0) confidence += 0.2
        if (harmonics.isNotEmpty()) confidence += 0.15
        if (deviation != null) confidence += 0.15

        return confidence.coerceIn(0.0, 1.0)
    }
}

// ─────────────────────────────────────────────────────────────
// DATA CLASSES
// ─────────────────────────────────────────────────────────────

data class FFTResult(
    val spectrum: FloatArray,
    val bins: Int,
    val binWidth: Float
)

data class SpectralFeatures(
    val centroid: Double,
    val rolloff: Double,
    val bandwidth: Double
)

data class VibrationHarmonic(
    val frequency: Double,
    val multiplier: Int,
    val magnitude: Double,
    val relativeMagnitude: Double
)

enum class VibrationLevel {
    LOW,
    NORMAL,
    ELEVATED,
    HIGH,
    DANGEROUS
}

data class VibrationBaseline(
    val rms: Double,
    val dominantFrequency: Double,
    val timestamp: Instant
)

data class DeviationResult(
    val rmsDeviation: Double,
    val frequencyDeviation: Double,
    val isAnomalous: Boolean,
    val severity: Severity
)

data class VibrationAnalysis(
    val timestamp: Instant,
    val rmsG: Double,
    val peakG: Double,
    val rmsX: Double,
    val rmsY: Double,
    val rmsZ: Double,
    val dominantFrequency: Double,
    val harmonics: List<VibrationHarmonic>,
    val spectralCentroid: Double,
    val spectralRolloff: Double,
    val spectralBandwidth: Double,
    val vibrationLevel: VibrationLevel,
    val diagnosis: VibrationDiagnosis,
    val baselineDeviation: DeviationResult?,
    val assetId: String?,
    val assetName: String?,
    val sampleRate: Float,
    val durationMs: Long
)

data class VibrationDiagnosis(
    val vibrationLevel: VibrationLevel,
    val causes: List<VibrationCause>,
    val dominantFrequency: Double,
    val harmonicsCount: Int,
    val hasAnomaly: Boolean,
    val anomalySeverity: Severity?,
    val recommendations: List<String>,
    val confidence: Double
)

data class VibrationCause(
    val name: String,
    val probability: Double,
    val explanation: String
)

data class VibrationComparison(
    val before: VibrationAnalysis,
    val after: VibrationAnalysis,
    val rmsChange: Double,
    val frequencyChange: Double,
    val improvementDetected: Boolean,
    val summary: String
)
