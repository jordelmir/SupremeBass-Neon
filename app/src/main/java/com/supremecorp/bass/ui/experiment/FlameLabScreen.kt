package com.supremecorp.bass.ui.experiment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.supremecorp.bass.domain.model.*
import com.supremecorp.bass.ui.components.NeonScreenTitle
import com.supremecorp.bass.ui.components.NeonCard
import com.supremecorp.bass.ui.components.NeonButton
import com.supremecorp.bass.ui.components.NeonSectionHeader
import com.supremecorp.bass.ui.components.MatrixRain
import com.supremecorp.bass.ui.theme.TitanColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlameLabScreen(
    viewModel: ExperimentLabViewModel = viewModel()
) {
    val flameSafety by viewModel.flameSafety.collectAsState()
    var distance by remember { mutableStateOf("1.0") }
    var duration by remember { mutableStateOf("30") }
    var amplitude by remember { mutableStateOf("0.5") }
    var frequency by remember { mutableStateOf("1000") }
    var selectedFlameType by remember { mutableStateOf(FlameType.CANDLE) }
    var selectedWaveform by remember { mutableStateOf(Waveform.SINE) }
    var showDisclaimer by remember { mutableStateOf(true) }
    var disclaimerAccepted by remember { mutableStateOf(false) }
    var testNotes by remember { mutableStateOf("") }

    val safetyState = viewModel.checkFlameSafety(
        FlameExperimentConfig(
            flameType = selectedFlameType,
            distanceMeters = distance.toDoubleOrNull() ?: 1.0,
            durationSeconds = duration.toIntOrNull() ?: 30,
            waveform = selectedWaveform,
            frequencyHz = frequency.toDoubleOrNull() ?: 1000.0,
            amplitude = amplitude.toDoubleOrNull() ?: 0.5,
            testNotes = testNotes
        )
    )

    Box(modifier = Modifier.fillMaxSize().background(TitanColors.AbsoluteBlack)) {
        MatrixRain(
            modifier = Modifier.fillMaxSize(),
            color = TitanColors.NeonOrange.copy(alpha = 0.12f)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
        NeonScreenTitle(
            title = "FLAME LAB",
            subtitle = "⚠ Experimental — digital signal only — no physical flame claims",
            accentColor = TitanColors.NeonOrange
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Safety warning banner
        if (showDisclaimer) {
            NeonCard(glowColor = Color(0xFFFF1744)) {
                Text("SAFETY DISCLAIMER", style = TextStyle(fontSize = 10.sp, color = Color(0xFFFF1744), fontWeight = FontWeight.Bold, letterSpacing = 2.sp))
                Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This is a digital acoustic signal generator for experimental research purposes only. " +
                                "It does NOT measure or claim to interact with physical flames. " +
                                "All outputs are digital signal characteristics. " +
                                "Do NOT use near open flames. This app is not a measurement instrument.",
                        style = TextStyle(fontSize = 10.sp, color = Color.Gray)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { disclaimerAccepted = true; showDisclaimer = false },
                            colors = ButtonDefaults.buttonColors(containerColor = TitanColors.NeonOrange),
                            shape = RoundedCornerShape(8.dp)
                        ) { Text("I UNDERSTAND", color = Color.White, fontSize = 10.sp) }
                        TextButton(onClick = { showDisclaimer = false }) {
                            Text("Dismiss", color = Color.Gray, fontSize = 10.sp)
                        }
                    }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Safety status
        SafetyStatusCard(safetyState)

        Spacer(modifier = Modifier.height(16.dp))

        // Experiment configuration
        NeonCard(glowColor = TitanColors.NeonCyan) {
                Text("CONFIGURATION", style = TextStyle(fontSize = 10.sp, color = TitanColors.NeonCyan.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, letterSpacing = 2.sp))
                Spacer(modifier = Modifier.height(12.dp))

                // Flame type
                Text("Flame Type", style = TextStyle(fontSize = 11.sp, color = Color.Gray))
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlameType.values().take(4).forEach { type ->
                        FilterChip(
                            selected = selectedFlameType == type,
                            onClick = { selectedFlameType = type },
                            label = { Text(type.name.replace("_", " "), fontSize = 9.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TitanColors.NeonOrange.copy(alpha = 0.2f),
                                selectedLabelColor = TitanColors.NeonOrange
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Waveform
                Text("Waveform", style = TextStyle(fontSize = 11.sp, color = Color.Gray))
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Waveform.SINE, Waveform.SQUARE, Waveform.TRIANGLE, Waveform.SAWTOOTH).forEach { wf ->
                        FilterChip(
                            selected = selectedWaveform == wf,
                            onClick = { selectedWaveform = wf },
                            label = { Text(wf.name, fontSize = 9.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TitanColors.NeonCyan.copy(alpha = 0.2f),
                                selectedLabelColor = TitanColors.NeonCyan
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Parameters
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = distance,
                        onValueChange = { distance = it },
                        label = { Text("Distance (m)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        isError = safetyState.violations.contains(FlameSafetyViolation.DISTANCE_TOO_CLOSE) ||
                                safetyState.violations.contains(FlameSafetyViolation.DISTANCE_TOO_FAR)
                    )
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { duration = it },
                        label = { Text("Duration (s)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        isError = safetyState.violations.contains(FlameSafetyViolation.DURATION_EXCEEDED)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = frequency,
                        onValueChange = { frequency = it },
                        label = { Text("Frequency (Hz)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        isError = safetyState.violations.contains(FlameSafetyViolation.FREQUENCY_OUT_OF_RANGE)
                    )
                    OutlinedTextField(
                        value = amplitude,
                        onValueChange = { amplitude = it },
                        label = { Text("Amplitude") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        isError = safetyState.violations.contains(FlameSafetyViolation.AMPLITUDE_TOO_HIGH)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = testNotes,
                    onValueChange = { testNotes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }

        Spacer(modifier = Modifier.height(16.dp))

        // Start button
        Button(
            onClick = {
                viewModel.startFlameExperiment(
                    name = "Flame: ${selectedFlameType.name} @ ${distance}m",
                    frequencyHz = frequency.toDoubleOrNull() ?: 1000.0,
                    amplitude = (amplitude.toDoubleOrNull() ?: 0.5).toFloat(),
                    waveform = selectedWaveform,
                    durationSeconds = duration.toIntOrNull() ?: 30
                )
                viewModel.markFlameExperimentCompleted()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (safetyState.canStart) TitanColors.NeonOrange else Color.Gray
            ),
            shape = RoundedCornerShape(12.dp),
            enabled = safetyState.canStart
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("START FLAME EXPERIMENT", fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 2.sp)
        }

        if (!safetyState.canStart) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Blocked: ${safetyState.violations.joinToString(", ") { it.name.replace("_", " ") }}",
                style = TextStyle(fontSize = 10.sp, color = Color(0xFFFF1744))
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SafetyStatusCard(state: com.supremecorp.bass.domain.model.FlameSafetyState) {
    val color = if (state.canStart) TitanColors.RadioactiveGreen else Color(0xFFFF1744)
    val text = if (state.canStart) "SAFE" else "BLOCKED"
    val detail = state.violations.joinToString(", ") { it.name.replace("_", " ") }

    NeonCard(glowColor = color) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text, style = TextStyle(fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold, letterSpacing = 2.sp))
                if (detail.isNotEmpty()) {
                    Text(detail, style = TextStyle(fontSize = 9.sp, color = Color.Gray))
                }
            }
        }
    }
}
