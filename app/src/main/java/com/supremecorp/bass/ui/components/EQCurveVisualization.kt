package com.supremecorp.bass.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.supremecorp.bass.ui.theme.TitanColors

/**
 * Visualizes the current EQ curve.
 * Shows frequency response modification from EQ settings.
 */
@Composable
fun EQCurveVisualization(
    eqGains: List<Double>, // 10 band gains in dB
    modifier: Modifier = Modifier
) {
    val bandFrequencies = listOf(31.0, 62.0, 125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0)

    Box(
        modifier = modifier
            .background(Color(0xFF0A0A0A), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val padding = 30f

            val plotWidth = width - padding * 2
            val plotHeight = height - padding * 2

            // Draw center line (0dB)
            val centerY = padding + plotHeight / 2
            drawLine(
                color = Color(0xFF2A2A2A),
                start = Offset(padding, centerY),
                end = Offset(width - padding, centerY),
                strokeWidth = 1f
            )

            // Draw grid lines for dB
            for (db in listOf(-12.0, -6.0, 0.0, 6.0, 12.0)) {
                val y = padding + plotHeight * (1.0 - (db + 12.0) / 24.0).toFloat()
                drawLine(
                    color = Color(0xFF1A1A1A),
                    start = Offset(padding, y),
                    end = Offset(width - padding, y),
                    strokeWidth = 1f
                )
            }

            if (eqGains.isEmpty()) return@Canvas

            // Draw the EQ curve
            val path = Path()
            val fillPath = Path()

            for (i in eqGains.indices) {
                val freq = bandFrequencies[i]
                val gain = eqGains[i].coerceIn(-12.0, 12.0)

                val x = padding + (i.toFloat() / (eqGains.size - 1)) * plotWidth
                val y = padding + plotHeight * (1.0 - (gain + 12.0) / 24.0).toFloat()

                if (i == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, centerY)
                    fillPath.lineTo(x, y)
                } else {
                    // Smooth curve using quadratic bezier
                    val prevX = padding + ((i - 1).toFloat() / (eqGains.size - 1)) * plotWidth
                    val midX = (prevX + x) / 2
                    path.quadraticBezierTo(midX, y, x, y)
                    fillPath.lineTo(x, y)
                }
            }

            // Close fill path
            fillPath.lineTo(width - padding, centerY)
            fillPath.close()

            // Draw gradient fill
            val gradient = Brush.verticalGradient(
                colors = listOf(
                    TitanColors.NeonCyan.copy(alpha = 0.3f),
                    Color.Transparent,
                    Color(0xFFFF00FF).copy(alpha = 0.3f)
                ),
                startY = padding,
                endY = height - padding
            )
            drawPath(
                path = fillPath,
                brush = gradient
            )

            // Draw the EQ curve
            drawPath(
                path = path,
                color = TitanColors.NeonCyan,
                style = Stroke(width = 2f)
            )

            // Draw control points
            for (i in eqGains.indices) {
                val freq = bandFrequencies[i]
                val gain = eqGains[i].coerceIn(-12.0, 12.0)

                val x = padding + (i.toFloat() / (eqGains.size - 1)) * plotWidth
                val y = padding + plotHeight * (1.0 - (gain + 12.0) / 24.0).toFloat()

                // Point
                drawCircle(
                    color = if (kotlin.math.abs(gain) > 0.5) TitanColors.NeonCyan else Color.Gray,
                    radius = 4f,
                    center = Offset(x, y)
                )

                // Glow effect for boosted/cut bands
                if (kotlin.math.abs(gain) > 3.0) {
                    drawCircle(
                        color = TitanColors.NeonCyan.copy(alpha = 0.3f),
                        radius = 8f,
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}
