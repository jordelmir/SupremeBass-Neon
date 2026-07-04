package com.supremecorp.bass.domain.model

enum class MusicGenre(val displayName: String) {
    EDM("EDM"), HIP_HOP("Hip-Hop"), REGGAETON("Reggaeton"),
    ROCK("Rock"), METAL("Metal"), JAZZ("Jazz"),
    CLASSICAL("Classical"), PODCAST("Podcast"), GAMING("Gaming"),
    MOVIES("Movies"), POP("Pop"), RNB("R&B")
}

enum class OutputDevice(val displayName: String) {
    HEADPHONES("Headphones"), BLUETOOTH_SPEAKER("Bluetooth Speaker"),
    CAR_AUDIO("Car Audio"), PHONE_SPEAKER("Phone Speaker"),
    HOME_THEATER("Home Theater"), WIRELESS_EARBUDS("Wireless Earbuds")
}

enum class SoundPreference(val displayName: String) {
    MORE_BASS("More Bass"), VOCAL_CLARITY("Vocal Clarity"),
    LESS_DISTORTION("Less Distortion"), LOW_VOLUME("Low Volume"),
    NIGHT_MODE("Night Mode"), MAX_POWER("Max Power"),
    BALANCED("Balanced")
}

enum class PresetIntensity(val displayName: String, val factor: Float) {
    SAFE("Safe", 0.5f), BALANCED("Balanced", 0.75f),
    EXTREME("Extreme Control", 1.0f)
}

data class AiPresetInput(
    val genre: MusicGenre,
    val outputDevice: OutputDevice,
    val preference: SoundPreference,
    val intensity: PresetIntensity,
    val userNotes: String? = null
)

data class EqualizerBand(val hz: Int, val gainDb: Float)

data class EqualizerPreset(
    val name: String,
    val bands: List<EqualizerBand>,
    val bassBoost: Int,
    val virtualizer: Int,
    val loudness: Int
)

data class AiPresetResult(
    val preset: EqualizerPreset,
    val explanation: String,
    val safety: PresetSafetyResult,
    val source: AiSource
)

enum class AiSource { LOCAL_RULE_ENGINE, REMOTE_LLM, FALLBACK }

enum class RiskLevel { LOW, MEDIUM, HIGH, BLOCKED }

data class PresetSafetyResult(
    val level: RiskLevel,
    val warnings: List<String>,
    val canApply: Boolean
)

data class AiExplanationResult(
    val explanation: String,
    val frequencyBreakdown: List<String>,
    val risks: List<String>
)

data class AiOutputReport(
    val presetName: String,
    val issue: String,
    val timestamp: Long = System.currentTimeMillis()
)
