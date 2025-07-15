package com.greenhorn.neuronet.listener

import android.app.Activity
import com.greenhorn.neuronet.EloAnalyticsEvent
import com.greenhorn.neuronet.enum.PRIORITY

interface PublishEventListener {
    suspend fun track(payload: EloAnalyticsEvent, currentUserId: Long,
                      guestUserId: Long, priority: PRIORITY = PRIORITY.LOW)
}

interface EloAnalyticsEventManager {
    fun flushPendingEvents(flushPendingEventTriggerSource: FlushPendingEventTriggerSource, activity: Activity? = null)
}

enum class FlushPendingEventTriggerSource {
    APP_MINIMIZE,
    ACTIVITY_DESTROY,
    EVENT_BATCH_COUNT_COMPLETE
}