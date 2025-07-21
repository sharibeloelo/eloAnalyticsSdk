package com.greenhorn.neuronet.client.useCase

import com.greenhorn.neuronet.client.repository.EloAnalyticsRepository
import com.greenhorn.neuronet.utils.Result
import com.greenhorn.neuronet.model.EloAnalyticsEventDto

class EloAnalyticsEventUseCaseImpl (private val repository: EloAnalyticsRepository) :
    EloAnalyticsEventUseCase {
    override suspend fun sendAnalyticEvents(eventDtos: List<EloAnalyticsEventDto>): Result<Boolean> {
        return repository.sendEloAnalyticEvents(events = eventDtos)
    }
}