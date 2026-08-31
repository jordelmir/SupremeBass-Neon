package com.supreme.android.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.supreme.android.data.WarrantyEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class Warranty(
    val id: String,
    val assetId: String,
    val provider: String,
    val purchaseDate: Long,
    val warrantyEnd: Long,
    val isActive: Boolean,
    val serialNumber: String?
)

data class WarrantyUiState(
    val warranties: List<Warranty> = emptyList(),
    val isLoading: Boolean = false
)

class WarrantyViewModel(application: Application) : BaseViewModel(application) {
    private val _uiState = MutableStateFlow(WarrantyUiState())
    val uiState: StateFlow<WarrantyUiState> = _uiState

    init {
        loadWarranties()
    }

    private fun loadWarranties() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val entities = container.warrantyDao.getAll()
                _uiState.value = WarrantyUiState(
                    warranties = entities.map { entity ->
                        Warranty(
                            id = entity.assetId,
                            assetId = entity.assetId,
                            provider = entity.provider,
                            purchaseDate = entity.purchaseDate,
                            warrantyEnd = entity.warrantyEnd,
                            isActive = entity.isActive,
                            serialNumber = entity.serialNumber
                        )
                    },
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = WarrantyUiState(isLoading = false)
            }
        }
    }
}
