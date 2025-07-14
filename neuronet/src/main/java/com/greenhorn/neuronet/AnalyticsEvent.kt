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
@Entity(tableName = "analytics_events")
data class AnalyticsEvent(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo("id") val id: Long = 0L,
    @ColumnInfo("eventName")  val eventName: String,
    @ColumnInfo("isUserLogin") val isUserLogin: Boolean,
    @ColumnInfo("payload") val payload: Map<String, Any>,
    @ColumnInfo("timestamp")  val timestamp: String,
    @ColumnInfo("sessionTimeStamp") val sessionTimeStamp: String,
    @ColumnInfo("isSynced") val isSynced: Boolean = false,
)


/**
 * Room TypeConverter to convert the event parameters Map to a JSON string and back.
 * This allows storing complex data structures in a single database column.
 */
class EventParamsConverter {
    private val moshi = Moshi.Builder().build()

    // Define the specific type for Map<String, Any>
    private val mapType: Type = Types.newParameterizedType(Map::class.java, String::class.java, Object::class.java)

    // Create a JsonAdapter for this specific type
    private val adapter = moshi.adapter<Map<String, Any>>(mapType)

    @TypeConverter
    fun toMap(value: String?): Map<String, Any>? {
        if (value == null) {
            return null
        }
        return adapter.fromJson(value)
    }

    @TypeConverter
    fun fromMap(map: Map<String, Any>?): String? {
        if (map == null) {
            return null
        }
        return adapter.toJson(map)
    }
}
