package com.supreme.android.ui.camerahub

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
import androidx.compose.ui.unit.sp
import com.supreme.android.ui.theme.*

data class Camera(val name: String, val type: String, val status: String)

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
            .background(TitanColors.AbsoluteBlack)
            .padding(16.dp)
    ) {
        NeonTitle(text = "Camera Hub", subtitle = "Manage your cameras")
        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(cameras) { camera ->
                NeonCard(glowColor = if (camera.status == "Online") TitanColors.RadioactiveGreen else TitanColors.NeonRed) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null, tint = if (camera.status == "Online") TitanColors.RadioactiveGreen else TitanColors.NeonRed)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(camera.name, fontWeight = FontWeight.Bold, color = TitanColors.GhostWhite)
                            Text("${camera.type} • ${camera.status}", color = TitanColors.GhostWhite.copy(alpha = 0.6f), fontSize = 12.sp)
                        }
                        Icon(
                            if (camera.status == "Online") Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (camera.status == "Online") TitanColors.RadioactiveGreen else TitanColors.NeonRed
                        )
                    }
                }
            }
        }
    }
}
