package com.supreme.emergency

import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import java.time.Instant

/**
 * Supreme Emergency — tools for emergencies.
 *
 * Features:
 * - Flashlight / SOS
 * - Emergency contacts
 * - Location sharing
 * - Offline emergency instructions
 * - Family meeting points
 * - Medical info card
 * - Home emergency shutoffs
 *
 * This module works OFFLINE — no internet required.
 */

class EmergencyEngine(private val context: Context) {

    /**
     * Flashlight control.
     */
    fun toggleFlashlight(on: Boolean) {
        // TODO: Use CameraManager to control flashlight
    }

    /**
     * SOS signal — flashes flashlight in SOS pattern.
     */
    fun startSOS() {
        // TODO: Implement SOS pattern (3 short, 3 long, 3 short)
    }

    fun stopSOS() {
        // TODO: Stop SOS pattern
    }

    /**
     * Call emergency services.
     */
    fun callEmergency(number: String = "911") {
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$number")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Call a specific emergency contact.
     */
    fun callContact(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$phoneNumber")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Send location via SMS.
     */
    fun sendLocationSMS(phoneNumber: String, location: Location?, message: String = "") {
        val locationText = if (location != null) {
            "https://maps.google.com/?q=${location.latitude},${location.longitude}"
        } else {
            "Location unavailable"
        }

        val fullMessage = if (message.isNotEmpty()) {
            "$message\n\nLocation: $locationText"
        } else {
            "Emergency! My location: $locationText"
        }

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phoneNumber")
            putExtra("sms_body", fullMessage)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Get emergency information card.
     */
    fun getMedicalInfoCard(): MedicalInfoCard {
        // TODO: Load from user settings
        return MedicalInfoCard(
            name = "User Name",
            bloodType = "O+",
            allergies = "None",
            medications = "None",
            conditions = "None",
            emergencyContact1 = EmergencyContact("Contact 1", "8888-8888", "Spouse"),
            emergencyContact2 = EmergencyContact("Contact 2", "8888-8889", "Parent"),
            doctorName = "Dr. Smith",
            doctorPhone = "8888-8890",
            insuranceProvider = "Health Insurance",
            insurancePolicy = "POL-12345"
        )
    }

    /**
     * Get home emergency shutoffs.
     */
    fun getHomeShutoffs(): List<EmergencyShutoff> {
        // TODO: Load from user settings
        return listOf(
            EmergencyShutoff(
                name = "Main Water Valve",
                location = "Under kitchen sink",
                instructions = "Turn clockwise to close",
                photoPath = null
            ),
            EmergencyShutoff(
                name = "Electrical Panel",
                location = "Garage, south wall",
                instructions = "Flip main breaker to OFF",
                photoPath = null
            ),
            EmergencyShutoff(
                name = "Gas Valve",
                location = "Outside, near meter",
                instructions = "Turn handle perpendicular to pipe",
                photoPath = null
            )
        )
    }

    /**
     * Get offline emergency instructions.
     */
    fun getOfflineInstructions(): List<EmergencyInstruction> {
        return listOf(
            EmergencyInstruction(
                title = "Fire Emergency",
                steps = listOf(
                    "Get out immediately",
                    "Stay low to avoid smoke",
                    "Feel doors before opening",
                    "Call 911 when safe",
                    "Meet at designated meeting point",
                    "Do not re-enter building"
                )
            ),
            EmergencyInstruction(
                title = "Earthquake",
                steps = listOf(
                    "Drop, Cover, Hold On",
                    "Stay away from windows",
                    "If outdoors, move to open area",
                    "If driving, pull over safely",
                    "After shaking stops, check for injuries",
                    "Be prepared for aftershocks"
                )
            ),
            EmergencyInstruction(
                title = "Flood",
                steps = listOf(
                    "Move to higher ground",
                    "Do not walk through moving water",
                    "Do not drive through flooded roads",
                    "Turn off utilities if safe",
                    "Avoid electrical wires",
                    "Call for help if trapped"
                )
            ),
            EmergencyInstruction(
                title = "Power Outage",
                steps = listOf(
                    "Unplug sensitive electronics",
                    "Leave one light switch on to know when power returns",
                    "Use flashlights, not candles",
                    "Keep refrigerator closed",
                    "Check on neighbors",
                    "Have backup charging for phone"
                )
            )
        )
    }

    /**
     * Get family meeting points.
     */
    fun getMeetingPoints(): List<MeetingPoint> {
        // TODO: Load from user settings
        return listOf(
            MeetingPoint(
                name = "Front Yard",
                description = "Near the mailbox",
                latitude = 0.0,
                longitude = 0.0
            ),
            MeetingPoint(
                name = "Neighbor's House",
                description = "234 Main Street",
                latitude = 0.0,
                longitude = 0.0
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────
// DATA CLASSES
// ─────────────────────────────────────────────────────────────

data class MedicalInfoCard(
    val name: String,
    val bloodType: String,
    val allergies: String,
    val medications: String,
    val conditions: String,
    val emergencyContact1: EmergencyContact,
    val emergencyContact2: EmergencyContact?,
    val doctorName: String,
    val doctorPhone: String,
    val insuranceProvider: String,
    val insurancePolicy: String
)

data class EmergencyContact(
    val name: String,
    val phone: String,
    val relationship: String
)

data class EmergencyShutoff(
    val name: String,
    val location: String,
    val instructions: String,
    val photoPath: String?
)

data class EmergencyInstruction(
    val title: String,
    val steps: List<String>
)

data class MeetingPoint(
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double
)
