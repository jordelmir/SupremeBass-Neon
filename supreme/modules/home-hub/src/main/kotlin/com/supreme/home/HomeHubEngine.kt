package com.supreme.home

import com.supreme.core.*
import kotlinx.coroutines.flow.*
import java.time.Instant

/**
 * Supreme Home Hub — control your entire home from one place.
 *
 * Integrates with Matter/Google Home to control:
 * - Lights, thermostats, locks, cameras
 * - Smart plugs, appliances, sensors
 * - Automations based on presence, time, conditions
 *
 * Google Home APIs expose 750M+ compatible devices.
 */

class HomeHubEngine {

    private val rooms = mutableMapOf<String, Room>()
    private val devices = mutableMapOf<String, HomeDevice>()
    private val automations = mutableMapOf<String, Automation>()
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    /**
     * Add a room.
     */
    fun addRoom(room: Room) {
        rooms[room.id] = room
        updateState()
    }

    /**
     * Add a device to a room.
     */
    fun addDevice(device: HomeDevice) {
        devices[device.id] = device
        rooms[device.roomId]?.let { room ->
            rooms[device.roomId] = room.copy(deviceIds = room.deviceIds + device.id)
        }
        updateState()
    }

    /**
     * Control a device.
     */
    suspend fun controlDevice(deviceId: String, command: HomeCommand): CommandResult {
        val device = devices[deviceId] ?: return CommandResult.Error("Device not found")

        return when (command) {
            is HomeCommand.TurnOn -> {
                devices[deviceId] = device.copy(isOn = true)
                updateState()
                CommandResult.Success(mapOf("state" to "on"))
            }
            is HomeCommand.TurnOff -> {
                devices[deviceId] = device.copy(isOn = false)
                updateState()
                CommandResult.Success(mapOf("state" to "off"))
            }
            is HomeCommand.SetTemperature -> {
                devices[deviceId] = device.copy(temperature = command.celsius)
                updateState()
                CommandResult.Success(mapOf("temperature" to command.celsius))
            }
            is HomeCommand.SetBrightness -> {
                devices[deviceId] = device.copy(brightness = command.percent)
                updateState()
                CommandResult.Success(mapOf("brightness" to command.percent))
            }
            is HomeCommand.Lock -> {
                devices[deviceId] = device.copy(isLocked = true)
                updateState()
                CommandResult.Success(mapOf("locked" to true))
            }
            is HomeCommand.Unlock -> {
                devices[deviceId] = device.copy(isLocked = false)
                updateState()
                CommandResult.Success(mapOf("locked" to false))
            }
            is HomeCommand.SetColor -> {
                devices[deviceId] = device.copy(color = command.hexColor)
                updateState()
                CommandResult.Success(mapOf("color" to command.hexColor))
            }
        }
    }

    /**
     * Get all devices in a room.
     */
    fun getRoomDevices(roomId: String): List<HomeDevice> {
        val room = rooms[roomId] ?: return emptyList()
        return room.deviceIds.mapNotNull { devices[it] }
    }

    /**
     * Get all lights.
     */
    fun getAllLights(): List<HomeDevice> {
        return devices.values.filter { it.type == DeviceType.LIGHT }
    }

    /**
     * Get all smart plugs.
     */
    fun getAllPlugs(): List<HomeDevice> {
        return devices.values.filter { it.type == DeviceType.SMART_PLUG }
    }

    /**
     * Check "Did I leave something on?"
     */
    fun getLeftOn(): List<HomeDevice> {
        return devices.values.filter { it.isOn && it.shouldAutoOff }
    }

    /**
     * Get devices that are on.
     */
    fun getActiveDevices(): List<HomeDevice> {
        return devices.values.filter { it.isOn }
    }

    /**
     * Run safety check.
     */
    fun runSafetyCheck(): SafetyCheckResult {
        val issues = mutableListOf<SafetyIssue>()

        // Check for dangerous plugs left on
        devices.values.filter {
            it.type == DeviceType.SMART_PLUG && it.isOn && it.isHighRisk
        }.forEach { device ->
            issues.add(SafetyIssue(
                type = IssueType.HIGH_RISK_PLUG,
                deviceId = device.id,
                deviceName = device.name,
                description = "${device.name} is ON (high-risk device)",
                severity = Severity.HIGH
            ))
        }

        // Check for open doors/windows
        devices.values.filter {
            it.type == DeviceType.LOCK && it.isLocked != true
        }.forEach { device ->
            issues.add(SafetyIssue(
                type = IssueType.DOOR_UNLOCKED,
                deviceId = device.id,
                deviceName = device.name,
                description = "${device.name} is unlocked",
                severity = Severity.MEDIUM
            ))
        }

        return SafetyCheckResult(
            timestamp = Instant.now(),
            issues = issues,
            isSafe = issues.isEmpty(),
            summary = if (issues.isEmpty()) "All clear" else "${issues.size} issues found"
        )
    }

    /**
     * Create an automation.
     */
    fun createAutomation(automation: Automation) {
        automations[automation.id] = automation
    }

    /**
     * Get energy consumption summary.
     * Cost estimate is UNKNOWN until user configures their electricity rate.
     */
    fun getEnergySummary(): EnergySummary {
        val activeDevices = devices.values.filter { it.isOn }
        val totalWatts = activeDevices.sumOf { it.powerWatts ?: 0.0 }

        return EnergySummary(
            activeDevices = activeDevices.size,
            totalWatts = totalWatts,
            estimatedDailyKwh = if (totalWatts > 0) totalWatts * 24 / 1000 else 0.0,
            monthlyCostEstimate = null // UNKNOWN: User must configure electricity rate
        )
    }

    private fun updateState() {
        _state.value = HomeState(
            rooms = rooms.values.toList(),
            devices = devices.values.toList(),
            activeDevices = devices.values.count { it.isOn },
            automations = automations.values.toList()
        )
    }
}

// ─────────────────────────────────────────────────────────────
// DATA CLASSES
// ─────────────────────────────────────────────────────────────

data class Room(
    val id: String,
    val name: String,
    val floor: Int = 0,
    val deviceIds: Set<String> = emptySet()
)

data class HomeDevice(
    val id: String,
    val name: String,
    val type: DeviceType,
    val roomId: String,
    val protocol: DeviceProtocol,
    val isOn: Boolean = false,
    val isLocked: Boolean? = null,
    val temperature: Double? = null,
    val brightness: Int? = null,
    val color: String? = null,
    val powerWatts: Double? = null,
    val shouldAutoOff: Boolean = false,
    val isHighRisk: Boolean = false,
    val manufacturer: String? = null,
    val model: String? = null
)

sealed class HomeCommand {
    data object TurnOn : HomeCommand()
    data object TurnOff : HomeCommand()
    data class SetTemperature(val celsius: Double) : HomeCommand()
    data class SetBrightness(val percent: Int) : HomeCommand()
    data object Lock : HomeCommand()
    data object Unlock : HomeCommand()
    data class SetColor(val hexColor: String) : HomeCommand()
}

data class Automation(
    val id: String,
    val name: String,
    val trigger: AutomationTrigger,
    val conditions: List<AutomationCondition>,
    val actions: List<AutomationAction>,
    val enabled: Boolean = true
)

sealed class AutomationTrigger {
    data object NobodyHome : AutomationTrigger()
    data object SomebodyHome : AutomationTrigger()
    data class TimeOfDay(val hour: Int, val minute: Int) : AutomationTrigger()
    data class TemperatureAbove(val celsius: Double) : AutomationTrigger()
    data class TemperatureBelow(val celsius: Double) : AutomationTrigger()
    data class MotionDetected(val deviceId: String) : AutomationTrigger()
    data class DoorOpened(val deviceId: String) : AutomationTrigger()
}

sealed class AutomationCondition {
    data object Daytime : AutomationCondition()
    data object Nighttime : AutomationCondition()
    data class TemperatureAbove(val celsius: Double) : AutomationCondition()
    data class TemperatureBelow(val celsius: Double) : AutomationCondition()
    data class HumidityAbove(val percent: Double) : AutomationCondition()
}

sealed class AutomationAction {
    data class TurnOn(val deviceId: String) : AutomationAction()
    data class TurnOff(val deviceId: String) : AutomationAction()
    data class SetTemperature(val deviceId: String, val celsius: Double) : AutomationAction()
    data class SendNotification(val message: String) : AutomationAction()
    data class RunScene(val sceneId: String) : AutomationAction()
}

data class HomeState(
    val rooms: List<Room> = emptyList(),
    val devices: List<HomeDevice> = emptyList(),
    val activeDevices: Int = 0,
    val automations: List<Automation> = emptyList()
)

data class SafetyCheckResult(
    val timestamp: Instant,
    val issues: List<SafetyIssue>,
    val isSafe: Boolean,
    val summary: String
)

data class SafetyIssue(
    val type: IssueType,
    val deviceId: String,
    val deviceName: String,
    val description: String,
    val severity: Severity
)

enum class IssueType {
    HIGH_RISK_PLUG,
    DOOR_UNLOCKED,
    WINDOW_OPEN,
    LIGHT_LEFT_ON,
    AC_LEFT_ON,
    OTHER
}

data class EnergySummary(
    val activeDevices: Int,
    val totalWatts: Double,
    val estimatedDailyKwh: Double,
    val monthlyCostEstimate: Double? = null
)

sealed class CommandResult {
    data class Success(val data: Map<String, Any>) : CommandResult()
    data class Error(val message: String) : CommandResult()
}
