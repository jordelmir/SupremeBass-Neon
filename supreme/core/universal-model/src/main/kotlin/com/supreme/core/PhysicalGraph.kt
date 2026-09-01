package com.supreme.core

import com.supreme.truth.TruthAuthority
import java.time.Instant
import java.util.UUID

/**
 * Physical Knowledge Graph — the relational backbone of Supreme.
 *
 * Connects: Asset → Component → Sensor → Observation → Symptom →
 *           FailureMode → DiagnosticTest → RepairProcedure → Part →
 *           Tool → Cost → Warranty → Evidence
 *
 * The same ontology works for:
 *   Vehicle:  engine → cylinder 1 → misfire → P0301 → compression test → injector → repair
 *   Appliance: motor → bearing → vibration → FFT peak → replacement → part → cost
 *   Building:  panel → breaker → thermal → hotspot → inspection → procedure
 *
 * Relationships are directed, typed, and carry metadata.
 * No relationship implies physical truth without observation.
 */

// ─────────────────────────────────────────────────────────────
// GRAPH IDENTIFIERS
// ─────────────────────────────────────────────────────────────

@JvmInline
value class ComponentId(val value: String) {
    companion object { fun generate() = ComponentId(UUID.randomUUID().toString()) }
}

@JvmInline
value class FailureModeId(val value: String) {
    companion object { fun generate() = FailureModeId(UUID.randomUUID().toString()) }
}

@JvmInline
value class DiagnosticTestId(val value: String) {
    companion object { fun generate() = DiagnosticTestId(UUID.randomUUID().toString()) }
}

@JvmInline
value class RepairProcedureId(val value: String) {
    companion object { fun generate() = RepairProcedureId(UUID.randomUUID().toString()) }
}

@JvmInline
value class PartId(val value: String) {
    companion object { fun generate() = PartId(UUID.randomUUID().toString()) }
}

@JvmInline
value class ToolId(val value: String) {
    companion object { fun generate() = ToolId(UUID.randomUUID().toString()) }
}

@JvmInline
value class SymptomId(val value: String) {
    companion object { fun generate() = SymptomId(UUID.randomUUID().toString()) }
}

// ─────────────────────────────────────────────────────────────
// GRAPH ENTITIES
// ─────────────────────────────────────────────────────────────

/**
 * Component — a sub-part of an Asset.
 * A car has engine, transmission, brakes. A washing machine has motor, drum, pump.
 */
data class Component(
    val id: ComponentId,
    val assetId: AssetId,
    val name: String,
    val type: ComponentType,
    val parentId: ComponentId? = null,
    val manufacturer: String? = null,
    val partNumber: String? = null,
    val specifications: Map<String, String> = emptyMap(),
    val installDate: Instant? = null,
    val expectedLifetimeHours: Double? = null,
    val createdAt: Instant = Instant.now()
)

enum class ComponentType {
    ENGINE,
    MOTOR,
    TRANSMISSION,
    BRAKE,
    BEARING,
    BELT,
    FILTER,
    PUMP,
    VALVE,
    SENSOR,
    CONTROLLER,
    PCB,
    WIRING,
    HOSE,
    TANK,
    COMPRESSOR,
    FAN,
    HEATER,
    COOLER,
    OTHER
}

/**
 * Symptom — what the user or system observes.
 * Not a diagnosis; just a description of what's happening.
 */
data class Symptom(
    val id: SymptomId,
    val assetId: AssetId,
    val componentId: ComponentId? = null,
    val name: String,
    val description: String,
    val observedAt: Instant,
    val reportedBy: TruthAuthority,
    val severity: Severity,
    val observationIds: List<ObservationId> = emptyList(),
    val isActive: Boolean = true
)

/**
 * FailureMode — how a component can fail.
 * Linked to symptoms that indicate this failure mode.
 */
data class FailureMode(
    val id: FailureModeId,
    val componentType: ComponentType,
    val name: String,
    val description: String,
    val mechanism: String,
    val indicators: List<String>,
    val symptoms: List<SymptomId>,
    val severity: Severity,
    val probability: Double = 0.0,
    val meanTimeBetweenFailuresHours: Double? = null,
    val references: List<String> = emptyList()
)

/**
 * DiagnosticTest — a test that discriminates between failure modes.
 * The Active Diagnostic Engine selects tests by:
 *   NextTest = argmax(ExpectedInformationGain(test) - Cost(test) - Risk(test))
 */
data class DiagnosticTest(
    val id: DiagnosticTestId,
    val name: String,
    val description: String,
    val type: DiagnosticTestType,
    val targetComponentType: ComponentType? = null,
    val discriminatedFailureModes: List<FailureModeId>,
    val requiredCapability: Capability? = null,
    val estimatedCost: Double = 0.0,
    val estimatedTimeMinutes: Int = 0,
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val instructions: List<String> = emptyList(),
    val expectedReadings: Map<String, String> = emptyMap(),
    val passCriteria: Map<String, String> = emptyMap()
)

enum class DiagnosticTestType {
    VISUAL_INSPECTION,
    AUDIO_RECORDING,
    VIBRATION_MEASUREMENT,
    THERMAL_IMAGING,
    ELECTRICAL_TEST,
    COMPRESSION_TEST,
    FLOW_TEST,
    PRESSURE_TEST,
    LEAK_TEST,
    ALIGNMENT_CHECK,
    BALANCE_CHECK,
    SOFTWARE_DIAGNOSTIC,
    OBD_QUERY,
    MULTI_METER,
    OSCILLOSCOPE,
    OTHER
}

enum class RiskLevel {
    NONE,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * RepairProcedure — step-by-step repair instructions.
 */
data class RepairProcedure(
    val id: RepairProcedureId,
    val failureModeId: FailureModeId,
    val name: String,
    val description: String,
    val difficulty: RepairDifficulty,
    val estimatedTimeMinutes: Int,
    val requiredTools: List<ToolId>,
    val requiredParts: List<PartId>,
    val steps: List<RepairStep>,
    val warnings: List<String> = emptyList(),
    val references: List<String> = emptyList(),
    val estimatedCost: Double = 0.0,
    val currency: String = "USD"
)

enum class RepairDifficulty {
    TRIVIAL,
    EASY,
    MODERATE,
    DIFFICULT,
    PROFESSIONAL_ONLY
}

data class RepairStep(
    val order: Int,
    val instruction: String,
    val toolId: ToolId? = null,
    val torqueNm: Double? = null,
    val warning: String? = null,
    val photoRef: String? = null
)

/**
 * Part — a replacement component.
 */
data class Part(
    val id: PartId,
    val name: String,
    val description: String,
    val oemNumber: String? = null,
    val compatiblePartNumbers: List<String> = emptyList(),
    val manufacturer: String? = null,
    val targetComponentType: ComponentType,
    val price: Double? = null,
    val currency: String = "USD",
    val supplier: String? = null,
    val supplierUrl: String? = null,
    val inStock: Boolean? = null,
    val leadTimeDays: Int? = null,
    val warrantyMonths: Int? = null,
    val specifications: Map<String, String> = emptyMap()
)

/**
 * Tool — a tool needed for repair.
 */
data class Tool(
    val id: ToolId,
    val name: String,
    val description: String,
    val type: ToolType,
    val size: String? = null,
    val torqueRangeNm: String? = null,
    val isSpecialty: Boolean = false,
    val estimatedCost: Double? = null,
    val currency: String = "USD"
)

enum class ToolType {
    WRENCH,
    SOCKET,
    SCREWDRIVER,
    PLIERS,
    MULTIMETER,
    OSCILLOSCOPE,
    TORQUE_WRENCH,
    PRESSURE_GAUGE,
    FLOW_METER,
    THERMAL_CAMERA,
    DIAGNOSTIC_SCANNER,
    LIFT,
    JACK,
    OTHER
}

// ─────────────────────────────────────────────────────────────
// GRAPH RELATIONSHIPS
// ─────────────────────────────────────────────────────────────

/**
 * A typed, directed edge in the Physical Knowledge Graph.
 * Every edge carries provenance and timestamp.
 */
data class GraphEdge(
    val id: String = UUID.randomUUID().toString(),
    val from: GraphNodeId,
    val to: GraphNodeId,
    val type: EdgeType,
    val weight: Double = 1.0,
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: Instant = Instant.now(),
    val source: TruthAuthority = TruthAuthority.USER_REPORTED
)

/**
 * All node types that can participate in graph edges.
 */
sealed class GraphNodeId {
    data class Asset(val id: AssetId) : GraphNodeId()
    data class Component(val id: ComponentId) : GraphNodeId()
    data class Device(val id: DeviceId) : GraphNodeId()
    data class Observation(val id: ObservationId) : GraphNodeId()
    data class Symptom(val id: SymptomId) : GraphNodeId()
    data class FailureMode(val id: FailureModeId) : GraphNodeId()
    data class DiagnosticTest(val id: DiagnosticTestId) : GraphNodeId()
    data class RepairProcedure(val id: RepairProcedureId) : GraphNodeId()
    data class Part(val id: PartId) : GraphNodeId()
    data class Tool(val id: ToolId) : GraphNodeId()
    data class Cost(val id: CostId) : GraphNodeId()
    data class Evidence(val id: EvidenceId) : GraphNodeId()
    data class Warranty(val assetId: AssetId) : GraphNodeId()
}

/**
 * Relationship types between graph nodes.
 */
enum class EdgeType {
    // Structural
    HAS_COMPONENT,         // Asset → Component
    HAS_SUB_COMPONENT,     // Component → Component (parent → child)
    HAS_DEVICE,            // Asset/Component → Device
    HAS_SENSOR,            // Component → Device (sensor)

    // Observational
    PRODUCES,              // Device/Sensor → Observation
    INDICATES,             // Observation → Symptom

    // Diagnostic
    SYMPTOM_OF,            // Symptom → FailureMode
    DISCRIMINATES,         // DiagnosticTest → FailureMode
    REQUIRES_TEST,         // FailureMode → DiagnosticTest
    TEST_FOR_COMPONENT,    // DiagnosticTest → Component

    // Repair
    REPAIRED_BY,           // FailureMode → RepairProcedure
    REQUIRES_PART,         // RepairProcedure → Part
    REQUIRES_TOOL,         // RepairProcedure → Tool
    REPLACES,              // Part → Component

    // Economic
    COST_OF,               // Cost → Action/RepairProcedure
    WARRANTY_COVERS,       // Warranty → Component/FailureMode
    EVIDENCE_OF,           // Evidence → Observation/Action/RepairProcedure

    // Temporal
    PRECEDES,              // Generic temporal ordering
    CAUSED_BY,             // Event → Event (causal)
    FOLLOWED_BY            // Event → Event (sequential)
}
