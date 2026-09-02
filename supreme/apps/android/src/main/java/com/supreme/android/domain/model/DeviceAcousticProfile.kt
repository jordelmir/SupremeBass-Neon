package com.supreme.android.domain.model

data class DeviceAcousticProfile(
    val id: String = java.util.UUID.randomUUID().toString(),
    val manufacturer: String,
    val model: String,
    val androidDevice: String,
    val outputRoute: OutputRoute,
    val supportedSampleRates: Set<Int>,
    val measuredResponses: List<FrequencyResponsePoint>,
    val authority: MeasurementAuthority
)

data class FrequencyResponsePoint(
    val frequencyHz: Double,
    val requestedAmplitude: Float,
    val measuredMetric: Double?,
    val metricType: AcousticMetricType,
    val authority: MeasurementAuthority
)

enum class AcousticMetricType {
    SPL,
    VOLTAGE,
    IMPEDANCE,
    THD,
    FREQUENCY_RESPONSE
}

data class DeviceInfo(
    val manufacturer: String = android.os.Build.MANUFACTURER,
    val model: String = android.os.Build.MODEL,
    val device: String = android.os.Build.DEVICE,
    val androidVersion: Int = android.os.Build.VERSION.SDK_INT,
    val supportedSampleRates: Set<Int> = emptySet()
)
