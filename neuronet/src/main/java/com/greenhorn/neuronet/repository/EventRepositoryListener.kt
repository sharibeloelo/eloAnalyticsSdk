package com.greenhorn.neuronet.repository

import com.greenhorn.neuronet.model.Event

interface EventRepositoryListener {
    suspend fun insertEvent(event: Event)
    suspend fun getUnsyncedEvents(limit: Int): List<Event>
    suspend fun markEventsAsSynced(eventIds: List<Long>)
    suspend fun deleteSyncedEvents(eventIds: List<Long>)
    suspend fun getEventCount(): Long
}