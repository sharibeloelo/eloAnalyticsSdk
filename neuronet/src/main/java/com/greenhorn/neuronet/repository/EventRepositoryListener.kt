package com.greenhorn.neuronet.repository

import com.greenhorn.neuronet.AnalyticsEvent
import com.greenhorn.neuronet.model.Event

interface EventRepositoryListener {
    suspend fun insertEvent(event: AnalyticsEvent)
    suspend fun getUnsyncedEvents(limit: Int): List<AnalyticsEvent>
    suspend fun markEventsAsSynced(eventIds: List<Long>)
    suspend fun deleteSyncedEvents(eventIds: List<Long>)
    suspend fun getEventCount(): Long
}