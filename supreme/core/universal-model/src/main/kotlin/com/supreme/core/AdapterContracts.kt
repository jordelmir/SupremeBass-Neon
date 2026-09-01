package com.supreme.core

import com.supreme.truth.TruthAuthority
import com.supreme.truth.CommandPhase
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Device Adapter Contracts — the universal interface for hardware integration.
 *
 * Every physical device in Supreme connects through a DeviceAdapter.
 * The adapter handles protocol specifics while the core handles truth semantics.
 *
 * Type parameters:
 *   C = Configuration type (e.g., FlirConfig, ElmConfig, MatterConfig)
 *   O = Observation type produced by this adapter
 *
 * Key principle:
 *   Adapter returns raw data with TruthAuthority.
 *   Adapter NEVER fabricates success.
 *   Adapter NEVER returns data it didn't receive from hardware.
 */
interface DeviceAdapter<C, O> {

    /** Unique adapter identifier (e.g., "flir-a70", "elm327", "matter-light"). */
    val adapterId: String

    /** Human-readable name. */
    val displayName: String

    /** Protocol this adapter speaks. */
    val protocol: DeviceProtocol

    /** Current connection state. */
    suspend fun state(): AdapterState

    /** Connect to the device. Returns false if connection fails. */
    suspend fun connect(config: C): Boolean

    /** Disconnect from the device. */
    suspend fun disconnect()

    /** Check if the device is reachable. */
    suspend fun isReachable(): Boolean

    /** Observe — continuous stream of observations from the device. */
    fun observe(): Flow<AdapterObservation<O>>

    /** Read — single observation on demand. */
    suspend fun read(): AdapterObservation<O>?

    /** Command — send a command to the device. Returns lifecycle. */
    suspend fun command(request: AdapterCommand): CommandResult

    /** Health — current device health status. */
    suspend fun health(): AdapterHealth

    /** Capabilities — what this adapter can do. */
    fun capabilities(): AdapterCapabilities

    /** Metadata — firmware version, serial number, etc. */
    fun metadata(): Map<String, String>
}

// ─────────────────────────────────────────────────────────────
// ADAPTER STATE
// ─────────────────────────────────────────────────────────────

enum class AdapterState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR,
    DISABLED
}

// ─────────────────────────────────────────────────────────────
// OBSERVATION WRAPPER
// ─────────────────────────────────────────────────────────────

/**
 * An observation from an adapter, with full truth metadata.
 * The adapter guarantees:
 *   - value is what it received from hardware (or derived from it)
 *   - authority reflects the actual source
 *   - rawPayload contains the original protocol data
 *   - timestamp is when the data was received (not when it was generated)
 */
data class AdapterObservation<T>(
    val value: T,
    val authority: TruthAuthority,
    val source: String,
    val receivedAt: Instant,
    val generatedAt: Instant? = null,
    val rawPayload: String? = null,
    val confidence: Double? = null,
    val metadata: Map<String, String> = emptyMap(),
    val uncertainty: Double? = null,
    val calibrationId: String? = null
)

// ─────────────────────────────────────────────────────────────
// COMMANDS
// ─────────────────────────────────────────────────────────────

/**
 * A command to send to an adapter.
 */
data class AdapterCommand(
    val type: String,
    val parameters: Map<String, Any> = emptyMap(),
    val timeoutMs: Long = 5000,
    val requiresVerification: Boolean = true
)

/**
 * Result of a command, following the CommandLifecycle semantics.
 */
data class CommandResult(
    val success: Boolean,
    val phase: CommandPhase,
    val transportAck: Boolean? = null,
    val protocolAck: Boolean? = null,
    val physicalState: String? = null,
    val physicalEvidence: Map<String, String> = emptyMap(),
    val error: String? = null,
    val timestamp: Instant = Instant.now()
) {
    companion object {
        /** NOT_IMPLEMENTED — adapter has no real hardware connection. */
        fun notImplemented(adapterId: String) = CommandResult(
            success = false,
            phase = CommandPhase.TRANSPORT_FAILED,
            error = "NOT_IMPLEMENTED: $adapterId has no real hardware connection"
        )

        /** Transport failure — command could not be sent. */
        fun transportFailed(reason: String) = CommandResult(
            success = false,
            phase = CommandPhase.TRANSPORT_FAILED,
            error = reason
        )

        /** Protocol failure — device rejected the command. */
        fun protocolFailed(reason: String) = CommandResult(
            success = false,
            phase = CommandPhase.PROTOCOL_FAILED,
            protocolAck = false,
            error = reason
        )

        /** Physical effect failed — command was sent but effect not verified. */
        fun physicalEffectFailed(reason: String) = CommandResult(
            success = false,
            phase = CommandPhase.PHYSICAL_EFFECT_FAILED,
            transportAck = true,
            protocolAck = true,
            error = reason
        )

        /** Success — physical effect verified. */
        fun verified(physicalState: String, evidence: Map<String, String> = emptyMap()) = CommandResult(
            success = true,
            phase = CommandPhase.PHYSICALLY_VERIFIED,
            transportAck = true,
            protocolAck = true,
            physicalState = physicalState,
            physicalEvidence = evidence
        )
    }
}

// ─────────────────────────────────────────────────────────────
// HEALTH
// ─────────────────────────────────────────────────────────────

data class AdapterHealth(
    val adapterId: String,
    val state: AdapterState,
    val timestamp: Instant,
    val uptimeMs: Long? = null,
    val signalStrengthDbm: Double? = null,
    val batteryLevel: Double? = null,
    val errorCount: Long = 0,
    val lastError: String? = null,
    val lastErrorAt: Instant? = null,
    val reconnectCount: Int = 0,
    val firmwareVersion: String? = null,
    val warnings: List<String> = emptyList()
)

// ─────────────────────────────────────────────────────────────
// CAPABILITIES
// ─────────────────────────────────────────────────────────────

data class AdapterCapabilities(
    val canObserve: Boolean = false,
    val canCommand: Boolean = false,
    val canStream: Boolean = false,
    val observableSensors: List<SensorType> = emptyList(),
    val commandableActuators: List<ActuatorType> = emptyList(),
    val supportedFormats: List<String> = emptyList(),
    val maxConcurrentStreams: Int = 1,
    val requiresPersistentConnection: Boolean = false
)

// ─────────────────────────────────────────────────────────────
// ADAPTER MANIFEST (for SDK/Marketplace)
// ─────────────────────────────────────────────────────────────

/**
 * Static description of an adapter — used for registration, discovery, and sandboxing.
 */
data class AdapterManifest(
    val adapterId: String,
    val version: String,
    val displayName: String,
    val description: String,
    val protocol: DeviceProtocol,
    val publisher: String,
    val permissions: List<AdapterPermission>,
    val capabilities: AdapterCapabilities,
    val configSchema: Map<String, String>,
    val minSdkVersion: Int = 26,
    val maxSdkVersion: Int = 35,
    val signatureHash: String? = null,
    val repositoryUrl: String? = null,
    val documentationUrl: String? = null
)

/**
 * Permissions an adapter requires.
 */
enum class AdapterPermission {
    BLUETOOTH_SCAN,
    BLUETOOTH_CONNECT,
    BLUETOOTH_ADMIN,
    WIFI_SCAN,
    WIFI_CONNECT,
    LOCATION_FINE,
    LOCATION_COARSE,
    CAMERA,
    MICROPHONE,
    NFC,
    USB,
    INTERNET,
    LOCAL_NETWORK,
    HARDWARE_SENSOR,
    OTHER
}
