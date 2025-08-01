package com.greenhorn.neuronet.usecase.local

import com.greenhorn.neuronet.constant.Constant
import com.greenhorn.neuronet.model.EloAnalyticsEvent
import com.greenhorn.neuronet.repository.local.EloAnalyticsLocalRepository

internal class EloAnalyticsLocalEventUseCaseImpl (private val repository: EloAnalyticsLocalRepository) :
    EloAnalyticsLocalEventUseCase {

    override suspend fun insertEvent(data: EloAnalyticsEvent): Long = repository.insertEvent(data)

    override suspend fun insertEvents(data: List<EloAnalyticsEvent>): List<Long> = repository.insertEvents(data)

    override suspend fun deleteEvents(ids: List<Long>): Int {
        val batchSize = Constant.DEFAULT_DELETE_BATCH_SIZE // by checking too many sql exception was happening on 30k+ but for safer side 900(common)
        val idBatches = ids.chunked(batchSize)

        var deletedCount = 0
        for (batch in idBatches) {
            deletedCount += repository.deleteEvents(ids = batch)
        }

        return deletedCount
    }

    override suspend fun getEvents(limit: Int): List<EloAnalyticsEvent> = repository.getEvents(limit)

    override suspend fun getEvents(): List<EloAnalyticsEvent> = repository.getEvents()

    override suspend fun getEventsCount(): Int = repository.getEventsCount()
}