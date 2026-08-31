package com.supreme.android.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class FixUiState(
    val isAnalyzing: Boolean = false,
    val diagnosis: String = "",
    val confidence: Float = 0f,
    val suggestions: List<String> = emptyList(),
    val error: String? = null
)

class FixViewModel(application: Application) : BaseViewModel(application) {
    private val _uiState = MutableStateFlow(FixUiState())
    val uiState: StateFlow<FixUiState> = _uiState

    fun analyze(imageData: ByteArray? = null, audioData: ByteArray? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAnalyzing = true, error = null)
            try {
                val result = container.fixAIEngine.diagnoseFromCameraAndAudio(
                    imageBytes = imageData ?: ByteArray(0),
                    audioBytes = audioData ?: ByteArray(0)
                )
                val topCause = result.mostLikelyCauses.firstOrNull()
                _uiState.value = FixUiState(
                    isAnalyzing = false,
                    diagnosis = topCause?.name ?: "No diagnosis available",
                    confidence = topCause?.probability?.toFloat() ?: 0f,
                    suggestions = result.checks.map { it.name }
                )
            } catch (e: Exception) {
                _uiState.value = FixUiState(
                    isAnalyzing = false,
                    error = "Analysis failed: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
