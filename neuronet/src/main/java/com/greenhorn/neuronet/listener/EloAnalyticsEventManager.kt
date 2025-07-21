package com.greenhorn.neuronet.listener

interface EloAnalyticsEventManager {
    fun trackEvent(name: String, attributes: Map<String, Any>)

    fun updateSessionTimeStamp(timeStamp: String)

    fun updateHeader(header: Map<String, String>)
}