package com.greenhorn.neuronet.model

import androidx.room.TypeConverter
import com.greenhorn.neuronet.extension.orDefault
import kotlinx.serialization.json.Json

/**
 * Room TypeConverter to convert the event parameters Map to a JSON string and back.
 * This allows storing complex data structures in a single database column.
 */
internal class EventParamsConverter {
    private val json by lazy {
        Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
            coerceInputValues = true
        }
    }

    @TypeConverter
    fun fromEloAnalyticsEventData(optionValuesString: String?): Map<String, String>? {
        return try {
            optionValuesString?.takeIf { it.isNotEmpty() }?.let {
                json.decodeFromString<Map<String, String>>(it)
            } ?: emptyMap()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }

    @TypeConverter
    fun toEloAnalyticsEventData(map: Map<String, String>?): String? {
        return try {
            map?.let { json.encodeToString(it) }.orDefault()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}