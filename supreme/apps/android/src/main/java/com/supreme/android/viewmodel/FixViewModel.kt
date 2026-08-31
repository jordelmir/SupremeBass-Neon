package com.supreme.android.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.supreme.core.Diagnosis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class FixUiState(
    val isAnalyzing: Boolean = false,
    val diagnosis: String = "",
    val confidence: Float = 0f,
    val suggestions: List<String> = emptyList()
)

class FixViewModel(application: Application) : BaseViewModel(application) {
    private val _uiState = MutableStateFlow(FixUiState())
    val uiState: StateFlow<FixUiState> = _uiState

    fun analyze(imageData: ByteArray? = null, audioData: ByteArray? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAnalyzing = true)
            try {
                val result = container.fixAIEngine.diagnoseFromCameraAndAudio(
                    imageBytes = imageData ?: ByteArray(0),
                    audioBytes = audioData ?: ByteArray(0)
                )
                _uiState.value = FixUiState(
                    isAnalyzing = false,
                    diagnosis = result.mostLikelyCause.description,
                    confidence = result.mostLikelyCause.confidence.toFloat(),
                    suggestions = result.checks.map { it.description }
                )
            } catch (e: Exception) {
                _uiState.value = FixUiState(
                    isAnalyzing = false,
                    diagnosis = "Error: ${e.message}",
                    confidence = 0f,
                    suggestions = emptyList()
                )
            }
        }
    }
}
