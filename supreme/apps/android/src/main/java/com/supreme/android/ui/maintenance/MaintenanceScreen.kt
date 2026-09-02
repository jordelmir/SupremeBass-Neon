package com.supreme.android.ui.maintenance

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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.supreme.android.ui.theme.*
import com.supreme.android.viewmodel.MaintenanceViewModel

@Composable
fun MaintenanceScreen(
    onBack: () -> Unit,
    viewModel: MaintenanceViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TitanColors.AbsoluteBlack)
            .padding(16.dp)
    ) {
        NeonTitle(text = "Maintenance", subtitle = "Scheduled maintenance for all your assets")
        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.tasks.isNotEmpty()) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.tasks) { task ->
                    NeonCard(glowColor = if (task.isOverdue) TitanColors.NeonRed else TitanColors.RadioactiveGreen) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (task.isOverdue) Icons.Default.Warning else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (task.isOverdue) TitanColors.NeonRed else TitanColors.RadioactiveGreen
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(task.title, fontWeight = FontWeight.Bold, color = TitanColors.GhostWhite)
                                Text(task.description, color = TitanColors.GhostWhite.copy(alpha = 0.6f), fontSize = 12.sp)
                                Text("Priority: ${task.priority}", color = TitanColors.GhostWhite.copy(alpha = 0.5f), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        } else {
            NeonCard(glowColor = TitanColors.NeonYellow) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No maintenance scheduled yet", color = TitanColors.GhostWhite.copy(alpha = 0.5f))
                }
            }
        }
    }
}
