package com.supremecorp.bass

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import com.supremecorp.bass.ui.components.AudioVisualizer
import com.supremecorp.bass.ui.components.BreathingText
import com.supremecorp.bass.ui.components.MatrixRain
import com.supremecorp.bass.ui.components.NeonSwitch
import com.supremecorp.bass.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdsManager.initialize(this)

        // Always stop service on fresh launch to ensure clean state
        // If it was persisted as enabled, we'll restart it below
        stopService(Intent(this, AudioService::class.java))

        setContent {
            TitanTheme {
                SupremeBassScreen()
            }
        }
    }

    override fun onDestroy() {
        // Persist current state before destruction
        super.onDestroy()
    }
}

@Composable
fun SupremeBassScreen() {
    val context = LocalContext.current
    var isEnabled by remember { mutableStateOf(AudioStatePersistence.isEnabled(context)) }
    var gainValue by remember { mutableFloatStateOf(AudioStatePersistence.gainValue(context)) }
    var presetChangeCount by remember { mutableIntStateOf(0) }

    val totalVolume = 100 + gainValue.toInt()

    val activity = context as? ComponentActivity

    // Sync service with UI state — always
    LaunchedEffect(isEnabled, gainValue) {
        // Persist state
        AudioStatePersistence.saveEnabled(context, isEnabled)
        AudioStatePersistence.saveGain(context, gainValue)

        if (isEnabled && gainValue > 0) {
            val intent = Intent(context, AudioService::class.java).apply {
                putExtra("GAIN", gainValue.toInt())
            }
            context.startService(intent)
        } else {
            context.stopService(Intent(context, AudioService::class.java))
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "main_screen")

    // Background ambient pulse
    val ambientAlpha by infiniteTransition.animateFloat(
        initialValue = 0.02f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TitanColors.AbsoluteBlack)
    ) {
        // Matrix Rain background
        MatrixRain(
            modifier = Modifier.fillMaxSize(),
            color = TitanColors.NeonCyan.copy(alpha = 0.25f)
        )

        // Ambient radial neon pulse
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                getWarningColor(gainValue.toInt()).copy(alpha = ambientAlpha),
                                Color.Transparent
                            ),
                            center = Offset(size.width / 2, size.height * 0.35f),
                            radius = size.maxDimension * 0.6f
                        )
                    )
                }
        )

        // Main content with banner ad fixed at bottom
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Spacer(modifier = Modifier.height(32.dp))

            // ═══════ TITLE ═══════
            BreathingText(
                text = "SUPREME BASS",
                color = TitanColors.NeonCyan,
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 6.sp,
                breatheScale = 0.04f,
                breatheDuration = 3000
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle with scan effect
            val subtitleAlpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 0.9f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "subtitle"
            )
            Text(
                text = "⚡ HARDWARE ENGINE v2.0 ⚡",
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 3.sp,
                    color = TitanColors.ElectricPurple.copy(alpha = subtitleAlpha),
                    shadow = Shadow(
                        color = TitanColors.ElectricPurple.copy(alpha = subtitleAlpha * 0.6f),
                        offset = Offset.Zero,
                        blurRadius = 12f
                    )
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ═══════ NEON SWITCH ═══════
            NeonSwitch(
                isEnabled = isEnabled,
                onToggle = { isEnabled = !isEnabled },
                modifier = Modifier.breathingGlow(
                    glowColor = if (isEnabled) TitanColors.RadioactiveGreen else Color(0xFFFF1744)
                )
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ═══════ VOLUME DISPLAY ═══════
            val warningColor = getWarningColor(gainValue.toInt())

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .reactorGlass(glowColor = warningColor)
                    .scanLineOverlay(lineColor = warningColor, duration = 2500),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Volume number with heavy glow
                    val volumeScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.05f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1800, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "vol_scale"
                    )

                    Text(
                        text = "$totalVolume%",
                        modifier = Modifier.scale(volumeScale),
                        style = TextStyle(
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Black,
                            color = warningColor,
                            shadow = Shadow(
                                color = warningColor.copy(alpha = 0.8f),
                                offset = Offset.Zero,
                                blurRadius = 20f
                            )
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Status text
                    val statusPulse by infiniteTransition.animateFloat(
                        initialValue = 0.5f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "status_pulse"
                    )

                    Text(
                        text = getStatusText(gainValue.toInt()),
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 3.sp,
                            color = warningColor.copy(alpha = statusPulse),
                            shadow = Shadow(
                                color = warningColor.copy(alpha = statusPulse * 0.6f),
                                offset = Offset.Zero,
                                blurRadius = 8f
                            )
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ═══════ AUDIO VISUALIZER ═══════
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neonGlass(glowColor = warningColor.copy(alpha = 0.5f))
                    .padding(vertical = 8.dp, horizontal = 4.dp)
            ) {
                AudioVisualizer(
                    isActive = isEnabled,
                    gainLevel = gainValue,
                    baseColor = TitanColors.NeonCyan
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ═══════ PRESETS GRID ═══════
            Text(
                text = "◉ BOOST PRESETS",
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    color = TitanColors.NeonCyan.copy(alpha = 0.7f),
                    shadow = Shadow(
                        color = TitanColors.NeonCyan.copy(alpha = 0.4f),
                        offset = Offset.Zero,
                        blurRadius = 6f
                    )
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            val presets = listOf(100, 125, 150, 175, 200, 225, 250, 275, 300, 400)
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                items(presets) { preset ->
                    val presetGain = (preset - 100).toFloat()
                    val isSelected = totalVolume == preset
                    val presetColor = getPresetColor(preset)

                    NeonPresetButton(
                        text = preset.toString(),
                        isSelected = isSelected,
                        glowColor = presetColor,
                        onClick = {
                            gainValue = presetGain
                            presetChangeCount++
                            // Show interstitial every 3 preset changes
                            if (presetChangeCount % 3 == 0 && activity != null) {
                                AdsManager.showInterstitialIfReady(activity)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ═══════ SLIDER ═══════
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neonGlass(glowColor = warningColor.copy(alpha = 0.3f))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "100%",
                            style = TextStyle(
                                fontSize = 10.sp,
                                color = TitanColors.NeonCyan.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "◈ GAIN CONTROL ◈",
                            style = TextStyle(
                                fontSize = 10.sp,
                                letterSpacing = 2.sp,
                                color = warningColor.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold,
                                shadow = Shadow(
                                    color = warningColor.copy(alpha = 0.5f),
                                    offset = Offset.Zero,
                                    blurRadius = 6f
                                )
                            )
                        )
                        Text(
                            text = "400%",
                            style = TextStyle(
                                fontSize = 10.sp,
                                color = TitanColors.NeonRed.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Slider(
                        value = gainValue,
                        onValueChange = { gainValue = it },
                        valueRange = 0f..300f,
                        colors = SliderDefaults.colors(
                            thumbColor = warningColor,
                            activeTrackColor = warningColor,
                            inactiveTrackColor = TitanColors.CarbonGray.copy(alpha = 0.8f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ═══════ WARNING CARD ═══════
            AnimatedVisibility(
                visible = gainValue > 100,
                enter = fadeIn(tween(500)) + expandVertically(tween(500)),
                exit = fadeOut(tween(300)) + shrinkVertically(tween(300))
            ) {
                WarningCard(gainValue = gainValue.toInt())
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer
            val footerAlpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 0.5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "footer"
            )
            Text(
                text = "SUPREME CORP™ // CLASSIFIED HARDWARE",
                style = TextStyle(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp,
                    color = TitanColors.NeonCyan.copy(alpha = footerAlpha),
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            }

            // ═══════ BANNER AD — Fixed at bottom ═══════
            AndroidView(
                factory = { ctx ->
                    AdsManager.createBannerAd(ctx)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(Color.Black)
            )
        }
    }
}

// ═══════ NEON PRESET BUTTON ═══════
@Composable
fun NeonPresetButton(
    text: String,
    isSelected: Boolean,
    glowColor: Color,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "preset_$text")

    val selectionGlow by infiniteTransition.animateFloat(
        initialValue = if (isSelected) 0.5f else 0f,
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sel_glow"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "preset_scale"
    )

    Box(
        modifier = Modifier
            .height(42.dp)
            .scale(scale)
            .shadow(
                elevation = if (isSelected) 12.dp else 2.dp,
                shape = RoundedCornerShape(10.dp),
                spotColor = if (isSelected) glowColor.copy(alpha = selectionGlow) else Color.Transparent,
                ambientColor = if (isSelected) glowColor.copy(alpha = selectionGlow * 0.5f) else Color.Transparent
            )
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) {
                    Brush.verticalGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.9f),
                            glowColor.copy(alpha = 0.6f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0A0A0A),
                            Color(0xFF050505)
                        )
                    )
                }
            )
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        glowColor.copy(alpha = if (isSelected) (selectionGlow * 0.8f + 0.2f) else 0.2f),
                        glowColor.copy(alpha = 0.05f),
                        glowColor.copy(alpha = if (isSelected) (selectionGlow * 0.5f + 0.1f) else 0.1f)
                    )
                ),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                color = if (isSelected) TitanColors.AbsoluteBlack else TitanColors.GhostWhite.copy(alpha = 0.7f),
                shadow = if (!isSelected) Shadow(
                    color = glowColor.copy(alpha = 0.3f),
                    offset = Offset.Zero,
                    blurRadius = 4f
                ) else null
            )
        )
    }
}

// ═══════ WARNING CARD ═══════
@Composable
fun WarningCard(gainValue: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "warning")

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "warn_pulse"
    )

    val warningScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "warn_scale"
    )

    val warningText = when {
        gainValue > 200 -> "☢️ CRITICAL: NUCLEAR OVERLOAD — HARDWARE MELTDOWN"
        gainValue > 150 -> "⚠️ DANGER: EXTREME LEVELS — SPEAKER DAMAGE RISK"
        else -> "⚡ CAUTION: HIGH PERFORMANCE MODE ACTIVE"
    }

    val borderColor = when {
        gainValue > 200 -> TitanColors.NeonRed
        gainValue > 150 -> TitanColors.NeonOrange
        else -> TitanColors.NeonYellow
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(warningScale)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(14.dp),
                spotColor = borderColor.copy(alpha = pulseAlpha * 0.6f),
                ambientColor = borderColor.copy(alpha = pulseAlpha * 0.3f)
            )
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A0000).copy(alpha = 0.9f),
                        Color(0xFF0A0000).copy(alpha = 0.95f)
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        borderColor.copy(alpha = pulseAlpha),
                        borderColor.copy(alpha = pulseAlpha * 0.3f),
                        borderColor.copy(alpha = pulseAlpha * 0.7f)
                    )
                ),
                shape = RoundedCornerShape(14.dp)
            )
            .scanLineOverlay(lineColor = borderColor, duration = 1500)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = borderColor.copy(alpha = pulseAlpha),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = warningText,
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = borderColor.copy(alpha = pulseAlpha * 0.7f + 0.3f),
                    shadow = Shadow(
                        color = borderColor.copy(alpha = pulseAlpha * 0.5f),
                        offset = Offset.Zero,
                        blurRadius = 8f
                    )
                )
            )
        }
    }
}

// ═══════ UTILITY FUNCTIONS ═══════
private fun getWarningColor(gain: Int): Color {
    return when {
        gain > 200 -> TitanColors.NeonRed
        gain > 100 -> TitanColors.NeonOrange
        gain > 50 -> TitanColors.NeonYellow
        else -> TitanColors.NeonCyan
    }
}

private fun getStatusText(gain: Int): String {
    return when {
        gain > 200 -> "☢ NUCLEAR STAGE ☢"
        gain > 100 -> "⚠ HIGH RISK ZONE"
        gain > 50 -> "⚡ ELEVATED POWER"
        else -> "◈ SYSTEM STABLE ◈"
    }
}

private fun getPresetColor(preset: Int): Color {
    return when {
        preset >= 300 -> TitanColors.NeonRed
        preset >= 200 -> TitanColors.NeonOrange
        preset >= 150 -> TitanColors.NeonYellow
        else -> TitanColors.NeonCyan
    }
}

// Extension to avoid error if clickable is used without Indication
@Composable
fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = this.clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = null,
    onClick = onClick
)
