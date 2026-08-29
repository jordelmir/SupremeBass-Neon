package com.supremecorp.bass.domain.model

data class SignalTelemetry(
    val sessionId: String,
    val timestamp: Long,
    val manufacturer: String,
    val model: String,
    val androidVersion: Int,
    val sampleRate: Int,
    val encoding: String,
    val bufferFrames: Int,
    val waveform: Waveform,
    val frequencyHz: Double,
    val amplitude: Float,
    val peak: Float,
    val rms: Float,
    val durationMs: Long,
    val audioRoute: OutputRoute,
    val underruns: Int,
    val terminationReason: String
)
