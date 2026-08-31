package com.supreme.android.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.supreme.android.ui.fix.FixScreen
import com.supreme.android.ui.home.AssetsScreen
import com.supreme.android.ui.home.AssetDetailScreen
import com.supreme.android.ui.home.HomeScreen
import com.supreme.android.ui.home.SettingsScreen
import com.supreme.android.ui.home.ToolsHubScreen
import com.supreme.android.ui.maintenance.MaintenanceScreen
import com.supreme.android.ui.network.NetworkScreen
import com.supreme.android.ui.noise.NoiseScreen
import com.supreme.android.ui.vibration.VibrationScreen
import com.supreme.android.ui.warranty.WarrantyScreen
import com.supreme.android.ui.homehub.HomeHubScreen
import com.supreme.android.ui.camerahub.CameraHubScreen
import com.supreme.android.ui.find.FindScreen

@Composable
fun SupremeNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier
    ) {
        composable("home") {
            HomeScreen(
                onNavigateToFix = { navController.navigate("fix") },
                onNavigateToTools = { navController.navigate("tools") },
                onNavigateToAssets = { navController.navigate("assets") },
                onNavigateToMaintenance = { navController.navigate("maintenance") },
                onNavigateToWarranty = { navController.navigate("warranty") },
                onNavigateToHomeHub = { navController.navigate("homehub") },
                onNavigateToCameraHub = { navController.navigate("camerahub") },
                onNavigateToFind = { navController.navigate("find") }
            )
        }

        composable("fix") {
            FixScreen(onBack = { navController.popBackStack() })
        }

        composable("tools") {
            ToolsHubScreen(
                onNavigateToNetwork = { navController.navigate("tools/network") },
                onNavigateToNoise = { navController.navigate("tools/noise") },
                onNavigateToVibration = { navController.navigate("tools/vibration") },
                onBack = { navController.popBackStack() }
            )
        }

        composable("tools/network") {
            NetworkScreen(onBack = { navController.popBackStack() })
        }

        composable("tools/noise") {
            NoiseScreen(onBack = { navController.popBackStack() })
        }

        composable("tools/vibration") {
            VibrationScreen(onBack = { navController.popBackStack() })
        }

        composable("assets") {
            AssetsScreen(
                onNavigateToAsset = { assetId -> navController.navigate("assets/$assetId") },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            "assets/{assetId}",
            arguments = listOf(navArgument("assetId") { type = NavType.StringType })
        ) { backStackEntry ->
            val assetId = backStackEntry.arguments?.getString("assetId") ?: ""
            AssetDetailScreen(assetId = assetId, onBack = { navController.popBackStack() })
        }

        composable("maintenance") {
            MaintenanceScreen(onBack = { navController.popBackStack() })
        }

        composable("warranty") {
            WarrantyScreen(onBack = { navController.popBackStack() })
        }

        composable("homehub") {
            HomeHubScreen(onBack = { navController.popBackStack() })
        }

        composable("camerahub") {
            CameraHubScreen(onBack = { navController.popBackStack() })
        }

        composable("find") {
            FindScreen(onBack = { navController.popBackStack() })
        }

        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
