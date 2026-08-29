package com.supremecorp.bass.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supremecorp.bass.dsp.ParametricEQ
import com.supremecorp.bass.ui.signal.DSPControlsState
import com.supremecorp.bass.ui.theme.TitanColors

/**
 * DSP Controls panel with EQ, Bass Boost, and Virtualizer controls.
 */
@Composable
fun DSPControlsPanel(
    state: DSPControlsState,
    onBassBoostChanged: (Float) -> Unit,
    onBassCutoffChanged: (Double) -> Unit,
    onBassEnabledChanged: (Boolean) -> Unit,
    onEQBandChanged: (Int, Double) -> Unit,
    onEQEnabledChanged: (Boolean) -> Unit,
    onEQPresetChanged: (ParametricEQ.EQPreset) -> Unit,
    onVirtualizerWidthChanged: (Float) -> Unit,
    onVirtualizerCrossfeedChanged: (Float) -> Unit,
    onVirtualizerEnabledChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // DSP Status
        NeonCard(glowColor = TitanColors.NeonGreen) {
            NeonSectionHeader(text = "DSP STATUS")
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusItem("Process", "${state.dspProcessTimeUs}μs")
                StatusItem("Clips", "${state.clippedSamples}")
            }
        }

        // Bass Boost
        NeonCard(glowColor = Color(0xFFFF00FF)) {
            NeonSectionHeader(text = "BASS BOOST")

            Spacer(modifier = Modifier.height(8.dp))

            NeonSwitchRow(
                checked = state.bassEnabled,
                onCheckedChange = onBassEnabledChanged,
                label = "Enabled"
            )

            if (state.bassEnabled) {
                NeonSlider(
                    value = state.bassBoostDb,
                    onValueChange = onBassBoostChanged,
                    valueRange = 0f..12f
                )

                Text(
                    text = "Boost: ${String.format("%.1f", state.bassBoostDb)} dB",
                    style = TextStyle(color = Color.Gray, fontSize = 10.sp)
                )

                NeonSlider(
                    value = state.bassCutoffHz.toFloat(),
                    onValueChange = { onBassCutoffChanged(it.toDouble()) },
                    valueRange = 50f..300f
                )

                Text(
                    text = "Cutoff: ${String.format("%.0f", state.bassCutoffHz)} Hz",
                    style = TextStyle(color = Color.Gray, fontSize = 10.sp)
                )
            }
        }

        // EQ
        NeonCard(glowColor = TitanColors.NeonCyan) {
            NeonSectionHeader(text = "10-BAND EQ")

            Spacer(modifier = Modifier.height(8.dp))

            NeonSwitchRow(
                checked = state.eqEnabled,
                onCheckedChange = onEQEnabledChanged,
                label = "Enabled"
            )

            if (state.eqEnabled) {
                // Preset selector
                Text(
                    text = "Preset: ${state.eqPreset.name}",
                    style = TextStyle(color = Color.Gray, fontSize = 12.sp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ParametricEQ.EQPreset.entries.take(5).forEach { preset ->
                        NeonChip(
                            text = preset.name.take(4),
                            selected = state.eqPreset == preset,
                            onClick = { onEQPresetChanged(preset) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // EQ Curve Visualization
                EQCurveVisualization(
                    eqGains = state.eqBands,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Band sliders
                val bandLabels = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")
                for (i in state.eqBands.indices) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = bandLabels[i],
                            style = TextStyle(
                                color = Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.width(30.dp)
                        )

                        NeonSlider(
                            value = state.eqBands[i].toFloat(),
                            onValueChange = { onEQBandChanged(i, it.toDouble()) },
                            valueRange = -12f..12f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Virtualizer
        NeonCard(glowColor = TitanColors.NeonYellow) {
            NeonSectionHeader(text = "VIRTUALIZER")

            Spacer(modifier = Modifier.height(8.dp))

            NeonSwitchRow(
                checked = state.virtualizerEnabled,
                onCheckedChange = onVirtualizerEnabledChanged,
                label = "Enabled"
            )

            if (state.virtualizerEnabled) {
                NeonSlider(
                    value = state.virtualizerWidth,
                    onValueChange = onVirtualizerWidthChanged,
                    valueRange = 0f..1f
                )

                Text(
                    text = "Width: ${String.format("%.0f", state.virtualizerWidth * 100)}%",
                    style = TextStyle(color = Color.Gray, fontSize = 10.sp)
                )

                NeonSlider(
                    value = state.virtualizerCrossfeed,
                    onValueChange = onVirtualizerCrossfeedChanged,
                    valueRange = 0f..1f
                )

                Text(
                    text = "Crossfeed: ${String.format("%.0f", state.virtualizerCrossfeed * 100)}%",
                    style = TextStyle(color = Color.Gray, fontSize = 10.sp)
                )
            }
        }
    }
}

@Composable
private fun NeonSwitchRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = TextStyle(color = Color.Gray, fontSize = 12.sp)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TitanColors.NeonCyan,
                checkedTrackColor = TitanColors.NeonCyan.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun StatusItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = TextStyle(
                color = TitanColors.NeonGreen,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = label,
            style = TextStyle(
                color = Color.Gray,
                fontSize = 10.sp
            )
        )
    }
}
