package com.supreme.android.ui.find

import android.Manifest
import android.os.Build
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
import com.supreme.android.permissions.RequestMultiplePermissionsEffect
import com.supreme.android.ui.theme.*

data class TrackedObject(val name: String, val lastSeen: String, val signal: String)

@Composable
fun FindScreen(onBack: () -> Unit) {
    val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_FINE_LOCATION)
    }

    var hasPermissions by remember { mutableStateOf(false) }

    RequestMultiplePermissionsEffect(
        permissions = bluetoothPermissions,
        onAllGranted = { hasPermissions = true }
    )

    val objects = listOf(
        TrackedObject("Keys", "2 min ago", "Strong"),
        TrackedObject("Backpack", "15 min ago", "Medium"),
        TrackedObject("Wallet", "1 hour ago", "Weak")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TitanColors.AbsoluteBlack)
            .padding(16.dp)
    ) {
        NeonTitle(text = "Find", subtitle = "Locate your objects")
        Spacer(modifier = Modifier.height(24.dp))

        if (!hasPermissions) {
            NeonCard(glowColor = TitanColors.NeonRed) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Permission Required", fontWeight = FontWeight.Bold, color = TitanColors.NeonRed)
                    Text("Bluetooth permission is needed to scan for objects", color = TitanColors.GhostWhite.copy(alpha = 0.6f))
                }
            }
            return
        }

        NeonButton(
            onClick = { /* TODO: Scan for BLE tags */ },
            text = "Scan for Objects",
            icon = Icons.Default.Search,
            modifier = Modifier.fillMaxWidth(),
            color = TitanColors.NeonRed
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(objects) { obj ->
                NeonCard(glowColor = TitanColors.NeonRed) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = TitanColors.NeonRed)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(obj.name, fontWeight = FontWeight.Bold, color = TitanColors.GhostWhite)
                            Text("Last seen: ${obj.lastSeen}", color = TitanColors.GhostWhite.copy(alpha = 0.6f))
                        }
                        Text(obj.signal, color = TitanColors.GhostWhite.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}
