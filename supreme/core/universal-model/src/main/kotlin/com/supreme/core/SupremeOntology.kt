package com.supreme.core

import java.time.Instant
import java.util.UUID

/**
 * Supreme Universal Device/Asset Model
 *
 * The single ontology that all Supreme modules speak.
 * Every physical thing — a washing machine, a car, a camera, a BLE tag, a water valve —
 * is represented as a Device with Capabilities, producing Observations, generating Anomalies,
 * triggering Actions, and leaving Evidence.
 *
 * This is the foundation. Get this right, and every module becomes a composition of these primitives.
 */

// ─────────────────────────────────────────────────────────────
// IDENTITY
// ─────────────────────────────────────────────────────────────

@JvmInline
value class AssetId(val value: String) {
    companion object {
        fun generate() = AssetId(UUID.randomUUID().toString())
    }
}

@JvmInline
value class DeviceId(val value: String) {
    companion object {
        fun generate() = DeviceId(UUID.randomUUID().toString())
    }
}

@JvmInline
value class ObservationId(val value: String) {
    companion object {
        fun generate() = ObservationId(UUID.randomUUID().toString())
    }
}

data class DocumentRef(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: String,
    val path: String? = null,
    val uri: String? = null,
    val createdAt: Instant = Instant.now()
)

@JvmInline
value class AnomalyId(val value: String) {
    companion object {
        fun generate() = AnomalyId(UUID.randomUUID().toString())
    }
}

@JvmInline
value class ActionId(val value: String) {
    companion object {
        fun generate() = ActionId(UUID.randomUUID().toString())
    }
}

@JvmInline
value class IncidentId(val value: String) {
    companion object {
        fun generate() = IncidentId(UUID.randomUUID().toString())
    }
}

@JvmInline
value class EvidenceId(val value: String) {
    companion object {
        fun generate() = EvidenceId(UUID.randomUUID().toString())
    }
}

@JvmInline
value class MaintenanceId(val value: String) {
    companion object {
        fun generate() = MaintenanceId(UUID.randomUUID().toString())
    }
}

@JvmInline
value class CostId(val value: String) {
    companion object {
        fun generate() = CostId(UUID.randomUUID().toString())
    }
}

// ─────────────────────────────────────────────────────────────
// CORE ENTITIES
// ─────────────────────────────────────────────────────────────

/**
 * Asset — anything the user owns and cares about.
 * A washing machine, a car, a house, a toolbox.
 */
data class Asset(
    val id: AssetId,
    val name: String,
    val category: AssetCategory,
    val subcategory: String? = null,
    val brand: String? = null,
    val model: String? = null,
    val serialNumber: String? = null,
    val purchaseDate: Instant? = null,
    val purchasePrice: Double? = null,
    val currency: String = "CRC",
    val warrantyExpiry: Instant? = null,
    val condition: AssetCondition = AssetCondition.GOOD,
    val location: AssetLocation? = null,
    val photos: List<String> = emptyList(),
    val documents: List<DocumentRef> = emptyList(),
    val devices: List<DeviceId> = emptyList(),
    val tags: List<String> = emptyList(),
    val notes: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

enum class AssetCategory {
    APPLIANCE,
    ELECTRONICS,
    VEHICLE,
    FURNITURE,
    TOOL,
    STRUCTURE,
    UTILITY,
    SENSOR,
    CAMERA,
    NETWORK,
    MEDICAL,
    OTHER
}

enum class AssetCondition {
    NEW,
    LIKE_NEW,
    GOOD,
    FAIR,
    POOR,
    BROKEN,
    SCRAPPED
}

data class AssetLocation(
    val room: String? = null,
    val zone: String? = null,
    val building: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val description: String? = null
)

/**
 * Device — a physical thing that produces observations.
 * Can be standalone or attached to an Asset.
 */
data class Device(
    val id: DeviceId,
    val name: String,
    val type: DeviceType,
    val protocol: DeviceProtocol,
    val capabilities: Set<Capability>,
    val assetId: AssetId? = null,
    val connected: Boolean = false,
    val batteryLevel: Double? = null,
    val firmwareVersion: String? = null,
    val lastSeen: Instant? = null,
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: Instant = Instant.now()
)

enum class DeviceType {
    THERMAL_CAMERA,
    CAMERA,
    SMART_PLUG,
    LIGHT,
    THERMOSTAT,
    LOCK,
    SENSOR,
    VALVE,
    METER,
    TAG_BLE,
    TAG_UWB,
    OBD,
    ACOUSTIC,
    VIBRATION,
    ENERGY_MONITOR,
    FLOW_SENSOR,
    LEAK_SENSOR,
    SMOKE_SENSOR,
    TEMPERATURE_SENSOR,
    HUMIDITY_SENSOR,
    SPEAKER,
    REMOTE_IR,
    REMOTE_RF,
    HUB,
    OTHER
}

enum class DeviceProtocol {
    BLE,
    UWB,
    WIFI,
    MATTER,
    ONVIF,
    RTSP,
    ZIGBEE,
    Z_WAVE,
    MODBUS,
    OBD2,
    USB,
    NFC,
    QR,
    CAMERA,
    MICROPHONE,
    ACCELEROMETER,
    GPS,
    OTHER
}

// ─────────────────────────────────────────────────────────────
// CAPABILITIES
// ─────────────────────────────────────────────────────────────

/**
 * What a device can do or sense.
 */
sealed class Capability {
    data class CanObserve(val sensorType: SensorType) : Capability()
    data class CanActuate(val actuatorType: ActuatorType) : Capability()
    data class CanIdentify(val identificationMethod: IdentificationMethod) : Capability()
    data class CanCommunicate(val commType: CommunicationType) : Capability()
}

enum class SensorType {
    TEMPERATURE,
    HUMIDITY,
    PRESSURE,
    FLOW_RATE,
    VIBRATION,
    ACOUSTIC,
    LIGHT,
    PROXIMITY,
    MOTION,
    SMOKE,
    FLAME,
    CO,
    GAS,
    LEAK,
    VOLTAGE,
    CURRENT,
    POWER,
    ENERGY,
    RSSI,
    GPS,
    CAMERA_THERMAL,
    CAMERA_NORMAL,
    CAMERA_DEPTH,
    UWB_RANGE,
    BLE_SIGNAL,
    OTHER
}

enum class ActuatorType {
    VALVE_OPEN,
    VALVE_CLOSE,
    MOTOR_START,
    MOTOR_STOP,
    LIGHT_ON,
    LIGHT_OFF,
    LOCK_LOCK,
    LOCK_UNLOCK,
    SPEAKER_PLAY,
    SPEAKER_STOP,
    RELAY_ON,
    RELAY_OFF,
    IR_SEND,
    OTHER
}

enum class IdentificationMethod {
    BARCODE,
    QR_CODE,
    NFC,
    OCR_SERIAL,
    OCR_MODEL,
    BLE_ADVERTISEMENT,
    UWB_RANGING,
    MANUAL_ENTRY,
    PHOTO_RECOGNITION,
    OTHER
}

enum class CommunicationType {
    BLE_SCAN,
    BLE_CONNECT,
    WIFI_CONNECT,
    MATTER_CONTROL,
    ONVIF_PROFILE,
    RTSP_STREAM,
    MODBUS_READ,
    OBD2_QUERY,
    USB_SERIAL,
    NFC_READ,
    CAMERA_CAPTURE,
    MICROPHONE_RECORD,
    ACCELEROMETER_READ,
    OTHER
}

// ─────────────────────────────────────────────────────────────
// OBSERVATIONS
// ─────────────────────────────────────────────────────────────

/**
 * A single observation from a device or user input.
 */
data class Observation(
    val id: ObservationId,
    val deviceId: DeviceId?,
    val assetId: AssetId?,
    val sensorType: SensorType,
    val timestamp: Instant,
    val readings: Map<String, Double>,
    val audioFile: String? = null,
    val imageFile: String? = null,
    val videoFile: String? = null,
    val spectrogramFile: String? = null,
    val fftFile: String? = null,
    val vibrationFile: String? = null,
    val rawFile: String? = null,
    val confidence: Double = 0.9,
    val source: ObservationSource = ObservationSource.DEVICE,
    val metadata: Map<String, String> = emptyMap()
)

enum class ObservationSource {
    DEVICE,
    USER_INPUT,
    AI_INFERENCE,
    SCHEDULED,
    TRIGGERED,
    MANUAL
}

// ─────────────────────────────────────────────────────────────
// ANOMALIES
// ─────────────────────────────────────────────────────────────

/**
 * Something abnormal detected from observations.
 */
data class Anomaly(
    val id: AnomalyId,
    val assetId: AssetId?,
    val deviceId: DeviceId?,
    val type: AnomalyType,
    val severity: Severity,
    val description: String,
    val evidence: List<EvidenceId>,
    val confidence: Double,
    val detectedAt: Instant,
    val resolvedAt: Instant? = null,
    val rootCause: String? = null,
    val recommendedActions: List<String> = emptyList(),
    val baseline: Map<String, Double> = emptyMap(),
    val current: Map<String, Double> = emptyMap(),
    val change: Map<String, Double> = emptyMap()
)

enum class AnomalyType {
    TEMPERATURE_HIGH,
    TEMPERATURE_LOW,
    TEMPERATURE_RAPID_CHANGE,
    VIBRATION_HIGH,
    VIBRATION_NEW_PATTERN,
    NOISE_ABNORMAL,
    NOISE_NEW_HARMONIC,
    POWER_CONSUMPTION_HIGH,
    POWER_CONSUMPTION_LOW,
    FLOW_ABNORMAL,
    LEAK_DETECTED,
    SMOKE_DETECTED,
    FLAME_DETECTED,
    CO_DETECTED,
    MOVEMENT_UNEXPECTED,
    SIGNAL_LOST,
    SIGNAL_WEAK,
    BATTERY_LOW,
    EFFICIENCY_DEGRADATION,
    MECHANICAL_WEAR,
    ELECTRICAL_ANOMALY,
    OTHER
}

enum class Severity {
    INFO,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

// ─────────────────────────────────────────────────────────────
// ACTIONS
// ─────────────────────────────────────────────────────────────

/**
 * Something that was done or recommended.
 */
data class Action(
    val id: ActionId,
    val assetId: AssetId?,
    val deviceId: DeviceId?,
    val type: ActionType,
    val description: String,
    val performedAt: Instant,
    val performedBy: ActionPerformer,
    val result: ActionResult,
    val cost: CostId? = null,
    val evidence: List<EvidenceId> = emptyList(),
    val followUp: MaintenanceId? = null
)

enum class ActionType {
    INSPECT,
    DIAGNOSE,
    REPAIR,
    REPLACE,
    CLEAN,
    CALIBRATE,
    ADJUST,
    TEST,
    ACTIVATE,
    DEACTIVATE,
    EMERGENCY_STOP,
    MAINTENANCE,
    OTHER
}

enum class ActionPerformer {
    USER,
    AI_RECOMMENDED,
    AI_EXECUTED,
    PROFESSIONAL,
    SCHEDULED,
    AUTOMATIC
}

enum class ActionResult {
    SUCCESS,
    PARTIAL,
    FAILED,
    PENDING,
    CANCELLED
}

// ─────────────────────────────────────────────────────────────
// EVIDENCE
// ─────────────────────────────────────────────────────────────

/**
 * Proof that something happened.
 */
data class Evidence(
    val id: EvidenceId,
    val type: EvidenceType,
    val timestamp: Instant,
    val filePath: String,
    val description: String,
    val hash: String? = null,
    val previousHash: String? = null,
    val source: EvidenceSource,
    val confidence: Double = 1.0
)

enum class EvidenceType {
    PHOTO,
    VIDEO,
    AUDIO,
    SPECTROGRAM,
    FFT,
    VIBRATION_WAVEFORM,
    DOCUMENT_SCAN,
    OCR_RESULT,
    SENSOR_READING,
    SYSTEM_LOG,
    USER_NOTE,
    INVOICE,
    WARRANTY_CARD,
    SERIAL_PHOTO,
    OTHER
}

enum class EvidenceSource {
    CAMERA,
    MICROPHONE,
    SENSOR,
    USER,
    AI,
    SYSTEM,
    DOCUMENT_SCANNER,
    OTHER
}

// ─────────────────────────────────────────────────────────────
// MAINTENANCE
// ─────────────────────────────────────────────────────────────

/**
 * A maintenance task — scheduled, overdue, or completed.
 */
data class MaintenanceTask(
    val id: MaintenanceId,
    val assetId: AssetId,
    val title: String,
    val description: String,
    val type: MaintenanceType,
    val priority: Priority,
    val scheduledDate: Instant,
    val dueDate: Instant,
    val completedDate: Instant? = null,
    val intervalDays: Int? = null,
    val completedActions: List<ActionId> = emptyList(),
    val estimatedCost: Double? = null,
    val actualCost: Double? = null,
    val currency: String = "CRC",
    val notes: String? = null,
    val recurring: Boolean = false,
    val nextDue: Instant? = null
)

enum class MaintenanceType {
    PREVENTIVE,
    CORRECTIVE,
    PREDICTIVE,
    EMERGENCY,
    INSPECTION,
    CLEANING,
    CALIBRATION,
    REPLACEMENT,
    OTHER
}

enum class Priority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT,
    OVERDUE
}

// ─────────────────────────────────────────────────────────────
// COST
// ─────────────────────────────────────────────────────────────

/**
 * Cost associated with an asset or action.
 */
data class Cost(
    val id: CostId,
    val assetId: AssetId?,
    val actionId: ActionId?,
    val amount: Double,
    val currency: String = "CRC",
    val type: CostType,
    val description: String,
    val date: Instant,
    val receipt: EvidenceId? = null,
    val vendor: String? = null
)

enum class CostType {
    PURCHASE,
    REPAIR,
    MAINTENANCE,
    REPLACEMENT,
    ENERGY,
    WATER,
    GAS,
    INSURANCE,
    DEPRECIATION,
    OTHER
}

// ─────────────────────────────────────────────────────────────
// DIAGNOSIS (Fix AI)
// ─────────────────────────────────────────────────────────────

/**
 * AI-powered diagnosis result.
 */
data class Diagnosis(
    val id: String = UUID.randomUUID().toString(),
    val assetId: AssetId? = null,
    val deviceId: DeviceId? = null,
    val inputType: DiagnosisInputType,
    val timestamp: Instant = Instant.now(),
    val mostLikelyCauses: List<Cause>,
    val checks: List<DiagnosticCheck>,
    val nextTests: List<String>,
    val confidence: Double,
    val rawAnalysis: String? = null
)

enum class DiagnosisInputType {
    CAMERA_AUDIO,
    CAMERA_ONLY,
    AUDIO_ONLY,
    VIBRATION,
    SENSOR_DATA,
    USER_DESCRIPTION,
    OCR,
    COMBINED
}

data class Cause(
    val name: String,
    val probability: Double,
    val explanation: String,
    val evidence: List<String> = emptyList(),
    val source: CauseSource = CauseSource.HEURISTIC
)

enum class CauseSource {
    HEURISTIC,       // Category-based estimate, NOT measured
    MEASURED,        // Derived from actual sensor data
    ML_MODEL,        // Derived from trained model
    USER_REPORTED    // Provided by user
}

data class DiagnosticCheck(
    val name: String,
    val status: CheckStatus,
    val detail: String? = null
)

enum class CheckStatus {
    PASSED,
    WARNING,
    FAILED,
    UNKNOWN
}

// ─────────────────────────────────────────────────────────────
// WARRANTY (Warranty Vault)
// ─────────────────────────────────────────────────────────────

/**
 * Warranty information extracted from documents.
 */
data class WarrantyInfo(
    val assetId: AssetId,
    val provider: String,
    val purchaseDate: Instant,
    val warrantyStart: Instant,
    val warrantyEnd: Instant,
    val warrantyMonths: Int,
    val purchasePrice: Double,
    val currency: String = "CRC",
    val serialNumber: String? = null,
    val invoiceScan: EvidenceId? = null,
    val warrantyCardScan: EvidenceId? = null,
    val manualScan: EvidenceId? = null,
    val isActive: Boolean = true,
    val daysRemaining: Long = 0,
    val notes: String? = null
)

// ─────────────────────────────────────────────────────────────
// MAINTENANCE SCHEDULE (Maintenance OS)
// ─────────────────────────────────────────────────────────────

/**
 * A complete maintenance schedule for all assets.
 */
data class MaintenanceSchedule(
    val assetId: AssetId,
    val tasks: List<MaintenanceTask>,
    val lastCompleted: Instant? = null,
    val nextDue: Instant? = null,
    val overdueCount: Int = 0,
    val upcomingCount: Int = 0
)

// ─────────────────────────────────────────────────────────────
// HOME / ROOM / ZONE
// ─────────────────────────────────────────────────────────────

data class Home(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val rooms: List<Room>,
    val assets: List<AssetId>,
    val devices: List<DeviceId>
)

data class Room(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val floor: Int? = null,
    val assets: List<AssetId>,
    val devices: List<DeviceId>,
    val sensors: List<DeviceId>
)

// ─────────────────────────────────────────────────────────────
// SUPREME STATE — THE COMPLETE USER STATE
// ─────────────────────────────────────────────────────────────

/**
 * The complete state of everything Supreme knows about.
 */
data class SupremeState(
    val home: Home? = null,
    val assets: List<Asset> = emptyList(),
    val devices: List<Device> = emptyList(),
    val observations: List<Observation> = emptyList(),
    val anomalies: List<Anomaly> = emptyList(),
    val actions: List<Action> = emptyList(),
    val evidence: List<Evidence> = emptyList(),
    val maintenance: List<MaintenanceTask> = emptyList(),
    val costs: List<Cost> = emptyList(),
    val warranties: List<WarrantyInfo> = emptyList(),
    val diagnoses: List<Diagnosis> = emptyList(),
    val lastUpdated: Instant = Instant.now()
) {
    fun getAssetById(id: AssetId): Asset? = assets.find { it.id == id }
    fun getDeviceById(id: DeviceId): Device? = devices.find { it.id == id }
    fun getAssetsByCategory(category: AssetCategory): List<Asset> = assets.filter { it.category == category }
    fun getDevicesByType(type: DeviceType): List<Device> = devices.filter { it.type == type }
    fun getActiveAnomalies(): List<Anomaly> = anomalies.filter { it.resolvedAt == null }
    fun getOverdueMaintenance(): List<MaintenanceTask> = maintenance.filter {
        it.completedDate == null && it.dueDate.isBefore(Instant.now())
    }
    fun getUpcomingMaintenance(daysAhead: Int = 30): List<MaintenanceTask> = maintenance.filter {
        it.completedDate == null && it.dueDate.isAfter(Instant.now())
    }
    fun getAssetCosts(assetId: AssetId): List<Cost> = costs.filter { it.assetId == assetId }
    fun getTotalAssetValue(): Double = assets.sumOf { it.purchasePrice ?: 0.0 }
}
