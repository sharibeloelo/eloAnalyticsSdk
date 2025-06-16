package com.eloelo.analytics.repository

import com.eloelo.analytics.AnalyticsEvent
import com.eloelo.analytics.model.Event

interface EventRepositoryListener {
    suspend fun insertEvent(event: Event)
    suspend fun getUnsyncedEvents(limit: Int): List<Event>
    suspend fun markEventsAsSynced(eventIds: List<Long>)
    suspend fun deleteSyncedEvents(eventIds: List<Long>)
    suspend fun getEventCount(): Long
}