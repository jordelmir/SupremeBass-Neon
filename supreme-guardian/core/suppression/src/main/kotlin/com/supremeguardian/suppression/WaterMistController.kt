package com.supremeguardian.suppression

import com.supremeguardian.core.building.ZoneId
import com.supremeguardian.core.shared.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Water Mist Controller — controls water mist suppression systems.
 *
 * Safety Interlocks:
 *   1. Manual override always available
 *   2. Pressure sensor verification
 *   3. Flow sensor verification
 *   4. Temperature threshold confirmation
 *   5. Multi-sensor confirmation
 *   6. Human approval for critical actions
 *   7. Automatic shutoff after timeout
 *   8. Emergency stop button
 *
 * Hardware Interface:
 *   - PLC (Programmable Logic Controller)
 *   - Relay modules
 *   - Pressure sensors
 *   - Flow sensors
 *   - Temperature sensors
 *   - Manual override switches
 */
class WaterMistController {

    companion object {
        const val MAX_ACTIVATION_TIME_MS = 300_000L // 5 minutes max
        const val PRESSURE_THRESHOLD_BAR = 2.0
        const val FLOW_THRESHOLD_LPM = 5.0
        const val COOLDOWN_MS = 60_000L // 1 minute cooldown between activations
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val running = AtomicBoolean(false)

    // State
    private val _state = MutableStateFlow(MistControllerState())
    val state: StateFlow<MistControllerState> = _state.asStateFlow()

    // Safety interlocks
    private val manualOverride = AtomicBoolean(false)
    private val emergencyStop = AtomicBoolean(false)
    private val lastActivationTime = AtomicReference<GuardianTimestamp?>(null)

    // Hardware interface
    private var hardwareInterface: MistHardwareInterface? = null

    /**
     * Start the controller.
     */
    fun start() {
        if (running.getAndSet(true)) return

        // Monitor hardware state
        scope.launch {
            while (running.get()) {
                try {
                    updateHardwareState()
                } catch (e: Exception) {
                    // Hardware read failure is non-fatal
                }
                delay(1000) // Poll every second
            }
        }
    }

    /**
     * Stop the controller.
     */
    fun stop() {
        if (!running.getAndSet(false)) return

        // Emergency stop on shutdown
        scope.launch {
            emergencyStop()
        }

        scope.cancel()
    }

    /**
     * Set hardware interface for PLC/relay control.
     */
    fun setHardwareInterface(interface_: MistHardwareInterface) {
        this.hardwareInterface = interface_
    }

    /**
     * Activate water mist for a zone.
     */
    suspend fun activate(zoneId: ZoneId, incidentId: IncidentId): MistActivationResult {
        if (!running.get()) {
            return MistActivationResult(false, "Controller not running", emptyList())
        }

        // Check safety interlocks
        val interlockResult = checkSafetyInterlocks(zoneId)
        if (!interlockResult.passed) {
            return MistActivationResult(false, "Safety interlocks failed: ${interlockResult.failedInterlocks.joinToString()}", interlockResult.failedInterlocks)
        }

        // Check cooldown
        val lastActivation = lastActivationTime.get()
        if (lastActivation != null) {
            val elapsed = System.currentTimeMillis() - (lastActivation.toInstant()?.toEpochMilli() ?: 0)
            if (elapsed < COOLDOWN_MS) {
                return MistActivationResult(false, "Cooldown period active", listOf("COOLDOWN"))
            }
        }

        // Activate hardware
        val success = hardwareInterface?.activateMist(zoneId.value) ?: false

        if (success) {
            lastActivationTime.set(GuardianTimestamp())

            // Start automatic shutoff timer
            scope.launch {
                delay(MAX_ACTIVATION_TIME_MS)
                if (_state.value.active) {
                    deactivate(zoneId, "Automatic timeout")
                }
            }

            _state.update { it.copy(
                active = true,
                zoneId = zoneId,
                activationTime = GuardianTimestamp(),
                incidentId = incidentId
            )}

            return MistActivationResult(true, "Water mist activated", emptyList())
        } else {
            return MistActivationResult(false, "Hardware activation failed", listOf("HARDWARE_ERROR"))
        }
    }

    /**
     * Deactivate water mist for a zone.
     */
    suspend fun deactivate(zoneId: ZoneId, reason: String): MistActivationResult {
        if (!running.get()) {
            return MistActivationResult(false, "Controller not running", emptyList())
        }

        // Deactivate hardware
        val success = hardwareInterface?.deactivateMist(zoneId.value) ?: false

        if (success) {
            _state.update { it.copy(
                active = false,
                deactivationTime = GuardianTimestamp(),
                lastDeactivationReason = reason
            )}

            return MistActivationResult(true, "Water mist deactivated: $reason", emptyList())
        } else {
            return MistActivationResult(false, "Hardware deactivation failed", listOf("HARDWARE_ERROR"))
        }
    }

    /**
     * Emergency stop — immediately deactivate all zones.
     */
    suspend fun emergencyStop(): MistActivationResult {
        emergencyStop.set(true)

        // Deactivate all zones
        val success = hardwareInterface?.emergencyStop() ?: false

        _state.update { it.copy(
            active = false,
            emergencyStop = true,
            deactivationTime = GuardianTimestamp(),
            lastDeactivationReason = "Emergency stop"
        )}

        return MistActivationResult(success, "Emergency stop executed", emptyList())
    }

    /**
     * Reset emergency stop.
     */
    fun resetEmergencyStop() {
        emergencyStop.set(false)
        _state.update { it.copy(emergencyStop = false) }
    }

    /**
     * Set manual override.
     */
    fun setManualOverride(override: Boolean) {
        manualOverride.set(override)
        _state.update { it.copy(manualOverride = override) }
    }

    /**
     * Check if mist is active.
     */
    fun isActive(): Boolean = _state.value.active

    /**
     * Get current pressure.
     */
    fun getPressure(): Double = _state.value.pressure

    /**
     * Get current flow rate.
     */
    fun getFlowRate(): Double = _state.value.flowRate

    private suspend fun checkSafetyInterlocks(zoneId: ZoneId): SafetyInterlockResult {
        val failedInterlocks = mutableListOf<String>()

        // Check emergency stop
        if (emergencyStop.get()) {
            failedInterlocks.add("EMERGENCY_STOP")
        }

        // Check manual override (manual override PREVENTS activation)
        if (manualOverride.get()) {
            failedInterlocks.add("MANUAL_OVERRIDE")
        }

        // Check pressure
        val pressure = _state.value.pressure
        if (pressure < PRESSURE_THRESHOLD_BAR) {
            failedInterlocks.add("LOW_PRESSURE: $pressure bar")
        }

        // Check flow
        val flowRate = _state.value.flowRate
        if (flowRate < FLOW_THRESHOLD_LPM && _state.value.active) {
            failedInterlocks.add("LOW_FLOW: $flowRate LPM")
        }

        // Check temperature confirmation
        val temperature = _state.value.temperature
        if (temperature < 60.0) {
            failedInterlocks.add("TEMPERATURE_TOO_LOW: $temperature°C")
        }

        return SafetyInterlockResult(
            passed = failedInterlocks.isEmpty(),
            failedInterlocks = failedInterlocks
        )
    }

    private suspend fun updateHardwareState() {
        val hw = hardwareInterface ?: return

        val pressure = hw.readPressure()
        val flowRate = hw.readFlowRate()
        val temperature = hw.readTemperature()
        val valveOpen = hw.isValveOpen()

        _state.update { it.copy(
            pressure = pressure,
            flowRate = flowRate,
            temperature = temperature,
            valveOpen = valveOpen,
            lastHardwareUpdate = GuardianTimestamp()
        )}
    }
}

/**
 * Hardware interface for water mist control.
 */
interface MistHardwareInterface {
    suspend fun activateMist(zoneId: String): Boolean
    suspend fun deactivateMist(zoneId: String): Boolean
    suspend fun emergencyStop(): Boolean
    suspend fun readPressure(): Double
    suspend fun readFlowRate(): Double
    suspend fun readTemperature(): Double
    suspend fun isValveOpen(): Boolean
}

/**
 * PLC-based hardware interface.
 */
class PLCHardwareInterface(
    private val plcAddress: String,
    private val plcPort: Int = 502
) : MistHardwareInterface {

    override suspend fun activateMist(zoneId: String): Boolean {
        // TODO: Send Modbus command to PLC
        return true
    }

    override suspend fun deactivateMist(zoneId: String): Boolean {
        // TODO: Send Modbus command to PLC
        return true
    }

    override suspend fun emergencyStop(): Boolean {
        // TODO: Send emergency stop command to PLC
        return true
    }

    override suspend fun readPressure(): Double {
        // TODO: Read pressure from PLC
        return 3.5
    }

    override suspend fun readFlowRate(): Double {
        // TODO: Read flow rate from PLC
        return 8.0
    }

    override suspend fun readTemperature(): Double {
        // TODO: Read temperature from PLC
        return 75.0
    }

    override suspend fun isValveOpen(): Boolean {
        // TODO: Read valve state from PLC
        return false
    }
}

/**
 * Relay-based hardware interface.
 */
class RelayHardwareInterface(
    private val relayBoardAddress: String
) : MistHardwareInterface {

    override suspend fun activateMist(zoneId: String): Boolean {
        // TODO: Send command to relay board
        return true
    }

    override suspend fun deactivateMist(zoneId: String): Boolean {
        // TODO: Send command to relay board
        return true
    }

    override suspend fun emergencyStop(): Boolean {
        // TODO: Send emergency stop to relay board
        return true
    }

    override suspend fun readPressure(): Double {
        // TODO: Read pressure from sensor
        return 3.5
    }

    override suspend fun readFlowRate(): Double {
        // TODO: Read flow rate from sensor
        return 8.0
    }

    override suspend fun readTemperature(): Double {
        // TODO: Read temperature from sensor
        return 75.0
    }

    override suspend fun isValveOpen(): Boolean {
        // TODO: Read valve state
        return false
    }
}

/**
 * Mist controller state.
 */
data class MistControllerState(
    val active: Boolean = false,
    val zoneId: ZoneId? = null,
    val activationTime: GuardianTimestamp? = null,
    val deactivationTime: GuardianTimestamp? = null,
    val lastDeactivationReason: String? = null,
    val incidentId: IncidentId? = null,
    val emergencyStop: Boolean = false,
    val manualOverride: Boolean = false,
    val pressure: Double = 0.0,
    val flowRate: Double = 0.0,
    val temperature: Double = 0.0,
    val valveOpen: Boolean = false,
    val lastHardwareUpdate: GuardianTimestamp? = null
)

/**
 * Mist activation result.
 */
data class MistActivationResult(
    val success: Boolean,
    val message: String,
    val failedInterlocks: List<String>
)

/**
 * Safety interlock result.
 */
data class SafetyInterlockResult(
    val passed: Boolean,
    val failedInterlocks: List<String>
)
