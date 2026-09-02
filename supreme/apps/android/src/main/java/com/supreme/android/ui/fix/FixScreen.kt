package com.supreme.android.ui.fix

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
import com.supreme.android.permissions.RequestMultiplePermissionsEffect
import com.supreme.android.ui.theme.*
import com.supreme.android.viewmodel.FixViewModel

@Composable
fun FixScreen(
    onBack: () -> Unit,
    viewModel: FixViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
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
            .background(TitanColors.AbsoluteBlack)
            .padding(16.dp)
    ) {
        NeonTitle(text = "Fix AI", subtitle = "Point camera at anything - diagnose what's wrong")
        Spacer(modifier = Modifier.height(24.dp))

        if (!hasPermissions) {
            NeonCard(glowColor = TitanColors.NeonRed) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Permissions Required", fontWeight = FontWeight.Bold, color = TitanColors.NeonRed)
                    Text("Camera and microphone permissions are needed for diagnosis", color = TitanColors.GhostWhite.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }
            return
        }

        NeonCard(glowColor = TitanColors.NeonCyan) {
            Box(modifier = Modifier.fillMaxSize().height(200.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Videocam, contentDescription = "Camera", modifier = Modifier.size(48.dp), tint = TitanColors.NeonCyan.copy(alpha = 0.5f))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        NeonButton(
            onClick = { viewModel.analyze() },
            text = if (uiState.isAnalyzing) "Analyzing..." else "Diagnose Problem",
            icon = if (uiState.isAnalyzing) null else Icons.Default.Build,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isAnalyzing,
            color = TitanColors.NeonCyan
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.diagnosis.isNotEmpty()) {
            NeonCard(glowColor = TitanColors.RadioactiveGreen) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Diagnosis", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TitanColors.RadioactiveGreen)
                    Text("Confidence: ${(uiState.confidence * 100).toInt()}%", fontSize = 12.sp, color = TitanColors.GhostWhite.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(uiState.diagnosis, color = TitanColors.GhostWhite)
                    if (uiState.suggestions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Suggestions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TitanColors.NeonCyan)
                        uiState.suggestions.forEach { suggestion ->
                            Text("• $suggestion", color = TitanColors.GhostWhite.copy(alpha = 0.8f), fontSize = 13.sp)
                        }
                    }
                }
            }
        } else {
            NeonCard(glowColor = TitanColors.NeonCyan) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Tap to diagnose a problem", color = TitanColors.GhostWhite.copy(alpha = 0.5f))
                }
            }
        }
    }
}
