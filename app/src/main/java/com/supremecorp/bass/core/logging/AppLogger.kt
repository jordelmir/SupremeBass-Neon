package com.supremecorp.bass.core.logging

import android.util.Log

object AppLogger {
    private const val TAG = "SupremeBass"
    var isDebug: Boolean = true

    fun d(module: String, message: String) {
        if (isDebug) Log.d("${TAG}_$module", message)
    }

    fun w(module: String, message: String) {
        Log.w("${TAG}_$module", message)
    }

    fun e(module: String, message: String, throwable: Throwable? = null) {
        Log.e("${TAG}_$module", message, throwable)
    }

    fun i(module: String, message: String) {
        Log.i("${TAG}_$module", message)
    }
}
