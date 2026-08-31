package com.supremeguardian.console

import com.supremeguardian.core.building.*
import com.supremeguardian.core.incident.Incident
import com.supremeguardian.core.shared.*
import com.supremeguardian.core.thermal.ThermalObservation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Console ViewModel — bridges domain models to UI.
 *
 * This is the ViewModel for the Supreme Guardian Android console.
 * It receives domain events and exposes UI state.
 */
class ConsoleViewModel {

    // UI State
    private val _siteState = MutableStateFlow(SiteState())
    val siteState: StateFlow<SiteState> = _siteState.asStateFlow()

    private val _selectedZone = MutableStateFlow<ZoneId?>(null)
    val selectedZone: StateFlow<ZoneId?> = _selectedZone.asStateFlow()

    private val _incidentDetail = MutableStateFlow<IncidentDetailState?>(null)
    val incidentDetail: StateFlow<IncidentDetailState?> = _incidentDetail.asStateFlow()

    /**
     * Update site state from building digital twin.
     */
    fun updateBuilding(building: Building) {
        val zoneStates = building.getAllZones().map { zone ->
            ZoneUIState(
                zoneId = zone.id,
                name = zone.name,
                status = ZoneStatus.NORMAL,
                temperature = null,
                camerasOnline = zone.cameras.size,
                camerasTotal = zone.cameras.size,
                sensorsOnline = zone.sensors.size,
                sensorsTotal = zone.sensors.size,
                suppressionArmed = false,
                suppressionActive = false
            )
        }

        _siteState.update { state ->
            state.copy(
                buildingName = building.name,
                zones = zoneStates
            )
        }
    }

    /**
     * Update zone state from ZoneState.
     */
    fun updateZoneState(zoneState: ZoneState) {
        _siteState.update { state ->
            val updatedZones = state.zones.map { zone ->
                if (zone.zoneId == zoneState.zoneId) {
                    zone.copy(
                        status = when {
                            zoneState.isCritical -> ZoneStatus.CRITICAL
                            zoneState.isWatch -> ZoneStatus.WATCH
                            else -> ZoneStatus.NORMAL
                        },
                        temperature = zoneState.maxTemperature,
                        camerasOnline = zoneState.camerasOnline,
                        camerasTotal = zoneState.camerasTotal,
                        sensorsOnline = zoneState.sensorsOnline,
                        sensorsTotal = zoneState.sensorsTotal,
                        suppressionArmed = zoneState.suppressionArmed,
                        suppressionActive = zoneState.suppressionActive
                    )
                } else zone
            }
            state.copy(zones = updatedZones)
        }
    }

    /**
     * Update incident state.
     */
    fun updateIncident(incident: Incident) {
        _siteState.update { state ->
            state.copy(
                activeIncidents = state.activeIncidents + 1,
                currentIncidentZone = incident.currentZone
            )
        }

        // Update zone status to CRITICAL
        _siteState.update { state ->
            val updatedZones = state.zones.map { zone ->
                if (zone.zoneId == incident.currentZone) {
                    zone.copy(status = ZoneStatus.CRITICAL)
                } else zone
            }
            state.copy(zones = updatedZones)
        }
    }

    /**
     * Select a zone for detailed view.
     */
    fun selectZone(zoneId: ZoneId?) {
        _selectedZone.value = zoneId
    }

    /**
     * Get incident detail for display.
     */
    fun showIncidentDetail(incident: Incident) {
        _incidentDetail.value = IncidentDetailState(
            incidentId = incident.id.value,
            zoneName = incident.currentZone.value,
            status = incident.state.name,
            durationMs = incident.durationMs,
            timeline = incident.timeline.map { entry ->
                TimelineEntryUI(
                    timestamp = entry.timestamp.iso8601,
                    fromState = entry.fromState,
                    toState = entry.toState,
                    reason = entry.reason
                )
            },
            evidenceCount = incident.timeline.sumOf { it.evidenceIds.size }
        )
    }

    /**
     * Clear incident detail.
     */
    fun clearIncidentDetail() {
        _incidentDetail.value = null
    }
}

/**
 * Site UI state — the main dashboard state.
 */
data class SiteState(
    val buildingName: String = "Loading...",
    val zones: List<ZoneUIState> = emptyList(),
    val activeIncidents: Int = 0,
    val currentIncidentZone: ZoneId? = null,
    val lastUpdate: GuardianTimestamp = GuardianTimestamp()
) {
    val totalCamerasOnline: Int get() = zones.sumOf { it.camerasOnline }
    val totalCameras: Int get() = zones.sumOf { it.camerasTotal }
    val totalSensorsOnline: Int get() = zones.sumOf { it.sensorsOnline }
    val totalSensors: Int get() = zones.sumOf { it.sensorsTotal }
    val criticalZones: Int get() = zones.count { it.status == ZoneStatus.CRITICAL }
    val watchZones: Int get() = zones.count { it.status == ZoneStatus.WATCH }
}

data class ZoneUIState(
    val zoneId: ZoneId,
    val name: String,
    val status: ZoneStatus,
    val temperature: Double?,
    val camerasOnline: Int,
    val camerasTotal: Int,
    val sensorsOnline: Int,
    val sensorsTotal: Int,
    val suppressionArmed: Boolean,
    val suppressionActive: Boolean
)

enum class ZoneStatus {
    NORMAL,
    WATCH,
    CRITICAL
}

data class IncidentDetailState(
    val incidentId: String,
    val zoneName: String,
    val status: String,
    val durationMs: Long,
    val timeline: List<TimelineEntryUI>,
    val evidenceCount: Int
)

data class TimelineEntryUI(
    val timestamp: String,
    val fromState: String,
    val toState: String,
    val reason: String
)
