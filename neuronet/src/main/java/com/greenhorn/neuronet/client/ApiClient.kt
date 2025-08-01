package com.greenhorn.neuronet.client

import com.greenhorn.neuronet.EloAnalyticsSdk
import com.greenhorn.neuronet.model.EloAnalyticsEventDto
import com.greenhorn.neuronet.model.mapper.toJsonArray
import com.greenhorn.neuronet.utils.AnalyticsSdkUtilProvider
import io.ktor.client.statement.HttpResponse

/**
 * Client class for handling analytics API communication.
 * 
 * This client acts as a bridge between the analytics SDK and the API service layer.
 * It handles the preparation of analytics events for transmission, including:
 * - Event data transformation to JSON format
 * - API endpoint configuration
 * - Header management from the SDK configuration
 * 
 * The client is designed to work seamlessly with the EloAnalytics SDK and provides
 * a clean interface for sending analytics events to the backend.
 * 
 * @author Sharib
 * @since 1.2.3
 */
internal class ApiClient(val apiService: ApiService) {
    
    /**
     * Sends a batch of analytics events to the configured API endpoint.
     * 
     * This method prepares the analytics events for transmission by:
     * 1. Converting the events to JSON format
     * 2. Retrieving the configured API endpoint
     * 3. Getting the current headers from the SDK
     * 4. Delegating the actual HTTP request to the API service
     * 
     * @param events List of analytics events to send to the backend
     * @return HttpResponse containing the server response
     * 
     * @throws Exception if the request preparation or transmission fails
     */
    suspend fun sendEventsNew(events: List<EloAnalyticsEventDto>): HttpResponse {
        return apiService.sendEloAnalyticEvents(
            // Get the configured API endpoint from the SDK
            url = AnalyticsSdkUtilProvider.getApiEndPoint(),
            
            // Retrieve current headers from the SDK's header provider
            headerMap = EloAnalyticsSdk.getDependencyContainer().mutableHeaderProvider.getHeaders(),
            
            // Convert events to JSON array format for transmission
            events = events.toJsonArray()
        )
    }
}