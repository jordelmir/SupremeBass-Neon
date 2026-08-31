package com.supreme.find

import com.supreme.core.*
import kotlinx.coroutines.flow.*
import java.time.Instant

/**
 * Supreme Find — locate your objects using BLE/UWB.
 *
 * "Find my... keys, backpack, toolbox, bicycle, car, remote, equipment."
 */

class FindEngine {

    private val trackedObjects = mutableMapOf<String, TrackedObject>()
    private val _objectList = MutableStateFlow<List<TrackedObject>>(emptyList())
    val objectList: StateFlow<List<TrackedObject>> = _objectList.asStateFlow()

    /**
     * Add an object to track.
     */
    fun addObject(obj: TrackedObject) {
        trackedObjects[obj.id] = obj
        _objectList.value = trackedObjects.values.toList()
    }

    /**
     * Remove an object from tracking.
     */
    fun removeObject(objectId: String) {
        trackedObjects.remove(objectId)
        _objectList.value = trackedObjects.values.toList()
    }

    /**
     * Update location from BLE scan.
     */
    fun updateFromBLE(
        objectId: String,
        rssi: Int,
        txPower: Int? = null,
        timestamp: Instant = Instant.now()
    ) {
        val obj = trackedObjects[objectId] ?: return

        val distance = if (txPower != null) {
            calculateDistance(rssi, txPower)
        } else null

        val proximity = classifyProximity(rssi)

        trackedObjects[objectId] = obj.copy(
            lastSeen = timestamp,
            rssi = rssi,
            distanceMeters = distance,
            proximity = proximity,
            connectionStatus = ConnectionStatus.IN_RANGE
        )
        _objectList.value = trackedObjects.values.toList()
    }

    /**
     * Update location from UWB ranging.
     */
    fun updateFromUWB(
        objectId: String,
        distanceMeters: Double,
        angleDegrees: Double? = null,
        timestamp: Instant = Instant.now()
    ) {
        val obj = trackedObjects[objectId] ?: return

        trackedObjects[objectId] = obj.copy(
            lastSeen = timestamp,
            distanceMeters = distanceMeters,
            angleDegrees = angleDegrees,
            proximity = classifyProximityFromDistance(distanceMeters),
            connectionStatus = ConnectionStatus.IN_RANGE,
            locationMethod = LocationMethod.UWB
        )
        _objectList.value = trackedObjects.values.toList()
    }

    /**
     * Get objects sorted by proximity.
     */
    fun getObjectsByProximity(): List<TrackedObject> {
        return trackedObjects.values.sortedBy { it.distanceMeters ?: Double.MAX_VALUE }
    }

    /**
     * Get lost objects (not seen recently).
     */
    fun getLostObjects(withinMinutes: Int = 30): List<TrackedObject> {
        val cutoff = Instant.now().minusSeconds(withinMinutes.toLong() * 60)
        return trackedObjects.values.filter {
            it.lastSeen?.isBefore(cutoff) ?: true
        }
    }

    /**
     * Find nearest object of a category.
     */
    fun findNearest(category: ObjectCategory): TrackedObject? {
        return trackedObjects.values
            .filter { it.category == category }
            .minByOrNull { it.distanceMeters ?: Double.MAX_VALUE }
    }

    // ─────────────────────────────────────────────────────────────
    // CALCULATIONS
    // ─────────────────────────────────────────────────────────────

    private fun calculateDistance(rssi: Int, txPower: Int): Double {
        if (rssi == 0) return -1.0
        val ratio = (txPower - rssi).toDouble() / (20.0)
        return 10.0.pow(ratio)
    }

    private fun classifyProximity(rssi: Int): Proximity {
        return when {
            rssi >= -50 -> Proximity.IMMEDIATE
            rssi >= -60 -> Proximity.NEAR
            rssi >= -70 -> Proximity.FAR
            else -> Proximity.OUT_OF_RANGE
        }
    }

    private fun classifyProximityFromDistance(distanceMeters: Double): Proximity {
        return when {
            distanceMeters <= 1.0 -> Proximity.IMMEDIATE
            distanceMeters <= 3.0 -> Proximity.NEAR
            distanceMeters <= 10.0 -> Proximity.FAR
            else -> Proximity.OUT_OF_RANGE
        }
    }

    private fun Double.pow(exponent: Double): Double {
        return Math.pow(this, exponent)
    }
}

// ─────────────────────────────────────────────────────────────
// DATA CLASSES
// ─────────────────────────────────────────────────────────────

data class TrackedObject(
    val id: String,
    val name: String,
    val category: ObjectCategory,
    val bleDeviceId: String? = null,
    val uwbDeviceId: String? = null,
    val lastSeen: Instant? = null,
    val rssi: Int? = null,
    val distanceMeters: Double? = null,
    val angleDegrees: Double? = null,
    val proximity: Proximity = Proximity.OUT_OF_RANGE,
    val connectionStatus: ConnectionStatus = ConnectionStatus.OUT_OF_RANGE,
    val locationMethod: LocationMethod = LocationMethod.BLE,
    val zone: String? = null,
    val photo: String? = null,
    val notes: String? = null
)

enum class ObjectCategory {
    KEYS,
    BACKPACK,
    TOOLBOX,
    BICYCLE,
    CAR,
    REMOTE,
    EQUIPMENT,
    WALLET,
    PHONE,
    TABLET,
    LAPTOP,
    CAMERA,
    OTHER
}

enum class Proximity {
    IMMEDIATE, // <1m
    NEAR,      // 1-3m
    FAR,       // 3-10m
    OUT_OF_RANGE // >10m or lost
}

enum class ConnectionStatus {
    CONNECTED,
    IN_RANGE,
    OUT_OF_RANGE,
    LOST
}

enum class LocationMethod {
    BLE,
    UWB,
    GPS,
    WIFI,
    MANUAL
}
