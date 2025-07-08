package com.greenhorn.neuronet.extension

import com.greenhorn.neuronet.AnalyticsEvent
import com.greenhorn.neuronet.constant.Constant.EVENT_NAME
import com.greenhorn.neuronet.constant.Constant.PRIMARY_ID
import com.greenhorn.neuronet.constant.Constant.SESSION_ID
import com.greenhorn.neuronet.constant.Constant.TIME_STAMP
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

fun CoroutineScope.safeLaunch(
    launchBody: suspend CoroutineScope.() -> Unit,
    handleError: (Throwable) -> Unit = {},
): Job {
    val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        handleError(throwable)
    }

    return launch(exceptionHandler) {
        launchBody()
    }
}

fun AnalyticsEvent.toMap(): Map<String, Any> {
    // Start with the main event properties in a mutable map
    val eventAsMap = mutableMapOf<String, Any>(
        EVENT_NAME to this.eventName,
        TIME_STAMP to this.timestamp,
        PRIMARY_ID to this.primaryId,
        SESSION_ID to this.sessionId
    )

    // Add all the key-value pairs from your payload into the main map
    // This flattens the structure, just like you were doing before.
    // Note: Ensure your payload values are simple types like String, Int, Boolean, etc.
    eventAsMap.putAll(this.payload)

    return eventAsMap
}

//fun AnalyticsEvent.toJsonObject() = JSONObject().apply {
//    put(EVENT_NAME, JsonPrimitive(this@toJsonObject.eventName))
//    put(TIME_STAMP, JsonPrimitive(this@toJsonObject.timestamp))
//    put(PRIMARY_ID, JsonPrimitive(this@toJsonObject.primaryId))
//    put(SESSION_ID, JsonPrimitive(this@toJsonObject.sessionId))
//
//    this@toJsonObject.payload.forEach { (key, value) ->
//        put(key, JsonPrimitive(value as String))
//    }
//}