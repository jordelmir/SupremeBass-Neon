package com.supreme.noise

import com.supreme.core.AssetCategory
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class NoiseDoctorEngineTest {

    private val engine = NoiseDoctorEngine()

    @Test
    fun `test analyze returns analysis`() = runBlocking {
        val audio = generateSineWave(440.0f, 44100, 15)
        val analysis = engine.analyze(audio, 44100, assetCategory = AssetCategory.APPLIANCE)
        assertNotNull(analysis)
        assertTrue(analysis.dominantFrequency > 0)
    }

    @Test
    fun `test analysis has noise type`() = runBlocking {
        val audio = generateSineWave(440.0f, 44100, 15)
        val analysis = engine.analyze(audio, 44100)
        assertNotNull(analysis.noiseType)
    }

    @Test
    fun `test analysis has diagnosis`() = runBlocking {
        val audio = generateSineWave(440.0f, 44100, 15)
        val analysis = engine.analyze(audio, 44100)
        assertNotNull(analysis.diagnosis)
        assertTrue(analysis.diagnosis.causes.isNotEmpty())
    }

    @Test
    fun `test analysis has harmonics`() = runBlocking {
        val audio = generateSineWave(440.0f, 44100, 15)
        val analysis = engine.analyze(audio, 44100)
        assertNotNull(analysis.harmonics)
    }

    @Test
    fun `test compare detects improvement`() = runBlocking {
        val loud = generateSineWave(440.0f, 44100, 15).map { it * 2.0f }.toFloatArray()
        val quiet = generateSineWave(440.0f, 44100, 15).map { it * 0.5f }.toFloatArray()
        val before = engine.analyze(loud, 44100)
        val after = engine.analyze(quiet, 44100)
        val comparison = engine.compare(before, after)
        assertTrue(comparison.improvementDetected)
    }

    private fun generateSineWave(freq: Float, sampleRate: Int, durationSec: Int): FloatArray {
        val samples = sampleRate * durationSec
        return FloatArray(samples) { i ->
            (Math.sin(2.0 * Math.PI * freq * i / sampleRate) * 0.8).toFloat()
        }
    }
}
