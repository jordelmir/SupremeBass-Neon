package com.supreme.core.device

import com.supreme.core.*
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Supreme Device Abstraction Layer
 *
 * Every physical thing — a washing machine, a light bulb, a thermal camera, a BLE tag,
 * a water valve, a car OBD — implements this interface.
 *
 * This is the polymorphic gateway: Supreme doesn't care HOW a device works,
 * only WHAT it can do.
 */

/**
 * The core device interface.
 */
interface SupremeDevice {
    val identity: DeviceIdentity
    val capabilities: Set<Capability>
    val state: StateFlow<DeviceState>
    val observations: SharedFlow<Observation>
    val events: SharedFlow<DeviceEvent>

    suspend fun connect(): Boolean
    suspend fun disconnect()
    suspend fun observe(sensorType: SensorType): Observation?
    suspend fun execute(command: DeviceCommand): CommandResult
    suspend fun getHealth(): DeviceHealth
    fun isAvailable(): Boolean
}

/**
 * Device identity.
 */
data class DeviceIdentity(
    val id: DeviceId,
    val name: String,
    val type: DeviceType,
    val protocol: DeviceProtocol,
    val manufacturer: String? = null,
    val model: String? = null,
    val firmwareVersion: String? = null,
    val serialNumber: String? = null,
    val assetId: AssetId? = null
)

/**
 * Device state.
 */
data class DeviceState(
    val connected: Boolean = false,
    val batteryLevel: Double? = null,
    val signalStrength: Double? = null,
    val lastSeen: Long? = null,
    val error: String? = null,
    val customState: Map<String, String> = emptyMap()
)

/**
 * Device events.
 */
sealed class DeviceEvent {
    data class Connected(val deviceId: DeviceId) : DeviceEvent()
    data class Disconnected(val deviceId: DeviceId, val reason: String) : DeviceEvent()
    data class ObservationReceived(val observation: Observation) : DeviceEvent()
    data class StateChanged(val state: DeviceState) : DeviceEvent()
    data class Error(val deviceId: DeviceId, val message: String) : DeviceEvent()
    data class BatteryLow(val deviceId: DeviceId, val level: Double) : DeviceEvent()
    data class FirmwareUpdateAvailable(val deviceId: DeviceId, val version: String) : DeviceEvent()
}

/**
 * Commands that can be sent to a device.
 */
sealed class DeviceCommand {
    data class Observe(val sensorType: SensorType, val durationMs: Long = 1000) : DeviceCommand()
    data class Actuate(val actuatorType: ActuatorType, val params: Map<String, String> = emptyMap()) : DeviceCommand()
    data class SetConfig(val key: String, val value: String) : DeviceCommand()
    data class GetConfig(val key: String) : DeviceCommand()
    data class Calibrate(val params: Map<String, Double> = emptyMap()) : DeviceCommand()
    data class Test(val testType: String) : DeviceCommand()
    data class Stream(val sensorType: SensorType, val enabled: Boolean) : DeviceCommand()
    object GetCapabilities : DeviceCommand()
    object GetState : DeviceCommand()
    object Ping : DeviceCommand()
}

/**
 * Command result.
 */
sealed class CommandResult {
    data class Success(val data: Map<String, Any> = emptyMap()) : CommandResult()
    data class Error(val message: String, val code: Int = -1) : CommandResult()
    data class Partial(val data: Map<String, Any>, val warnings: List<String>) : CommandResult()
    object NotSupported : CommandResult()
    object Timeout : CommandResult()
    object Disconnected : CommandResult()
}

/**
 * Device health.
 */
data class DeviceHealth(
    val available: Boolean,
    val connected: Boolean,
    val batteryLevel: Double? = null,
    val signalStrength: Double? = null,
    val lastSeen: Long? = null,
    val uptimeMs: Long? = null,
    val errorCount: Int = 0,
    val warnings: List<String> = emptyList()
)

// ─────────────────────────────────────────────────────────────
// CONCRETE DEVICE IMPLEMENTATIONS
// ─────────────────────────────────────────────────────────────

/**
 * Thermal Camera Device
 */
class ThermalCameraDevice(
    override val identity: DeviceIdentity,
    private val adapter: ThermalCameraAdapter
) : SupremeDevice {

    private val _state = MutableDeviceStateFlow(DeviceState())
    private val _observations = MutableSharedFlow<Observation>()
    private val _events = MutableSharedFlow<DeviceEvent>()

    override val state: StateFlow<DeviceState> = _state
    override val observations: SharedFlow<Observation> = _observations
    override val events: SharedFlow<DeviceEvent> = _events

    override val capabilities: Set<Capability> = setOf(
        Capability.CanObserve(SensorType.CAMERA_THERMAL),
        Capability.CanObserve(SensorType.TEMPERATURE),
        Capability.CanCommunicate(CommunicationType.CAMERA_CAPTURE)
    )

    override suspend fun connect(): Boolean {
        return try {
            adapter.connect()
            _state.value = DeviceState(connected = true)
            _events.tryEmit(DeviceEvent.Connected(identity.id))
            true
        } catch (e: Exception) {
            _state.value = DeviceState(connected = false, error = e.message)
            _events.tryEmit(DeviceEvent.Error(identity.id, e.message ?: "Connection failed"))
            false
        }
    }

    override suspend fun disconnect() {
        adapter.disconnect()
        _state.value = DeviceState(connected = false)
        _events.tryEmit(DeviceEvent.Disconnected(identity.id, "User requested"))
    }

    override suspend fun observe(sensorType: SensorType): Observation? {
        if (sensorType != SensorType.CAMERA_THERMAL && sensorType != SensorType.TEMPERATURE) {
            return null
        }

        return try {
            val frame = adapter.captureFrame()
            val observation = Observation(
                id = ObservationId.generate(),
                deviceId = identity.id,
                assetId = identity.assetId,
                sensorType = SensorType.CAMERA_THERMAL,
                timestamp = Instant.now(),
                readings = mapOf(
                    "max_celsius" to frame.maxCelsius,
                    "min_celsius" to frame.minCelsius,
                    "average_celsius" to frame.averageCelsius
                ),
                confidence = 0.9,
                source = ObservationSource.DEVICE
            )
            _observations.tryEmit(observation)
            observation
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun execute(command: DeviceCommand): CommandResult {
        return when (command) {
            is DeviceCommand.Observe -> observe(command.sensorType)?.let {
                CommandResult.Success(mapOf("observation" to it))
            } ?: CommandResult.Error("Failed to observe")
            is DeviceCommand.Stream -> CommandResult.Success(mapOf("streaming" to command.enabled))
            is DeviceCommand.GetCapabilities -> CommandResult.Success(
                mapOf("capabilities" to capabilities.map { it.toString() })
            )
            is DeviceCommand.GetState -> CommandResult.Success(mapOf("state" to _state.value))
            is DeviceCommand.Ping -> CommandResult.Success()
            else -> CommandResult.NotSupported
        }
    }

    override suspend fun getHealth(): DeviceHealth {
        return DeviceHealth(
            available = adapter.isConnected(),
            connected = _state.value.connected,
            lastSeen = _state.value.lastSeen
        )
    }

    override fun isAvailable(): Boolean = adapter.isConnected()
}

/**
 * BLE Tag Device
 */
class BLETagDevice(
    override val identity: DeviceIdentity,
    private val bleScanner: BLEScanner
) : SupremeDevice {

    private val _state = MutableDeviceStateFlow(DeviceState())
    private val _observations = MutableSharedFlow<Observation>()
    private val _events = MutableSharedFlow<DeviceEvent>()

    override val state: StateFlow<DeviceState> = _state
    override val observations: SharedFlow<Observation> = _observations
    override val events: SharedFlow<DeviceEvent> = _events

    override val capabilities: Set<Capability> = setOf(
        Capability.CanObserve(SensorType.BLE_SIGNAL),
        Capability.CanCommunicate(CommunicationType.BLE_SCAN)
    )

    override suspend fun connect(): Boolean {
        _state.value = DeviceState(connected = true)
        _events.tryEmit(DeviceEvent.Connected(identity.id))
        return true
    }

    override suspend fun disconnect() {
        _state.value = DeviceState(connected = false)
        _events.tryEmit(DeviceEvent.Disconnected(identity.id, "User requested"))
    }

    override suspend fun observe(sensorType: SensorType): Observation? {
        if (sensorType != SensorType.BLE_SIGNAL) return null

        return try {
            val scanResult = bleScanner.scan(identity.id.value)
            val observation = Observation(
                id = ObservationId.generate(),
                deviceId = identity.id,
                assetId = identity.assetId,
                sensorType = SensorType.BLE_SIGNAL,
                timestamp = Instant.now(),
                readings = mapOf(
                    "rssi" to scanResult.rssi.toDouble(),
                    "tx_power" to (scanResult.txPower?.toDouble() ?: 0.0),
                    "distance_estimate" to (scanResult.distanceMeters?.toDouble() ?: 0.0)
                ),
                confidence = 0.7,
                source = ObservationSource.DEVICE
            )
            _observations.tryEmit(observation)
            observation
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun execute(command: DeviceCommand): CommandResult {
        return when (command) {
            is DeviceCommand.Observe -> observe(command.sensorType)?.let {
                CommandResult.Success(mapOf("observation" to it))
            } ?: CommandResult.Error("Failed to scan")
            is DeviceCommand.Ping -> CommandResult.Success()
            else -> CommandResult.NotSupported
        }
    }

    override suspend fun getHealth(): DeviceHealth {
        return DeviceHealth(
            available = true,
            connected = _state.value.connected,
            batteryLevel = _state.value.batteryLevel,
            signalStrength = _state.value.signalStrength
        )
    }

    override fun isAvailable(): Boolean = true
}

/**
 * Smart Plug Device
 */
class SmartPlugDevice(
    override val identity: DeviceIdentity,
    private val matterController: MatterController
) : SupremeDevice {

    private val _state = MutableDeviceStateFlow(DeviceState())
    private val _observations = MutableSharedFlow<Observation>()
    private val _events = MutableSharedFlow<DeviceEvent>()

    override val state: StateFlow<DeviceState> = _state
    override val observations: SharedFlow<Observation> = _observations
    override val events: SharedFlow<DeviceEvent> = _events

    override val capabilities: Set<Capability> = setOf(
        Capability.CanObserve(SensorType.POWER),
        Capability.CanObserve(SensorType.ENERGY),
        Capability.CanActuate(ActuatorType.RELAY_ON),
        Capability.CanActuate(ActuatorType.RELAY_OFF),
        Capability.CanCommunicate(CommunicationType.MATTER_CONTROL)
    )

    override suspend fun connect(): Boolean {
        return try {
            matterController.connect(identity.id.value)
            _state.value = DeviceState(connected = true)
            _events.tryEmit(DeviceEvent.Connected(identity.id))
            true
        } catch (e: Exception) {
            _state.value = DeviceState(connected = false, error = e.message)
            false
        }
    }

    override suspend fun disconnect() {
        matterController.disconnect(identity.id.value)
        _state.value = DeviceState(connected = false)
    }

    override suspend fun observe(sensorType: SensorType): Observation? {
        return try {
            val readings = matterController.getReadings(identity.id.value)
            val observation = Observation(
                id = ObservationId.generate(),
                deviceId = identity.id,
                assetId = identity.assetId,
                sensorType = sensorType,
                timestamp = Instant.now(),
                readings = readings,
                confidence = 0.9,
                source = ObservationSource.DEVICE
            )
            _observations.tryEmit(observation)
            observation
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun execute(command: DeviceCommand): CommandResult {
        return when (command) {
            is DeviceCommand.Actuate -> {
                when (command.actuatorType) {
                    ActuatorType.RELAY_ON -> {
                        matterController.setSwitch(identity.id.value, true)
                        CommandResult.Success(mapOf("switch" to true))
                    }
                    ActuatorType.RELAY_OFF -> {
                        matterController.setSwitch(identity.id.value, false)
                        CommandResult.Success(mapOf("switch" to false))
                    }
                    else -> CommandResult.NotSupported
                }
            }
            is DeviceCommand.Observe -> observe(command.sensorType)?.let {
                CommandResult.Success(mapOf("observation" to it))
            } ?: CommandResult.Error("Failed to observe")
            else -> CommandResult.NotSupported
        }
    }

    override suspend fun getHealth(): DeviceHealth {
        return DeviceHealth(
            available = matterController.isConnected(identity.id.value),
            connected = _state.value.connected
        )
    }

    override fun isAvailable(): Boolean = matterController.isConnected(identity.id.value)
}

// ─────────────────────────────────────────────────────────────
// ADAPTER INTERFACES (for concrete implementations)
// ─────────────────────────────────────────────────────────────

interface ThermalCameraAdapter {
    suspend fun connect(): Boolean
    suspend fun disconnect()
    fun isConnected(): Boolean
    suspend fun captureFrame(): ThermalFrame
}

interface BLEScanner {
    suspend fun scan(deviceId: String): BLEScanResult
    suspend fun startContinuousScan(callback: (BLEScanResult) -> Unit)
    suspend fun stopScan()
}

interface MatterController {
    suspend fun connect(deviceId: String): Boolean
    suspend fun disconnect(deviceId: String)
    fun isConnected(deviceId: String): Boolean
    suspend fun getReadings(deviceId: String): Map<String, Double>
    suspend fun setSwitch(deviceId: String, on: Boolean)
    suspend fun getDeviceList(): List<DeviceIdentity>
}

// ─────────────────────────────────────────────────────────────
// DATA CLASSES FOR ADAPTERS
// ─────────────────────────────────────────────────────────────

data class ThermalFrame(
    val maxCelsius: Double,
    val minCelsius: Double,
    val averageCelsius: Double,
    val width: Int,
    val height: Int,
    val pixels: FloatArray? = null
)

data class BLEScanResult(
    val deviceId: String,
    val rssi: Int,
    val txPower: Int? = null,
    val distanceMeters: Double? = null,
    val name: String? = null,
    val services: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

// ─────────────────────────────────────────────────────────────
// HELPER: MutableStateFlow
// ─────────────────────────────────────────────────────────────

class MutableDeviceStateFlow<T>(initial: T) : MutableStateFlow<T> by kotlinx.coroutines.flow.MutableStateFlow(initial)
