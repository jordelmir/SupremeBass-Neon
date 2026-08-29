package com.supremecorp.bass.domain.model

sealed class SignalEngineError {
    data object UnsupportedAudioFormat : SignalEngineError()
    data object InvalidSampleRate : SignalEngineError()
    data object RouteUnavailable : SignalEngineError()
    data object AudioTrackInitializationFailed : SignalEngineError()
    data object WriteFailed : SignalEngineError()
    data object SpeakerRouteRequired : SignalEngineError()
    data object ThermalLimitReached : SignalEngineError()
    data object FrequencyBelowNyquist : SignalEngineError()
    data object AmplitudeExceedsLimit : SignalEngineError()
    data object InvalidConfiguration : SignalEngineError()
    data class PlatformFailure(val cause: Throwable) : SignalEngineError()

    fun description(): String = when (this) {
        is UnsupportedAudioFormat -> "Audio format not supported by device"
        is InvalidSampleRate -> "Invalid sample rate"
        is RouteUnavailable -> "Audio route unavailable"
        is AudioTrackInitializationFailed -> "AudioTrack failed to initialize"
        is WriteFailed -> "AudioTrack write failed"
        is SpeakerRouteRequired -> "Speaker route required for this mode"
        is ThermalLimitReached -> "Device thermal limit reached"
        is FrequencyBelowNyquist -> "Frequency exceeds Nyquist limit"
        is AmplitudeExceedsLimit -> "Amplitude exceeds allowed limit"
        is InvalidConfiguration -> "Invalid signal configuration"
        is PlatformFailure -> "Platform error: ${cause.message}"
    }
}
