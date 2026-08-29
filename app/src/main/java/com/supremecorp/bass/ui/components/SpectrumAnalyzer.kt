package com.supremecorp.bass.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supremecorp.bass.ui.theme.TitanColors

/**
 * Real-time spectrum analyzer visualization.
 * Shows frequency spectrum from FFT data.
 */
@Composable
fun SpectrumAnalyzer(
    magnitudes: List<Double>,
    sampleRate: Int,
    fftSize: Int,
    modifier: Modifier = Modifier,
    peakHold: Boolean = true,
    showGrid: Boolean = true,
    showLabels: Boolean = true
) {
    val textMeasurer = rememberTextMeasurer()

    // Peak hold state
    var peakValues by remember { mutableStateOf(List(magnitudes.size) { -200.0 }) }
    var peakDecay by remember { mutableStateOf(System.currentTimeMillis()) }

    // Update peak hold
    LaunchedEffect(magnitudes) {
        val now = System.currentTimeMillis()
        val decayMs = 1000L // 1 second peak hold

        peakValues = magnitudes.mapIndexed { i, mag ->
            val current = if (peakHold) peakValues.getOrElse(i) { -200.0 } else -200.0
            val newPeak = maxOf(current, mag)

            // Decay old peaks
            if (now - peakDecay > decayMs && newPeak > mag) {
                maxOf(mag, newPeak - 3.0) // Decay 3dB per second
            } else {
                newPeak
            }
        }
        peakDecay = now
    }

    Box(
        modifier = modifier
            .background(Color(0xFF0A0A0A), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val padding = 40f

            val plotWidth = width - padding * 2
            val plotHeight = height - padding * 2

            if (magnitudes.isEmpty()) return@Canvas

            // Draw grid
            if (showGrid) {
                // Frequency grid (log scale)
                val freqLabels = listOf(20.0, 50.0, 100.0, 200.0, 500.0, 1000.0, 2000.0, 5000.0, 10000.0, 20000.0)
                for (freq in freqLabels) {
                    val x = padding + (kotlin.math.log10(freq / 20.0) / kotlin.math.log10(20000.0 / 20.0) * plotWidth).toFloat()
                    if (x in padding..width - padding) {
                        drawLine(
                            color = Color(0xFF1A1A1A),
                            start = Offset(x, padding),
                            end = Offset(x, height - padding),
                            strokeWidth = 1f
                        )
                    }
                }

                // dB grid
                for (db in -80..0 step 20) {
                    val y = padding + plotHeight * (1.0 - (db + 80.0) / 80.0).toFloat()
                    drawLine(
                        color = Color(0xFF1A1A1A),
                        start = Offset(padding, y),
                        end = Offset(width - padding, y),
                        strokeWidth = 1f
                    )
                }
            }

            // Draw spectrum
            val path = Path()
            val gradientPath = Path()

            for (i in magnitudes.indices) {
                val freq = (i * sampleRate.toDouble() / fftSize)
                if (freq < 20.0 || freq > 20000.0) continue

                val x = padding + (kotlin.math.log10(freq / 20.0) / kotlin.math.log10(20000.0 / 20.0) * plotWidth).toFloat()
                val db = magnitudes[i].coerceIn(-80.0, 0.0)
                val y = padding + plotHeight * (1.0 - (db + 80.0) / 80.0).toFloat()

                if (i == 0 || path.isEmpty) {
                    path.moveTo(x, y)
                    gradientPath.moveTo(x, height - padding)
                    gradientPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    gradientPath.lineTo(x, y)
                }
            }

            // Close gradient path
            gradientPath.lineTo(width - padding, height - padding)
            gradientPath.close()

            // Draw gradient fill
            drawPath(
                path = gradientPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        TitanColors.NeonCyan.copy(alpha = 0.4f),
                        TitanColors.NeonCyan.copy(alpha = 0.0f)
                    ),
                    startY = padding,
                    endY = height - padding
                )
            )

            // Draw spectrum line
            drawPath(
                path = path,
                color = TitanColors.NeonCyan,
                style = Stroke(width = 2f)
            )

            // Draw peak hold
            if (peakHold) {
                val peakPath = Path()
                for (i in peakValues.indices) {
                    val freq = (i * sampleRate.toDouble() / fftSize)
                    if (freq < 20.0 || freq > 20000.0) continue

                    val x = padding + (kotlin.math.log10(freq / 20.0) / kotlin.math.log10(20000.0 / 20.0) * plotWidth).toFloat()
                    val db = peakValues[i].coerceIn(-80.0, 0.0)
                    val y = padding + plotHeight * (1.0 - (db + 80.0) / 80.0).toFloat()

                    if (peakPath.isEmpty) {
                        peakPath.moveTo(x, y)
                    } else {
                        peakPath.lineTo(x, y)
                    }
                }

                drawPath(
                    path = peakPath,
                    color = Color.Yellow,
                    style = Stroke(width = 1f)
                )
            }

            // Draw frequency labels
            if (showLabels) {
                val textStyle = TextStyle(
                    fontSize = 10.sp,
                    color = Color.Gray
                )

                for (freq in listOf(20.0, 100.0, 1000.0, 10000.0, 20000.0)) {
                    val x = padding + (kotlin.math.log10(freq / 20.0) / kotlin.math.log10(20000.0 / 20.0) * plotWidth).toFloat()
                    val label = when {
                        freq >= 1000 -> "${(freq / 1000).toInt()}k"
                        else -> "${freq.toInt()}"
                    }
                    drawText(
                        textMeasurer = textMeasurer,
                        text = label,
                        topLeft = Offset(x - 10f, height - padding + 5f),
                        style = textStyle
                    )
                }
            }
        }
    }
}
