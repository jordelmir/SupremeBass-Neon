package com.supremecorp.bass.dsp

import com.supremecorp.bass.domain.model.SignalConfig
import com.supremecorp.bass.domain.model.Waveform
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class SweepEngineTest {

    @Test
    fun `sweep plan validates start less than end`() {
        val plan = SweepPlan(
            startHz = 20.0,
            endHz = 20_000.0,
            steps = 100,
            dwellMs = 50,
            amplitude = 0.5f
        )
        assertTrue(plan.startHz < plan.endHz)
    }

    @Test
    fun `sweep plan total duration is steps * dwell`() {
        val plan = SweepPlan(
            startHz = 20.0,
            endHz = 20_000.0,
            steps = 100,
            dwellMs = 50,
            amplitude = 0.5f
        )
        assertEquals(5000L, plan.totalDurationMs())
    }

    @Test
    fun `linear sweep frequencies are linearly spaced`() {
        val plan = SweepPlan(
            startHz = 100.0,
            endHz = 1000.0,
            steps = 10,
            dwellMs = 10,
            amplitude = 0.5f,
            sweepType = SweepType.LINEAR
        )
        val f0 = plan.getFrequency(0)
        val f5 = plan.getFrequency(5)
        val f9 = plan.getFrequency(9)
        assertEquals(100.0, f0, 0.01)
        assertEquals(1000.0, f9, 0.01)
        assertTrue(f5 > f0 && f5 < f9, "Mid frequency should be between start and end")
    }

    @Test
    fun `log sweep frequencies are logarithmically spaced`() {
        val plan = SweepPlan(
            startHz = 100.0,
            endHz = 10_000.0,
            steps = 3,
            dwellMs = 10,
            amplitude = 0.5f,
            sweepType = SweepType.LOGARITHMIC
        )
        val f0 = plan.getFrequency(0)
        val f1 = plan.getFrequency(1)
        val f2 = plan.getFrequency(2)
        assertEquals(100.0, f0, 0.01)
        assertTrue(f1 > f0 && f1 < f2)
        assertEquals(10_000.0, f2, 0.01)
    }

    @Test
    fun `stepped sweep returns discrete frequencies`() {
        val plan = SweepPlan(
            startHz = 100.0,
            endHz = 1000.0,
            steps = 5,
            dwellMs = 10,
            amplitude = 0.5f,
            sweepType = SweepType.STEPPED
        )
        val freqs = (0 until 5).map { plan.getFrequency(it) }
        assertTrue(freqs.all { it > 0.0 })
        assertEquals(5, freqs.size)
    }

    @Test
    fun `sweep engine renders buffers`() {
        val gen = SignalGenerator()
        gen.configure(48_000)
        val sweep = SweepEngine(gen)
        val plan = SweepPlan(
            startHz = 100.0,
            endHz = 1000.0,
            steps = 3,
            dwellMs = 100,
            amplitude = 0.5f
        )
        sweep.start(plan, 48_000)
        val buffer = FloatArray(1024)
        val state = sweep.render(buffer, 1024, 48_000)
        assertTrue(state is SweepState.StepComplete)
        assertTrue(buffer.any { abs(it) > 0.0f })
    }

    @Test
    fun `sweep engine completes after all steps`() {
        val gen = SignalGenerator()
        gen.configure(48_000)
        val sweep = SweepEngine(gen)
        val plan = SweepPlan(
            startHz = 100.0,
            endHz = 1000.0,
            steps = 2,
            dwellMs = 10,
            amplitude = 0.5f
        )
        sweep.start(plan, 48_000)
        val buffer = FloatArray(480)
        repeat(10) {
            sweep.render(buffer, 480, 48_000)
        }
        val state = sweep.render(buffer, 480, 48_000)
        assertTrue(state is SweepState.Complete)
    }

    @Test
    fun `sweep engine stop resets state`() {
        val gen = SignalGenerator()
        gen.configure(48_000)
        val sweep = SweepEngine(gen)
        val plan = SweepPlan(
            startHz = 100.0,
            endHz = 1000.0,
            steps = 5,
            dwellMs = 100,
            amplitude = 0.5f
        )
        sweep.start(plan, 48_000)
        sweep.stop()
        assertEquals(0, sweep.getCurrentStep())
        assertEquals(null, sweep.getActivePlan())
    }
}
