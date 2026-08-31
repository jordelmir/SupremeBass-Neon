package com.supremeguardian.engine

import com.supremeguardian.core.incident.*
import com.supremeguardian.core.sensor.SensorObservation
import com.supremeguardian.core.sensor.SensorType
import com.supremeguardian.core.shared.*

/**
 * Fire Detection Rules — deterministic rules for incident state transitions.
 *
 * These are NOT ML models. These are explicit, auditable rules.
 * Every rule has a clear condition and a clear outcome.
 *
 * Safety principle: when in doubt, escalate (don't suppress).
 */
class FireDetectionRules {

    companion object {
        // Temperature thresholds (Celsius)
        const val TEMP_WATCH = 50.0
        const val TEMP_ANOMALY = 70.0
        const val TEMP_SUSPECT = 90.0
        const val TEMP_CONFIRMED = 120.0

        // Rate of rise thresholds (Celsius per minute)
        const val ROR_WATCH = 5.0
        const val ROR_ANOMALY = 10.0
        const val ROR_SUSPECT = 20.0

        // Confidence thresholds
        const val CONFIRM_MIN_SENSORS = 2
        const val CONFIRM_MIN_CONFIDENCE = 0.7

        // Smoke thresholds
        const val SMOKE_WATCH = 0.3
        const val SMOKE_CONFIRMED = 0.7

        // Time windows (ms)
        const val VERIFICATION_WINDOW_MS = 10_000L // 10 seconds
        const val REIGNITION_WATCH_MS = 300_000L // 5 minutes
    }

    /**
     * Evaluate if a new incident should be created from an observation.
     * Returns null if no incident needed, or a decision with the new state.
     */
    fun evaluateNewIncident(observation: SensorObservation): TransitionDecision? {
        // Rule 1: High temperature → WATCH
        val maxTemp = observation.readings["max_celsius"] ?: observation.readings["temperature"]
        if (maxTemp != null && maxTemp >= TEMP_WATCH) {
            return TransitionDecision(
                newState = IncidentState.Watch(
                    enteredAt = GuardianTimestamp(),
                    evidenceChain = EvidenceChain(),
                    triggerObservation = observation,
                    reason = "Temperature ${maxTemp}°C exceeds WATCH threshold ${TEMP_WATCH}°C"
                ),
                reason = "Temperature ${maxTemp}°C exceeds WATCH threshold"
            )
        }

        // Rule 2: Rapid rate of rise → WATCH
        val rateOfRise = observation.readings["rate_of_rise"]
        if (rateOfRise != null && rateOfRise >= ROR_WATCH) {
            return TransitionDecision(
                newState = IncidentState.Watch(
                    enteredAt = GuardianTimestamp(),
                    evidenceChain = EvidenceChain(),
                    triggerObservation = observation,
                    reason = "Rate of rise ${rateOfRise}°C/min exceeds WATCH threshold ${ROR_WATCH}°C/min"
                ),
                reason = "Rate of rise ${rateOfRise}°C/min exceeds WATCH threshold"
            )
        }

        // Rule 3: Smoke detected → WATCH
        if (observation.sensorType == SensorType.SMOKE_DETECTOR) {
            val smokeLevel = observation.readings["smoke_level"]
            if (smokeLevel != null && smokeLevel >= SMOKE_WATCH) {
                return TransitionDecision(
                    newState = IncidentState.Watch(
                        enteredAt = GuardianTimestamp(),
                        evidenceChain = EvidenceChain(),
                        triggerObservation = observation,
                        reason = "Smoke level $smokeLevel exceeds WATCH threshold $SMOKE_WATCH"
                    ),
                    reason = "Smoke detected at level $smokeLevel"
                )
            }
        }

        // Rule 4: Flame detected → directly to THERMAL_ANOMALY
        if (observation.sensorType == SensorType.FLAME_DETECTOR) {
            val flameConfidence = observation.readings["flame_confidence"] ?: 0.0
            if (flameConfidence >= 0.5) {
                return TransitionDecision(
                    newState = IncidentState.ThermalAnomaly(
                        enteredAt = GuardianTimestamp(),
                        evidenceChain = EvidenceChain(),
                        thermalObservationId = EvidenceId(),
                        maxCelsius = observation.readings["temperature"] ?: 0.0,
                        rateOfRise = null
                    ),
                    reason = "Flame detected with confidence $flameConfidence"
                )
            }
        }

        // Rule 5: Electrical anomaly → WATCH
        if (observation.sensorType == SensorType.ELECTRICAL_SENSOR) {
            val voltageAnomaly = observation.readings["voltage_anomaly"] ?: 0.0
            val currentAnomaly = observation.readings["current_anomaly"] ?: 0.0
            if (voltageAnomaly > 0.5 || currentAnomaly > 0.5) {
                return TransitionDecision(
                    newState = IncidentState.Watch(
                        enteredAt = GuardianTimestamp(),
                        evidenceChain = EvidenceChain(),
                        triggerObservation = observation,
                        reason = "Electrical anomaly detected (voltage=$voltageAnomaly, current=$currentAnomaly)"
                    ),
                    reason = "Electrical anomaly detected"
                )
            }
        }

        return null
    }

    /**
     * Evaluate transition for an active incident based on new observation.
     */
    fun evaluateTransition(
        currentState: IncidentState,
        observation: SensorObservation,
        incident: Incident
    ): TransitionDecision? {
        return when (currentState) {
            is IncidentState.Normal -> evaluateFromNormal(observation)
            is IncidentState.Watch -> evaluateFromWatch(currentState, observation, incident)
            is IncidentState.ThermalAnomaly -> evaluateFromThermalAnomaly(currentState, observation, incident)
            is IncidentState.Suspect -> evaluateFromSuspect(currentState, observation, incident)
            is IncidentState.MultisensorVerifying -> evaluateFromMultisensorVerifying(currentState, observation, incident)
            is IncidentState.ConfirmedIncident -> evaluateFromConfirmedIncident(currentState, observation, incident)
            is IncidentState.AlarmActive -> evaluateFromAlarmActive(currentState, observation, incident)
            is IncidentState.SuppressionPrepared -> evaluateFromSuppressionPrepared(currentState, observation, incident)
            is IncidentState.SuppressionActive -> evaluateFromSuppressionActive(currentState, observation, incident)
            is IncidentState.VerifyingResponse -> evaluateFromVerifyingResponse(currentState, observation, incident)
            is IncidentState.Extinguished -> evaluateFromExtinguished(currentState, observation, incident)
            is IncidentState.ReignitionWatch -> evaluateFromReignitionWatch(currentState, observation, incident)
            is IncidentState.Recovered -> null // Terminal state
            is IncidentState.Escalated -> null // Awaiting external action
            is IncidentState.FireService -> null // Awaiting external action
        }
    }

    private fun evaluateFromNormal(observation: SensorObservation): TransitionDecision? {
        return evaluateNewIncident(observation)
    }

    private fun evaluateFromWatch(
        state: IncidentState.Watch,
        observation: SensorObservation,
        incident: Incident
    ): TransitionDecision? {
        val maxTemp = observation.readings["max_celsius"] ?: observation.readings["temperature"]

        // Rule: Temperature rising → THERMAL_ANOMALY
        if (maxTemp != null && maxTemp >= TEMP_ANOMALY) {
            return TransitionDecision(
                newState = IncidentState.ThermalAnomaly(
                    enteredAt = GuardianTimestamp(),
                    evidenceChain = state.evidenceChain,
                    thermalObservationId = EvidenceId(),
                    maxCelsius = maxTemp,
                    rateOfRise = observation.readings["rate_of_rise"]
                ),
                reason = "Temperature ${maxTemp}°C exceeds ANOMALY threshold ${TEMP_ANOMALY}°C"
            )
        }

        // Rule: Multiple indicators → SUSPECT
        val indicators = countIndicators(observation, incident)
        if (indicators >= 2) {
            return TransitionDecision(
                newState = IncidentState.Suspect(
                    enteredAt = GuardianTimestamp(),
                    evidenceChain = state.evidenceChain,
                    observations = listOf(EvidenceId()),
                    indicators = listOf("Multiple indicators: $indicators")
                ),
                reason = "$indicators indicators detected"
            )
        }

        // Rule: Temperature dropped below watch → NORMAL (recovery)
        if (maxTemp != null && maxTemp < TEMP_WATCH * 0.8) {
            return TransitionDecision(
                newState = IncidentState.Normal(
                    enteredAt = GuardianTimestamp(),
                    evidenceChain = state.evidenceChain
                ),
                reason = "Temperature ${maxTemp}°C dropped below recovery threshold"
            )
        }

        return null
    }

    private fun evaluateFromThermalAnomaly(
        state: IncidentState.ThermalAnomaly,
        observation: SensorObservation,
        incident: Incident
    ): TransitionDecision? {
        val maxTemp = observation.readings["max_celsius"] ?: observation.readings["temperature"]

        // Rule: Temperature very high + multiple sensors → CONFIRMED
        if (maxTemp != null && maxTemp >= TEMP_CONFIRMED) {
            val indicators = countIndicators(observation, incident)
            if (indicators >= CONFIRM_MIN_SENSORS) {
                return TransitionDecision(
                    newState = IncidentState.ConfirmedIncident(
                        enteredAt = GuardianTimestamp(),
                        evidenceChain = state.evidenceChain,
                        confirmations = listOf(EvidenceId()),
                        confidence = calculateConfidence(maxTemp, indicators),
                        affectedZones = listOf(incident.currentZone)
                    ),
                    reason = "Temperature ${maxTemp}°C with $indicators sensor confirmations"
                )
            }
        }

        // Rule: Rate of rise accelerating → SUSPECT
        val rateOfRise = observation.readings["rate_of_rise"]
        if (rateOfRise != null && rateOfRise >= ROR_SUSPECT) {
            return TransitionDecision(
                newState = IncidentState.Suspect(
                    enteredAt = GuardianTimestamp(),
                    evidenceChain = state.evidenceChain,
                    observations = listOf(EvidenceId()),
                    indicators = listOf("Rate of rise ${rateOfRise}°C/min")
                ),
                reason = "Rate of rise ${rateOfRise}°C/min exceeds SUSPECT threshold"
            )
        }

        // Rule: Temperature dropping → back to WATCH
        if (maxTemp != null && maxTemp < TEMP_ANOMALY * 0.8) {
            return TransitionDecision(
                newState = IncidentState.Watch(
                    enteredAt = GuardianTimestamp(),
                    evidenceChain = state.evidenceChain,
                    triggerObservation = observation,
                    reason = "Temperature ${maxTemp}°C dropped below ANOMALY threshold"
                ),
                reason = "Temperature decreasing"
            )
        }

        return null
    }

    private fun evaluateFromSuspect(
        state: IncidentState.Suspect,
        observation: SensorObservation,
        incident: Incident
    ): TransitionDecision? {
        // Rule: Multi-sensor verification
        val indicators = countIndicators(observation, incident)
        if (indicators >= CONFIRM_MIN_SENSORS) {
            return TransitionDecision(
                newState = IncidentState.MultisensorVerifying(
                    enteredAt = GuardianTimestamp(),
                    evidenceChain = state.evidenceChain,
                    verificationStart = GuardianTimestamp(),
                    sensorsChecking = listOf(observation.sensorId)
                ),
                reason = "$indicators sensors triggered — initiating verification"
            )
        }

        return null
    }

    private fun evaluateFromMultisensorVerifying(
        state: IncidentState.MultisensorVerifying,
        observation: SensorObservation,
        incident: Incident
    ): TransitionDecision? {
        val elapsed = GuardianTimestamp().elapsedSince(state.verificationStart)

        // Rule: Verification window expired with enough confirmations → CONFIRMED
        if (elapsed >= VERIFICATION_WINDOW_MS) {
            val indicators = countIndicators(observation, incident)
            if (indicators >= CONFIRM_MIN_SENSORS) {
                return TransitionDecision(
                    newState = IncidentState.ConfirmedIncident(
                        enteredAt = GuardianTimestamp(),
                        evidenceChain = state.evidenceChain,
                        confirmations = listOf(EvidenceId()),
                        confidence = calculateConfidence(0.0, indicators),
                        affectedZones = listOf(incident.currentZone)
                    ),
                    reason = "Verification complete: $indicators sensors confirmed"
                )
            } else {
                // Not enough confirmations → back to WATCH
                return TransitionDecision(
                    newState = IncidentState.Watch(
                        enteredAt = GuardianTimestamp(),
                        evidenceChain = state.evidenceChain,
                        triggerObservation = observation,
                        reason = "Verification failed: only $indicators sensors confirmed"
                    ),
                    reason = "Insufficient sensor confirmations after verification"
                )
            }
        }

        return null
    }

    private fun evaluateFromConfirmedIncident(
        state: IncidentState.ConfirmedIncident,
        observation: SensorObservation,
        incident: Incident
    ): TransitionDecision? {
        // Rule: Confirmed incident → automatically proceed to ALARM_ACTIVE
        // In production, this would trigger alarm hardware
        return TransitionDecision(
            newState = IncidentState.AlarmActive(
                enteredAt = GuardianTimestamp(),
                evidenceChain = state.evidenceChain,
                alarmId = CommandId(),
                notificationsSent = listOf("system:alarm")
            ),
            reason = "Incident confirmed — activating alarm"
        )
    }

    private fun evaluateFromAlarmActive(
        state: IncidentState.AlarmActive,
        observation: SensorObservation,
        incident: Incident
    ): TransitionDecision? {
        // Rule: Alarm active → prepare suppression
        return TransitionDecision(
            newState = IncidentState.SuppressionPrepared(
                enteredAt = GuardianTimestamp(),
                evidenceChain = state.evidenceChain,
                suppressionType = "water_mist",
                targetZones = listOf(incident.currentZone)
            ),
            reason = "Alarm active — arming suppression"
        )
    }

    private fun evaluateFromSuppressionPrepared(
        state: IncidentState.SuppressionPrepared,
        observation: SensorObservation,
        incident: Incident
    ): TransitionDecision? {
        // Rule: Suppression armed → activate (in production, requires human approval)
        return TransitionDecision(
            newState = IncidentState.SuppressionActive(
                enteredAt = GuardianTimestamp(),
                evidenceChain = state.evidenceChain,
                commandId = CommandId(),
                suppressionState = com.supremeguardian.core.safety.SuppressionState(
                    type = com.supremeguardian.core.safety.SuppressionType.WATER_MIST,
                    armed = true,
                    active = true,
                    targetZones = state.targetZones,
                    activatedAt = GuardianTimestamp()
                )
            ),
            reason = "Suppression armed — activating water mist"
        )
    }

    private fun evaluateFromSuppressionActive(
        state: IncidentState.SuppressionActive,
        observation: SensorObservation,
        incident: Incident
    ): TransitionDecision? {
        // Rule: Suppression active → verify response
        return TransitionDecision(
            newState = IncidentState.VerifyingResponse(
                enteredAt = GuardianTimestamp(),
                evidenceChain = state.evidenceChain,
                verificationStart = GuardianTimestamp(),
                sensorsMonitoring = listOf(observation.sensorId)
            ),
            reason = "Suppression active — monitoring for effect"
        )
    }

    private fun evaluateFromVerifyingResponse(
        state: IncidentState.VerifyingResponse,
        observation: SensorObservation,
        incident: Incident
    ): TransitionDecision? {
        val maxTemp = observation.readings["max_celsius"] ?: observation.readings["temperature"]

        // Rule: Temperature dropped significantly → EXTINGUISHED
        if (maxTemp != null && maxTemp < TEMP_WATCH) {
            return TransitionDecision(
                newState = IncidentState.Extinguished(
                    enteredAt = GuardianTimestamp(),
                    evidenceChain = state.evidenceChain,
                    extinguishedAt = GuardianTimestamp()
                ),
                reason = "Temperature ${maxTemp}°C dropped below ${TEMP_WATCH}°C — fire extinguished"
            )
        }

        // Rule: Temperature still high → continue suppression
        val elapsed = GuardianTimestamp().elapsedSince(state.verificationStart)
        if (elapsed > 60_000) { // After 1 minute
            return TransitionDecision(
                newState = IncidentState.SuppressionActive(
                    enteredAt = GuardianTimestamp(),
                    evidenceChain = state.evidenceChain,
                    commandId = CommandId(),
                    suppressionState = com.supremeguardian.core.safety.SuppressionState(
                        type = com.supremeguardian.core.safety.SuppressionType.WATER_MIST,
                        armed = true,
                        active = true,
                        targetZones = emptyList(),
                        activatedAt = GuardianTimestamp()
                    )
                ),
                reason = "Temperature still elevated — continuing suppression"
            )
        }

        return null
    }

    private fun evaluateFromExtinguished(
        state: IncidentState.Extinguished,
        observation: SensorObservation,
        incident: Incident
    ): TransitionDecision? {
        // Rule: After extinguishing → reignition watch
        return TransitionDecision(
            newState = IncidentState.ReignitionWatch(
                enteredAt = GuardianTimestamp(),
                evidenceChain = state.evidenceChain,
                watchUntil = GuardianTimestamp(instant = java.time.Instant.now().plusMillis(REIGNITION_WATCH_MS))
            ),
            reason = "Entering reignition watch period"
        )
    }

    private fun evaluateFromReignitionWatch(
        state: IncidentState.ReignitionWatch,
        observation: SensorObservation,
        incident: Incident
    ): TransitionDecision? {
        val maxTemp = observation.readings["max_celsius"] ?: observation.readings["temperature"]

        // Rule: Temperature rising again during watch → CONFIRMED (reignition)
        if (maxTemp != null && maxTemp >= TEMP_ANOMALY) {
            return TransitionDecision(
                newState = IncidentState.ConfirmedIncident(
                    enteredAt = GuardianTimestamp(),
                    evidenceChain = state.evidenceChain,
                    confirmations = listOf(EvidenceId()),
                    confidence = 0.9,
                    affectedZones = listOf(incident.currentZone)
                ),
                reason = "Temperature ${maxTemp}°C rising during reignition watch — possible reignition"
            )
        }

        // Rule: Watch period expired without reignition → RECOVERED
        val now = GuardianTimestamp()
        if (now.instant.isAfter(state.watchUntil.instant)) {
            return TransitionDecision(
                newState = IncidentState.Recovered(
                    enteredAt = GuardianTimestamp(),
                    evidenceChain = state.evidenceChain,
                    recoveredAt = GuardianTimestamp(),
                    duration = now.elapsedSince(incident.createdAt)
                ),
                reason = "Reignition watch period expired — incident recovered"
            )
        }

        return null
    }

    /**
     * Count how many independent indicators are present.
     */
    private fun countIndicators(observation: SensorObservation, incident: Incident): Int {
        var count = 0
        val readings = observation.readings

        if ((readings["max_celsius"] ?: 0.0) >= TEMP_ANOMALY) count++
        if ((readings["rate_of_rise"] ?: 0.0) >= ROR_ANOMALY) count++
        if ((readings["smoke_level"] ?: 0.0) >= SMOKE_CONFIRMED) count++
        if ((readings["flame_confidence"] ?: 0.0) >= 0.5) count++
        if ((readings["co_level"] ?: 0.0) >= 0.5) count++

        return count
    }

    /**
     * Calculate confidence based on temperature and indicators.
     */
    private fun calculateConfidence(maxTemp: Double, indicators: Int): Double {
        var confidence = 0.0

        // Temperature contribution
        when {
            maxTemp >= 200.0 -> confidence += 0.4
            maxTemp >= 150.0 -> confidence += 0.3
            maxTemp >= 120.0 -> confidence += 0.2
            maxTemp >= 90.0 -> confidence += 0.1
        }

        // Indicator contribution
        confidence += indicators * 0.15

        return confidence.coerceIn(0.0, 1.0)
    }
}
