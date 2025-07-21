package com.greenhorn.neuronet.listener

internal interface HeaderProvider {
    fun getHeaders(): Map<String, String>
}