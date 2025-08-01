package com.greenhorn.neuronet.utils

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.SocketTimeoutException

// HTTP status codes for response validation
private const val CODE_200 = 200
private const val CODE_201 = 201
private const val CODE_600_ANDROID = 600
private const val CODE_503_NO_INTERNET = 503

// Error message for network connectivity issues
private const val NO_INTERNET = "No Internet Connection!"

/**
 * Base repository class providing common network handling functionality.
 * 
 * This abstract class provides a foundation for repository implementations that need
 * to perform HTTP requests. It includes:
 * - Standardized network call execution
 * - Common error handling patterns
 * - JSON response parsing
 * - Network result processing
 * 
 * The class is designed to work with Ktor HTTP client and provides a consistent
 * interface for handling HTTP responses and errors across the application.
 * 
 * @author EloAnalytics SDK Team
 * @since 1.0.0
 */
internal open class BaseRepository {

    private val TAG = "BaseRepository"
    var message = ""
    
    // Configured JSON parser with lenient settings for robust parsing
    private val json by lazy {
        Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
            coerceInputValues = true
        }
    }

    /**
     * Safely decodes JSON string to the specified type.
     * 
     * @param jsonString The JSON string to decode
     * @return Decoded object of type T, or null if decoding fails
     */
    private inline fun <reified T> decodeJson(jsonString: String): T? {
        return try {
            json.decodeFromString<T>(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Executes a network call with standardized error handling and response processing.
     * 
     * This method provides a consistent way to execute HTTP requests and process
     * responses. It handles:
     * - HTTP status code validation
     * - Response body parsing
     * - Error response creation
     * - Exception handling
     * 
     * @param networkCall Suspending function that performs the actual HTTP request
     * @return NetworkResult<T> containing the processed response or error details
     */
    suspend fun <T : Any> doNetworkCall(
        networkCall: suspend () -> HttpResponse
    ): NetworkResult<T> {
        val networkResponse: NetworkResponse<T> = invokeNetworkCall(networkCall)
        
        if (networkResponse is NetworkResponse.Success) {
            val response = networkResponse.response
            return if (response.status.value == CODE_200 || response.status.value == CODE_201) {
                // Successful response - return success with headers
                NetworkResult.Success(data = null, headers = response.headers)
            } else {
                // HTTP error - return failure with status details
                NetworkResult.Failure(response.status.description, response.status.value)
            }
        } else if (networkResponse is NetworkResponse.Failure) {
            // Try to parse error response body for additional details
            try {
                if (networkResponse.errorBody.isNullOrEmpty().not()) {
                    decodeJson<ErrorResponse>(networkResponse.errorBody.orEmpty())?.let {
                        message = it.message.orEmpty()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // Handle special cases for specific endpoints (legacy support)
        try {
            if (networkResponse.toString().split("path").isNotEmpty() && networkResponse.toString()
                    .split("path").getOrNull(1).orEmpty().contains("otp/send")
            ) {
                val mesg = networkResponse.toString().split("message").getOrNull(1).orEmpty().split("Exception: ").getOrNull(1)
                message = mesg.orEmpty().split(",").getOrNull(0).orEmpty()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Return HTTP failure with processed error details
        return when (networkResponse) {
            is NetworkResponse.Failure -> NetworkResult.HttpFailure(
                message, networkResponse.errorCode
            )
            else -> NetworkResult.HttpFailure(
                message, 0
            )
        }
    }

    /**
     * Invokes the network call and wraps the response in a NetworkResponse.
     * 
     * This method handles the low-level HTTP call execution and provides
     * consistent error handling for network-related exceptions.
     * 
     * @param networkCall The suspending function to execute
     * @return NetworkResponse<T> containing the HTTP response or error details
     */
    private suspend fun <T : Any> invokeNetworkCall(networkCall: suspend () -> HttpResponse): NetworkResponse<T> {
        return try {
            val response = networkCall.invoke()
            if (response.status.isSuccess()) {
                // Successful HTTP response
                NetworkResponse.Success(response)
            } else {
                // HTTP error response - extract error body safely
                val errorBody = try {
                    response.bodyAsText()
                } catch (e: Exception) {
                    ""
                }
                NetworkResponse.Failure(
                    IOException(),
                    response.status.value,
                    errorBody = errorBody
                )
            }
        } catch (e: IOException) {
            // Network-level exception (timeout, connection issues)
            NetworkResponse.Failure(
                SocketTimeoutException(e.localizedMessage), 0
            )
        }
    }

    /**
     * Creates an empty error response for fallback scenarios.
     * 
     * @return ErrorResponse with empty values
     */
    protected fun createEmptyErrorResponse(): ErrorResponse {
        return ErrorResponse(
            code = "",
            message = "",
            ErrorData(status = "")
        )
    }

    /**
     * Creates an error response from HTTP failure details.
     * 
     * @param response NetworkResult.HttpFailure containing error information
     * @return ErrorResponse with formatted error details
     */
    protected fun createErrorResponse(response: NetworkResult.HttpFailure): ErrorResponse {
        return ErrorResponse(
            code = response.errorCode.toString(),
            message = response.message,
            ErrorData(status = "")
        )
    }

    /**
     * Creates an error response from network failure details.
     * 
     * @param response NetworkResult.Failure containing error information
     * @return ErrorResponse with formatted error details
     */
    protected fun createErrorResponse(response: NetworkResult.Failure): ErrorResponse {
        return ErrorResponse(
            code = response.errorCode.toString(),
            message = response.errorMessage,
            ErrorData(status = "")
        )
    }

    /**
     * Creates an error response from an exception.
     * 
     * @param e The exception that occurred
     * @return ErrorResponse with exception details
     */
    protected fun createExceptionResponse(e: Exception): ErrorResponse {
        return ErrorResponse(
            code = CODE_600_ANDROID.toString(),
            message = e.message,
            ErrorData(status = "")
        )
    }

    /**
     * Handles the case when no internet connectivity is available.
     * 
     * @return Result<Boolean> indicating failure due to no internet
     */
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

/**
 * Data wrapper for paginated API responses.
 * 
 * This class represents the structure of paginated responses from the analytics API,
 * containing metadata about the current page, total elements, and the actual content.
 * 
 * @param T The type of content in the response
 */
data class DataSessionWrapper<T>(
    val content: List<T>,
    val empty: Boolean,
    val first: Boolean,
    val last: Boolean,
    val number: Int,
    val numberOfElements: Int,
    val size: Int,
    val totalElements: Int,
    val totalPages: Int
)

