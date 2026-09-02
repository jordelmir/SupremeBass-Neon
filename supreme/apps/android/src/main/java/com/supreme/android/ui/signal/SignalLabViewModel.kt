package com.supreme.android.ui.signal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.supreme.android.core.logging.AppLogger
import com.supreme.android.domain.model.*
import com.supreme.android.dsp.ParametricEQ
import com.supreme.android.dsp.SweepEngine
import com.supreme.android.dsp.SweepPlan
import com.supreme.android.dsp.SweepState
import com.supreme.android.dsp.SweepType
import com.supreme.android.infrastructure.export.TelemetryExporter
import com.supreme.android.signal.SignalEngine
import com.supreme.android.signal.SignalEngineState
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
    val dspControls: DSPControlsState = DSPControlsState()
)

class SignalLabViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(SignalLabState())
    val state: StateFlow<SignalLabState> = _state.asStateFlow()

    private val signalEngine = SignalEngine(application)
    private val sweepEngine = SweepEngine()
    private val exporter = TelemetryExporter()
    private var sweepJob: Job? = null

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
            dspControls = _state.value.dspControls.copy(bassBoostDb = db)
        )
    }

    fun setBassCutoff(hz: Double) {
        signalEngine.dsp.bassBoost.setCutoffFrequency(hz)
        _state.value = _state.value.copy(
            dspControls = _state.value.dspControls.copy(bassCutoffHz = hz)
        )
    }

    fun setBassEnabled(enabled: Boolean) {
        signalEngine.dsp.bassBoost.setEnabled(enabled)
        _state.value = _state.value.copy(
            dspControls = _state.value.dspControls.copy(bassEnabled = enabled)
        )
    }

    fun setEQBandGain(bandIndex: Int, gainDb: Double) {
        signalEngine.dsp.eq.setBandGain(bandIndex, gainDb)
        val newBands = _state.value.dspControls.eqBands.toMutableList()
        if (bandIndex in newBands.indices) {
            newBands[bandIndex] = gainDb
        }
        _state.value = _state.value.copy(
            dspControls = _state.value.dspControls.copy(eqBands = newBands)
        )
    }

    fun setEQEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(
            dspControls = _state.value.dspControls.copy(eqEnabled = enabled)
        )
    }

    fun applyEQPreset(preset: ParametricEQ.EQPreset) {
        signalEngine.dsp.eq.applyPreset(preset)
        _state.value = _state.value.copy(
            dspControls = _state.value.dspControls.copy(
                eqPreset = preset,
                eqBands = signalEngine.dsp.eq.getBands().map { it.gainDb }
            )
        )
    }

    fun setVirtualizerWidth(width: Float) {
        signalEngine.dsp.virtualizer.setWidth(width)
        _state.value = _state.value.copy(
            dspControls = _state.value.dspControls.copy(virtualizerWidth = width)
        )
    }

    fun setVirtualizerCrossfeed(cf: Float) {
        signalEngine.dsp.virtualizer.setCrossfeed(cf)
        _state.value = _state.value.copy(
            dspControls = _state.value.dspControls.copy(virtualizerCrossfeed = cf)
        )
    }

    fun setVirtualizerEnabled(enabled: Boolean) {
        signalEngine.dsp.virtualizer.setEnabled(enabled)
        _state.value = _state.value.copy(
            dspControls = _state.value.dspControls.copy(virtualizerEnabled = enabled)
        )
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
            sweepProgress = 0f
        )

        sweepJob?.cancel()
        sweepJob = viewModelScope.launch(Dispatchers.Default) {
            val audioConfig = AudioOutputConfig(sampleRate = s.sampleRate)
            val buffer = FloatArray(1024)

            signalEngine.start(
                SignalConfig(
                    frequencyHz = plan.startHz,
                    amplitude = plan.amplitude,
                    waveform = plan.waveform
                ),
                audioConfig
            )

            while (true) {
                val sweepState = sweepEngine.render(buffer, 1024, s.sampleRate)
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

            signalEngine.stop()
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
                    dspControls = _state.value.dspControls.copy(
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
        signalEngine.release()
    }
}
