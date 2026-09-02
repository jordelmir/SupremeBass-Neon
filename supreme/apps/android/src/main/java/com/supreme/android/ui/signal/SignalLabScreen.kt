package com.supreme.android.ui.signal

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.supreme.android.domain.model.Waveform
import com.supreme.android.signal.SignalEngineState
import com.supreme.android.ui.theme.TitanColors
import com.supreme.android.ui.components.NeonScreenTitle
import com.supreme.android.ui.components.NeonCard
import com.supreme.android.ui.components.NeonButton
import com.supreme.android.ui.components.NeonSectionHeader
import com.supreme.android.ui.components.NeonChip
import com.supreme.android.ui.components.DetailRow as SharedDetailRow
import com.supreme.android.ui.components.EngineStateIndicator as SharedEngineStateIndicator
import com.supreme.android.ui.components.MatrixRain

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SignalLabScreen(
    viewModel: SignalLabViewModel = viewModel()
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
            title = "SIGNAL LAB",
            subtitle = "Digital signal generation — acoustic output not independently verified"
        )

        // Engine state indicator
        SharedEngineStateIndicator(state.engineState)

        Spacer(modifier = Modifier.height(16.dp))

        // Waveform selector
        NeonSectionHeader(text = "WAVEFORM")
        Spacer(modifier = Modifier.height(8.dp))
        WaveformSelector(
            selected = state.waveform,
            onSelect = viewModel::updateWaveform
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Frequency control
        NeonSectionHeader(text = "FREQUENCY: ${String.format("%.1f", state.frequencyHz)} Hz")
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = state.frequencyHz.toFloat(),
            onValueChange = { viewModel.updateFrequency(it.toDouble()) },
            valueRange = 20f..20_000f,
            colors = SliderDefaults.colors(
                thumbColor = TitanColors.NeonCyan,
                activeTrackColor = TitanColors.NeonCyan
            )
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("20 Hz", style = TextStyle(fontSize = 9.sp, color = Color.Gray))
            Text("20 kHz", style = TextStyle(fontSize = 9.sp, color = Color.Gray))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Amplitude control
        NeonSectionHeader(text = "AMPLITUDE: ${String.format("%.0f", state.amplitude * 100)}%")
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = state.amplitude,
            onValueChange = viewModel::updateAmplitude,
            valueRange = 0.0f..1.0f,
            colors = SliderDefaults.colors(
                thumbColor = TitanColors.NeonCyan,
                activeTrackColor = TitanColors.NeonCyan
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Duration control
        NeonSectionHeader(text = "DURATION: ${if (state.durationMs == 0L) "Continuous" else "${state.durationMs}ms"}")
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = state.durationMs.toFloat(),
            onValueChange = { viewModel.updateDuration(it.toLong()) },
            valueRange = 0f..10_000f,
            colors = SliderDefaults.colors(
                thumbColor = TitanColors.NeonCyan,
                activeTrackColor = TitanColors.NeonCyan
            )
        )

        // Chirp end frequency (conditional)
        if (state.waveform == Waveform.CHIRP) {
            Spacer(modifier = Modifier.height(12.dp))
            NeonSectionHeader(text = "CHIRP END: ${String.format("%.1f", state.chirpEndHz)} Hz")
            Spacer(modifier = Modifier.height(4.dp))
            Slider(
                value = state.chirpEndHz.toFloat(),
                onValueChange = { viewModel.updateChirpEnd(it.toDouble()) },
                valueRange = 20f..20_000f,
                colors = SliderDefaults.colors(
                    thumbColor = TitanColors.NeonCyan,
                    activeTrackColor = TitanColors.NeonCyan
                )
            )
        }

        // Noise band controls (conditional)
        if (state.waveform == Waveform.NOISE_BAND) {
            Spacer(modifier = Modifier.height(12.dp))
            NeonSectionHeader(text = "NOISE LOW: ${String.format("%.0f", state.noiseLowHz)} Hz")
            Slider(
                value = state.noiseLowHz.toFloat(),
                onValueChange = { viewModel.updateNoiseLow(it.toDouble()) },
                valueRange = 1f..10_000f,
                colors = SliderDefaults.colors(thumbColor = TitanColors.NeonCyan, activeTrackColor = TitanColors.NeonCyan)
            )
            NeonSectionHeader(text = "NOISE HIGH: ${String.format("%.0f", state.noiseHighHz)} Hz")
            Slider(
                value = state.noiseHighHz.toFloat(),
                onValueChange = { viewModel.updateNoiseHigh(it.toDouble()) },
                valueRange = 100f..20_000f,
                colors = SliderDefaults.colors(thumbColor = TitanColors.NeonCyan, activeTrackColor = TitanColors.NeonCyan)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sample rate selector
        NeonSectionHeader(text = "SAMPLE RATE")
        Spacer(modifier = Modifier.height(8.dp))
        SampleRateSelector(
            selected = state.sampleRate,
            onSelect = viewModel::updateSampleRate
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val isRunning = state.engineState is SignalEngineState.Running

            Button(
                onClick = { if (isRunning) viewModel.stopSignal() else viewModel.startSignal() },
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) TitanColors.NeonOrange else TitanColors.NeonCyan
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = TitanColors.AbsoluteBlack
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isRunning) "STOP" else "START",
                    fontWeight = FontWeight.Bold,
                    color = TitanColors.AbsoluteBlack
                )
            }

            Button(
                onClick = { viewModel.startSweep() },
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TitanColors.ElectricPurple
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.SwapVert, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SWEEP", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Emergency stop
        Button(
            onClick = viewModel::emergencyStop,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF1744)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("EMERGENCY STOP", fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 2.sp)
        }

        // Sweep progress
        if (state.totalSweepSteps > 0 && state.sweepProgress < 1f) {
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { state.sweepProgress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = TitanColors.NeonCyan,
                trackColor = TitanColors.CarbonGray
            )
            Text(
                text = "Sweep: Step ${state.currentSweepStep + 1}/${state.totalSweepSteps}",
                style = TextStyle(fontSize = 10.sp, color = TitanColors.NeonCyan.copy(alpha = 0.7f))
            )
        }

        // Telemetry display
        state.telemetry?.let { telem ->
            Spacer(modifier = Modifier.height(16.dp))
            NeonCard(glowColor = TitanColors.NeonCyan) {
                NeonSectionHeader(text = "SESSION TELEMETRY")
                Spacer(modifier = Modifier.height(8.dp))
                SharedDetailRow("Peak", String.format("%.4f", telem.peak))
                SharedDetailRow("RMS", String.format("%.4f", telem.rms))
                SharedDetailRow("Duration", "${telem.durationMs}ms")
                SharedDetailRow("Route", telem.audioRoute.name)
                SharedDetailRow("Termination", telem.terminationReason)
            }
        }

        // Export button
        if (state.engineState is SignalEngineState.Idle && state.telemetry != null) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = viewModel::exportTelemetry,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TitanColors.NeonCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("EXPORT SESSION")
            }
        }

        // Exported JSON preview
        state.exportedJson?.let { json ->
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = json.take(500) + if (json.length > 500) "\n..." else "",
                    style = TextStyle(fontSize = 8.sp, color = Color.Gray),
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        }
    }
}



@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WaveformSelector(selected: Waveform, onSelect: (Waveform) -> Unit) {
    val waveforms = listOf(
        Waveform.SINE, Waveform.SQUARE, Waveform.TRIANGLE,
        Waveform.SAWTOOTH, Waveform.CHIRP, Waveform.NOISE_BAND,
        Waveform.MULTI_TONE
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        waveforms.forEach { wf ->
            val isSelected = wf == selected
            Surface(
                onClick = { onSelect(wf) },
                color = if (isSelected) TitanColors.NeonCyan else Color(0xFF1A1A1A),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.border(
                    width = if (isSelected) 1.5.dp else 0.5.dp,
                    color = if (isSelected) TitanColors.NeonCyan else Color.Gray.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                )
            ) {
                Text(
                    text = wf.name,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) TitanColors.AbsoluteBlack else Color.Gray,
                        letterSpacing = 1.sp
                    )
                )
            }
        }
    }
}

@Composable
fun SampleRateSelector(selected: Int, onSelect: (Int) -> Unit) {
    val rates = listOf(44_100, 48_000, 96_000)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        rates.forEach { rate ->
            val isSelected = rate == selected
            Surface(
                onClick = { onSelect(rate) },
                color = if (isSelected) TitanColors.NeonCyan else Color(0xFF1A1A1A),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.border(
                    width = if (isSelected) 1.5.dp else 0.5.dp,
                    color = if (isSelected) TitanColors.NeonCyan else Color.Gray.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                )
            ) {
                Text(
                    text = "${rate / 1000}kHz",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) TitanColors.AbsoluteBlack else Color.Gray
                    )
                )
            }
        }
    }
}


