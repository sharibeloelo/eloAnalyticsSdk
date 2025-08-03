package com.greenhorn.neuronet.header

import com.greenhorn.neuronet.listener.HeaderProvider
import com.greenhorn.neuronet.utils.EloSdkLogger

internal class MutableHeaderProvider(initialHeaders: Map<String, String> = emptyMap()) : HeaderProvider {
    @Volatile
    private var currentHeaders: Map<String, String> = initialHeaders

    override fun getHeaders(): Map<String, String> {
        EloSdkLogger.d("HeaderProvider: Returning ${currentHeaders.size} headers")
        return currentHeaders
    }

    fun updateHeaders(newHeaders: Map<String, String>) {
        EloSdkLogger.d("HeaderProvider: Updating headers from ${currentHeaders.size} to ${newHeaders.size}")
        EloSdkLogger.d("HeaderProvider: New headers: $newHeaders")
        this.currentHeaders = newHeaders
    }
}