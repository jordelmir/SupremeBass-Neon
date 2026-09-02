package com.supreme.android.dsp

import com.supreme.android.domain.model.SignalConfig
import com.supreme.android.domain.model.Waveform
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.max
import kotlin.math.ln
import kotlin.math.pow
import java.util.Random

class SignalGenerator(
    private val oscillator: Oscillator = Oscillator()
) {
    private var currentPhase: Double = 0.0
    private var chirpTime: Double = 0.0
    private val noiseRandom = Random()

    // Session-wide sample clock for envelope tracking (persists across buffer boundaries)
    private var envelopeSampleClock: Long = 0L

    fun configure(sampleRate: Int) {
        oscillator.setSampleRate(sampleRate)
    }

    fun reset() {
        oscillator.reset()
        currentPhase = 0.0
        chirpTime = 0.0
        envelopeSampleClock = 0L
    }

    fun reset(phase: Double) {
        oscillator.reset(phase)
        currentPhase = phase
        chirpTime = 0.0
        envelopeSampleClock = 0L
    }

    fun getCurrentPhase(): Double = currentPhase

    fun render(
        buffer: FloatArray,
        frameCount: Int,
        config: SignalConfig,
        sampleRate: Int
    ) {
        oscillator.setSampleRate(sampleRate)

        when (config.waveform) {
            Waveform.CHIRP -> renderChirp(buffer, frameCount, config, sampleRate)
            Waveform.NOISE_BAND -> renderNoiseBand(buffer, frameCount, config, sampleRate)
            Waveform.MULTI_TONE, Waveform.HARMONIC_STACK -> {
                renderMultiTone(buffer, frameCount, config, sampleRate)
            }
            else -> {
                oscillator.frequencyHz = config.frequencyHz
                oscillator.renderBuffer(buffer, frameCount, config.waveform, config.amplitude)
            }
        }

        applyEnvelope(buffer, frameCount, config, sampleRate)
        currentPhase = oscillator.getPhase()
    }

    private fun renderChirp(
        buffer: FloatArray,
        frameCount: Int,
        config: SignalConfig,
        sampleRate: Int
    ) {
        val startFreq = config.frequencyHz
        val endFreq = config.chirpEndHz ?: startFreq * 2.0
        val durationSec = if (config.durationMs > 0) config.durationMs / 1000.0 else frameCount.toDouble() / sampleRate
        if (durationSec <= 0.0) return

        val k = (endFreq - startFreq) / durationSec // Rate of frequency change (Hz/s)

        // Correct chirp phase: φ(t) = 2π(f₀t + ½kt²)
        // This integrates instantaneous frequency f(t) = f₀ + kt
        for (i in 0 until frameCount) {
            val t = chirpTime + i.toDouble() / sampleRate
            val instantFreq = startFreq + k * t
            if (instantFreq <= 0.0 || !instantFreq.isFinite()) {
                buffer[i] = 0.0f
                continue
            }
            // Phase = 2π * (startFreq * t + 0.5 * k * t²)
            val phase = 2.0 * PI * (startFreq * t + 0.5 * k * t * t)
            val sample = sin(phase).toFloat()
            buffer[i] = sample * config.amplitude
        }

        chirpTime += frameCount.toDouble() / sampleRate
    }

    private fun renderNoiseBand(
        buffer: FloatArray,
        frameCount: Int,
        config: SignalConfig,
        sampleRate: Int
    ) {
        val lowFreq = config.noiseLowHz ?: 20.0
        val highFreq = config.noiseHighHz ?: minOf(config.frequencyHz * 2.0, sampleRate / 2.0 - 1.0)
        val bandwidth = highFreq - lowFreq
        if (bandwidth <= 0.0) return

        val centerFreq = (lowFreq + highFreq) / 2.0
        val q = centerFreq / bandwidth // Quality factor

        // Generate white noise and apply bandpass biquad filter
        // Using simple biquad bandpass: H(z) = (1 - z^-2) / (1 - 2r*cos(ω0)*z^-1 + r²z^-2)
        val w0 = 2.0 * PI * centerFreq / sampleRate
        val alpha = sin(w0) / (2.0 * q)
        val r = exp(-w0 / (2.0 * q)) // Damping factor

        // Biquad coefficients for bandpass
        val b0 = alpha / (1.0 + alpha)
        val b1 = 0.0
        val b2 = -alpha / (1.0 + alpha)
        val a1 = -2.0 * r * cos(w0) / (1.0 + alpha)
        val a2 = r * r / (1.0 + alpha)

        // State variables for filter
        var x1 = 0.0; var x2 = 0.0
        var y1 = 0.0; var y2 = 0.0

        for (i in 0 until frameCount) {
            // White noise input [-1, 1]
            val x0 = noiseRandom.nextDouble() * 2.0 - 1.0

            // Biquad difference equation
            val y0 = b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2

            // Update state
            x2 = x1; x1 = x0
            y2 = y1; y1 = y0

            buffer[i] = (y0 * config.amplitude).toFloat()
        }
    }

    private fun renderMultiTone(
        buffer: FloatArray,
        frameCount: Int,
        config: SignalConfig,
        sampleRate: Int
    ) {
        for (i in 0 until frameCount) {
            buffer[i] = 0.0f
        }

        val harmonics = 5
        for (h in 1..harmonics) {
            val harmFreq = config.frequencyHz * h
            if (harmFreq >= sampleRate / 2.0) break

            val harmAmp = config.amplitude / h
            oscillator.frequencyHz = harmFreq

            val tempBuffer = FloatArray(frameCount)
            oscillator.renderBuffer(tempBuffer, frameCount, Waveform.SINE, harmAmp)

            for (i in 0 until frameCount) {
                buffer[i] += tempBuffer[i]
            }
        }

        val peak = buffer.maxOfOrNull { kotlin.math.abs(it) } ?: 1.0f
        if (peak > 1.0f) {
            for (i in 0 until frameCount) {
                buffer[i] /= peak
            }
        }
    }

    private fun applyEnvelope(
        buffer: FloatArray,
        frameCount: Int,
        config: SignalConfig,
        sampleRate: Int
    ) {
        val env = config.envelope ?: return
        when (env) {
            is com.supreme.android.domain.model.Envelope.Ramp -> {
                val attackSamples = (env.attackMs * sampleRate / 1000.0).toLong()
                val releaseSamples = (env.releaseMs * sampleRate / 1000.0).toLong()

                for (i in 0 until frameCount) {
                    val globalSample = envelopeSampleClock + i
                    val gain = when {
                        // Attack phase: ramp up from 0 to 1
                        globalSample < attackSamples -> globalSample.toFloat() / attackSamples.toFloat()
                        // Sustain phase: hold at 1
                        else -> 1.0f
                    }
                    buffer[i] *= gain.coerceIn(0.0f, 1.0f)
                }
            }
            is com.supreme.android.domain.model.Envelope.Burst -> {
                val onSamples = (env.onMs * sampleRate / 1000.0).toLong()
                val cycleSamples = onSamples + (env.offMs * sampleRate / 1000.0).toLong()
                if (cycleSamples <= 0) return

                for (i in 0 until frameCount) {
                    val globalSample = envelopeSampleClock + i
                    val posInCycle = globalSample % cycleSamples
                    buffer[i] *= if (posInCycle < onSamples) 1.0f else 0.0f
                }
            }
        }

        // Advance the session-wide clock
        envelopeSampleClock += frameCount
    }
}
