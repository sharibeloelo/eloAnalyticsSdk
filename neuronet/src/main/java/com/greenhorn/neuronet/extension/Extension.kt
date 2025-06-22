package com.greenhorn.neuronet.extension

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.greenhorn.neuronet.constant.Constant.EVENT_NAME
import com.greenhorn.neuronet.constant.Constant.PRIMARY_ID
import com.greenhorn.neuronet.constant.Constant.SESSION_ID
import com.greenhorn.neuronet.constant.Constant.TIME_STAMP
import com.greenhorn.neuronet.model.Event
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.collections.component1
import kotlin.collections.forEach

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

fun Event.toJsonObject() = JsonObject().apply {
    add(EVENT_NAME, JsonPrimitive(this@toJsonObject.eventName))
    add(TIME_STAMP, JsonPrimitive(this@toJsonObject.timestamp))
    add(PRIMARY_ID, JsonPrimitive(this@toJsonObject.primaryId))
    add(SESSION_ID, JsonPrimitive(this@toJsonObject.sessionId))

    this@toJsonObject.payload.forEach { (key, value) ->
        add(key, JsonPrimitive(value as String))
    }
}