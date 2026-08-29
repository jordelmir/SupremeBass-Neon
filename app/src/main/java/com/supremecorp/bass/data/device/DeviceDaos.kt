package com.supremecorp.bass.data.device

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceAcousticProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: DeviceAcousticProfileEntity)

    @Update
    suspend fun update(profile: DeviceAcousticProfileEntity)

    @Query("SELECT * FROM device_acoustic_profiles ORDER BY updatedAt DESC")
    fun getAllProfiles(): Flow<List<DeviceAcousticProfileEntity>>

    @Query("SELECT * FROM device_acoustic_profiles WHERE id = :id")
    fun getProfileById(id: String): Flow<DeviceAcousticProfileEntity?>

    @Query("SELECT * FROM frequency_response_points WHERE profileId = :profileId ORDER BY frequencyHz ASC")
    fun getPointsForProfile(profileId: String): Flow<List<FrequencyResponsePointEntity>>

    @Query("DELETE FROM device_acoustic_profiles WHERE id = :id")
    suspend fun deleteProfile(id: String)

    @Query("DELETE FROM device_acoustic_profiles")
    suspend fun deleteAllProfiles()
}

@Dao
interface FrequencyResponsePointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(points: List<FrequencyResponsePointEntity>)

    @Query("SELECT * FROM frequency_response_points WHERE profileId = :profileId ORDER BY frequencyHz ASC")
    fun getPointsForProfile(profileId: String): Flow<List<FrequencyResponsePointEntity>>

    @Query("DELETE FROM frequency_response_points WHERE profileId = :profileId")
    suspend fun deletePointsForProfile(profileId: String)

    @Query("DELETE FROM frequency_response_points")
    suspend fun deleteAllPoints()
}

@Dao
interface DeviceInfoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(info: DeviceInfoEntity)

    @Query("SELECT * FROM device_info WHERE id = 'current_device'")
    fun getCurrentDevice(): Flow<DeviceInfoEntity?>
}