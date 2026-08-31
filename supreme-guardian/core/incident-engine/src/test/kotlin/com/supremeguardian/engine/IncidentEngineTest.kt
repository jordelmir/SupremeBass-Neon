package com.supremeguardian.engine

import com.supremeguardian.core.building.ZoneId
import com.supremeguardian.core.incident.IncidentState
import com.supremeguardian.core.sensor.SensorType
import com.supremeguardian.core.shared.*
import com.supremeguardian.core.thermal.CameraId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.Instant

class IncidentEngineTest {

    private val rules = FireDetectionRules()
    private val engine = IncidentEngine(rules)

    @Test
    fun `test initial state is NORMAL`() {
        val zoneId = ZoneId("zone-1")
        val incident = engine.processObservation(
            sensorId = com.supremeguardian.core.sensor.SensorId("sensor-1"),
            sensorType = SensorType.THERMAL_CAMERA,
            zoneId = zoneId,
            confidence = 0.5,
            readings = mapOf("max_celsius" to 25.0),
            reason = "Normal temperature"
        )

        // Should not create incident for normal temperature
        assertNull(incident, "Should not create incident for normal temperature")
    }

    @Test
    fun `test WATCH state on elevated temperature`() {
        val zoneId = ZoneId("zone-1")
        val incident = engine.processObservation(
            sensorId = com.supremeguardian.core.sensor.SensorId("sensor-1"),
            sensorType = SensorType.THERMAL_CAMERA,
            zoneId = zoneId,
            confidence = 0.8,
            readings = mapOf("max_celsius" to 55.0),
            reason = "Elevated temperature detected"
        )

        // Should create incident in WATCH state
        assertNotNull(incident, "Should create incident for elevated temperature")
        assertEquals(IncidentState.WATCH, incident?.state, "Should be in WATCH state")
    }

    @Test
    fun `test THERMAL_ANOMALY on higher temperature`() {
        val zoneId = ZoneId("zone-1")
        val incident = engine.processObservation(
            sensorId = com.supremeguardian.core.sensor.SensorId("sensor-1"),
            sensorType = SensorType.THERMAL_CAMERA,
            zoneId = zoneId,
            confidence = 0.9,
            readings = mapOf("max_celsius" to 75.0),
            reason = "High temperature detected"
        )

        assertNotNull(incident, "Should create incident for high temperature")
        assertEquals(IncidentState.THERMAL_ANOMALY, incident?.state, "Should be in THERMAL_ANOMALY state")
    }

    @Test
    fun `test SUSPECT on very high temperature`() {
        val zoneId = ZoneId("zone-1")
        val incident = engine.processObservation(
            sensorId = com.supremeguardian.core.sensor.SensorId("sensor-1"),
            sensorType = SensorType.THERMAL_CAMERA,
            zoneId = zoneId,
            confidence = 0.95,
            readings = mapOf("max_celsius" to 95.0),
            reason = "Very high temperature detected"
        )

        assertNotNull(incident, "Should create incident for very high temperature")
        assertEquals(IncidentState.SUSPECT, incident?.state, "Should be in SUSPECT state")
    }

    @Test
    fun `test CONFIRMED_INCIDENT on critical temperature`() {
        val zoneId = ZoneId("zone-1")
        val incident = engine.processObservation(
            sensorId = com.supremeguardian.core.sensor.SensorId("sensor-1"),
            sensorType = SensorType.THERMAL_CAMERA,
            zoneId = zoneId,
            confidence = 0.99,
            readings = mapOf("max_celsius" to 150.0),
            reason = "Critical temperature detected"
        )

        assertNotNull(incident, "Should create incident for critical temperature")
        assertEquals(IncidentState.CONFIRMED_INCIDENT, incident?.state, "Should be in CONFIRMED_INCIDENT state")
    }

    @Test
    fun `test rate of rise triggers higher state`() {
        val zoneId = ZoneId("zone-1")
        val incident = engine.processObservation(
            sensorId = com.supremeguardian.core.sensor.SensorId("sensor-1"),
            sensorType = SensorType.THERMAL_CAMERA,
            zoneId = zoneId,
            confidence = 0.85,
            readings = mapOf(
                "max_celsius" to 80.0,
                "rate_of_rise" to 25.0
            ),
            reason = "Rapid temperature rise detected"
        )

        assertNotNull(incident, "Should create incident for rapid temperature rise")
        // Rate of rise should push state higher
        assertTrue(
            incident?.state?.ordinal ?: 0 >= IncidentState.SUSPECT.ordinal,
            "Rate of rise should push state to SUSPECT or higher"
        )
    }

    @Test
    fun `test smoke detection increases state`() {
        val zoneId = ZoneId("zone-1")
        val incident = engine.processObservation(
            sensorId = com.supremeguardian.core.sensor.SensorId("sensor-1"),
            sensorType = SensorType.SMOKE_DETECTOR,
            zoneId = zoneId,
            confidence = 0.9,
            readings = mapOf("smoke_level" to 0.8),
            reason = "High smoke level detected"
        )

        assertNotNull(incident, "Should create incident for high smoke level")
        assertTrue(
            incident?.state?.ordinal ?: 0 >= IncidentState.SUSPECT.ordinal,
            "Smoke detection should increase state"
        )
    }

    @Test
    fun `test state transition records evidence`() {
        val zoneId = ZoneId("zone-1")

        // First observation - WATCH
        val incident1 = engine.processObservation(
            sensorId = com.supremeguardian.core.sensor.SensorId("sensor-1"),
            sensorType = SensorType.THERMAL_CAMERA,
            zoneId = zoneId,
            confidence = 0.8,
            readings = mapOf("max_celsius" to 55.0),
            reason = "Elevated temperature"
        )

        assertNotNull(incident1)
        assertEquals(IncidentState.WATCH, incident1?.state)
        assertTrue(incident1?.timeline?.isNotEmpty() ?: false, "Should have timeline entries")

        // Second observation - THERMAL_ANOMALY
        val incident2 = engine.processObservation(
            sensorId = com.supremeguardian.core.sensor.SensorId("sensor-1"),
            sensorType = SensorType.THERMAL_CAMERA,
            zoneId = zoneId,
            confidence = 0.9,
            readings = mapOf("max_celsius" to 75.0),
            reason = "High temperature"
        )

        assertNotNull(incident2)
        assertEquals(IncidentState.THERMAL_ANOMALY, incident2?.state)
        assertTrue(incident2?.timeline?.size ?: 0 >= 2, "Should have multiple timeline entries")
    }

    @Test
    fun `test incident duration is tracked`() {
        val zoneId = ZoneId("zone-1")
        val incident = engine.processObservation(
            sensorId = com.supremeguardian.core.sensor.SensorId("sensor-1"),
            sensorType = SensorType.THERMAL_CAMERA,
            zoneId = zoneId,
            confidence = 0.8,
            readings = mapOf("max_celsius" to 55.0),
            reason = "Elevated temperature"
        )

        assertNotNull(incident)
        assertTrue(incident?.durationMs ?: 0 >= 0, "Duration should be non-negative")
    }
}
