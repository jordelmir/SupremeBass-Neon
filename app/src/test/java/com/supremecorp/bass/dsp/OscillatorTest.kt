package com.supremecorp.bass.dsp

import com.supremecorp.bass.domain.model.Waveform
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class OscillatorTest {

    @Test
    fun `sine output is within -1 to 1`() {
        val osc = Oscillator(48_000)
        osc.frequencyHz = 440.0
        repeat(48_000) {
            val sample = osc.renderSample(Waveform.SINE)
            assertTrue(sample >= -1.0f && sample <= 1.0f, "Sine sample out of range: $sample at frame $it")
        }
    }

    @Test
    fun `sine output matches expected values`() {
        val osc = Oscillator(48_000)
        osc.frequencyHz = 1.0
        val sample = osc.renderSample(Waveform.SINE)
        val expected = sin(2.0 * PI / 48_000).toFloat()
        assertTrue(abs(sample - expected) < 0.01f, "Sine mismatch: $sample vs $expected")
    }

    @Test
    fun `square output is binary`() {
        val osc = Oscillator(48_000)
        osc.frequencyHz = 100.0
        val samples = (0 until 48_000).map { osc.renderSample(Waveform.SQUARE) }
        assertTrue(samples.all { it == 1.0f || it == -1.0f }, "Square should only produce +1/-1")
    }

    @Test
    fun `triangle output is within -1 to 1`() {
        val osc = Oscillator(48_000)
        osc.frequencyHz = 440.0
        repeat(48_000) {
            val sample = osc.renderSample(Waveform.TRIANGLE)
            assertTrue(sample >= -1.0f && sample <= 1.0f, "Triangle out of range: $sample")
        }
    }

    @Test
    fun `sawtooth output is within -1 to 1`() {
        val osc = Oscillator(48_000)
        osc.frequencyHz = 440.0
        repeat(48_000) {
            val sample = osc.renderSample(Waveform.SAWTOOTH)
            assertTrue(sample >= -1.0f && sample <= 1.0f, "Sawtooth out of range: $sample")
        }
    }

    @Test
    fun `phase accumulation is bounded`() {
        val osc = Oscillator(48_000)
        osc.frequencyHz = 1000.0
        repeat(480_000) {
            osc.renderSample(Waveform.SINE)
        }
        val phase = osc.getPhase()
        assertTrue(phase >= 0.0 && phase < 2.0 * PI, "Phase out of bounds: $phase")
    }

    @Test
    fun `renderBuffer fills entire buffer`() {
        val osc = Oscillator(48_000)
        osc.frequencyHz = 440.0
        val buffer = FloatArray(1024)
        osc.renderBuffer(buffer, 1024, Waveform.SINE, 0.5f)
        val nonZeroCount = buffer.count { it != 0.0f }
        assertTrue(nonZeroCount > 900, "Most buffer should have non-zero values, got $nonZeroCount")
        assertTrue(buffer.all { abs(it) <= 0.5f }, "Buffer should respect amplitude")
    }

    @Test
    fun `reset clears phase`() {
        val osc = Oscillator(48_000)
        osc.frequencyHz = 440.0
        repeat(1000) { osc.renderSample(Waveform.SINE) }
        assertNotEquals(0.0, osc.getPhase())
        osc.reset()
        assertEquals(0.0, osc.getPhase())
    }

    @Test
    fun `setSampleRate updates phase increment`() {
        val osc = Oscillator(44_100)
        osc.frequencyHz = 440.0
        osc.setSampleRate(96_000)
        assertEquals(96_000, osc.getSampleRate())
    }
}
