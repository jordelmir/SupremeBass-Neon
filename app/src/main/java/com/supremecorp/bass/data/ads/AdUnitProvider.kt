package com.supremecorp.bass.data.ads

object AdUnitProvider {
    // Test IDs for debug
    private const val TEST_BANNER = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"
    
    // Production IDs - configure via BuildConfig or remote
    private const val PROD_BANNER = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"
    private const val PROD_INTERSTITIAL = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"
    private const val PROD_REWARDED = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"
    
    fun bannerAdId(isDebug: Boolean = true): String = if (isDebug) TEST_BANNER else PROD_BANNER
    fun interstitialAdId(isDebug: Boolean = true): String = if (isDebug) TEST_INTERSTITIAL else PROD_INTERSTITIAL
    fun rewardedAdId(isDebug: Boolean = true): String = if (isDebug) TEST_REWARDED else PROD_REWARDED
}
