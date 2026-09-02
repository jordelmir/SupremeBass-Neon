package com.supreme.android.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan
import kotlin.math.sqrt

/**
 * Biquad filter — implements all standard filter types.
 * Based on Audio EQ Cookbook (Robert Bristow-Johnson).
 *
 * Transfer function: H(z) = (b0 + b1*z^-1 + b2*z^-2) / (a0 + a1*z^-1 + a2*z^-2)
 */
class BiquadFilter {

    enum class Type {
        LOW_PASS,
        HIGH_PASS,
        BAND_PASS,
        NOTCH,
        ALL_PASS,
        PEAKING,
        LOW_SHELF,
        HIGH_SHELF
    }

    var type: Type = Type.PEAKING
        private set
    var frequencyHz: Double = 1000.0
        private set
    var Q: Double = 0.707
        private set
    var gainDb: Double = 0.0
        private set
    var sampleRate: Int = 48_000
        private set

    // Filter coefficients
    private var b0 = 1.0
    private var b1 = 0.0
    private var b2 = 0.0
    private var a0 = 1.0
    private var a1 = 0.0
    private var a2 = 0.0

    // Per-channel state (Direct Form II Transposed)
    private data class ChannelState(var z1: Double = 0.0, var z2: Double = 0.0)
    private val channelStates = mutableMapOf<Int, ChannelState>()

    private fun getState(channel: Int): ChannelState {
        return channelStates.getOrPut(channel) { ChannelState() }
    }

    fun configure(
        type: Type,
        sampleRate: Int,
        frequencyHz: Double,
        Q: Double = 0.707,
        gainDb: Double = 0.0
    ) {
        this.type = type
        this.sampleRate = sampleRate
        this.frequencyHz = frequencyHz
        this.Q = Q
        this.gainDb = gainDb
        computeCoefficients()
    }

    fun setGainDb(gainDb: Double) {
        if (this.gainDb != gainDb) {
            this.gainDb = gainDb
            computeCoefficients()
        }
    }

    fun setFrequency(frequencyHz: Double) {
        if (this.frequencyHz != frequencyHz) {
            this.frequencyHz = frequencyHz
            computeCoefficients()
        }
    }

    fun reset() {
        channelStates.clear()
    }

    /**
     * Process a single sample through the filter (channel 0 by default).
     */
    fun process(input: Double, channel: Int = 0): Double {
        val state = getState(channel)
        val output = b0 * input + state.z1
        state.z1 = b1 * input - a1 * output + state.z2
        state.z2 = b2 * input - a2 * output
        return output
    }

    /**
     * Process a buffer of samples (mono, in-place).
     */
    fun process(buffer: FloatArray, frameCount: Int, channel: Int = 0) {
        for (i in 0 until frameCount) {
            buffer[i] = process(buffer[i].toDouble(), channel).toFloat()
        }
    }

    /**
     * Process interleaved stereo buffer (in-place).
     * Uses independent state for L/R channels — no cross-channel contamination.
     */
    fun processStereo(buffer: FloatArray, frameCount: Int) {
        for (i in 0 until frameCount) {
            val left = process(buffer[i * 2].toDouble(), 0).toFloat()
            val right = process(buffer[i * 2 + 1].toDouble(), 1).toFloat()
            buffer[i * 2] = left
            buffer[i * 2 + 1] = right
        }
    }

    /**
     * Process multi-channel buffer (in-place).
     */
    fun processMultiChannel(buffer: FloatArray, frameCount: Int, channelCount: Int) {
        for (i in 0 until frameCount) {
            for (ch in 0 until channelCount) {
                val idx = i * channelCount + ch
                buffer[idx] = process(buffer[idx].toDouble(), ch).toFloat()
            }
        }
    }

    /**
     * Compute filter coefficients based on type and parameters.
     * Uses Audio EQ Cookbook formulas (Robert Bristow-Johnson).
     */
    private fun computeCoefficients() {
        val A = if (gainDb != 0.0) pow10(gainDb / 40.0) else 1.0
        val w0 = 2.0 * PI * frequencyHz / sampleRate
        val cosW0 = cos(w0)
        val sinW0 = sin(w0)
        val alpha = sinW0 / (2.0 * Q)

        when (type) {
            Type.LOW_PASS -> {
                val norm = 1.0 / (1.0 + alpha)
                b0 = (1.0 - cosW0) / 2.0 * norm
                b1 = (1.0 - cosW0) * norm
                b2 = b0
                a0 = 1.0
                a1 = -2.0 * cosW0 * norm
                a2 = (1.0 - alpha) * norm
            }

            Type.HIGH_PASS -> {
                val norm = 1.0 / (1.0 + alpha)
                b0 = (1.0 + cosW0) / 2.0 * norm
                b1 = -(1.0 + cosW0) * norm
                b2 = b0
                a0 = 1.0
                a1 = -2.0 * cosW0 * norm
                a2 = (1.0 - alpha) * norm
            }

            Type.BAND_PASS -> {
                val norm = 1.0 / (1.0 + alpha)
                b0 = alpha * norm
                b1 = 0.0
                b2 = -alpha * norm
                a0 = 1.0
                a1 = -2.0 * cosW0 * norm
                a2 = (1.0 - alpha) * norm
            }

            Type.NOTCH -> {
                val norm = 1.0 / (1.0 + alpha)
                b0 = 1.0 * norm
                b1 = -2.0 * cosW0 * norm
                b2 = 1.0 * norm
                a0 = 1.0
                a1 = -2.0 * cosW0 * norm
                a2 = (1.0 - alpha) * norm
            }

            Type.ALL_PASS -> {
                val norm = 1.0 / (1.0 + alpha)
                b0 = (1.0 - alpha) * norm
                b1 = -2.0 * cosW0 * norm
                b2 = (1.0 + alpha) * norm
                a0 = 1.0
                a1 = -2.0 * cosW0 * norm
                a2 = (1.0 - alpha) * norm
            }

            Type.PEAKING -> {
                val norm = 1.0 / (1.0 + alpha / A)
                b0 = (1.0 + alpha * A) * norm
                b1 = -2.0 * cosW0 * norm
                b2 = (1.0 - alpha * A) * norm
                a0 = 1.0
                a1 = -2.0 * cosW0 * norm
                a2 = (1.0 - alpha / A) * norm
            }

            Type.LOW_SHELF -> {
                val sqrtA = sqrt(A)
                val twoSqrtAAlpha = 2.0 * sqrtA * alpha
                val norm = 1.0 / (1.0 + twoSqrtAAlpha + A)
                b0 = A * ((A + 1.0) - (A - 1.0) * cosW0 + twoSqrtAAlpha) * norm
                b1 = 2.0 * A * ((A - 1.0) - (A + 1.0) * cosW0) * norm
                b2 = A * ((A + 1.0) - (A - 1.0) * cosW0 - twoSqrtAAlpha) * norm
                a0 = 1.0
                a1 = -2.0 * ((A - 1.0) + (A + 1.0) * cosW0) * norm
                a2 = ((A + 1.0) + (A - 1.0) * cosW0 - twoSqrtAAlpha) * norm
            }

            Type.HIGH_SHELF -> {
                val sqrtA = sqrt(A)
                val twoSqrtAAlpha = 2.0 * sqrtA * alpha
                val norm = 1.0 / (1.0 + twoSqrtAAlpha + A)
                b0 = A * ((A + 1.0) + (A - 1.0) * cosW0 + twoSqrtAAlpha) * norm
                b1 = -2.0 * A * ((A - 1.0) + (A + 1.0) * cosW0) * norm
                b2 = A * ((A + 1.0) + (A - 1.0) * cosW0 - twoSqrtAAlpha) * norm
                a0 = 1.0
                a1 = 2.0 * ((A - 1.0) - (A + 1.0) * cosW0) * norm
                a2 = ((A + 1.0) - (A - 1.0) * cosW0 - twoSqrtAAlpha) * norm
            }
        }
    }

    companion object {
        private fun pow10(x: Double): Double {
            // 10^x = e^(x * ln(10))
            return kotlin.math.exp(x * 2.302585093)
        }
    }
}
