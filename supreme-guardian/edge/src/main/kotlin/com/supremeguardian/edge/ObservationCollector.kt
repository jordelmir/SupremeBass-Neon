package com.supremeguardian.edge

import com.supremeguardian.core.building.ZoneId
import com.supremeguardian.core.sensor.SensorType
import com.supremeguardian.core.shared.*
import com.supremeguardian.core.thermal.CameraId
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Observation Collector — aggregates readings from multiple sources.
 *
 * Responsibilities:
 *   1. Collect thermal observations from cameras
 *   2. Collect sensor readings (smoke, heat, flame, CO)
 *   3. Buffer observations within time window
 *   4. Emit aggregated observations for fusion engine
 *   5. Track observation quality and completeness
 */
class ObservationCollector {
    companion object {
        const val COLLECTION_WINDOW_MS = 5000L // 5 second window
        const val MAX_OBSERVATIONS_PER_CAMERA = 10
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val running = AtomicBoolean(false)

    // Buffered observations
    private val thermalBuffer = ConcurrentHashMap<CameraId, MutableList<ThermalObservation>>()
    private val sensorBuffer = ConcurrentHashMap<SensorId, MutableList<SensorReading>>()

    // State
    private val _state = MutableStateFlow(CollectorState())
    val state: StateFlow<CollectorState> = _state.asStateFlow()

    // Output
    private val _observations = MutableSharedFlow<CollectedObservation>(replay = 1)
    val observations: SharedFlow<CollectedObservation> = _observations.asSharedFlow()

    /**
     * Start the collector.
     */
    fun start() {
        if (running.getAndSet(true)) return

        // Start collection loop
        scope.launch {
            while (running.get()) {
                try {
                    collectAndEmit()
                } catch (e: Exception) {
                    // Collection failure is non-fatal
                }
                delay(COLLECTION_WINDOW_MS)
            }
        }
    }

    /**
     * Stop the collector.
     */
    fun stop() {
        if (!running.getAndSet(false)) return

        thermalBuffer.clear()
        sensorBuffer.clear()
        scope.cancel()
    }

    /**
     * Add a thermal observation.
     */
    fun addThermalObservation(observation: ThermalObservation) {
        if (!running.get()) return

        val cameraId = observation.cameraId
        val buffer = thermalBuffer.getOrPut(cameraId) { mutableListOf() }

        buffer.add(observation)

        // Limit buffer size
        if (buffer.size > MAX_OBSERVATIONS_PER_CAMERA) {
            buffer.removeFirst()
        }

        updateState()
    }

    /**
     * Add a sensor reading.
     */
    fun addSensorReading(reading: SensorReading) {
        if (!running.get()) return

        val sensorId = reading.sensorId
        val buffer = sensorBuffer.getOrPut(sensorId) { mutableListOf() }

        buffer.add(reading)

        updateState()
    }

    /**
     * Get aggregated observations for a zone.
     */
    fun getAggregatedObservations(zoneId: ZoneId): AggregatedObservations {
        val thermal = thermalBuffer.values.flatten()
            .filter { it.zoneId == zoneId }

        val sensor = sensorBuffer.values.flatten()
            .filter { it.zoneId == zoneId }

        return AggregatedObservations(
            zoneId = zoneId,
            thermalObservations = thermal,
            sensorReadings = sensor,
            timestamp = GuardianTimestamp()
        )
    }

    /**
     * Get observation statistics.
     */
    fun getStatistics(): CollectorStatistics {
        val totalThermal = thermalBuffer.values.sumOf { it.size }
        val totalSensor = sensorBuffer.values.sumOf { it.size }

        return CollectorStatistics(
            thermalObservationsCount = totalThermal,
            sensorReadingsCount = totalSensor,
            activeCameras = thermalBuffer.size,
            activeSensors = sensorBuffer.size,
            oldestObservation = findOldestObservation()
        )
    }

    /**
     * Clear all buffered observations.
     */
    fun clear() {
        thermalBuffer.clear()
        sensorBuffer.clear()
        updateState()
    }

    private suspend fun collectAndEmit() {
        // Group observations by zone
        val allThermal = thermalBuffer.values.flatten()
        val allSensor = sensorBuffer.values.flatten()

        val byZone = (allThermal.map { it.zoneId } + allSensor.map { it.zoneId })
            .distinct()

        byZone.forEach { zoneId ->
            val observations = getAggregatedObservations(zoneId)

            if (observations.hasData()) {
                _observations.tryEmit(CollectedObservation(
                    zoneId = zoneId,
                    thermal = observations.thermalObservations,
                    sensors = observations.sensorReadings,
                    timestamp = GuardianTimestamp()
                ))
            }
        }

        // Clear processed observations
        thermalBuffer.clear()
        sensorBuffer.clear()

        updateState()
    }

    private fun findOldestObservation(): GuardianTimestamp? {
        val allTimestamps = thermalBuffer.values.flatten().map { it.timestamp } +
                           sensorBuffer.values.flatten().map { it.timestamp }

        return allTimestamps.minByOrNull { it.toInstant()?.toEpochMilli() ?: 0 }
    }

    private fun updateState() {
        _state.update { state ->
            state.copy(
                thermalObservationsCount = thermalBuffer.values.sumOf { it.size },
                sensorReadingsCount = sensorBuffer.values.sumOf { it.size },
                activeCameras = thermalBuffer.size,
                activeSensors = sensorBuffer.size,
                zones = (thermalBuffer.values.flatten().map { it.zoneId } +
                        sensorBuffer.values.flatten().map { it.zoneId }).distinct()
            )
        }
    }
}

/**
 * Thermal observation.
 */
data class ThermalObservation(
    val cameraId: CameraId,
    val zoneId: ZoneId,
    val timestamp: GuardianTimestamp,
    val maxCelsius: Double,
    val minCelsius: Double,
    val averageCelsius: Double,
    val hotspot: com.supremeguardian.engine.HotspotInfo?,
    val rateOfRise: Double?
)

/**
 * Sensor reading.
 */
data class SensorReading(
    val sensorId: SensorId,
    val sensorType: SensorType,
    val zoneId: ZoneId,
    val timestamp: GuardianTimestamp,
    val readings: Map<String, Double>,
    val confidence: Double
)

/**
 * Collected observation.
 */
data class CollectedObservation(
    val zoneId: ZoneId,
    val thermal: List<ThermalObservation>,
    val sensors: List<SensorReading>,
    val timestamp: GuardianTimestamp
)

/**
 * Aggregated observations for a zone.
 */
data class AggregatedObservations(
    val zoneId: ZoneId,
    val thermalObservations: List<ThermalObservation>,
    val sensorReadings: List<SensorReading>,
    val timestamp: GuardianTimestamp
) {
    fun hasData(): Boolean = thermalObservations.isNotEmpty() || sensorReadings.isNotEmpty()
}

/**
 * Collector state.
 */
data class CollectorState(
    val thermalObservationsCount: Int = 0,
    val sensorReadingsCount: Int = 0,
    val activeCameras: Int = 0,
    val activeSensors: Int = 0,
    val zones: List<ZoneId> = emptyList()
)

/**
 * Collector statistics.
 */
data class CollectorStatistics(
    val thermalObservationsCount: Int,
    val sensorReadingsCount: Int,
    val activeCameras: Int,
    val activeSensors: Int,
    val oldestObservation: GuardianTimestamp?
)
