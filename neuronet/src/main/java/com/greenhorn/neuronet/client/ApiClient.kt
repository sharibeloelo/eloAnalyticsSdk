package com.greenhorn.neuronet.client

import com.greenhorn.neuronet.EloAnalyticsSdk
import com.greenhorn.neuronet.model.EloAnalyticsEventDto
import com.greenhorn.neuronet.model.mapper.toJsonArray
import com.greenhorn.neuronet.utils.AnalyticsSdkUtilProvider
import kotlinx.serialization.json.JsonElement
import retrofit2.Response

internal class ApiClient(val apiClient: ApiService) {
    suspend fun sendEventsNew(events: List<EloAnalyticsEventDto>): Response<JsonElement> {
        return apiClient.sendEloAnalyticEvents(
            url = AnalyticsSdkUtilProvider.getApiEndPoint(),
            headerMap = EloAnalyticsSdk.getDependencyContainer().mutableHeaderProvider.getHeaders(),
            events = events.toJsonArray()
        )
    }
}