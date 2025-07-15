package com.greenhorn.neuronet.repository

import com.greenhorn.neuronet.EloAnalyticsEvent
import com.greenhorn.neuronet.db.AnalyticsDatabase

/**
 * Repository class that abstracts access to the underlying data source (Room).
 * It provides a clean API for the rest of the SDK to interact with event data.
 * @param dao The Data Access Object for the analytics events.
 */
class EventRepository(private val db: AnalyticsDatabase) : EventRepositoryListener {

    override suspend fun insertEvent(event: EloAnalyticsEvent) {
//        db.analyticsEventDao().insertEvent(event)
    }

    override suspend fun getUnsyncedEvents(limit: Int): List<EloAnalyticsEvent> {
//        return db.analyticsEventDao().getUnsyncedEvents(limit).map { it }
        return emptyList()
    }

    override suspend fun deleteSyncedEvents(eventIds: List<Long>) {
//        db.analyticsEventDao().markEventsAsSynced(eventIds)
    }

    override suspend fun markEventsAsSynced(eventIds: List<Long>) {
//        db.analyticsEventDao().deleteSyncedEvents(eventIds)
    }

    override suspend fun getEventCount(): Long {
//        return db.analyticsEventDao().getEventCount()
        return  0
    }
}