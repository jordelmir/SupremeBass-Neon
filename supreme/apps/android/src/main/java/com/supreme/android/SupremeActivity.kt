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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.supreme.android.navigation.SupremeNavGraph
import com.supreme.android.ui.theme.SupremeTheme
import com.supreme.android.viewmodel.SupremeViewModelFactory

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
        topBar = {
            TopAppBar(
                title = { Text("Supreme") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = currentRoute == "home",
                    onClick = { navController.navigate("home") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Build, contentDescription = "Fix") },
                    label = { Text("Fix") },
                    selected = currentRoute == "fix",
                    onClick = { navController.navigate("fix") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Handyman, contentDescription = "Tools") },
                    label = { Text("Tools") },
                    selected = currentRoute?.startsWith("tools") == true,
                    onClick = { navController.navigate("tools") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Inventory, contentDescription = "Assets") },
                    label = { Text("Assets") },
                    selected = currentRoute == "assets",
                    onClick = { navController.navigate("assets") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = currentRoute == "settings",
                    onClick = { navController.navigate("settings") }
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
