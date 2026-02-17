package com.supremecorp.bass.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.supremecorp.bass.ui.theme.TitanColors
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun AudioVisualizer(
    isActive: Boolean,
    gainLevel: Float,
    modifier: Modifier = Modifier,
    barCount: Int = 32,
    baseColor: Color = TitanColors.NeonCyan
) {
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val breathe by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    // Generate random offsets once
    val barSeeds = remember { List(barCount) { Random.nextFloat() * 3f } }

    val glowColor = when {
        gainLevel > 200f -> TitanColors.NeonRed
        gainLevel > 100f -> TitanColors.NeonOrange
        else -> baseColor
    }

    Canvas(modifier = modifier.fillMaxWidth().height(60.dp)) {
        val barWidth = size.width / (barCount * 1.8f)
        val spacing = size.width / barCount
        val maxHeight = size.height * 0.9f

        for (i in 0 until barCount) {
            val seed = barSeeds[i]
            val intensity = if (isActive) {
                val wave1 = sin((phase + seed * 1.5f).toDouble()).toFloat() * 0.4f
                val wave2 = sin((phase * 1.7f + seed * 2.3f).toDouble()).toFloat() * 0.3f
                val wave3 = sin((phase * 0.5f + seed).toDouble()).toFloat() * 0.2f
                val base = 0.15f + (gainLevel / 300f) * 0.4f
                (base + wave1 + wave2 + wave3).coerceIn(0.05f, 1f) * breathe
            } else {
                0.03f + sin((phase * 0.3f + seed).toDouble()).toFloat() * 0.02f
            }

            val barHeight = maxHeight * intensity
            val x = i * spacing + (spacing - barWidth) / 2
            val y = (size.height - barHeight) / 2

            // Bar gradient
            val topColor = glowColor
            val bottomColor = glowColor.copy(alpha = 0.2f)

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(topColor.copy(alpha = intensity * 0.9f), bottomColor),
                    startY = y,
                    endY = y + barHeight
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )

            // Glow overlay for bright bars
            if (intensity > 0.5f && isActive) {
                drawRoundRect(
                    color = glowColor.copy(alpha = (intensity - 0.5f) * 0.4f),
                    topLeft = Offset(x - 1, y - 1),
                    size = Size(barWidth + 2, barHeight + 2),
                    cornerRadius = CornerRadius(barWidth / 2 + 1, barWidth / 2 + 1)
                )
            }
        }
    }
}
