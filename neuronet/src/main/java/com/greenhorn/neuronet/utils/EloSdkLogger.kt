package com.greenhorn.neuronet.utils

import android.util.Log

internal object EloSdkLogger {

    private const val DEFAULT_TAG = "EloAnalyticsSDK_DEBUG"
    private var isDebug: Boolean = false

    fun init(debug: Boolean) {
        isDebug = debug
    }

    fun d(message: String, tag: String = DEFAULT_TAG) {
        if (isDebug) Log.d(tag, message)
    }

    fun i(message: String, tag: String = DEFAULT_TAG) {
        if (isDebug) Log.i(tag, message)
    }

    fun w(message: String, tag: String = DEFAULT_TAG) {
        if (isDebug) Log.w(tag, message)
    }

    fun e(message: String, throwable: Throwable? = null, tag: String = DEFAULT_TAG) {
        if (isDebug) Log.e(tag, message, throwable)
    }

    fun v(message: String, tag: String = DEFAULT_TAG) {
        if (isDebug) Log.v(tag, message)
    }
}
