package com.supreme.android.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.supreme.android.data.AssetEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class Asset(
    val id: String,
    val name: String,
    val category: String,
    val brand: String?,
    val model: String?,
    val purchaseDate: Long?,
    val warrantyExpiry: Long?,
    val condition: String
)

data class AssetsUiState(
    val assets: List<Asset> = emptyList(),
    val isLoading: Boolean = false
)

class AssetsViewModel(application: Application) : BaseViewModel(application) {
    private val _uiState = MutableStateFlow(AssetsUiState())
    val uiState: StateFlow<AssetsUiState> = _uiState

    init {
        loadAssets()
    }

    private fun loadAssets() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val entities = container.assetDao.getAll()
                _uiState.value = AssetsUiState(
                    assets = entities.map { entity ->
                        Asset(
                            id = entity.id,
                            name = entity.name,
                            category = entity.category,
                            brand = entity.brand,
                            model = entity.model,
                            purchaseDate = entity.purchaseDate,
                            warrantyExpiry = entity.warrantyExpiry,
                            condition = entity.condition
                        )
                    },
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = AssetsUiState(isLoading = false)
            }
        }
    }
}
