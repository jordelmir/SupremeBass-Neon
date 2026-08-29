package com.supremecorp.bass.dsp

import com.supremecorp.bass.domain.model.SignalConfig
import com.supremecorp.bass.domain.model.Waveform
import kotlin.math.PI
import kotlin.math.sin
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

    fun configure(sampleRate: Int) {
        oscillator.setSampleRate(sampleRate)
    }

    fun reset() {
        oscillator.reset()
        currentPhase = 0.0
        chirpTime = 0.0
    }

    fun reset(phase: Double) {
        oscillator.reset(phase)
        currentPhase = phase
        chirpTime = 0.0
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

        applyEnvelope(buffer, frameCount, config)
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

        val freqSlope = (endFreq - startFreq) / durationSec

        for (i in 0 until frameCount) {
            val t = chirpTime + i.toDouble() / sampleRate
            val instantFreq = startFreq + freqSlope * t
            if (instantFreq <= 0.0 || !instantFreq.isFinite()) {
                buffer[i] = 0.0f
                continue
            }
            val sample = sin(2.0 * PI * instantFreq * t).toFloat()
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

        for (i in 0 until frameCount) {
            val freq = lowFreq + noiseRandom.nextDouble() * bandwidth
            val phase = noiseRandom.nextDouble() * 2.0 * PI
            val sample = sin(phase).toFloat()
            buffer[i] = sample * config.amplitude
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
        config: SignalConfig
    ) {
        val env = config.envelope ?: return
        val sampleRate = oscillator.getSampleRate()
        when (env) {
            is com.supremecorp.bass.domain.model.Envelope.Ramp -> {
                val attackSamples = (env.attackMs * sampleRate / 1000.0).toInt()
                val releaseSamples = (env.releaseMs * sampleRate / 1000.0).toInt()
                val releaseStart = frameCount - releaseSamples

                for (i in 0 until frameCount) {
                    val gain = when {
                        i < attackSamples -> i.toFloat() / attackSamples.toFloat()
                        i >= releaseStart -> (frameCount - i).toFloat() / releaseSamples.toFloat()
                        else -> 1.0f
                    }
                    buffer[i] *= gain.coerceIn(0.0f, 1.0f)
                }
            }
            is com.supremecorp.bass.domain.model.Envelope.Burst -> {
                val onSamples = (env.onMs * sampleRate / 1000.0).toInt()
                val cycleSamples = onSamples + (env.offMs * sampleRate / 1000.0).toInt()
                if (cycleSamples <= 0) return

                for (i in 0 until frameCount) {
                    val posInCycle = i % cycleSamples
                    buffer[i] *= if (posInCycle < onSamples) 1.0f else 0.0f
                }
            }
        }
    }
}
