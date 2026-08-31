package com.supreme.android.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.supreme.android.data.MaintenanceTaskEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class MaintenanceTask(
    val id: String,
    val assetId: String,
    val title: String,
    val description: String,
    val dueDate: Long,
    val isOverdue: Boolean,
    val priority: String
)

data class MaintenanceUiState(
    val tasks: List<MaintenanceTask> = emptyList(),
    val isLoading: Boolean = false
)

class MaintenanceViewModel(application: Application) : BaseViewModel(application) {
    private val _uiState = MutableStateFlow(MaintenanceUiState())
    val uiState: StateFlow<MaintenanceUiState> = _uiState

    init {
        loadTasks()
    }

    private fun loadTasks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val now = System.currentTimeMillis()
                val entities = container.maintenanceDao.getDueSoon(now + 30L * 24 * 60 * 60 * 1000)
                _uiState.value = MaintenanceUiState(
                    tasks = entities.map { entity ->
                        MaintenanceTask(
                            id = entity.id,
                            assetId = entity.assetId,
                            title = entity.title,
                            description = entity.description,
                            dueDate = entity.dueDate,
                            isOverdue = entity.dueDate < now,
                            priority = entity.priority
                        )
                    },
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = MaintenanceUiState(isLoading = false)
            }
        }
    }
}
