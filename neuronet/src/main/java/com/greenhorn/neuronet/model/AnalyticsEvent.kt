package com.greenhorn.neuronet.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive


/**
 * Represents a single analytics event.
 * This data class is used as a Room Entity to store events locally.
 */
@Entity(tableName = "analytics_sdk_events")
internal data class EloAnalyticsEvent(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo("id") val id: Long = 0L,
    @ColumnInfo("event_name") val eventName: String,
    @ColumnInfo("timestamp") val timestamp: String,
    @ColumnInfo("primary_id") val primaryId: String,
    @ColumnInfo("session_id") val sessionId: String,
    @ColumnInfo("event_data") val eventData: Map<String, String>
) {
    fun logEvent(): Event {
        return Event(
            eventName = this.eventName,
            eventData = this.eventData,
            sessionId = this.sessionId,
            primaryId = this.primaryId,
            timeStamp = this.timestamp
        )
    }

    // Helper data class for logging
    data class Event(
        val eventName: String,
        val eventData: Map<String, String>,
        val timeStamp: String,
        val primaryId: String,
        val sessionId: String,
    )

    companion object {
        private const val EVENT_NAME = "ep_event_name"
        const val TIME_STAMP = "ep_time_stamp"
        private const val PRIMARY_ID = "ep_primary_id"
        private const val SESSION_ID = "ep_session_id"
    }

    fun toJsonObject(): JsonObject {
        val jsonMap = mutableMapOf<String, JsonElement>()

        // Add top-level keys
        jsonMap[EVENT_NAME] = JsonPrimitive(eventName)
        jsonMap[TIME_STAMP] = JsonPrimitive(timestamp)
        jsonMap[PRIMARY_ID] = JsonPrimitive(primaryId)
        jsonMap[SESSION_ID] = JsonPrimitive(sessionId)

        // Flatten and add eventData
        eventData.forEach { (key, value) ->
            jsonMap[key] = JsonPrimitive(value)
        }

        return JsonObject(jsonMap)
    }
}
