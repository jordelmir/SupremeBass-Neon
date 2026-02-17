package com.supremecorp.bass.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supremecorp.bass.ui.theme.TitanColors

@Composable
fun NeonSwitch(
    isEnabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "switch_pulse")

    val activeColor by animateColorAsState(
        targetValue = if (isEnabled) TitanColors.RadioactiveGreen else Color(0xFFFF1744),
        animationSpec = tween(600),
        label = "switch_color"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "switch_glow"
    )

    val thumbOffset by animateFloatAsState(
        targetValue = if (isEnabled) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "thumb_offset"
    )

    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isEnabled) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status label
        Text(
            text = if (isEnabled) "🔊 MOTOR DE SONIDO" else "🔇 MOTOR DE SONIDO",
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = activeColor.copy(alpha = glowAlpha * 0.5f + 0.5f),
                shadow = Shadow(
                    color = activeColor.copy(alpha = glowAlpha * 0.8f),
                    offset = Offset.Zero,
                    blurRadius = 16f
                )
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // The switch track
        Box(
            modifier = Modifier
                .width(180.dp)
                .height(50.dp)
                .scale(breatheScale)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(25.dp),
                    spotColor = activeColor.copy(alpha = glowAlpha * 0.6f),
                    ambientColor = activeColor.copy(alpha = glowAlpha * 0.3f),
                )
                .clip(RoundedCornerShape(25.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = if (isEnabled) {
                            listOf(Color(0xFF002200), Color(0xFF003300), Color(0xFF002200))
                        } else {
                            listOf(Color(0xFF220000), Color(0xFF110000), Color(0xFF220000))
                        }
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            activeColor.copy(alpha = glowAlpha * 0.8f),
                            activeColor.copy(alpha = 0.2f),
                            activeColor.copy(alpha = glowAlpha * 0.8f)
                        )
                    ),
                    shape = RoundedCornerShape(25.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggle
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            // Track label
            Text(
                text = if (isEnabled) "ENCENDIDO" else "APAGADO",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = if (isEnabled) 16.dp else 60.dp,
                        end = if (isEnabled) 60.dp else 16.dp
                    ),
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 3.sp,
                    color = activeColor.copy(alpha = 0.9f),
                    shadow = Shadow(
                        color = activeColor.copy(alpha = 0.7f),
                        offset = Offset.Zero,
                        blurRadius = 8f
                    )
                )
            )

            // Animated thumb
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .offset(x = (thumbOffset * 130).dp)
                    .size(42.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = CircleShape,
                        spotColor = activeColor.copy(alpha = glowAlpha),
                        ambientColor = activeColor.copy(alpha = glowAlpha * 0.5f)
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                activeColor,
                                activeColor.copy(alpha = 0.7f),
                                Color(0xFF111111)
                            )
                        )
                    )
                    .border(1.dp, activeColor.copy(alpha = glowAlpha), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = if (isEnabled) "Apagar Motor" else "Encender Motor",
                    tint = if (isEnabled) Color.White else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Small pulsing status indicator
        Box(
            modifier = Modifier
                .size(8.dp)
                .alpha(if (isEnabled) glowAlpha else 0.3f)
                .shadow(4.dp, CircleShape, spotColor = activeColor, ambientColor = activeColor)
                .clip(CircleShape)
                .background(activeColor)
        )
    }
}
