package com.supremeguardian.core.evidence

import com.supremeguardian.core.incident.IncidentId
import com.supremeguardian.core.shared.*
import java.security.MessageDigest
import java.time.Instant

/**
 * Evidence Chain Verifier — verifies the integrity of the evidence chain.
 *
 * Responsibilities:
 *   1. Verify SHA-256 hash chain integrity
 *   2. Detect tampering or missing evidence
 *   3. Validate timestamp ordering
 *   4. Generate verification reports
 *   5. Export evidence chain for audit
 *
 * Security:
 *   - SHA-256 hash chain (tamper-evident)
 *   - Timestamp ordering verification
 *   - Source validation
 *   - Confidence level verification
 */
class EvidenceChainVerifier {

    /**
     * Verify the integrity of an evidence chain.
     */
    fun verify(chain: List<Evidence>): VerificationResult {
        if (chain.isEmpty()) {
            return VerificationResult(
                valid = false,
                errors = listOf("Empty evidence chain"),
                warnings = emptyList(),
                statistics = VerificationStatistics(0, 0, 0, 0)
            )
        }

        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        var validHashes = 0
        var validTimestamps = 0
        var validSources = 0
        var validConfidence = 0

        // Verify each evidence entry
        for (i in chain.indices) {
            val evidence = chain[i]

            // Verify hash
            if (evidence.hash != null) {
                val expectedHash = computeHash(evidence)
                if (evidence.hash != expectedHash) {
                    errors.add("Hash mismatch at index $i: expected $expectedHash, got ${evidence.hash}")
                } else {
                    validHashes++
                }
            } else {
                warnings.add("Missing hash at index $i")
            }

            // Verify timestamp ordering
            if (i > 0) {
                val prevTimestamp = chain[i - 1].timestamp.toInstant()?.toEpochMilli() ?: 0
                val currTimestamp = evidence.timestamp.toInstant()?.toEpochMilli() ?: 0
                if (currTimestamp < prevTimestamp) {
                    errors.add("Timestamp out of order at index $i: ${evidence.timestamp} < ${chain[i - 1].timestamp}")
                } else {
                    validTimestamps++
                }
            } else {
                validTimestamps++
            }

            // Verify source
            if (evidence.source.sourceId.isNotEmpty() && evidence.source.sourceType.isNotEmpty()) {
                validSources++
            } else {
                warnings.add("Empty source at index $i")
            }

            // Verify confidence
            if (evidence.confidence.value in 0.0..1.0) {
                validConfidence++
            } else {
                errors.add("Invalid confidence at index $i: ${evidence.confidence.value}")
            }
        }

        // Verify hash chain linkage
        for (i in 1 until chain.size) {
            val prevEvidence = chain[i - 1]
            val currEvidence = chain[i]

            if (prevEvidence.hash != null && currEvidence.previousHash != null) {
                if (prevEvidence.hash != currEvidence.previousHash) {
                    errors.add("Hash chain broken at index $i: previous hash mismatch")
                }
            }
        }

        return VerificationResult(
            valid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
            statistics = VerificationStatistics(
                totalEntries = chain.size,
                validHashes = validHashes,
                validTimestamps = validTimestamps,
                validSources = validSources
            )
        )
    }

    /**
     * Compute SHA-256 hash for evidence.
     */
    fun computeHash(evidence: Evidence): String {
        val data = buildString {
            append(evidence.type.name)
            append(evidence.source.sourceId)
            append(evidence.source.sourceType)
            append(evidence.timestamp.iso8601)
            append(evidence.data.toString())
            append(evidence.confidence.value.toString())
            evidence.previousHash?.let { append(it) }
        }

        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(data.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Generate verification report.
     */
    fun generateReport(chain: List<Evidence>): VerificationReport {
        val verification = verify(chain)

        return VerificationReport(
            incidentId = chain.firstOrNull()?.let {
                // Extract incident ID from first evidence
                IncidentId("unknown") // Placeholder
            },
            verification = verification,
            chainLength = chain.size,
            firstTimestamp = chain.firstOrNull()?.timestamp,
            lastTimestamp = chain.lastOrNull()?.timestamp,
            generatedAt = GuardianTimestamp()
        )
    }

    /**
     * Export evidence chain for audit.
     */
    fun exportForAudit(chain: List<Evidence>): AuditExport {
        return AuditExport(
            evidence = chain,
            metadata = mapOf(
                "chain_length" to chain.size.toString(),
                "first_timestamp" to (chain.firstOrNull()?.timestamp?.iso8601 ?: ""),
                "last_timestamp" to (chain.lastOrNull()?.timestamp?.iso8601 ?: ""),
                "export_timestamp" to GuardianTimestamp().iso8601
            )
        )
    }
}

/**
 * Verification result.
 */
data class VerificationResult(
    val valid: Boolean,
    val errors: List<String>,
    val warnings: List<String>,
    val statistics: VerificationStatistics
)

/**
 * Verification statistics.
 */
data class VerificationStatistics(
    val totalEntries: Int,
    val validHashes: Int,
    val validTimestamps: Int,
    val validSources: Int
)

/**
 * Verification report.
 */
data class VerificationReport(
    val incidentId: IncidentId?,
    val verification: VerificationResult,
    val chainLength: Int,
    val firstTimestamp: GuardianTimestamp?,
    val lastTimestamp: GuardianTimestamp?,
    val generatedAt: GuardianTimestamp
)

/**
 * Audit export.
 */
data class AuditExport(
    val evidence: List<Evidence>,
    val metadata: Map<String, String>
)
