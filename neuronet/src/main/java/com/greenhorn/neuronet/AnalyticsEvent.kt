package com.greenhorn.neuronet

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type


/**
 * Represents a single analytics event.
 * This data class is used as a Room Entity to store events locally.
 */
@Entity(tableName = "analytics_sdk_events")
data class EloAnalyticsEvent(
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


/**
 * Room TypeConverter to convert the event parameters Map to a JSON string and back.
 * This allows storing complex data structures in a single database column.
 */
class EventParamsConverter {
    private val moshi = Moshi.Builder().build()

    private val mapType: Type = Types.newParameterizedType(
        Map::class.java,
        String::class.java,
        String::class.java
    )

    private val adapter = moshi.adapter<Map<String, String>>(mapType)

    @TypeConverter
    fun toMap(value: String?): Map<String, String>? {
        return value?.let {
            try {
                adapter.fromJson(it)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    @TypeConverter
    fun fromMap(map: Map<String, String>?): String? {
        return map?.let {
            try {
                adapter.toJson(it)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
