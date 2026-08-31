package com.supreme.android.ui.network

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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.supreme.android.viewmodel.NetworkViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScreen(
    onBack: () -> Unit,
    viewModel: NetworkViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Network Doctor", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Diagnose Wi-Fi and Internet issues", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.diagnose() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isDiagnosing
        ) {
            if (uiState.isDiagnosing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (uiState.isDiagnosing) "Diagnosing..." else "Run Diagnosis")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.diagnosis.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Diagnosis", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(uiState.diagnosis, style = MaterialTheme.typography.bodyMedium)

                    if (uiState.checks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Checks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        uiState.checks.forEach { check ->
                            Text("• $check", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    if (uiState.recommendations.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Recommendations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        uiState.recommendations.forEach { rec ->
                            Text("• $rec", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Tap to diagnose your network", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
