package com.greenhorn.neuronet.client.repository

import com.greenhorn.neuronet.utils.Result
import com.greenhorn.neuronet.model.EloAnalyticsEventDto

internal interface EloAnalyticsRepository {
    suspend fun sendEloAnalyticEvents(events: List<EloAnalyticsEventDto>) : Result<Boolean>
}