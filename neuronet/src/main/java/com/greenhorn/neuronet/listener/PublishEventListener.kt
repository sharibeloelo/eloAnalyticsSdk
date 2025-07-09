package com.greenhorn.neuronet.listener

import com.greenhorn.neuronet.AnalyticsEvent
import com.greenhorn.neuronet.enum.PRIORITY

interface PublishEventListener {
    suspend fun track(payload: AnalyticsEvent, currentUserId: Long,
                      guestUserId: Long, priority: PRIORITY = PRIORITY.LOW)
}