package com.supremecorp.bass.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun BreathingText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Cyan,
    fontSize: TextUnit = 32.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    letterSpacing: TextUnit = 4.sp,
    breatheScale: Float = 0.03f,
    breatheDuration: Int = 2500,
    style: TextStyle = TextStyle.Default
) {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing_text")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1f + breatheScale,
        animationSpec = infiniteRepeatable(
            animation = tween(breatheDuration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "text_scale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(breatheDuration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "text_glow"
    )

    val glowRadius by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(breatheDuration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_radius"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Outer glow layer
        Text(
            text = text,
            style = style.copy(
                fontSize = fontSize,
                fontWeight = fontWeight,
                letterSpacing = letterSpacing,
                color = color.copy(alpha = glowAlpha * 0.3f),
                shadow = Shadow(
                    color = color.copy(alpha = glowAlpha * 0.6f),
                    offset = Offset.Zero,
                    blurRadius = glowRadius * 2
                )
            ),
            modifier = Modifier.scale(scale * 1.01f).alpha(glowAlpha * 0.4f)
        )
        // Main text
        Text(
            text = text,
            style = style.copy(
                fontSize = fontSize,
                fontWeight = fontWeight,
                letterSpacing = letterSpacing,
                color = color.copy(alpha = glowAlpha * 0.6f + 0.4f),
                shadow = Shadow(
                    color = color.copy(alpha = glowAlpha),
                    offset = Offset.Zero,
                    blurRadius = glowRadius
                )
            ),
            modifier = Modifier.scale(scale)
        )
    }
}
