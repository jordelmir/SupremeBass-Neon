package com.supreme.android

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

object AudioStatePersistence {
    private const val TAG = "SupremeBass_Persistence"
    private const val PREFS_NAME = "supreme_bass_audio_state"
    private const val KEY_IS_ENABLED = "is_enabled"
    private const val KEY_GAIN_VALUE = "gain_value"
    private const val KEY_BASS_BOOST = "bass_boost"
    private const val KEY_DISCLAIMER_ACCEPTED = "disclaimer_accepted"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveEnabled(context: Context, enabled: Boolean) {
        Log.d(TAG, "saveEnabled: $enabled")
        prefs(context).edit().putBoolean(KEY_IS_ENABLED, enabled).apply()
    }

    fun saveGain(context: Context, gain: Float) {
        Log.d(TAG, "saveGain: $gain")
        prefs(context).edit().putFloat(KEY_GAIN_VALUE, gain).apply()
    }

    fun saveBassBoost(context: Context, boost: Int) {
        Log.d(TAG, "saveBassBoost: $boost")
        prefs(context).edit().putInt(KEY_BASS_BOOST, boost).apply()
    }

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_IS_ENABLED, false)

    fun gainValue(context: Context): Float =
        prefs(context).getFloat(KEY_GAIN_VALUE, 0f)

    fun bassBoost(context: Context): Int =
        prefs(context).getInt(KEY_BASS_BOOST, 0)

    fun hasAcceptedDisclaimer(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DISCLAIMER_ACCEPTED, false)

    fun saveDisclaimerAccepted(context: Context, accepted: Boolean) {
        Log.i(TAG, "Disclaimer accepted: $accepted")
        prefs(context).edit().putBoolean(KEY_DISCLAIMER_ACCEPTED, accepted).apply()
    }

    fun clear(context: Context) {
        Log.w(TAG, "Clearing all audio state")
        prefs(context).edit().clear().apply()
    }
}
