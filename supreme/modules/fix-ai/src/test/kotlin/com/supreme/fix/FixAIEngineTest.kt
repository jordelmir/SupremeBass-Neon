package com.supreme.fix

import com.supreme.core.AssetCategory
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class FixAIEngineTest {

    private val engine = FixAIEngine()

    @Test
    fun `test diagnoseFromAudio returns diagnosis`() = runBlocking {
        val audio = FloatArray(44100) { (Math.random() * 2 - 1).toFloat() }
        val diagnosis = engine.diagnoseFromAudio(audio, assetCategory = AssetCategory.APPLIANCE)
        assertNotNull(diagnosis)
        assertTrue(diagnosis.mostLikelyCauses.isNotEmpty())
        assertTrue(diagnosis.confidence > 0)
    }

    @Test
    fun `test diagnosis has ranked causes`() = runBlocking {
        val audio = FloatArray(44100) { (Math.random() * 2 - 1).toFloat() }
        val diagnosis = engine.diagnoseFromAudio(audio, assetCategory = AssetCategory.APPLIANCE)
        val sorted = diagnosis.mostLikelyCauses.sortedByDescending { it.probability }
        assertEquals(sorted, diagnosis.mostLikelyCauses)
    }

    @Test
    fun `test diagnosis has checks`() = runBlocking {
        val audio = FloatArray(44100) { (Math.random() * 2 - 1).toFloat() }
        val diagnosis = engine.diagnoseFromAudio(audio)
        assertTrue(diagnosis.checks.isNotEmpty())
    }

    @Test
    fun `test diagnosis has next tests`() = runBlocking {
        val audio = FloatArray(44100) { (Math.random() * 2 - 1).toFloat() }
        val diagnosis = engine.diagnoseFromAudio(audio)
        assertTrue(diagnosis.nextTests.isNotEmpty())
    }

    @Test
    fun `test vehicle category gives different causes`() = runBlocking {
        val audio = FloatArray(44100) { (Math.random() * 2 - 1).toFloat() }
        val vehicleDiag = engine.diagnoseFromAudio(audio, assetCategory = AssetCategory.VEHICLE)
        val applianceDiag = engine.diagnoseFromAudio(audio, assetCategory = AssetCategory.APPLIANCE)
        assertNotEquals(
            vehicleDiag.mostLikelyCauses.firstOrNull()?.name,
            applianceDiag.mostLikelyCauses.firstOrNull()?.name
        )
    }
}
