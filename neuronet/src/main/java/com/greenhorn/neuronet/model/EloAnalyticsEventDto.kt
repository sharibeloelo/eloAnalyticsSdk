package com.greenhorn.neuronet.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class EloAnalyticsEventDto(
    val eventName: String,
    val eventTimeStamp: String,
    val primaryId: String,
    val sessionId: String,
    val eventData: Map<String, String>
) {

    companion object {
        private const val EVENT_NAME = "ep_event_name"
        const val TIME_STAMP = "ep_time_stamp"
        const val APPS_FLYER_ID = "appsflyer_id"
        private const val PRIMARY_ID = "ep_primary_id"
        private const val SESSION_ID = "ep_session_id"
    }

    fun toJsonObject(): JsonObject {
        val jsonMap = mutableMapOf<String, JsonElement>()

        // Add top-level keys
        jsonMap[EVENT_NAME] = JsonPrimitive(eventName)
        jsonMap[TIME_STAMP] = JsonPrimitive(eventTimeStamp)
        jsonMap[PRIMARY_ID] = JsonPrimitive(primaryId)
        jsonMap[SESSION_ID] = JsonPrimitive(sessionId)

        // Flatten and add eventData
        eventData.forEach { (key, value) ->
            jsonMap[key] = JsonPrimitive(value)
        }

        return JsonObject(jsonMap)
    }
}
