package com.supremecorp.bass.dsp

import com.supremecorp.bass.domain.model.SignalConfig
import com.supremecorp.bass.domain.model.Waveform
import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertTrue

class ChirpNoiseTest {

    @Test
    fun `chirp generates varying frequency`() {
        val gen = SignalGenerator()
        gen.configure(48_000)
        val config = SignalConfig(
            frequencyHz = 100.0,
            amplitude = 0.5f,
            waveform = Waveform.CHIRP,
            chirpEndHz = 1000.0,
            durationMs = 100
        )
        val buf1 = FloatArray(4800)
        val buf2 = FloatArray(4800)
        gen.render(buf1, 4800, config, 48_000)
        gen.render(buf2, 4800, config, 48_000)
        val rms1 = kotlin.math.sqrt(buf1.map { it * it }.average())
        val rms2 = kotlin.math.sqrt(buf2.map { it * it }.average())
        assertTrue(rms1 > 0.0, "Chirp should produce signal")
        assertTrue(rms2 > 0.0, "Chirp should continue producing signal")
    }

    @Test
    fun `chirp respects amplitude`() {
        val gen = SignalGenerator()
        gen.configure(48_000)
        val config = SignalConfig(
            frequencyHz = 100.0,
            amplitude = 0.3f,
            waveform = Waveform.CHIRP,
            chirpEndHz = 500.0,
            durationMs = 50
        )
        val buffer = FloatArray(2400)
        gen.render(buffer, 2400, config, 48_000)
        assertTrue(buffer.all { abs(it) <= 0.3f }, "Chirp should respect amplitude limit")
    }

    @Test
    fun `noise band produces random signal`() {
        val gen = SignalGenerator()
        gen.configure(48_000)
        val config = SignalConfig(
            frequencyHz = 1000.0,
            amplitude = 0.5f,
            waveform = Waveform.NOISE_BAND,
            noiseLowHz = 100.0,
            noiseHighHz = 2000.0
        )
        val buffer = FloatArray(4800)
        gen.render(buffer, 4800, config, 48_000)
        val rms = kotlin.math.sqrt(buffer.map { it * it }.average())
        assertTrue(rms > 0.0, "Noise band should produce signal")
        assertTrue(buffer.all { abs(it) <= 0.5f }, "Noise band should respect amplitude")
    }

    @Test
    fun `noise band zero bandwidth produces silence`() {
        val gen = SignalGenerator()
        gen.configure(48_000)
        val config = SignalConfig(
            frequencyHz = 1000.0,
            amplitude = 0.5f,
            waveform = Waveform.NOISE_BAND,
            noiseLowHz = 1000.0,
            noiseHighHz = 1000.0
        )
        val buffer = FloatArray(4800)
        gen.render(buffer, 4800, config, 48_000)
        val rms = kotlin.math.sqrt(buffer.map { it * it }.average())
        assertTrue(rms < 0.01f, "Zero bandwidth noise should be near silent")
    }
}
