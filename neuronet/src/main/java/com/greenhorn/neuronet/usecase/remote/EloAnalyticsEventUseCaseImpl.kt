package com.greenhorn.neuronet.usecase.remote

import com.greenhorn.neuronet.model.EloAnalyticsEvent
import com.greenhorn.neuronet.repository.remote.EloAnalyticsRepository
import com.greenhorn.neuronet.utils.EloSdkLogger
import com.greenhorn.neuronet.utils.Result

internal class EloAnalyticsEventUseCaseImpl (private val repository: EloAnalyticsRepository) :
    EloAnalyticsEventUseCase {
    override suspend fun sendAnalyticEvents(events: List<EloAnalyticsEvent>): Result<Boolean> {
        EloSdkLogger.d("UseCase: Sending ${events.size} analytics events to remote server")
        val result = repository.sendEloAnalyticEvents(events = events)
        EloSdkLogger.d("UseCase: Remote send result: $result")
        return result
    }
}