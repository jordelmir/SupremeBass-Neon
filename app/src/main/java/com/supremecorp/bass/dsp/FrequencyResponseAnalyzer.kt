package com.supremecorp.bass.dsp

import android.util.Log
import com.supremecorp.bass.audio.input.AudioInputProcessor
import com.supremecorp.bass.core.logging.AppLogger
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Measures frequency response using microphone input.
 *
 * Method:
 * 1. Generate a sine sweep from 20Hz to 20kHz
 * 2. Play sweep through speaker while recording with microphone
 * 3. Compute FFT of recorded signal
 * 4. Extract magnitude at each sweep frequency
 * 5. Compare to known input amplitude for response curve
 *
 * Note: Without calibration, results are relative (not absolute SPL).
 */
class FrequencyResponseAnalyzer {

    companion object {
        private const val TAG = "FreqResponseAnalyzer"
        private const val FFT_SIZE = 8192
        private const val MIN_AMPLITUDE = 1e-6
    }

    private val fft = FFT(FFT_SIZE)
    private var sampleRate: Int = 44100

    data class FrequencyPoint(
        val frequencyHz: Double,
        val magnitudeDb: Double,
        val phaseDegrees: Double
    )

    data class FrequencyResponse(
        val points: List<FrequencyPoint>,
        val minHz: Double,
        val maxHz: Double,
        val averageMagnitudeDb: Double,
        val flatnessScore: Double // 0 = perfectly flat, higher = more variation
    )

    /**
     * Analyze recorded audio from a sine sweep.
     *
     * @param recordedSamples Microphone recording of the sweep
     * @param sweepFrequencies List of frequencies that were swept (in order)
     * @param samplesPerFrequency Number of samples per frequency step
     * @return Frequency response measurement
     */
    fun analyzeSweepRecording(
        recordedSamples: FloatArray,
        sweepFrequencies: List<Double>,
        samplesPerFrequency: Int
    ): FrequencyResponse {
        val points = mutableListOf<FrequencyPoint>()

        for ((index, freq) in sweepFrequencies.withIndex()) {
            val startSample = index * samplesPerFrequency
            val endSample = minOf(startSample + samplesPerFrequency, recordedSamples.size)

            if (startSample >= recordedSamples.size) break

            val segment = recordedSamples.copyOfRange(startSample, endSample)

            // Apply window
            val windowed = DoubleArray(segment.size)
            for (i in segment.indices) {
                windowed[i] = segment[i].toDouble()
            }
            fft.hanningWindow(windowed)

            // Pad to FFT size if needed
            val real = DoubleArray(FFT_SIZE)
            val imag = DoubleArray(FFT_SIZE)
            for (i in windowed.indices) {
                real[i] = windowed[i]
            }

            // Compute FFT
            fft.forward(real, imag)

            // Find the bin closest to our target frequency
            val targetBin = (freq * FFT_SIZE / sampleRate).toInt()
                .coerceIn(0, FFT_SIZE / 2 - 1)

            // Get magnitude at target frequency
            val re = real[targetBin]
            val im = imag[targetBin]
            val magnitude = sqrt(re * re + im * im)
            val magnitudeDb = if (magnitude > MIN_AMPLITUDE) {
                20.0 * log10(magnitude)
            } else {
                -200.0
            }

            // Get phase
            val phaseDegrees = Math.toDegrees(kotlin.math.atan2(im, re))

            points.add(FrequencyPoint(freq, magnitudeDb, phaseDegrees))
        }

        // Calculate statistics
        val validPoints = points.filter { it.magnitudeDb > -150.0 }
        val avgMagnitude = if (validPoints.isNotEmpty()) {
            validPoints.map { it.magnitudeDb }.average()
        } else {
            0.0
        }

        // Calculate flatness (standard deviation of magnitudes)
        val flatness = if (validPoints.size > 1) {
            val mean = avgMagnitude
            val variance = validPoints.map { (it.magnitudeDb - mean) * (it.magnitudeDb - mean) }.average()
            sqrt(variance)
        } else {
            0.0
        }

        return FrequencyResponse(
            points = points,
            minHz = sweepFrequencies.firstOrNull() ?: 20.0,
            maxHz = sweepFrequencies.lastOrNull() ?: 20000.0,
            averageMagnitudeDb = avgMagnitude,
            flatnessScore = flatness
        )
    }

    /**
     * Generate logarithmic sweep frequencies.
     */
    fun generateSweepFrequencies(
        startHz: Double = 20.0,
        endHz: Double = 20000.0,
        steps: Int = 30
    ): List<Double> {
        val frequencies = mutableListOf<Double>()
        for (i in 0 until steps) {
            val t = i.toDouble() / (steps - 1)
            val freq = startHz * (endHz / startHz).pow(t)
            frequencies.add(freq)
        }
        return frequencies
    }

    /**
     * Normalize response to 0dB at average level.
     */
    fun normalizeResponse(response: FrequencyResponse): FrequencyResponse {
        val offset = response.averageMagnitudeDb
        val normalizedPoints = response.points.map {
            it.copy(magnitudeDb = it.magnitudeDb - offset)
        }
        return response.copy(
            points = normalizedPoints,
            averageMagnitudeDb = 0.0
        )
    }

    /**
     * Find peak frequency in response.
     */
    fun findPeakFrequency(response: FrequencyResponse): FrequencyPoint? {
        return response.points.maxByOrNull { it.magnitudeDb }
    }

    /**
     * Find -3dB bandwidth (frequency range where response is within 3dB of peak).
     */
    fun find3dBBandwidth(response: FrequencyResponse): Pair<Double, Double>? {
        val peak = findPeakFrequency(response) ?: return null
        val threshold = peak.magnitudeDb - 3.0

        var lowHz = peak.frequencyHz
        var highHz = peak.frequencyHz

        for (point in response.points) {
            if (point.magnitudeDb >= threshold) {
                if (point.frequencyHz < lowHz) lowHz = point.frequencyHz
                if (point.frequencyHz > highHz) highHz = point.frequencyHz
            }
        }

        return Pair(lowHz, highHz)
    }

    private fun Double.pow(exp: Double): Double {
        return kotlin.math.exp(exp * kotlin.math.ln(this))
    }
}
