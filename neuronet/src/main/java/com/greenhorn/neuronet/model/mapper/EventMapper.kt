package com.greenhorn.neuronet.model.mapper

import com.greenhorn.neuronet.model.EloAnalyticsEvent
import com.greenhorn.neuronet.model.EloAnalyticsEventDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement

internal object EventMapper {
    fun toEventDtos(
        events: List<EloAnalyticsEvent>,
        currentUserId: Long,
        guestUserId: Long
    ): List<EloAnalyticsEventDto> {

        return events.map {
            it.toEventDto(
                currentUserId = currentUserId,
                guestUserId = guestUserId
            )
        }
    }
}

fun Map<String, Any>.toStringMap(): Map<String, String> {
    return this.mapValues { entry -> entry.value.toString() }
}

internal fun List<EloAnalyticsEventDto>.toJsonObject(): kotlinx.serialization.json.JsonArray {
    val json = Json { encodeDefaults = true }
    val elements: List<JsonElement> = this.map { json.encodeToJsonElement(it) }
    return kotlinx.serialization.json.JsonArray(elements)
}
internal fun EloAnalyticsEvent.toEventDto(guestUserId: Long, currentUserId: Long): EloAnalyticsEventDto {
    val userId = if (this.isUserLogin) currentUserId else guestUserId
    return EloAnalyticsEventDto(
        eventName = this.eventName,
        eventTimeStamp = this.eventTimestamp,
        primaryId = "${userId}_${this.eventTimestamp}",
        sessionId = "${userId}_${this.sessionTimeStamp}",
        eventData = this.eventData,
    )
}