package com.supreme.android.signal

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import com.supreme.android.audio.backend.AndroidAudioTrackBackend
import com.supreme.android.audio.backend.AudioOutputBackend
import com.supreme.android.audio.safety.AcousticSafetyController
import com.supreme.android.audio.safety.AudioRouteMonitor
import com.supreme.android.audio.safety.RouteChangeListener
import com.supreme.android.audio.safety.SafetyStopReason
import com.supreme.android.core.logging.AppLogger
import com.supreme.android.domain.model.*
import com.supreme.android.dsp.AudioDSPChain
import com.supreme.android.dsp.Limiter
import com.supreme.android.dsp.Oscillator
import com.supreme.android.dsp.SignalGenerator
import com.supreme.android.ui.settings.AudioSettingsPrefs
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

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
        val currentState = state.get()
        if (currentState is SignalEngineState.Running) {
            AppLogger.w("SignalEngine", "Already running, stop first")
            return false
        }

        state.set(SignalEngineState.Preparing)

        val safeConfig = config.withNyquistGuard(audioConfig.sampleRate)
        val safetyCheck = safetyController.validateConfig(safeConfig)
        if (safetyCheck is com.supreme.android.audio.safety.SafetyCheckResult.Blocked) {
            AppLogger.e("SignalEngine", "Safety blocked: ${safetyCheck.reason}")
            state.set(SignalEngineState.Failed(SignalEngineError.SpeakerRouteRequired))
            return false
        }

        val clampedAmplitude = safetyController.clampAmplitude(safeConfig.amplitude)
        val finalConfig = safeConfig.copy(amplitude = clampedAmplitude)
        currentConfig = finalConfig

        generator.configure(audioConfig.sampleRate)
        generator.reset()
        dspChain.configure(audioConfig.sampleRate)
        dspChain.reset()
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

        state.set(SignalEngineState.Running(session))

        audioThread = Thread({
            renderLoop(session, finalConfig, audioConfig)
        }, "SignalEngine-Render").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }

        AppLogger.i("SignalEngine", "Started session ${session.id}: " +
                "${finalConfig.waveform} ${finalConfig.frequencyHz}Hz " +
                "amp=${finalConfig.amplitude} @ ${audioConfig.sampleRate}Hz")

        return true
    }

    private fun renderLoop(
        session: SignalSession,
        config: SignalConfig,
        audioConfig: AudioOutputConfig
    ) {
        val bufferFrames = 1024
        val buffer = FloatArray(bufferFrames * audioConfig.channelCount)
        var isRampingDown = false
        var rampDownRemaining = 0

        while (state.get() is SignalEngineState.Running) {
            val safetyStop = safetyController.shouldStop()
            if (safetyStop != null) {
                AppLogger.w("SignalEngine", "Safety stop: $safetyStop")
                // Initiate ramp-down before stopping
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
                is AudioBackendResult.Success -> { /* continue */ }
            }

            // If ramp-down complete, stop
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
            underruns = dspChain.limiter.clippedSamples,
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
