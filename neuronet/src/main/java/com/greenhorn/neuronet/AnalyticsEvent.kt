package com.greenhorn.neuronet

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.greenhorn.neuronet.model.Event
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


/**
 * Represents a single analytics event.
 * This data class is used as a Room Entity to store events locally.
 */
@Entity(tableName = "analytics_events")
@TypeConverters(EventParamsConverter::class)
data class AnalyticsEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventName: String,
    val isUserLogin: Boolean,
    val payload: Map<String, String>,
    val timestamp: String,
    val sessionTimeStamp: String,
    val isSynced: Boolean = false
)

fun Event.toDbEntity(): AnalyticsEvent {
    return AnalyticsEvent(id, eventName, isUserLogin, payload, timestamp, sessionTimeStamp, isSynced)
}

fun AnalyticsEvent.toEvent(): Event {
    return Event(id, eventName, isUserLogin, payload, timestamp, sessionTimeStamp, isSynced)
}

/**
 * Room TypeConverter to convert the event parameters Map to a JSON string and back.
 * This allows storing complex data structures in a single database column.
 */
class EventParamsConverter {
    private val gson by lazy { Gson() }

    @TypeConverter
    fun fromMap(value: String): Map<String, String> {
        val mapType = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(value, mapType)
    }

    @TypeConverter
    fun toMap(map: Map<String, String>): String {
        return gson.toJson(map)
    }
}
