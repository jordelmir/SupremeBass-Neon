package com.supremecorp.bass.ui.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supremecorp.bass.dsp.eq.EqualizerPreset
import com.supremecorp.bass.ui.components.NeonScreenTitle
import com.supremecorp.bass.ui.components.NeonCard
import com.supremecorp.bass.ui.components.NeonSectionHeader
import com.supremecorp.bass.ui.components.MatrixRain
import com.supremecorp.bass.ui.theme.TitanColors

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    var useOboe by remember { mutableStateOf(AudioSettingsPrefs.useOboe(context)) }
    var routeInterlock by remember { mutableStateOf(AudioSettingsPrefs.routeInterlock(context)) }
    var amplitudeLimit by remember { mutableStateOf(AudioSettingsPrefs.amplitudeLimit(context)) }
    var durationLimit by remember { mutableStateOf(AudioSettingsPrefs.durationLimit(context)) }
    var pcmFloat by remember { mutableStateOf(AudioSettingsPrefs.pcmFloat(context)) }

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
        NeonScreenTitle(title = "SETTINGS")

        // Audio engine settings
        SettingsSection("AUDIO ENGINE") {
            SettingsToggle(
                title = "Oboe native backend",
                description = "Use C++ Oboe for lower latency (experimental)",
                checked = useOboe,
                onCheckedChange = {
                    useOboe = it
                    AudioSettingsPrefs.setUseOboe(context, it)
                }
            )
            SettingsToggle(
                title = "PCM Float",
                description = "Use 32-bit float encoding",
                checked = pcmFloat,
                onCheckedChange = {
                    pcmFloat = it
                    AudioSettingsPrefs.setPcmFloat(context, it)
                }
            )
            SettingsInfo("Default sample rate", "48 kHz")
            SettingsInfo("Oboe backend status", if (useOboe) "Native C++" else "AudioTrack (Java)")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Equalizer presets
        SettingsSection("EQUALIZER PRESETS") {
            var selectedPreset by remember { mutableStateOf(AudioSettingsPrefs.eqPreset(context)) }

            EqualizerPreset.ALL_PRESETS.forEach { preset ->
                SettingsPresetItem(
                    name = preset.name,
                    description = preset.description,
                    isSelected = selectedPreset == preset.name,
                    onClick = {
                        selectedPreset = preset.name
                        AudioSettingsPrefs.setEqPreset(context, preset.name)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Safety settings
        SettingsSection("SAFETY") {
            SettingsToggle(
                title = "Route interlock",
                description = "Block unsafe audio routes",
                checked = routeInterlock,
                onCheckedChange = {
                    routeInterlock = it
                    AudioSettingsPrefs.setRouteInterlock(context, it)
                }
            )
            SettingsToggle(
                title = "Amplitude limit",
                description = "Clamp max amplitude to 80%",
                checked = amplitudeLimit,
                onCheckedChange = {
                    amplitudeLimit = it
                    AudioSettingsPrefs.setAmplitudeLimit(context, it)
                }
            )
            SettingsToggle(
                title = "Duration limit",
                description = "Max 30s continuous",
                checked = durationLimit,
                onCheckedChange = {
                    durationLimit = it
                    AudioSettingsPrefs.setDurationLimit(context, it)
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // About
        SettingsSection("ABOUT") {
            SettingsInfo("Version", "2.0.0-alpha")
            SettingsInfo("Build", "Signal Engine V1 + Oboe C++")
            SettingsInfo("Architecture", "Supreme Acoustics")
            SettingsInfo("Native DSP", "C++17 (Oboe 1.8.0)")
        }

        Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    NeonSectionHeader(text = title)
    NeonCard(modifier = Modifier.fillMaxWidth()) {
        content()
    }
}

@Composable
fun SettingsToggle(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = TextStyle(fontSize = 13.sp, color = Color.White))
            Text(description, style = TextStyle(fontSize = 10.sp, color = Color.Gray))
        }
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
fun SettingsInfo(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = TextStyle(fontSize = 13.sp, color = Color.White))
        Text(value, style = TextStyle(fontSize = 13.sp, color = TitanColors.NeonCyan, fontWeight = FontWeight.Medium))
    }
}

@Composable
fun SettingsPresetItem(name: String, description: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) TitanColors.NeonCyan.copy(alpha = 0.1f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isSelected) TitanColors.NeonCyan else Color.Gray)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                name,
                style = TextStyle(
                    fontSize = 13.sp,
                    color = if (isSelected) TitanColors.NeonCyan else Color.White,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            )
            Text(description, style = TextStyle(fontSize = 10.sp, color = Color.Gray))
        }
    }
}

object AudioSettingsPrefs {
    private const val PREFS = "supreme_audio_settings"
    private const val KEY_OBOE = "use_oboe"
    private const val KEY_ROUTE_INTERLOCK = "route_interlock"
    private const val KEY_AMPLITUDE_LIMIT = "amplitude_limit"
    private const val KEY_DURATION_LIMIT = "duration_limit"
    private const val KEY_PCM_FLOAT = "pcm_float"
    private const val KEY_EQ_PRESET = "eq_preset"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun useOboe(context: Context) = prefs(context).getBoolean(KEY_OBOE, false)
    fun setUseOboe(context: Context, value: Boolean) = prefs(context).edit().putBoolean(KEY_OBOE, value).apply()

    fun routeInterlock(context: Context) = prefs(context).getBoolean(KEY_ROUTE_INTERLOCK, true)
    fun setRouteInterlock(context: Context, value: Boolean) = prefs(context).edit().putBoolean(KEY_ROUTE_INTERLOCK, value).apply()

    fun amplitudeLimit(context: Context) = prefs(context).getBoolean(KEY_AMPLITUDE_LIMIT, true)
    fun setAmplitudeLimit(context: Context, value: Boolean) = prefs(context).edit().putBoolean(KEY_AMPLITUDE_LIMIT, value).apply()

    fun durationLimit(context: Context) = prefs(context).getBoolean(KEY_DURATION_LIMIT, true)
    fun setDurationLimit(context: Context, value: Boolean) = prefs(context).edit().putBoolean(KEY_DURATION_LIMIT, value).apply()

    fun pcmFloat(context: Context) = prefs(context).getBoolean(KEY_PCM_FLOAT, true)
    fun setPcmFloat(context: Context, value: Boolean) = prefs(context).edit().putBoolean(KEY_PCM_FLOAT, value).apply()

    fun eqPreset(context: Context) = prefs(context).getString(KEY_EQ_PRESET, "Flat") ?: "Flat"
    fun setEqPreset(context: Context, value: String) = prefs(context).edit().putString(KEY_EQ_PRESET, value).apply()
}
