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
                    Text(
                        "SUPREME",
                        color = TitanColors.NeonCyan
                    )
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
                    label = { Text("Home", color = if (currentRoute == "home") TitanColors.NeonCyan else TitanColors.GhostWhite.copy(alpha = 0.5f)) },
                    selected = currentRoute == "home",
                    onClick = { navController.navigate("home") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = TitanColors.NeonCyan.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Build, contentDescription = "Fix", tint = if (currentRoute == "fix") TitanColors.NeonCyan else TitanColors.GhostWhite.copy(alpha = 0.5f)) },
                    label = { Text("Fix", color = if (currentRoute == "fix") TitanColors.NeonCyan else TitanColors.GhostWhite.copy(alpha = 0.5f)) },
                    selected = currentRoute == "fix",
                    onClick = { navController.navigate("fix") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = TitanColors.NeonCyan.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Handyman, contentDescription = "Tools", tint = if (currentRoute?.startsWith("tools") == true) TitanColors.NeonCyan else TitanColors.GhostWhite.copy(alpha = 0.5f)) },
                    label = { Text("Tools", color = if (currentRoute?.startsWith("tools") == true) TitanColors.NeonCyan else TitanColors.GhostWhite.copy(alpha = 0.5f)) },
                    selected = currentRoute?.startsWith("tools") == true,
                    onClick = { navController.navigate("tools") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = TitanColors.NeonCyan.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Inventory, contentDescription = "Assets", tint = if (currentRoute == "assets") TitanColors.NeonCyan else TitanColors.GhostWhite.copy(alpha = 0.5f)) },
                    label = { Text("Assets", color = if (currentRoute == "assets") TitanColors.NeonCyan else TitanColors.GhostWhite.copy(alpha = 0.5f)) },
                    selected = currentRoute == "assets",
                    onClick = { navController.navigate("assets") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = TitanColors.NeonCyan.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings", tint = if (currentRoute == "settings") TitanColors.NeonCyan else TitanColors.GhostWhite.copy(alpha = 0.5f)) },
                    label = { Text("Settings", color = if (currentRoute == "settings") TitanColors.NeonCyan else TitanColors.GhostWhite.copy(alpha = 0.5f)) },
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
