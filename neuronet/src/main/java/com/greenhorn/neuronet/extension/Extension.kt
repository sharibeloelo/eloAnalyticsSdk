package com.greenhorn.neuronet.extension

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal fun CoroutineScope.safeLaunch(
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

fun String?.orDefault(default: String = ""): String = this ?: default

fun Int?.orDefault(defaultValue: Int = 0) = this ?: defaultValue

fun Long?.orDefault(defaultValue: Long = 0L) = this ?: defaultValue
