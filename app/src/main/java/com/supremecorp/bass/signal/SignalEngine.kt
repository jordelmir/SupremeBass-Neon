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
import com.supremecorp.bass.audio.safety.SafetyStopReason
import com.supremecorp.bass.core.logging.AppLogger
import com.supremecorp.bass.domain.model.*
import com.supremecorp.bass.dsp.Limiter
import com.supremecorp.bass.dsp.Oscillator
import com.supremecorp.bass.dsp.SignalGenerator
import com.supremecorp.bass.ui.settings.AudioSettingsPrefs
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

class SignalEngine(
    private val context: Context,
    private val backend: AudioOutputBackend = AndroidAudioTrackBackend()
) {
    private val state = AtomicReference<SignalEngineState>(SignalEngineState.Idle)
    private val generator = SignalGenerator(Oscillator())
    private val limiter = Limiter()
    private val safetyController = AcousticSafetyController()
    private val routeMonitor = AudioRouteMonitor(context)

    private val handler = Handler(Looper.getMainLooper())
    private var audioThread: Thread? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var currentConfig: SignalConfig? = null

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
        if (safetyCheck is com.supremecorp.bass.audio.safety.SafetyCheckResult.Blocked) {
            AppLogger.e("SignalEngine", "Safety blocked: ${safetyCheck.reason}")
            state.set(SignalEngineState.Failed(SignalEngineError.SpeakerRouteRequired))
            return false
        }

        val clampedAmplitude = safetyController.clampAmplitude(safeConfig.amplitude)
        val finalConfig = safeConfig.copy(amplitude = clampedAmplitude)
        currentConfig = finalConfig

        generator.configure(audioConfig.sampleRate)
        generator.reset()
        limiter.reset()
        safetyController.setRoute(routeMonitor.getCurrentRoute())
        safetyController.routeInterlockEnabled = AudioSettingsPrefs.routeInterlock(context)

        val backendResult = backend.start(audioConfig)
        if (backendResult is AudioBackendResult.Failure) {
            AppLogger.e("SignalEngine", "Backend start failed: ${backendResult.error}")
            state.set(SignalEngineState.Failed(backendResult.error))
            return false
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

        while (state.get() is SignalEngineState.Running) {
            val safetyStop = safetyController.shouldStop()
            if (safetyStop != null) {
                AppLogger.w("SignalEngine", "Safety stop: $safetyStop")
                break
            }

            generator.render(buffer, bufferFrames, config, audioConfig.sampleRate)
            limiter.process(buffer, bufferFrames)

            when (val result = backend.write(buffer, bufferFrames)) {
                is AudioBackendResult.Failure -> {
                    AppLogger.e("SignalEngine", "Write failed: ${result.error}")
                    state.set(SignalEngineState.Failed(result.error))
                    break
                }
                is AudioBackendResult.Success -> { /* continue */ }
            }
        }

        handler.post { stopInternal() }
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
            peak = limiter.peak,
            rms = limiter.rms,
            durationMs = elapsed,
            audioRoute = session.route,
            underruns = 0,
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
