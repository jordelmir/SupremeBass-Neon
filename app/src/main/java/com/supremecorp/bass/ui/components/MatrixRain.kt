package com.supremecorp.bass.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.NativePaint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun MatrixRain(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF00FF41)
) {
    val characters = "01アイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワヲン♫♪☢⚡"
    val columnCount = 45
    val rowCount = 65

    val columnStates = remember {
        List(columnCount) {
            mutableStateOf(Random.nextInt(-rowCount, 0))
        }
    }

    // Each column gets a random color variation
    val columnColors = remember {
        List(columnCount) {
            when (Random.nextInt(4)) {
                0 -> color
                1 -> Color(0xFF00FFFF) // Cyan
                2 -> Color(0xFF39FF14) // Green
                else -> Color(0xFFBF00FF) // Purple
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(45)
            columnStates.forEach { state ->
                state.value = if (state.value > rowCount) -Random.nextInt(5, 25) else state.value + 1
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val charWidth = size.width / columnCount
        val charHeight = size.height / rowCount

        drawIntoCanvas { canvas ->
            columnStates.forEachIndexed { columnIndex, state ->
                val x = columnIndex * charWidth
                val colColor = columnColors[columnIndex]

                repeat(18) { i ->
                    val rowIndex = state.value - i
                    if (rowIndex in 0 until rowCount) {
                        val y = rowIndex * charHeight
                        val alpha = (1f - (i.toFloat() / 18f)).coerceIn(0f, 1f)

                        val paint = NativePaint().apply {
                            this.textSize = charWidth * 0.75f
                            this.isAntiAlias = true

                            if (i == 0) {
                                // Head character: bright white-ish glow
                                this.color = android.graphics.Color.argb(
                                    255,
                                    255,
                                    255,
                                    255
                                )
                                this.setShadowLayer(
                                    16f, 0f, 0f,
                                    android.graphics.Color.argb(
                                        220,
                                        (colColor.red * 255).toInt(),
                                        (colColor.green * 255).toInt(),
                                        (colColor.blue * 255).toInt()
                                    )
                                )
                            } else {
                                this.color = android.graphics.Color.argb(
                                    (alpha * 200).toInt(),
                                    (colColor.red * 255).toInt(),
                                    (colColor.green * 255).toInt(),
                                    (colColor.blue * 255).toInt()
                                )
                                if (i < 3) {
                                    this.setShadowLayer(
                                        8f, 0f, 0f,
                                        android.graphics.Color.argb(
                                            (alpha * 150).toInt(),
                                            (colColor.red * 255).toInt(),
                                            (colColor.green * 255).toInt(),
                                            (colColor.blue * 255).toInt()
                                        )
                                    )
                                }
                            }
                        }

                        val char = characters[Random.nextInt(characters.length)].toString()
                        canvas.nativeCanvas.drawText(char, x, y, paint)
                    }
                }
            }
        }
    }
}
