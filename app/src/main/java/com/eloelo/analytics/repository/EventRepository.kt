package com.eloelo.analytics.repository

import com.eloelo.analytics.db.AnalyticsDatabase
import com.eloelo.analytics.model.Event
import com.eloelo.analytics.toDbEntity
import com.eloelo.analytics.toEvent
import kotlin.collections.map

/**
 * Repository class that abstracts access to the underlying data source (Room).
 * It provides a clean API for the rest of the SDK to interact with event data.
 * @param dao The Data Access Object for the analytics events.
 */
class EventRepository(private val db: AnalyticsDatabase) : EventRepositoryListener {

    override suspend fun insertEvent(event: Event) {
        db.analyticsEventDao().insertEvent(event.toDbEntity())
    }

    override suspend fun getUnsyncedEvents(limit: Int): List<Event> {
        return db.analyticsEventDao().getUnsyncedEvents(limit).map { it.toEvent() }
    }

    override suspend fun deleteSyncedEvents(eventIds: List<Long>) {
        db.analyticsEventDao().markEventsAsSynced(eventIds)
    }

    override suspend fun markEventsAsSynced(eventIds: List<Long>) {
        db.analyticsEventDao().deleteSyncedEvents(eventIds)
    }

    override suspend fun getEventCount(): Long {
        return db.analyticsEventDao().getEventCount()
    }
}