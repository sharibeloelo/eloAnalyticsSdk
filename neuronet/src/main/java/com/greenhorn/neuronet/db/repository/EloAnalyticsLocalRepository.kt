package com.greenhorn.neuronet.db.repository

import com.greenhorn.neuronet.EloAnalyticsEvent

interface EloAnalyticsLocalRepository {
    suspend fun insertEvent(data: EloAnalyticsEvent): Long

    suspend fun insertEvents(data: List<EloAnalyticsEvent>): List<Long>

    suspend fun deleteEvents(ids: List<Long>): Int

    suspend fun getEvents(limit: Int): List<EloAnalyticsEvent>

    suspend fun getEvents(): List<EloAnalyticsEvent>

    suspend fun getEventsCount(): Int
}
