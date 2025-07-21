package com.greenhorn.neuronet.utils

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    @SerialName("code") val code: String? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("data") val data: ErrorData? = null
) {
    companion object {
        fun createEmpty() = ErrorResponse(
            code = "",
            message = "",
            data = ErrorData(status = "")
        )
    }
}

@Serializable
data class EPLErrorResponse(
    @SerialName("code") val code: String? = null,
    @SerialName("message") val message: String? = null
)

@Serializable
data class ErrorData(
    @SerialName("status") val status: String
)
