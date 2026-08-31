package com.supreme.android.ui.camerahub

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

data class Camera(
    val name: String,
    val type: String,
    val status: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraHubScreen(onBack: () -> Unit) {
    val cameras = listOf(
        Camera("Front Door", "ONVIF", "Online"),
        Camera("Backyard", "RTSP", "Online"),
        Camera("Garage", "IP Camera", "Offline")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Camera Hub", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Manage your cameras", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(cameras) { camera ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(camera.name, fontWeight = FontWeight.Bold)
                            Text("${camera.type} • ${camera.status}", style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(
                            if (camera.status == "Online") Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (camera.status == "Online") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
