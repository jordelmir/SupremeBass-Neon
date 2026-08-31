package com.supreme.android

import android.app.Application

/**
 * Supreme Application — initializes all modules.
 */
class SupremeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: SupremeApplication
            private set
    }
}
