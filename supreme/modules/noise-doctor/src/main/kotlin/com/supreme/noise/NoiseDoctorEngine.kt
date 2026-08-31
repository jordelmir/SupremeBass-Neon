package com.supreme.noise

import com.supreme.core.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.time.Instant
import kotlin.math.*

/**
 * Supreme Noise Doctor — "What is this sound?"
 *
 * Analyzes audio recordings to diagnose mechanical issues.
 * Reuses SupremeBass DSP engine (FFT, harmonics, spectral analysis).
 *
 * Input: 15-second audio recording
 * Output: frequency analysis, harmonic patterns, potential causes
 *
 * Works with:
 * - fans, pumps, washing machines, cars, motors
 * - AC compressors, refrigerators, power tools
 * - PC fans, grinders, drills
 */

class NoiseDoctorEngine {

    private val _analyses = MutableSharedFlow<NoiseAnalysis>(replay = 1)
    val analyses: SharedFlow<NoiseAnalysis> = _analyses

    /**
     * Analyze an audio recording.
     */
    suspend fun analyze(
        audioData: FloatArray,
        sampleRate: Int = 44100,
        userDescription: String? = null,
        assetCategory: AssetCategory? = null,
        assetSubcategory: String? = null
    ): NoiseAnalysis {
        // 1. FFT Analysis
        val fftResult = performFFT(audioData, sampleRate)

        // 2. Detect dominant frequencies
        val dominantFreqs = detectDominantFrequencies(fftResult)

        // 3. Detect harmonics
        val harmonics = detectHarmonics(dominantFreqs)

        // 4. Calculate spectral features
        val spectralFeatures = calculateSpectralFeatures(fftResult, sampleRate)

        // 5. Detect periodicity
        val periodicity = detectPeriodicity(audioData, sampleRate)

        // 6. Classify noise type
        val noiseType = classifyNoise(dominantFreqs, harmonics, spectralFeatures, periodicity)

        // 7. Generate diagnosis
        val diagnosis = generateDiagnosis(
            noiseType, dominantFreqs, harmonics, spectralFeatures,
            periodicity, assetCategory, assetSubcategory, userDescription
        )

        val analysis = NoiseAnalysis(
            timestamp = Instant.now(),
            dominantFrequency = dominantFreqs.firstOrNull() ?: 0.0,
            harmonics = harmonics,
            spectralCentroid = spectralFeatures.centroid,
            spectralRolloff = spectralFeatures.rolloff,
            spectralBandwidth = spectralFeatures.bandwidth,
            rmsLevel = spectralFeatures.rms,
            peakLevel = spectralFeatures.peak,
            zeroCrossingRate = spectralFeatures.zeroCrossingRate,
            periodicityScore = periodicity.score,
            periodicityFrequency = periodicity.frequency,
            noiseType = noiseType,
            diagnosis = diagnosis,
            sampleRate = sampleRate,
            durationMs = audioData.size.toLong() / sampleRate * 1000
        )

        _analyses.tryEmit(analysis)
        return analysis
    }

    /**
     * Compare two recordings (before/after repair).
     */
    suspend fun compare(before: NoiseAnalysis, after: NoiseAnalysis): NoiseComparison {
        val freqChange = if (before.dominantFrequency > 0) {
            ((after.dominantFrequency - before.dominantFrequency) / before.dominantFrequency * 100)
        } else 0.0

        val rmsChange = if (before.rmsLevel > 0) {
            ((after.rmsLevel - before.rmsLevel) / before.rmsLevel * 100)
        } else 0.0

        val harmonicChange = if (before.harmonics.isNotEmpty()) {
            after.harmonics.size - before.harmonics.size
        } else 0

        val improvement = rmsChange < -10 || harmonicChange < 0

        return NoiseComparison(
            before = before,
            after = after,
            dominantFrequencyChange = freqChange,
            rmsChange = rmsChange,
            harmonicChange = harmonicChange,
            improvementDetected = improvement,
            summary = if (improvement) {
                "Sound improved after repair"
            } else {
                "No significant improvement detected"
            }
        )
    }

    // ─────────────────────────────────────────────────────────────
    // FFT AND SPECTRAL ANALYSIS
    // ─────────────────────────────────────────────────────────────

    private fun performFFT(audioData: FloatArray, sampleRate: Int): FFTResult {
        // Simple DFT implementation (in production use FFTW or KissFFT)
        val n = audioData.size
        val spectrum = FloatArray(n / 2)

        for (k in 0 until n / 2) {
            var real = 0.0
            var imag = 0.0
            for (j in 0 until n) {
                val angle = 2.0 * PI * k * j / n
                real += audioData[j] * cos(angle).toFloat()
                imag -= audioData[j] * sin(angle).toFloat()
            }
            spectrum[k] = sqrt(real * real + imag * imag).toFloat() / n
        }

        return FFTResult(
            spectrum = spectrum,
            bins = n / 2,
            binWidth = sampleRate.toFloat() / n
        )
    }

    private fun detectDominantFrequencies(fft: FFTResult, topN: Int = 5): List<Double> {
        val frequencies = mutableListOf<Pair<Double, Float>>()

        // Skip DC component (bin 0) and very low frequencies
        for (k in 2 until fft.bins) {
            val freq = k * fft.binWidth.toDouble()
            if (freq > 10 && freq < 10000) { // 10 Hz to 10 kHz
                frequencies.add(Pair(freq, fft.spectrum[k]))
            }
        }

        return frequencies.sortedByDescending { it.second }
            .take(topN)
            .map { it.first }
    }

    private fun detectHarmonics(dominantFreqs: List<Double>): List<Harmonic> {
        if (dominantFreqs.isEmpty()) return emptyList()

        val fundamental = dominantFreqs.first()
        val harmonics = mutableListOf<Harmonic>()

        // Check for harmonics at integer multiples
        for (multiplier in 2..8) {
            val expectedFreq = fundamental * multiplier
            val matched = dominantFreqs.any { abs(it - expectedFreq) < 5.0 }
            harmonics.add(Harmonic(
                frequency = expectedFreq,
                multiplier = multiplier,
                detected = matched
            ))
        }

        return harmonics
    }

    private fun calculateSpectralFeatures(fft: FFTResult, sampleRate: Int): SpectralFeatures {
        val spectrum = fft.spectrum
        val binWidth = fft.binWidth

        // Spectral centroid
        var weightedSum = 0.0
        var magnitudeSum = 0.0
        for (k in 1 until fft.bins) {
            val freq = k * binWidth
            val mag = spectrum[k].toDouble()
            weightedSum += freq * mag
            magnitudeSum += mag
        }
        val centroid = if (magnitudeSum > 0) weightedSum / magnitudeSum else 0.0

        // Spectral rolloff (85%)
        val totalEnergy = spectrum.drop(1).sumOf { it.toDouble() }
        val threshold = totalEnergy * 0.85
        var cumulative = 0.0
        var rolloffBin = fft.bins - 1
        for (k in 1 until fft.bins) {
            cumulative += spectrum[k].toDouble()
            if (cumulative >= threshold) {
                rolloffBin = k
                break
            }
        }
        val rolloff = rolloffBin * binWidth

        // Spectral bandwidth
        var bandwidthSum = 0.0
        for (k in 1 until fft.bins) {
            val freq = k * binWidth
            val mag = spectrum[k].toDouble()
            bandwidthSum += mag * (freq - centroid).pow(2)
        }
        val bandwidth = if (magnitudeSum > 0) sqrt(bandwidthSum / magnitudeSum) else 0.0

        // RMS
        val rms = sqrt(spectrum.drop(1).sumOf { it.toDouble().pow(2) } / fft.bins)

        // Peak
        val peak = spectrum.drop(1).maxOrNull()?.toDouble() ?: 0.0

        // Zero crossing rate (from time domain approximation)
        val zcr = 0.0 // Would need time domain data

        return SpectralFeatures(
            centroid = centroid,
            rolloff = rolloff,
            bandwidth = bandwidth,
            rms = rms,
            peak = peak,
            zeroCrossingRate = zcr
        )
    }

    private fun detectPeriodicity(audioData: FloatArray, sampleRate: Int): PeriodicityResult {
        // Simple autocorrelation-based periodicity detection
        val windowSize = minOf(audioData.size, sampleRate) // 1 second window
        val correlation = FloatArray(windowSize)

        for (lag in 0 until windowSize) {
            var sum = 0.0f
            var count = 0
            for (i in 0 until audioData.size - lag) {
                sum += audioData[i] * audioData[i + lag]
                count++
            }
            correlation[lag] = if (count > 0) sum / count else 0.0f
        }

        // Find first peak after zero crossing
        var firstPeakLag = 0
        var foundZero = false
        for (lag in 1 until windowSize) {
            if (!foundZero && correlation[lag] < correlation[lag - 1] * 0.5) {
                foundZero = true
            }
            if (foundZero && correlation[lag] > correlation[lag - 1]) {
                firstPeakLag = lag
                break
            }
        }

        val frequency = if (firstPeakLag > 0) {
            sampleRate.toDouble() / firstPeakLag
        } else 0.0

        val score = if (firstPeakLag > 0 && correlation[firstPeakLag] > 0.3) {
            correlation[firstPeakLag].toDouble()
        } else 0.0

        return PeriodicityResult(
            score = score,
            frequency = frequency,
            lag = firstPeakLag
        )
    }

    // ─────────────────────────────────────────────────────────────
    // CLASSIFICATION
    // ─────────────────────────────────────────────────────────────

    private fun classifyNoise(
        dominantFreqs: List<Double>,
        harmonics: List<Harmonic>,
        spectral: SpectralFeatures,
        periodicity: PeriodicityResult
    ): NoiseType {
        val hasStrongPeriodicity = periodicity.score > 0.5
        val harmonicCount = harmonics.count { it.detected }
        val hasHighFrequency = dominantFreqs.any { it > 1000 }
        val hasLowFrequency = dominantFreqs.any { it < 100 }

        return when {
            hasStrongPeriodicity && harmonicCount >= 2 -> NoiseType.ROTATIONAL
            hasStrongPeriodicity && hasLowFrequency -> NoiseType.VIBRATIONAL
            hasHighFrequency && spectral.bandwidth > 500 -> NoiseType.BROADBAND
            hasHighFrequency && !hasStrongPeriodicity -> NoiseType.HISSING
            hasLowFrequency && hasStrongPeriodicity -> NoiseType.HUMMING
            spectral.rms > 0.5 -> NoiseType.LOUD
            else -> NoiseType.UNKNOWN
        }
    }

    // ─────────────────────────────────────────────────────────────
    // DIAGNOSIS
    // ─────────────────────────────────────────────────────────────

    private fun generateDiagnosis(
        noiseType: NoiseType,
        dominantFreqs: List<Double>,
        harmonics: List<Harmonic>,
        spectral: SpectralFeatures,
        periodicity: PeriodicityResult,
        category: AssetCategory?,
        subcategory: String?,
        userDescription: String?
    ): NoiseDiagnosis {
        val causes = mutableListOf<NoiseCause>()

        when (noiseType) {
            NoiseType.ROTATIONAL -> {
                causes.add(NoiseCause("Bearing wear", 0.35, "Harmonic pattern suggests bearing degradation"))
                causes.add(NoiseCause("Imbalance", 0.25, "Rotational frequency with harmonics"))
                causes.add(NoiseCause("Misalignment", 0.20, "Multiple harmonics detected"))
                causes.add(NoiseCause("Normal wear", 0.15, "Within expected range"))
                causes.add(NoiseCause("Other", 0.05, "Insufficient data"))
            }
            NoiseType.VIBRATIONAL -> {
                causes.add(NoiseCause("Loose component", 0.30, "Low frequency vibration"))
                causes.add(NoiseCause("Resonance", 0.25, "Matching natural frequency"))
                causes.add(NoiseCause("Mounting issue", 0.25, "Structural vibration"))
                causes.add(NoiseCause("Other", 0.20, "Need more data"))
            }
            NoiseType.BROADBAND -> {
                causes.add(NoiseCause("Air flow turbulence", 0.30, "Broadband noise"))
                causes.add(NoiseCause("Fluid flow", 0.25, "White noise pattern"))
                causes.add(NoiseCause("Friction", 0.20, "Surface contact noise"))
                causes.add(NoiseCause("Other", 0.25, "Need more info"))
            }
            NoiseType.HISSING -> {
                causes.add(NoiseCause("Air leak", 0.35, "High frequency hiss"))
                causes.add(NoiseCause("Pressure release", 0.25, "Controlled leak"))
                causes.add(NoiseCause("Other", 0.40, "Need more info"))
            }
            NoiseType.HUMMING -> {
                causes.add(NoiseCause("Electrical hum", 0.35, "50/60 Hz pattern"))
                causes.add(NoiseCause("Motor issue", 0.25, "Low frequency hum"))
                causes.add(NoiseCause("Transformer", 0.20, "Magnetic vibration"))
                causes.add(NoiseCause("Other", 0.20, "Need more info"))
            }
            NoiseType.LOUD -> {
                causes.add(NoiseCause("Mechanical failure", 0.30, "High amplitude"))
                causes.add(NoiseCause("Loose part", 0.25, "Impact noise"))
                causes.add(NoiseCause("Other", 0.45, "Need more info"))
            }
            NoiseType.UNKNOWN -> {
                causes.add(NoiseCause("Insufficient data", 0.50, "Need more information"))
                causes.add(NoiseCause("Other", 0.50, "Cannot determine"))
            }
        }

        // Generate recommendations
        val recommendations = mutableListOf<String>()
        if (noiseType == NoiseType.ROTATIONAL) {
            recommendations.add("Record at different RPM settings")
            recommendations.add("Check mounting bolts tightness")
            recommendations.add("Inspect for visible damage")
        }
        recommendations.add("Compare with baseline recording")
        recommendations.add("Check temperature during operation")

        return NoiseDiagnosis(
            noiseType = noiseType,
            causes = causes.sortedByDescending { it.probability },
            dominantFrequency = dominantFreqs.firstOrNull(),
            harmonicsDetected = harmonics.count { it.detected },
            periodicityDetected = periodicity.score > 0.3,
            recommendations = recommendations,
            confidence = calculateConfidence(noiseType, dominantFreqs, harmonics, periodicity)
        )
    }

    private fun calculateConfidence(
        noiseType: NoiseType,
        dominantFreqs: List<Double>,
        harmonics: List<Harmonic>,
        periodicity: PeriodicityResult
    ): Double {
        var confidence = 0.3 // Base confidence

        if (dominantFreqs.isNotEmpty()) confidence += 0.2
        if (harmonics.any { it.detected }) confidence += 0.2
        if (periodicity.score > 0.3) confidence += 0.2
        if (noiseType != NoiseType.UNKNOWN) confidence += 0.1

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

data class Harmonic(
    val frequency: Double,
    val multiplier: Int,
    val detected: Boolean
)

data class SpectralFeatures(
    val centroid: Double,
    val rolloff: Double,
    val bandwidth: Double,
    val rms: Double,
    val peak: Double,
    val zeroCrossingRate: Double
)

data class PeriodicityResult(
    val score: Double,
    val frequency: Double,
    val lag: Int
)

enum class NoiseType {
    ROTATIONAL,
    VIBRATIONAL,
    BROADBAND,
    HISSING,
    HUMMING,
    LOUD,
    UNKNOWN
}

data class NoiseAnalysis(
    val timestamp: Instant,
    val dominantFrequency: Double,
    val harmonics: List<Harmonic>,
    val spectralCentroid: Double,
    val spectralRolloff: Double,
    val spectralBandwidth: Double,
    val rmsLevel: Double,
    val peakLevel: Double,
    val zeroCrossingRate: Double,
    val periodicityScore: Double,
    val periodicityFrequency: Double,
    val noiseType: NoiseType,
    val diagnosis: NoiseDiagnosis,
    val sampleRate: Int,
    val durationMs: Long
)

data class NoiseDiagnosis(
    val noiseType: NoiseType,
    val causes: List<NoiseCause>,
    val dominantFrequency: Double?,
    val harmonicsDetected: Int,
    val periodicityDetected: Boolean,
    val recommendations: List<String>,
    val confidence: Double
)

data class NoiseCause(
    val name: String,
    val probability: Double,
    val explanation: String
)

data class NoiseComparison(
    val before: NoiseAnalysis,
    val after: NoiseAnalysis,
    val dominantFrequencyChange: Double,
    val rmsChange: Double,
    val harmonicChange: Int,
    val improvementDetected: Boolean,
    val summary: String
)
