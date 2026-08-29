package com.supremecorp.bass

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.MobileAds
import com.supremecorp.bass.data.ads.AdUnitProvider

object AdsManager {
    private const val TAG = "SupremeBass_Ads"

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        Log.i(TAG, "Initializing AdMob")
        AdUnitProvider.logAdIds(context)
        MobileAds.initialize(context) { initializationStatus ->
            Log.d(TAG, "AdMob initialized: ${initializationStatus.adapterStatusMap}")
            isInitialized = true
            preloadInterstitial(context.applicationContext)
            preloadRewarded(context.applicationContext)
        }
    }

    // ─── BANNER ───

    fun createBannerAd(context: Context): AdView {
        val adId = AdUnitProvider.bannerAdId(context)
        Log.d(TAG, "Creating banner ad with unitId: $adId")
        return AdView(context).apply {
            adUnitId = adId
            setAdSize(AdSize.BANNER)
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d(TAG, "Banner ad loaded successfully")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Banner ad failed to load: code=${error.code}, message=${error.message}")
                }
                override fun onAdOpened() {
                    Log.d(TAG, "Banner ad opened")
                }
                override fun onAdClicked() {
                    Log.d(TAG, "Banner ad clicked")
                }
            }
            loadAd(AdRequest.Builder().build())
        }
    }

    // ─── INTERSTITIAL ───

    private fun preloadInterstitial(context: Context) {
        InterstitialAd.load(
            context,
            AdUnitProvider.interstitialAdId(context),
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d(TAG, "Interstitial preloaded")
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            interstitialAd = null
                            preloadInterstitial(context)
                        }
                        override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                            interstitialAd = null
                            preloadInterstitial(context)
                        }
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Interstitial preload failed: ${error.message}")
                    interstitialAd = null
                }
            }
        )
    }

    fun showInterstitialIfReady(activity: Activity): Boolean {
        val ad = interstitialAd
        if (ad != null) {
            ad.show(activity)
            return true
        }
        return false
    }

    // ─── REWARDED ───

    private fun preloadRewarded(context: Context) {
        RewardedAd.load(
            context,
            AdUnitProvider.rewardedAdId(context),
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    Log.d(TAG, "Rewarded ad preloaded")
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            rewardedAd = null
                            preloadRewarded(context)
                        }
                        override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                            rewardedAd = null
                            preloadRewarded(context)
                        }
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Rewarded preload failed: ${error.message}")
                    rewardedAd = null
                }
            }
        )
    }

    fun showRewardedIfReady(
        activity: Activity,
        onRewarded: () -> Unit
    ): Boolean {
        val ad = rewardedAd
        if (ad != null) {
            ad.show(activity) { onRewarded() }
            return true
        }
        return false
    }
}
