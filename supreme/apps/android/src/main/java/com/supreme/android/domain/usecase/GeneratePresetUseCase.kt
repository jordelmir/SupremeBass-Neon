package com.supreme.android.domain.usecase

import com.supreme.android.domain.model.*

interface GeneratePresetUseCase {
    suspend fun generate(input: AiPresetInput): AiPresetResult
}

interface ExplainPresetUseCase {
    suspend fun explain(preset: EqualizerPreset): AiExplanationResult
}

interface ValidatePresetSafetyUseCase {
    fun validate(preset: EqualizerPreset): PresetSafetyResult
}
