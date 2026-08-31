package com.supremeguardian.core.sensor

import com.supremeguardian.core.building.ZoneId
import com.supremeguardian.core.shared.*

/**
 * Sensor observation — generic sensor data from any sensor type.
 *
 * This is the base type for all sensor data before it's processed
 * into specific domain objects (ThermalObservation, etc.)
 */
data class SensorObservation(
    val sensorId: SensorId,
    val sensorType: SensorType,
    val zoneId: ZoneId,
    val timestamp: GuardianTimestamp,
    val readings: Map<String, Double>,
    val confidence: Confidence,
    val authority: ObservationAuthority
)

enum class SensorType {
    THERMAL_CAMERA,
    RGB_CAMERA,
    SMOKE_DETECTOR,
    HEAT_DETECTOR,
    GAS_DETECTOR,
    CO_DETECTOR,
    FLAME_DETECTOR,
    ACOUSTIC_SENSOR,
    ELECTRICAL_SENSOR,
    TEMPERATURE_SENSOR,
    HUMIDITY_SENSOR,
    MOTION_SENSOR,
    DOOR_WINDOW_SENSOR,
    PRESSURE_SENSOR,
    WATER_FLOW_SENSOR,
    VOLTAGE_SENSOR,
    CURRENT_SENSOR,
    POWER_SENSOR
}

/**
 * Sensor fusion result — combined reading from multiple sensors.
 */
data class FusionResult(
    val zoneId: ZoneId,
    val timestamp: GuardianTimestamp,
    val contributingSensors: List<SensorId>,
    val fusedReading: Map<String, Double>,
    val fireConfidence: Double, // 0.0 to 1.0
    val anomalyScore: Double, // 0.0 to 1.0
    val recommendedState: String, // suggested incident state
    val reasoning: List<String> // explanation of fusion result
)

/**
 * Sensor fusion engine interface.
 */
interface SensorFusionEngine {
    /**
     * Fuse observations from multiple sensors in a zone.
     */
    fun fuse(observations: List<SensorObservation>): FusionResult

    /**
     * Get fire confidence from all available data.
     */
    fun getFireConfidence(zoneId: ZoneId, observations: List<SensorObservation>): Double

    /**
     * Check if anomaly is confirmed by multiple independent sensors.
     */
    fun isMultiSensorConfirmed(zoneId: ZoneId, observations: List<SensorObservation>): Boolean
}
