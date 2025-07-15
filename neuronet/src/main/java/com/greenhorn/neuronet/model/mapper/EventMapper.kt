package com.greenhorn.neuronet.model.mapper

import com.greenhorn.neuronet.EloAnalyticsEvent
import com.greenhorn.neuronet.model.EloAnalyticsEventDto

object EventMapper {
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

fun EloAnalyticsEvent.toEventDto(guestUserId: Long, currentUserId: Long): EloAnalyticsEventDto {
    val userId = if (this.isUserLogin) currentUserId else guestUserId
    return EloAnalyticsEventDto(
        eventName = this.eventName,
        eventTimeStamp = this.eventTimestamp.toString(),
        primaryId = "${userId}_${this.eventTimestamp}",
        sessionId = "${userId}_${this.sessionTimeStamp}",
        eventData = this.eventData,
    )
}