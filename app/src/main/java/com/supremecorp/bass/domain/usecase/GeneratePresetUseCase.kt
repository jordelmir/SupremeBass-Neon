package com.supremecorp.bass.domain.usecase

import com.supremecorp.bass.domain.model.*

interface GeneratePresetUseCase {
    suspend fun generate(input: AiPresetInput): AiPresetResult
}

interface ExplainPresetUseCase {
    suspend fun explain(preset: EqualizerPreset): AiExplanationResult
}

interface ValidatePresetSafetyUseCase {
    fun validate(preset: EqualizerPreset): PresetSafetyResult
}
