package com.greenhorn.neuronet.client.eloAnalytics

import com.greenhorn.neuronet.log.utils.Result
import com.greenhorn.neuronet.model.EloAnalyticsEventDto

interface EloAnalyticsRepository {
    suspend fun sendEloAnalyticEvents(events: List<EloAnalyticsEventDto>) : Result<Boolean>
}