package com.greenhorn.neuronet.listener

import com.greenhorn.neuronet.enum.PRIORITY

interface PublishEventListener {
    suspend fun track(eventName: String, userId : Long, isUserLogin: Boolean, payload: MutableMap<String, Any>, priority: PRIORITY = PRIORITY.LOW)
}