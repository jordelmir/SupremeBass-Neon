package com.supremecorp.bass.audio.backend

import com.supremecorp.bass.domain.model.AudioBackendResult
import com.supremecorp.bass.domain.model.AudioOutputConfig

interface AudioOutputBackend {
    fun start(config: AudioOutputConfig): AudioBackendResult
    fun write(buffer: FloatArray, frames: Int): AudioBackendResult
    fun stop(): AudioBackendResult
    fun release()
    fun isRunning(): Boolean
    fun getConfig(): AudioOutputConfig?
}
