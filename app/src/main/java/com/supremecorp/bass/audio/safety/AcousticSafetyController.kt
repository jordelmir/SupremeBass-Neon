package com.supremecorp.bass.audio.safety

import com.supremecorp.bass.domain.model.OutputRoute
import com.supremecorp.bass.domain.model.SignalConfig
import com.supremecorp.bass.core.logging.AppLogger

class AcousticSafetyController {

    private var currentRoute: OutputRoute = OutputRoute.UNKNOWN
    private var maxAmplitude: Float = 0.8f
    private var rampUpMs: Long = 50
    private var rampDownMs: Long = 50
    private var maxContinuousDurationMs: Long = 30_000
    private var cooldownMs: Long = 5_000
    private var sessionStartTime: Long = 0
    private var lastStopTime: Long = 0
    var routeInterlockEnabled: Boolean = true

    fun setRoute(route: OutputRoute) {
        currentRoute = route
    }

    fun configure(
        maxAmplitude: Float = 0.8f,
        rampUpMs: Long = 50,
        rampDownMs: Long = 50,
        maxContinuousDurationMs: Long = 30_000,
        cooldownMs: Long = 5_000
    ) {
        this.maxAmplitude = maxAmplitude.coerceIn(0.1f, 1.0f)
        this.rampUpMs = rampUpMs
        this.rampDownMs = rampDownMs
        this.maxContinuousDurationMs = maxContinuousDurationMs
        this.cooldownMs = cooldownMs
    }

    fun validateConfig(config: SignalConfig): SafetyCheckResult {
        if (routeInterlockEnabled &&
            (currentRoute == OutputRoute.WIRED_HEADPHONES ||
            currentRoute == OutputRoute.BLUETOOTH)) {
            return SafetyCheckResult.Blocked("Headphone/Bluetooth route blocked for high-output mode")
        }

        if (config.amplitude > maxAmplitude) {
            return SafetyCheckResult.Warning(
                "Amplitude ${config.amplitude} exceeds limit $maxAmplitude, clamping"
            )
        }

        val now = System.currentTimeMillis()
        if (now - lastStopTime < cooldownMs) {
            return SafetyCheckResult.Blocked("Cooldown active, wait ${cooldownMs - (now - lastStopTime)}ms")
        }

        return SafetyCheckResult.Pass
    }

    fun onSessionStart() {
        sessionStartTime = System.currentTimeMillis()
        AppLogger.i("Safety", "Session started, route=$currentRoute")
    }

    fun onSessionStop() {
        lastStopTime = System.currentTimeMillis()
        AppLogger.i("Safety", "Session stopped")
    }

    fun shouldStop(): SafetyStopReason? {
        val elapsed = System.currentTimeMillis() - sessionStartTime
        if (elapsed >= maxContinuousDurationMs) {
            return SafetyStopReason.DurationLimit
        }
        return null
    }

    fun clampAmplitude(amplitude: Float): Float {
        return amplitude.coerceIn(0.0f, maxAmplitude)
    }

    fun getRampUpSamples(sampleRate: Int): Int {
        return (rampUpMs * sampleRate / 1000.0).toInt()
    }

    fun getRampDownSamples(sampleRate: Int): Int {
        return (rampDownMs * sampleRate / 1000.0).toInt()
    }
}

sealed interface SafetyCheckResult {
    data object Pass : SafetyCheckResult
    data class Warning(val message: String) : SafetyCheckResult
    data class Blocked(val reason: String) : SafetyCheckResult
}

enum class SafetyStopReason {
    DurationLimit,
    RouteChange,
    ThermalLimit,
    BackendError
}
