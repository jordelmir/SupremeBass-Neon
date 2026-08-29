package com.supremecorp.bass.dsp

import com.supremecorp.bass.domain.model.Envelope
import com.supremecorp.bass.domain.model.SignalConfig
import com.supremecorp.bass.domain.model.Waveform
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class SignalGeneratorTest {

    @Test
    fun `generates sine buffer`() {
        val gen = SignalGenerator()
        gen.configure(48_000)
        val config = SignalConfig(
            frequencyHz = 440.0,
            amplitude = 0.5f,
            waveform = Waveform.SINE
        )
        val buffer = FloatArray(1024)
        gen.render(buffer, 1024, config, 48_000)
        assertTrue(buffer.any { abs(it) > 0.0f }, "Buffer should have non-zero samples")
        assertTrue(buffer.all { abs(it) <= 0.5f }, "Buffer should respect amplitude limit")
    }

    @Test
    fun `generates square buffer`() {
        val gen = SignalGenerator()
        gen.configure(48_000)
        val config = SignalConfig(
            frequencyHz = 440.0,
            amplitude = 0.8f,
            waveform = Waveform.SQUARE
        )
        val buffer = FloatArray(1024)
        gen.render(buffer, 1024, config, 48_000)
        assertTrue(buffer.any { it == 0.8f || it == -0.8f }, "Square should produce +/- amplitude")
    }

    @Test
    fun `generates triangle buffer`() {
        val gen = SignalGenerator()
        gen.configure(48_000)
        val config = SignalConfig(
            frequencyHz = 440.0,
            amplitude = 0.5f,
            waveform = Waveform.TRIANGLE
        )
        val buffer = FloatArray(1024)
        gen.render(buffer, 1024, config, 48_000)
        assertTrue(buffer.all { abs(it) <= 0.5f }, "Triangle should respect amplitude")
    }

    @Test
    fun `generates sawtooth buffer`() {
        val gen = SignalGenerator()
        gen.configure(48_000)
        val config = SignalConfig(
            frequencyHz = 440.0,
            amplitude = 0.5f,
            waveform = Waveform.SAWTOOTH
        )
        val buffer = FloatArray(1024)
        gen.render(buffer, 1024, config, 48_000)
        assertTrue(buffer.all { abs(it) <= 0.5f }, "Sawtooth should respect amplitude")
    }

    @Test
    fun `multi-tone normalizes sum`() {
        val gen = SignalGenerator()
        gen.configure(48_000)
        val config = SignalConfig(
            frequencyHz = 100.0,
            amplitude = 1.0f,
            waveform = Waveform.MULTI_TONE
        )
        val buffer = FloatArray(4096)
        gen.render(buffer, 4096, config, 48_000)
        val peak = buffer.maxOfOrNull { abs(it) } ?: 0.0f
        assertTrue(peak <= 1.0f, "Multi-tone should not exceed 1.0 after normalization, peak=$peak")
    }

    @Test
    fun `ramp envelope applies attack`() {
        val gen = SignalGenerator()
        gen.configure(48_000)
        val config = SignalConfig(
            frequencyHz = 440.0,
            amplitude = 1.0f,
            waveform = Waveform.SINE,
            envelope = Envelope.Ramp(attackMs = 100, releaseMs = 100)
        )
        val buffer = FloatArray(4800)
        gen.render(buffer, 4800, config, 48_000)
        val firstSample = abs(buffer[0])
        val midSample = abs(buffer[2400])
        assertTrue(firstSample < midSample, "Attack ramp should start low: first=$firstSample, mid=$midSample")
    }

    @Test
    fun `burst envelope gates output`() {
        val gen = SignalGenerator()
        gen.configure(48_000)
        val config = SignalConfig(
            frequencyHz = 440.0,
            amplitude = 1.0f,
            waveform = Waveform.SINE,
            envelope = Envelope.Burst(onMs = 10, offMs = 10)
        )
        val buffer = FloatArray(4800)
        gen.render(buffer, 4800, config, 48_000)
        val midOn = abs(buffer[240])
        assertTrue(midOn > 0.0f, "Burst ON region should have signal, got $midOn")
        val offRegion = buffer.slice(500..900)
        val maxInOff = offRegion.maxOfOrNull { abs(it) } ?: 0.0f
        assertTrue(maxInOff < 0.01f, "Burst OFF region should be silent, max=$maxInOff")
    }

    @Test
    fun `phase continuity across renders`() {
        val gen = SignalGenerator()
        gen.configure(48_000)
        val config = SignalConfig(
            frequencyHz = 440.0,
            amplitude = 0.5f,
            waveform = Waveform.SINE
        )
        val buf1 = FloatArray(1024)
        val buf2 = FloatArray(1024)
        gen.render(buf1, 1024, config, 48_000)
        gen.render(buf2, 1024, config, 48_000)
        val lastSample = buf1[1023]
        val firstSample = buf2[0]
        val diff = abs(firstSample - lastSample)
        assertTrue(diff < 0.1f, "Phase discontinuity detected: diff=$diff")
    }
}
