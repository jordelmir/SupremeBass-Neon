package com.supremecorp.bass.data.consent

import android.app.Activity

sealed interface ConsentStatusResult {
    data object Required : ConsentStatusResult
    data object NotRequired : ConsentStatusResult
    data class Error(val reason: String) : ConsentStatusResult
}

sealed interface ConsentFormResult {
    data object Accepted : ConsentFormResult
    data object Denied : ConsentFormResult
    data object NotRequired : ConsentFormResult
    data class Error(val reason: String) : ConsentFormResult
}

interface ConsentManager {
    suspend fun refreshConsentStatus(activity: Activity): ConsentStatusResult
    suspend fun showConsentFormIfRequired(activity: Activity): ConsentFormResult
    fun canRequestAds(): Boolean
    fun isPrivacyOptionsRequired(): Boolean
    suspend fun showPrivacyOptions(activity: Activity): ConsentFormResult
}
