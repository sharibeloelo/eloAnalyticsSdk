package com.greenhorn.neuronet.extension

import com.greenhorn.neuronet.constant.Constant.EVENT_NAME
import com.greenhorn.neuronet.constant.Constant.PRIMARY_ID
import com.greenhorn.neuronet.constant.Constant.SESSION_ID
import com.greenhorn.neuronet.constant.Constant.TIME_STAMP
import com.greenhorn.neuronet.model.EloAnalyticsEventDto
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