package com.supreme.android.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
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
            .padding(16.dp)
    ) {
        Text(
            text = "Supreme",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Everyday Intelligence Platform",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    onClick = onNavigateToFix
                )
            }

            item {
                ModuleCard(
                    title = "Tools",
                    subtitle = "Network, Noise, Vibration",
                    icon = Icons.Default.Handyman,
                    onClick = onNavigateToTools
                )
            }

            item {
                ModuleCard(
                    title = "Assets",
                    subtitle = "Your possessions",
                    icon = Icons.Default.Inventory,
                    onClick = onNavigateToAssets
                )
            }

            item {
                ModuleCard(
                    title = "Maintenance",
                    subtitle = "Reminders & schedules",
                    icon = Icons.Default.CalendarMonth,
                    onClick = onNavigateToMaintenance
                )
            }

            item {
                ModuleCard(
                    title = "Warranty",
                    subtitle = "Invoices & guarantees",
                    icon = Icons.Default.Description,
                    onClick = onNavigateToWarranty
                )
            }

            item {
                ModuleCard(
                    title = "Home",
                    subtitle = "Smart home control",
                    icon = Icons.Default.Home,
                    onClick = onNavigateToHomeHub
                )
            }

            item {
                ModuleCard(
                    title = "Cameras",
                    subtitle = "Camera management",
                    icon = Icons.Default.Videocam,
                    onClick = onNavigateToCameraHub
                )
            }

            item {
                ModuleCard(
                    title = "Find",
                    subtitle = "Locate objects",
                    icon = Icons.Default.LocationSearching,
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
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
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
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
            .padding(16.dp)
    ) {
        Text(
            text = "Tools",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onNavigateToNetwork)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Network Doctor", fontWeight = FontWeight.Bold)
                    Text("Diagnose Wi-Fi and Internet issues")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onNavigateToNoise)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Noise Doctor", fontWeight = FontWeight.Bold)
                    Text("Analyze sounds and diagnose issues")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onNavigateToVibration)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Vibration Doctor", fontWeight = FontWeight.Bold)
                    Text("Measure equipment vibration")
                }
            }
        }
    }
}
