package com.supreme.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.supreme.android.navigation.SupremeNavGraph
import com.supreme.android.ui.theme.*

class SupremeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SupremeTheme {
                SupremeApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupremeApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        containerColor = TitanColors.AbsoluteBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text("SUPREMEBASS", color = TitanColors.NeonCyan)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TitanColors.AbsoluteBlack
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = TitanColors.CarbonGray,
                contentColor = TitanColors.NeonCyan
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home", tint = if (currentRoute == "home") TitanColors.NeonCyan else TitanColors.GhostWhite.copy(alpha = 0.5f)) },
                    label = { Text("Home") },
                    selected = currentRoute == "home",
                    onClick = { navController.navigate("home") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = TitanColors.NeonCyan.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Equalizer, contentDescription = "Signal", tint = if (currentRoute == "signal") TitanColors.NeonCyan else TitanColors.GhostWhite.copy(alpha = 0.5f)) },
                    label = { Text("Signal") },
                    selected = currentRoute == "signal",
                    onClick = { navController.navigate("signal") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = TitanColors.NeonCyan.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Build, contentDescription = "Fix", tint = if (currentRoute == "fix") TitanColors.NeonCyan else TitanColors.GhostWhite.copy(alpha = 0.5f)) },
                    label = { Text("Fix") },
                    selected = currentRoute == "fix",
                    onClick = { navController.navigate("fix") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = TitanColors.NeonCyan.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Devices, contentDescription = "Device", tint = if (currentRoute == "device") TitanColors.NeonCyan else TitanColors.GhostWhite.copy(alpha = 0.5f)) },
                    label = { Text("Device") },
                    selected = currentRoute == "device",
                    onClick = { navController.navigate("device") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = TitanColors.NeonCyan.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Science, contentDescription = "Lab", tint = if (currentRoute?.startsWith("experiment") == true || currentRoute == "flame" || currentRoute == "visual") TitanColors.NeonCyan else TitanColors.GhostWhite.copy(alpha = 0.5f)) },
                    label = { Text("Lab") },
                    selected = currentRoute?.startsWith("experiment") == true || currentRoute == "flame" || currentRoute == "visual",
                    onClick = { navController.navigate("experiment") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = TitanColors.NeonCyan.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings", tint = if (currentRoute == "settings") TitanColors.NeonCyan else TitanColors.GhostWhite.copy(alpha = 0.5f)) },
                    label = { Text("Settings") },
                    selected = currentRoute == "settings",
                    onClick = { navController.navigate("settings") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = TitanColors.NeonCyan.copy(alpha = 0.1f)
                    )
                )
            }
        }
    ) { paddingValues ->
        SupremeNavGraph(
            navController = navController,
            modifier = Modifier.padding(paddingValues)
        )
    }
}
