package com.greenhorn.neuronet.client

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.Url

internal interface ApiService {
    @POST()
    suspend fun sendEloAnalyticEvents(
        @Url url: String,
        @HeaderMap headerMap: Map<String, String>,
        @Body events: JsonArray
    ): Response<JsonElement>
}