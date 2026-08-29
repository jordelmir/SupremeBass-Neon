package com.supremecorp.bass.ui.device

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.supremecorp.bass.core.logging.AppLogger
import com.supremecorp.bass.data.device.DeviceRepository
import com.supremecorp.bass.data.device.DeviceDatabase
import com.supremecorp.bass.dsp.SweepEngine
import com.supremecorp.bass.dsp.SweepPlan
import com.supremecorp.bass.dsp.SweepState
import com.supremecorp.bass.dsp.SweepType
import com.supremecorp.bass.domain.model.*
import com.supremecorp.bass.signal.SignalEngine
import com.supremecorp.bass.signal.SignalEngineState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MeasuredPoint(
    val frequencyHz: Double,
    val requestedAmplitude: Float,
    val peak: Float,
    val rms: Float,
    val authority: MeasurementAuthority = MeasurementAuthority.DIGITAL
)

data class DeviceLabState(
    val isCharacterizing: Boolean = false,
    val currentFrequency: Double = 0.0,
    val progress: Float = 0f,
    val currentStep: Int = 0,
    val totalSteps: Int = 0,
    val measuredPoints: List<MeasuredPoint> = emptyList(),
    val engineState: SignalEngineState = SignalEngineState.Idle,
    val savedProfiles: List<DeviceAcousticProfile> = emptyList(),
    val selectedProfile: DeviceAcousticProfile? = null,
    val error: String? = null
)

class DeviceLabViewModel(application: Application) : AndroidViewModel(application) {

    private companion object {
        const val TAG = "SupremeBass_DeviceLab"
    }

    private val _state = MutableStateFlow(DeviceLabState())
    val state: StateFlow<DeviceLabState> = _state.asStateFlow()

    private val signalEngine = SignalEngine(application)
    private val sweepEngine = SweepEngine()
    private val deviceRepository: DeviceRepository

    private var sweepJob: Job? = null
    private var characterizationPlan: SweepPlan? = null

    init {
        Log.i(TAG, "DeviceLabViewModel initialized")
        val db = DeviceDatabase.getInstance(application)
        deviceRepository = DeviceRepository(
            db.deviceAcousticProfileDao(),
            db.frequencyResponsePointDao(),
            db.deviceInfoDao()
        )
        loadProfiles()
    }

    private fun loadProfiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val profiles = deviceRepository.getAllProfilesWithPoints()
            Log.d(TAG, "Loaded ${profiles.size} device profiles")
            _state.value = _state.value.copy(savedProfiles = profiles)
        }
    }

    fun startCharacterization() {
        if (_state.value.isCharacterizing) {
            Log.w(TAG, "Characterization already in progress")
            return
        }

        Log.i(TAG, "Starting characterization: 20-20000Hz, 30 steps, 300ms dwell, 0.5 amp")

        val plan = SweepPlan(
            startHz = 20.0,
            endHz = 20_000.0,
            steps = 30,
            dwellMs = 300,
            amplitude = 0.5f,
            waveform = Waveform.SINE,
            sweepType = SweepType.LOGARITHMIC
        )
        characterizationPlan = plan

        sweepEngine.configure(48_000)
        sweepEngine.start(plan, 48_000)

        _state.value = _state.value.copy(
            isCharacterizing = true,
            progress = 0f,
            currentStep = 0,
            totalSteps = plan.steps,
            measuredPoints = emptyList(),
            error = null
        )

        sweepJob?.cancel()
        sweepJob = viewModelScope.launch(Dispatchers.Default) {
            val audioConfig = AudioOutputConfig(sampleRate = 48_000)
            val buffer = FloatArray(1024)

            val started = signalEngine.start(
                SignalConfig(
                    frequencyHz = plan.startHz,
                    amplitude = plan.amplitude,
                    waveform = plan.waveform
                ),
                audioConfig
            )

            if (!started) {
                Log.e(TAG, "Failed to start signal engine for characterization")
                _state.value = _state.value.copy(
                    isCharacterizing = false,
                    error = "Failed to start signal engine"
                )
                return@launch
            }

            Log.i(TAG, "Signal engine started, beginning sweep")

            while (true) {
                val sweepState = sweepEngine.render(buffer, 1024, 48_000)
                val currentConfig = plan.getStepConfig(sweepEngine.getCurrentStep())
                val telemetry = signalEngine.getTelemetry()

                when (sweepState) {
                    is SweepState.StepComplete -> {
                        val measuredPoint = MeasuredPoint(
                            frequencyHz = currentConfig.frequencyHz,
                            requestedAmplitude = currentConfig.amplitude,
                            peak = telemetry?.peak ?: 0f,
                            rms = telemetry?.rms ?: 0f
                        )

                        Log.d(TAG, "Step ${sweepState.step + 1}/${plan.steps}: freq=${currentConfig.frequencyHz}Hz peak=${telemetry?.peak} rms=${telemetry?.rms}")

                        _state.value = _state.value.copy(
                            currentFrequency = currentConfig.frequencyHz,
                            currentStep = sweepState.step + 1,
                            progress = sweepState.progress,
                            measuredPoints = _state.value.measuredPoints + measuredPoint
                        )
                    }
                    is SweepState.Complete -> {
                        Log.i(TAG, "Sweep complete: ${_state.value.measuredPoints.size} points measured")
                        signalEngine.stop()
                        saveCharacterization()
                        _state.value = _state.value.copy(
                            isCharacterizing = false,
                            progress = 1f
                        )
                        loadProfiles()
                        break
                    }
                }
            }
        }
    }

    fun stopCharacterization() {
        Log.i(TAG, "Stopping characterization")
        sweepJob?.cancel()
        sweepJob = null
        sweepEngine.stop()
        signalEngine.stop()
        _state.value = _state.value.copy(isCharacterizing = false)
    }

    private fun saveCharacterization() {
        val state = _state.value
        val plan = characterizationPlan ?: return

        val points = state.measuredPoints.map { mp ->
            FrequencyResponsePoint(
                frequencyHz = mp.frequencyHz,
                requestedAmplitude = mp.requestedAmplitude,
                measuredMetric = mp.peak.toDouble(),
                metricType = AcousticMetricType.FREQUENCY_RESPONSE,
                authority = mp.authority
            )
        }

        val profile = DeviceAcousticProfile(
            manufacturer = android.os.Build.MANUFACTURER,
            model = android.os.Build.MODEL,
            androidDevice = android.os.Build.DEVICE,
            outputRoute = OutputRoute.BUILT_IN_SPEAKER,
            supportedSampleRates = setOf(44_100, 48_000, 96_000),
            measuredResponses = points,
            authority = MeasurementAuthority.DIGITAL
        )

        viewModelScope.launch(Dispatchers.IO) {
            deviceRepository.saveProfile(profile)
            AppLogger.i("DeviceLab", "Saved characterization profile: ${profile.id}")
        }
    }

    fun deleteProfile(profile: DeviceAcousticProfile) {
        Log.i(TAG, "Deleting profile: ${profile.id}")
        viewModelScope.launch(Dispatchers.IO) {
            deviceRepository.deleteProfile(profile.id)
            loadProfiles()
        }
    }

    fun selectProfile(profile: DeviceAcousticProfile?) {
        _state.value = _state.value.copy(selectedProfile = profile)
    }

    override fun onCleared() {
        Log.i(TAG, "DeviceLabViewModel cleared")
        super.onCleared()
        sweepJob?.cancel()
        signalEngine.release()
    }
}