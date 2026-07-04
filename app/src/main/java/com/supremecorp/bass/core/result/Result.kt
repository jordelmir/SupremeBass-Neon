package com.supremecorp.bass.core.result

sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>
    data class Error(val error: com.supremecorp.bass.core.error.SupremeBassError) : Result<Nothing>
    data object Loading : Result<Nothing>
}
