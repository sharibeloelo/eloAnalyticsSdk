package com.greenhorn.neuronet.model

import com.greenhorn.neuronet.AnalyticsEvent
import com.greenhorn.neuronet.extension.toEventDto

object EventMapper {
    fun toEventDtos(
        events: List<AnalyticsEvent>,
        currentUserId: Long,
        guestUserId: Long
    ): List<EloAnalyticsEventDto> {
        return events.map { event ->
            event.toEventDto(currentUserId = currentUserId, guestUserId = guestUserId)
        }
    }
}