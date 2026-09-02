package com.supreme.android.signal

import com.supreme.android.domain.model.SignalConfig
import com.supreme.android.domain.model.AudioOutputConfig
import com.supreme.android.domain.model.OutputRoute
import com.supreme.android.domain.model.SignalTelemetry

data class SignalSession(
    val id: String,
    val config: SignalConfig,
    val audioConfig: AudioOutputConfig,
    val startTime: Long,
    val route: OutputRoute
)

sealed interface SignalEngineState {
    data object Idle : SignalEngineState
    data object Preparing : SignalEngineState
    data class Running(val session: SignalSession) : SignalEngineState
    data object Stopping : SignalEngineState
    data class Failed(val error: com.supreme.android.domain.model.SignalEngineError) : SignalEngineState
}

fun SignalEngineState.name(): String = when (this) {
    is SignalEngineState.Idle -> "Idle"
    is SignalEngineState.Preparing -> "Preparing"
    is SignalEngineState.Running -> "Running"
    is SignalEngineState.Stopping -> "Stopping"
    is SignalEngineState.Failed -> "Failed(${error.description()})"
}
