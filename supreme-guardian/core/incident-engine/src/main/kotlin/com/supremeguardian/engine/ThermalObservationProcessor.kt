package com.supremeguardian.engine

import com.supremeguardian.core.building.ZoneId
import com.supremeguardian.core.sensor.*
import com.supremeguardian.core.shared.*
import com.supremeguardian.core.thermal.*

/**
 * Thermal Observation Processor — converts raw thermal data into domain observations.
 *
 * Responsibilities:
 *   1. Extract temperature measurements from thermal frames
 *   2. Detect hotspots and rate-of-rise
 *   3. Generate ThermalEvent for significant changes
 *   4. Convert to SensorObservation for fusion engine
 */
class ThermalObservationProcessor {

    private val previousFrames = mutableMapOf<CameraId, ThermalFrame>()
    private val rateOfRiseHistory = mutableMapOf<CameraId, MutableList<Pair<Long, Double>>>()

    companion object {
        const val RATE_OF_RISE_WINDOW_MS = 60_000L // 1 minute window
        const val MIN_RATE_OF_RISE_SAMPLES = 3
    }

    /**
     * Process a thermal frame and extract observations.
     */
    fun processFrame(frame: ThermalFrame): ThermalProcessingResult {
        val cameraId = frame.cameraId
        val zoneId = frame.zoneId

        // Extract measurements
        val measurements = extractMeasurements(frame)

        // Calculate rate of rise
        val previousFrame = previousFrames[cameraId]
        val rateOfRise = if (previousFrame != null) {
            frame.calculateRateOfRise(previousFrame)
        } else null

        // Update rate of rise history
        if (rateOfRise != null) {
            updateRateOfRiseHistory(cameraId, rateOfRise)
        }

        // Get smoothed rate of rise
        val smoothedROR = getSmoothedRateOfRise(cameraId)

        // Detect hotspot
        val hotspot = detectHotspot(frame)

        // Generate events
        val events = generateEvents(frame, measurements, rateOfRise, hotspot)

        // Store current frame for next comparison
        previousFrames[cameraId] = frame

        // Create SensorObservation for fusion engine
        val sensorObservation = SensorObservation(
            sensorId = SensorId(cameraId.value),
            sensorType = SensorType.THERMAL_CAMERA,
            zoneId = zoneId,
            timestamp = frame.timestamp,
            readings = buildMap {
                put("max_celsius", frame.maxCelsius)
                put("min_celsius", frame.minCelsius)
                put("average_celsius", frame.averageCelsius)
                smoothedROR?.let { put("rate_of_rise", it) }
                hotspot?.let { put("hotspot_detected", 1.0) }
            },
            confidence = Confidence(
                value = if (frame.maxCelsius > 0) 0.9 else 0.1,
                authority = ObservationAuthority.HARDWARE_MEASURED
            ),
            authority = ObservationAuthority.HARDWARE_MEASURED
        )

        return ThermalProcessingResult(
            frame = frame,
            measurements = measurements,
            rateOfRise = smoothedROR,
            hotspot = hotspot,
            events = events,
            sensorObservation = sensorObservation
        )
    }

    private fun extractMeasurements(frame: ThermalFrame): List<TemperatureMeasurement> {
        // Sample measurements at key points
        val measurements = mutableListOf<TemperatureMeasurement>()

        // Center point
        val centerX = frame.width / 2
        val centerY = frame.height / 2
        frame.getTemperatureAt(centerX, centerY)?.let { temp ->
            measurements.add(
                TemperatureMeasurement(
                    cameraId = frame.cameraId,
                    zoneId = frame.zoneId,
                    timestamp = frame.timestamp,
                    x = centerX.toDouble() / frame.width,
                    y = centerY.toDouble() / frame.height,
                    celsius = temp,
                    confidence = 0.9
                )
            )
        }

        // Hottest point
        val (hotX, hotY, hotTemp) = frame.findHottestPixel()
        measurements.add(
            TemperatureMeasurement(
                cameraId = frame.cameraId,
                zoneId = frame.zoneId,
                timestamp = frame.timestamp,
                x = hotX.toDouble() / frame.width,
                y = hotY.toDouble() / frame.height,
                celsius = hotTemp,
                confidence = 0.95
            )
        )

        return measurements
    }

    private fun detectHotspot(frame: ThermalFrame): HotspotInfo? {
        val (hotX, hotY, hotTemp) = frame.findHottestPixel()

        // Calculate average temperature
        val avgTemp = frame.averageCelsius

        // Hotspot is significant if much hotter than average
        val deltaFromAverage = hotTemp - avgTemp
        if (deltaFromAverage < 10.0) return null // Not significant

        // Find region around hotspot
        val regionSize = 20 // pixels
        val xMin = (hotX - regionSize).coerceAtLeast(0)
        val yMin = (hotY - regionSize).coerceAtLeast(0)
        val xMax = (hotX + regionSize).coerceAtMost(frame.width - 1)
        val yMax = (hotY + regionSize).coerceAtMost(frame.height - 1)

        return HotspotInfo(
            centerX = hotX,
            centerY = hotY,
            maxCelsius = hotTemp,
            deltaFromAverage = deltaFromAverage,
            region = listOf(
                Pair(xMin.toDouble() / frame.width, yMin.toDouble() / frame.height),
                Pair(xMax.toDouble() / frame.width, yMin.toDouble() / frame.height),
                Pair(xMax.toDouble() / frame.width, yMax.toDouble() / frame.height),
                Pair(xMin.toDouble() / frame.width, yMax.toDouble() / frame.height)
            )
        )
    }

    private fun updateRateOfRiseHistory(cameraId: CameraId, rateOfRise: Double) {
        val history = rateOfRiseHistory.getOrPut(cameraId) { mutableListOf() }
        val now = System.currentTimeMillis()
        history.add(Pair(now, rateOfRise))

        // Remove old entries outside the window
        history.removeAll { now - it.first > RATE_OF_RISE_WINDOW_MS }
    }

    private fun getSmoothedRateOfRise(cameraId: CameraId): Double? {
        val history = rateOfRiseHistory[cameraId] ?: return null
        if (history.size < MIN_RATE_OF_RISE_SAMPLES) return null

        // Simple moving average
        return history.map { it.second }.average()
    }

    private fun generateEvents(
        frame: ThermalFrame,
        measurements: List<TemperatureMeasurement>,
        rateOfRise: Double?,
        hotspot: HotspotInfo?
    ): List<ThermalEvent> {
        val events = mutableListOf<ThermalEvent>()

        // Temperature threshold events
        if (frame.maxCelsius >= 100.0) {
            events.add(
                ThermalEvent.TemperatureThresholdExceeded(
                    cameraId = frame.cameraId,
                    zoneId = frame.zoneId,
                    timestamp = frame.timestamp,
                    severity = if (frame.maxCelsius >= 150.0) EventSeverity.CRITICAL else EventSeverity.WARNING,
                    measuredCelsius = frame.maxCelsius,
                    thresholdCelsius = 100.0
                )
            )
        }

        // Rate of rise events
        if (rateOfRise != null && rateOfRise >= 10.0) {
            events.add(
                ThermalEvent.RateOfRiseDetected(
                    cameraId = frame.cameraId,
                    zoneId = frame.zoneId,
                    timestamp = frame.timestamp,
                    severity = if (rateOfRise >= 20.0) EventSeverity.CRITICAL else EventSeverity.WARNING,
                    rateCPerMinute = rateOfRise,
                    currentCelsius = frame.maxCelsius
                )
            )
        }

        // Hotspot events
        if (hotspot != null && hotspot.deltaFromAverage >= 20.0) {
            events.add(
                ThermalEvent.HotspotDetected(
                    cameraId = frame.cameraId,
                    zoneId = frame.zoneId,
                    timestamp = frame.timestamp,
                    severity = if (hotspot.deltaFromAverage >= 50.0) EventSeverity.CRITICAL else EventSeverity.WARNING,
                    hotspot = com.supremeguardian.core.thermal.Polygon(hotspot.region),
                    maxCelsius = hotspot.maxCelsius
                )
            )
        }

        return events
    }

    /**
     * Get historical rate of rise for a camera.
     */
    fun getRateOfRiseHistory(cameraId: CameraId): List<Pair<Long, Double>> {
        return rateOfRiseHistory[cameraId]?.toList() ?: emptyList()
    }

    /**
     * Clear history for a camera (e.g., after disconnection).
     */
    fun clearHistory(cameraId: CameraId) {
        previousFrames.remove(cameraId)
        rateOfRiseHistory.remove(cameraId)
    }
}

/**
 * Result of processing a thermal frame.
 */
data class ThermalProcessingResult(
    val frame: ThermalFrame,
    val measurements: List<TemperatureMeasurement>,
    val rateOfRise: Double?,
    val hotspot: HotspotInfo?,
    val events: List<ThermalEvent>,
    val sensorObservation: SensorObservation
)

/**
 * Hotspot information.
 */
data class HotspotInfo(
    val centerX: Int,
    val centerY: Int,
    val maxCelsius: Double,
    val deltaFromAverage: Double,
    val region: List<Pair<Double, Double>>
)
