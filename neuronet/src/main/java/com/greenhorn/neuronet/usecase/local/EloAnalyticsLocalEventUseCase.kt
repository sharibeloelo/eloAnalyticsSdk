package com.greenhorn.neuronet.usecase.local

import com.greenhorn.neuronet.model.EloAnalyticsEvent

internal interface EloAnalyticsLocalEventUseCase {
    suspend fun insertEvent(data: EloAnalyticsEvent): Long

    suspend fun insertEvents(data: List<EloAnalyticsEvent>): List<Long>

    suspend fun deleteEvents(ids: List<Long>): Int

    suspend fun getEvents(limit: Int): List<EloAnalyticsEvent>

    suspend fun getEvents(): List<EloAnalyticsEvent>

    suspend fun getEventsCount(): Int
}