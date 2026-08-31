package com.supreme.android.ui.vibration

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.supreme.android.viewmodel.VibrationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VibrationScreen(
    onBack: () -> Unit,
    viewModel: VibrationViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Vibration Doctor", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Place phone on equipment to measure vibration", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Place phone flat on the machine", fontWeight = FontWeight.Bold)
                Text("Keep still during recording", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (uiState.isRecording) {
                    viewModel.stopRecording(FloatArray(0), FloatArray(0), FloatArray(0))
                } else {
                    viewModel.startRecording()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = if (uiState.isRecording) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors()
        ) {
            Icon(if (uiState.isRecording) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (uiState.isRecording) "Stop" else "Record 10 seconds")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.baselineComparison.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Vibration Analysis", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("RMS X: ${String.format("%.3f", uiState.rmsX)} g", style = MaterialTheme.typography.bodyMedium)
                    Text("RMS Y: ${String.format("%.3f", uiState.rmsY)} g", style = MaterialTheme.typography.bodyMedium)
                    Text("RMS Z: ${String.format("%.3f", uiState.rmsZ)} g", style = MaterialTheme.typography.bodyMedium)
                    Text("Total RMS: ${String.format("%.3f", uiState.totalRMS)} g", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("Dominant: ${uiState.dominantFrequency.toInt()} Hz", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Baseline: ${uiState.baselineComparison}", style = MaterialTheme.typography.bodyMedium)

                    if (uiState.suggestions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Recommendations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        uiState.suggestions.forEach { rec ->
                            Text("• $rec", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
