package com.greenhorn.neuronet.client

import android.net.http.HttpResponseCache.install
import android.util.Log
import com.greenhorn.neuronet.model.Event
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * A simple, mock API client to simulate sending events to a backend.
 * In a real-world application, this would be replaced by a Retrofit or Ktor client.
 *
 * @param endpoint The URL of the backend service.
 */
class ApiClient(private val urlPath: String) {

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
    }

    /**
     * Simulates sending a batch of events to the backend.
     * This function introduces an artificial delay and can randomly fail to
     * demonstrate the SDK's retry and error handling capabilities.
     *
     * @param events The list of events to send.
     * @return A boolean indicating if the call was successful.
     */
    suspend fun sendEvents(events: List<Event>): Boolean {
        Log.d("ApiClient", "Preparing to send ${events.size} events to $urlPath")

        // In a real implementation, you would serialize the 'events' list to JSON
        // and send it as the body of a POST request.
        // val gson = Gson()
        // val jsonPayload = gson.toJson(events)
        // ... make actual HTTP POST request here ...
        return try {
            val response = httpClient.post(urlPath) {
                contentType(ContentType.Application.Json)
                setBody(events)
            }
            Log.d("ApiClient", "Successfully sent ${events.size} events.")
            response.status.value == 200 // Or whatever your success status is
        }catch (e: Exception){
            Log.e("ApiClient", "Error sending events: ${e.message}")
            false
        }
    }
}