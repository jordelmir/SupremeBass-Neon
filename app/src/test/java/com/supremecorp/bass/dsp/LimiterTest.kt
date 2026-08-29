package com.supremecorp.bass.dsp

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class LimiterTest {

    @Test
    fun `does not clip within ceiling`() {
        val limiter = Limiter(ceiling = 1.0f)
        val buffer = floatArrayOf(0.5f, -0.5f, 0.8f, -0.8f, 0.1f)
        limiter.process(buffer, 5)
        assertEquals(0, limiter.clippedSamples)
        assertEquals(0.8f, limiter.peak)
    }

    @Test
    fun `clips samples exceeding ceiling`() {
        val limiter = Limiter(ceiling = 0.8f)
        val buffer = floatArrayOf(0.5f, 1.0f, -1.0f, 0.3f, 0.9f)
        limiter.process(buffer, 5)
        assertEquals(3, limiter.clippedSamples)
        assertEquals(0.8f, limiter.peak)
        assertTrue(buffer[1] == 0.8f, "Positive clip failed")
        assertTrue(buffer[2] == -0.8f, "Negative clip failed")
        assertTrue(buffer[4] == 0.8f, "Positive clip at end failed")
    }

    @Test
    fun `rms is calculated correctly`() {
        val limiter = Limiter(ceiling = 1.0f)
        val buffer = floatArrayOf(1.0f, -1.0f, 1.0f, -1.0f)
        limiter.process(buffer, 4)
        assertEquals(1.0f, limiter.rms, 0.001f)
    }

    @Test
    fun `crest factor is correct`() {
        val limiter = Limiter(ceiling = 1.0f)
        val buffer = floatArrayOf(1.0f, 0.0f, 1.0f, 0.0f)
        limiter.process(buffer, 4)
        val crest = limiter.getCrestFactor()
        assertTrue(crest > 1.0f, "Crest factor should be > 1 for non-constant signal")
    }

    @Test
    fun `reset clears state`() {
        val limiter = Limiter(ceiling = 0.5f)
        val buffer = floatArrayOf(1.0f, -1.0f)
        limiter.process(buffer, 2)
        assertTrue(limiter.clippedSamples > 0)
        limiter.reset()
        assertEquals(0, limiter.clippedSamples)
        assertEquals(0.0f, limiter.peak)
        assertEquals(0.0f, limiter.rms)
    }

    @Test
    fun `all samples within ceiling after process`() {
        val limiter = Limiter(ceiling = 0.5f)
        val buffer = FloatArray(1024) { (it % 100).toFloat() / 50.0f }
        limiter.process(buffer, 1024)
        assertTrue(buffer.all { abs(it) <= 0.5f }, "All samples should be within ceiling")
    }
}
