package com.supremecorp.bass

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class MatrixRainView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private data class MatrixDrop(
        var x: Float,
        var y: Float,
        var speed: Float,
        var length: Int,
        var chars: MutableList<Char>
    )

    private val paint = Paint().apply {
        textSize = 24f
        isAntiAlias = true
    }

    private val drops = mutableListOf<MatrixDrop>()
    private val matrixChars = "01アイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワヲン".toCharArray()
    private val maxDrops = 35
    
    // Cyberpunk color palette
    private val neonCyan = 0xFF00FFFF.toInt()
    private val neonMagenta = 0xFFFF00FF.toInt()
    private val neonGreen = 0xFF00FF41.toInt()

    init {
        setBackgroundColor(0x00000000) // Transparent
        initializeDrops()
    }

    private fun initializeDrops() {
        drops.clear()
        repeat(maxDrops) {
            addRandomDrop()
        }
    }

    private fun addRandomDrop() {
        val drop = MatrixDrop(
            x = Random.nextFloat() * width,
            y = -Random.nextFloat() * height,
            speed = Random.nextFloat() * 8f + 4f,
            length = Random.nextInt(8, 20),
            chars = MutableList(Random.nextInt(8, 20)) { matrixChars.random() }
        )
        drops.add(drop)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (width == 0 || height == 0) return

        // Draw each drop
        drops.forEach { drop ->
            drop.chars.forEachIndexed { index, char ->
                val y = drop.y + (index * 28f)
                
                // Calculate alpha based on position in trail
                val alpha = when {
                    index == 0 -> 255 // Head is brightest
                    index < 3 -> 200
                    index < 6 -> 150
                    else -> (100 - (index * 5)).coerceAtLeast(30)
                }

                // Alternate colors for cyberpunk effect
                val color = when (index % 3) {
                    0 -> neonCyan
                    1 -> neonGreen
                    else -> neonMagenta
                }

                paint.color = color
                paint.alpha = alpha

                // Add glow effect to the head
                if (index == 0) {
                    paint.setShadowLayer(15f, 0f, 0f, color)
                } else {
                    paint.clearShadowLayer()
                }

                canvas.drawText(char.toString(), drop.x, y, paint)
            }

            // Update drop position
            drop.y += drop.speed

            // Occasionally mutate characters for dynamic effect
            if (Random.nextFloat() > 0.95f) {
                val randomIndex = Random.nextInt(drop.chars.size)
                drop.chars[randomIndex] = matrixChars.random()
            }

            // Reset drop if it goes off screen
            if (drop.y > height + (drop.length * 28f)) {
                drop.x = Random.nextFloat() * width
                drop.y = -Random.nextFloat() * 200f
                drop.speed = Random.nextFloat() * 8f + 4f
                drop.length = Random.nextInt(8, 20)
                drop.chars = MutableList(Random.nextInt(8, 20)) { matrixChars.random() }
            }
        }

        // Ensure exactly maxDrops
        while (drops.size < maxDrops) {
            addRandomDrop()
        }

        invalidate() // Continuous animation
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (drops.isEmpty()) {
            initializeDrops()
        }
    }
}
