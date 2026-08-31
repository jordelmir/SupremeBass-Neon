package com.supreme.android.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SupremeViewModelFactory(
    private val application: Application
) : ViewModelProvider.AndroidViewModelFactory(application) {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(FixViewModel::class.java) -> FixViewModel(application) as T
            modelClass.isAssignableFrom(NetworkViewModel::class.java) -> NetworkViewModel(application) as T
            modelClass.isAssignableFrom(NoiseViewModel::class.java) -> NoiseViewModel(application) as T
            modelClass.isAssignableFrom(VibrationViewModel::class.java) -> VibrationViewModel(application) as T
            modelClass.isAssignableFrom(AssetsViewModel::class.java) -> AssetsViewModel(application) as T
            modelClass.isAssignableFrom(MaintenanceViewModel::class.java) -> MaintenanceViewModel(application) as T
            modelClass.isAssignableFrom(WarrantyViewModel::class.java) -> WarrantyViewModel(application) as T
            else -> super.create(modelClass)
        }
    }
}
