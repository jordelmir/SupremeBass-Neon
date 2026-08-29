package com.supremecorp.bass.data.device

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "device_acoustic_profiles")
data class DeviceAcousticProfileEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val manufacturer: String,
    val model: String,
    val androidDevice: String,
    val outputRoute: String, // enum name
    val supportedSampleRates: String, // JSON
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "frequency_response_points")
data class FrequencyResponsePointEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val profileId: String,
    val frequencyHz: Double,
    val requestedAmplitude: Float,
    val measuredMetric: Double?,
    val metricType: String, // enum name
    val authority: String, // enum name
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "device_info")
data class DeviceInfoEntity(
    @PrimaryKey val id: String = "current_device",
    val manufacturer: String,
    val model: String,
    val device: String,
    val androidVersion: Int,
    val supportedSampleRates: String, // JSON
    val updatedAt: Long = System.currentTimeMillis()
)