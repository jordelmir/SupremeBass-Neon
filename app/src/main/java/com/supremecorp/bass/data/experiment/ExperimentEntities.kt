package com.supremecorp.bass.data.experiment

import androidx.room.*
import com.supremecorp.bass.domain.model.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "experiments")
data class ExperimentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val protocolVersion: Int,
    val deviceProfileId: String?,
    val stepCount: Int,
    val dwellMs: Int,
    val repeatsPerStep: Int,
    val status: String,
    val currentStep: Int,
    val startedAtMs: Long?,
    val completedAtMs: Long?,
    val errorMessage: String?,
    val signalConfigJson: String,
    val variablesJson: String
)

@Entity(
    tableName = "experiment_observations",
    foreignKeys = [ForeignKey(
        entity = ExperimentEntity::class,
        parentColumns = ["id"],
        childColumns = ["experimentId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("experimentId")]
)
data class ObservationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val experimentId: String,
    val frequencyHz: Double,
    val variable: String,
    val requestedValue: Double,
    val measuredPeak: Double,
    val measuredRms: Double,
    val phaseDegrees: Double?,
    val timestampMs: Long,
    val authority: String
)

@Entity(tableName = "experiment_results")
data class ExperimentResultEntity(
    @PrimaryKey val experimentId: String,
    val summary: String,
    val peakGainDb: Double,
    val rmsGainDb: Double,
    val thdPercent: Double?,
    val durationMs: Long
)

@Dao
interface ExperimentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExperiment(experiment: ExperimentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObservations(observations: List<ObservationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: ExperimentResultEntity)

    @Query("SELECT * FROM experiments ORDER BY startedAtMs DESC")
    fun getAllExperiments(): Flow<List<ExperimentEntity>>

    @Query("SELECT * FROM experiments WHERE id = :id")
    suspend fun getExperimentById(id: String): ExperimentEntity?

    @Query("SELECT * FROM experiment_observations WHERE experimentId = :experimentId ORDER BY frequencyHz")
    suspend fun getObservations(experimentId: String): List<ObservationEntity>

    @Query("SELECT * FROM experiment_results WHERE experimentId = :experimentId")
    suspend fun getResult(experimentId: String): ExperimentResultEntity?

    @Query("SELECT * FROM experiments WHERE status = :status ORDER BY startedAtMs DESC")
    fun getExperimentsByStatus(status: String): Flow<List<ExperimentEntity>>

    @Delete
    suspend fun deleteExperiment(experiment: ExperimentEntity)

    @Query("DELETE FROM experiments WHERE id = :id")
    suspend fun deleteExperimentById(id: String)
}
