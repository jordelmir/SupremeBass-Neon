package com.supremecorp.bass.dsp

import com.supremecorp.bass.domain.model.Waveform
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.roundToInt

class Oscillator(
    private var sampleRate: Int = 48_000
) {
    private var phase: Double = 0.0
    private var phaseIncrement: Double = 0.0

    var frequencyHz: Double = 0.0
        set(value) {
            field = value
            phaseIncrement = 2.0 * PI * value / sampleRate
        }

    fun setSampleRate(rate: Int) {
        sampleRate = rate
        phaseIncrement = 2.0 * PI * frequencyHz / sampleRate
    }

    fun getSampleRate(): Int = sampleRate

    fun reset() {
        phase = 0.0
    }

    fun reset(startPhase: Double) {
        phase = startPhase
    }

    fun getPhase(): Double = phase

    fun renderSample(waveform: Waveform): Float {
        val sample: Float = when (waveform) {
            Waveform.SINE -> sin(phase).toFloat()
            Waveform.SQUARE -> if (phase % (2.0 * PI) < PI) 1.0f else -1.0f
            Waveform.TRIANGLE -> {
                val normalized = (phase % (2.0 * PI)) / (2.0 * PI)
                (4.0f * kotlin.math.abs(normalized.toFloat() - 0.5f) - 1.0f)
            }
            Waveform.SAWTOOTH -> {
                val normalized = (phase % (2.0 * PI)) / (2.0 * PI)
                (2.0f * normalized.toFloat() - 1.0f)
            }
            Waveform.PULSE -> {
                val duty = 0.25
                val normalized = (phase % (2.0 * PI)) / (2.0 * PI)
                if (normalized < duty) 1.0f else -1.0f
            }
            else -> sin(phase).toFloat()
        }

        advancePhase()
        return sample
    }

    fun renderBuffer(
        buffer: FloatArray,
        frameCount: Int,
        waveform: Waveform,
        amplitude: Float
    ) {
        for (i in 0 until frameCount) {
            buffer[i] = renderSample(waveform) * amplitude
        }
    }

    fun renderBufferWithPhase(
        buffer: FloatArray,
        frameCount: Int,
        waveform: Waveform,
        amplitude: Float,
        outPhase: FloatArray
    ) {
        for (i in 0 until frameCount) {
            buffer[i] = renderSample(waveform) * amplitude
            outPhase[i] = phase.toFloat()
        }
    }

    private fun advancePhase() {
        phase += phaseIncrement
        if (phase >= 2.0 * PI) {
            phase -= 2.0 * PI
        }
    }
}
