package com.supremecorp.bass.dsp.eq

data class EqualizerPreset(
    val name: String,
    val description: String,
    val bandGains: FloatArray,
    val bassBoost: Float = 0f,
    val trebleBoost: Float = 0f
) {
    init {
        require(bandGains.size == 10) { "bandGains must have 10 elements" }
    }

    companion object {
        val FLAT = EqualizerPreset(
            name = "Flat",
            description = "Neutral, uncolored response",
            bandGains = FloatArray(10) { 0f }
        )

        val BASS_BOOST = EqualizerPreset(
            name = "Bass Boost",
            description = "Enhanced low-end response",
            bandGains = floatArrayOf(12f, 10f, 8f, 5f, 2f, 0f, 0f, 0f, 0f, 0f),
            bassBoost = 12f
        )

        val TREBLE_BOOST = EqualizerPreset(
            name = "Treble Boost",
            description = "Enhanced high-end clarity",
            bandGains = floatArrayOf(0f, 0f, 0f, 0f, 0f, 2f, 5f, 8f, 10f, 12f),
            trebleBoost = 12f
        )

        val VOCAL = EqualizerPreset(
            name = "Vocal",
            description = "Crisp, clear vocals",
            bandGains = floatArrayOf(-2f, 0f, 2f, 4f, 6f, 6f, 4f, 2f, 0f, -2f)
        )

        val ROCK = EqualizerPreset(
            name = "Rock",
            description = "Punchy mids and tight bass",
            bandGains = floatArrayOf(6f, 4f, 2f, 0f, -2f, -2f, 0f, 4f, 6f, 8f)
        )

        val ELECTRONIC = EqualizerPreset(
            name = "Electronic",
            description = "Deep sub-bass and sparkling highs",
            bandGains = floatArrayOf(10f, 8f, 4f, 0f, -2f, -2f, 0f, 4f, 8f, 10f)
        )

        val ACOUSTIC = EqualizerPreset(
            name = "Acoustic",
            description = "Natural, warm tone",
            bandGains = floatArrayOf(4f, 3f, 2f, 1f, 0f, 0f, 1f, 2f, 3f, 4f)
        )

        val PODCAST = EqualizerPreset(
            name = "Podcast",
            description = "Optimized for speech clarity",
            bandGains = floatArrayOf(-4f, -2f, 0f, 2f, 6f, 8f, 6f, 2f, 0f, -2f)
        )

        val HIP_HOP = EqualizerPreset(
            name = "Hip Hop",
            description = "Heavy bass with crisp highs",
            bandGains = floatArrayOf(10f, 8f, 6f, 2f, 0f, 0f, 2f, 4f, 6f, 8f)
        )

        val JAZZ = EqualizerPreset(
            name = "Jazz",
            description = "Warm, smooth, natural",
            bandGains = floatArrayOf(4f, 3f, 1f, 2f, -1f, -1f, 0f, 2f, 3f, 4f)
        )

        val CLASSICAL = EqualizerPreset(
            name = "Classical",
            description = "Wide dynamic range",
            bandGains = floatArrayOf(6f, 4f, 2f, 0f, -1f, -1f, 0f, 2f, 4f, 6f)
        )

        val ALL_PRESETS = listOf(
            FLAT, BASS_BOOST, TREBLE_BOOST, VOCAL, ROCK,
            ELECTRONIC, ACOUSTIC, PODCAST, HIP_HOP, JAZZ, CLASSICAL
        )

        fun getByName(name: String): EqualizerPreset {
            return ALL_PRESETS.find { it.name == name } ?: FLAT
        }
    }

    fun applyTo(bandLevels: ShortArray): ShortArray {
        require(bandLevels.size == 10) { "bandLevels must have 10 elements" }
        return ShortArray(10) { i ->
            (bandGains[i] * 100).toInt().toShort()
        }
    }
}
