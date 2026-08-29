package com.supremecorp.bass.domain.model

import java.util.UUID

enum class ExperimentType {
    FREQUENCY_RESPONSE,
    DISTORTION_PROFILE,
    IMPEDANCE_ESTIMATION,
    PHASE_RESPONSE,
    COMB_FILTER_SCAN,
    FLAME_PROPAGATION,
    CUSTOM
}

enum class ExperimentStatus {
    CONFIGURED,
    RUNNING,
    PAUSED,
    COMPLETED,
    ABORTED,
    FAILED
}

data class ExperimentVariable(
    val name: String,
    val unit: String,
    val min: Double,
    val max: Double,
    val step: Double,
    val current: Double
) {
    init {
        require(min <= max) { "min must be <= max" }
        require(step > 0) { "step must be > 0" }
        require(current in min..max) { "current must be in [min, max]" }
    }
}

data class ExperimentObservation(
    val frequencyHz: Double,
    val variable: String,
    val requestedValue: Double,
    val measuredPeak: Double,
    val measuredRms: Double,
    val phaseDegrees: Double? = null,
    val timestampMs: Long = System.currentTimeMillis(),
    val authority: MeasurementAuthority = MeasurementAuthority.DIGITAL
)

data class ExperimentResult(
    val summary: String,
    val peakGainDb: Double,
    val rmsGainDb: Double,
    val thdPercent: Double? = null,
    val observations: List<ExperimentObservation>,
    val durationMs: Long
)

data class AcousticExperiment(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: ExperimentType,
    val protocolVersion: Int = 1,
    val deviceProfile: DeviceAcousticProfile? = null,
    val variables: List<ExperimentVariable>,
    val signalConfig: SignalConfig,
    val stepCount: Int = 30,
    val dwellMs: Int = 300,
    val repeatsPerStep: Int = 3,
    val status: ExperimentStatus = ExperimentStatus.CONFIGURED,
    val currentStep: Int = 0,
    val observations: List<ExperimentObservation> = emptyList(),
    val result: ExperimentResult? = null,
    val startedAtMs: Long? = null,
    val completedAtMs: Long? = null,
    val errorMessage: String? = null
) {
    val progress: Float
        get() = if (stepCount > 0) currentStep.toFloat() / stepCount else 0f

    val isTerminal: Boolean
        get() = status in listOf(ExperimentStatus.COMPLETED, ExperimentStatus.ABORTED, ExperimentStatus.FAILED)

    fun withStarted(): AcousticExperiment = copy(
        status = ExperimentStatus.RUNNING,
        currentStep = 0,
        startedAtMs = System.currentTimeMillis(),
        errorMessage = null
    )

    fun withStep(step: Int, obs: List<ExperimentObservation>): AcousticExperiment = copy(
        currentStep = step,
        observations = observations + obs
    )

    fun withCompleted(result: ExperimentResult): AcousticExperiment = copy(
        status = ExperimentStatus.COMPLETED,
        result = result,
        completedAtMs = System.currentTimeMillis()
    )

    fun withAborted(reason: String): AcousticExperiment = copy(
        status = ExperimentStatus.ABORTED,
        errorMessage = reason,
        completedAtMs = System.currentTimeMillis()
    )

    fun withFailed(error: String): AcousticExperiment = copy(
        status = ExperimentStatus.FAILED,
        errorMessage = error,
        completedAtMs = System.currentTimeMillis()
    )
}
