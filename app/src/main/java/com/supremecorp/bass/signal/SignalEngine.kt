package com.supremecorp.bass.signal

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import com.supremecorp.bass.audio.backend.AndroidAudioTrackBackend
import com.supremecorp.bass.audio.backend.AudioOutputBackend
import com.supremecorp.bass.audio.safety.AcousticSafetyController
import com.supremecorp.bass.audio.safety.AudioRouteMonitor
import com.supremecorp.bass.audio.safety.RouteChangeListener
import com.supremecorp.bass.core.logging.AppLogger
import com.supremecorp.bass.domain.model.*
import com.supremecorp.bass.dsp.AudioDSPChain
import com.supremecorp.bass.dsp.DSPStats
import com.supremecorp.bass.dsp.Oscillator
import com.supremecorp.bass.dsp.SignalGenerator
import com.supremecorp.bass.domain.model.SignalTelemetry
import com.supremecorp.bass.ui.settings.AudioSettingsPrefs
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Immutable snapshot of DSP configuration at start time.
 * Prevents UI-modified DSP state from being reset during playback.
 */
data class DspConfiguration(
    val bassBoostDb: Float,
    val eqEnabled: Boolean,
    val eqBandGains: List<Double>,
    val virtualizerWidth: Float,
    val limiterCeiling: Float
) {
    companion object {
        fun fromChain(chain: AudioDSPChain): DspConfiguration {
            return DspConfiguration(
                bassBoostDb = chain.bassBoost.getBoost(),
                eqEnabled = true,
                eqBandGains = chain.eq.getBands().map { it.gainDb },
                virtualizerWidth = chain.virtualizer.getWidth(),
                limiterCeiling = 0.95f
            )
        }
    }
}

class SignalEngine(
    private val context: Context,
    private val backend: AudioOutputBackend = AndroidAudioTrackBackend()
) {
    private val state = AtomicReference<SignalEngineState>(SignalEngineState.Idle)
    private val generator = SignalGenerator(Oscillator())
    private val dspChain = AudioDSPChain()
    private val safetyController = AcousticSafetyController()
    private val routeMonitor = AudioRouteMonitor(context)

    /** Access the DSP chain for UI controls (EQ, bass boost, virtualizer) */
    val dsp: AudioDSPChain get() = dspChain

    private val handler = Handler(Looper.getMainLooper())
    private var audioThread: Thread? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var currentConfig: SignalConfig? = null

    // Telemetry counters (separated, not conflated)
    private val clippedSamplesCounter = AtomicInteger(0)
    private val underrunCounter = AtomicInteger(0)
    private val partialWriteCounter = AtomicInteger(0)
    private val framesWrittenCounter = AtomicLong(0L)

    // Safety ramp state
    private var rampSamplesRemaining: Int = 0
    private var rampTotalSamples: Int = 0
    private var isRampingUp: Boolean = true

    private val routeChangeListener = RouteChangeListener { _, newRoute ->
        safetyController.setRoute(newRoute)
        if (newRoute == OutputRoute.WIRED_HEADPHONES || newRoute == OutputRoute.BLUETOOTH) {
            AppLogger.w("SignalEngine", "Unsafe route detected: $newRoute, stopping")
            stop()
        }
    }

    fun getState(): SignalEngineState = state.get()

    fun start(config: SignalConfig, audioConfig: AudioOutputConfig = AudioOutputConfig()): Boolean {
        // CAS: IDLE → PREPARING
        if (!state.compareAndSet(SignalEngineState.Idle, SignalEngineState.Preparing)) {
            AppLogger.w("SignalEngine", "Cannot start: state=${state.get().name()}")
            return false
        }

        try {
            val safeConfig = config.withNyquistGuard(audioConfig.sampleRate)
            val safetyCheck = safetyController.validateConfig(safeConfig)
            if (safetyCheck is com.supremecorp.bass.audio.safety.SafetyCheckResult.Blocked) {
                AppLogger.e("SignalEngine", "Safety blocked: ${safetyCheck.reason}")
                state.set(SignalEngineState.Failed(SignalEngineError.SpeakerRouteRequired))
                return false
            }

            val clampedAmplitude = safetyController.clampAmplitude(safeConfig.amplitude)
            val finalConfig = safeConfig.copy(amplitude = clampedAmplitude)
            currentConfig = finalConfig

            // Snapshot DSP config BEFORE starting — prevents UI race conditions
            val dspSnapshot = DspConfiguration.fromChain(dspChain)

            generator.configure(audioConfig.sampleRate)
            generator.reset()

            // Configure DSP without resetting user settings
            dspChain.configure(audioConfig.sampleRate)
            applyDspSnapshot(dspSnapshot)

            safetyController.setRoute(routeMonitor.getCurrentRoute())
            safetyController.routeInterlockEnabled = AudioSettingsPrefs.routeInterlock(context)

            val backendResult = backend.start(audioConfig)
            if (backendResult is AudioBackendResult.Failure) {
                AppLogger.e("SignalEngine", "Backend start failed: ${backendResult.error}")
                state.set(SignalEngineState.Failed(backendResult.error))
                return false
            }

            // Connect the routed device supplier for accurate route detection
            if (backend is AndroidAudioTrackBackend) {
                routeMonitor.setRoutedDeviceSupplier { backend.getRoutedDevice() }
            }

            val session = SignalSession(
                id = UUID.randomUUID().toString(),
                config = finalConfig,
                audioConfig = audioConfig,
                startTime = System.currentTimeMillis(),
                route = routeMonitor.getCurrentRoute()
            )

            acquireWakeLock()
            routeMonitor.setListener(routeChangeListener)
            routeMonitor.start()
            safetyController.onSessionStart()

            // Initialize safety ramp
            rampTotalSamples = safetyController.getRampUpSamples(audioConfig.sampleRate)
            rampSamplesRemaining = rampTotalSamples
            isRampingUp = true

            // Reset telemetry counters
            clippedSamplesCounter.set(0)
            underrunCounter.set(0)
            partialWriteCounter.set(0)
            framesWrittenCounter.set(0L)

            // CAS: PREPARING → RUNNING
            if (!state.compareAndSet(SignalEngineState.Preparing, SignalEngineState.Running(session))) {
                AppLogger.e("SignalEngine", "State transition failed during start")
                backend.stop()
                releaseWakeLock()
                return false
            }

            audioThread = Thread({
                renderLoop(session, finalConfig, audioConfig, dspSnapshot)
            }, "SignalEngine-Render").apply {
                priority = Thread.MAX_PRIORITY
                start()
            }

            AppLogger.i("SignalEngine", "Started session ${session.id}: " +
                    "${finalConfig.waveform} ${finalConfig.frequencyHz}Hz " +
                    "amp=${finalConfig.amplitude} @ ${audioConfig.sampleRate}Hz")

            return true

        } catch (e: Exception) {
            AppLogger.e("SignalEngine", "Start failed: ${e.message}")
            state.set(SignalEngineState.Idle)
            return false
        }
    }

    /**
     * Apply a DSP snapshot without calling reset() on the chain.
     * This preserves user EQ/bass/virtualizer settings across start/stop cycles.
     */
    private fun applyDspSnapshot(snapshot: DspConfiguration) {
        dspChain.bassBoost.setBoost(snapshot.bassBoostDb)
        snapshot.eqBandGains.forEachIndexed { index, gain ->
            dspChain.eq.setBandGain(index, gain)
        }
        dspChain.virtualizer.setWidth(snapshot.virtualizerWidth)
    }

    private fun renderLoop(
        session: SignalSession,
        config: SignalConfig,
        audioConfig: AudioOutputConfig,
        dspSnapshot: DspConfiguration
    ) {
        val bufferFrames = 1024
        val buffer = FloatArray(bufferFrames * audioConfig.channelCount)
        var isRampingDown = false
        var rampDownRemaining = 0

        while (state.get() is SignalEngineState.Running) {
            val safetyStop = safetyController.shouldStop()
            if (safetyStop != null) {
                AppLogger.w("SignalEngine", "Safety stop: $safetyStop")
                isRampingDown = true
                rampDownRemaining = safetyController.getRampDownSamples(audioConfig.sampleRate)
            }

            generator.render(buffer, bufferFrames, config, audioConfig.sampleRate)

            // Process through DSP chain (Bass → EQ → Virtualizer → Limiter)
            if (audioConfig.channelCount == 2) {
                dspChain.processStereo(buffer, bufferFrames)
            } else {
                dspChain.processMono(buffer, bufferFrames)
            }

            // Apply safety ramp
            val rampSamples = when {
                isRampingUp && rampSamplesRemaining > 0 -> {
                    val samplesThisBlock = minOf(bufferFrames, rampSamplesRemaining)
                    rampSamplesRemaining -= samplesThisBlock
                    if (rampSamplesRemaining <= 0) isRampingUp = false
                    samplesThisBlock
                }
                isRampingDown && rampDownRemaining > 0 -> {
                    val samplesThisBlock = minOf(bufferFrames, rampDownRemaining)
                    rampDownRemaining -= samplesThisBlock
                    samplesThisBlock
                }
                else -> 0
            }

            if (rampSamples > 0) {
                applyRampToBuffer(buffer, bufferFrames, audioConfig.channelCount,
                    rampSamples, rampTotalSamples, isRampingUp)
            }

            when (val result = backend.write(buffer, bufferFrames)) {
                is AudioBackendResult.Failure -> {
                    AppLogger.e("SignalEngine", "Write failed: ${result.error}")
                    state.set(SignalEngineState.Failed(result.error))
                    break
                }
                is AudioBackendResult.Success -> {
                    framesWrittenCounter.addAndGet(bufferFrames.toLong())
                }
            }

            // Track clipped samples from limiter
            clippedSamplesCounter.addAndGet(dspChain.limiter.clippedSamples.toLong().toInt())

            if (isRampingDown && rampDownRemaining <= 0) {
                AppLogger.i("SignalEngine", "Ramp-down complete, stopping")
                break
            }
        }

        handler.post { stopInternal() }
    }

    private fun applyRampToBuffer(
        buffer: FloatArray,
        frameCount: Int,
        channelCount: Int,
        rampSamples: Int,
        totalRampSamples: Int,
        isRampUp: Boolean
    ) {
        if (totalRampSamples <= 0) return

        for (i in 0 until frameCount) {
            val globalSample = if (isRampUp) {
                totalRampSamples - rampSamples + i
            } else {
                rampSamples - i
            }

            val gain = (globalSample.toFloat() / totalRampSamples).coerceIn(0.0f, 1.0f)

            for (ch in 0 until channelCount) {
                val sampleIndex = i * channelCount + ch
                if (sampleIndex < buffer.size) {
                    buffer[sampleIndex] *= gain
                }
            }
        }
    }

    fun stop() {
        val currentState = state.get()
        if (currentState !is SignalEngineState.Running && currentState !is SignalEngineState.Preparing) {
            return
        }
        state.set(SignalEngineState.Stopping)
        AppLogger.i("SignalEngine", "Stopping...")
        stopInternal()
    }

    private fun stopInternal() {
        routeMonitor.stop()
        safetyController.onSessionStop()
        backend.stop()
        releaseWakeLock()

        audioThread?.join(1000)
        audioThread = null
        currentConfig = null

        if (state.get() !is SignalEngineState.Failed) {
            state.set(SignalEngineState.Idle)
        }

        AppLogger.i("SignalEngine", "Stopped, final state=${state.get().name()}")
    }

    fun emergencyStop() {
        AppLogger.w("SignalEngine", "EMERGENCY STOP")
        state.set(SignalEngineState.Stopping)
        audioThread?.interrupt()
        routeMonitor.stop()
        safetyController.onSessionStop()
        backend.release()
        releaseWakeLock()
        audioThread = null
        currentConfig = null
        state.set(SignalEngineState.Idle)
    }

    fun getTelemetry(): SignalTelemetry? {
        val session = (state.get() as? SignalEngineState.Running)?.session ?: return null
        val elapsed = System.currentTimeMillis() - session.startTime

        return SignalTelemetry(
            sessionId = session.id,
            timestamp = session.startTime,
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            androidVersion = Build.VERSION.SDK_INT,
            sampleRate = session.audioConfig.sampleRate,
            encoding = session.audioConfig.encoding.name,
            bufferFrames = 1024,
            waveform = session.config.waveform,
            frequencyHz = session.config.frequencyHz,
            amplitude = session.config.amplitude,
            peak = dspChain.limiter.peak,
            rms = dspChain.limiter.rms,
            durationMs = elapsed,
            audioRoute = session.route,
            clippedSamples = clippedSamplesCounter.get(),
            underrunCount = underrunCounter.get(),
            partialWrites = partialWriteCounter.get(),
            framesWritten = framesWrittenCounter.get(),
            terminationReason = state.get().name()
        )
    }

    fun release() {
        emergencyStop()
        backend.release()
    }

    private fun acquireWakeLock() {
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SupremeAcoustics::SignalEngine").apply {
                acquire(60 * 60 * 1000L)
            }
        } catch (e: Exception) {
            AppLogger.e("SignalEngine", "WakeLock acquire failed: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let { if (it.isHeld) it.release() }
            wakeLock = null
        } catch (e: Exception) {
            AppLogger.w("SignalEngine", "WakeLock release error: ${e.message}")
        }
    }
}
