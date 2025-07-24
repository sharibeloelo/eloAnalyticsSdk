package com.greenhorn.neuronet.model.mapper

import com.greenhorn.neuronet.model.EloAnalyticsEvent
import com.greenhorn.neuronet.model.EloAnalyticsEventDto
import kotlinx.serialization.json.JsonArray


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

internal fun Map<String, Any>.toStringMap(): Map<String, String> {
    return this.mapValues { entry -> entry.value.toString() }
}

internal fun List<EloAnalyticsEventDto>.toJsonArray(): JsonArray {
    return JsonArray(this.map { it.toJsonObject() })
}

internal fun EloAnalyticsEvent.toEventDto(
    guestUserId: Long,
    currentUserId: Long
): EloAnalyticsEventDto {
    val userId = if (this.isUserLogin) currentUserId else guestUserId
    return EloAnalyticsEventDto(
        eventName = this.eventName,
        eventTimeStamp = this.eventTimestamp,
        primaryId = "${userId}_${this.eventTimestamp}",
        sessionId = "${userId}_${this.sessionTimeStamp}",
        eventData = this.eventData,
    )
}