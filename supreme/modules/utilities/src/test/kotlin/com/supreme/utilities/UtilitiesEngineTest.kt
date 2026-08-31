package com.supreme.utilities

import com.supreme.core.DeviceProtocol
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.Instant

class UtilitiesEngineTest {

    private val engine = UtilitiesEngine()

    @Test
    fun `test add meter`() {
        val meter = Meter(
            id = "water-1",
            name = "Main Water",
            type = MeterType.WATER,
            unit = "m³"
        )
        engine.addMeter(meter)
        assertEquals(1, engine.state.value.totalMeters)
    }

    @Test
    fun `test record reading`() {
        val meter = Meter(id = "water-1", name = "Main Water", type = MeterType.WATER, unit = "m³")
        engine.addMeter(meter)
        engine.recordReading("water-1", MeterReading(value = 100.0))
        engine.recordReading("water-1", MeterReading(value = 105.0))
        assertEquals(2, engine.state.value.totalReadings)
    }

    @Test
    fun `test consumption history`() {
        val meter = Meter(id = "water-1", name = "Main Water", type = MeterType.WATER, unit = "m³")
        engine.addMeter(meter)
        engine.recordReading("water-1", MeterReading(value = 100.0))
        engine.recordReading("water-1", MeterReading(value = 105.0))
        val history = engine.getConsumptionHistory("water-1")
        assertEquals(1, history.size)
        assertEquals(5.0, history[0].consumption)
    }

    @Test
    fun `test consumption summary`() {
        val meter = Meter(id = "water-1", name = "Main Water", type = MeterType.WATER, unit = "m³", costPerUnit = 0.15)
        engine.addMeter(meter)
        engine.recordReading("water-1", MeterReading(value = 100.0))
        engine.recordReading("water-1", MeterReading(value = 105.0))
        val summary = engine.getConsumptionSummary("water-1")
        assertEquals(5.0, summary.totalConsumption)
    }
}
