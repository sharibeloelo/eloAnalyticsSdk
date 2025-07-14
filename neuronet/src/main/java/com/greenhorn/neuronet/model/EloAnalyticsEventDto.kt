package com.greenhorn.neuronet.model

import java.io.Serializable

data class EloAnalyticsEventDto(
    val eventName: String,
    val eventTimeStamp: String,
    val primaryId: String,
    val sessionId: String,
    val eventData: Map<String, Any>
): Serializable {

    companion object {
        private const val EVENT_NAME = "ep_event_name"
        const val TIME_STAMP = "ep_time_stamp"
        const val APPS_FLYER_ID = "appsflyer_id"
        private const val PRIMARY_ID = "ep_primary_id"
        private const val SESSION_ID = "ep_session_id"
    }
}