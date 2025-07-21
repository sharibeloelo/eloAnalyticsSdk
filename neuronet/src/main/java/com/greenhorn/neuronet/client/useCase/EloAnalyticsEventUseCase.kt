package com.greenhorn.neuronet.client.useCase

import com.greenhorn.neuronet.utils.Result
import com.greenhorn.neuronet.model.EloAnalyticsEventDto

interface EloAnalyticsEventUseCase {
    suspend fun sendAnalyticEvents(
        eventDtos: List<EloAnalyticsEventDto>
    ): Result<Boolean>
}