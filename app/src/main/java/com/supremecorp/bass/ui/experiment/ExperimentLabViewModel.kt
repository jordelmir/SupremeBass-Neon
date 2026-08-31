package com.supremecorp.bass.ui.experiment

import android.app.Application
import android.media.AudioManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.supremecorp.bass.data.experiment.ExperimentDao
import com.supremecorp.bass.data.experiment.ExperimentDatabase
import com.supremecorp.bass.data.experiment.ExperimentRepository
import com.supremecorp.bass.domain.model.*
import com.supremecorp.bass.experiment.ExperimentRunner
import com.supremecorp.bass.experiment.FlameSafetyController
import com.supremecorp.bass.signal.SignalEngine
import com.supremecorp.bass.ui.settings.AudioSettingsPrefs
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ExperimentLabState(
    val experiments: List<AcousticExperiment> = emptyList(),
    val currentExperiment: AcousticExperiment? = null,
    val selectedExperiment: AcousticExperiment? = null,
    val isRunning: Boolean = false,
    val error: String? = null
)

class ExperimentLabViewModel(application: Application) : AndroidViewModel(application) {

    private companion object {
        const val TAG = "SupremeBass_ExperimentVM"
    }

    private val signalEngine = SignalEngine(application)
    private val runner = ExperimentRunner(signalEngine)
    private val audioManager = application.getSystemService(AudioManager::class.java)
    private val flameSafetyController = FlameSafetyController(
        audioManager,
        routeInterlockEnabled = AudioSettingsPrefs.routeInterlock(application)
    )

    private val experimentDao: ExperimentDao by lazy {
        ExperimentDatabase.getInstance(application).experimentDao()
    }
    private val repository = ExperimentRepository(experimentDao)

    private val _state = MutableStateFlow(ExperimentLabState())
    val state: StateFlow<ExperimentLabState> = _state.asStateFlow()

    val flameSafety: StateFlow<FlameSafetyState> = flameSafetyController.safetyState

    init {
        Log.i(TAG, "ExperimentLabViewModel initialized")
        viewModelScope.launch {
            repository.getAllExperiments().collect { experiments ->
                Log.d(TAG, "Experiments loaded: ${experiments.size}")
                _state.update { it.copy(experiments = experiments) }
            }
        }
        viewModelScope.launch {
            runner.currentExperiment.collect { exp ->
                _state.update { it.copy(currentExperiment = exp, isRunning = exp?.status == ExperimentStatus.RUNNING) }
                if (exp?.isTerminal == true) {
                    Log.i(TAG, "Experiment reached terminal state: ${exp.name} | status=${exp.status}")
                    exp.id.let { id ->
                        repository.saveExperiment(exp)
                    }
                }
            }
        }
    }

    fun startFrequencyResponseExperiment(
        name: String = "Freq Response ${System.currentTimeMillis()}",
        startHz: Double = 20.0,
        endHz: Double = 20000.0,
        steps: Int = 30,
        dwellMs: Int = 300
    ) {
        Log.i(TAG, "Starting freq response experiment: $name | ${startHz}-${endHz}Hz | ${steps} steps | ${dwellMs}ms dwell")
        val experiment = AcousticExperiment(
            name = name,
            type = ExperimentType.FREQUENCY_RESPONSE,
            variables = listOf(
                ExperimentVariable("frequency", VariableType.FREQUENCY, "Hz", startHz, endHz, (endHz - startHz) / steps, startHz)
            ),
            signalConfig = SignalConfig(frequencyHz = startHz, amplitude = 0.5f, waveform = Waveform.SINE),
            stepCount = steps,
            dwellMs = dwellMs
        )
        viewModelScope.launch {
            try {
                runner.startExperiment(experiment)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start freq response: ${e.message}", e)
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    /**
     * Start a flame experiment with the specified parameters.
     *
     * This generates a digital acoustic signal with the given characteristics.
     * It does NOT interact with physical flames — it's a signal generator only.
     *
     * @param name Experiment name
     * @param frequencyHz Signal frequency in Hz
     * @param amplitude Signal amplitude (0.0-1.0)
     * @param waveform Signal waveform type
     * @param durationSeconds Duration in seconds
     */
    fun startFlameExperiment(
        name: String,
        frequencyHz: Double,
        amplitude: Float,
        waveform: Waveform,
        durationSeconds: Int
    ) {
        Log.i(TAG, "Starting flame experiment: $name | ${frequencyHz}Hz | amp=$amplitude | ${waveform.name} | ${durationSeconds}s")

        val steps = durationSeconds // 1 step per second
        val dwellMs = 1000 // 1 second per step

        val experiment = AcousticExperiment(
            name = name,
            type = ExperimentType.FREQUENCY_RESPONSE,
            variables = listOf(
                ExperimentVariable("frequency", VariableType.FREQUENCY, "Hz", frequencyHz, frequencyHz, 0.0, frequencyHz),
                ExperimentVariable("amplitude", VariableType.AMPLITUDE, "x", amplitude.toDouble(), amplitude.toDouble(), 0.0, amplitude.toDouble())
            ),
            signalConfig = SignalConfig(
                frequencyHz = frequencyHz,
                amplitude = amplitude,
                waveform = waveform
            ),
            stepCount = steps,
            dwellMs = dwellMs
        )

        viewModelScope.launch {
            try {
                runner.startExperiment(experiment)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start flame experiment: ${e.message}", e)
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun startDistortionExperiment(name: String = "Distortion ${System.currentTimeMillis()}") {
        Log.i(TAG, "Starting distortion experiment: $name")
        val experiment = AcousticExperiment(
            name = name,
            type = ExperimentType.DISTORTION_PROFILE,
            variables = listOf(
                ExperimentVariable("amplitude", VariableType.AMPLITUDE, "x", 0.1, 1.0, 0.1, 0.1)
            ),
            signalConfig = SignalConfig(frequencyHz = 1000.0, amplitude = 0.1f, waveform = Waveform.SINE),
            stepCount = 10,
            dwellMs = 500
        )
        viewModelScope.launch {
            try {
                runner.startExperiment(experiment)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start distortion: ${e.message}", e)
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun pauseExperiment() {
        Log.i(TAG, "Pausing experiment")
        runner.pauseExperiment()
    }

    fun resumeExperiment() {
        Log.i(TAG, "Resuming experiment")
        runner.resumeExperiment()
    }

    fun abortExperiment() {
        Log.w(TAG, "Aborting experiment")
        runner.abortExperiment()
    }

    fun selectExperiment(experiment: AcousticExperiment) {
        Log.d(TAG, "Selected experiment: ${experiment.name}")
        _state.update { it.copy(selectedExperiment = experiment) }
    }

    fun deleteExperiment(id: String) {
        Log.i(TAG, "Deleting experiment: $id")
        viewModelScope.launch {
            repository.deleteExperiment(id)
            _state.update { it.copy(selectedExperiment = null) }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun checkFlameSafety(config: FlameExperimentConfig): FlameSafetyState {
        return flameSafetyController.checkSafety(config)
    }

    fun markFlameExperimentCompleted() {
        flameSafetyController.markExperimentCompleted()
    }
}
