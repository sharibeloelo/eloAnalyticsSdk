package com.greenhorn.neuronet.client

import com.greenhorn.neuronet.AnalyticsSdkUtilProvider
import com.greenhorn.neuronet.model.EloAnalyticsEventDto
import com.greenhorn.neuronet.model.mapper.toJsonObject
import kotlinx.serialization.json.JsonElement
import retrofit2.Response

class ApiClient(val apiClient: ApiService) {
    suspend fun sendEventsNew(events: List<EloAnalyticsEventDto>): Response<JsonElement> {
        return apiClient.sendEloAnalyticEvents(
            url = AnalyticsSdkUtilProvider.getApiEndPoint(),
            events.toJsonObject()
        )
    }
}