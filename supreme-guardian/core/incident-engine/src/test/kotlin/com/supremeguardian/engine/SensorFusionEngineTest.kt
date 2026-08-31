package com.supremeguardian.engine

import com.supremeguardian.core.building.ZoneId
import com.supremeguardian.core.sensor.SensorObservation
import com.supremeguardian.core.sensor.SensorType
import com.supremeguardian.core.sensor.SensorId
import com.supremeguardian.core.sensor.Confidence
import com.supremeguardian.core.sensor.ObservationAuthority
import com.supremeguardian.core.shared.*
import com.supremeguardian.core.thermal.CameraId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class SensorFusionEngineTest {

    private val fusionEngine = DeterministicSensorFusion()

    @Test
    fun `test empty observations returns low confidence`() {
        val result = fusionEngine.fuse(emptyList())

        assertEquals(0.0, result.fireConfidence, "Empty observations should have 0 fire confidence")
        assertEquals("NORMAL", result.recommendedState, "Empty observations should recommend NORMAL")
    }

    @Test
    fun `test single thermal observation with low temperature`() {
        val observations = listOf(
            SensorObservation(
                sensorId = SensorId("thermal-1"),
                sensorType = SensorType.THERMAL_CAMERA,
                zoneId = ZoneId("zone-1"),
                timestamp = GuardianTimestamp(),
                readings = mapOf("max_celsius" to 25.0),
                confidence = Confidence(0.9, ObservationAuthority.HARDWARE_MEASURED),
                authority = ObservationAuthority.HARDWARE_MEASURED
            )
        )

        val result = fusionEngine.fuse(observations)

        assertEquals("NORMAL", result.recommendedState, "Low temperature should be NORMAL")
        assertTrue(result.fireConfidence < 0.2, "Low temperature should have low fire confidence")
    }

    @Test
    fun `test single thermal observation with high temperature`() {
        val observations = listOf(
            SensorObservation(
                sensorId = SensorId("thermal-1"),
                sensorType = SensorType.THERMAL_CAMERA,
                zoneId = ZoneId("zone-1"),
                timestamp = GuardianTimestamp(),
                readings = mapOf("max_celsius" to 150.0),
                confidence = Confidence(0.9, ObservationAuthority.HARDWARE_MEASURED),
                authority = ObservationAuthority.HARDWARE_MEASURED
            )
        )

        val result = fusionEngine.fuse(observations)

        assertEquals("CONFIRMED_INCIDENT", result.recommendedState, "High temperature should be CONFIRMED_INCIDENT")
        assertTrue(result.fireConfidence >= 0.4, "High temperature should have high fire confidence")
    }

    @Test
    fun `test multi-sensor confirmation increases confidence`() {
        val observations = listOf(
            SensorObservation(
                sensorId = SensorId("thermal-1"),
                sensorType = SensorType.THERMAL_CAMERA,
                zoneId = ZoneId("zone-1"),
                timestamp = GuardianTimestamp(),
                readings = mapOf("max_celsius" to 80.0),
                confidence = Confidence(0.9, ObservationAuthority.HARDWARE_MEASURED),
                authority = ObservationAuthority.HARDWARE_MEASURED
            ),
            SensorObservation(
                sensorId = SensorId("smoke-1"),
                sensorType = SensorType.SMOKE_DETECTOR,
                zoneId = ZoneId("zone-1"),
                timestamp = GuardianTimestamp(),
                readings = mapOf("smoke_level" to 0.5),
                confidence = Confidence(0.8, ObservationAuthority.HARDWARE_MEASURED),
                authority = ObservationAuthority.HARDWARE_MEASURED
            )
        )

        val result = fusionEngine.fuse(observations)

        assertTrue(result.fireConfidence > 0.4, "Multi-sensor should increase confidence")
        assertTrue(result.contributingSensors.size >= 2, "Should have multiple contributing sensors")
    }

    @Test
    fun `test smoke detection alone increases anomaly score`() {
        val observations = listOf(
            SensorObservation(
                sensorId = SensorId("smoke-1"),
                sensorType = SensorType.SMOKE_DETECTOR,
                zoneId = ZoneId("zone-1"),
                timestamp = GuardianTimestamp(),
                readings = mapOf("smoke_level" to 0.5),
                confidence = Confidence(0.8, ObservationAuthority.HARDWARE_MEASURED),
                authority = ObservationAuthority.HARDWARE_MEASURED
            )
        )

        val result = fusionEngine.fuse(observations)

        assertTrue(result.anomalyScore > 0.0, "Smoke detection should increase anomaly score")
    }

    @Test
    fun `test rate of rise increases fire confidence`() {
        val observations = listOf(
            SensorObservation(
                sensorId = SensorId("thermal-1"),
                sensorType = SensorType.THERMAL_CAMERA,
                zoneId = ZoneId("zone-1"),
                timestamp = GuardianTimestamp(),
                readings = mapOf(
                    "max_celsius" to 80.0,
                    "rate_of_rise" to 25.0
                ),
                confidence = Confidence(0.9, ObservationAuthority.HARDWARE_MEASURED),
                authority = ObservationAuthority.HARDWARE_MEASURED
            )
        )

        val result = fusionEngine.fuse(observations)

        assertTrue(result.fireConfidence > 0.3, "Rate of rise should increase fire confidence")
        assertTrue(result.reasoning.any { it.contains("rise") }, "Should include rate of rise in reasoning")
    }

    @Test
    fun `test observations are grouped by zone`() {
        val observations = listOf(
            SensorObservation(
                sensorId = SensorId("thermal-1"),
                sensorType = SensorType.THERMAL_CAMERA,
                zoneId = ZoneId("zone-1"),
                timestamp = GuardianTimestamp(),
                readings = mapOf("max_celsius" to 150.0),
                confidence = Confidence(0.9, ObservationAuthority.HARDWARE_MEASURED),
                authority = ObservationAuthority.HARDWARE_MEASURED
            ),
            SensorObservation(
                sensorId = SensorId("thermal-2"),
                sensorType = SensorType.THERMAL_CAMERA,
                zoneId = ZoneId("zone-2"),
                timestamp = GuardianTimestamp(),
                readings = mapOf("max_celsius" to 25.0),
                confidence = Confidence(0.9, ObservationAuthority.HARDWARE_MEASURED),
                authority = ObservationAuthority.HARDWARE_MEASURED
            )
        )

        val result = fusionEngine.fuse(observations)

        // Should return highest risk zone (zone-1 with 150°C)
        assertEquals(ZoneId("zone-1"), result.zoneId, "Should return highest risk zone")
    }

    @Test
    fun `test getFireConfidence for specific zone`() {
        val observations = listOf(
            SensorObservation(
                sensorId = SensorId("thermal-1"),
                sensorType = SensorType.THERMAL_CAMERA,
                zoneId = ZoneId("zone-1"),
                timestamp = GuardianTimestamp(),
                readings = mapOf("max_celsius" to 100.0),
                confidence = Confidence(0.9, ObservationAuthority.HARDWARE_MEASURED),
                authority = ObservationAuthority.HARDWARE_MEASURED
            )
        )

        val confidence = fusionEngine.getFireConfidence(ZoneId("zone-1"), observations)

        assertTrue(confidence > 0.0, "Should return non-zero confidence for zone with high temperature")
    }

    @Test
    fun `test isMultiSensorConfirmed`() {
        val observations = listOf(
            SensorObservation(
                sensorId = SensorId("thermal-1"),
                sensorType = SensorType.THERMAL_CAMERA,
                zoneId = ZoneId("zone-1"),
                timestamp = GuardianTimestamp(),
                readings = mapOf("max_celsius" to 80.0),
                confidence = Confidence(0.9, ObservationAuthority.HARDWARE_MEASURED),
                authority = ObservationAuthority.HARDWARE_MEASURED
            ),
            SensorObservation(
                sensorId = SensorId("smoke-1"),
                sensorType = SensorType.SMOKE_DETECTOR,
                zoneId = ZoneId("zone-1"),
                timestamp = GuardianTimestamp(),
                readings = mapOf("smoke_level" to 0.5),
                confidence = Confidence(0.8, ObservationAuthority.HARDWARE_MEASURED),
                authority = ObservationAuthority.HARDWARE_MEASURED
            )
        )

        val confirmed = fusionEngine.isMultiSensorConfirmed(ZoneId("zone-1"), observations)

        assertTrue(confirmed, "Should be confirmed with 2+ sensors")
    }

    @Test
    fun `test not multi-sensor confirmed with single sensor`() {
        val observations = listOf(
            SensorObservation(
                sensorId = SensorId("thermal-1"),
                sensorType = SensorType.THERMAL_CAMERA,
                zoneId = ZoneId("zone-1"),
                timestamp = GuardianTimestamp(),
                readings = mapOf("max_celsius" to 80.0),
                confidence = Confidence(0.9, ObservationAuthority.HARDWARE_MEASURED),
                authority = ObservationAuthority.HARDWARE_MEASURED
            )
        )

        val confirmed = fusionEngine.isMultiSensorConfirmed(ZoneId("zone-1"), observations)

        assertFalse(confirmed, "Should not be confirmed with single sensor")
    }
}
