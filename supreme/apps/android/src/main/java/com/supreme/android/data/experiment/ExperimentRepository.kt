package com.supreme.android.data.experiment

import com.supreme.android.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class ExperimentRepository(private val dao: ExperimentDao) {

    fun getAllExperiments(): Flow<List<AcousticExperiment>> {
        return dao.getAllExperiments().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getExperimentsByStatus(status: ExperimentStatus): Flow<List<AcousticExperiment>> {
        return dao.getExperimentsByStatus(status.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getExperiment(id: String): AcousticExperiment? {
        val entity = dao.getExperimentById(id) ?: return null
        val observations = dao.getObservations(id).map { it.toDomain() }
        val result = dao.getResult(id)?.toDomain()
        return entity.toDomain().copy(
            observations = observations,
            result = result
        )
    }

    suspend fun saveExperiment(experiment: AcousticExperiment) {
        dao.insertExperiment(experiment.toEntity())

        if (experiment.observations.isNotEmpty()) {
            dao.insertObservations(experiment.observations.map { it.toEntity(experiment.id) })
        }

        experiment.result?.let {
            dao.insertResult(it.toEntity(experiment.id))
        }
    }

    suspend fun deleteExperiment(id: String) {
        dao.deleteExperimentById(id)
    }
}

private fun ExperimentEntity.toDomain(): AcousticExperiment {
    return AcousticExperiment(
        id = id,
        name = name,
        type = try { ExperimentType.valueOf(type) } catch (_: Exception) { ExperimentType.CUSTOM },
        protocolVersion = protocolVersion,
        stepCount = stepCount,
        dwellMs = dwellMs,
        repeatsPerStep = repeatsPerStep,
        status = try { ExperimentStatus.valueOf(status) } catch (_: Exception) { ExperimentStatus.CONFIGURED },
        currentStep = currentStep,
        startedAtMs = startedAtMs,
        completedAtMs = completedAtMs,
        errorMessage = errorMessage,
        signalConfig = parseSignalConfig(signalConfigJson),
        variables = parseVariables(variablesJson)
    )
}

private fun AcousticExperiment.toEntity(): ExperimentEntity {
    return ExperimentEntity(
        id = id,
        name = name,
        type = type.name,
        protocolVersion = protocolVersion,
        deviceProfileId = deviceProfile?.id,
        stepCount = stepCount,
        dwellMs = dwellMs,
        repeatsPerStep = repeatsPerStep,
        status = status.name,
        currentStep = currentStep,
        startedAtMs = startedAtMs,
        completedAtMs = completedAtMs,
        errorMessage = errorMessage,
        signalConfigJson = serializeSignalConfig(signalConfig),
        variablesJson = serializeVariables(variables)
    )
}

private fun ObservationEntity.toDomain(): ExperimentObservation {
    return ExperimentObservation(
        frequencyHz = frequencyHz,
        variable = variable,
        requestedValue = requestedValue,
        measuredPeak = measuredPeak,
        measuredRms = measuredRms,
        phaseDegrees = phaseDegrees,
        timestampMs = timestampMs,
        authority = try { MeasurementAuthority.valueOf(authority) } catch (_: Exception) { MeasurementAuthority.DIGITAL }
    )
}

private fun ExperimentObservation.toEntity(experimentId: String): ObservationEntity {
    return ObservationEntity(
        experimentId = experimentId,
        frequencyHz = frequencyHz,
        variable = variable,
        requestedValue = requestedValue,
        measuredPeak = measuredPeak,
        measuredRms = measuredRms,
        phaseDegrees = phaseDegrees,
        timestampMs = timestampMs,
        authority = authority.name
    )
}

private fun ExperimentResultEntity.toDomain(): ExperimentResult {
    return ExperimentResult(
        summary = summary,
        peakGainDb = peakGainDb,
        rmsGainDb = rmsGainDb,
        thdPercent = thdPercent,
        observations = emptyList(),
        durationMs = durationMs
    )
}

private fun ExperimentResult.toEntity(experimentId: String): ExperimentResultEntity {
    return ExperimentResultEntity(
        experimentId = experimentId,
        summary = summary,
        peakGainDb = peakGainDb,
        rmsGainDb = rmsGainDb,
        thdPercent = thdPercent,
        durationMs = durationMs
    )
}

private fun serializeSignalConfig(config: SignalConfig): String {
    val json = JSONObject()
    json.put("frequencyHz", config.frequencyHz)
    json.put("amplitude", config.amplitude.toDouble())
    json.put("waveform", config.waveform.name)
    json.put("durationMs", config.durationMs)
    json.put("phaseRadians", config.phaseRadians)
    return json.toString()
}

private fun parseSignalConfig(json: String): SignalConfig {
    return try {
        val obj = JSONObject(json)
        SignalConfig(
            frequencyHz = obj.getDouble("frequencyHz"),
            amplitude = obj.getDouble("amplitude").toFloat(),
            waveform = try { Waveform.valueOf(obj.getString("waveform")) } catch (_: Exception) { Waveform.SINE },
            durationMs = obj.optLong("durationMs", 0),
            phaseRadians = obj.optDouble("phaseRadians", 0.0)
        )
    } catch (_: Exception) {
        SignalConfig(frequencyHz = 1000.0, amplitude = 0.5f, waveform = Waveform.SINE)
    }
}

private fun serializeVariables(variables: List<ExperimentVariable>): String {
    val arr = JSONArray()
    variables.forEach { v ->
        val json = JSONObject()
        json.put("name", v.name)
        json.put("type", v.type.name)
        json.put("unit", v.unit)
        json.put("min", v.min)
        json.put("max", v.max)
        json.put("step", v.step)
        json.put("current", v.current)
        arr.put(json)
    }
    return arr.toString()
}

private fun parseVariables(json: String): List<ExperimentVariable> {
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            val typeName = if (obj.has("type")) obj.getString("type") else "FREQUENCY"
            val type = try { VariableType.valueOf(typeName) } catch (_: Exception) { VariableType.FREQUENCY }
            ExperimentVariable(
                name = obj.getString("name"),
                type = type,
                unit = obj.getString("unit"),
                min = obj.getDouble("min"),
                max = obj.getDouble("max"),
                step = obj.getDouble("step"),
                current = obj.getDouble("current")
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}
