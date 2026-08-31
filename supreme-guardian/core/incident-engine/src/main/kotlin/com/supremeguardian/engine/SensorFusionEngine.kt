package com.supremeguardian.engine

import com.supremeguardian.core.building.ZoneId
import com.supremeguardian.core.sensor.*
import com.supremeguardian.core.shared.*
import java.time.Instant

/**
 * Sensor Fusion Engine — combines multiple sensor inputs into a unified assessment.
 *
 * This is a DETERMINISTIC engine — no ML inference.
 * Rules are explicit, auditable, and based on domain knowledge.
 *
 * Fusion strategy:
 *   1. Collect observations within a time window
 *   2. Group by zone
 *   3. Apply deterministic rules
 *   4. Output FusionResult with fire confidence
 */
class DeterministicSensorFusion : SensorFusionEngine {

    companion object {
        // Time window for grouping observations (ms)
        const val FUSION_WINDOW_MS = 5_000L

        // Minimum observations for multi-sensor confirmation
        const val MIN_OBSERVATIONS_FOR_CONFIRMATION = 2
    }

    override fun fuse(observations: List<SensorObservation>): FusionResult {
        if (observations.isEmpty()) {
            return FusionResult(
                zoneId = ZoneId("unknown"),
                timestamp = GuardianTimestamp(),
                contributingSensors = emptyList(),
                fusedReading = emptyMap(),
                fireConfidence = 0.0,
                anomalyScore = 0.0,
                recommendedState = "NORMAL",
                reasoning = listOf("No observations available")
            )
        }

        // Group observations by zone
        val byZone = observations.groupBy { it.zoneId }

        // Process each zone independently
        val results = byZone.map { (zoneId, zoneObservations) ->
            fuseZone(zoneId, zoneObservations)
        }

        // Return the highest-risk zone result
        return results.maxByOrNull { it.fireConfidence } ?: results.first()
    }

    private fun fuseZone(zoneId: ZoneId, observations: List<SensorObservation>): FusionResult {
        val reasoning = mutableListOf<String>()
        var fireScore = 0.0
        var anomalyScore = 0.0

        // Analyze each sensor type
        val thermalObs = observations.filter { it.sensorType == SensorType.THERMAL_CAMERA }
        val smokeObs = observations.filter { it.sensorType == SensorType.SMOKE_DETECTOR }
        val heatObs = observations.filter { it.sensorType == SensorType.HEAT_DETECTOR }
        val flameObs = observations.filter { it.sensorType == SensorType.FLAME_DETECTOR }
        val coObs = observations.filter { it.sensorType == SensorType.CO_DETECTOR }
        val electricalObs = observations.filter { it.sensorType == SensorType.ELECTRICAL_SENSOR }

        // Thermal analysis
        if (thermalObs.isNotEmpty()) {
            val maxTemp = thermalObs.flatMap { it.readings["max_celsius"] ?: listOf(it.readings.values.maxOrNull() ?: 0.0) }
                .maxOrNull() ?: 0.0

            when {
                maxTemp >= 150.0 -> {
                    fireScore += 0.4
                    reasoning.add("Thermal: HIGH temperature ${maxTemp}°C")
                }
                maxTemp >= 100.0 -> {
                    fireScore += 0.25
                    reasoning.add("Thermal: Elevated temperature ${maxTemp}°C")
                }
                maxTemp >= 60.0 -> {
                    anomalyScore += 0.2
                    reasoning.add("Thermal: Above normal ${maxTemp}°C")
                }
                else -> {
                    reasoning.add("Thermal: Normal ${maxTemp}°C")
                }
            }

            // Rate of rise analysis
            val rateOfRise = thermalObs.mapNotNull { it.readings["rate_of_rise"] }.maxOrNull()
            if (rateOfRise != null) {
                when {
                    rateOfRise >= 20.0 -> {
                        fireScore += 0.3
                        reasoning.add("Thermal: RAPID rise ${rateOfRise}°C/min")
                    }
                    rateOfRise >= 10.0 -> {
                        fireScore += 0.15
                        reasoning.add("Thermal: Rapid rise ${rateOfRise}°C/min")
                    }
                    rateOfRise >= 5.0 -> {
                        anomalyScore += 0.1
                        reasoning.add("Thermal: Moderate rise ${rateOfRise}°C/min")
                    }
                }
            }
        }

        // Smoke analysis
        if (smokeObs.isNotEmpty()) {
            val maxSmoke = smokeObs.mapNotNull { it.readings["smoke_level"] }.maxOrNull() ?: 0.0
            when {
                maxSmoke >= 0.7 -> {
                    fireScore += 0.3
                    reasoning.add("Smoke: HIGH level $maxSmoke")
                }
                maxSmoke >= 0.3 -> {
                    anomalyScore += 0.2
                    reasoning.add("Smoke: Detected level $maxSmoke")
                }
                else -> {
                    reasoning.add("Smoke: Normal")
                }
            }
        }

        // Heat detector analysis
        if (heatObs.isNotEmpty()) {
            val maxHeat = heatObs.mapNotNull { it.readings["temperature"] }.maxOrNull() ?: 0.0
            if (maxHeat >= 70.0) {
                fireScore += 0.25
                reasoning.add("Heat: Triggered at ${maxHeat}°C")
            }
        }

        // Flame detector analysis
        if (flameObs.isNotEmpty()) {
            val maxFlameConf = flameObs.mapNotNull { it.readings["flame_confidence"] }.maxOrNull() ?: 0.0
            if (maxFlameConf >= 0.5) {
                fireScore += 0.4
                reasoning.add("Flame: Detected with confidence $maxFlameConf")
            } else if (maxFlameConf >= 0.2) {
                anomalyScore += 0.15
                reasoning.add("Flame: Possible detection confidence $maxFlameConf")
            }
        }

        // CO analysis
        if (coObs.isNotEmpty()) {
            val maxCO = coObs.mapNotNull { it.readings["co_level"] }.maxOrNull() ?: 0.0
            if (maxCO >= 0.5) {
                fireScore += 0.2
                reasoning.add("CO: Elevated level $maxCO")
            }
        }

        // Electrical anomaly analysis
        if (electricalObs.isNotEmpty()) {
            val maxVoltageAnomaly = electricalObs.mapNotNull { it.readings["voltage_anomaly"] }.maxOrNull() ?: 0.0
            val maxCurrentAnomaly = electricalObs.mapNotNull { it.readings["current_anomaly"] }.maxOrNull() ?: 0.0

            if (maxVoltageAnomaly > 0.5 || maxCurrentAnomaly > 0.5) {
                anomalyScore += 0.2
                reasoning.add("Electrical: Anomaly detected (V=$maxVoltageAnomaly, I=$maxCurrentAnomaly)")
            }
        }

        // Multi-sensor bonus
        val uniqueSensors = observations.map { it.sensorId }.distinct().size
        if (uniqueSensors >= MIN_OBSERVATIONS_FOR_CONFIRMATION) {
            val bonus = (uniqueSensors - 1) * 0.05
            fireScore += bonus
            reasoning.add("Multi-sensor: $uniqueSensors independent sensors triggered (+${String.format("%.2f", bonus)} bonus)")
        }

        // Normalize scores
        fireScore = fireScore.coerceIn(0.0, 1.0)
        anomalyScore = anomalyScore.coerceIn(0.0, 1.0)

        // Determine recommended state
        val recommendedState = when {
            fireScore >= 0.7 -> "CONFIRMED_INCIDENT"
            fireScore >= 0.4 -> "SUSPECT"
            fireScore >= 0.2 -> "WATCH"
            anomalyScore >= 0.2 -> "WATCH"
            else -> "NORMAL"
        }

        return FusionResult(
            zoneId = zoneId,
            timestamp = GuardianTimestamp(),
            contributingSensors = observations.map { it.sensorId }.distinct(),
            fusedReading = observations.flatMap { it.readings.entries }.associate { it.key to it.value },
            fireConfidence = fireScore,
            anomalyScore = anomalyScore,
            recommendedState = recommendedState,
            reasoning = reasoning
        )
    }

    override fun getFireConfidence(zoneId: ZoneId, observations: List<SensorObservation>): Double {
        val zoneObs = observations.filter { it.zoneId == zoneId }
        val result = fuseZone(zoneId, zoneObs)
        return result.fireConfidence
    }

    override fun isMultiSensorConfirmed(zoneId: ZoneId, observations: List<SensorObservation>): Boolean {
        val zoneObs = observations.filter { it.zoneId == zoneId }
        val uniqueSensors = zoneObs.map { it.sensorId }.distinct().size
        return uniqueSensors >= MIN_OBSERVATIONS_FOR_CONFIRMATION
    }
}
