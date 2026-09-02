package com.supreme.home

import com.supreme.core.DeviceProtocol
import com.supreme.core.DeviceType
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class HomeHubEngineTest {

    private val engine = HomeHubEngine()

    @Test
    fun `test add room`() {
        val room = Room(id = "living", name = "Living Room")
        engine.addRoom(room)
        val state = engine.state.value
        assertEquals(1, state.rooms.size)
    }

    @Test
    fun `test add device to room`() {
        engine.addRoom(Room(id = "living", name = "Living Room"))
        val device = HomeDevice(
            id = "light-1",
            name = "Main Light",
            type = DeviceType.LIGHT,
            roomId = "living",
            protocol = DeviceProtocol.MATTER
        )
        engine.addDevice(device)
        val devices = engine.getRoomDevices("living")
        assertEquals(1, devices.size)
    }

    @Test
    fun `test control device turn on`() = runBlocking {
        engine.addRoom(Room(id = "living", name = "Living Room"))
        val device = HomeDevice(
            id = "light-1",
            name = "Main Light",
            type = DeviceType.LIGHT,
            roomId = "living",
            protocol = DeviceProtocol.MATTER
        )
        engine.addDevice(device)
        val result = engine.controlDevice("light-1", HomeCommand.TurnOn)
        assertTrue(result is CommandResult.Success)
        assertTrue(engine.state.value.devices.first { it.id == "light-1" }.isOn)
    }

    @Test
    fun `test get left on devices`() {
        engine.addRoom(Room(id = "living", name = "Living Room"))
        engine.addDevice(HomeDevice(
            id = "plug-1",
            name = "Heater",
            type = DeviceType.SMART_PLUG,
            roomId = "living",
            protocol = DeviceProtocol.MATTER,
            isOn = true,
            shouldAutoOff = true,
            isHighRisk = true
        ))
        val leftOn = engine.getLeftOn()
        assertEquals(1, leftOn.size)
    }

    @Test
    fun `test safety check detects high risk plug`() {
        engine.addRoom(Room(id = "living", name = "Living Room"))
        engine.addDevice(HomeDevice(
            id = "plug-1",
            name = "Heater",
            type = DeviceType.SMART_PLUG,
            roomId = "living",
            protocol = DeviceProtocol.MATTER,
            isOn = true,
            isHighRisk = true
        ))
        val result = engine.runSafetyCheck()
        assertFalse(result.isSafe)
        assertTrue(result.issues.any { it.type == IssueType.HIGH_RISK_PLUG })
    }

    @Test
    fun `test energy summary`() {
        engine.addRoom(Room(id = "living", name = "Living Room"))
        engine.addDevice(HomeDevice(
            id = "light-1",
            name = "Light",
            type = DeviceType.LIGHT,
            roomId = "living",
            protocol = DeviceProtocol.MATTER,
            isOn = true,
            powerWatts = 60.0
        ))
        val summary = engine.getEnergySummary()
        assertEquals(1, summary.activeDevices)
        assertEquals(60.0, summary.totalWatts)
    }
}
