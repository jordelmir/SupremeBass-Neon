package com.supremecorp.bass.audio.backend

import com.supremecorp.bass.domain.model.AudioBackendResult
import com.supremecorp.bass.domain.model.AudioOutputConfig
import com.supremecorp.bass.domain.model.SignalEngineError

class AudioOutputBackendOboe : AudioOutputBackend {
    private val nativeDsp = NativeDsp()
    private var isStarted = false
    private var currentConfig: AudioOutputConfig? = null

    override fun start(config: AudioOutputConfig): AudioBackendResult {
        return try {
            nativeDsp.nativeConfigure(config.sampleRate)
            currentConfig = config
            isStarted = true
            AudioBackendResult.Success
        } catch (e: Exception) {
            AudioBackendResult.Failure(SignalEngineError.AudioTrackInitializationFailed)
        }
    }

    override fun write(buffer: FloatArray, frames: Int): AudioBackendResult {
        if (!isStarted) return AudioBackendResult.Failure(SignalEngineError.AudioTrackInitializationFailed)
        nativeDsp.nativeProcessBuffer(buffer)
        return AudioBackendResult.Success
    }

    override fun stop(): AudioBackendResult {
        nativeDsp.nativeReset()
        isStarted = false
        currentConfig = null
        return AudioBackendResult.Success
    }

    override fun release() {
        stop()
    }

    override fun isRunning(): Boolean = isStarted

    override fun getConfig(): AudioOutputConfig? = currentConfig

    fun processSample(): Float {
        if (!isStarted) return 0.0f
        return nativeDsp.nativeProcessSample()
    }

    fun getPeak(): Float = nativeDsp.nativeGetPeak()
    fun getRms(): Float = nativeDsp.nativeGetRms()
}
