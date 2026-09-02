package com.supreme.android.ui.signal

import com.supreme.android.dsp.ParametricEQ

data class DSPControlsState(
    val bassBoostDb: Float = 0.0f,
    val bassCutoffHz: Double = 150.0,
    val bassEnabled: Boolean = true,
    val eqEnabled: Boolean = true,
    val eqPreset: ParametricEQ.EQPreset = ParametricEQ.EQPreset.FLAT,
    val eqBands: List<Double> = List(10) { 0.0 },
    val virtualizerEnabled: Boolean = true,
    val virtualizerWidth: Float = 0.5f,
    val virtualizerCrossfeed: Float = 0.0f,
    val dspProcessTimeUs: Long = 0,
    val clippedSamples: Int = 0
)
