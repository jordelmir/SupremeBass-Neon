package com.supremecorp.bass.data.ads

import android.content.Context
import android.content.SharedPreferences

class AdFrequencyController(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ad_frequency", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_INTERSTITIAL_LAST_SHOWN = "interstitial_last"
        private const val KEY_INTERSTITIAL_COUNT = "interstitial_count"
        private const val KEY_INTERSTITIAL_ACTION_COUNT = "interstitial_actions"
        private const val KEY_APP_OPEN_LAST_SHOWN = "app_open_last"
        
        private const val INTERSTITIAL_MIN_INTERVAL_MS = 3 * 60 * 1000L // 3 minutes
        private const val INTERSTITIAL_MAX_ACTIONS = 5
        private const val APP_OPEN_MIN_INTERVAL_MS = 4 * 60 * 60 * 1000L // 4 hours
    }
    
    fun canShowInterstitial(): Boolean {
        val lastShown = prefs.getLong(KEY_INTERSTITIAL_LAST_SHOWN, 0)
        val now = System.currentTimeMillis()
        return (now - lastShown) >= INTERSTITIAL_MIN_INTERVAL_MS
    }
    
    fun recordInterstitialShown() {
        prefs.edit()
            .putLong(KEY_INTERSTITIAL_LAST_SHOWN, System.currentTimeMillis())
            .putInt(KEY_INTERSTITIAL_COUNT, prefs.getInt(KEY_INTERSTITIAL_COUNT, 0) + 1)
            .putInt(KEY_INTERSTITIAL_ACTION_COUNT, 0)
            .apply()
    }
    
    fun incrementActionCount(): Boolean {
        val count = prefs.getInt(KEY_INTERSTITIAL_ACTION_COUNT, 0) + 1
        prefs.edit().putInt(KEY_INTERSTITIAL_ACTION_COUNT, count).apply()
        return count >= INTERSTITIAL_MAX_ACTIONS
    }
    
    fun canShowAppOpen(): Boolean {
        val lastShown = prefs.getLong(KEY_APP_OPEN_LAST_SHOWN, 0)
        return (System.currentTimeMillis() - lastShown) >= APP_OPEN_MIN_INTERVAL_MS
    }
    
    fun recordAppOpenShown() {
        prefs.edit().putLong(KEY_APP_OPEN_LAST_SHOWN, System.currentTimeMillis()).apply()
    }
}
