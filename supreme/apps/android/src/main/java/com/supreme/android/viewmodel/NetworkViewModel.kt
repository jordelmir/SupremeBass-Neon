package com.supreme.android.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class NetworkUiState(
    val isDiagnosing: Boolean = false,
    val diagnosis: String = "",
    val checks: List<String> = emptyList(),
    val recommendations: List<String> = emptyList()
)

class NetworkViewModel(application: Application) : BaseViewModel(application) {
    private val _uiState = MutableStateFlow(NetworkUiState())
    val uiState: StateFlow<NetworkUiState> = _uiState

    fun diagnose() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDiagnosing = true)
            try {
                val result = container.networkDoctorEngine.diagnose()
                _uiState.value = NetworkUiState(
                    isDiagnosing = false,
                    diagnosis = result.summary,
                    checks = result.checks.map { "${it.name}: ${it.status}" },
                    recommendations = result.recommendations
                )
            } catch (e: Exception) {
                _uiState.value = NetworkUiState(
                    isDiagnosing = false,
                    diagnosis = "Error: ${e.message}"
                )
            }
        }
    }
}
