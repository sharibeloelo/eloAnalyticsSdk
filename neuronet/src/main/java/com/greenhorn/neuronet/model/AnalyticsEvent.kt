package com.greenhorn.neuronet.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


/**
 * Represents a single analytics event.
 * This data class is used as a Room Entity to store events locally.
 */
@Entity(tableName = "analytics_sdk_events")
internal data class EloAnalyticsEvent(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo("id") val id: Long = 0L,
    @ColumnInfo("event_name") val eventName: String,
    @ColumnInfo("is_user_login") val isUserLogin: Boolean,
    @ColumnInfo("time_stamp") val eventTimestamp: String,
    @ColumnInfo("session_time_stamp") val sessionTimeStamp: String,
    @ColumnInfo("event_data") val eventData: Map<String, String>
) {
    fun logEvent(): Event {
        return Event(
            eventName = this.eventName,
            eventData = this.eventData,
            sessionTimeStamp = this.sessionTimeStamp,
            timeStamp = this.eventTimestamp,
            isUserLogin = this.isUserLogin
        )
    }

    // Helper data class for logging
    data class Event(
        val eventName: String,
        val eventData: Map<String, String>,
        val sessionTimeStamp: String,
        val timeStamp: String,
        val isUserLogin: Boolean
    )
}
