package com.supreme.android.core.result

sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>
    data class Error(val error: com.supreme.android.core.error.SupremeBassError) : Result<Nothing>
    data object Loading : Result<Nothing>
}
