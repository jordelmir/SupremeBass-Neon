package com.supreme.vibration

import com.supreme.core.AssetCategory
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class VibrationDoctorEngineTest {

    private val engine = VibrationDoctorEngine()

    @Test
    fun `test analyze returns analysis`() = runBlocking {
        val x = FloatArray(1000) { (Math.random() * 0.1).toFloat() }
        val y = FloatArray(1000) { (Math.random() * 0.1).toFloat() }
        val z = FloatArray(1000) { (Math.random() * 0.5).toFloat() }
        val analysis = engine.analyze(x, y, z, 100f, assetCategory = AssetCategory.APPLIANCE)
        assertNotNull(analysis)
        assertTrue(analysis.rmsG >= 0)
    }

    @Test
    fun `test analysis has vibration level`() = runBlocking {
        val x = FloatArray(1000) { (Math.random() * 0.1).toFloat() }
        val y = FloatArray(1000) { (Math.random() * 0.1).toFloat() }
        val z = FloatArray(1000) { (Math.random() * 0.5).toFloat() }
        val analysis = engine.analyze(x, y, z, 100f)
        assertNotNull(analysis.vibrationLevel)
    }

    @Test
    fun `test analysis has diagnosis`() = runBlocking {
        val x = FloatArray(1000) { (Math.random() * 0.1).toFloat() }
        val y = FloatArray(1000) { (Math.random() * 0.1).toFloat() }
        val z = FloatArray(1000) { (Math.random() * 0.5).toFloat() }
        val analysis = engine.analyze(x, y, z, 100f)
        assertNotNull(analysis.diagnosis)
        assertTrue(analysis.diagnosis.causes.isNotEmpty())
    }

    @Test
    fun `test set and get baseline`() = runBlocking {
        val x = FloatArray(1000) { (Math.random() * 0.1).toFloat() }
        val y = FloatArray(1000) { (Math.random() * 0.1).toFloat() }
        val z = FloatArray(1000) { (Math.random() * 0.5).toFloat() }
        val analysis = engine.analyze(x, y, z, 100f, assetId = "washer-1")
        engine.setBaseline("washer-1", analysis)
        val baseline = engine.getBaseline("washer-1")
        assertNotNull(baseline)
    }
}
