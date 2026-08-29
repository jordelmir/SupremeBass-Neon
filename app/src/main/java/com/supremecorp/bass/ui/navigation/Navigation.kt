package com.supremecorp.bass.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supremecorp.bass.ui.theme.TitanColors

enum class Screen(val label: String, val icon: ImageVector) {
    ENHANCE("Enhance", Icons.Default.VolumeUp),
    SIGNAL_LAB("Signal Lab", Icons.Default.GraphicEq),
    DEVICE_LAB("Device Lab", Icons.Default.Memory),
    EXPERIMENT_LAB("Experiments", Icons.Default.Science),
    FLAME_LAB("Flame Lab", Icons.Default.LocalFireDepartment),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun SupremeAcousticsNavHost(
    enhanceContent: @Composable () -> Unit,
    signalLabContent: @Composable () -> Unit,
    deviceLabContent: @Composable () -> Unit,
    experimentLabContent: @Composable () -> Unit,
    flameLabContent: @Composable () -> Unit,
    settingsContent: @Composable () -> Unit
) {
    var currentScreen by remember { mutableStateOf(Screen.ENHANCE) }

    Column(modifier = Modifier.fillMaxSize().background(TitanColors.AbsoluteBlack)) {
        Box(modifier = Modifier.weight(1f)) {
            when (currentScreen) {
                Screen.ENHANCE -> enhanceContent()
                Screen.SIGNAL_LAB -> signalLabContent()
                Screen.DEVICE_LAB -> deviceLabContent()
                Screen.EXPERIMENT_LAB -> experimentLabContent()
                Screen.FLAME_LAB -> flameLabContent()
                Screen.SETTINGS -> settingsContent()
            }
        }

        // Bottom navigation bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0A0A0A))
                .border(1.dp, Color(0xFF1A1A1A))
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Screen.entries.forEach { screen ->
                val isSelected = currentScreen == screen
                val color = if (isSelected) TitanColors.NeonCyan else Color.Gray

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { currentScreen = screen }
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Icon(
                        screen.icon,
                        contentDescription = screen.label,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = screen.label,
                        style = TextStyle(
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = color,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }
        }
    }
}
