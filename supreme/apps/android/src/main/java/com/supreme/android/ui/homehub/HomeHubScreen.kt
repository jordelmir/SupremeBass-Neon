package com.supreme.android.ui.homehub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.supreme.android.ui.theme.*

data class Room(val name: String, val devices: List<String>)

@Composable
fun HomeHubScreen(onBack: () -> Unit) {
    val rooms = listOf(
        Room("Living Room", listOf("Lights", "TV", "AC", "Camera")),
        Room("Kitchen", listOf("Smart Plug", "Smoke Sensor")),
        Room("Bedroom", listOf("Lights", "AC")),
        Room("Garage", listOf("Gate", "Camera", "Lights"))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TitanColors.AbsoluteBlack)
            .padding(16.dp)
    ) {
        NeonTitle(text = "Home Hub", subtitle = "Control your smart home")
        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(rooms) { room ->
                NeonCard(glowColor = TitanColors.ElectricPurple) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Room, contentDescription = null, tint = TitanColors.ElectricPurple)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(room.name, fontWeight = FontWeight.Bold, color = TitanColors.GhostWhite)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        room.devices.forEach { device ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.DeviceHub, contentDescription = null, modifier = Modifier.size(16.dp), tint = TitanColors.GhostWhite.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(device, color = TitanColors.GhostWhite.copy(alpha = 0.8f))
                                Spacer(modifier = Modifier.weight(1f))
                                Switch(checked = false, onCheckedChange = { })
                            }
                        }
                    }
                }
            }
        }
    }
}
