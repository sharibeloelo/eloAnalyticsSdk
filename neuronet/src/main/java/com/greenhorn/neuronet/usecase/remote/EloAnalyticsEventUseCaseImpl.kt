package com.greenhorn.neuronet.usecase.remote

import com.greenhorn.neuronet.repository.remote.EloAnalyticsRepository
import com.greenhorn.neuronet.utils.Result
import com.greenhorn.neuronet.model.EloAnalyticsEventDto
import com.greenhorn.neuronet.utils.EloSdkLogger

internal class EloAnalyticsEventUseCaseImpl (private val repository: EloAnalyticsRepository) :
    EloAnalyticsEventUseCase {
    override suspend fun sendAnalyticEvents(eventDtos: List<EloAnalyticsEventDto>): Result<Boolean> {
        EloSdkLogger.d("UseCase: Sending ${eventDtos.size} analytics events to remote server")
        val result = repository.sendEloAnalyticEvents(events = eventDtos)
        EloSdkLogger.d("UseCase: Remote send result: $result")
        return result
    }
}