package com.supremeguardian.core.building

import com.supremeguardian.core.shared.*
import com.supremeguardian.core.thermal.ThermalCamera
import com.supremeguardian.core.sensor.SensorType

/**
 * Building digital twin — physical representation of a monitored facility.
 *
 * This is NOT a decorative 3D model.
 * This is a situational-awareness engine with:
 *   - Physical coordinates
 *   - Device locations
 *   - Zone boundaries
 *   - Camera FOVs
 *   - Sensor coverage
 *   - Real-time state
 */
data class Building(
    val id: BuildingId,
    val name: String,
    val address: String,
    val floors: List<Floor>,
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * Get all zones across all floors.
     */
    fun getAllZones(): List<Zone> = floors.flatMap { it.zones }

    /**
     * Find zone by ID.
     */
    fun findZone(zoneId: ZoneId): Zone? = getAllZones().find { it.id == zoneId }

    /**
     * Get all cameras across all zones.
     */
    fun getAllCameras(): List<ThermalCamera> = getAllZones().flatMap { it.cameras }

    /**
     * Get all sensors across all zones.
     */
    fun getAllSensors(): List<SensorPlacement> = getAllZones().flatMap { it.sensors }

    /**
     * Get all actuators across all zones.
     */
    fun getAllActuators(): List<ActuatorPlacement> = getAllZones().flatMap { it.actuators }
}

data class Floor(
    val id: FloorId,
    val level: Int, // -1 = basement, 0 = ground, 1+ = upper floors
    val name: String,
    val zones: List<Zone>,
    val svgPath: String? = null // SVG path for floor plan rendering
)

data class Zone(
    val id: ZoneId,
    val floorId: FloorId,
    val name: String,
    val boundaries: List<PhysicalCoordinates>, // Polygon vertices
    val cameras: List<ThermalCamera>,
    val sensors: List<SensorPlacement>,
    val actuators: List<ActuatorPlacement>,
    val riskLevel: RiskLevel,
    val suppressionType: String? // "water_mist", "acoustic", "sprinkler", null = none
) {
    /**
     * Check if a point is within this zone's boundaries.
     */
    fun containsPoint(x: Double, y: Double): Boolean {
        // Ray casting algorithm for point-in-polygon
        var inside = false
        val n = boundaries.size
        for (i in 0 until n) {
            val j = (i + n - 1) % n
            val xi = boundaries[i].xMeters
            val yi = boundaries[i].yMeters
            val xj = boundaries[j].xMeters
            val yj = boundaries[j].yMeters

            if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) {
                inside = !inside
            }
        }
        return inside
    }
}

data class SensorPlacement(
    val sensorId: SensorId,
    val type: SensorType,
    val location: PhysicalCoordinates,
    val coverageRadiusMeters: Double,
    val mountingHeightMeters: Double
)

data class ActuatorPlacement(
    val id: String,
    val type: String, // "water_mist_nozzle", "acoustic_transducer", "alarm", "valve"
    val location: PhysicalCoordinates,
    val controlledZones: List<ZoneId>,
    val healthStatus: String = "unknown"
)

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
    HAZARDOUS
}

/**
 * Real-time zone state — updated continuously.
 */
data class ZoneState(
    val zoneId: ZoneId,
    val timestamp: GuardianTimestamp,
    val maxTemperature: Double?,
    val minTemperature: Double?,
    val rateOfRise: Double?,
    val smokeLevel: Double?,
    val flameDetected: Boolean,
    val motionDetected: Boolean,
    val electricalAnomaly: Boolean,
    val activeIncidents: Int,
    val camerasOnline: Int,
    val camerasTotal: Int,
    val sensorsOnline: Int,
    val sensorsTotal: Int,
    val suppressionArmed: Boolean,
    val suppressionActive: Boolean
) {
    val isNormal: Boolean
        get() = !flameDetected && !electricalAnomaly && activeIncidents == 0 &&
                (maxTemperature ?: 0.0) < 60.0

    val isWatch: Boolean
        get() = !isNormal && !isCritical

    val isCritical: Boolean
        get() = flameDetected || activeIncidents > 0 ||
                (maxTemperature ?: 0.0) > 100.0 ||
                (rateOfRise ?: 0.0) > 10.0
}
