package com.supreme.android.domain.model

data class AudioOutputConfig(
    val sampleRate: Int = 48_000,
    val channelCount: Int = 1,
    val encoding: AudioEncoding = AudioEncoding.PCM_FLOAT,
    val bufferFrames: Int = 0
) {
    init {
        require(sampleRate > 0) { "sampleRate must be > 0" }
        require(channelCount in 1..2) { "channelCount must be 1 or 2" }
        require(bufferFrames >= 0) { "bufferFrames must be >= 0" }
    }
}

enum class AudioEncoding {
    PCM_FLOAT,
    PCM_16BIT
}

sealed interface AudioBackendResult {
    data object Success : AudioBackendResult
    data class Failure(val error: SignalEngineError) : AudioBackendResult
}
