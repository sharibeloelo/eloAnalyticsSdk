package com.greenhorn.neuronet.service

import com.greenhorn.neuronet.model.EloAnalyticsEventDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface ApiService {
    @POST
    suspend fun trackEvent(@Url url: String, @Body request: Any): Response<Void>

    @POST("v2/analytics/send/moe/event") //todo: url
    suspend fun sendEloAnalyticEvents(@Body events: List<EloAnalyticsEventDto>): Response<Any>

}
