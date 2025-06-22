package com.greenhorn.neuronet.client

import android.util.Log
import com.greenhorn.neuronet.model.Event
import com.greenhorn.neuronet.service.ApiService
import retrofit2.Response
import retrofit2.Retrofit
import kotlin.jvm.java

/**
 * A simple, mock API client to simulate sending events to a backend.
 * In a real-world application, this would be replaced by a Retrofit or Ktor client.
 *
 * @param endpoint The URL of the backend service.
 */
class ApiClient(val apiClient: ApiService){
    /**
     * Simulates sending a batch of events to the backend.
     * This function introduces an artificial delay and can randomly fail to
     * demonstrate the SDK's retry and error handling capabilities.
     *
     * @param events The list of events to send.
     * @return A boolean indicating if the call was successful.
     */
    suspend fun sendEvents(url : String, events: List<Event>) : Response<Void> {
        // In a real implementation, you would serialize the 'events' list to JSON
        // and send it as the body of a POST request.
        return apiClient.trackEvent(url, events)
    }
}