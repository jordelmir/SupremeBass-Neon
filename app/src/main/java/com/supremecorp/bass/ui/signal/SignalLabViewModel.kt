package com.supremecorp.bass.ui.signal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.supremecorp.bass.audio.input.AudioInputProcessor
import com.supremecorp.bass.core.logging.AppLogger
import com.supremecorp.bass.domain.model.*
import com.supremecorp.bass.dsp.FFT
import com.supremecorp.bass.dsp.ParametricEQ
import com.supremecorp.bass.dsp.SweepEngine
import com.supremecorp.bass.dsp.SweepPlan
import com.supremecorp.bass.dsp.SweepState
import com.supremecorp.bass.dsp.SweepType
import com.supremecorp.bass.infrastructure.export.TelemetryExporter
import com.supremecorp.bass.signal.SignalEngine
import com.supremecorp.bass.signal.SignalEngineState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SignalLabState(
    val waveform: Waveform = Waveform.SINE,
    val frequencyHz: Double = 440.0,
    val amplitude: Float = 0.5f,
    val durationMs: Long = 0,
    val chirpEndHz: Double = 1000.0,
    val noiseLowHz: Double = 100.0,
    val noiseHighHz: Double = 2000.0,
    val sampleRate: Int = 48_000,
    val engineState: SignalEngineState = SignalEngineState.Idle,
    val sweepProgress: Float = 0f,
    val currentSweepStep: Int = 0,
    val totalSweepSteps: Int = 0,
    val telemetry: SignalTelemetry? = null,
    val exportedJson: String? = null,
    // DSP Controls
    val dspState: DSPControlsState = DSPControlsState(),
    // Spectrum data
    val spectrumData: FloatArray = FloatArray(128) { -80f }
)

class SignalLabViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(SignalLabState())
    val state: StateFlow<SignalLabState> = _state.asStateFlow()

    private val signalEngine = SignalEngine(application)
    private val sweepEngine = SweepEngine()
    private val exporter = TelemetryExporter()
    private var sweepJob: Job? = null

    // Microphone input for spectrum analysis
    private val audioInputProcessor = AudioInputProcessor(application)
    private val fft = FFT(1024)
    private var micJob: Job? = null

    fun updateWaveform(waveform: Waveform) {
        _state.value = _state.value.copy(waveform = waveform)
    }

    fun updateFrequency(freq: Double) {
        _state.value = _state.value.copy(frequencyHz = freq)
    }

    fun updateAmplitude(amp: Float) {
        _state.value = _state.value.copy(amplitude = amp)
    }

    fun updateDuration(ms: Long) {
        _state.value = _state.value.copy(durationMs = ms)
    }

    fun updateChirpEnd(endHz: Double) {
        _state.value = _state.value.copy(chirpEndHz = endHz)
    }

    fun updateNoiseLow(hz: Double) {
        _state.value = _state.value.copy(noiseLowHz = hz)
    }

    fun updateNoiseHigh(hz: Double) {
        _state.value = _state.value.copy(noiseHighHz = hz)
    }

    fun updateSampleRate(rate: Int) {
        _state.value = _state.value.copy(sampleRate = rate)
    }

    // ── DSP Controls ──

    fun setBassBoost(db: Float) {
        signalEngine.dsp.bassBoost.setBoost(db)
        _state.value = _state.value.copy(
            dspState = _state.value.dspState.copy(bassBoostDb = db)
        )
    }

    fun setBassCutoff(hz: Double) {
        signalEngine.dsp.bassBoost.setCutoffFrequency(hz)
        _state.value = _state.value.copy(
            dspState = _state.value.dspState.copy(bassCutoffHz = hz)
        )
    }

    fun setBassEnabled(enabled: Boolean) {
        signalEngine.dsp.bassBoost.setEnabled(enabled)
        _state.value = _state.value.copy(
            dspState = _state.value.dspState.copy(bassEnabled = enabled)
        )
    }

    fun setEQBandGain(bandIndex: Int, gainDb: Double) {
        signalEngine.dsp.eq.setBandGain(bandIndex, gainDb)
        val newBands = _state.value.dspState.eqBands.toMutableList()
        if (bandIndex in newBands.indices) {
            newBands[bandIndex] = gainDb
        }
        _state.value = _state.value.copy(
            dspState = _state.value.dspState.copy(eqBands = newBands)
        )
    }

    fun setEQEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(
            dspState = _state.value.dspState.copy(eqEnabled = enabled)
        )
    }

    fun applyEQPreset(preset: ParametricEQ.EQPreset) {
        signalEngine.dsp.eq.applyPreset(preset)
        _state.value = _state.value.copy(
            dspState = _state.value.dspState.copy(
                eqPreset = preset,
                eqBands = signalEngine.dsp.eq.getBands().map { it.gainDb }
            )
        )
    }

    fun setVirtualizerWidth(width: Float) {
        signalEngine.dsp.virtualizer.setWidth(width)
        _state.value = _state.value.copy(
            dspState = _state.value.dspState.copy(virtualizerWidth = width)
        )
    }

    fun setVirtualizerCrossfeed(cf: Float) {
        signalEngine.dsp.virtualizer.setCrossfeed(cf)
        _state.value = _state.value.copy(
            dspState = _state.value.dspState.copy(virtualizerCrossfeed = cf)
        )
    }

    fun setVirtualizerEnabled(enabled: Boolean) {
        signalEngine.dsp.virtualizer.setEnabled(enabled)
        _state.value = _state.value.copy(
            dspState = _state.value.dspState.copy(virtualizerEnabled = enabled)
        )
    }

    // ── Microphone Input ──

    fun startMicInput() {
        if (!audioInputProcessor.hasPermission()) {
            AppLogger.w(TAG, "RECORD_AUDIO permission not granted")
            return
        }

        if (audioInputProcessor.isRecording()) return

        audioInputProcessor.onAudioData = { data, samplesRead ->
            processMicAudio(data, samplesRead)
        }

        if (audioInputProcessor.start()) {
            micJob = viewModelScope.launch(Dispatchers.Default) {
                val buffer = FloatArray(1024)
                while (audioInputProcessor.isRecording() && isActive) {
                    audioInputProcessor.read(buffer)
                }
            }
        }
    }

    fun stopMicInput() {
        audioInputProcessor.stop()
        micJob?.cancel()
        micJob = null
        _state.value = _state.value.copy(
            spectrumData = FloatArray(128) { -80f }
        )
    }

    private fun processMicAudio(data: FloatArray, samplesRead: Int) {
        val fftSize = fft.getSize()

        // Apply Hanning window and convert to DoubleArray
        val real = DoubleArray(fftSize)
        val imag = DoubleArray(fftSize)
        for (i in 0 until minOf(samplesRead, fftSize)) {
            val window = 0.5 * (1.0 - kotlin.math.cos(2.0 * Math.PI * i / (fftSize - 1)))
            real[i] = data[i].toDouble() * window
        }

        // Compute FFT
        fft.forward(real, imag)

        // Compute magnitudes in dB
        val magnitudes = FloatArray(fftSize / 2) { i ->
            val re = real[i]
            val im = imag[i]
            val mag = kotlin.math.sqrt(re * re + im * im)
            val db = if (mag > 1e-10) (20.0 * kotlin.math.log10(mag)).toFloat() else -80.0f
            db.coerceIn(-80.0f, 0.0f)
        }

        _state.value = _state.value.copy(spectrumData = magnitudes)
    }

    fun startSignal() {
        val s = _state.value
        val config = SignalConfig(
            frequencyHz = s.frequencyHz,
            amplitude = s.amplitude,
            waveform = s.waveform,
            durationMs = s.durationMs,
            chirpEndHz = s.chirpEndHz,
            noiseLowHz = s.noiseLowHz,
            noiseHighHz = s.noiseHighHz
        )
        val audioConfig = AudioOutputConfig(sampleRate = s.sampleRate)

        viewModelScope.launch(Dispatchers.Default) {
            val started = signalEngine.start(config, audioConfig)
            if (started) {
                _state.value = _state.value.copy(engineState = signalEngine.getState())
                pollEngineState()
            } else {
                _state.value = _state.value.copy(engineState = signalEngine.getState())
            }
        }
    }

    fun startSweep() {
        val s = _state.value
        val plan = SweepPlan(
            startHz = 20.0,
            endHz = 20_000.0,
            steps = 20,
            dwellMs = 200,
            amplitude = s.amplitude,
            waveform = s.waveform,
            sweepType = SweepType.LOGARITHMIC
        )

        sweepEngine.configure(s.sampleRate)
        sweepEngine.start(plan, s.sampleRate)
        _state.value = _state.value.copy(
            totalSweepSteps = plan.steps,
            currentSweepStep = 0,
            sweepProgress = 0f,
            engineState = SignalEngineState.Running(
                com.supremecorp.bass.signal.SignalSession(
                    id = "sweep-${System.currentTimeMillis()}",
                    config = SignalConfig(
                        frequencyHz = plan.startHz,
                        amplitude = plan.amplitude,
                        waveform = plan.waveform
                    ),
                    audioConfig = AudioOutputConfig(sampleRate = s.sampleRate),
                    startTime = System.currentTimeMillis(),
                    route = OutputRoute.BUILT_IN_SPEAKER
                )
            )
        )

        sweepJob?.cancel()
        sweepJob = viewModelScope.launch(Dispatchers.Default) {
            val audioConfig = AudioOutputConfig(sampleRate = s.sampleRate)
            val backend = com.supremecorp.bass.audio.backend.AndroidAudioTrackBackend()
            val dspChain = com.supremecorp.bass.dsp.AudioDSPChain()

            // Configure DSP with current settings
            dspChain.configure(s.sampleRate)
            dspChain.bassBoost.setBoost(s.dspState.bassBoostDb)
            s.dspState.eqBands.forEachIndexed { index, gain ->
                dspChain.eq.setBandGain(index, gain)
            }
            dspChain.virtualizer.setWidth(s.dspState.virtualizerWidth)

            // Start audio backend directly (NOT signalEngine — we render sweep ourselves)
            val backendResult = backend.start(audioConfig)
            if (backendResult is AudioBackendResult.Failure) {
                AppLogger.e("SignalLabViewModel", "Backend start failed for sweep")
                _state.value = _state.value.copy(
                    engineState = SignalEngineState.Idle,
                    sweepProgress = 0f
                )
                return@launch
            }

            val channelCount = audioConfig.channelCount
            val bufferFrames = 1024
            val buffer = FloatArray(bufferFrames * channelCount)

            AppLogger.i("SignalLabViewModel", "Sweep started: ${plan.startHz}-${plan.endHz}Hz, ${plan.steps} steps, ${channelCount}ch")

            while (true) {
                val sweepState = sweepEngine.render(buffer, bufferFrames, s.sampleRate)

                // Process through DSP chain — use correct method for channel count
                if (channelCount == 2) {
                    dspChain.processStereo(buffer, bufferFrames)
                } else {
                    dspChain.processMono(buffer, bufferFrames)
                }

                // Write to audio backend
                val writeResult = backend.write(buffer, bufferFrames)
                if (writeResult is AudioBackendResult.Failure) {
                    AppLogger.e("SignalLabViewModel", "Write failed during sweep")
                    break
                }

                when (sweepState) {
                    is SweepState.Complete -> break
                    is SweepState.StepComplete -> {
                        _state.value = _state.value.copy(
                            currentSweepStep = sweepState.step,
                            sweepProgress = sweepState.progress
                        )
                    }
                }
            }

            backend.stop()
            AppLogger.i("SignalLabViewModel", "Sweep complete")
            _state.value = _state.value.copy(
                engineState = SignalEngineState.Idle,
                sweepProgress = 1f
            )
        }
    }

    fun stopSignal() {
        sweepJob?.cancel()
        sweepJob = null
        sweepEngine.stop()
        signalEngine.stop()
        _state.value = _state.value.copy(
            engineState = SignalEngineState.Idle,
            sweepProgress = 0f,
            currentSweepStep = 0
        )
    }

    fun emergencyStop() {
        sweepJob?.cancel()
        sweepJob = null
        sweepEngine.stop()
        signalEngine.emergencyStop()
        _state.value = _state.value.copy(
            engineState = SignalEngineState.Idle,
            sweepProgress = 0f,
            currentSweepStep = 0
        )
    }

    fun exportTelemetry() {
        val telemetry = signalEngine.getTelemetry()
        if (telemetry != null) {
            val json = exporter.exportSession(telemetry)
            _state.value = _state.value.copy(
                telemetry = telemetry,
                exportedJson = json
            )
        }
    }

    private fun pollEngineState() {
        viewModelScope.launch(Dispatchers.Default) {
            while (signalEngine.getState() is SignalEngineState.Running) {
                kotlinx.coroutines.delay(100)
                val stats = signalEngine.dsp.getStats()
                _state.value = _state.value.copy(
                    engineState = signalEngine.getState(),
                    telemetry = signalEngine.getTelemetry(),
                    dspState = _state.value.dspState.copy(
                        dspProcessTimeUs = stats.processTimeUs,
                        clippedSamples = stats.clippedSamples
                    )
                )
            }
            _state.value = _state.value.copy(engineState = signalEngine.getState())
        }
    }

    override fun onCleared() {
        super.onCleared()
        sweepJob?.cancel()
        micJob?.cancel()
        audioInputProcessor.stop()
        signalEngine.release()
    }

    companion object {
        private const val TAG = "SignalLabViewModel"
    }
}
