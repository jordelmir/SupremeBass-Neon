package com.supreme.android.dsp

import com.supreme.android.domain.model.SignalConfig
import com.supreme.android.domain.model.Waveform
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.exp

data class SweepPlan(
    val startHz: Double,
    val endHz: Double,
    val steps: Int,
    val dwellMs: Long,
    val amplitude: Float,
    val waveform: Waveform = Waveform.SINE,
    val sweepType: SweepType = SweepType.LINEAR
) {
    init {
        require(startHz > 0.0) { "startHz must be > 0, was $startHz" }
        require(endHz > startHz) { "endHz must be > startHz, was $endHz <= $startHz" }
        require(steps > 0) { "steps must be > 0, was $steps" }
        require(dwellMs > 0) { "dwellMs must be > 0, was $dwellMs" }
        require(amplitude in 0.0f..1.0f) { "amplitude must be in 0.0..1.0, was $amplitude" }
    }

    fun getFrequency(step: Int): Double {
        val t = step.toDouble() / (steps - 1).coerceAtLeast(1)
        return when (sweepType) {
            SweepType.LINEAR -> startHz + (endHz - startHz) * t
            SweepType.LOGARITHMIC -> startHz * (endHz / startHz).pow(t)
            SweepType.STEPPED -> {
                val stepIndex = (t * (steps - 1)).toInt().coerceIn(0, steps - 1)
                val logMin = ln(startHz)
                val logMax = ln(endHz)
                val logFreq = logMin + (logMax - logMin) * stepIndex.toDouble() / (steps - 1)
                exp(logFreq)
            }
        }
    }

    fun getStepConfig(step: Int): SignalConfig {
        return SignalConfig(
            frequencyHz = getFrequency(step),
            amplitude = amplitude,
            waveform = waveform,
            durationMs = dwellMs
        )
    }

    fun totalDurationMs(): Long = dwellMs * steps
}

enum class SweepType {
    LINEAR,
    LOGARITHMIC,
    STEPPED
}

class SweepEngine(
    private val generator: SignalGenerator = SignalGenerator()
) {
    private var currentStep: Int = 0
    private var sampleCountInStep: Int = 0
    private var samplesPerStep: Int = 0
    private var activePlan: SweepPlan? = null

    fun configure(sampleRate: Int) {
        generator.configure(sampleRate)
    }

    fun start(plan: SweepPlan, sampleRate: Int) {
        activePlan = plan
        currentStep = 0
        sampleCountInStep = 0
        samplesPerStep = (plan.dwellMs * sampleRate / 1000.0).toInt()
        generator.reset()
    }

    fun render(buffer: FloatArray, frameCount: Int, sampleRate: Int): SweepState {
        val plan = activePlan ?: return SweepState.Complete

        if (currentStep >= plan.steps) {
            return SweepState.Complete
        }

        val config = plan.getStepConfig(currentStep)
        generator.render(buffer, frameCount, config, sampleRate)

        sampleCountInStep += frameCount
        val stepProgress = (currentStep + 1).toFloat() / plan.steps

        if (sampleCountInStep >= samplesPerStep) {
            currentStep++
            sampleCountInStep = 0

            if (currentStep >= plan.steps) {
                return SweepState.StepComplete(plan.steps - 1, plan.steps, 1.0f)
            }
        }

        return SweepState.StepComplete(currentStep, plan.steps, stepProgress)
    }

    fun stop() {
        activePlan = null
        currentStep = 0
        sampleCountInStep = 0
        generator.reset()
    }

    fun getCurrentStep(): Int = currentStep
    fun getActivePlan(): SweepPlan? = activePlan
}

sealed interface SweepState {
    data object Complete : SweepState
    data class StepComplete(
        val step: Int,
        val totalSteps: Int,
        val progress: Float
    ) : SweepState
}
