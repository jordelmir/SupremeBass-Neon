package com.supreme.leak

import com.supreme.core.*
import kotlinx.coroutines.flow.*
import java.time.Instant

/**
 * Supreme Leak Watch — detect and stop water leaks.
 *
 * Hardware:
 * - BLE/Matter water leak sensors
 * - Flow meter
 * - Motorized shutoff valve
 *
 * Philosophy:
 *   COMMAND_SENT ≠ PHYSICAL_EFFECT_VERIFIED
 *   Supreme won't say "closed" until a sensor confirms flow reduction.
 */

class LeakWatchEngine {

    private val sensors = mutableMapOf<String, LeakSensor>()
    private val valves = mutableMapOf<String, ShutoffValve>()
    private val readings = mutableMapOf<String, MutableList<LeakReading>>()
    private val _state = MutableStateFlow(LeakWatchState())
    val state: StateFlow<LeakWatchState> = _state.asStateFlow()
    private val _alerts = MutableSharedFlow<LeakAlert>(replay = 1)
    val alerts: SharedFlow<LeakAlert> = _alerts

    /**
     * Add a leak sensor.
     */
    fun addSensor(sensor: LeakSensor) {
        sensors[sensor.id] = sensor
        readings[sensor.id] = mutableListOf()
        updateState()
    }

    /**
     * Add a shutoff valve.
     */
    fun addValve(valve: ShutoffValve) {
        valves[valve.id] = valve
        updateState()
    }

    /**
     * Record a sensor reading.
     */
    fun recordReading(sensorId: String, reading: LeakReading) {
        readings.getOrPut(sensorId) { mutableListOf() }.add(reading)

        // Check for leak
        if (reading.waterDetected) {
            val sensor = sensors[sensorId]
            _alerts.tryEmit(LeakAlert(
                timestamp = Instant.now(),
                sensorId = sensorId,
                sensorName = sensor?.name ?: sensorId,
                location = sensor?.location ?: "Unknown",
                flowRate = reading.flowRateLpm,
                severity = if ((reading.flowRateLpm ?: 0.0) > 10) Severity.CRITICAL else Severity.HIGH,
                action = LeakAction.DETECTED
            ))
        }

        updateState()
    }

    /**
     * Shut off water for a zone.
     */
    suspend fun shutoffValve(valveId: String): ShutoffResult {
        val valve = valves[valveId] ?: return ShutoffResult(false, "Valve not found")

        // Send command
        val commandSent = sendValveCommand(valveId, false)
        if (!commandSent) {
            return ShutoffResult(false, "Failed to send command")
        }

        // Wait for verification
        kotlinx.coroutines.delay(2000)

        // Verify flow reduction
        val verified = verifyFlowReduction(valve.zone)

        return if (verified) {
            valves[valveId] = valve.copy(
                isOpen = false,
                lastAction = Instant.now(),
                lastActionType = ValveActionType.SHUTOFF
            )
            updateState()
            ShutoffResult(true, "Valve closed and flow verified")
        } else {
            ShutoffResult(false, "Command sent but flow not verified")
        }
    }

    /**
     * Open a valve.
     * Returns failure until real hardware is connected.
     */
    suspend fun openValve(valveId: String): ShutoffResult {
        val valve = valves[valveId] ?: return ShutoffResult(false, "Valve not found")

        // NOT_IMPLEMENTED: No real valve hardware connected
        return ShutoffResult(false, "NOT_IMPLEMENTED: No valve hardware connected")
    }

    /**
     * Get current flow status.
     */
    fun getFlowStatus(): FlowStatus {
        val activeSensors = sensors.values.filter { it.connected }
        val activeReadings = readings.values.flatten().takeLast(10)

        val avgFlow = if (activeReadings.isNotEmpty()) {
            activeReadings.mapNotNull { it.flowRateLpm }.average()
        } else 0.0

        val anyWaterDetected = activeReadings.any { it.waterDetected }

        return FlowStatus(
            activeSensors = activeSensors.size,
            averageFlowLpm = avgFlow,
            waterDetected = anyWaterDetected,
            valvesOpen = valves.values.count { it.isOpen },
            valvesClosed = valves.values.count { !it.isOpen }
        )
    }

    /**
     * Get leak history.
     */
    fun getLeakHistory(): List<LeakEvent> {
        return readings.values.flatten()
            .filter { it.waterDetected }
            .sortedByDescending { it.timestamp }
            .map { reading ->
                LeakEvent(
                    timestamp = reading.timestamp,
                    sensorId = reading.sensorId,
                    flowRate = reading.flowRateLpm,
                    resolved = false
                )
            }
    }

    private fun sendValveCommand(valveId: String, open: Boolean): Boolean {
        // NOT_IMPLEMENTED: No real valve hardware connected
        // Returns false to indicate command was NOT sent
        return false
    }

    private fun verifyFlowReduction(zone: String): Boolean {
        // Check recent readings for flow reduction
        val recentReadings = readings.values.flatten()
            .filter { it.timestamp.isAfter(Instant.now().minusSeconds(5)) }
        val avgFlow = recentReadings.mapNotNull { it.flowRateLpm }.average()
        return avgFlow < 1.0 // Flow should drop below 1 L/min
    }

    private fun updateState() {
        _state.value = LeakWatchState(
            sensors = sensors.values.toList(),
            valves = valves.values.toList(),
            activeSensors = sensors.values.count { it.connected },
            totalReadings = readings.values.sumOf { it.size }
        )
    }
}

// ─────────────────────────────────────────────────────────────
// DATA CLASSES
// ─────────────────────────────────────────────────────────────

data class LeakSensor(
    val id: String,
    val name: String,
    val location: String,
    val protocol: DeviceProtocol,
    val connected: Boolean = false,
    val batteryLevel: Double? = null
)

data class ShutoffValve(
    val id: String,
    val name: String,
    val zone: String,
    val protocol: DeviceProtocol,
    val isOpen: Boolean = true,
    val lastAction: Instant? = null,
    val lastActionType: ValveActionType? = null
)

enum class ValveActionType {
    SHUTOFF,
    OPEN,
    TEST
}

data class LeakReading(
    val sensorId: String,
    val timestamp: Instant,
    val waterDetected: Boolean,
    val flowRateLpm: Double? = null,
    val temperatureC: Double? = null,
    val humidity: Double? = null
)

data class LeakAlert(
    val timestamp: Instant,
    val sensorId: String,
    val sensorName: String,
    val location: String,
    val flowRate: Double?,
    val severity: Severity,
    val action: LeakAction
)

enum class LeakAction {
    DETECTED,
    SHUTOFF_SENT,
    SHUTOFF_VERIFIED,
    SHUTOFF_FAILED,
    RESOLVED
}

data class ShutoffResult(
    val success: Boolean,
    val message: String,
    val flowVerified: Boolean = success
)

data class FlowStatus(
    val activeSensors: Int,
    val averageFlowLpm: Double,
    val waterDetected: Boolean,
    val valvesOpen: Int,
    val valvesClosed: Int
)

data class LeakEvent(
    val timestamp: Instant,
    val sensorId: String,
    val flowRate: Double?,
    val resolved: Boolean
)

data class LeakWatchState(
    val sensors: List<LeakSensor> = emptyList(),
    val valves: List<ShutoffValve> = emptyList(),
    val activeSensors: Int = 0,
    val totalReadings: Int = 0
)
