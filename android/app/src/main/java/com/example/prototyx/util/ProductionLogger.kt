package com.example.prototyx.util

import android.util.Log

object ProductionLogger {
    private const val TAG = "PROTOTYX_PROD"

    fun logInfo(message: String) {
        Log.i(TAG, "[INFO] $message")
    }

    fun logWarning(message: String) {
        Log.w(TAG, "[WARN] $message")
    }

    fun logError(message: String, throwable: Throwable? = null) {
        Log.e(TAG, "[ERROR] $message", throwable)
    }

    fun recordException(throwable: Throwable) {
        Log.e(TAG, "[EXCEPTION] Non-fatal exception captured: ${throwable.localizedMessage}", throwable)
    }
}
