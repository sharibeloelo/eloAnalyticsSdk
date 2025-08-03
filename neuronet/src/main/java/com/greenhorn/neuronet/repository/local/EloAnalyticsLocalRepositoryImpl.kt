package com.greenhorn.neuronet.repository.local

import com.greenhorn.neuronet.model.EloAnalyticsEvent
import com.greenhorn.neuronet.db.dao.EloAnalyticsDao
import com.greenhorn.neuronet.utils.EloSdkLogger

internal class EloAnalyticsLocalRepositoryImpl(private val dao: EloAnalyticsDao) :
    EloAnalyticsLocalRepository {
    
    override suspend fun insertEvent(data: EloAnalyticsEvent): Long {
        EloSdkLogger.d("Inserting single event: ${data.eventName}")
        val result = dao.insertEvent(data)
        EloSdkLogger.d("Event inserted with ID: $result")
        return result
    }

    override suspend fun insertEvents(data: List<EloAnalyticsEvent>): List<Long> {
        EloSdkLogger.d("Inserting ${data.size} events")
        val result = dao.insertEvents(data)
        EloSdkLogger.d("Events inserted with IDs: $result")
        return result
    }

    override suspend fun deleteEvents(ids: List<Long>): Int {
        EloSdkLogger.d("Deleting events with IDs: $ids")
        val result = dao.deleteEvents(eventIds = ids)
        EloSdkLogger.d("Deleted $result events")
        return result
    }

    override suspend fun getEvents(limit: Int): List<EloAnalyticsEvent> {
        EloSdkLogger.d("Getting events with limit: $limit")
        val result = dao.getEvents(limit)
        EloSdkLogger.d("Retrieved ${result.size} events")
        return result
    }

    override suspend fun getEvents(): List<EloAnalyticsEvent> {
        EloSdkLogger.d("Getting all events")
        val result = dao.getEvents()
        EloSdkLogger.d("Retrieved ${result.size} events")
        return result
    }

    override suspend fun getEventsCount(): Int {
        val result = dao.getEventsCount()
        EloSdkLogger.d("Total events count: $result")
        return result
    }
}
