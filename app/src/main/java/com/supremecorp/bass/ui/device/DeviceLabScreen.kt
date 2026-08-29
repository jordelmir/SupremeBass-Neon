package com.supremecorp.bass.ui.device

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.supremecorp.bass.domain.model.DeviceAcousticProfile
import com.supremecorp.bass.ui.device.MeasuredPoint
import com.supremecorp.bass.signal.SignalEngineState
import com.supremecorp.bass.ui.components.NeonScreenTitle
import com.supremecorp.bass.ui.components.NeonCard
import com.supremecorp.bass.ui.components.NeonButton
import com.supremecorp.bass.ui.components.NeonSectionHeader
import com.supremecorp.bass.ui.components.DetailRow as SharedDetailRow
import com.supremecorp.bass.ui.components.EngineStateIndicator as SharedEngineStateIndicator
import com.supremecorp.bass.ui.components.MatrixRain
import com.supremecorp.bass.ui.theme.TitanColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceLabScreen(
    viewModel: DeviceLabViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(TitanColors.AbsoluteBlack)) {
        MatrixRain(
            modifier = Modifier.fillMaxSize(),
            color = TitanColors.NeonCyan.copy(alpha = 0.12f)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
        NeonScreenTitle(
            title = "DEVICE LAB",
            subtitle = "Frequency response characterization — digital signal only, no calibrated mic"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Characterization controls
        NeonCard(glowColor = TitanColors.NeonCyan) {
                NeonSectionHeader(text = "CHARACTERIZATION")
                Spacer(modifier = Modifier.height(12.dp))

                if (state.isCharacterizing) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(
                            text = "Step ${state.currentStep}/${state.totalSteps} — ${String.format("%.1f", state.currentFrequency)} Hz",
                            style = TextStyle(fontSize = 13.sp, color = Color.White)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = state.progress,
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = TitanColors.NeonCyan,
                            trackColor = TitanColors.CarbonGray
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Live response preview - simple text list
                        if (state.measuredPoints.isNotEmpty()) {
                            Text("Latest measurements:", style = TextStyle(fontSize = 11.sp, color = TitanColors.NeonCyan.copy(alpha = 0.7f)))
                            Spacer(modifier = Modifier.height(4.dp))
                            Column {
                                state.measuredPoints.takeLast(5).reversed().forEach { point ->
                                    Text(
                                        text = "${String.format("%.1f", point.frequencyHz)} Hz → Peak: ${String.format("%.4f", point.peak)} RMS: ${String.format("%.4f", point.rms)}",
                                        style = TextStyle(fontSize = 10.sp, color = Color.Gray)
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = viewModel::stopCharacterization,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TitanColors.NeonOrange),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("STOP", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else {
                    Text(
                        text = "Logarithmic sweep: 20 Hz → 20 kHz, 30 steps, 300ms dwell",
                        style = TextStyle(fontSize = 11.sp, color = Color.Gray)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = viewModel::startCharacterization,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TitanColors.RadioactiveGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = TitanColors.AbsoluteBlack)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("START CHARACTERIZATION", fontWeight = FontWeight.Bold, color = TitanColors.AbsoluteBlack)
                    }
                }

                state.error?.let { err ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(err, style = TextStyle(fontSize = 11.sp, color = Color(0xFFFF1744)))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Acoustic Measurements Section
        NeonCard(glowColor = Color(0xFFFF00FF)) {
            NeonSectionHeader(text = "ACOUSTIC MEASUREMENTS")
            Spacer(modifier = Modifier.height(12.dp))

            // RT60 Measurement
            Text(
                text = "RT60 (Reverberation Time)",
                style = TextStyle(fontSize = 12.sp, color = Color(0xFFFF00FF), fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Measures how long sound takes to decay by 60 dB",
                style = TextStyle(fontSize = 10.sp, color = Color.Gray)
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (state.isMeasuringRT60) {
                Column {
                    LinearProgressIndicator(
                        progress = state.rt60Progress,
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFFFF00FF),
                        trackColor = TitanColors.CarbonGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = viewModel::stopRT60Measurement,
                        colors = ButtonDefaults.buttonColors(containerColor = TitanColors.NeonOrange),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("STOP", fontSize = 10.sp)
                    }
                }
            } else {
                Button(
                    onClick = viewModel::startRT60Measurement,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF00FF)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("START RT60", fontSize = 10.sp, color = Color.White)
                }
            }

            state.rt60Result?.let { result ->
                Spacer(modifier = Modifier.height(8.dp))
                NeonCard(glowColor = Color(0xFFFF00FF).copy(alpha = 0.5f)) {
                    SharedDetailRow("RT60", "${String.format("%.2f", result.rt60Seconds)} seconds")
                    SharedDetailRow("EDT", "${String.format("%.2f", result.edtSeconds)} seconds")
                    SharedDetailRow("Room Type", result.roomQuality)
                    SharedDetailRow("Confidence", "${String.format("%.0f", result.confidence * 100)}%")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SPL Measurement
            Text(
                text = "SPL (Sound Pressure Level)",
                style = TextStyle(fontSize = 12.sp, color = Color(0xFFFF00FF), fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Measures loudness relative to digital full scale",
                style = TextStyle(fontSize = 10.sp, color = Color.Gray)
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (state.isMeasuringSPL) {
                Column {
                    LinearProgressIndicator(
                        progress = state.splProgress,
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFFFF00FF),
                        trackColor = TitanColors.CarbonGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = viewModel::stopSPLMeasurement,
                        colors = ButtonDefaults.buttonColors(containerColor = TitanColors.NeonOrange),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("STOP", fontSize = 10.sp)
                    }
                }
            } else {
                Button(
                    onClick = viewModel::startSPLMeasurement,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF00FF)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("START SPL", fontSize = 10.sp, color = Color.White)
                }
            }

            state.splResult?.let { result ->
                Spacer(modifier = Modifier.height(8.dp))
                NeonCard(glowColor = Color(0xFFFF00FF).copy(alpha = 0.5f)) {
                    SharedDetailRow("Peak dBFS", "${String.format("%.1f", result.peakDbFS)} dB")
                    SharedDetailRow("RMS dBFS", "${String.format("%.1f", result.rmsDbFS)} dB")
                    SharedDetailRow("Integrated", "${String.format("%.1f", result.integratedLoudness)} LUFS")
                    SharedDetailRow("Dynamic Range", "${String.format("%.1f", result.dynamicRange)} dB")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // THD Measurement
            Text(
                text = "THD (Total Harmonic Distortion)",
                style = TextStyle(fontSize = 12.sp, color = Color(0xFFFF00FF), fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Measures harmonic distortion in the signal",
                style = TextStyle(fontSize = 10.sp, color = Color.Gray)
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (state.isMeasuringTHD) {
                Column {
                    LinearProgressIndicator(
                        progress = state.thdProgress,
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFFFF00FF),
                        trackColor = TitanColors.CarbonGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = viewModel::stopTHDMeasurement,
                        colors = ButtonDefaults.buttonColors(containerColor = TitanColors.NeonOrange),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("STOP", fontSize = 10.sp)
                    }
                }
            } else {
                Button(
                    onClick = viewModel::startTHDMeasurement,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF00FF)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("START THD", fontSize = 10.sp, color = Color.White)
                }
            }

            state.thdResult?.let { result ->
                Spacer(modifier = Modifier.height(8.dp))
                NeonCard(glowColor = Color(0xFFFF00FF).copy(alpha = 0.5f)) {
                    SharedDetailRow("THD", "${String.format("%.3f", result.thdPercent)}%")
                    SharedDetailRow("THD+N", "${String.format("%.3f", result.thdPlusNPercent)}%")
                    SharedDetailRow("Fundamental", "${String.format("%.1f", result.fundamentalHz)} Hz")
                    SharedDetailRow("Quality", result.qualityRating)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Saved profiles
        NeonCard(glowColor = TitanColors.NeonCyan) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    NeonSectionHeader(text = "SAVED PROFILES")
                    if (state.savedProfiles.isNotEmpty()) {
                        Text("${state.savedProfiles.size} profiles", style = TextStyle(fontSize = 10.sp, color = Color.Gray))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (state.savedProfiles.isEmpty()) {
                    Text(
                        text = "No characterizations saved yet",
                        style = TextStyle(fontSize = 13.sp, color = Color.Gray),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).align(Alignment.CenterHorizontally)
                    )
                } else {
                    Column {
                        state.savedProfiles.forEach { profile ->
                            ProfileListItem(
                                profile = profile,
                                isSelected = state.selectedProfile?.id == profile.id,
                                onClick = { viewModel.selectProfile(profile) },
                                onDelete = { viewModel.deleteProfile(profile) }
                            )
                        }
                    }
                }
        }

        // Selected profile detail
        state.selectedProfile?.let { profile ->
            Spacer(modifier = Modifier.height(16.dp))
            NeonCard(glowColor = TitanColors.NeonCyan) {
                    NeonSectionHeader(text = "PROFILE DETAIL")
                    Spacer(modifier = Modifier.height(8.dp))
                    ProfileDetailCard(profile = profile)
                }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ProfileListItem(
    profile: DeviceAcousticProfile,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val color = if (isSelected) TitanColors.NeonCyan else Color.Gray

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = color.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            ),
        color = if (isSelected) TitanColors.NeonCyan.copy(alpha = 0.1f) else Color(0xFF1A1A1A),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${profile.manufacturer} ${profile.model}",
                    style = TextStyle(fontSize = 12.sp, color = if (isSelected) TitanColors.NeonCyan else Color.White, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${profile.measuredResponses.size} points • ${profile.outputRoute.name} • ${profile.authority.name}",
                    style = TextStyle(fontSize = 9.sp, color = Color.Gray)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF1744))
            }
        }
    }
}

@Composable
fun ProfileDetailCard(profile: DeviceAcousticProfile) {
    Column {
        SharedDetailRow("Manufacturer", profile.manufacturer)
        SharedDetailRow("Model", profile.model)
        SharedDetailRow("Device", profile.androidDevice)
        SharedDetailRow("Route", profile.outputRoute.name)
        SharedDetailRow("Sample Rates", profile.supportedSampleRates.joinToString(", ") { "${it/1000}kHz" })
        SharedDetailRow("Points", profile.measuredResponses.size.toString())
        SharedDetailRow("Authority", profile.authority.name)

        if (profile.measuredResponses.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("FREQUENCY RESPONSE (last 10 points)", style = TextStyle(fontSize = 9.sp, color = TitanColors.NeonCyan.copy(alpha = 0.7f), fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(4.dp))
            Column {
                profile.measuredResponses.takeLast(10).reversed().forEach { point ->
                    Text(
                        text = "${String.format("%.1f", point.frequencyHz)} Hz → ${String.format("%.4f", point.measuredMetric ?: 0.0)} (${point.authority.name})",
                        style = TextStyle(fontSize = 9.sp, color = Color.Gray)
                    )
                }
            }
        }
    }
}
