package com.supremecorp.bass.signal

import com.supremecorp.bass.domain.model.*
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class SignalEngineStateTest {

    @Test
    fun `valid state transitions`() {
        var state: SignalEngineState = SignalEngineState.Idle
        assertTrue(state is SignalEngineState.Idle)

        state = SignalEngineState.Preparing
        assertTrue(state is SignalEngineState.Preparing)

        val session = SignalSession(
            id = "test",
            config = SignalConfig(frequencyHz = 440.0, amplitude = 0.5f, waveform = Waveform.SINE),
            audioConfig = AudioOutputConfig(),
            startTime = System.currentTimeMillis(),
            route = OutputRoute.BUILT_IN_SPEAKER
        )
        state = SignalEngineState.Running(session)
        assertTrue(state is SignalEngineState.Running)

        state = SignalEngineState.Stopping
        assertTrue(state is SignalEngineState.Stopping)

        state = SignalEngineState.Idle
        assertTrue(state is SignalEngineState.Idle)
    }

    @Test
    fun `failed state contains error`() {
        val error = SignalEngineError.AudioTrackInitializationFailed
        val state = SignalEngineState.Failed(error)
        assertTrue(state is SignalEngineState.Failed)
        assertEquals("AudioTrack failed to initialize", (state as SignalEngineState.Failed).error.description())
    }

    @Test
    fun `state name returns correct strings`() {
        assertEquals("Idle", SignalEngineState.Idle.name())
        assertEquals("Preparing", SignalEngineState.Preparing.name())
        assertEquals("Stopping", SignalEngineState.Stopping.name())

        val session = SignalSession(
            id = "test",
            config = SignalConfig(frequencyHz = 440.0, amplitude = 0.5f, waveform = Waveform.SINE),
            audioConfig = AudioOutputConfig(),
            startTime = System.currentTimeMillis(),
            route = OutputRoute.BUILT_IN_SPEAKER
        )
        assertEquals("Running", SignalEngineState.Running(session).name())
        assertEquals("Failed(Platform error: test)", SignalEngineState.Failed(SignalEngineError.PlatformFailure(Exception("test"))).name())
    }

    @Test
    fun `error descriptions are non-empty`() {
        val errors = listOf(
            SignalEngineError.UnsupportedAudioFormat,
            SignalEngineError.InvalidSampleRate,
            SignalEngineError.RouteUnavailable,
            SignalEngineError.AudioTrackInitializationFailed,
            SignalEngineError.WriteFailed,
            SignalEngineError.SpeakerRouteRequired,
            SignalEngineError.ThermalLimitReached,
            SignalEngineError.FrequencyBelowNyquist,
            SignalEngineError.AmplitudeExceedsLimit,
            SignalEngineError.InvalidConfiguration,
            SignalEngineError.PlatformFailure(Exception("test"))
        )
        errors.forEach { error ->
            assertTrue(error.description().isNotEmpty(), "Error ${error::class.simpleName} has empty description")
        }
    }
}
