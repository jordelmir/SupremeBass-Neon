package com.supremecorp.bass.experiment

import android.media.AudioManager
import android.util.Log
import com.supremecorp.bass.domain.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FlameSafetyController(
    private val audioManager: AudioManager,
    private val routeInterlockEnabled: Boolean = true
) {
    private val _safetyState = MutableStateFlow(FlameSafetyState())
    val safetyState: StateFlow<FlameSafetyState> = _safetyState.asStateFlow()

    companion object {
        private const val TAG = "SupremeBass_FlameSafety"
        const val MAX_DURATION_SECONDS = 300
        const val MAX_AMPLITUDE = 1.0
        const val COOLDOWN_MS = 30_000L
        const val MIN_DISTANCE_METERS = 0.1
        const val MAX_DISTANCE_METERS = 5.0
        const val MIN_FREQUENCY_HZ = 20.0
        const val MAX_FREQUENCY_HZ = 20_000.0
    }

    fun checkSafety(config: FlameExperimentConfig): FlameSafetyState {
        val violations = mutableListOf<FlameSafetyViolation>()
        val state = _safetyState.value

        if (routeInterlockEnabled && isHeadphoneConnected()) {
            violations.add(FlameSafetyViolation.HEADPHONES_DETECTED)
            Log.w(TAG, "Safety violation: headphones detected")
        }

        if (config.durationSeconds > MAX_DURATION_SECONDS) {
            violations.add(FlameSafetyViolation.DURATION_EXCEEDED)
            Log.w(TAG, "Safety violation: duration ${config.durationSeconds}s > max $MAX_DURATION_SECONDS")
        }

        if (config.amplitude > MAX_AMPLITUDE) {
            violations.add(FlameSafetyViolation.AMPLITUDE_TOO_HIGH)
            Log.w(TAG, "Safety violation: amplitude ${config.amplitude} > max $MAX_AMPLITUDE")
        }

        if (config.frequencyHz < MIN_FREQUENCY_HZ || config.frequencyHz > MAX_FREQUENCY_HZ) {
            violations.add(FlameSafetyViolation.FREQUENCY_OUT_OF_RANGE)
            Log.w(TAG, "Safety violation: freq ${config.frequencyHz}Hz out of range [$MIN_FREQUENCY_HZ-$MAX_FREQUENCY_HZ]")
        }

        if (config.distanceMeters < MIN_DISTANCE_METERS) {
            violations.add(FlameSafetyViolation.DISTANCE_TOO_CLOSE)
            Log.w(TAG, "Safety violation: distance ${config.distanceMeters}m < min $MIN_DISTANCE_METERS")
        }

        if (config.distanceMeters > MAX_DISTANCE_METERS) {
            violations.add(FlameSafetyViolation.DISTANCE_TOO_FAR)
            Log.w(TAG, "Safety violation: distance ${config.distanceMeters}m > max $MAX_DISTANCE_METERS")
        }

        if (state.lastExperimentMs != null) {
            val elapsed = System.currentTimeMillis() - state.lastExperimentMs
            if (elapsed < COOLDOWN_MS) {
                violations.add(FlameSafetyViolation.COOLDOWN_ACTIVE)
                Log.w(TAG, "Safety violation: cooldown active, ${COOLDOWN_MS - elapsed}ms remaining")
            }
        }

        val isSafe = violations.isEmpty()
        if (isSafe) {
            Log.i(TAG, "Safety check PASSED for freq=${config.frequencyHz}Hz amp=${config.amplitude} dur=${config.durationSeconds}s dist=${config.distanceMeters}m")
        } else {
            Log.w(TAG, "Safety check FAILED: ${violations.size} violation(s)")
        }

        return FlameSafetyState(
            isSafe = isSafe,
            violations = violations,
            lastExperimentMs = state.lastExperimentMs,
            cooldownRemainingMs = if (violations.contains(FlameSafetyViolation.COOLDOWN_ACTIVE)) {
                COOLDOWN_MS - (System.currentTimeMillis() - (state.lastExperimentMs ?: 0))
            } else 0
        ).also {
            _safetyState.value = it
        }
    }

    fun markExperimentCompleted() {
        val current = _safetyState.value
        Log.i(TAG, "Experiment completed, starting cooldown (${COOLDOWN_MS}ms)")
        _safetyState.value = current.copy(
            lastExperimentMs = System.currentTimeMillis()
        )
    }

    suspend fun startCooldownMonitor(onTick: (Long) -> Unit) {
        Log.d(TAG, "Cooldown monitor started")
        while (_safetyState.value.cooldownRemainingMs > 0) {
            val remaining = _safetyState.value.cooldownRemainingMs
            onTick(remaining)
            delay(1000L)
            val updated = _safetyState.value
            if (updated.lastExperimentMs != null) {
                val elapsed = System.currentTimeMillis() - updated.lastExperimentMs
                val newRemaining = (COOLDOWN_MS - elapsed).coerceAtLeast(0)
                _safetyState.value = updated.copy(cooldownRemainingMs = newRemaining)
            }
        }
        Log.d(TAG, "Cooldown monitor ended")
    }

    fun isHeadphoneConnected(): Boolean {
        return try {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            val connected = devices.any {
                it.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                it.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                it.type == android.media.AudioDeviceInfo.TYPE_USB_HEADSET
            }
            Log.d(TAG, "Headphone check: $connected (devices=${devices.size})")
            connected
        } catch (e: Exception) {
            Log.e(TAG, "Headphone check failed: ${e.message}")
            false
        }
    }
}
