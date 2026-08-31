package com.supremeguardian.core.thermal

import com.supremeguardian.core.shared.*
import kotlinx.coroutines.flow.Flow

/**
 * Thermal camera adapter interface.
 *
 * Vendor-agnostic abstraction for thermal cameras.
 * Each vendor (FLIR, Axis, Hikvision) implements this interface.
 *
 * This allows Supreme Guardian to work with any thermal camera
 * without knowing vendor-specific protocols.
 */
interface ThermalCameraAdapter {

    /**
     * Get camera capabilities.
     */
    suspend fun capabilities(): ThermalCapabilities

    /**
     * Stream thermal frames (radiometric data).
     * Returns a Flow of ThermalFrame objects.
     */
    fun thermalFrames(): Flow<ThermalFrame>

    /**
     * Stream thermal events (threshold crossings, hotspots, etc.)
     */
    fun events(): Flow<ThermalEvent>

    /**
     * Get temperature measurements for specific regions.
     */
    suspend fun measurements(regions: List<String>? = null): List<TemperatureMeasurement>

    /**
     * Get camera health status.
     */
    suspend fun health(): CameraHealth

    /**
     * Get camera identification.
     */
    fun getCameraId(): CameraId

    /**
     * Get camera zone assignment.
     */
    fun getZoneId(): ZoneId

    /**
     * Check if camera is currently reachable.
     */
    suspend fun isReachable(): Boolean
}

/**
 * Thermal frame — a single radiometric image.
 */
data class ThermalFrame(
    val cameraId: CameraId,
    val zoneId: ZoneId,
    val timestamp: GuardianTimestamp,
    val width: Int,
    val height: Int,
    val pixelData: FloatArray, // Temperature in Celsius per pixel
    val minCelsius: Double,
    val maxCelsius: Double,
    val averageCelsius: Double,
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * Get temperature at pixel coordinates.
     */
    fun getTemperatureAt(x: Int, y: Int): Double? {
        if (x < 0 || x >= width || y < 0 || y >= height) return null
        return pixelData[y * width + x].toDouble()
    }

    /**
     * Find hottest pixel.
     */
    fun findHottestPixel(): Triple<Int, Int, Double> {
        var maxTemp = Double.MIN_VALUE
        var maxX = 0
        var maxY = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                val temp = pixelData[y * width + x].toDouble()
                if (temp > maxTemp) {
                    maxTemp = temp
                    maxX = x
                    maxY = y
                }
            }
        }
        return Triple(maxX, maxY, maxTemp)
    }

    /**
     * Calculate rate of rise from previous frame.
     */
    fun calculateRateOfRise(previousFrame: ThermalFrame): Double? {
        if (previousFrame.width != width || previousFrame.height != height) return null
        val timeDiff = timestamp.elapsedSince(previousFrame.timestamp) / 1000.0 // seconds
        if (timeDiff <= 0) return null
        val tempDiff = maxCelsius - previousFrame.maxCelsius
        return tempDiff / timeDiff * 60.0 // degrees per minute
    }
}

/**
 * Thermal camera adapter registry — manages multiple cameras.
 */
class ThermalCameraRegistry {
    private val adapters = mutableMapOf<CameraId, ThermalCameraAdapter>()

    fun register(camera: ThermalCameraAdapter) {
        adapters[camera.getCameraId()] = camera
    }

    fun unregister(cameraId: CameraId) {
        adapters.remove(cameraId)
    }

    fun getAdapter(cameraId: CameraId): ThermalCameraAdapter? = adapters[cameraId]

    fun getAllAdapters(): List<ThermalCameraAdapter> = adapters.values.toList()

    fun getAdaptersForZone(zoneId: ZoneId): List<ThermalCameraAdapter> {
        return adapters.values.filter { it.getZoneId() == zoneId }
    }
}
