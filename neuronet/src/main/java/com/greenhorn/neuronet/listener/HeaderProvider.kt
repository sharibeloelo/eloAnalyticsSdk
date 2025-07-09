package com.greenhorn.neuronet.listener

interface HeaderProvider {
    fun getHeaders(): Map<String, String>
}