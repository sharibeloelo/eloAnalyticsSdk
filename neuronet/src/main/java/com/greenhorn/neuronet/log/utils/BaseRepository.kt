package com.greenhorn.neuronet.log.utils

import com.squareup.moshi.Moshi
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

const val CODE_200 = 200
const val CODE_201 = 201
private const val CODE_600_ANDROID = 600
private const val CODE_503_NO_INTERNET = 503

private const val NO_INTERNET = "No Internet Connection!"

open class BaseRepository {

    private val TAG = "BaseRepository"
    var message = ""

    private val moshi by lazy { Moshi.Builder().build() }
    private val errorAdapter by lazy { moshi.adapter(ErrorResponse::class.java) }

    suspend fun <T : Any> doNetworkCall(
        networkCall: suspend () -> Response<T>
    ): NetworkResult<T> {
        val networkResponse: NetworkResponse<T> = invokeNetworkCall(networkCall)

        if (networkResponse is NetworkResponse.Success) {
            val response = networkResponse.response
            return if (response.code() == CODE_200 || response.code() == CODE_201) {
                NetworkResult.Success(response.body(), response.headers())
            } else {
                NetworkResult.Failure(response.message(), response.code())
            }
        } else if (networkResponse is NetworkResponse.Failure) {
            try {
                if (!networkResponse.errorBody.isNullOrEmpty()) {
                    message = errorAdapter.fromJson(networkResponse.errorBody ?: "")?.message.orEmpty()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        try {
            if (networkResponse.toString().contains("path") && networkResponse.toString()
                    .split("path").getOrNull(1).orEmpty().contains("otp/send")
            ) {
                val mesg = networkResponse.toString()
                    .split("message").getOrNull(1).orEmpty()
                    .split("Exception: ").getOrNull(1)
                message = mesg.orEmpty().split(",").getOrNull(0).orEmpty()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return NetworkResult.HttpFailure(
            message, (networkResponse as NetworkResponse.Failure).errorCode
        )
    }

    private suspend fun <T : Any> invokeNetworkCall(networkCall: suspend () -> Response<T>): NetworkResponse<T> {
        return try {
            val response = networkCall.invoke()
            if (response.isSuccessful && response.body() != null) {
                NetworkResponse.Success(response)
            } else {
                NetworkResponse.Failure(
                    IOException(),
                    response.code(),
                    errorBody = response.errorBody()?.string()
                )
            }
        } catch (e: IOException) {
            NetworkResponse.Failure(
                SocketTimeoutException(e.localizedMessage), 0
            )
        }
    }

    protected fun createEmptyErrorResponse(): ErrorResponse {
        return ErrorResponse(
            code = "",
            message = "",
            ErrorData(status = "")
        )
    }

    protected fun createErrorResponse(response: NetworkResult.HttpFailure): ErrorResponse {
        return ErrorResponse(
            code = response.errorCode.toString(),
            message = response.message,
            ErrorData(status = "")
        )
    }

    protected fun createErrorResponse(response: NetworkResult.Failure): ErrorResponse {
        return ErrorResponse(
            code = response.errorCode.toString(),
            message = response.errorMessage,
            ErrorData(status = "")
        )
    }

    protected fun createExceptionResponse(e: Exception): ErrorResponse {
        return ErrorResponse(
            code = CODE_600_ANDROID.toString(),
            message = e.message,
            ErrorData(status = "")
        )
    }

    protected fun handleNoInternetCase(): Result<Boolean> {
        return Failure(
            errorResponse = ErrorResponse(
                code = CODE_503_NO_INTERNET.toString(),
                message = NO_INTERNET,
                ErrorData(status = "")
            )
        )
    }
}
