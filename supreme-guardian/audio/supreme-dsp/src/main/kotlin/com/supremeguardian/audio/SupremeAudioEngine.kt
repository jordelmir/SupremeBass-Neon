package com.supremeguardian.audio

import com.supremeguardian.core.building.ZoneId
import com.supremeguardian.core.shared.*
import kotlinx.coroutines.flow.Flow

/**
 * Supreme Audio Engine — extracted from SupremeBass DSP.
 *
 * Responsibilities:
 *   1. Generate acoustic signals for suppression research
 *   2. Measure acoustic properties of spaces
 *   3. Control speaker arrays for zone-targeted audio
 *   4. Monitor audio output health
 *
 * This module is the "Supreme Audio Engine" subsystem within Supreme Guardian.
 */
interface SupremeAudioEngine {

    /**
     * Generate a signal for acoustic suppression research.
     * The signal is played through the specified speaker array.
     */
    suspend fun generateSignal(config: AudioSignalConfig): AudioSignalResult

    /**
     * Measure acoustic properties of a zone.
     * Returns RT60, frequency response, THD, etc.
     */
    suspend fun measureAcoustics(zoneId: ZoneId): AcousticMeasurement

    /**
     * Control a speaker array for zone-targeted audio.
     */
    suspend fun controlArray(command: ArrayCommand): ArrayCommandResult

    /**
     * Get current audio engine status.
     */
    fun getStatus(): AudioEngineStatus

    /**
     * Stream audio engine telemetry.
     */
    fun telemetry(): Flow<AudioTelemetry>
}

/**
 * Audio signal configuration.
 */
data class AudioSignalConfig(
    val zoneId: ZoneId,
    val frequencyHz: Double,
    val amplitude: Float,
    val waveform: AudioWaveform,
    val durationMs: Long,
    val targetNodes: List<NodeId> // Speaker nodes to activate
)

enum class AudioWaveform {
    SINE,
    SQUARE,
    TRIANGLE,
    SAWTOOTH,
    NOISE,
    CHIRP,
    SWEEP
}

/**
 * Audio signal result.
 */
data class AudioSignalResult(
    val success: Boolean,
    val actualDurationMs: Long,
    val peakAmplitude: Float,
    val rmsAmplitude: Float,
    val clippingDetected: Boolean
)

/**
 * Acoustic measurement result.
 */
data class AcousticMeasurement(
    val zoneId: ZoneId,
    val timestamp: GuardianTimestamp,
    val rt60Ms: Double?, // Reverberation time
    val rt30Ms: Double?,
    val frequencyResponse: List<FrequencyResponsePoint>,
    val thdPercent: Double?, // Total harmonic distortion
    val splDbFS: Double?, // Relative SPL (dBFS, not absolute SPL)
    val backgroundNoiseDbFS: Double?,
    val confidence: Double
)

data class FrequencyResponsePoint(
    val frequencyHz: Double,
    val magnitudeDb: Double,
    val phaseDegrees: Double
)

/**
 * Speaker array command.
 */
data class ArrayCommand(
    val zoneId: ZoneId,
    val action: ArrayAction,
    val nodes: List<NodeId>,
    val signal: AudioSignalConfig? = null
)

enum class ArrayAction {
    ACTIVATE,
    DEACTIVATE,
    SET_VOLUME,
    SET_EQ,
    CALIBRATE,
    HEALTH_CHECK
}

data class ArrayCommandResult(
    val success: Boolean,
    val activatedNodes: List<NodeId>,
    val failedNodes: List<NodeId>,
    val message: String
)

/**
 * Audio engine status.
 */
data class AudioEngineStatus(
    val isRunning: Boolean,
    val activeZones: List<ZoneId>,
    val activeNodes: List<NodeId>,
    val sampleRate: Int,
    val dspChainActive: Boolean,
    val lastMeasurementTime: GuardianTimestamp?
)

/**
 * Audio telemetry.
 */
data class AudioTelemetry(
    val timestamp: GuardianTimestamp,
    val activeNodes: Int,
    val totalNodes: Int,
    val cpuUsagePercent: Double,
    val bufferUtilization: Double,
    val clippingEvents: Int,
    val underrunEvents: Int
)
