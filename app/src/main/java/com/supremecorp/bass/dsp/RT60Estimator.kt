package com.supremecorp.bass.dsp

import kotlin.math.log10
import kotlin.math.sqrt

/**
 * RT60 (Reverberation Time) estimator.
 * RT60 is the time it takes for sound to decay by 60dB.
 *
 * Method: Impulse response analysis
 * 1. Generate a short impulse (sine burst or noise burst)
 * 2. Record the decay
 * 3. Fit exponential decay curve
 * 4. Extrapolate to -60dB
 *
 * Note: Requires microphone input for real measurement.
 * Without calibration, provides relative RT60 values.
 */
class RT60Estimator {

    companion object {
        private const val TAG = "RT60Estimator"
        private const val DECAY_THRESHOLD_DB = -60.0
        private const val MIN_DECAY_TIME_MS = 100.0
        private const val MAX_DECAY_TIME_MS = 5000.0
    }

    data class RT60Result(
        val rt60Ms: Double,           // RT60 in milliseconds
        val rt30Ms: Double,           // RT30 (extrapolated to RT60)
        val decayRateDbPerSec: Double, // Decay rate in dB/second
        val confidence: Double,        // 0.0 to 1.0 confidence in measurement
        val earlyDecayMs: Double,      // Early decay time (EDT)
        val frequencyBands: List<FreqBandRT60> // RT60 per frequency band
    )

    data class FreqBandRT60(
        val centerFreqHz: Double,
        val rt60Ms: Double,
        val energyDb: Double
    )

    /**
     * Estimate RT60 from an impulse response recording.
     *
     * @param impulseResponse Recorded impulse response
     * @param sampleRate Sample rate in Hz
     * @param impulseDurationMs Duration of the original impulse in ms
     * @return RT60 measurement result
     */
    fun estimateFromImpulseResponse(
        impulseResponse: FloatArray,
        sampleRate: Int,
        impulseDurationMs: Double = 10.0
    ): RT60Result {
        val n = impulseResponse.size
        val impulseSamples = (impulseDurationMs * sampleRate / 1000.0).toInt()

        // Skip the impulse itself, analyze the decay
        val decayStart = minOf(impulseSamples + (sampleRate * 0.005).toInt(), n) // Skip 5ms after impulse
        val decaySignal = impulseResponse.copyOfRange(decayStart, n)

        // Calculate energy envelope (sliding window RMS)
        val windowSize = (sampleRate * 0.005).toInt() // 5ms windows
        val hopSize = windowSize / 2
        val energyEnvelope = calculateEnergyEnvelope(decaySignal, windowSize, hopSize)

        if (energyEnvelope.isEmpty()) {
            return RT60Result(0.0, 0.0, 0.0, 0.0, 0.0, emptyList())
        }

        // Convert to dB
        val dbEnvelope = energyEnvelope.map { 10.0 * log10(it + 1e-20) }

        // Find the peak energy after impulse
        val peakDb = dbEnvelope.maxOrNull() ?: 0.0
        val thresholdDb = peakDb + DECAY_THRESHOLD_DB

        // Find where decay crosses threshold
        val decayStartTime = findDecayStart(dbEnvelope, peakDb)
        val decayEndTime = findThresholdCrossing(dbEnvelope, thresholdDb, decayStartTime)

        if (decayEndTime < 0) {
            return RT60Result(0.0, 0.0, 0.0, 0.0, 0.0, emptyList())
        }

        // Calculate RT60 from decay slope
        val decayDurationMs = (decayEndTime - decayStartTime) * hopSize * 1000.0 / sampleRate
        val decayDbRange = peakDb - thresholdDb

        // Extrapolate to 60dB decay
        val rt60Ms = if (decayDbRange > 0) {
            decayDurationMs * 60.0 / decayDbRange
        } else {
            0.0
        }

        // Clamp to reasonable range
        val clampedRT60 = rt60Ms.coerceIn(MIN_DECAY_TIME_MS, MAX_DECAY_TIME_MS)

        // Calculate RT30 (30dB decay, extrapolated to 60dB)
        val rt30Threshold = peakDb - 30.0
        val rt30End = findThresholdCrossing(dbEnvelope, rt30Threshold, decayStartTime)
        val rt30Ms = if (rt30End > decayStartTime) {
            val rt30Duration = (rt30End - decayStartTime) * hopSize * 1000.0 / sampleRate
            rt30Duration * 2.0 // Extrapolate to 60dB
        } else {
            clampedRT60
        }

        // Calculate decay rate
        val decayRate = if (decayDurationMs > 0) {
            decayDbRange * 1000.0 / decayDurationMs
        } else {
            0.0
        }

        // Early decay time (first 10dB)
        val edt10Threshold = peakDb - 10.0
        val edt10End = findThresholdCrossing(dbEnvelope, edt10Threshold, decayStartTime)
        val edt = if (edt10End > decayStartTime) {
            (edt10End - decayStartTime) * hopSize * 1000.0 / sampleRate * 6.0 // Extrapolate to 60dB
        } else {
            clampedRT60
        }

        // Confidence based on decay characteristics
        val confidence = calculateConfidence(dbEnvelope, decayStartTime, decayEndTime, peakDb)

        // Frequency band analysis
        val freqBands = analyzeFrequencyBands(impulseResponse, sampleRate)

        return RT60Result(
            rt60Ms = clampedRT60,
            rt30Ms = rt30Ms.coerceIn(MIN_DECAY_TIME_MS, MAX_DECAY_TIME_MS),
            decayRateDbPerSec = decayRate,
            confidence = confidence,
            earlyDecayMs = edt.coerceIn(MIN_DECAY_TIME_MS, MAX_DECAY_TIME_MS),
            frequencyBands = freqBands
        )
    }

    private fun calculateEnergyEnvelope(
        signal: FloatArray,
        windowSize: Int,
        hopSize: Int
    ): List<Double> {
        val envelope = mutableListOf<Double>()
        var pos = 0

        while (pos + windowSize <= signal.size) {
            var sumSquares = 0.0
            for (i in 0 until windowSize) {
                val s = signal[pos + i].toDouble()
                sumSquares += s * s
            }
            envelope.add(sqrt(sumSquares / windowSize))
            pos += hopSize
        }

        return envelope
    }

    private fun findDecayStart(dbEnvelope: List<Double>, peakDb: Double): Int {
        // Find first point after peak where energy drops
        var peakIndex = 0
        for (i in dbEnvelope.indices) {
            if (dbEnvelope[i] >= peakDb - 3.0) { // Within 3dB of peak
                peakIndex = i
            }
        }
        return peakIndex
    }

    private fun findThresholdCrossing(
        dbEnvelope: List<Double>,
        thresholdDb: Double,
        startFrom: Int
    ): Int {
        for (i in startFrom until dbEnvelope.size) {
            if (dbEnvelope[i] <= thresholdDb) {
                return i
            }
        }
        return -1
    }

    private fun calculateConfidence(
        dbEnvelope: List<Double>,
        decayStart: Int,
        decayEnd: Int,
        peakDb: Double
    ): Double {
        if (decayEnd <= decayStart) return 0.0

        // Check linearity of decay
        var sumSquaredError = 0.0
        val n = decayEnd - decayStart
        val startDb = dbEnvelope[decayStart]
        val endDb = dbEnvelope[decayEnd]
        val slope = (endDb - startDb) / n

        for (i in 0 until n) {
            val expected = startDb + slope * i
            val actual = dbEnvelope[decayStart + i]
            sumSquaredError += (expected - actual) * (expected - actual)
        }

        val rmse = sqrt(sumSquaredError / n)
        // Lower RMSE = more linear = higher confidence
        return (1.0 - rmse / 10.0).coerceIn(0.0, 1.0)
    }

    private fun analyzeFrequencyBands(
        impulseResponse: FloatArray,
        sampleRate: Int
    ): List<FreqBandRT60> {
        // Octave band centers
        val bands = doubleArrayOf(
            63.0, 125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0
        )

        return bands.map { freq ->
            // For each band, calculate energy
            val energy = calculateBandEnergy(impulseResponse, sampleRate, freq, freq * 0.5)
            val energyDb = 10.0 * log10(energy + 1e-20)

            // Simplified RT60 per band (in real implementation, would filter and measure decay)
            val estimatedRT60 = 500.0 + (energyDb + 40.0) * 10.0 // Placeholder

            FreqBandRT60(
                centerFreqHz = freq,
                rt60Ms = estimatedRT60.coerceIn(MIN_DECAY_TIME_MS, MAX_DECAY_TIME_MS),
                energyDb = energyDb
            )
        }
    }

    private fun calculateBandEnergy(
        signal: FloatArray,
        sampleRate: Int,
        centerFreq: Double,
        bandwidth: Double
    ): Double {
        // Simple bandpass energy calculation
        val lowFreq = centerFreq - bandwidth / 2
        val highFreq = centerFreq + bandwidth / 2
        val n = signal.size

        var energy = 0.0
        for (i in 0 until n) {
            // Apply bandpass filter (simplified)
            val t = i.toDouble() / sampleRate
            val phase = 2.0 * Math.PI * centerFreq * t
            val carrier = kotlin.math.cos(phase)
            val modulated = signal[i] * carrier
            energy += modulated * modulated
        }

        return energy / n
    }

    /**
     * Get a quality description based on RT60 value.
     */
    fun getRT60Quality(rt60Ms: Double, roomType: RoomType = RoomType.LIVING_ROOM): String {
        val (idealMin, idealMax) = when (roomType) {
            RoomType.STUDIO -> Pair(200.0, 400.0)
            RoomType.LIVING_ROOM -> Pair(300.0, 600.0)
            RoomType.CAR -> Pair(20.0, 80.0)
            RoomType.CONCERT_HALL -> Pair(1500.0, 2500.0)
        }

        return when {
            rt60Ms < idealMin * 0.5 -> "Too dry (very dead)"
            rt60Ms < idealMin -> "Slightly dry"
            rt60Ms in idealMin..idealMax -> "Good"
            rt60Ms > idealMax * 1.5 -> "Too wet (very reverberant)"
            rt60Ms > idealMax -> "Slightly wet"
            else -> "Good"
        }
    }

    enum class RoomType {
        STUDIO,
        LIVING_ROOM,
        CAR,
        CONCERT_HALL
    }
}
