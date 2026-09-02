package com.supreme.android.dsp

import kotlin.math.abs
import kotlin.math.sqrt

class Limiter(
    private val ceiling: Float = 1.0f
) {
    var clippedSamples: Int = 0
        private set
    var peak: Float = 0.0f
        private set
    var rms: Float = 0.0f
        private set

    fun reset() {
        clippedSamples = 0
        peak = 0.0f
        rms = 0.0f
    }

    fun process(buffer: FloatArray, frameCount: Int): FloatArray {
        var sumSquares = 0.0f
        var maxPeak = 0.0f
        var clips = 0

        for (i in 0 until frameCount) {
            val sample = buffer[i]
            val absSample = abs(sample)

            if (absSample > ceiling) {
                buffer[i] = if (sample > 0) ceiling else -ceiling
                clips++
            }

            val absAfter = abs(buffer[i])
            if (absAfter > maxPeak) maxPeak = absAfter
            sumSquares += buffer[i] * buffer[i]
        }

        clippedSamples += clips
        peak = maxPeak
        rms = sqrt(sumSquares / frameCount)

        return buffer
    }

    fun getCrestFactor(): Float {
        return if (rms > 0.0f) peak / rms else 0.0f
    }
}
