package com.greenhorn.neuronet.service

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface ApiService {
    @POST
    suspend fun trackEvent(@Url url: String, @Body request: Any): Response<Void>
}
