package com.supremecorp.bass

import android.content.Context
import android.content.SharedPreferences

object AudioStatePersistence {
    private const val PREFS_NAME = "supreme_bass_audio_state"
    private const val KEY_IS_ENABLED = "is_enabled"
    private const val KEY_GAIN_VALUE = "gain_value"
    private const val KEY_BASS_BOOST = "bass_boost"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_IS_ENABLED, enabled).apply()
    }

    fun saveGain(context: Context, gain: Float) {
        prefs(context).edit().putFloat(KEY_GAIN_VALUE, gain).apply()
    }

    fun saveBassBoost(context: Context, boost: Int) {
        prefs(context).edit().putInt(KEY_BASS_BOOST, boost).apply()
    }

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_IS_ENABLED, false)

    fun gainValue(context: Context): Float =
        prefs(context).getFloat(KEY_GAIN_VALUE, 0f)

    fun bassBoost(context: Context): Int =
        prefs(context).getInt(KEY_BASS_BOOST, 0)

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
