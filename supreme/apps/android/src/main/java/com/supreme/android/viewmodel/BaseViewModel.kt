package com.supreme.android.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.supreme.android.SupremeApplication
import com.supreme.android.di.AppContainer

abstract class BaseViewModel(application: Application) : AndroidViewModel(application) {
    protected val container: AppContainer
        get() = (application as SupremeApplication).container
}
