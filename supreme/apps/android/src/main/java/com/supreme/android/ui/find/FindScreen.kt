package com.supreme.android.ui.find

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

data class TrackedObject(
    val name: String,
    val lastSeen: String,
    val signal: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindScreen(onBack: () -> Unit) {
    val objects = listOf(
        TrackedObject("Keys", "2 min ago", "Strong"),
        TrackedObject("Backpack", "15 min ago", "Medium"),
        TrackedObject("Wallet", "1 hour ago", "Weak")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Find", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Locate your objects", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = { /* TODO: Scan for BLE tags */ }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Search, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Scan for Objects")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(objects) { obj ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(obj.name, fontWeight = FontWeight.Bold)
                            Text("Last seen: ${obj.lastSeen}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(obj.signal, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
