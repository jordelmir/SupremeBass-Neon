package com.supremecorp.bass.data.ads

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

object AdUnitProvider {
    private const val TAG = "SupremeBass_AdUnitProvider"

    private const val TEST_BANNER = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"

    private const val PROD_BANNER = "ca-app-pub-9295208787843008/6689805076"
    private const val PROD_INTERSTITIAL = "ca-app-pub-9295208787843008/2323184655"
    private const val PROD_REWARDED = "ca-app-pub-9295208787843008/6322973561"

    private fun isDebug(context: Context): Boolean {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )
            (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (_: Exception) {
            true
        }
    }

    fun bannerAdId(context: Context): String =
        if (isDebug(context)) TEST_BANNER else PROD_BANNER

    fun interstitialAdId(context: Context): String =
        if (isDebug(context)) TEST_INTERSTITIAL else PROD_INTERSTITIAL

    fun rewardedAdId(context: Context): String =
        if (isDebug(context)) TEST_REWARDED else PROD_REWARDED

    fun logAdIds(context: Context) {
        val debug = isDebug(context)
        Log.i(TAG, "Ad IDs mode: ${if (debug) "DEBUG" else "PRODUCTION"}")
        Log.d(TAG, "Banner: ${bannerAdId(context)}")
        Log.d(TAG, "Interstitial: ${interstitialAdId(context)}")
        Log.d(TAG, "Rewarded: ${rewardedAdId(context)}")
    }
}
