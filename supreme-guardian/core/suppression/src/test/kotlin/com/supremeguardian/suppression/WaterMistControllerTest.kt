package com.supremeguardian.suppression

import com.supremeguardian.core.building.ZoneId
import com.supremeguardian.core.incident.IncidentId
import com.supremeguardian.core.shared.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

class WaterMistControllerTest {

    private lateinit var controller: WaterMistController
    private lateinit var mockHardware: MockMistHardware

    @BeforeEach
    fun setup() {
        controller = WaterMistController()
        mockHardware = MockMistHardware()
        controller.setHardwareInterface(mockHardware)
        controller.start()
    }

    @Test
    fun `test activation with valid interlocks`() = runBlocking {
        // Set up valid state
        mockHardware.pressure = 3.5
        mockHardware.flowRate = 8.0
        mockHardware.temperature = 75.0

        val result = controller.activate(
            zoneId = ZoneId("zone-1"),
            incidentId = IncidentId("incident-1")
        )

        assertTrue(result.success, "Activation should succeed with valid interlocks")
        assertTrue(controller.isActive(), "Controller should be active")
    }

    @Test
    fun `test activation blocked by emergency stop`() = runBlocking {
        controller.emergencyStop()

        val result = controller.activate(
            zoneId = ZoneId("zone-1"),
            incidentId = IncidentId("incident-1")
        )

        assertFalse(result.success, "Activation should fail with emergency stop")
        assertTrue(result.failedInterlocks.contains("EMERGENCY_STOP"), "Should report emergency stop interlock")
    }

    @Test
    fun `test activation blocked by manual override`() = runBlocking {
        controller.setManualOverride(true)

        val result = controller.activate(
            zoneId = ZoneId("zone-1"),
            incidentId = IncidentId("incident-1")
        )

        assertFalse(result.success, "Activation should fail with manual override")
        assertTrue(result.failedInterlocks.contains("MANUAL_OVERRIDE"), "Should report manual override interlock")
    }

    @Test
    fun `test activation blocked by low pressure`() = runBlocking {
        mockHardware.pressure = 1.5 // Below threshold of 2.0

        val result = controller.activate(
            zoneId = ZoneId("zone-1"),
            incidentId = IncidentId("incident-1")
        )

        assertFalse(result.success, "Activation should fail with low pressure")
        assertTrue(result.failedInterlocks.any { it.contains("LOW_PRESSURE") }, "Should report low pressure interlock")
    }

    @Test
    fun `test activation blocked by low temperature`() = runBlocking {
        mockHardware.temperature = 50.0 // Below threshold of 60.0

        val result = controller.activate(
            zoneId = ZoneId("zone-1"),
            incidentId = IncidentId("incident-1")
        )

        assertFalse(result.success, "Activation should fail with low temperature")
        assertTrue(result.failedInterlocks.any { it.contains("TEMPERATURE_TOO_LOW") }, "Should report temperature interlock")
    }

    @Test
    fun `test deactivation`() = runBlocking {
        // First activate
        mockHardware.pressure = 3.5
        mockHardware.flowRate = 8.0
        mockHardware.temperature = 75.0

        controller.activate(
            zoneId = ZoneId("zone-1"),
            incidentId = IncidentId("incident-1")
        )

        // Then deactivate
        val result = controller.deactivate(ZoneId("zone-1"), "Manual deactivation")

        assertTrue(result.success, "Deactivation should succeed")
        assertFalse(controller.isActive(), "Controller should not be active after deactivation")
    }

    @Test
    fun `test emergency stop deactivates everything`() = runBlocking {
        // First activate
        mockHardware.pressure = 3.5
        mockHardware.flowRate = 8.0
        mockHardware.temperature = 75.0

        controller.activate(
            zoneId = ZoneId("zone-1"),
            incidentId = IncidentId("incident-1")
        )

        // Emergency stop
        val result = controller.emergencyStop()

        assertTrue(result.success, "Emergency stop should succeed")
        assertFalse(controller.isActive(), "Controller should not be active after emergency stop")
        assertTrue(controller.state.value.emergencyStop, "Emergency stop flag should be set")
    }

    @Test
    fun `test reset emergency stop`() = runBlocking {
        controller.emergencyStop()
        assertTrue(controller.state.value.emergencyStop, "Emergency stop should be set")

        controller.resetEmergencyStop()
        assertFalse(controller.state.value.emergencyStop, "Emergency stop should be reset")
    }

    @Test
    fun `test hardware state updates`() = runBlocking {
        mockHardware.pressure = 4.0
        mockHardware.flowRate = 10.0
        mockHardware.temperature = 80.0

        // Wait for hardware update
        kotlinx.coroutines.delay(1100)

        assertEquals(4.0, controller.getPressure(), "Pressure should be updated")
        assertEquals(10.0, controller.getFlowRate(), "Flow rate should be updated")
        assertEquals(80.0, controller.state.value.temperature, "Temperature should be updated")
    }

    @Test
    fun `test cooldown period`() = runBlocking {
        // Set up valid state
        mockHardware.pressure = 3.5
        mockHardware.flowRate = 8.0
        mockHardware.temperature = 75.0

        // First activation
        controller.activate(
            zoneId = ZoneId("zone-1"),
            incidentId = IncidentId("incident-1")
        )

        // Deactivate
        controller.deactivate(ZoneId("zone-1"), "Test")

        // Try to activate again immediately
        val result = controller.activate(
            zoneId = ZoneId("zone-1"),
            incidentId = IncidentId("incident-2")
        )

        assertFalse(result.success, "Activation should fail during cooldown")
        assertTrue(result.failedInterlocks.contains("COOLDOWN"), "Should report cooldown interlock")
    }
}

/**
 * Mock hardware interface for testing.
 */
class MockMistHardware : MistHardwareInterface {
    var pressure: Double = 3.5
    var flowRate: Double = 8.0
    var temperature: Double = 75.0
    var valveOpen: Boolean = false
    var activated: Boolean = false

    override suspend fun activateMist(zoneId: String): Boolean {
        activated = true
        valveOpen = true
        return true
    }

    override suspend fun deactivateMist(zoneId: String): Boolean {
        activated = false
        valveOpen = false
        return true
    }

    override suspend fun emergencyStop(): Boolean {
        activated = false
        valveOpen = false
        return true
    }

    override suspend fun readPressure(): Double = pressure
    override suspend fun readFlowRate(): Double = flowRate
    override suspend fun readTemperature(): Double = temperature
    override suspend fun isValveOpen(): Boolean = valveOpen
}
