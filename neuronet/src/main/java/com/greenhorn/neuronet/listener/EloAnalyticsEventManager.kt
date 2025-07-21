package com.greenhorn.neuronet.listener

import android.app.Activity
import com.greenhorn.neuronet.utils.FlushPendingEventTriggerSource

interface EloAnalyticsEventManager {
    fun flushPendingEvents(flushPendingEventTriggerSource: FlushPendingEventTriggerSource, activity: Activity? = null)
}