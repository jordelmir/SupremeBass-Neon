package com.supremecorp.bass.ui.experiment

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.supremecorp.bass.domain.model.AcousticExperiment
import com.supremecorp.bass.domain.model.ExperimentStatus
import com.supremecorp.bass.domain.model.ExperimentType
import com.supremecorp.bass.ui.components.NeonScreenTitle
import com.supremecorp.bass.ui.components.NeonCard
import com.supremecorp.bass.ui.components.NeonButton
import com.supremecorp.bass.ui.components.NeonSectionHeader
import com.supremecorp.bass.ui.components.DetailRow as SharedDetailRow
import com.supremecorp.bass.ui.components.MatrixRain
import com.supremecorp.bass.ui.theme.TitanColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperimentLabScreen(
    viewModel: ExperimentLabViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    var showNewExperimentDialog by remember { mutableStateOf(false) }

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
            title = "EXPERIMENT LAB",
            subtitle = "Acoustic experiments — digital signal only, no physical measurements claimed"
        )

        // New experiment button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NeonButton(text = "NEW EXPERIMENT", onClick = { showNewExperimentDialog = true }, modifier = Modifier.weight(1f), color = TitanColors.RadioactiveGreen)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Running experiment
        state.currentExperiment?.let { experiment ->
            if (experiment.status == ExperimentStatus.RUNNING || experiment.status == ExperimentStatus.PAUSED) {
                NeonCard(glowColor = TitanColors.RadioactiveGreen) {
                        Text("RUNNING", style = TextStyle(fontSize = 10.sp, color = TitanColors.RadioactiveGreen, fontWeight = FontWeight.Bold, letterSpacing = 2.sp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(experiment.name, style = TextStyle(fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold))
                        Text("${experiment.type.name} • Step ${experiment.currentStep}/${experiment.stepCount}", style = TextStyle(fontSize = 11.sp, color = Color.Gray))

                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = experiment.progress,
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = if (experiment.status == ExperimentStatus.RUNNING) TitanColors.RadioactiveGreen else TitanColors.NeonYellow,
                            trackColor = TitanColors.CarbonGray
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (experiment.status == ExperimentStatus.RUNNING) {
                                Button(
                                    onClick = viewModel::pauseExperiment,
                                    colors = ButtonDefaults.buttonColors(containerColor = TitanColors.NeonYellow),
                                    shape = RoundedCornerShape(8.dp)
                                ) { Text("PAUSE", fontSize = 10.sp, color = TitanColors.AbsoluteBlack) }
                            } else {
                                Button(
                                    onClick = viewModel::resumeExperiment,
                                    colors = ButtonDefaults.buttonColors(containerColor = TitanColors.RadioactiveGreen),
                                    shape = RoundedCornerShape(8.dp)
                                ) { Text("RESUME", fontSize = 10.sp, color = TitanColors.AbsoluteBlack) }
                            }
                            Button(
                                onClick = viewModel::abortExperiment,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744)),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("ABORT", fontSize = 10.sp, color = Color.White) }
                        }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Experiment history
        NeonCard(glowColor = TitanColors.NeonCyan) {
                NeonSectionHeader(text = "EXPERIMENTS")
                Spacer(modifier = Modifier.height(8.dp))

                if (state.experiments.isEmpty()) {
                    Text(
                        text = "No experiments yet",
                        style = TextStyle(fontSize = 13.sp, color = Color.Gray),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                    )
                } else {
                    Column {
                        state.experiments.forEach { experiment ->
                            ExperimentListItem(
                                experiment = experiment,
                                isSelected = state.selectedExperiment?.id == experiment.id,
                                onClick = { viewModel.selectExperiment(experiment) },
                                onDelete = { viewModel.deleteExperiment(experiment.id) }
                            )
                        }
                    }
                }
        }

        // Selected experiment detail
        state.selectedExperiment?.let { experiment ->
            Spacer(modifier = Modifier.height(16.dp))
            NeonCard(glowColor = TitanColors.NeonCyan) {
                    NeonSectionHeader(text = "EXPERIMENT DETAIL")
                    Spacer(modifier = Modifier.height(8.dp))
                    ExperimentDetailCard(experiment = experiment)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
    }

    if (showNewExperimentDialog) {
        NewExperimentDialog(
            onDismiss = { showNewExperimentDialog = false },
            onStartFreqResponse = { name, steps, dwell ->
                showNewExperimentDialog = false
                viewModel.startFrequencyResponseExperiment(name, steps = steps, dwellMs = dwell)
            },
            onStartDistortion = { name ->
                showNewExperimentDialog = false
                viewModel.startDistortionExperiment(name)
            }
        )
    }
}

@Composable
fun NewExperimentDialog(
    onDismiss: () -> Unit,
    onStartFreqResponse: (String, Int, Int) -> Unit,
    onStartDistortion: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var steps by remember { mutableStateOf("30") }
    var dwell by remember { mutableStateOf("300") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        title = { Text("New Experiment", color = TitanColors.NeonCyan) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = steps,
                    onValueChange = { steps = it },
                    label = { Text("Steps") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = dwell,
                    onValueChange = { dwell = it },
                    label = { Text("Dwell (ms)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onStartFreqResponse(name.ifEmpty { "Freq Response" }, steps.toIntOrNull() ?: 30, dwell.toIntOrNull() ?: 300) },
                    colors = ButtonDefaults.buttonColors(containerColor = TitanColors.RadioactiveGreen)
                ) { Text("Freq Response", color = TitanColors.AbsoluteBlack) }
                Button(
                    onClick = { onStartDistortion(name.ifEmpty { "Distortion" }) },
                    colors = ButtonDefaults.buttonColors(containerColor = TitanColors.NeonOrange)
                ) { Text("Distortion", color = Color.White) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        }
    )
}

@Composable
fun ExperimentListItem(
    experiment: AcousticExperiment,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val color = when (experiment.status) {
        ExperimentStatus.COMPLETED -> TitanColors.RadioactiveGreen
        ExperimentStatus.RUNNING -> TitanColors.NeonYellow
        ExperimentStatus.FAILED -> Color(0xFFFF1744)
        ExperimentStatus.ABORTED -> TitanColors.NeonOrange
        else -> Color.Gray
    }

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
        color = if (isSelected) color.copy(alpha = 0.1f) else Color(0xFF1A1A1A),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = experiment.name,
                    style = TextStyle(fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${experiment.type.name} • ${experiment.status.name} • ${experiment.observations.size} obs",
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
fun ExperimentDetailCard(experiment: AcousticExperiment) {
    Column {
        SharedDetailRow("Name", experiment.name)
        SharedDetailRow("Type", experiment.type.name)
        SharedDetailRow("Status", experiment.status.name)
        SharedDetailRow("Protocol", "v${experiment.protocolVersion}")
        SharedDetailRow("Steps", "${experiment.currentStep}/${experiment.stepCount}")
        SharedDetailRow("Dwell", "${experiment.dwellMs}ms")
        SharedDetailRow("Repeats", experiment.repeatsPerStep.toString())
        SharedDetailRow("Observations", experiment.observations.size.toString())

        // Experiment Variables
        if (experiment.variables.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("VARIABLES", style = TextStyle(fontSize = 9.sp, color = TitanColors.NeonCyan.copy(alpha = 0.7f), fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(4.dp))
            experiment.variables.forEach { variable ->
                SharedDetailRow(
                    variable.name,
                    "${String.format("%.1f", variable.min)} - ${String.format("%.1f", variable.max)} ${variable.unit}"
                )
            }
        }

        experiment.result?.let { result ->
            Spacer(modifier = Modifier.height(12.dp))
            Text("RESULTS", style = TextStyle(fontSize = 10.sp, color = TitanColors.RadioactiveGreen, fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
            Spacer(modifier = Modifier.height(8.dp))

            // Main metrics
            NeonCard(glowColor = TitanColors.RadioactiveGreen.copy(alpha = 0.5f)) {
                SharedDetailRow("Peak Gain", "${String.format("%.2f", result.peakGainDb)} dB")
                SharedDetailRow("RMS Gain", "${String.format("%.2f", result.rmsGainDb)} dB")
                SharedDetailRow("Duration", "${result.durationMs / 1000}s")
                result.thdPercent?.let { thd ->
                    SharedDetailRow("THD", "${String.format("%.3f", thd)}%")
                }
            }

            // Summary
            Spacer(modifier = Modifier.height(8.dp))
            Text("SUMMARY", style = TextStyle(fontSize = 9.sp, color = TitanColors.NeonCyan.copy(alpha = 0.7f), fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(4.dp))
            Text(result.summary, style = TextStyle(fontSize = 11.sp, color = Color.Gray))

            // Observations preview
            if (result.observations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("OBSERVATIONS (${result.observations.size})", style = TextStyle(fontSize = 9.sp, color = TitanColors.NeonCyan.copy(alpha = 0.7f), fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(4.dp))
                Column {
                    result.observations.takeLast(5).reversed().forEach { obs ->
                        Text(
                            text = "Step ${obs.variable}: ${obs.frequencyHz}Hz → Peak=${String.format("%.4f", obs.measuredPeak)} RMS=${String.format("%.4f", obs.measuredRms)}",
                            style = TextStyle(fontSize = 9.sp, color = Color.Gray)
                        )
                    }
                    if (result.observations.size > 5) {
                        Text(
                            text = "... and ${result.observations.size - 5} more observations",
                            style = TextStyle(fontSize = 9.sp, color = Color.Gray.copy(alpha = 0.7f))
                        )
                    }
                }
            }
        }

        experiment.errorMessage?.let { err ->
            Spacer(modifier = Modifier.height(4.dp))
            Text("ERROR: $err", style = TextStyle(fontSize = 9.sp, color = Color(0xFFFF1744)))
        }
    }
}

