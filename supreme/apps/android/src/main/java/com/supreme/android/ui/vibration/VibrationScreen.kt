package com.supreme.android.ui.vibration

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.supreme.android.ui.theme.*
import com.supreme.android.viewmodel.VibrationViewModel

@Composable
fun VibrationScreen(
    onBack: () -> Unit,
    viewModel: VibrationViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.HIGH_SAMPLING_RATE_SENSORS
        ) == PackageManager.PERMISSION_GRANTED
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TitanColors.AbsoluteBlack)
            .padding(16.dp)
    ) {
        NeonTitle(text = "Vibration Doctor", subtitle = "Place phone on equipment to measure vibration")
        Spacer(modifier = Modifier.height(24.dp))

        NeonCard(glowColor = TitanColors.NeonOrange) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(48.dp), tint = TitanColors.NeonOrange)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Place phone flat on the machine", fontWeight = FontWeight.Bold, color = TitanColors.GhostWhite)
                Text("Keep still during recording", color = TitanColors.GhostWhite.copy(alpha = 0.6f), fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        NeonButton(
            onClick = {
                if (uiState.isRecording) {
                    viewModel.stopRecording(FloatArray(0), FloatArray(0), FloatArray(0))
                } else {
                    viewModel.startRecording()
                }
            },
            text = if (uiState.isRecording) "Stop" else "Record 10 seconds",
            icon = if (uiState.isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
            modifier = Modifier.fillMaxWidth(),
            color = if (uiState.isRecording) TitanColors.NeonRed else TitanColors.NeonOrange
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.baselineComparison.isNotEmpty()) {
            NeonCard(glowColor = TitanColors.NeonOrange) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Vibration Analysis", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TitanColors.NeonOrange)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("RMS X: ${String.format("%.3f", uiState.rmsX)} g", color = TitanColors.GhostWhite)
                    Text("RMS Y: ${String.format("%.3f", uiState.rmsY)} g", color = TitanColors.GhostWhite)
                    Text("RMS Z: ${String.format("%.3f", uiState.rmsZ)} g", color = TitanColors.GhostWhite)
                    Text("Total RMS: ${String.format("%.3f", uiState.totalRMS)} g", fontWeight = FontWeight.Bold, color = TitanColors.NeonCyan)
                    Text("Dominant: ${uiState.dominantFrequency.toInt()} Hz", color = TitanColors.GhostWhite)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Baseline: ${uiState.baselineComparison}", color = TitanColors.GhostWhite.copy(alpha = 0.7f))
                    if (uiState.suggestions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Recommendations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TitanColors.RadioactiveGreen)
                        uiState.suggestions.forEach { rec ->
                            Text("• $rec", color = TitanColors.GhostWhite.copy(alpha = 0.8f), fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
