package com.supreme.camera

import com.supreme.core.*
import kotlinx.coroutines.flow.*
import java.time.Instant

/**
 * Supreme Camera Hub — manage existing cameras (ONVIF, RTSP, USB, phone).
 *
 * You don't need to sell cameras.
 * You sell intelligence over existing cameras.
 */

class CameraHubEngine {

    private val cameras = mutableMapOf<String, CameraInfo>()
    private val _cameraList = MutableStateFlow<List<CameraInfo>>(emptyList())
    val cameraList: StateFlow<List<CameraInfo>> = _cameraList.asStateFlow()

    /**
     * Add a camera.
     */
    fun addCamera(camera: CameraInfo) {
        cameras[camera.id] = camera
        _cameraList.value = cameras.values.toList()
    }

    /**
     * Remove a camera.
     */
    fun removeCamera(cameraId: String) {
        cameras.remove(cameraId)
        _cameraList.value = cameras.values.toList()
    }

    /**
     * Get camera by ID.
     */
    fun getCamera(cameraId: String): CameraInfo? = cameras[cameraId]

    /**
     * Get all cameras for a zone.
     */
    fun getCamerasForZone(zone: String): List<CameraInfo> {
        return cameras.values.filter { it.zone == zone }
    }

    /**
     * Analyze a camera frame for events.
     * Returns NOT_IMPLEMENTED until real CV pipeline is connected.
     */
    suspend fun analyzeFrame(
        cameraId: String,
        frameBytes: ByteArray
    ): CameraAnalysisResult {
        val camera = cameras[cameraId] ?: return CameraAnalysisResult(
            cameraId = cameraId,
            timestamp = Instant.now(),
            events = emptyList(),
            error = "Camera not found"
        )

        // NOT_IMPLEMENTED: No CV pipeline connected
        return CameraAnalysisResult(
            cameraId = cameraId,
            timestamp = Instant.now(),
            events = emptyList(),
            error = "NOT_IMPLEMENTED: No CV pipeline connected"
        )
    }
}

data class CameraInfo(
    val id: String,
    val name: String,
    val protocol: CameraProtocol,
    val address: String,
    val port: Int = 554,
    val username: String? = null,
    val password: String? = null,
    val zone: String = "default",
    val capabilities: Set<CameraCapability> = emptySet(),
    val connected: Boolean = false
)

enum class CameraProtocol {
    ONVIF,
    RTSP,
    USB,
    PHONE_CAMERA,
    VendorSpecific
}

enum class CameraCapability {
    THERMAL,
    NIGHT_VISION,
    PTZ,
    AUDIO,
    MOTION_DETECTION,
    PEOPLE_DETECTION,
    VEHICLE_DETECTION,
    PACKAGE_DETECTION,
    PET_DETECTION,
    SMOKE_DETECTION,
    FLAME_DETECTION,
    WATER_DETECTION
}

sealed class CameraEvent {
    data class MotionDetected(
        val cameraId: String,
        val timestamp: Instant,
        val confidence: Double,
        val zone: String
    ) : CameraEvent()

    data class PersonDetected(
        val cameraId: String,
        val timestamp: Instant,
        val confidence: Double,
        val zone: String
    ) : CameraEvent()

    data class VehicleDetected(
        val cameraId: String,
        val timestamp: Instant,
        val confidence: Double,
        val zone: String
    ) : CameraEvent()

    data class PackageDetected(
        val cameraId: String,
        val timestamp: Instant,
        val confidence: Double,
        val zone: String
    ) : CameraEvent()

    data class SmokeDetected(
        val cameraId: String,
        val timestamp: Instant,
        val confidence: Double,
        val zone: String
    ) : CameraEvent()

    data class DoorLeftOpen(
        val cameraId: String,
        val timestamp: Instant,
        val durationSeconds: Int,
        val zone: String
    ) : CameraEvent()

    data class LightLeftOn(
        val cameraId: String,
        val timestamp: Instant,
        val durationHours: Int,
        val zone: String
    ) : CameraEvent()
}

data class CameraAnalysisResult(
    val cameraId: String,
    val timestamp: Instant,
    val events: List<CameraEvent>,
    val error: String? = null
)
