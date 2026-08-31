package com.supreme.android.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class NoiseUiState(
    val isRecording: Boolean = false,
    val isAnalyzing: Boolean = false,
    val dominantFrequency: Float = 0f,
    val harmonics: List<Float> = emptyList(),
    val classification: String = "",
    val suggestions: List<String> = emptyList()
)

class NoiseViewModel(application: Application) : BaseViewModel(application) {
    private val _uiState = MutableStateFlow(NoiseUiState())
    val uiState: StateFlow<NoiseUiState> = _uiState

    fun startRecording() {
        _uiState.value = _uiState.value.copy(isRecording = true)
    }

    fun stopRecording(audioData: FloatArray) {
        _uiState.value = _uiState.value.copy(isRecording = false, isAnalyzing = true)
        viewModelScope.launch {
            try {
                val result = container.noiseDoctorEngine.analyze(audioData)
                _uiState.value = NoiseUiState(
                    isRecording = false,
                    isAnalyzing = false,
                    dominantFrequency = result.dominantFrequency,
                    harmonics = result.harmonics.map { it.frequency },
                    classification = result.classification,
                    suggestions = result.potentialCauses
                )
            } catch (e: Exception) {
                _uiState.value = NoiseUiState(
                    isRecording = false,
                    isAnalyzing = false,
                    classification = "Error: ${e.message}"
                )
            }
        }
    }
}
