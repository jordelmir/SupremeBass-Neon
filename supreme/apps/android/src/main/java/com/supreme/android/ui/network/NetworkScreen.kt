package com.supreme.android.ui.network

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
import com.supreme.android.viewmodel.NetworkViewModel

@Composable
fun NetworkScreen(
    onBack: () -> Unit,
    viewModel: NetworkViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var hasPermission by remember { mutableStateOf(false) }

    RequestPermissionEffect(
        permission = Manifest.permission.ACCESS_FINE_LOCATION,
        onGranted = { hasPermission = true }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TitanColors.AbsoluteBlack)
            .padding(16.dp)
    ) {
        NeonTitle(text = "Network Doctor", subtitle = "Diagnose Wi-Fi and Internet issues")
        Spacer(modifier = Modifier.height(24.dp))

        if (!hasPermission) {
            NeonCard(glowColor = TitanColors.NeonRed) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Permission Required", fontWeight = FontWeight.Bold, color = TitanColors.NeonRed)
                    Text("Location permission is needed to scan Wi-Fi networks", color = TitanColors.GhostWhite.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }
            return
        }

        NeonButton(
            onClick = { viewModel.diagnose() },
            text = if (uiState.isDiagnosing) "Diagnosing..." else "Run Diagnosis",
            icon = if (uiState.isDiagnosing) null else Icons.Default.PlayArrow,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isDiagnosing,
            color = TitanColors.RadioactiveGreen
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.diagnosis.isNotEmpty()) {
            NeonCard(glowColor = TitanColors.RadioactiveGreen) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Diagnosis", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TitanColors.RadioactiveGreen)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(uiState.diagnosis, color = TitanColors.GhostWhite)
                    if (uiState.checks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Checks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TitanColors.NeonCyan)
                        uiState.checks.forEach { check ->
                            Text("• $check", color = TitanColors.GhostWhite.copy(alpha = 0.8f), fontSize = 13.sp)
                        }
                    }
                    if (uiState.recommendations.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Recommendations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TitanColors.NeonYellow)
                        uiState.recommendations.forEach { rec ->
                            Text("• $rec", color = TitanColors.GhostWhite.copy(alpha = 0.8f), fontSize = 13.sp)
                        }
                    }
                }
            }
        } else {
            NeonCard(glowColor = TitanColors.RadioactiveGreen) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Tap to diagnose your network", color = TitanColors.GhostWhite.copy(alpha = 0.5f))
                }
            }
        }
    }
}
