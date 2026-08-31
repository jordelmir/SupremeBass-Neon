package com.supreme.android.ui.noise

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
import com.supreme.android.viewmodel.NoiseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoiseScreen(
    onBack: () -> Unit,
    viewModel: NoiseViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Noise Doctor", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Analyze sounds to diagnose mechanical issues", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (uiState.isRecording) {
                    viewModel.stopRecording(FloatArray(0))
                } else {
                    viewModel.startRecording()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = if (uiState.isRecording) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors()
        ) {
            Icon(if (uiState.isRecording) Icons.Default.Stop else Icons.Default.Mic, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (uiState.isRecording) "Stop Recording" else "Record 15 seconds")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.classification.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Analysis", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Classification: ${uiState.classification}", style = MaterialTheme.typography.bodyMedium)
                    Text("Dominant Frequency: ${uiState.dominantFrequency.toInt()} Hz", style = MaterialTheme.typography.bodyMedium)

                    if (uiState.harmonics.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Harmonics:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        uiState.harmonics.forEach { freq ->
                            Text("• ${freq.toInt()} Hz", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    if (uiState.suggestions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Potential Causes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        uiState.suggestions.forEach { cause ->
                            Text("• $cause", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Record a sound to analyze", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
