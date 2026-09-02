package com.supreme.android.ui.noise

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.supreme.android.permissions.RequestPermissionEffect
import com.supreme.android.ui.theme.*
import com.supreme.android.viewmodel.NoiseViewModel

@Composable
fun NoiseScreen(
    onBack: () -> Unit,
    viewModel: NoiseViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var hasPermission by remember { mutableStateOf(false) }

    RequestPermissionEffect(
        permission = Manifest.permission.RECORD_AUDIO,
        onGranted = { hasPermission = true }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TitanColors.AbsoluteBlack)
            .padding(16.dp)
    ) {
        NeonTitle(text = "Noise Doctor", subtitle = "Analyze sounds to diagnose mechanical issues")
        Spacer(modifier = Modifier.height(24.dp))

        if (!hasPermission) {
            NeonCard(glowColor = TitanColors.NeonRed) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Permission Required", fontWeight = FontWeight.Bold, color = TitanColors.NeonRed)
                    Text("Microphone permission is needed to record sounds", color = TitanColors.GhostWhite.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }
            return
        }

        NeonButton(
            onClick = {
                if (uiState.isRecording) {
                    viewModel.stopRecording(FloatArray(0))
                } else {
                    viewModel.startRecording()
                }
            },
            text = if (uiState.isRecording) "Stop Recording" else "Record 15 seconds",
            icon = if (uiState.isRecording) Icons.Default.Stop else Icons.Default.Mic,
            modifier = Modifier.fillMaxWidth(),
            color = if (uiState.isRecording) TitanColors.NeonRed else TitanColors.NeonYellow
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.classification.isNotEmpty()) {
            NeonCard(glowColor = TitanColors.NeonYellow) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Analysis", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TitanColors.NeonYellow)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Classification: ${uiState.classification}", color = TitanColors.GhostWhite)
                    Text("Dominant Frequency: ${uiState.dominantFrequency.toInt()} Hz", color = TitanColors.GhostWhite)
                    if (uiState.harmonics.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Harmonics:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TitanColors.NeonCyan)
                        uiState.harmonics.forEach { freq ->
                            Text("• ${freq.toInt()} Hz", color = TitanColors.GhostWhite.copy(alpha = 0.8f), fontSize = 13.sp)
                        }
                    }
                    if (uiState.suggestions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Potential Causes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TitanColors.NeonRed)
                        uiState.suggestions.forEach { cause ->
                            Text("• $cause", color = TitanColors.GhostWhite.copy(alpha = 0.8f), fontSize = 13.sp)
                        }
                    }
                }
            }
        } else {
            NeonCard(glowColor = TitanColors.NeonYellow) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Record a sound to analyze", color = TitanColors.GhostWhite.copy(alpha = 0.5f))
                }
            }
        }
    }
}
