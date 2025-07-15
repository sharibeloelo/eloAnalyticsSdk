package com.greenhorn.neuronet.db.repository

import com.greenhorn.neuronet.EloAnalyticsEvent
import com.greenhorn.neuronet.db.EloAnalyticsDao

class EloAnalyticsLocalRepositoryImpl(private val dao: EloAnalyticsDao) :
    EloAnalyticsLocalRepository {
    override suspend fun insertEvent(data: EloAnalyticsEvent): Long = dao.insertEvent(data)

    override suspend fun insertEvents(data: List<EloAnalyticsEvent>): List<Long> = dao.insertEvents(data)

    override suspend fun deleteEvents(ids: List<Long>): Int = dao.deleteEvents(eventIds = ids)

    override suspend fun getEvents(limit: Int): List<EloAnalyticsEvent> = dao.getEvents(limit)

    override suspend fun getEvents(): List<EloAnalyticsEvent> = dao.getEvents()

    override suspend fun getEventsCount(): Int = dao.getEventsCount()
}
