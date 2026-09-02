package com.supreme.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.supreme.android.viewmodel.AssetsViewModel

@Composable
fun AssetsScreen(
    onNavigateToAsset: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: AssetsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TitanColors.AbsoluteBlack)
            .padding(16.dp)
    ) {
        NeonTitle(text = "Assets", subtitle = "Track everything you own")
        Spacer(modifier = Modifier.height(24.dp))

        NeonButton(
            onClick = { /* TODO: Add asset */ },
            text = "Add Asset",
            icon = Icons.Default.Add,
            modifier = Modifier.fillMaxWidth(),
            color = TitanColors.NeonOrange
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.assets.isNotEmpty()) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.assets) { asset ->
                    NeonCard(
                        glowColor = TitanColors.NeonOrange,
                        onClick = { onNavigateToAsset(asset.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Inventory, contentDescription = null, tint = TitanColors.NeonOrange)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(asset.name, fontWeight = FontWeight.Bold, color = TitanColors.GhostWhite)
                                Text(asset.category, fontSize = 12.sp, color = TitanColors.GhostWhite.copy(alpha = 0.6f))
                                asset.brand?.let { Text("Brand: $it", fontSize = 12.sp, color = TitanColors.GhostWhite.copy(alpha = 0.5f)) }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TitanColors.NeonOrange)
                        }
                    }
                }
            }
        } else {
            NeonCard(glowColor = TitanColors.NeonOrange) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No assets added yet", color = TitanColors.GhostWhite.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun AssetDetailScreen(
    assetId: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TitanColors.AbsoluteBlack)
            .padding(16.dp)
    ) {
        NeonTitle(text = "Asset Detail", subtitle = "ID: $assetId")
        Spacer(modifier = Modifier.height(24.dp))
        NeonCard(glowColor = TitanColors.NeonCyan) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("Asset details coming soon", color = TitanColors.GhostWhite.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TitanColors.AbsoluteBlack)
            .padding(16.dp)
    ) {
        NeonTitle(text = "Settings", subtitle = "Configure Supreme")
        Spacer(modifier = Modifier.height(24.dp))
        NeonCard(glowColor = TitanColors.NeonCyan) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("Settings coming soon", color = TitanColors.GhostWhite.copy(alpha = 0.5f))
            }
        }
    }
}
