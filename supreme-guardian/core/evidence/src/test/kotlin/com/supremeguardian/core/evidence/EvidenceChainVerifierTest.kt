package com.supremeguardian.core.evidence

import com.supremeguardian.core.incident.Evidence
import com.supremeguardian.core.incident.EvidenceType
import com.supremeguardian.core.shared.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class EvidenceChainVerifierTest {

    private val verifier = EvidenceChainVerifier()

    @Test
    fun `test empty chain is invalid`() {
        val result = verifier.verify(emptyList())

        assertFalse(result.valid, "Empty chain should be invalid")
        assertTrue(result.errors.contains("Empty evidence chain"), "Should report empty chain error")
    }

    @Test
    fun `test single valid evidence entry`() {
        val evidence = Evidence(
            type = EvidenceType.OBSERVATION,
            source = EvidenceSource("sensor-1", "ThermalCamera"),
            timestamp = GuardianTimestamp(),
            data = mapOf("temperature" to "75.0"),
            confidence = Confidence(0.9, ObservationAuthority.HARDWARE_MEASURED)
        )

        val result = verifier.verify(listOf(evidence))

        assertTrue(result.valid, "Single valid evidence should be valid")
        assertEquals(1, result.statistics.totalEntries, "Should have 1 entry")
    }

    @Test
    fun `test hash computation is deterministic`() {
        val evidence = Evidence(
            type = EvidenceType.OBSERVATION,
            source = EvidenceSource("sensor-1", "ThermalCamera"),
            timestamp = GuardianTimestamp(),
            data = mapOf("temperature" to "75.0"),
            confidence = Confidence(0.9, ObservationAuthority.HARDWARE_MEASURED)
        )

        val hash1 = verifier.computeHash(evidence)
        val hash2 = verifier.computeHash(evidence)

        assertEquals(hash1, hash2, "Hash computation should be deterministic")
    }

    @Test
    fun `test different evidence produces different hashes`() {
        val evidence1 = Evidence(
            type = EvidenceType.OBSERVATION,
            source = EvidenceSource("sensor-1", "ThermalCamera"),
            timestamp = GuardianTimestamp(),
            data = mapOf("temperature" to "75.0"),
            confidence = Confidence(0.9, ObservationAuthority.HARDWARE_MEASURED)
        )

        val evidence2 = Evidence(
            type = EvidenceType.OBSERVATION,
            source = EvidenceSource("sensor-1", "ThermalCamera"),
            timestamp = GuardianTimestamp(),
            data = mapOf("temperature" to "80.0"),
            confidence = Confidence(0.9, ObservationAuthority.HARDWARE_MEASURED)
        )

        val hash1 = verifier.computeHash(evidence1)
        val hash2 = verifier.computeHash(evidence2)

        assertNotEquals(hash1, hash2, "Different evidence should produce different hashes")
    }

    @Test
    fun `test timestamp ordering validation`() {
        val time1 = GuardianTimestamp()
        val time2 = GuardianTimestamp()

        val evidence1 = Evidence(
            type = EvidenceType.OBSERVATION,
            source = EvidenceSource("sensor-1", "ThermalCamera"),
            timestamp = time1,
            data = mapOf("temperature" to "75.0"),
            confidence = Confidence(0.9, ObservationAuthority.HARDWARE_MEASURED)
        )

        val evidence2 = Evidence(
            type = EvidenceType.OBSERVATION,
            source = EvidenceSource("sensor-1", "ThermalCamera"),
            timestamp = time2,
            data = mapOf("temperature" to "80.0"),
            confidence = Confidence(0.9, ObservationAuthority.HARDWARE_MEASURED),
            previousHash = verifier.computeHash(evidence1)
        )

        val result = verifier.verify(listOf(evidence1, evidence2))

        assertTrue(result.valid, "Valid timestamp order should be valid")
        assertEquals(2, result.statistics.validTimestamps, "Should have 2 valid timestamps")
    }

    @Test
    fun `test out of order timestamps are invalid`() {
        val time1 = GuardianTimestamp()
        val time2 = GuardianTimestamp()

        val evidence1 = Evidence(
            type = EvidenceType.OBSERVATION,
            source = EvidenceSource("sensor-1", "ThermalCamera"),
            timestamp = time2, // Later timestamp first
            data = mapOf("temperature" to "75.0"),
            confidence = Confidence(0.9, ObservationAuthority.HARDWARE_MEASURED)
        )

        val evidence2 = Evidence(
            type = EvidenceType.OBSERVATION,
            source = EvidenceSource("sensor-1", "ThermalCamera"),
            timestamp = time1, // Earlier timestamp second
            data = mapOf("temperature" to "80.0"),
            confidence = Confidence(0.9, ObservationAuthority.HARDWARE_MEASURED)
        )

        val result = verifier.verify(listOf(evidence1, evidence2))

        assertFalse(result.valid, "Out of order timestamps should be invalid")
        assertTrue(result.errors.any { it.contains("Timestamp out of order") }, "Should report timestamp error")
    }

    @Test
    fun `test confidence validation`() {
        val evidence = Evidence(
            type = EvidenceType.OBSERVATION,
            source = EvidenceSource("sensor-1", "ThermalCamera"),
            timestamp = GuardianTimestamp(),
            data = mapOf("temperature" to "75.0"),
            confidence = Confidence(0.9, ObservationAuthority.HARDWARE_MEASURED)
        )

        val result = verifier.verify(listOf(evidence))

        assertEquals(1, result.statistics.validConfidence, "Valid confidence should be counted")
    }

    @Test
    fun `test audit export contains all evidence`() {
        val evidence1 = Evidence(
            type = EvidenceType.OBSERVATION,
            source = EvidenceSource("sensor-1", "ThermalCamera"),
            timestamp = GuardianTimestamp(),
            data = mapOf("temperature" to "75.0"),
            confidence = Confidence(0.9, ObservationAuthority.HARDWARE_MEASURED)
        )

        val evidence2 = Evidence(
            type = EvidenceType.ACTOR_ACTION,
            source = EvidenceSource("actuator-1", "MistController"),
            timestamp = GuardianTimestamp(),
            data = mapOf("action" to "activated"),
            confidence = Confidence(1.0, ObservationAuthority.SYSTEM_GENERATED)
        )

        val export = verifier.exportForAudit(listOf(evidence1, evidence2))

        assertEquals(2, export.evidence.size, "Export should contain all evidence")
        assertTrue(export.metadata.containsKey("chain_length"), "Export should have chain length metadata")
        assertEquals("2", export.metadata["chain_length"], "Chain length should be 2")
    }

    @Test
    fun `test verification report generation`() {
        val evidence = Evidence(
            type = EvidenceType.OBSERVATION,
            source = EvidenceSource("sensor-1", "ThermalCamera"),
            timestamp = GuardianTimestamp(),
            data = mapOf("temperature" to "75.0"),
            confidence = Confidence(0.9, ObservationAuthority.HARDWARE_MEASURED)
        )

        val report = verifier.generateReport(listOf(evidence))

        assertNotNull(report, "Report should not be null")
        assertEquals(1, report.chainLength, "Chain length should be 1")
        assertNotNull(report.firstTimestamp, "First timestamp should not be null")
        assertNotNull(report.lastTimestamp, "Last timestamp should not be null")
        assertNotNull(report.generatedAt, "Generated at should not be null")
    }
}
