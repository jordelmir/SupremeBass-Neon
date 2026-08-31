package com.supremeguardian.edge

import com.supremeguardian.core.building.ZoneId
import com.supremeguardian.core.sensor.SensorType
import com.supremeguardian.core.shared.*
import com.supremeguardian.core.thermal.CameraId
import com.supremeguardian.core.thermal.ThermalCameraAdapter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Device Manager — discovers and manages physical devices.
 *
 * Responsibilities:
 *   1. Discover cameras and sensors on the network
 *   2. Maintain connection to each device
 *   3. Track device health
 *   4. Route frames from devices to Edge Agent
 *   5. Handle disconnections gracefully
 */
class DeviceManager(
    private val edgeAgent: EdgeAgent
) {
    companion object {
        const val DISCOVERY_INTERVAL_MS = 60_000L // Discover every 60s
        const val HEALTH_CHECK_INTERVAL_MS = 10_000L // Check health every 10s
        const val RECONNECT_DELAY_MS = 5_000L
        const val MAX_RECONNECT_ATTEMPTS = 5
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val running = AtomicBoolean(false)

    // Discovered devices
    private val cameras = ConcurrentHashMap<CameraId, DeviceInfo>()
    private val sensors = ConcurrentHashMap<SensorId, DeviceInfo>()

    // Active connections
    private val activeConnections = ConcurrentHashMap<String, Job>()

    // State
    private val _deviceState = MutableStateFlow(DeviceManagerState())
    val deviceState: StateFlow<DeviceManagerState> = _deviceState.asStateFlow()

    /**
     * Start device manager.
     */
    fun start() {
        if (running.getAndSet(true)) return

        // Start discovery loop
        scope.launch {
            while (running.get()) {
                try {
                    discoverDevices()
                } catch (e: Exception) {
                    // Discovery failure is non-fatal
                }
                delay(DISCOVERY_INTERVAL_MS)
            }
        }

        // Start health check loop
        scope.launch {
            while (running.get()) {
                try {
                    checkDeviceHealth()
                } catch (e: Exception) {
                    // Health check failure is non-fatal
                }
                delay(HEALTH_CHECK_INTERVAL_MS)
            }
        }
    }

    /**
     * Stop device manager.
     */
    fun stop() {
        if (!running.getAndSet(false)) return

        // Cancel all connections
        activeConnections.values.forEach { it.cancel() }
        activeConnections.clear()

        // Clear devices
        cameras.clear()
        sensors.clear()

        scope.cancel()
    }

    /**
     * Add a camera manually.
     */
    fun addCamera(camera: ThermalCameraAdapter, zoneId: ZoneId) {
        val cameraId = camera.cameraId
        cameras[cameraId] = DeviceInfo(
            id = cameraId.value,
            type = DeviceType.THERMAL_CAMERA,
            name = camera.info.name,
            zoneId = zoneId,
            status = DeviceStatus.CONNECTING,
            lastSeen = GuardianTimestamp()
        )

        // Start streaming from camera
        scope.launch {
            streamFromCamera(camera, zoneId)
        }

        updateState()
    }

    /**
     * Add a sensor manually.
     */
    fun addSensor(sensorId: SensorId, sensorType: SensorType, zoneId: ZoneId) {
        sensors[sensorId] = DeviceInfo(
            id = sensorId.value,
            type = DeviceType.SENSOR,
            name = "$sensorType sensor",
            zoneId = zoneId,
            status = DeviceStatus.CONNECTED,
            lastSeen = GuardianTimestamp()
        )

        updateState()
    }

    /**
     * Get all cameras.
     */
    fun getCameras(): Map<CameraId, DeviceInfo> = cameras.toMap()

    /**
     * Get all sensors.
     */
    fun getSensors(): Map<SensorId, DeviceInfo> = sensors.toMap()

    /**
     * Get devices for a zone.
     */
    fun getDevicesForZone(zoneId: ZoneId): List<DeviceInfo> {
        return cameras.values.filter { it.zoneId == zoneId } +
               sensors.values.filter { it.zoneId == zoneId }
    }

    private suspend fun discoverDevices() {
        // In a real implementation, this would scan the network for devices
        // For now, we just update the last seen time of existing devices
        cameras.forEach { (id, info) ->
            cameras[id] = info.copy(lastSeen = GuardianTimestamp())
        }
    }

    private suspend fun streamFromCamera(camera: ThermalCameraAdapter, zoneId: ZoneId) {
        val cameraId = camera.cameraId
        val connectionId = "camera-${cameraId.value}"

        // Cancel existing connection for this camera
        activeConnections[connectionId]?.cancel()

        val job = scope.launch {
            var reconnectAttempts = 0

            while (running.get() && isActive) {
                try {
                    // Connect to camera
                    if (camera.connect()) {
                        cameras[cameraId] = cameras[cameraId]!!.copy(
                            status = DeviceStatus.CONNECTED
                        )
                        reconnectAttempts = 0

                        // Stream frames
                        camera.getThermalFrame().collect { frame ->
                            edgeAgent.ingestThermalFrame(frame)

                            // Update last seen
                            cameras[cameraId] = cameras[cameraId]!!.copy(
                                lastSeen = GuardianTimestamp()
                            )
                        }
                    } else {
                        throw Exception("Failed to connect to camera ${camera.info.name}")
                    }
                } catch (e: Exception) {
                    cameras[cameraId] = cameras[cameraId]!!.copy(
                        status = DeviceStatus.DISCONNECTED
                    )

                    reconnectAttempts++
                    if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
                        cameras[cameraId] = cameras[cameraId]!!.copy(
                            status = DeviceStatus.FAILED
                        )
                        break
                    }

                    delay(RECONNECT_DELAY_MS * reconnectAttempts)
                }
            }
        }

        activeConnections[connectionId] = job
    }

    private suspend fun checkDeviceHealth() {
        val now = System.currentTimeMillis()

        // Check camera health
        cameras.forEach { (id, info) ->
            val lastSeenMs = info.lastSeen.toInstant()?.toEpochMilli() ?: 0
            if (now - lastSeenMs > HEALTH_CHECK_INTERVAL_MS * 3) {
                cameras[id] = info.copy(status = DeviceStatus.DISCONNECTED)
            }
        }

        updateState()
    }

    private fun updateState() {
        _deviceState.update { state ->
            state.copy(
                cameras = cameras.values.toList(),
                sensors = sensors.values.toList(),
                totalCameras = cameras.size,
                connectedCameras = cameras.values.count { it.status == DeviceStatus.CONNECTED },
                totalSensors = sensors.size,
                connectedSensors = sensors.values.count { it.status == DeviceStatus.CONNECTED }
            )
        }
    }
}

/**
 * Device manager state.
 */
data class DeviceManagerState(
    val cameras: List<DeviceInfo> = emptyList(),
    val sensors: List<DeviceInfo> = emptyList(),
    val totalCameras: Int = 0,
    val connectedCameras: Int = 0,
    val totalSensors: Int = 0,
    val connectedSensors: Int = 0
)

/**
 * Device info.
 */
data class DeviceInfo(
    val id: String,
    val type: DeviceType,
    val name: String,
    val zoneId: ZoneId,
    val status: DeviceStatus,
    val lastSeen: GuardianTimestamp
)

enum class DeviceType {
    THERMAL_CAMERA,
    SENSOR,
    SUPPRESSION_NODE,
    ACTUATOR
}

enum class DeviceStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    FAILED
}
