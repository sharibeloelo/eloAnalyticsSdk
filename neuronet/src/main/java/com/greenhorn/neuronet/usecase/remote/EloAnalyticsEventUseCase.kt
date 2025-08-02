package com.greenhorn.neuronet.usecase.remote

import com.greenhorn.neuronet.utils.Result
import com.greenhorn.neuronet.model.EloAnalyticsEventDto

internal interface EloAnalyticsEventUseCase {
    suspend fun sendAnalyticEvents(
        eventDtos: List<EloAnalyticsEventDto>
    ): Result<Boolean>
}