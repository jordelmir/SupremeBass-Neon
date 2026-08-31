package com.supreme.android.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.supreme.android.ui.fix.FixScreen
import com.supreme.android.ui.home.HomeScreen
import com.supreme.android.ui.maintenance.MaintenanceScreen
import com.supreme.android.ui.network.NetworkScreen
import com.supreme.android.ui.noise.NoiseScreen
import com.supreme.android.ui.vibration.VibrationScreen
import com.supreme.android.ui.warranty.WarrantyScreen

/**
 * Supreme Navigation Graph
 *
 * Routes:
 * - home: Dashboard with all modules
 * - fix: Fix AI diagnosis
 * - tools: Tools hub (network, noise, vibration)
 * - tools/network: Network Doctor
 * - tools/noise: Noise Doctor
 * - tools/vibration: Vibration Doctor
 * - assets: Asset list
 * - assets/{id}: Asset detail
 * - maintenance: Maintenance schedule
 * - warranty: Warranty Vault
 * - settings: Settings
 */
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
        // Home Dashboard
        composable("home") {
            HomeScreen(
                onNavigateToFix = { navController.navigate("fix") },
                onNavigateToTools = { navController.navigate("tools") },
                onNavigateToAssets = { navController.navigate("assets") },
                onNavigateToMaintenance = { navController.navigate("maintenance") },
                onNavigateToWarranty = { navController.navigate("warranty") }
            )
        }

        // Fix AI
        composable("fix") {
            FixScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Tools Hub
        composable("tools") {
            ToolsHubScreen(
                onNavigateToNetwork = { navController.navigate("tools/network") },
                onNavigateToNoise = { navController.navigate("tools/noise") },
                onNavigateToVibration = { navController.navigate("tools/vibration") },
                onBack = { navController.popBackStack() }
            )
        }

        // Network Doctor
        composable("tools/network") {
            NetworkScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Noise Doctor
        composable("tools/noise") {
            NoiseScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Vibration Doctor
        composable("tools/vibration") {
            VibrationScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Assets
        composable("assets") {
            AssetsScreen(
                onNavigateToAsset = { assetId ->
                    navController.navigate("assets/$assetId")
                },
                onBack = { navController.popBackStack() }
            )
        }

        // Asset Detail
        composable(
            "assets/{assetId}",
            arguments = listOf(navArgument("assetId") { type = NavType.StringType })
        ) { backStackEntry ->
            val assetId = backStackEntry.arguments?.getString("assetId") ?: ""
            AssetDetailScreen(
                assetId = assetId,
                onBack = { navController.popBackStack() }
            )
        }

        // Maintenance
        composable("maintenance") {
            MaintenanceScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Warranty Vault
        composable("warranty") {
            WarrantyScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Settings
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
