package com.supreme.android.ui.warranty

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
import com.supreme.android.viewmodel.WarrantyViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WarrantyScreen(
    onBack: () -> Unit,
    viewModel: WarrantyViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TitanColors.AbsoluteBlack)
            .padding(16.dp)
    ) {
        NeonTitle(text = "Warranty Vault", subtitle = "Scan invoices and warranty cards")
        Spacer(modifier = Modifier.height(24.dp))

        NeonButton(
            onClick = { /* TODO: Scan document */ },
            text = "Scan Document",
            icon = Icons.Default.CameraAlt,
            modifier = Modifier.fillMaxWidth(),
            color = TitanColors.UltraViolet
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.warranties.isNotEmpty()) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.warranties) { warranty ->
                    NeonCard(glowColor = if (warranty.isActive) TitanColors.RadioactiveGreen else TitanColors.NeonRed) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (warranty.isActive) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (warranty.isActive) TitanColors.RadioactiveGreen else TitanColors.NeonRed
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(warranty.assetId, fontWeight = FontWeight.Bold, color = TitanColors.GhostWhite)
                                Text("Provider: ${warranty.provider}", color = TitanColors.GhostWhite.copy(alpha = 0.6f), fontSize = 12.sp)
                                Text("Expires: ${SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(warranty.warrantyEnd))}", color = TitanColors.GhostWhite.copy(alpha = 0.5f), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        } else {
            NeonCard(glowColor = TitanColors.UltraViolet) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No warranties tracked yet", color = TitanColors.GhostWhite.copy(alpha = 0.5f))
                }
            }
        }
    }
}
