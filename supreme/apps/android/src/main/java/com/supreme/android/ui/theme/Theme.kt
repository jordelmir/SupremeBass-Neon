package com.supreme.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NeonDarkScheme = darkColorScheme(
    primary = TitanColors.NeonCyan,
    onPrimary = TitanColors.AbsoluteBlack,
    primaryContainer = TitanColors.ElectricBlue.copy(alpha = 0.3f),
    onPrimaryContainer = TitanColors.NeonCyan,
    secondary = TitanColors.RadioactiveGreen,
    onSecondary = TitanColors.AbsoluteBlack,
    secondaryContainer = TitanColors.RadioactiveGreen.copy(alpha = 0.15f),
    onSecondaryContainer = TitanColors.RadioactiveGreen,
    tertiary = TitanColors.NeonRed,
    onTertiary = TitanColors.AbsoluteBlack,
    tertiaryContainer = TitanColors.NeonRed.copy(alpha = 0.15f),
    onTertiaryContainer = TitanColors.NeonRed,
    error = TitanColors.NeonRed,
    onError = TitanColors.AbsoluteBlack,
    errorContainer = TitanColors.NeonRed.copy(alpha = 0.15f),
    onErrorContainer = TitanColors.NeonRed,
    background = TitanColors.AbsoluteBlack,
    onBackground = TitanColors.GhostWhite,
    surface = TitanColors.CarbonGray,
    onSurface = TitanColors.GhostWhite,
    surfaceVariant = TitanColors.CarbonGray.copy(alpha = 0.8f),
    onSurfaceVariant = TitanColors.GhostWhite.copy(alpha = 0.7f),
    outline = TitanColors.NeonCyan.copy(alpha = 0.3f),
    surfaceTint = TitanColors.NeonCyan
)

@Composable
fun SupremeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NeonDarkScheme,
        typography = SupremeTypography,
        content = content
    )
}
