package com.supremecorp.bass.dsp

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * THD (Total Harmonic Distortion) analyzer.
 * Measures harmonic distortion in audio signals.
 *
 * THD is calculated as:
 * THD = sqrt(V2² + V3² + V4² + ... + Vn²) / V1
 *
 * Where V1 is the fundamental amplitude and V2..Vn are harmonics.
 *
 * THD+N includes noise floor in the measurement.
 */
class THDAnalyzer {

    companion object {
        private const val MAX_HARMONICS = 10
        private const val MIN_AMPLITUDE = 1e-6
    }

    data class THDResult(
        val thdPercent: Double,      // THD as percentage
        val thdDB: Double,           // THD in dB (usually negative)
        val fundamentalHz: Double,   // Detected fundamental frequency
        val fundamentalDb: Double,   // Fundamental level in dB
        val harmonicLevels: List<Pair<Int, Double>>, // (harmonic number, level in dB)
        val thdNPercent: Double      // THD+N (including noise)
    )

    /**
     * Analyze THD of a recorded signal.
     *
     * @param samples Audio samples containing a sine wave
     * @param expectedFrequency Expected fundamental frequency (Hz)
     * @param sampleRate Sample rate (Hz)
     * @return THD measurement result
     */
    fun analyze(
        samples: FloatArray,
        expectedFrequency: Double,
        sampleRate: Int
    ): THDResult {
        val n = samples.size

        // 1. Find the actual fundamental frequency using zero-crossing detection
        val actualFrequency = detectFundamental(samples, sampleRate)

        // 2. Compute FFT
        var fftSize = 256
        while (fftSize < n) fftSize *= 2
        val fft = FFT(fftSize)
        val real = DoubleArray(fftSize)
        val imag = DoubleArray(fftSize)
        for (i in samples.indices) {
            real[i] = samples[i].toDouble()
        }
        fft.hanningWindow(real)
        fft.forward(real, imag)

        // 3. Find fundamental bin
        val fundamentalBin = (actualFrequency * fft.getSize() / sampleRate).toInt()
            .coerceIn(1, fft.getSize() / 2 - 1)

        // 4. Extract harmonic levels
        val fundamentalAmplitude = extractBinMagnitude(real, imag, fundamentalBin)
        val fundamentalDb = 20.0 * log10(amplitude = fundamentalAmplitude)

        val harmonicLevels = mutableListOf<Pair<Int, Double>>()
        var harmonicSumSquares = 0.0

        for (h in 2..MAX_HARMONICS) {
            val harmonicBin = fundamentalBin * h
            if (harmonicBin >= fft.getSize() / 2) break

            val harmonicAmplitude = extractBinMagnitude(real, imag, harmonicBin)
            val harmonicDb = 20.0 * log10(amplitude = harmonicAmplitude)

            harmonicLevels.add(Pair(h, harmonicDb))
            harmonicSumSquares += harmonicAmplitude * harmonicAmplitude
        }

        // 5. Calculate THD
        val thd = if (fundamentalAmplitude > MIN_AMPLITUDE) {
            sqrt(harmonicSumSquares) / fundamentalAmplitude
        } else {
            0.0
        }

        val thdPercent = thd * 100.0
        val thdDB = if (thd > MIN_AMPLITUDE) {
            20.0 * log10(amplitude = thd)
        } else {
            -200.0
        }

        // 6. Calculate THD+N (include noise floor)
        // Total power minus fundamental and harmonics
        var totalPower = 0.0
        for (i in 1 until fft.getSize() / 2) {
            val amp = extractBinMagnitude(real, imag, i)
            totalPower += amp * amp
        }

        val noisePower = totalPower - fundamentalAmplitude * fundamentalAmplitude - harmonicSumSquares
        val thdN = if (fundamentalAmplitude > MIN_AMPLITUDE && noisePower > 0) {
            sqrt(noisePower) / fundamentalAmplitude
        } else {
            0.0
        }

        return THDResult(
            thdPercent = thdPercent,
            thdDB = thdDB,
            fundamentalHz = actualFrequency,
            fundamentalDb = fundamentalDb,
            harmonicLevels = harmonicLevels,
            thdNPercent = thdN * 100.0
        )
    }

    /**
     * Detect fundamental frequency using zero-crossing method.
     */
    private fun detectFundamental(samples: FloatArray, sampleRate: Int): Double {
        var crossings = 0
        var lastSign = samples[0] >= 0

        for (i in 1 until samples.size) {
            val currentSign = samples[i] >= 0
            if (currentSign != lastSign) {
                crossings++
                lastSign = currentSign
            }
        }

        // Frequency = crossings / (2 * duration)
        val duration = samples.size.toDouble() / sampleRate
        return crossings / (2.0 * duration)
    }

    private fun extractBinMagnitude(real: DoubleArray, imag: DoubleArray, bin: Int): Double {
        val re = real[bin]
        val im = imag[bin]
        return sqrt(re * re + im * im)
    }

    private fun log10(amplitude: Double): Double {
        return kotlin.math.ln(amplitude) / kotlin.math.ln(10.0)
    }

    /**
     * Check if THD is acceptable for speaker testing.
     * Typically < 1% is good, < 0.1% is excellent.
     */
    fun isTHDAcceptable(thdPercent: Double, threshold: Double = 1.0): Boolean {
        return thdPercent < threshold
    }

    /**
     * Get a quality rating based on THD percentage.
     */
    fun getTHDQuality(thdPercent: Double): String {
        return when {
            thdPercent < 0.1 -> "Excellent"
            thdPercent < 0.5 -> "Very Good"
            thdPercent < 1.0 -> "Good"
            thdPercent < 3.0 -> "Fair"
            thdPercent < 5.0 -> "Poor"
            else -> "Very Poor"
        }
    }
}
