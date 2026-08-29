package com.supremecorp.bass.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supremecorp.bass.signal.SignalEngineState
import com.supremecorp.bass.ui.theme.breathingGlow
import com.supremecorp.bass.ui.theme.neonGlass
import com.supremecorp.bass.ui.theme.premiumGlass
import com.supremecorp.bass.ui.theme.TitanColors

@Composable
fun NeonScreenTitle(title: String, subtitle: String? = null, accentColor: Color = TitanColors.NeonCyan) {
    Text(
        text = title,
        style = TextStyle(
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = accentColor,
            letterSpacing = 4.sp,
            shadow = Shadow(
                color = accentColor.copy(alpha = 0.6f),
                offset = Offset.Zero,
                blurRadius = 12f
            )
        )
    )
    if (subtitle != null) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = TextStyle(
                fontSize = 9.sp,
                color = accentColor.copy(alpha = 0.5f),
                letterSpacing = 1.sp
            )
        )
    }
}

@Composable
fun NeonSectionHeader(text: String, accentColor: Color = TitanColors.NeonCyan) {
    Text(
        text = text,
        style = TextStyle(
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = accentColor.copy(alpha = 0.7f),
            shadow = Shadow(
                color = accentColor.copy(alpha = 0.3f),
                offset = Offset.Zero,
                blurRadius = 8f
            )
        )
    )
}

@Composable
fun NeonCard(
    modifier: Modifier = Modifier,
    glowColor: Color = TitanColors.NeonCyan,
    useReactorGlow: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .then(
                if (useReactorGlow) {
                    Modifier.neonGlass(cornerRadius = 16.dp, glowColor = glowColor)
                } else {
                    Modifier.premiumGlass(cornerRadius = 16.dp, borderColor = glowColor.copy(alpha = 0.3f), glassAlpha = 0.4f, glowRadius = 8.dp)
                }
            )
            .padding(16.dp),
        content = content
    )
}

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = TitanColors.NeonCyan,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp),
        enabled = enabled
    ) {
        icon?.invoke()
        if (icon != null) Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            color = if (enabled) TitanColors.AbsoluteBlack else Color.Gray,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun NeonOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = TitanColors.NeonCyan,
    icon: @Composable (() -> Unit)? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        shape = RoundedCornerShape(12.dp),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.linearGradient(
                colors = listOf(color.copy(alpha = 0.5f), color.copy(alpha = 0.2f))
            )
        )
    ) {
        icon?.invoke()
        if (icon != null) Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
fun NeonSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    color: Color = TitanColors.NeonCyan
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        modifier = modifier,
        colors = SliderDefaults.colors(
            thumbColor = color,
            activeTrackColor = color,
            inactiveTrackColor = TitanColors.CarbonGray
        )
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = TextStyle(
                fontSize = 11.sp,
                color = Color.Gray.copy(alpha = 0.8f),
                fontWeight = FontWeight.Normal
            )
        )
        Text(
            value,
            style = TextStyle(
                fontSize = 11.sp,
                color = TitanColors.NeonCyan,
                fontWeight = FontWeight.Medium,
                shadow = Shadow(
                    color = TitanColors.NeonCyan.copy(alpha = 0.2f),
                    offset = Offset.Zero,
                    blurRadius = 4f
                )
            )
        )
    }
}

@Composable
fun EngineStateIndicator(state: SignalEngineState) {
    val color = when (state) {
        is SignalEngineState.Idle -> Color.Gray
        is SignalEngineState.Preparing -> TitanColors.NeonYellow
        is SignalEngineState.Running -> TitanColors.RadioactiveGreen
        is SignalEngineState.Stopping -> TitanColors.NeonOrange
        is SignalEngineState.Failed -> Color(0xFFFF1744)
    }
    val text = when (state) {
        is SignalEngineState.Idle -> "IDLE"
        is SignalEngineState.Preparing -> "PREPARING"
        is SignalEngineState.Running -> "RUNNING"
        is SignalEngineState.Stopping -> "STOPPING"
        is SignalEngineState.Failed -> "FAILED: ${state.error.description()}"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "engine_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = color.copy(alpha = pulseAlpha),
                ambientColor = color.copy(alpha = pulseAlpha * 0.5f)
            )
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.15f),
                        color.copy(alpha = 0.05f),
                        color.copy(alpha = 0.15f)
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.5f),
                        color.copy(alpha = 0.2f),
                        color.copy(alpha = 0.5f)
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
                .shadow(4.dp, CircleShape, spotColor = color.copy(alpha = 0.5f))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text,
            style = TextStyle(
                fontSize = 11.sp,
                color = color,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                shadow = Shadow(
                    color = color.copy(alpha = 0.4f),
                    offset = Offset.Zero,
                    blurRadius = 6f
                )
            )
        )
    }
}

@Composable
fun NeonChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    selectedColor: Color = TitanColors.NeonCyan
) {
    Surface(
        onClick = onClick,
        color = if (selected) selectedColor.copy(alpha = 0.15f) else Color(0xFF0D0D0D),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .border(
                width = if (selected) 1.5.dp else 0.5.dp,
                color = if (selected) selectedColor.copy(alpha = 0.7f) else Color.Gray.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            )
            .then(
                if (selected) {
                    Modifier.shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(8.dp),
                        spotColor = selectedColor.copy(alpha = 0.3f),
                        ambientColor = selectedColor.copy(alpha = 0.15f)
                    )
                } else Modifier
            )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = TextStyle(
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) selectedColor else Color.Gray,
                letterSpacing = 1.sp,
                shadow = if (selected) Shadow(
                    color = selectedColor.copy(alpha = 0.3f),
                    offset = Offset.Zero,
                    blurRadius = 4f
                ) else null
            )
        )
    }
}
