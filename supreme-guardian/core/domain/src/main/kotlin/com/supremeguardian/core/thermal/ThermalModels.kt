package com.supremeguardian.core.thermal

import com.supremeguardian.core.building.ZoneId
import com.supremeguardian.core.shared.*
import java.time.Instant

/**
 * Thermal observation from a camera or sensor.
 *
 * This is the raw domain object for thermal data.
 * It does NOT assume a specific camera vendor or protocol.
 */
data class ThermalObservation(
    val cameraId: CameraId,
    val zoneId: ZoneId,
    val timestamp: GuardianTimestamp,

    // Temperature measurements (Celsius)
    val minCelsius: Double?,
    val maxCelsius: Double?,
    val averageCelsius: Double?,

    // Hotspot information
    val hotspot: Polygon?,

    // Rate of rise (critical for fire detection)
    val rateOfRiseCPerMinute: Double?,

    // Confidence and authority
    val confidence: Confidence,
    val authority: ObservationAuthority,

    // Raw frame reference (for evidence)
    val frameRef: String? = null
)

/**
 * 2D polygon for hotspot region.
 */
data class Polygon(
    val points: List<Pair<Double, Double>>
) {
    init {
        require(points.size >= 3) { "Polygon requires at least 3 points" }
    }
}

/**
 * Temperature measurement at a specific point.
 */
data class TemperatureMeasurement(
    val cameraId: CameraId,
    val zoneId: ZoneId,
    val timestamp: GuardianTimestamp,
    val x: Double,
    val y: Double,
    val celsius: Double,
    val confidence: Double
)

/**
 * Camera capabilities — what can this camera provide?
 */
data class ThermalCapabilities(
    val resolutionWidth: Int,
    val resolutionHeight: Int,
    val hasRadiometricData: Boolean,
    val hasTemperatureMeasurements: Boolean,
    val hasRTSP: Boolean,
    val hasMQTT: Boolean,
    val hasREST: Boolean,
    val hasModbus: Boolean,
    val hasDigitalIO: Boolean,
    val temperatureRange: Pair<Double, Double>, // min/max Celsius
    val accuracyCelsius: Double,
    val refreshRateHz: Double
)

/**
 * Camera health status.
 */
data class CameraHealth(
    val cameraId: CameraId,
    val timestamp: GuardianTimestamp,
    val isOnline: Boolean,
    val temperatureCelsius: Double?,
    val uptimeMs: Long?,
    val lastFrameAge: Long?, // ms since last frame
    val errorCount: Int,
    val warnings: List<String>
)

/**
 * Thermal event — something noteworthy happened.
 */
sealed class ThermalEvent {
    abstract val cameraId: CameraId
    abstract val zoneId: ZoneId
    abstract val timestamp: GuardianTimestamp
    abstract val severity: EventSeverity

    data class TemperatureThresholdExceeded(
        override val cameraId: CameraId,
        override val zoneId: ZoneId,
        override val timestamp: GuardianTimestamp,
        override val severity: EventSeverity,
        val measuredCelsius: Double,
        val thresholdCelsius: Double
    ) : ThermalEvent()

    data class RateOfRiseDetected(
        override val cameraId: CameraId,
        override val zoneId: ZoneId,
        override val timestamp: GuardianTimestamp,
        override val severity: EventSeverity,
        val rateCPerMinute: Double,
        val currentCelsius: Double
    ) : ThermalEvent()

    data class HotspotDetected(
        override val cameraId: CameraId,
        override val zoneId: ZoneId,
        override val timestamp: GuardianTimestamp,
        override val severity: EventSeverity,
        val hotspot: Polygon,
        val maxCelsius: Double
    ) : ThermalEvent()

    data class CameraOffline(
        override val cameraId: CameraId,
        override val zoneId: ZoneId,
        override val timestamp: GuardianTimestamp,
        override val severity: EventSeverity
    ) : ThermalEvent()

    data class CameraRecovered(
        override val cameraId: CameraId,
        override val zoneId: ZoneId,
        override val timestamp: GuardianTimestamp,
        override val severity: EventSeverity
    ) : ThermalEvent()
}

enum class EventSeverity {
    INFO,
    WATCH,
    WARNING,
    CRITICAL,
    EMERGENCY
}
