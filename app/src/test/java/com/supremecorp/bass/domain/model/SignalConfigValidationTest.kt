package com.supremecorp.bass.domain.model

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class SignalConfigValidationTest {

    @Test
    fun `valid config passes`() {
        val config = SignalConfig(
            frequencyHz = 440.0,
            amplitude = 0.5f,
            waveform = Waveform.SINE
        )
        assertTrue(config.frequencyHz == 440.0)
    }

    @Test
    fun `zero frequency fails`() {
        assertFailsWith<IllegalArgumentException> {
            SignalConfig(frequencyHz = 0.0, amplitude = 0.5f, waveform = Waveform.SINE)
        }
    }

    @Test
    fun `negative frequency fails`() {
        assertFailsWith<IllegalArgumentException> {
            SignalConfig(frequencyHz = -100.0, amplitude = 0.5f, waveform = Waveform.SINE)
        }
    }

    @Test
    fun `NaN frequency fails`() {
        assertFailsWith<IllegalArgumentException> {
            SignalConfig(frequencyHz = Double.NaN, amplitude = 0.5f, waveform = Waveform.SINE)
        }
    }

    @Test
    fun `Infinity frequency fails`() {
        assertFailsWith<IllegalArgumentException> {
            SignalConfig(frequencyHz = Double.POSITIVE_INFINITY, amplitude = 0.5f, waveform = Waveform.SINE)
        }
    }

    @Test
    fun `amplitude above 1 fails`() {
        assertFailsWith<IllegalArgumentException> {
            SignalConfig(frequencyHz = 440.0, amplitude = 1.5f, waveform = Waveform.SINE)
        }
    }

    @Test
    fun `negative amplitude fails`() {
        assertFailsWith<IllegalArgumentException> {
            SignalConfig(frequencyHz = 440.0, amplitude = -0.1f, waveform = Waveform.SINE)
        }
    }

    @Test
    fun `negative duration fails`() {
        assertFailsWith<IllegalArgumentException> {
            SignalConfig(frequencyHz = 440.0, amplitude = 0.5f, waveform = Waveform.SINE, durationMs = -1)
        }
    }

    @Test
    fun `nyquist guard clamps frequency`() {
        val config = SignalConfig(frequencyHz = 30_000.0, amplitude = 0.5f, waveform = Waveform.SINE)
        val guarded = config.withNyquistGuard(48_000)
        assertTrue(guarded.frequencyHz < 24_000.0, "Nyquist guard should clamp: ${guarded.frequencyHz}")
    }

    @Test
    fun `valid frequency passes nyquist guard`() {
        val config = SignalConfig(frequencyHz = 440.0, amplitude = 0.5f, waveform = Waveform.SINE)
        val guarded = config.withNyquistGuard(48_000)
        assertTrue(guarded.frequencyHz == 440.0)
    }
}
