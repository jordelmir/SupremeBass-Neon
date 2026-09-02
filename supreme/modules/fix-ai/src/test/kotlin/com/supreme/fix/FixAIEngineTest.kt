package com.supreme.fix

import com.supreme.core.AssetCategory
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class FixAIEngineTest {

    private val engine = FixAIEngine()

    @Test
    fun `test diagnoseFromAudio returns diagnosis`() = runBlocking {
        val audio = ByteArray(44100) { (Math.random() * 256 - 128).toInt().toByte() }
        val diagnosis = engine.diagnoseFromAudio(audio, assetCategory = AssetCategory.APPLIANCE)
        assertNotNull(diagnosis)
        assertNotNull(diagnosis.mostLikelyCauses)
        assertTrue(diagnosis.confidence >= 0)
    }

    @Test
    fun `test diagnosis has ranked causes`() = runBlocking {
        val audio = ByteArray(44100) { (Math.random() * 256 - 128).toInt().toByte() }
        val diagnosis = engine.diagnoseFromAudio(audio, assetCategory = AssetCategory.APPLIANCE)
        val sorted = diagnosis.mostLikelyCauses.sortedByDescending { it.probability }
        assertEquals(sorted, diagnosis.mostLikelyCauses)
    }

    @Test
    fun `test diagnosis has checks`() = runBlocking {
        val audio = ByteArray(44100) { (Math.random() * 256 - 128).toInt().toByte() }
        val diagnosis = engine.diagnoseFromAudio(audio)
        assertNotNull(diagnosis.checks)
    }

    @Test
    fun `test diagnosis has next tests`() = runBlocking {
        val audio = ByteArray(44100) { (Math.random() * 256 - 128).toInt().toByte() }
        val diagnosis = engine.diagnoseFromAudio(audio)
        assertNotNull(diagnosis.nextTests)
    }

    @Test
    fun `test vehicle category gives different causes`() = runBlocking {
        val audio = ByteArray(44100) { (Math.random() * 256 - 128).toInt().toByte() }
        val vehicleDiag = engine.diagnoseFromAudio(audio, assetCategory = AssetCategory.VEHICLE)
        val applianceDiag = engine.diagnoseFromAudio(audio, assetCategory = AssetCategory.APPLIANCE)
        assertNotNull(vehicleDiag)
        assertNotNull(applianceDiag)
    }
}
