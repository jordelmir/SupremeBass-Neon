package com.supremecorp.bass.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * SUPREME BASS — NEON GLASS EFFECTS SUITE
 * Ultra-futuristic modifiers with breathing, pulsing, scanning animations.
 */

fun Modifier.premiumGlass(
    cornerRadius: Dp = 16.dp,
    borderColor: Color = Color.White.copy(alpha = 0.2f),
    glassAlpha: Float = 0.3f,
    glowRadius: Dp = 0.dp
): Modifier = composed {
    val shape = RoundedCornerShape(cornerRadius)
    this
        .then(
            if (glowRadius > 0.dp) {
                Modifier.shadow(
                    elevation = glowRadius,
                    shape = shape,
                    spotColor = borderColor.copy(alpha = 0.5f),
                    ambientColor = borderColor.copy(alpha = 0.5f),
                    clip = false
                )
            } else Modifier
        )
        .clip(shape)
        .background(TitanColors.CarbonGray.copy(alpha = glassAlpha))
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    borderColor.copy(alpha = 0.5f),
                    Color.Transparent,
                    borderColor.copy(alpha = 0.2f)
                )
            ),
            shape = shape
        )
}

fun Modifier.reactorGlass(
    cornerRadius: Dp = 20.dp,
    glowColor: Color = TitanColors.NeonCyan,
    alpha: Float = 0.45f,
    glowRadius: Dp = 12.dp
): Modifier = composed {
    val shape = RoundedCornerShape(cornerRadius)
    val infiniteTransition = rememberInfiniteTransition(label = "reactor_breathe")

    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "reactor_scale"
    )

    val glowPulse by infiniteTransition.animateFloat(
        initialValue = glowRadius.value * 0.7f,
        targetValue = glowRadius.value * 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "reactor_glow"
    )

    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "reactor_border"
    )

    this
        .scale(breatheScale)
        .shadow(
            elevation = glowPulse.dp,
            shape = shape,
            spotColor = glowColor.copy(alpha = borderAlpha * 0.6f),
            ambientColor = glowColor.copy(alpha = borderAlpha * 0.3f),
            clip = false
        )
        .clip(shape)
        .background(TitanColors.AbsoluteBlack.copy(alpha = alpha))
        .border(
            width = 1.2.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    glowColor.copy(alpha = borderAlpha),
                    Color.Transparent,
                    glowColor.copy(alpha = borderAlpha * 0.5f)
                )
            ),
            shape = shape
        )
}

fun Modifier.neonGlass(
    cornerRadius: Dp = 16.dp,
    glowColor: Color = TitanColors.NeonCyan,
    strokeWidth: Dp = 1.dp
): Modifier = composed {
    val shape = RoundedCornerShape(cornerRadius)
    val infiniteTransition = rememberInfiniteTransition(label = "neon_glass")

    val glowIntensity by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "neon_intensity"
    )

    this
        .shadow(
            elevation = glowIntensity.dp,
            shape = shape,
            spotColor = glowColor.copy(alpha = 0.6f),
            ambientColor = glowColor.copy(alpha = 0.4f),
            clip = false
        )
        .clip(shape)
        .background(TitanColors.CarbonGray.copy(alpha = 0.6f))
        .border(
            width = strokeWidth,
            brush = Brush.verticalGradient(
                colors = listOf(
                    glowColor.copy(alpha = 0.8f),
                    Color.Transparent,
                    glowColor.copy(alpha = 0.3f)
                )
            ),
            shape = shape
        )
}

fun Modifier.pulsingNeonBorder(
    cornerRadius: Dp = 20.dp,
    glowColor: Color = TitanColors.NeonCyan,
    glassAlpha: Float = 0.2f,
    glowRadius: Dp = 14.dp
): Modifier = composed {
    val shape = RoundedCornerShape(cornerRadius)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_alpha"
    )

    val glowElevation by infiniteTransition.animateFloat(
        initialValue = glowRadius.value * 0.5f,
        targetValue = glowRadius.value * 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_elevation"
    )

    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_breathe"
    )

    this
        .scale(breatheScale)
        .shadow(
            elevation = glowElevation.dp,
            shape = shape,
            spotColor = glowColor.copy(alpha = borderAlpha * 0.6f),
            ambientColor = glowColor.copy(alpha = borderAlpha * 0.4f),
            clip = false
        )
        .clip(shape)
        .background(TitanColors.AbsoluteBlack.copy(alpha = glassAlpha))
        .border(
            width = 1.5.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    glowColor.copy(alpha = borderAlpha),
                    glowColor.copy(alpha = borderAlpha * 0.3f),
                    glowColor.copy(alpha = borderAlpha * 0.8f)
                )
            ),
            shape = shape
        )
}

fun Modifier.breathingGlow(
    glowColor: Color = TitanColors.NeonCyan,
    minScale: Float = 1f,
    maxScale: Float = 1.03f,
    duration: Int = 2500
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing_glow")

    val scale by infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg_scale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg_alpha"
    )

    this
        .scale(scale)
        .drawBehind {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = glowAlpha * 0.15f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.maxDimension * 0.8f
                )
            )
        }
}

fun Modifier.scanLineOverlay(
    lineColor: Color = TitanColors.NeonCyan,
    duration: Int = 3000
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "scanline")

    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_progress"
    )

    this.drawBehind {
        val y = scanProgress * size.height
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    lineColor.copy(alpha = 0.15f),
                    lineColor.copy(alpha = 0.3f),
                    lineColor.copy(alpha = 0.15f),
                    Color.Transparent
                )
            ),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 2f
        )
    }
}
