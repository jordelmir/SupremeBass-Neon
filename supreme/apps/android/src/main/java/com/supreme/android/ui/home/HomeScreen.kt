package com.supreme.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supreme.android.ui.theme.*

@Composable
fun HomeScreen(
    onNavigateToFix: () -> Unit,
    onNavigateToTools: () -> Unit,
    onNavigateToAssets: () -> Unit,
    onNavigateToMaintenance: () -> Unit,
    onNavigateToWarranty: () -> Unit,
    onNavigateToHomeHub: () -> Unit = {},
    onNavigateToCameraHub: () -> Unit = {},
    onNavigateToFind: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TitanColors.AbsoluteBlack)
            .padding(16.dp)
    ) {
        NeonTitle(
            text = "SUPREME",
            subtitle = "Everyday Intelligence Platform"
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ModuleCard(
                    title = "Fix AI",
                    subtitle = "Diagnose problems",
                    icon = Icons.Default.Build,
                    glowColor = TitanColors.NeonCyan,
                    onClick = onNavigateToFix
                )
            }
            item {
                ModuleCard(
                    title = "Tools",
                    subtitle = "Network, Noise, Vibration",
                    icon = Icons.Default.Handyman,
                    glowColor = TitanColors.RadioactiveGreen,
                    onClick = onNavigateToTools
                )
            }
            item {
                ModuleCard(
                    title = "Assets",
                    subtitle = "Your possessions",
                    icon = Icons.Default.Inventory,
                    glowColor = TitanColors.NeonOrange,
                    onClick = onNavigateToAssets
                )
            }
            item {
                ModuleCard(
                    title = "Maintenance",
                    subtitle = "Reminders & schedules",
                    icon = Icons.Default.CalendarMonth,
                    glowColor = TitanColors.NeonYellow,
                    onClick = onNavigateToMaintenance
                )
            }
            item {
                ModuleCard(
                    title = "Warranty",
                    subtitle = "Invoices & guarantees",
                    icon = Icons.Default.Description,
                    glowColor = TitanColors.UltraViolet,
                    onClick = onNavigateToWarranty
                )
            }
            item {
                ModuleCard(
                    title = "Home",
                    subtitle = "Smart home control",
                    icon = Icons.Default.Home,
                    glowColor = TitanColors.ElectricPurple,
                    onClick = onNavigateToHomeHub
                )
            }
            item {
                ModuleCard(
                    title = "Cameras",
                    subtitle = "Camera management",
                    icon = Icons.Default.Videocam,
                    glowColor = TitanColors.ElectricBlue,
                    onClick = onNavigateToCameraHub
                )
            }
            item {
                ModuleCard(
                    title = "Find",
                    subtitle = "Locate objects",
                    icon = Icons.Default.LocationSearching,
                    glowColor = TitanColors.NeonRed,
                    onClick = onNavigateToFind
                )
            }
        }
    }
}

@Composable
fun ModuleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    glowColor: Color = TitanColors.NeonCyan,
    onClick: () -> Unit
) {
    NeonCard(
        glowColor = glowColor,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = glowColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TitanColors.GhostWhite
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TitanColors.GhostWhite.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun ToolsHubScreen(
    onNavigateToNetwork: () -> Unit,
    onNavigateToNoise: () -> Unit,
    onNavigateToVibration: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TitanColors.AbsoluteBlack)
            .padding(16.dp)
    ) {
        NeonTitle(text = "Tools", subtitle = "Diagnostic instruments")
        Spacer(modifier = Modifier.height(16.dp))

        NeonCard(glowColor = TitanColors.RadioactiveGreen, onClick = onNavigateToNetwork) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(32.dp), tint = TitanColors.RadioactiveGreen)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Network Doctor", fontWeight = FontWeight.Bold, color = TitanColors.GhostWhite)
                    Text("Diagnose Wi-Fi and Internet issues", color = TitanColors.GhostWhite.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        NeonCard(glowColor = TitanColors.NeonYellow, onClick = onNavigateToNoise) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(32.dp), tint = TitanColors.NeonYellow)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Noise Doctor", fontWeight = FontWeight.Bold, color = TitanColors.GhostWhite)
                    Text("Analyze sounds and diagnose issues", color = TitanColors.GhostWhite.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        NeonCard(glowColor = TitanColors.NeonOrange, onClick = onNavigateToVibration) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(32.dp), tint = TitanColors.NeonOrange)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Vibration Doctor", fontWeight = FontWeight.Bold, color = TitanColors.GhostWhite)
                    Text("Measure equipment vibration", color = TitanColors.GhostWhite.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }
        }
    }
}
