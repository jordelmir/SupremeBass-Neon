package com.supremecorp.bass.ui.device

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.supremecorp.bass.audio.input.AudioInputProcessor
import com.supremecorp.bass.core.logging.AppLogger
import com.supremecorp.bass.data.device.DeviceRepository
import com.supremecorp.bass.data.device.DeviceDatabase
import com.supremecorp.bass.domain.model.*
import com.supremecorp.bass.dsp.*
import com.supremecorp.bass.dsp.Limiter
import com.supremecorp.bass.dsp.SweepEngine
import com.supremecorp.bass.dsp.SweepPlan
import com.supremecorp.bass.dsp.SweepState
import com.supremecorp.bass.dsp.SweepType
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
    val savedProfiles: List<DeviceAcousticProfile> = emptyList(),
    val selectedProfile: DeviceAcousticProfile? = null,
    val error: String? = null,
    // RT60 Measurement
    val isMeasuringRT60: Boolean = false,
    val rt60Progress: Float = 0f,
    val rt60Result: RT60Result? = null,
    // SPL Measurement
    val isMeasuringSPL: Boolean = false,
    val splProgress: Float = 0f,
    val splResult: SPLResult? = null,
    // THD Measurement
    val isMeasuringTHD: Boolean = false,
    val thdProgress: Float = 0f,
    val thdResult: THDResult? = null
)

data class RT60Result(
    val rt60Seconds: Double,
    val edtSeconds: Double,
    val roomQuality: String,
    val confidence: Double
)

data class SPLResult(
    val peakDbFS: Double,
    val rmsDbFS: Double,
    val integratedLoudness: Double,
    val dynamicRange: Double
)

data class THDResult(
    val thdPercent: Double,
    val thdPlusNPercent: Double,
    val fundamentalHz: Double,
    val qualityRating: String
)

class DeviceLabViewModel(application: Application) : AndroidViewModel(application) {

    private companion object {
        const val TAG = "SupremeBass_DeviceLab"
    }

    private val _state = MutableStateFlow(DeviceLabState())
    val state: StateFlow<DeviceLabState> = _state.asStateFlow()

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
            val sampleRate = 48_000
            val audioConfig = AudioOutputConfig(sampleRate = sampleRate)
            val backend = com.supremecorp.bass.audio.backend.AndroidAudioTrackBackend()
            val limiter = com.supremecorp.bass.dsp.Limiter()
            val buffer = FloatArray(1024)

            val backendResult = backend.start(audioConfig)
            if (backendResult is AudioBackendResult.Failure) {
                Log.e(TAG, "Failed to start audio backend for characterization")
                _state.value = _state.value.copy(
                    isCharacterizing = false,
                    error = "Failed to start audio backend"
                )
                return@launch
            }

            Log.i(TAG, "Audio backend started, beginning sweep")

            while (true) {
                val sweepState = sweepEngine.render(buffer, 1024, sampleRate)
                val currentConfig = plan.getStepConfig(sweepEngine.getCurrentStep())

                // Apply limiter to prevent clipping
                limiter.process(buffer, 1024)

                // Actually write the sweep buffer to the audio output
                val writeResult = backend.write(buffer, 1024)
                if (writeResult is AudioBackendResult.Failure) {
                    Log.e(TAG, "Write failed during sweep")
                    backend.stop()
                    _state.value = _state.value.copy(
                        isCharacterizing = false,
                        error = "Audio write failed"
                    )
                    return@launch
                }

                // Read back peak/rms from limiter for telemetry
                val measuredPoint = MeasuredPoint(
                    frequencyHz = currentConfig.frequencyHz,
                    requestedAmplitude = currentConfig.amplitude,
                    peak = limiter.peak,
                    rms = limiter.rms
                )

                when (sweepState) {
                    is SweepState.StepComplete -> {
                        Log.d(TAG, "Step ${sweepState.step + 1}/${plan.steps}: freq=${currentConfig.frequencyHz}Hz peak=${limiter.peak} rms=${limiter.rms}")

                        _state.value = _state.value.copy(
                            currentFrequency = currentConfig.frequencyHz,
                            currentStep = sweepState.step + 1,
                            progress = sweepState.progress,
                            measuredPoints = _state.value.measuredPoints + measuredPoint
                        )
                    }
                    is SweepState.Complete -> {
                        Log.i(TAG, "Sweep complete: ${_state.value.measuredPoints.size} points measured")
                        backend.stop()
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

    // ── RT60 Measurement ──

    fun startRT60Measurement() {
        if (_state.value.isMeasuringRT60) return

        val audioInput = AudioInputProcessor(getApplication())
        if (!audioInput.hasPermission()) {
            _state.value = _state.value.copy(error = "RECORD_AUDIO permission required")
            return
        }

        _state.value = _state.value.copy(
            isMeasuringRT60 = true,
            rt60Progress = 0f,
            rt60Result = null,
            error = null
        )

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val sampleRate = 48000

                // STEP 1: Start microphone FIRST (captures entire event)
                audioInput.start()
                kotlinx.coroutines.delay(100) // Preroll — let mic stabilize

                // STEP 2: Record pre-impulse baseline
                val baselineDuration = 0.5 // 500ms baseline
                val baselineSamples = (sampleRate * baselineDuration).toInt()
                val baseline = FloatArray(baselineSamples)
                var samplesRead = 0
                while (samplesRead < baselineSamples && audioInput.isRecording()) {
                    val buffer = FloatArray(1024)
                    val read = audioInput.read(buffer)
                    if (read > 0) {
                        val copyLength = minOf(read, baselineSamples - samplesRead)
                        System.arraycopy(buffer, 0, baseline, samplesRead, copyLength)
                        samplesRead += copyLength
                    }
                }

                // STEP 3: Play impulse WHILE mic is still recording
                val impulseDuration = 0.1 // 100ms impulse
                val impulseSamples = (sampleRate * impulseDuration).toInt()
                val impulse = FloatArray(impulseSamples) { i ->
                    if (i < impulseSamples / 10) {
                        (Math.random() * 2 - 1).toFloat()
                    } else {
                        0f
                    }
                }

                val backend = com.supremecorp.bass.audio.backend.AndroidAudioTrackBackend()
                val audioConfig = AudioOutputConfig(sampleRate = sampleRate)
                backend.start(audioConfig)
                backend.write(impulse, impulseSamples)

                // STEP 4: Continue recording decay (mic is still running)
                val decayDuration = 3.0 // 3 seconds of decay
                val decaySamples = (sampleRate * decayDuration).toInt()
                val fullRecording = FloatArray(baselineSamples + decaySamples)
                System.arraycopy(baseline, 0, fullRecording, 0, baselineSamples)
                samplesRead = baselineSamples
                var decayRead = 0

                while (decayRead < decaySamples && audioInput.isRecording()) {
                    val buffer = FloatArray(1024)
                    val read = audioInput.read(buffer)
                    if (read > 0) {
                        val copyLength = minOf(read, decaySamples - decayRead)
                        System.arraycopy(buffer, 0, fullRecording, samplesRead, copyLength)
                        samplesRead += copyLength
                        decayRead += copyLength
                        _state.value = _state.value.copy(
                            rt60Progress = (baselineSamples + decayRead).toFloat() / fullRecording.size
                        )
                    }
                }

                // STEP 5: Stop everything
                backend.stop()
                audioInput.stop()

                // STEP 6: Analyze RT60 from the full recording (baseline + impulse + decay)
                val rt60Estimator = RT60Estimator()
                val analysis = rt60Estimator.estimateFromImpulseResponse(fullRecording, sampleRate)

                _state.value = _state.value.copy(
                    isMeasuringRT60 = false,
                    rt60Progress = 1f,
                    rt60Result = RT60Result(
                        rt60Seconds = analysis.rt60Ms / 1000.0,
                        edtSeconds = analysis.earlyDecayMs / 1000.0,
                        roomQuality = if (analysis.confidence > 0.7) "Good" else "Fair",
                        confidence = analysis.confidence
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "RT60 measurement failed", e)
                _state.value = _state.value.copy(
                    isMeasuringRT60 = false,
                    error = "RT60 measurement failed: ${e.message}"
                )
            }
        }
    }

    fun stopRT60Measurement() {
        _state.value = _state.value.copy(isMeasuringRT60 = false)
    }

    // ── SPL Measurement ──

    fun startSPLMeasurement() {
        if (_state.value.isMeasuringSPL) return

        val audioInput = AudioInputProcessor(getApplication())
        if (!audioInput.hasPermission()) {
            _state.value = _state.value.copy(error = "RECORD_AUDIO permission required")
            return
        }

        _state.value = _state.value.copy(
            isMeasuringSPL = true,
            splProgress = 0f,
            splResult = null,
            error = null
        )

        viewModelScope.launch(Dispatchers.Default) {
            try {
                audioInput.start()
                val sampleRate = audioInput.getSampleRate()
                val duration = 5.0 // 5 seconds of recording
                val totalSamples = (sampleRate * duration).toInt()
                val recording = FloatArray(totalSamples)
                var samplesRead = 0

                while (samplesRead < totalSamples && audioInput.isRecording()) {
                    val buffer = FloatArray(1024)
                    val read = audioInput.read(buffer)
                    if (read > 0) {
                        val copyLength = minOf(read, totalSamples - samplesRead)
                        System.arraycopy(buffer, 0, recording, samplesRead, copyLength)
                        samplesRead += copyLength
                        _state.value = _state.value.copy(splProgress = samplesRead.toFloat() / totalSamples)
                    }
                }
                audioInput.stop()

                // Analyze SPL
                val splEstimator = SPLEstimator()
                val analysis = splEstimator.estimate(recording, sampleRate)

                _state.value = _state.value.copy(
                    isMeasuringSPL = false,
                    splProgress = 1f,
                    splResult = SPLResult(
                        peakDbFS = analysis.peakDb,
                        rmsDbFS = analysis.rmsDb,
                        integratedLoudness = analysis.loudnessLUFS,
                        dynamicRange = analysis.dynamicRangeDb
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "SPL measurement failed", e)
                _state.value = _state.value.copy(
                    isMeasuringSPL = false,
                    error = "SPL measurement failed: ${e.message}"
                )
            }
        }
    }

    fun stopSPLMeasurement() {
        _state.value = _state.value.copy(isMeasuringSPL = false)
    }

    // ── THD Measurement ──

    fun startTHDMeasurement() {
        if (_state.value.isMeasuringTHD) return

        val audioInput = AudioInputProcessor(getApplication())
        if (!audioInput.hasPermission()) {
            _state.value = _state.value.copy(error = "RECORD_AUDIO permission required")
            return
        }

        _state.value = _state.value.copy(
            isMeasuringTHD = true,
            thdProgress = 0f,
            thdResult = null,
            error = null
        )

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val sampleRate = 48000
                val testFrequency = 1000.0 // 1kHz test tone

                // STEP 1: Start microphone FIRST
                audioInput.start()
                kotlinx.coroutines.delay(100) // Preroll — let mic stabilize

                // STEP 2: Record pre-stimulus baseline (100ms)
                val baselineDuration = 0.1
                val baselineSamples = (sampleRate * baselineDuration).toInt()
                val baseline = FloatArray(baselineSamples)
                var samplesRead = 0
                while (samplesRead < baselineSamples && audioInput.isRecording()) {
                    val buffer = FloatArray(1024)
                    val read = audioInput.read(buffer)
                    if (read > 0) {
                        val copyLength = minOf(read, baselineSamples - samplesRead)
                        System.arraycopy(buffer, 0, baseline, samplesRead, copyLength)
                        samplesRead += copyLength
                    }
                }

                // STEP 3: Play test tone WHILE mic is still recording
                val toneDuration = 2.0
                val toneSamples = (sampleRate * toneDuration).toInt()
                val testTone = FloatArray(toneSamples) { i ->
                    (0.5 * kotlin.math.sin(2.0 * Math.PI * testFrequency * i / sampleRate)).toFloat()
                }

                val backend = com.supremecorp.bass.audio.backend.AndroidAudioTrackBackend()
                val audioConfig = AudioOutputConfig(sampleRate = sampleRate)
                backend.start(audioConfig)

                // Write tone in chunks to avoid blocking too long
                val chunkSize = 4096
                var toneOffset = 0
                while (toneOffset < toneSamples) {
                    val remaining = toneSamples - toneOffset
                    val writeSize = minOf(chunkSize, remaining)
                    val chunk = FloatArray(writeSize)
                    System.arraycopy(testTone, toneOffset, chunk, 0, writeSize)
                    backend.write(chunk, writeSize)
                    toneOffset += writeSize

                    // Read mic data simultaneously
                    val micBuffer = FloatArray(1024)
                    val read = audioInput.read(micBuffer)
                    if (read > 0) {
                        // Store for later analysis
                    }
                }

                // STEP 4: Record post-stimulus decay (500ms) for settling
                val settlingDuration = 0.5
                val settlingSamples = (sampleRate * settlingDuration).toInt()

                // STEP 5: Build full recording (baseline + tone + settling)
                val totalSamples = baselineSamples + toneSamples + settlingSamples
                val fullRecording = FloatArray(totalSamples)
                System.arraycopy(baseline, 0, fullRecording, 0, baselineSamples)
                samplesRead = baselineSamples

                // Read the tone portion from mic
                var toneRead = 0
                while (toneRead < toneSamples && audioInput.isRecording()) {
                    val buffer = FloatArray(1024)
                    val read = audioInput.read(buffer)
                    if (read > 0) {
                        val copyLength = minOf(read, toneSamples - toneRead)
                        System.arraycopy(buffer, 0, fullRecording, samplesRead, copyLength)
                        samplesRead += copyLength
                        toneRead += copyLength
                        _state.value = _state.value.copy(
                            thdProgress = (baselineSamples + toneRead).toFloat() / totalSamples * 0.7f
                        )
                    }
                }

                // Read settling portion
                var settlingRead = 0
                while (settlingRead < settlingSamples && audioInput.isRecording()) {
                    val buffer = FloatArray(1024)
                    val read = audioInput.read(buffer)
                    if (read > 0) {
                        val copyLength = minOf(read, settlingSamples - settlingRead)
                        System.arraycopy(buffer, 0, fullRecording, samplesRead, copyLength)
                        samplesRead += copyLength
                        settlingRead += copyLength
                        _state.value = _state.value.copy(
                            thdProgress = (baselineSamples + toneSamples + settlingRead).toFloat() / totalSamples * 0.7f + 0.3f
                        )
                    }
                }

                // STEP 6: Stop everything
                backend.stop()
                audioInput.stop()

                // STEP 7: Analyze THD from the tone portion only (skip baseline)
                val toneStartSample = baselineSamples
                val toneRecording = fullRecording.copyOfRange(toneStartSample, toneStartSample + toneSamples)

                val thdAnalyzer = THDAnalyzer()
                val analysis = thdAnalyzer.analyze(toneRecording, testFrequency, sampleRate)

                _state.value = _state.value.copy(
                    isMeasuringTHD = false,
                    thdProgress = 1f,
                    thdResult = THDResult(
                        thdPercent = analysis.thdPercent,
                        thdPlusNPercent = analysis.thdNPercent,
                        fundamentalHz = analysis.fundamentalHz,
                        qualityRating = when {
                            analysis.thdPercent < 1.0 -> "Excellent"
                            analysis.thdPercent < 3.0 -> "Good"
                            analysis.thdPercent < 5.0 -> "Fair"
                            else -> "Poor"
                        }
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "THD measurement failed", e)
                _state.value = _state.value.copy(
                    isMeasuringTHD = false,
                    error = "THD measurement failed: ${e.message}"
                )
            }
        }
    }

    fun stopTHDMeasurement() {
        _state.value = _state.value.copy(isMeasuringTHD = false)
    }

    override fun onCleared() {
        Log.i(TAG, "DeviceLabViewModel cleared")
        super.onCleared()
        sweepJob?.cancel()
    }
}