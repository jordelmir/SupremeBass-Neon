package com.supremecorp.bass.data.ai

import com.supremecorp.bass.domain.model.*

class AiSafetyFilter {
    
    fun sanitizeInput(input: AiPresetInput): AiPresetInput {
        return input.copy(
            userNotes = input.userNotes?.take(200)?.trim()
        )
    }
    
    fun clampPreset(preset: EqualizerPreset): EqualizerPreset {
        return preset.copy(
            bands = preset.bands.map { band ->
                EqualizerBand(band.hz, band.gainDb.coerceIn(-12f, 12f))
            },
            bassBoost = preset.bassBoost.coerceIn(0, 1000),
            virtualizer = preset.virtualizer.coerceIn(0, 1000),
            loudness = preset.loudness.coerceIn(0, 1000)
        )
    }
    
    fun isPromptSafe(notes: String?): Boolean {
        if (notes.isNullOrBlank()) return true
        val lower = notes.lowercase()
        val blocked = listOf("ignore", "override", "bypass", "hack", "inject", "<script", "drop table")
        return blocked.none { lower.contains(it) }
    }
}
