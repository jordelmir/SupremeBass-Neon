package com.supreme.leak

import com.supreme.core.DeviceProtocol
import com.supreme.core.Severity
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.Instant

class LeakWatchEngineTest {

    private val engine = LeakWatchEngine()

    @Test
    fun `test add sensor`() {
        val sensor = LeakSensor(
            id = "leak-1",
            name = "Kitchen Sensor",
            location = "Kitchen sink",
            protocol = DeviceProtocol.BLE
        )
        engine.addSensor(sensor)
        assertEquals(1, engine.state.value.sensors.size)
    }

    @Test
    fun `test add valve`() {
        val valve = ShutoffValve(
            id = "valve-1",
            name = "Main Valve",
            zone = "kitchen",
            protocol = DeviceProtocol.MATTER
        )
        engine.addValve(valve)
        assertEquals(1, engine.state.value.valves.size)
    }

    @Test
    fun `test record reading with water`() {
        engine.addSensor(LeakSensor(id = "leak-1", name = "Sensor", location = "Kitchen", protocol = DeviceProtocol.BLE))
        val reading = LeakReading(
            sensorId = "leak-1",
            timestamp = Instant.now(),
            waterDetected = true,
            flowRateLpm = 12.0
        )
        engine.recordReading("leak-1", reading)
        assertTrue(engine.state.value.totalReadings > 0)
    }

    @Test
    fun `test flow status`() {
        engine.addSensor(LeakSensor(id = "leak-1", name = "Sensor", location = "Kitchen", protocol = DeviceProtocol.BLE))
        engine.addValve(ShutoffValve(id = "valve-1", name = "Main Valve", zone = "kitchen", protocol = DeviceProtocol.MATTER))
        val status = engine.getFlowStatus()
        assertEquals(1, status.activeSensors)
        assertEquals(1, status.valvesOpen)
    }

    @Test
    fun `test leak history`() {
        engine.addSensor(LeakSensor(id = "leak-1", name = "Sensor", location = "Kitchen", protocol = DeviceProtocol.BLE))
        engine.recordReading("leak-1", LeakReading(
            sensorId = "leak-1",
            timestamp = Instant.now(),
            waterDetected = true,
            flowRateLpm = 5.0
        ))
        val history = engine.getLeakHistory()
        assertEquals(1, history.size)
    }
}
