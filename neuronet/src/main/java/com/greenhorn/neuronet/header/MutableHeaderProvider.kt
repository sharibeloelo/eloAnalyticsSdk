package com.greenhorn.neuronet.header

import com.greenhorn.neuronet.listener.HeaderProvider

internal class MutableHeaderProvider(initialHeaders: Map<String, String> = emptyMap()) : HeaderProvider {
    @Volatile
    private var currentHeaders: Map<String, String> = initialHeaders

    override fun getHeaders(): Map<String, String> = currentHeaders

    fun updateHeaders(newHeaders: Map<String, String>) {
        this.currentHeaders = newHeaders
    }
}