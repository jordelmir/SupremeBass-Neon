package com.supremecorp.bass.experiment

import android.util.Log
import com.supremecorp.bass.domain.model.*
import com.supremecorp.bass.signal.SignalEngine
import com.supremecorp.bass.signal.SignalEngineState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

class ExperimentRunner(
    private val signalEngine: SignalEngine
) {
    private companion object {
        const val TAG = "SupremeBass_ExperimentRunner"
    }

    private val _currentExperiment = MutableStateFlow<AcousticExperiment?>(null)
    val currentExperiment: StateFlow<AcousticExperiment?> = _currentExperiment.asStateFlow()

    private var runnerJob: Job? = null

    suspend fun startExperiment(experiment: AcousticExperiment) {
        require(!experiment.isTerminal) { "Experiment already in terminal state: ${experiment.status}" }
        require(experiment.variables.isNotEmpty()) { "Experiment must have at least one variable" }

        Log.i(TAG, "Starting experiment: ${experiment.name} | type=${experiment.type} | steps=${experiment.stepCount} | dwell=${experiment.dwellMs}ms")
        _currentExperiment.value = experiment.withStarted()

        runnerJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                runExperimentLoop(experiment)
            } catch (e: CancellationException) {
                Log.w(TAG, "Experiment cancelled: ${experiment.name}")
                _currentExperiment.value = _currentExperiment.value?.withAborted("Cancelled: ${e.message}")
                signalEngine.stop()
            } catch (e: Exception) {
                Log.e(TAG, "Experiment failed: ${experiment.name} — ${e.message}", e)
                _currentExperiment.value = _currentExperiment.value?.withFailed(e.message ?: "Unknown error")
                signalEngine.stop()
            }
        }
    }

    fun pauseExperiment() {
        val exp = _currentExperiment.value ?: return
        if (exp.status == ExperimentStatus.RUNNING) {
            Log.i(TAG, "Pausing experiment: ${exp.name} at step ${exp.currentStep}")
            signalEngine.stop()
            _currentExperiment.value = exp.copy(status = ExperimentStatus.PAUSED)
            runnerJob?.cancel()
        }
    }

    fun resumeExperiment() {
        val exp = _currentExperiment.value ?: return
        if (exp.status == ExperimentStatus.PAUSED) {
            Log.i(TAG, "Resuming experiment: ${exp.name} from step ${exp.currentStep}")
            _currentExperiment.value = exp.copy(status = ExperimentStatus.RUNNING)
            runnerJob = CoroutineScope(Dispatchers.IO).launch {
                try {
                    runExperimentLoop(exp)
                } catch (e: CancellationException) {
                    Log.w(TAG, "Experiment cancelled during resume: ${exp.name}")
                    _currentExperiment.value = _currentExperiment.value?.withAborted("Cancelled")
                    signalEngine.stop()
                } catch (e: Exception) {
                    Log.e(TAG, "Experiment failed during resume: ${exp.name} — ${e.message}", e)
                    _currentExperiment.value = _currentExperiment.value?.withFailed(e.message ?: "Unknown")
                    signalEngine.stop()
                }
            }
        }
    }

    fun abortExperiment() {
        val exp = _currentExperiment.value
        Log.w(TAG, "Aborting experiment: ${exp?.name} at step ${exp?.currentStep}")
        runnerJob?.cancel()
        signalEngine.stop()
        _currentExperiment.value = _currentExperiment.value?.withAborted("User abort")
    }

    private suspend fun runExperimentLoop(experiment: AcousticExperiment) {
        val exp = _currentExperiment.value ?: return
        val startStep = exp.currentStep
        val allObservations = exp.observations.toMutableList()

        Log.i(TAG, "Loop started: ${exp.name} | startStep=$startStep | totalSteps=${exp.stepCount}")

        for (step in startStep until exp.stepCount) {
            if (runnerJob?.isActive != true) return

            val variable = exp.variables.first()
            val freq = variable.min + (variable.max - variable.min) * step / (exp.stepCount - 1).coerceAtLeast(1)

            Log.d(TAG, "Step ${step + 1}/${exp.stepCount}: freq=${String.format("%.1f", freq)}Hz")

            val stepObs = runStep(exp, freq, exp.repeatsPerStep)
            allObservations.addAll(stepObs)

            _currentExperiment.value = _currentExperiment.value?.withStep(step + 1, stepObs)

            delay(exp.dwellMs.toLong())
        }

        val result = computeResult(allObservations, exp)
        Log.i(TAG, "Experiment complete: ${exp.name} | observations=${allObservations.size} | peakGainDb=${String.format("%.2f", result.peakGainDb)} | rmsGainDb=${String.format("%.2f", result.rmsGainDb)} | duration=${result.durationMs}ms")
        _currentExperiment.value = _currentExperiment.value?.withCompleted(result)
    }

    private suspend fun runStep(
        experiment: AcousticExperiment,
        frequencyHz: Double,
        repeats: Int
    ): List<ExperimentObservation> {
        val obs = mutableListOf<ExperimentObservation>()

        for (r in 0 until repeats) {
            if (runnerJob?.isActive != true) return obs

            val config = experiment.signalConfig.copy(frequencyHz = frequencyHz)
            val started = signalEngine.start(config)

            if (!started) {
                Log.e(TAG, "Failed to start signal at ${frequencyHz}Hz, repeat $r")
                obs.add(
                    ExperimentObservation(
                        frequencyHz = frequencyHz,
                        variable = experiment.variables.first().name,
                        requestedValue = frequencyHz,
                        measuredPeak = 0.0,
                        measuredRms = 0.0,
                        authority = MeasurementAuthority.DIGITAL
                    )
                )
                continue
            }

            delay(experiment.dwellMs.toLong())

            val telemetry = signalEngine.getTelemetry()
            signalEngine.stop()

            delay(50)

            obs.add(
                ExperimentObservation(
                    frequencyHz = frequencyHz,
                    variable = experiment.variables.first().name,
                    requestedValue = frequencyHz,
                    measuredPeak = telemetry?.peak?.toDouble() ?: 0.0,
                    measuredRms = telemetry?.rms?.toDouble() ?: 0.0,
                    authority = MeasurementAuthority.DIGITAL
                )
            )

            Log.d(TAG, "Obs: freq=${frequencyHz}Hz peak=${telemetry?.peak} rms=${telemetry?.rms}")
        }
        return obs
    }

    private fun computeResult(
        observations: List<ExperimentObservation>,
        experiment: AcousticExperiment
    ): ExperimentResult {
        if (observations.isEmpty()) {
            return ExperimentResult(
                summary = "No observations collected",
                peakGainDb = 0.0,
                rmsGainDb = 0.0,
                observations = observations,
                durationMs = 0L
            )
        }

        val peakMax = observations.maxOfOrNull { it.measuredPeak } ?: 0.0
        val rmsMax = observations.maxOfOrNull { it.measuredRms } ?: 0.0
        val peakMin = observations.minOfOrNull { it.measuredPeak } ?: 0.0
        val rmsMin = observations.minOfOrNull { it.measuredRms } ?: 0.0

        val peakGainDb = if (peakMax > 0 && peakMin > 0) {
            20 * log10(peakMax / peakMin)
        } else 0.0

        val rmsGainDb = if (rmsMax > 0 && rmsMin > 0) {
            20 * log10(rmsMax / rmsMin)
        } else 0.0

        val durationMs = experiment.startedAtMs?.let {
            System.currentTimeMillis() - it
        } ?: 0L

        val peakAvg = observations.map { it.measuredPeak }.average()
        val rmsAvg = observations.map { it.measuredRms }.average()

        return ExperimentResult(
            summary = "${experiment.name}: ${observations.size} obs, " +
                    "peak range: ${String.format("%.4f", peakMin)}-${String.format("%.4f", peakMax)}, " +
                    "rms range: ${String.format("%.4f", rmsMin)}-${String.format("%.4f", rmsMax)}",
            peakGainDb = peakGainDb,
            rmsGainDb = rmsGainDb,
            observations = observations,
            durationMs = durationMs
        )
    }
}
