package com.greenhorn.neuronet.log.utils

import com.squareup.moshi.Json

data class ErrorResponse(
    @Json(name = "code") val code: String?,
    @Json(name = "message") val message: String?,
    @Json(name = "data") val data: ErrorData?
) {
    companion object {
        fun createEmpty() = ErrorResponse(
            code = "",
            message = "",
            data = ErrorData(status = "")
        )
    }
}

data class EPLErrorResponse(
    @Json(name = "code") val code: String?,
    @Json(name = "message") val message: String?
)

data class ErrorData(
    @Json(name = "status") val status: String?
)
