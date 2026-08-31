package com.supremeguardian.core.thermal

import com.supremeguardian.core.shared.CameraId
import com.supremeguardian.core.shared.PhysicalCoordinates
import com.supremeguardian.core.shared.ZoneId

/**
 * Thermal camera placement in the building.
 */
data class ThermalCamera(
    val cameraId: CameraId,
    val zoneId: ZoneId,
    val model: String,
    val location: PhysicalCoordinates,
    val mountingHeightMeters: Double,
    val fieldOfView: FieldOfView,
    val capabilities: ThermalCapabilities
)

data class FieldOfView(
    val horizontalDegrees: Double,
    val verticalDegrees: Double,
    val direction: Double // Heading in degrees (0 = North)
)
