package com.supreme.android.audio.backend

import com.supreme.android.domain.model.AudioBackendResult
import com.supreme.android.domain.model.AudioOutputConfig

interface AudioOutputBackend {
    fun start(config: AudioOutputConfig): AudioBackendResult
    fun write(buffer: FloatArray, frames: Int): AudioBackendResult
    fun stop(): AudioBackendResult
    fun release()
    fun isRunning(): Boolean
    fun getConfig(): AudioOutputConfig?
}
