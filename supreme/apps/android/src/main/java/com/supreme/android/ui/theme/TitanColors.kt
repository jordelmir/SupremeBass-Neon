package com.supreme.android.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object TitanColors {
    val AbsoluteBlack = Color(0xFF000000)
    val CarbonGray = Color(0xFF080808)
    val DeepVoid = Color(0xFF020205)

    val NeonCyan = Color(0xFF00FFFF)
    val NeonRed = Color(0xFFFF073A)
    val RadioactiveGreen = Color(0xFF39FF14)
    val ElectricBlue = Color(0xFF007FFF)

    val NeonOrange = Color(0xFFFF3F00)
    val NeonYellow = Color(0xFFD4FF00)
    val UltraViolet = Color(0xFF6F00FF)
    val ElectricPurple = Color(0xFFBF00FF)
    val HolographicBlue = Color(0xFF0044FF)

    val PlasmaPurple = Color(0xFF9400D3)
    val QuantumPink = Color(0xFFB026FF)
    val AcidLime = Color(0xFFCCFF00)

    val NeonGreen = RadioactiveGreen
    val GhostWhite = Color(0xFFE0E0E0)

    val CyanToViolet = Brush.linearGradient(listOf(NeonCyan, UltraViolet))
    val PinkToOrange = Brush.linearGradient(listOf(QuantumPink, NeonOrange))
    val GreenToYellow = Brush.linearGradient(listOf(RadioactiveGreen, NeonYellow))
    val RedToViolet = Brush.linearGradient(listOf(NeonRed, PlasmaPurple))

    fun phosphorescentBrush(primary: Color, secondary: Color = primary.copy(alpha = 0.3f)): Brush {
        return Brush.radialGradient(listOf(primary, secondary, Color.Transparent))
    }
}
