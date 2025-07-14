package com.greenhorn.neuronet.log

import android.util.Log

object Logger {
    private var isEnabled = false
    private const val DEFAULT_TAG = "Neuronet_Logger" // You can change this to your app's name

    /**
     * Initializes the logger. This should be called once in your Application class.
     * This acts as the "builder method" to configure logging.
     *
     * @param enableLogs Set to true to enable logging (e.g., for debug builds),
     * false to disable it (e.g., for release builds).
     */
    fun initialize(enableLogs: Boolean) {
        this.isEnabled = enableLogs
    }

    /**
     * Log a debug message.
     */
    fun d(message: String, tag: String = DEFAULT_TAG) {
        if (!isEnabled) return
        Log.d(tag, message)
    }

    /**
     * Log an info message.
     */
    fun i(message: String, tag: String = DEFAULT_TAG) {
        if (!isEnabled) return
        Log.i(tag, message)
    }

    /**
     * Log a warning message.
     */
    fun w(message: String, tag: String = DEFAULT_TAG) {
        if (!isEnabled) return
        Log.w(tag, message)
    }

    /**
     * Log an error message with an optional throwable.
     */
    fun e(message: String, throwable: Throwable? = null, tag: String = DEFAULT_TAG) {
        if (!isEnabled) return
        Log.e(tag, message, throwable)
    }
}