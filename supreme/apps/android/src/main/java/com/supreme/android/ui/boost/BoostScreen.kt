package com.supreme.android.ui.boost

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supreme.android.AudioService
import com.supreme.android.AudioStatePersistence
import com.supreme.android.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun BoostScreen() {
    val context = LocalContext.current
    var isEnabled by remember { mutableStateOf(AudioStatePersistence.isEnabled(context)) }
    var gainValue by remember { mutableFloatStateOf(AudioStatePersistence.gainValue(context)) }
    var showDisclaimer by remember { mutableStateOf(!AudioStatePersistence.hasAcceptedDisclaimer(context)) }
    var shutoffMinutesRemaining by remember { mutableIntStateOf(0) }

    val totalVolume = 100 + gainValue.toInt()

    // Auto-shutoff timer (30 minutes max)
    LaunchedEffect(isEnabled) {
        if (isEnabled) {
            shutoffMinutesRemaining = 30
            while (shutoffMinutesRemaining > 0 && isEnabled) {
                delay(60_000L)
                shutoffMinutesRemaining--
            }
            if (shutoffMinutesRemaining <= 0 && isEnabled) {
                isEnabled = false
                AudioStatePersistence.saveEnabled(context, false)
                context.stopService(Intent(context, AudioService::class.java))
            }
        } else {
            shutoffMinutesRemaining = 0
        }
    }

    // Sync service with UI state
    LaunchedEffect(isEnabled, gainValue) {
        AudioStatePersistence.saveEnabled(context, isEnabled)
        AudioStatePersistence.saveGain(context, gainValue)

        if (isEnabled) {
            val intent = Intent(context, AudioService::class.java).apply {
                putExtra("GAIN", gainValue.toInt())
            }
            context.startService(intent)
        } else {
            context.stopService(Intent(context, AudioService::class.java))
        }
    }

    // First-run disclaimer
    if (showDisclaimer) {
        AlertDialog(
            onDismissRequest = { },
            containerColor = Color(0xFF1A1A1A),
            title = { Text("Safety Warning", color = TitanColors.NeonRed, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("This app boosts system audio output. High gain levels can cause:", fontSize = 13.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Permanent hearing damage", fontSize = 12.sp, color = Color.Gray)
                    Text("Speaker hardware failure", fontSize = 12.sp, color = Color.Gray)
                    Text("Audio distortion at high volumes", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Use at your own risk. Maximum boost is 300% (~30 dB). Auto-shutoff after 30 minutes.", fontSize = 12.sp, color = TitanColors.NeonYellow)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        AudioStatePersistence.saveDisclaimerAccepted(context, true)
                        showDisclaimer = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TitanColors.NeonCyan)
                ) {
                    Text("I Understand", color = TitanColors.AbsoluteBlack, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TitanColors.AbsoluteBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                "SUPREMEBASS",
                style = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = TitanColors.NeonCyan,
                    letterSpacing = 6.sp,
                    shadow = Shadow(
                        color = TitanColors.NeonCyan.copy(alpha = 0.6f),
                        offset = Offset.Zero,
                        blurRadius = 16f
                    )
                )
            )
            Text(
                "AUDIO BOOST",
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TitanColors.GhostWhite.copy(alpha = 0.5f),
                    letterSpacing = 4.sp
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Big circular boost display
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(220.dp)
            ) {
                // Outer glow ring
                val infiniteTransition = rememberInfiniteTransition(label = "glow")
                val glowAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 0.8f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "glow_alpha"
                )

                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        getWarningColor(gainValue.toInt()).copy(alpha = glowAlpha * 0.2f),
                                        Color.Transparent
                                    ),
                                    radius = size.maxDimension * 0.8f
                                )
                            )
                        }
                )

                // Circle border
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = CircleShape,
                            spotColor = getWarningColor(gainValue.toInt()).copy(alpha = 0.5f)
                        )
                        .clip(CircleShape)
                        .background(TitanColors.CarbonGray.copy(alpha = 0.8f))
                        .border(
                            width = 3.dp,
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    getWarningColor(gainValue.toInt()),
                                    getWarningColor(gainValue.toInt()).copy(alpha = 0.3f),
                                    getWarningColor(gainValue.toInt())
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$totalVolume%",
                            style = TextStyle(
                                fontSize = 56.sp,
                                fontWeight = FontWeight.Black,
                                color = getWarningColor(gainValue.toInt()),
                                shadow = Shadow(
                                    color = getWarningColor(gainValue.toInt()).copy(alpha = 0.6f),
                                    offset = Offset.Zero,
                                    blurRadius = 12f
                                )
                            )
                        )
                        Text(
                            getStatusText(gainValue.toInt()),
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = getWarningColor(gainValue.toInt()).copy(alpha = 0.8f),
                                letterSpacing = 2.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Enable/Disable switch
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(TitanColors.CarbonGray.copy(alpha = 0.6f))
                    .border(1.dp, TitanColors.NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .clickable { isEnabled = !isEnabled }
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Icon(
                    if (isEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                    contentDescription = null,
                    tint = if (isEnabled) TitanColors.RadioactiveGreen else TitanColors.GhostWhite.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (isEnabled) "BOOST ACTIVE" else "BOOST OFF",
                        fontWeight = FontWeight.Bold,
                        color = if (isEnabled) TitanColors.RadioactiveGreen else TitanColors.GhostWhite.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                    if (isEnabled && shutoffMinutesRemaining > 0) {
                        Text(
                            "Auto-shutoff: ${shutoffMinutesRemaining}min",
                            fontSize = 11.sp,
                            color = TitanColors.GhostWhite.copy(alpha = 0.4f)
                        )
                    }
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { isEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TitanColors.RadioactiveGreen,
                        checkedTrackColor = TitanColors.RadioactiveGreen.copy(alpha = 0.3f),
                        uncheckedThumbColor = TitanColors.GhostWhite.copy(alpha = 0.5f),
                        uncheckedTrackColor = TitanColors.CarbonGray
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Preset grid
            Text("PRESETS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TitanColors.GhostWhite.copy(alpha = 0.5f), letterSpacing = 3.sp)
            Spacer(modifier = Modifier.height(8.dp))

            val presets = listOf(100, 125, 150, 175, 200, 250, 300)
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(120.dp)
            ) {
                items(presets) { preset ->
                    val presetGain = (preset - 100).toFloat()
                    val isSelected = totalVolume == preset
                    val color = if (isSelected) TitanColors.NeonCyan else TitanColors.GhostWhite.copy(alpha = 0.3f)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) TitanColors.NeonCyan.copy(alpha = 0.15f) else TitanColors.CarbonGray.copy(alpha = 0.5f))
                            .border(1.dp, color, RoundedCornerShape(8.dp))
                            .clickable {
                                gainValue = presetGain
                                if (!isEnabled) isEnabled = true
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "$preset%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Slider
            Slider(
                value = gainValue,
                onValueChange = { gainValue = it },
                valueRange = 0f..200f,
                colors = SliderDefaults.colors(
                    thumbColor = getWarningColor(gainValue.toInt()),
                    activeTrackColor = getWarningColor(gainValue.toInt()),
                    inactiveTrackColor = TitanColors.CarbonGray
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("100%", fontSize = 11.sp, color = TitanColors.GhostWhite.copy(alpha = 0.4f))
                Text("${gainValue.toInt() + 100}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = getWarningColor(gainValue.toInt()))
                Text("300%", fontSize = 11.sp, color = TitanColors.GhostWhite.copy(alpha = 0.4f))
            }

            // Warning card
            AnimatedVisibility(visible = gainValue > 100) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = getWarningColor(gainValue.toInt()).copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = getWarningColor(gainValue.toInt()), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            getWarningText(gainValue.toInt()),
                            fontSize = 11.sp,
                            color = getWarningColor(gainValue.toInt()),
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

fun getWarningColor(gain: Int): Color {
    val totalVolume = 100 + gain
    return when {
        totalVolume <= 150 -> TitanColors.RadioactiveGreen
        totalVolume <= 200 -> TitanColors.NeonYellow
        totalVolume <= 250 -> TitanColors.NeonOrange
        else -> TitanColors.NeonRed
    }
}

fun getStatusText(gain: Int): String {
    val totalVolume = 100 + gain
    return when {
        totalVolume <= 100 -> "NORMAL"
        totalVolume <= 150 -> "ENHANCED"
        totalVolume <= 200 -> "POWERED"
        totalVolume <= 250 -> "INTENSE"
        else -> "EXTREME"
    }
}

fun getWarningText(gain: Int): String {
    val totalVolume = 100 + gain
    return when {
        totalVolume <= 150 -> "Moderate boost. Safe for most speakers."
        totalVolume <= 200 -> "High boost. May cause distortion on small speakers."
        totalVolume <= 250 -> "Very high boost. Risk of speaker damage."
        else -> "EXTREME BOOST. High risk of hearing damage and speaker failure."
    }
}
