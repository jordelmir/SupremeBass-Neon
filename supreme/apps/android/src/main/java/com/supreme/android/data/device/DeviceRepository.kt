package com.supreme.android.data.device

import com.supreme.android.domain.model.AcousticMetricType
import com.supreme.android.domain.model.DeviceAcousticProfile
import com.supreme.android.domain.model.DeviceInfo
import com.supreme.android.domain.model.FrequencyResponsePoint
import com.supreme.android.domain.model.MeasurementAuthority
import com.supreme.android.domain.model.OutputRoute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class DeviceRepository(
    private val profileDao: DeviceAcousticProfileDao,
    private val pointDao: FrequencyResponsePointDao,
    private val infoDao: DeviceInfoDao
) {
    suspend fun saveProfile(profile: DeviceAcousticProfile) {
        val entity = DeviceAcousticProfileEntity(
            id = profile.id ?: java.util.UUID.randomUUID().toString(),
            manufacturer = profile.manufacturer,
            model = profile.model,
            androidDevice = profile.androidDevice,
            outputRoute = profile.outputRoute.name,
            supportedSampleRates = profile.supportedSampleRates.toString(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        profileDao.insert(entity)

        val points = profile.measuredResponses.map { resp ->
            FrequencyResponsePointEntity(
                profileId = entity.id,
                frequencyHz = resp.frequencyHz,
                requestedAmplitude = resp.requestedAmplitude,
                measuredMetric = resp.measuredMetric,
                metricType = resp.metricType.name,
                authority = resp.authority.name
            )
        }
        pointDao.insertAll(points)
    }

    fun getAllProfiles(): Flow<List<DeviceAcousticProfile>> {
        return profileDao.getAllProfiles().map { entities ->
            entities.map { entity ->
                DeviceAcousticProfile(
                    id = entity.id,
                    manufacturer = entity.manufacturer,
                    model = entity.model,
                    androidDevice = entity.androidDevice,
                    outputRoute = OutputRoute.valueOf(entity.outputRoute),
                    supportedSampleRates = emptySet(),
                    measuredResponses = emptyList(),
                    authority = MeasurementAuthority.DIGITAL
                )
            }
        }
    }

    suspend fun getProfileWithPoints(profileId: String): DeviceAcousticProfile? {
        val entity = profileDao.getProfileById(profileId).firstOrNull()
        if (entity == null) return null
        val points = pointDao.getPointsForProfile(entity.id).firstOrNull() ?: emptyList()
        return DeviceAcousticProfile(
            id = entity.id,
            manufacturer = entity.manufacturer,
            model = entity.model,
            androidDevice = entity.androidDevice,
            outputRoute = OutputRoute.valueOf(entity.outputRoute),
            supportedSampleRates = emptySet(),
            measuredResponses = points.map { point ->
                FrequencyResponsePoint(
                    frequencyHz = point.frequencyHz,
                    requestedAmplitude = point.requestedAmplitude,
                    measuredMetric = point.measuredMetric,
                    metricType = AcousticMetricType.valueOf(point.metricType),
                    authority = MeasurementAuthority.valueOf(point.authority)
                )
            },
            authority = MeasurementAuthority.DIGITAL
        )
    }

    suspend fun getAllProfilesWithPoints(): List<DeviceAcousticProfile> {
        val entities = profileDao.getAllProfiles().firstOrNull() ?: emptyList()
        return entities.map { entity ->
            val points = pointDao.getPointsForProfile(entity.id).firstOrNull() ?: emptyList()
            DeviceAcousticProfile(
                id = entity.id,
                manufacturer = entity.manufacturer,
                model = entity.model,
                androidDevice = entity.androidDevice,
                outputRoute = OutputRoute.valueOf(entity.outputRoute),
                supportedSampleRates = emptySet(),
                measuredResponses = points.map { point ->
                    FrequencyResponsePoint(
                        frequencyHz = point.frequencyHz,
                        requestedAmplitude = point.requestedAmplitude,
                        measuredMetric = point.measuredMetric,
                        metricType = AcousticMetricType.valueOf(point.metricType),
                        authority = MeasurementAuthority.valueOf(point.authority)
                    )
                },
                authority = MeasurementAuthority.DIGITAL
            )
        }
    }

    suspend fun deleteProfile(id: String) {
        pointDao.deletePointsForProfile(id)
        profileDao.deleteProfile(id)
    }

    suspend fun saveDeviceInfo(info: DeviceInfo) {
        val entity = DeviceInfoEntity(
            manufacturer = info.manufacturer,
            model = info.model,
            device = info.device,
            androidVersion = info.androidVersion,
            supportedSampleRates = info.supportedSampleRates.toString()
        )
        infoDao.insert(entity)
    }

    fun getCurrentDeviceInfo(): Flow<DeviceInfo?> {
        return infoDao.getCurrentDevice().map { entity ->
            entity?.let { e ->
                DeviceInfo(
                    manufacturer = e.manufacturer,
                    model = e.model,
                    device = e.device,
                    androidVersion = e.androidVersion,
                    supportedSampleRates = emptySet()
                )
            }
        }
    }
}