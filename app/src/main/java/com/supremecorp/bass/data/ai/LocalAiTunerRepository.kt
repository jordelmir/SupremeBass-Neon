package com.supremecorp.bass.data.ai

import com.supremecorp.bass.domain.model.*
import com.supremecorp.bass.core.logging.AppLogger

class LocalAiTunerRepository {
    
    fun generatePreset(input: AiPresetInput): AiPresetResult {
        val bands = calculateBands(input)
        val bassBoost = calculateBassBoost(input)
        val virtualizer = calculateVirtualizer(input)
        val loudness = calculateLoudness(input)
        
        val preset = EqualizerPreset(
            name = generatePresetName(input),
            bands = bands,
            bassBoost = bassBoost,
            virtualizer = virtualizer,
            loudness = loudness
        )
        
        val safety = validateSafety(preset, input)
        val explanation = generateExplanation(input, preset)
        
        return AiPresetResult(
            preset = preset,
            explanation = explanation,
            safety = safety,
            source = AiSource.LOCAL_RULE_ENGINE
        )
    }
    
    private fun calculateBands(input: AiPresetInput): List<EqualizerBand> {
        val base = when (input.genre) {
            MusicGenre.EDM -> listOf(6f, 4f, 0f, 2f, 3f)
            MusicGenre.HIP_HOP -> listOf(7f, 3f, -1f, 1f, 2f)
            MusicGenre.REGGAETON -> listOf(6f, 4f, 0f, 2f, 2f)
            MusicGenre.ROCK -> listOf(3f, 2f, 4f, 3f, 1f)
            MusicGenre.METAL -> listOf(4f, 1f, 5f, 4f, 2f)
            MusicGenre.JAZZ -> listOf(2f, 3f, 4f, 2f, 1f)
            MusicGenre.CLASSICAL -> listOf(1f, 2f, 3f, 2f, 1f)
            MusicGenre.PODCAST -> listOf(0f, 3f, 5f, 3f, 0f)
            MusicGenre.GAMING -> listOf(3f, 2f, 4f, 5f, 3f)
            MusicGenre.MOVIES -> listOf(5f, 2f, 3f, 4f, 3f)
            MusicGenre.POP -> listOf(3f, 3f, 2f, 3f, 2f)
            MusicGenre.RNB -> listOf(5f, 3f, 1f, 2f, 2f)
        }
        
        val deviceMultiplier = when (input.outputDevice) {
            OutputDevice.PHONE_SPEAKER -> 0.4f
            OutputDevice.HEADPHONES -> 1.0f
            OutputDevice.WIRELESS_EARBUDS -> 0.8f
            OutputDevice.BLUETOOTH_SPEAKER -> 0.9f
            OutputDevice.CAR_AUDIO -> 1.1f
            OutputDevice.HOME_THEATER -> 1.0f
        }
        
        val prefAdjustment = when (input.preference) {
            SoundPreference.MORE_BASS -> listOf(2f, 0f, 0f, 0f, 0f)
            SoundPreference.VOCAL_CLARITY -> listOf(0f, 0f, 2f, 2f, 0f)
            SoundPreference.LESS_DISTORTION -> listOf(-1f, 0f, 0f, 0f, -1f)
            SoundPreference.LOW_VOLUME -> listOf(1f, 1f, 1f, 1f, 1f)
            SoundPreference.NIGHT_MODE -> listOf(2f, 0f, -1f, 0f, -1f)
            SoundPreference.MAX_POWER -> listOf(3f, 1f, 1f, 1f, 2f)
            SoundPreference.BALANCED -> listOf(0f, 0f, 0f, 0f, 0f)
        }
        
        val hzValues = listOf(60, 230, 910, 3600, 14000)
        return base.mapIndexed { i, gain ->
            val adjusted = (gain * deviceMultiplier + prefAdjustment[i]) * input.intensity.factor
            EqualizerBand(hzValues[i], adjusted.coerceIn(-12f, 12f))
        }
    }
    
    private fun calculateBassBoost(input: AiPresetInput): Int {
        val base = when (input.preference) {
            SoundPreference.MORE_BASS -> 80
            SoundPreference.MAX_POWER -> 90
            SoundPreference.NIGHT_MODE -> 40
            SoundPreference.LESS_DISTORTION -> 30
            SoundPreference.VOCAL_CLARITY -> 20
            SoundPreference.LOW_VOLUME -> 30
            SoundPreference.BALANCED -> 50
        }
        val deviceMultiplier = when (input.outputDevice) {
            OutputDevice.PHONE_SPEAKER -> 0.3f
            OutputDevice.HEADPHONES -> 1.0f
            OutputDevice.WIRELESS_EARBUDS -> 0.7f
            OutputDevice.BLUETOOTH_SPEAKER -> 0.8f
            OutputDevice.CAR_AUDIO -> 1.0f
            OutputDevice.HOME_THEATER -> 0.9f
        }
        return (base * deviceMultiplier * input.intensity.factor).toInt().coerceIn(0, 1000)
    }
    
    private fun calculateVirtualizer(input: AiPresetInput): Int {
        return when (input.outputDevice) {
            OutputDevice.HEADPHONES -> 60
            OutputDevice.WIRELESS_EARBUDS -> 50
            OutputDevice.PHONE_SPEAKER -> 30
            else -> 40
        }
    }
    
    private fun calculateLoudness(input: AiPresetInput): Int {
        val base = when (input.preference) {
            SoundPreference.MAX_POWER -> 70
            SoundPreference.NIGHT_MODE -> 20
            SoundPreference.LESS_DISTORTION -> 30
            SoundPreference.LOW_VOLUME -> 40
            else -> 50
        }
        return (base * input.intensity.factor).toInt().coerceIn(0, 1000)
    }
    
    private fun generatePresetName(input: AiPresetInput): String {
        val genrePart = when (input.genre) {
            MusicGenre.EDM -> "Club"
            MusicGenre.HIP_HOP -> "Trap"
            MusicGenre.REGGAETON -> "Latin"
            MusicGenre.ROCK -> "Crunch"
            MusicGenre.METAL -> "Metal"
            MusicGenre.JAZZ -> "Smooth"
            MusicGenre.CLASSICAL -> "Orchestra"
            MusicGenre.PODCAST -> "Voice"
            MusicGenre.GAMING -> "Game"
            MusicGenre.MOVIES -> "Cinema"
            MusicGenre.POP -> "Pop"
            MusicGenre.RNB -> "Soul"
        }
        val prefPart = when (input.preference) {
            SoundPreference.MORE_BASS -> "Deep"
            SoundPreference.VOCAL_CLARITY -> "Clean"
            SoundPreference.MAX_POWER -> "Power"
            SoundPreference.NIGHT_MODE -> "Night"
            SoundPreference.BALANCED -> "Balance"
            SoundPreference.LOW_VOLUME -> "Warm"
            SoundPreference.LESS_DISTORTION -> "Pure"
        }
        return "$genrePart $prefPart"
    }
    
    private fun validateSafety(preset: EqualizerPreset, input: AiPresetInput): PresetSafetyResult {
        val warnings = mutableListOf<String>()
        var level = RiskLevel.LOW
        
        val positiveGainSum = preset.bands.sumOf { it.gainDb.toDouble() }.toFloat()
        if (positiveGainSum > 20f) {
            warnings.add("High total gain may cause distortion")
            level = RiskLevel.MEDIUM
        }
        
        if (preset.bassBoost > 75 && preset.loudness > 70) {
            warnings.add("High bass + loudness combo risks speaker damage")
            level = RiskLevel.HIGH
        }
        
        val lowFreqGain = preset.bands.firstOrNull()?.gainDb ?: 0f
        if (lowFreqGain > 8f) {
            warnings.add("Excessive low frequency boost")
            level = RiskLevel.HIGH
        }
        
        if (input.outputDevice == OutputDevice.PHONE_SPEAKER && preset.bassBoost > 60) {
            warnings.add("Phone speaker may distort at this bass level")
            level = RiskLevel.MEDIUM
        }
        
        if (input.preference == SoundPreference.NIGHT_MODE) {
            if (preset.loudness > 50) {
                warnings.add("Loudness reduced for night mode")
            }
        }
        
        val canApply = level != RiskLevel.BLOCKED
        
        return PresetSafetyResult(level = level, warnings = warnings, canApply = canApply)
    }
    
    private fun generateExplanation(input: AiPresetInput, preset: EqualizerPreset): String {
        return buildString {
            appendLine("🎵 ${input.genre.displayName} preset for ${input.outputDevice.displayName}")
            appendLine()
            appendLine("Bass: ${preset.bassBoost}% | Virtualizer: ${preset.virtualizer}% | Loudness: ${preset.loudness}%")
            appendLine()
            preset.bands.forEach { band ->
                val direction = if (band.gainDb > 0) "↑" else if (band.gainDb < 0) "↓" else "→"
                appendLine("${band.hz}Hz: ${String.format("%+.1f", band.gainDb)}dB $direction")
            }
        }
    }
}
