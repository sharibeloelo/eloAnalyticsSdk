package com.greenhorn.neuronet.repository.remote

import com.greenhorn.neuronet.model.EloAnalyticsEvent
import com.greenhorn.neuronet.utils.Result

internal interface EloAnalyticsRepository {
    suspend fun sendEloAnalyticEvents(events: List<EloAnalyticsEvent>) : Result<Boolean>
}