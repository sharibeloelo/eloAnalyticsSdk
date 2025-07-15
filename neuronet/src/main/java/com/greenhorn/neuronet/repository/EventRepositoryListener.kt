package com.greenhorn.neuronet.repository

import com.greenhorn.neuronet.EloAnalyticsEvent

interface EventRepositoryListener {
    suspend fun insertEvent(event: EloAnalyticsEvent)
    suspend fun getUnsyncedEvents(limit: Int): List<EloAnalyticsEvent>
    suspend fun markEventsAsSynced(eventIds: List<Long>)
    suspend fun deleteSyncedEvents(eventIds: List<Long>)
    suspend fun getEventCount(): Long
}