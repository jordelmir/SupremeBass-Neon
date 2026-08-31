package com.supreme.utilities

import com.supreme.core.*
import kotlinx.coroutines.flow.*
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Supreme Utilities — track water, electricity, and gas consumption.
 *
 * Modes:
 * - Basic: photograph meter → OCR → reading history
 * - Advanced: smart plug / BLE monitor / Modbus meter → per-device analysis
 *
 * Detects anomalies: leaks, unusual consumption, meter issues.
 */

class UtilitiesEngine {

    private val meters = mutableMapOf<String, Meter>()
    private val readings = mutableMapOf<String, MutableList<MeterReading>>()
    private val _state = MutableStateFlow(UtilitiesState())
    val state: StateFlow<UtilitiesState> = _state.asStateFlow()

    /**
     * Add a meter.
     */
    fun addMeter(meter: Meter) {
        meters[meter.id] = meter
        readings[meter.id] = mutableListOf()
        updateState()
    }

    /**
     * Record a meter reading.
     */
    fun recordReading(meterId: String, reading: MeterReading) {
        readings.getOrPut(meterId) { mutableListOf() }.add(reading)
        updateState()
    }

    /**
     * Get consumption history for a meter.
     */
    fun getConsumptionHistory(meterId: String, days: Int = 30): List<ConsumptionEntry> {
        val meterReadings = readings[meterId] ?: return emptyList()
        val now = Instant.now()
        val cutoff = now.minus(days.toLong(), ChronoUnit.DAYS)

        return meterReadings
            .filter { it.timestamp.isAfter(cutoff) }
            .windowed(2)
            .map { (prev, curr) ->
                ConsumptionEntry(
                    date = curr.timestamp,
                    previousReading = prev.value,
                    currentReading = curr.value,
                    consumption = curr.value - prev.value,
                    unit = meters[meterId]?.unit ?: ""
                )
            }
    }

    /**
     * Get consumption summary.
     */
    fun getConsumptionSummary(meterId: String): ConsumptionSummary {
        val history = getConsumptionHistory(meterId, 30)
        val meter = meters[meterId] ?: return ConsumptionSummary()

        if (history.isEmpty()) return ConsumptionSummary()

        val totalConsumption = history.sumOf { it.consumption }
        val averageDaily = totalConsumption / 30
        val maxDaily = history.maxOfOrNull { it.consumption } ?: 0.0
        val minDaily = history.minOfOrNull { it.consumption } ?: 0.0

        // Projection for current month
        val daysInMonth = 30
        val daysPassed = history.size
        val projectedTotal = if (daysPassed > 0) {
            totalConsumption / daysPassed * daysInMonth
        } else 0.0

        // Anomaly detection
        val anomalies = detectAnomalies(history, averageDaily)

        return ConsumptionSummary(
            meterId = meterId,
            meterType = meter.type,
            periodDays = 30,
            totalConsumption = totalConsumption,
            averageDaily = averageDaily,
            maxDaily = maxDaily,
            minDaily = minDaily,
            projectedMonthly = projectedTotal,
            unit = meter.unit,
            estimatedCost = totalConsumption * (meter.costPerUnit ?: 0.0),
            anomalies = anomalies
        )
    }

    /**
     * Get all meters summary.
     */
    fun getAllMetersSummary(): List<ConsumptionSummary> {
        return meters.keys.map { getConsumptionSummary(it) }
    }

    /**
     * Detect anomalies in consumption.
     */
    private fun detectAnomalies(history: List<ConsumptionEntry>, averageDaily: Double): List<ConsumptionAnomaly> {
        val anomalies = mutableListOf<ConsumptionAnomaly>()

        if (averageDaily <= 0) return anomalies

        // Check for spikes
        history.forEach { entry ->
            val deviation = if (averageDaily > 0) {
                (entry.consumption - averageDaily) / averageDaily * 100
            } else 0.0

            if (deviation > 50) {
                anomalies.add(ConsumptionAnomaly(
                    type = AnomalyType.SPIKE,
                    date = entry.date,
                    value = entry.consumption,
                    expected = averageDaily,
                    deviation = deviation,
                    description = "Consumption ${deviation.toInt()}% above average"
                ))
            }
        }

        // Check for continuous increase
        if (history.size >= 7) {
            val recentWeek = history.takeLast(7)
            val increases = recentWeek.zipWithNext().count { (a, b) -> b.consumption > a.consumption }
            if (increases >= 5) {
                anomalies.add(ConsumptionAnomaly(
                    type = AnomalyType.TREND_INCREASE,
                    date = Instant.now(),
                    value = recentWeek.last().consumption,
                    expected = averageDaily,
                    deviation = ((recentWeek.last().consumption - averageDaily) / averageDaily * 100),
                    description = "Consumption increasing over past week"
                ))
            }
        }

        return anomalies
    }

    /**
     * OCR a meter photo.
     */
    suspend fun ocrMeterPhoto(imageBytes: ByteArray, meterType: MeterType): OCRMeterResult {
        // TODO: Use ML Kit OCR
        return OCRMeterResult(
            reading = 0.0,
            confidence = 0.0,
            rawText = "",
            success = false,
            error = "OCR not yet implemented"
        )
    }

    private fun updateState() {
        _state.value = UtilitiesState(
            meters = meters.values.toList(),
            totalMeters = meters.size,
            totalReadings = readings.values.sumOf { it.size }
        )
    }
}

// ─────────────────────────────────────────────────────────────
// DATA CLASSES
// ─────────────────────────────────────────────────────────────

data class Meter(
    val id: String,
    val name: String,
    val type: MeterType,
    val unit: String,
    val costPerUnit: Double? = null,
    val location: String? = null,
    val protocol: DeviceProtocol = DeviceProtocol.CAMERA
)

enum class MeterType {
    WATER,
    ELECTRICITY,
    GAS,
    OTHER
}

data class MeterReading(
    val value: Double,
    val timestamp: Instant = Instant.now(),
    val source: ReadingSource = ReadingSource.MANUAL,
    val confidence: Double = 1.0,
    val photoPath: String? = null
)

enum class ReadingSource {
    MANUAL,
    OCR,
    SMART_PLUG,
    BLE_MONITOR,
    MODBUS,
    API
}

data class ConsumptionEntry(
    val date: Instant,
    val previousReading: Double,
    val currentReading: Double,
    val consumption: Double,
    val unit: String
)

data class ConsumptionSummary(
    val meterId: String = "",
    val meterType: MeterType = MeterType.OTHER,
    val periodDays: Int = 0,
    val totalConsumption: Double = 0.0,
    val averageDaily: Double = 0.0,
    val maxDaily: Double = 0.0,
    val minDaily: Double = 0.0,
    val projectedMonthly: Double = 0.0,
    val unit: String = "",
    val estimatedCost: Double = 0.0,
    val anomalies: List<ConsumptionAnomaly> = emptyList()
)

data class ConsumptionAnomaly(
    val type: AnomalyType,
    val date: Instant,
    val value: Double,
    val expected: Double,
    val deviation: Double,
    val description: String
)

enum class AnomalyType {
    SPIKE,
    DROP,
    TREND_INCREASE,
    TREND_DECREASE,
    PATTERN_CHANGE
}

data class UtilitiesState(
    val meters: List<Meter> = emptyList(),
    val totalMeters: Int = 0,
    val totalReadings: Int = 0
)

data class OCRMeterResult(
    val reading: Double,
    val confidence: Double,
    val rawText: String,
    val success: Boolean,
    val error: String? = null
)
