package com.greenhorn.neuronet.usecase.local

import com.greenhorn.neuronet.constant.Constant
import com.greenhorn.neuronet.model.EloAnalyticsEvent
import com.greenhorn.neuronet.repository.local.EloAnalyticsLocalRepository
import com.greenhorn.neuronet.utils.EloSdkLogger

internal class EloAnalyticsLocalEventUseCaseImpl (private val repository: EloAnalyticsLocalRepository) :
    EloAnalyticsLocalEventUseCase {

    override suspend fun insertEvent(data: EloAnalyticsEvent): Long {
        EloSdkLogger.d("UseCase: Inserting single event: ${data.eventName}")
        val result = repository.insertEvent(data)
        EloSdkLogger.d("UseCase: Event inserted with ID: $result")
        return result
    }

    override suspend fun insertEvents(data: List<EloAnalyticsEvent>): List<Long> {
        EloSdkLogger.d("UseCase: Inserting ${data.size} events")
        val result = repository.insertEvents(data)
        EloSdkLogger.d("UseCase: Events inserted with IDs: $result")
        return result
    }

    override suspend fun deleteEvents(ids: List<Long>): Int {
        EloSdkLogger.d("UseCase: Deleting ${ids.size} events with batch size: ${Constant.DEFAULT_DELETE_BATCH_SIZE}")
        val batchSize = Constant.DEFAULT_DELETE_BATCH_SIZE // by checking too many sql exception was happening on 30k+ but for safer side 900(common)
        val idBatches = ids.chunked(batchSize)

        var deletedCount = 0
        for ((index, batch) in idBatches.withIndex()) {
            EloSdkLogger.d("UseCase: Processing batch ${index + 1}/${idBatches.size} with ${batch.size} IDs")
            deletedCount += repository.deleteEvents(ids = batch)
        }

        EloSdkLogger.d("UseCase: Total deleted events: $deletedCount")
        return deletedCount
    }

    override suspend fun getEvents(limit: Int): List<EloAnalyticsEvent> {
        EloSdkLogger.d("UseCase: Getting events with limit: $limit")
        val result = repository.getEvents(limit)
        EloSdkLogger.d("UseCase: Retrieved ${result.size} events")
        return result
    }

    override suspend fun getEvents(): List<EloAnalyticsEvent> {
        EloSdkLogger.d("UseCase: Getting all events")
        val result = repository.getEvents()
        EloSdkLogger.d("UseCase: Retrieved ${result.size} events")
        return result
    }

    override suspend fun getEventsCount(): Int {
        val result = repository.getEventsCount()
        EloSdkLogger.d("UseCase: Total events count: $result")
        return result
    }
}