package com.supreme.android.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class VibrationUiState(
    val isRecording: Boolean = false,
    val isAnalyzing: Boolean = false,
    val rmsX: Float = 0f,
    val rmsY: Float = 0f,
    val rmsZ: Float = 0f,
    val totalRMS: Float = 0f,
    val dominantFrequency: Float = 0f,
    val baselineComparison: String = "",
    val suggestions: List<String> = emptyList()
)

class VibrationViewModel(application: Application) : BaseViewModel(application) {
    private val _uiState = MutableStateFlow(VibrationUiState())
    val uiState: StateFlow<VibrationUiState> = _uiState

    fun startRecording() {
        _uiState.value = _uiState.value.copy(isRecording = true)
    }

    fun stopRecording(x: FloatArray, y: FloatArray, z: FloatArray) {
        _uiState.value = _uiState.value.copy(isRecording = false, isAnalyzing = true)
        viewModelScope.launch {
            try {
                val result = container.vibrationDoctorEngine.analyze(x, y, z)
                _uiState.value = VibrationUiState(
                    isRecording = false,
                    isAnalyzing = false,
                    rmsX = result.rmsX,
                    rmsY = result.rmsY,
                    rmsZ = result.rmsZ,
                    totalRMS = result.totalRMS,
                    dominantFrequency = result.dominantFrequency,
                    baselineComparison = result.baselineComparison,
                    suggestions = result.recommendations
                )
            } catch (e: Exception) {
                _uiState.value = VibrationUiState(
                    isRecording = false,
                    isAnalyzing = false,
                    baselineComparison = "Error: ${e.message}"
                )
            }
        }
    }
}
