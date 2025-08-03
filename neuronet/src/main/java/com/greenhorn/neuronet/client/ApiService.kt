package com.greenhorn.neuronet.client

import com.greenhorn.neuronet.utils.EloSdkLogger
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonArray

/**
 * Service class for handling analytics API calls using Ktor HTTP client.
 * 
 * This service provides a clean abstraction layer for making HTTP requests to the analytics
 * backend. It handles the low-level HTTP communication details while providing a simple
 * interface for sending analytics events.
 * 
 * The service is designed to work with the EloAnalytics SDK and handles:
 * - POST requests to analytics endpoints
 * - JSON payload serialization
 * - Custom header management
 * - Content-Type configuration
 * 
 * @author EloAnalytics SDK Team
 * @since 1.0.0
 */
internal class ApiService(private val httpClient: HttpClient) {
    
    /**
     * Sends analytics events to the specified endpoint.
     * 
     * This method performs a POST request to the analytics API with the provided events
     * data. It handles all the HTTP communication details including headers, content type,
     * and request body serialization.
     * 
     * @param url The target endpoint URL for the analytics API
     * @param headerMap Map of HTTP headers to include in the request
     * @param events JsonArray containing the analytics events to send
     * @return HttpResponse containing the server response
     * 
     * @throws Exception if the HTTP request fails or network issues occur
     */
    suspend fun sendEloAnalyticEvents(
        url: String,
        headerMap: Map<String, String>,
        events: JsonArray
    ): HttpResponse {
        EloSdkLogger.d("ApiService: Making POST request to: $url")
        EloSdkLogger.d("ApiService: Request headers: $headerMap")
        EloSdkLogger.d("ApiService: Request body size: ${events.size} events")
        
        return httpClient.post {
            // Set the target URL for the request
            this.url(url)
            
            // Set the request body with the analytics events
            setBody(events)
            
            // Configure content type as JSON
            contentType(ContentType.Application.Json)
            
            // Add all custom headers to the request
            headerMap.forEach { (key, value) ->
                headers.append(key, value)
            }
        }.also {
            EloSdkLogger.d("ApiService: HTTP request completed with status: ${it.status}")
        }
    }
}