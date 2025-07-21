package com.greenhorn.neuronet.utils

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ErrorResponse(
    @SerialName("code") val code: String? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("data") val data: ErrorData? = null
)

@Serializable
internal data class ErrorData(
    @SerialName("status") val status: String
)
