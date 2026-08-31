package com.supreme.network

import com.supreme.core.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant

/**
 * Supreme Network Doctor — "Why is my Internet bad?"
 *
 * Diagnoses Wi-Fi and Internet issues using only the phone's built-in capabilities.
 * No external hardware required.
 *
 * Checks:
 * - Wi-Fi RSSI (signal strength)
 * - Link speed
 * - Latency (ping)
 * - DNS resolution
 * - Packet loss
 * - Gateway reachability
 * - Internet connectivity
 * - Band (2.4/5/6 GHz)
 * - Channel congestion
 */

class NetworkDoctorEngine {

    private val _state = MutableStateFlow(NetworkState())
    val state: StateFlow<NetworkState> = _state.asStateFlow()

    private val _history = MutableStateFlow<List<NetworkSnapshot>>(emptyList())
    val history: StateFlow<List<NetworkSnapshot>> = _history.asStateFlow()

    /**
     * Run full network diagnosis.
     */
    suspend fun diagnose(): NetworkDiagnosis {
        _state.value = NetworkState(analyzing = true)

        val checks = mutableListOf<NetworkCheck>()

        // 1. Wi-Fi signal strength
        val rssiCheck = checkWiFiSignal()
        checks.add(rssiCheck)

        // 2. Link speed
        val linkSpeedCheck = checkLinkSpeed()
        checks.add(linkSpeedCheck)

        // 3. Latency
        val latencyCheck = checkLatency()
        checks.add(latencyCheck)

        // 4. DNS
        val dnsCheck = checkDNS()
        checks.add(dnsCheck)

        // 5. Packet loss
        val packetLossCheck = checkPacketLoss()
        checks.add(packetLossCheck)

        // 6. Gateway
        val gatewayCheck = checkGateway()
        checks.add(gatewayCheck)

        // 7. Internet connectivity
        val internetCheck = checkInternet()
        checks.add(internetCheck)

        // 8. Band detection
        val bandCheck = checkBand()
        checks.add(bandCheck)

        // Generate diagnosis
        val diagnosis = generateDiagnosis(checks)

        // Record snapshot
        val snapshot = NetworkSnapshot(
            timestamp = Instant.now(),
            checks = checks,
            diagnosis = diagnosis
        )
        _history.value = _history.value + snapshot

        _state.value = NetworkState(
            analyzing = false,
            lastDiagnosis = diagnosis,
            checks = checks
        )

        return diagnosis
    }

    /**
     * Get historical data for a time period.
     */
    fun getHistory(hoursBack: Int = 24): List<NetworkSnapshot> {
        val cutoff = Instant.now().minusSeconds(hoursBack.toLong() * 3600)
        return _history.value.filter { it.timestamp.isAfter(cutoff) }
    }

    /**
     * Get Wi-Fi coverage map (RSSI at different locations).
     */
    fun getCoverageMap(): List<CoveragePoint> {
        // Future: use location + RSSI to build coverage map
        return emptyList()
    }

    // ─────────────────────────────────────────────────────────────
    // CHECKS
    // ─────────────────────────────────────────────────────────────

    private suspend fun checkWiFiSignal(): NetworkCheck {
        // TODO: Use Android WifiManager to get RSSI
        val rssi = -55 // Placeholder: dBm
        val status = when {
            rssi >= -50 -> CheckStatus.PASSED
            rssi >= -60 -> CheckStatus.WARNING
            else -> CheckStatus.FAILED
        }
        return NetworkCheck(
            name = "Wi-Fi Signal",
            status = status,
            value = rssi.toDouble(),
            unit = "dBm",
            detail = when (status) {
                CheckStatus.PASSED -> "Excellent signal"
                CheckStatus.WARNING -> "Weak signal — may cause slow speeds"
                CheckStatus.FAILED -> "Very weak signal — likely cause of issues"
                CheckStatus.UNKNOWN -> "Unknown"
            },
            recommendation = if (status != CheckStatus.PASSED) {
                "Move closer to router or add mesh node"
            } else null
        )
    }

    private suspend fun checkLinkSpeed(): NetworkCheck {
        // TODO: Use WifiManager to get link speed
        val linkSpeed = 433 // Placeholder: Mbps
        val status = when {
            linkSpeed >= 300 -> CheckStatus.PASSED
            linkSpeed >= 100 -> CheckStatus.WARNING
            else -> CheckStatus.FAILED
        }
        return NetworkCheck(
            name = "Link Speed",
            status = status,
            value = linkSpeed.toDouble(),
            unit = "Mbps",
            detail = when (status) {
                CheckStatus.PASSED -> "Good link speed"
                CheckStatus.WARNING -> "Moderate link speed"
                CheckStatus.FAILED -> "Slow link speed"
                CheckStatus.UNKNOWN -> "Unknown"
            }
        )
    }

    private suspend fun checkLatency(): NetworkCheck {
        // TODO: Ping gateway and internet servers
        val latencyMs = 45.0 // Placeholder
        val status = when {
            latencyMs <= 20 -> CheckStatus.PASSED
            latencyMs <= 50 -> CheckStatus.WARNING
            else -> CheckStatus.FAILED
        }
        return NetworkCheck(
            name = "Latency",
            status = status,
            value = latencyMs,
            unit = "ms",
            detail = when (status) {
                CheckStatus.PASSED -> "Low latency — good for real-time apps"
                CheckStatus.WARNING -> "Moderate latency"
                CheckStatus.FAILED -> "High latency — may cause lag"
                CheckStatus.UNKNOWN -> "Unknown"
            }
        )
    }

    private suspend fun checkDNS(): NetworkCheck {
        // TODO: Test DNS resolution speed
        val dnsMs = 25.0 // Placeholder
        val status = when {
            dnsMs <= 15 -> CheckStatus.PASSED
            dnsMs <= 50 -> CheckStatus.WARNING
            else -> CheckStatus.FAILED
        }
        return NetworkCheck(
            name = "DNS Resolution",
            status = status,
            value = dnsMs,
            unit = "ms",
            detail = when (status) {
                CheckStatus.PASSED -> "Fast DNS resolution"
                CheckStatus.WARNING -> "Slow DNS — try different DNS server"
                CheckStatus.FAILED -> "DNS resolution failing"
                CheckStatus.UNKNOWN -> "Unknown"
            },
            recommendation = if (status != CheckStatus.PASSED) {
                "Try Google DNS (8.8.8.8) or Cloudflare (1.1.1.1)"
            } else null
        )
    }

    private suspend fun checkPacketLoss(): NetworkCheck {
        // TODO: Send test packets
        val lossPercent = 0.0 // Placeholder
        val status = when {
            lossPercent == 0.0 -> CheckStatus.PASSED
            lossPercent <= 2.0 -> CheckStatus.WARNING
            else -> CheckStatus.FAILED
        }
        return NetworkCheck(
            name = "Packet Loss",
            status = status,
            value = lossPercent,
            unit = "%",
            detail = when (status) {
                CheckStatus.PASSED -> "No packet loss"
                CheckStatus.WARNING -> "Some packet loss — may cause buffering"
                CheckStatus.FAILED -> "Significant packet loss"
                CheckStatus.UNKNOWN -> "Unknown"
            }
        )
    }

    private suspend fun checkGateway(): NetworkCheck {
        // TODO: Ping gateway
        val reachable = true // Placeholder
        val latencyMs = 5.0
        return NetworkCheck(
            name = "Gateway",
            status = if (reachable) CheckStatus.PASSED else CheckStatus.FAILED,
            value = latencyMs,
            unit = "ms",
            detail = if (reachable) "Gateway reachable" else "Gateway unreachable"
        )
    }

    private suspend fun checkInternet(): NetworkCheck {
        // TODO: Test internet connectivity
        val connected = true // Placeholder
        val speedMbps = 25.0
        return NetworkCheck(
            name = "Internet",
            status = if (connected) CheckStatus.PASSED else CheckStatus.FAILED,
            value = speedMbps,
            unit = "Mbps",
            detail = if (connected) "Internet connection active" else "No internet connection"
        )
    }

    private suspend fun checkBand(): NetworkCheck {
        // TODO: Detect Wi-Fi band
        val band = "5 GHz" // Placeholder
        val status = when (band) {
            "6 GHz" -> CheckStatus.PASSED
            "5 GHz" -> CheckStatus.PASSED
            "2.4 GHz" -> CheckStatus.WARNING
            else -> CheckStatus.UNKNOWN
        }
        return NetworkCheck(
            name = "Wi-Fi Band",
            status = status,
            detail = "Connected on $band",
            recommendation = if (band == "2.4 GHz") {
                "Switch to 5 GHz for faster speeds"
            } else null
        )
    }

    // ─────────────────────────────────────────────────────────────
    // DIAGNOSIS
    // ─────────────────────────────────────────────────────────────

    private fun generateDiagnosis(checks: List<NetworkCheck>): NetworkDiagnosis {
        val failed = checks.filter { it.status == CheckStatus.FAILED }
        val warnings = checks.filter { it.status == CheckStatus.WARNING }
        val passed = checks.filter { it.status == CheckStatus.PASSED }

        val overallStatus = when {
            failed.isNotEmpty() -> DiagnosisStatus.ISSUES_FOUND
            warnings.isNotEmpty() -> DiagnosisStatus.SUBOPTIMAL
            else -> DiagnosisStatus.HEALTHY
        }

        val summary = when (overallStatus) {
            DiagnosisStatus.HEALTHY -> "Your internet connection is healthy."
            DiagnosisStatus.SUBOPTIMAL -> "Your connection works but could be improved."
            DiagnosisStatus.ISSUES_FOUND -> "Issues detected that may affect your connection."
        }

        val recommendations = checks.mapNotNull { it.recommendation }

        return NetworkDiagnosis(
            timestamp = Instant.now(),
            overallStatus = overallStatus,
            summary = summary,
            checks = checks,
            failedChecks = failed,
            warnings = warnings,
            passedChecks = passed,
            recommendations = recommendations,
            score = (passed.size.toDouble() / checks.size * 100).toInt()
        )
    }
}

// ─────────────────────────────────────────────────────────────
// DATA CLASSES
// ─────────────────────────────────────────────────────────────

data class NetworkState(
    val analyzing: Boolean = false,
    val lastDiagnosis: NetworkDiagnosis? = null,
    val checks: List<NetworkCheck> = emptyList()
)

data class NetworkDiagnosis(
    val timestamp: Instant,
    val overallStatus: DiagnosisStatus,
    val summary: String,
    val checks: List<NetworkCheck>,
    val failedChecks: List<NetworkCheck>,
    val warnings: List<NetworkCheck>,
    val passedChecks: List<NetworkCheck>,
    val recommendations: List<String>,
    val score: Int // 0-100
)

enum class DiagnosisStatus {
    HEALTHY,
    SUBOPTIMAL,
    ISSUES_FOUND
}

data class NetworkCheck(
    val name: String,
    val status: CheckStatus,
    val value: Double? = null,
    val unit: String? = null,
    val detail: String,
    val recommendation: String? = null
)

data class NetworkSnapshot(
    val timestamp: Instant,
    val checks: List<NetworkCheck>,
    val diagnosis: NetworkDiagnosis
)

data class CoveragePoint(
    val latitude: Double,
    val longitude: Double,
    val rssi: Int,
    val timestamp: Instant,
    val locationName: String? = null
)
