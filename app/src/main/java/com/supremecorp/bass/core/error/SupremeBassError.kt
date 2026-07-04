package com.supremecorp.bass.core.error

sealed interface SupremeBassError {
    data class Network(val code: Int?, val message: String?) : SupremeBassError
    data class Ads(val code: String, val recoverable: Boolean) : SupremeBassError
    data class Consent(val reason: String) : SupremeBassError
    data class Ai(val reason: String, val fallbackAvailable: Boolean) : SupremeBassError
    data class Validation(val field: String, val reason: String) : SupremeBassError
    data class Audio(val reason: String) : SupremeBassError
}
