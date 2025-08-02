package com.greenhorn.neuronet.usecase.remote

import com.greenhorn.neuronet.repository.remote.EloAnalyticsRepository
import com.greenhorn.neuronet.utils.Result
import com.greenhorn.neuronet.model.EloAnalyticsEventDto

internal class EloAnalyticsEventUseCaseImpl (private val repository: EloAnalyticsRepository) :
    EloAnalyticsEventUseCase {
    override suspend fun sendAnalyticEvents(eventDtos: List<EloAnalyticsEventDto>): Result<Boolean> {
        return repository.sendEloAnalyticEvents(events = eventDtos)
    }
}