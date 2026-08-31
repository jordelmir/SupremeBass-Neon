package com.supreme.android.ui.fix

import android.Manifest
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.supreme.android.permissions.PermissionHelper
import com.supreme.android.permissions.RequestMultiplePermissionsEffect
import com.supreme.android.viewmodel.FixViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixScreen(
    onBack: () -> Unit,
    viewModel: FixViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var hasPermissions by remember { mutableStateOf(false) }

    RequestMultiplePermissionsEffect(
        permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ),
        onAllGranted = { hasPermissions = true }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Fix AI",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Point camera at anything — diagnose what's wrong",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (!hasPermissions) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Permissions Required", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                    Text("Camera and microphone permissions are needed for diagnosis", color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
            return
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Videocam, contentDescription = "Camera", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.analyze() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isAnalyzing
        ) {
            if (uiState.isAnalyzing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Build, contentDescription = null)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (uiState.isAnalyzing) "Analyzing..." else "Diagnose Problem")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.diagnosis.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Diagnosis", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Confidence: ${(uiState.confidence * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(uiState.diagnosis, style = MaterialTheme.typography.bodyMedium)
                    if (uiState.suggestions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Suggestions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        uiState.suggestions.forEach { suggestion ->
                            Text("• $suggestion", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Tap to diagnose a problem", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
