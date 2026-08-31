package com.supremeguardian.core.evidence

import com.supremeguardian.core.shared.*

/**
 * Evidence chain — immutable record of all evidence for an incident.
 *
 * Principles:
 *   - Every entry is append-only (no mutation)
 *   - Every entry has a cryptographic hash chain
 *   - Every entry has a timestamp and source
 *   - Evidence can be physical, digital, or derived
 */
data class EvidenceChain(
    val entries: List<EvidenceEntry> = emptyList()
) {
    val size: Int get() = entries.size
    val isEmpty: Boolean get() = entries.isEmpty()

    /**
     * Append new evidence to the chain.
     * Returns a new chain (immutable).
     */
    fun append(entry: EvidenceEntry): EvidenceChain {
        val prevHash = entries.lastOrNull()?.hash ?: "GENESIS"
        return copy(entries = entries + entry.copy(previousHash = prevHash))
    }

    /**
     * Verify the hash chain integrity.
     */
    fun verify(): Boolean {
        for (i in entries.indices) {
            val entry = entries[i]
            val expectedPrevHash = if (i == 0) "GENESIS" else entries[i - 1].hash
            if (entry.previousHash != expectedPrevHash) return false
        }
        return true
    }

    /**
     * Get all evidence of a specific type.
     */
    fun getByType(type: EvidenceType): List<EvidenceEntry> {
        return entries.filter { it.type == type }
    }

    /**
     * Get evidence within a time window.
     */
    fun getTimeRange(start: GuardianTimestamp, end: GuardianTimestamp): List<EvidenceEntry> {
        return entries.filter {
            it.timestamp.instant >= start.instant && it.timestamp.instant <= end.instant
        }
    }
}

/**
 * Single evidence entry — immutable record.
 */
data class EvidenceEntry(
    val id: EvidenceId,
    val timestamp: GuardianTimestamp,
    val type: EvidenceType,
    val source: String, // "thermal:camera-001", "smoke:sensor-042", "user:admin"
    val description: String,
    val data: Map<String, String>, // Key-value pairs for structured data
    val binaryRef: String? = null, // Reference to binary data (image, audio, etc.)
    val confidence: Confidence,
    val previousHash: String = "GENESIS",
    val hash: String = computeHash(this)
) {
    companion object {
        /**
         * Compute SHA-256 hash of evidence entry.
         */
        fun computeHash(entry: EvidenceEntry): String {
            val content = "${entry.id.value}|${entry.timestamp.iso8601}|${entry.type}|${entry.source}|${entry.description}|${entry.previousHash}"
            return try {
                val digest = java.security.MessageDigest.getInstance("SHA-256")
                digest.digest(content.toByteArray()).joinToString("") { "%02x".format(it) }
            } catch (e: Exception) {
                "HASH_ERROR_${content.hashCode()}"
            }
        }
    }
}

/**
 * Types of evidence.
 */
enum class EvidenceType {
    THERMAL_IMAGE,
    THERMAL_MEASUREMENT,
    RGB_IMAGE,
    SMOKE_DETECTION,
    HEAT_DETECTION,
    GAS_DETECTION,
    ELECTRICAL_ANOMALY,
    ACOUSTIC_EVENT,
    FLAME_DETECTION,
    USER_REPORT,
    SYSTEM_EVENT,
    COMMAND_EXECUTED,
    COMMAND_ACKNOWLEDGED,
    PHYSICAL_EFFECT_OBSERVED,
    SENSOR_READING,
    RATE_OF_RISE,
    VIDEO_FRAME,
    AUDIO_RECORDING
}

/**
 * Evidence package — bundled evidence for export/reporting.
 */
data class EvidencePackage(
    val incidentId: IncidentId,
    val chain: EvidenceChain,
    val summary: String,
    val generatedAt: GuardianTimestamp,
    val format: EvidenceFormat
) {
    fun toExportString(): String = buildString {
        appendLine("=== EVIDENCE PACKAGE ===")
        appendLine("Incident: ${incidentId.value}")
        appendLine("Generated: ${generatedAt.iso8601}")
        appendLine("Format: ${format.name}")
        appendLine("Chain integrity: ${if (chain.verify()) "VALID" else "COMPROMISED"}")
        appendLine("Entries: ${chain.size}")
        appendLine()
        appendLine("=== SUMMARY ===")
        appendLine(summary)
        appendLine()
        appendLine("=== EVIDENCE ENTRIES ===")
        for (entry in chain.entries) {
            appendLine("[${entry.timestamp.iso8601}] ${entry.type.name}: ${entry.description}")
            appendLine("  Source: ${entry.source}")
            appendLine("  Confidence: ${entry.confidence.value} (${entry.confidence.authority.name})")
            if (entry.data.isNotEmpty()) {
                appendLine("  Data: ${entry.data}")
            }
            appendLine()
        }
    }
}

enum class EvidenceFormat {
    TEXT,
    JSON,
    PDF,
    XML
}
