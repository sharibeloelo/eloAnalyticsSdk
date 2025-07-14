package com.greenhorn.neuronet.client

import com.greenhorn.neuronet.AnalyticsEvent
import com.greenhorn.neuronet.extension.toEventDto
import com.greenhorn.neuronet.extension.toMap
import com.greenhorn.neuronet.model.EloAnalyticsEventDto
import com.greenhorn.neuronet.service.ApiService
import retrofit2.Response
import kotlin.collections.map

/**
 * A simple, mock API client to simulate sending events to a backend.
 * In a real-world application, this would be replaced by a Retrofit or Ktor client.
 *
 * @param endpoint The URL of the backend service.
 */
//TODO: THIS SHOULD BE INJECTED VIA DI to not make it coupled in the dependent classes
class ApiClient(val apiClient: ApiService){
    /**
     * Simulates sending a batch of events to the backend.
     * This function introduces an artificial delay and can randomly fail to
     * demonstrate the SDK's retry and error handling capabilities.
     *
     * @param events The list of events to send.
     * @return A boolean indicating if the call was successful.
     */
    suspend fun sendEvents(url : String, events: List<EloAnalyticsEventDto>) : Response<Void> {
        // In a real implementation, you would serialize the 'events' list to JSON
        // and send it as the body of a POST request.
        val eventsForApi: List<Map<String, Any>> = events.map { event ->
            event.toMap() // Use the new function here
        }
        return apiClient.trackEvent(url, eventsForApi)
    }

    //TODO: CAN MAKE OVERLOAD FUNCTION WITH DIFFERENT SIGNATURE
    suspend fun sendSingleEvents(url : String, events: EloAnalyticsEventDto) : Response<Void> {
        // In a real implementation, you would serialize the 'events' list to JSON
        // and send it as the body of a POST request.
        return apiClient.trackEvent(url, events.toMap())
    }
}