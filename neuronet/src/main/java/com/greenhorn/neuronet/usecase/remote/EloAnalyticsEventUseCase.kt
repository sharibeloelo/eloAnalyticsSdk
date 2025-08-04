package com.greenhorn.neuronet.usecase.remote

import com.greenhorn.neuronet.model.EloAnalyticsEvent
import com.greenhorn.neuronet.utils.Result

internal interface EloAnalyticsEventUseCase {
    suspend fun sendAnalyticEvents(
        events: List<EloAnalyticsEvent>
    ): Result<Boolean>
}