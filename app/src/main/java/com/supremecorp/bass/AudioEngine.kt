package com.supremecorp.bass

import android.content.Context
import android.media.audiofx.LoudnessEnhancer
import android.util.Log

class AudioEngine(private val context: Context) {
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private val SESSION_ID = 0 // Global Mix

    fun startSession() {
        try {
            loudnessEnhancer = LoudnessEnhancer(SESSION_ID)
            loudnessEnhancer?.enabled = true
            Log.d("SupremeBass", "Audio Session 0 Attached")
        } catch (e: Exception) {
            Log.e("SupremeBass", "Failed to attach to Session 0: ${e.message}")
            // Fallback strategy would go here (e.g., Equalizer)
        }
    }

    fun setGain(gainMboi: Int) {
        // Gain Input: 0 to 200 (DOUBLED RANGE)
        // Range: 100% to 300%
        // Multiplier: 60 millibels per unit
        // Max Output (200): 12000mB (+120dB). EXTREME hardware-breaking territory.
        // WARNING: Values above 100 (6000mB) can cause permanent speaker damage.
        
        val boostedGain = (gainMboi * 60)
        try {
            loudnessEnhancer?.setTargetGain(boostedGain)
        } catch (e: Exception) {
            Log.e("SupremeBass", "Failed to set gain: ${e.message}")
        }
    }

    fun stopSession() {
        loudnessEnhancer?.enabled = false
        loudnessEnhancer?.release()
        loudnessEnhancer = null
    }
}
