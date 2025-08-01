package com.greenhorn.neuronet.utils

import io.ktor.client.statement.HttpResponse
import io.ktor.http.Headers

/**
 * Sealed class representing the result of an operation.
 * 
 * This class provides a type-safe way to handle both successful and failed operations.
 * It uses the sealed class pattern to ensure exhaustive handling of all possible states.
 * 
 * @param T The type of data returned on success
 */
internal sealed class Result<out T : Any>

/**
 * Represents a successful operation result.
 * 
 * @param data The data returned by the successful operation
 */
internal data class Success<out T : Any>(val data: T) : Result<T>()

/**
 * Represents a failed operation result.
 * 
 * @param errorResponse Details about the error that occurred
 * @param throwable Optional exception that caused the failure
 */
internal data class Failure(val errorResponse: ErrorResponse, val throwable: Throwable? = null) : Result<Nothing>()

/**
 * Executes an action on success and returns the original result.
 * 
 * This extension function allows for side effects to be performed when a result is successful,
 * while maintaining the original result for further processing.
 * 
 * @param action The action to perform on successful results
 * @return The original result unchanged
 */
internal inline fun <T : Any> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
  if (this is Success) action(data)
  return this
}

/**
 * Executes an action on failure.
 * 
 * This extension function allows for side effects to be performed when a result is a failure.
 * 
 * @param action The action to perform on failed results
 */
internal inline fun <T : Any> Result<T>.onFailure(action: (ErrorResponse) -> Unit) {
  if (this is Failure) action(errorResponse)
}

/**
 * Executes an action on failure with access to both error response and throwable.
 * 
 * This extension function provides access to both the error response and the original
 * exception that caused the failure.
 * 
 * @param action The action to perform on failed results with throwable access
 */
internal inline fun <T : Any> Result<T>.onFailureWithThrowable(action: (ErrorResponse, Throwable?) -> Unit) {
  if (this is Failure) action(errorResponse, throwable)
}

/**
 * Sealed class representing the result of a network operation.
 * 
 * This class provides detailed information about network operations including
 * success with data and headers, HTTP failures, and network-level failures.
 * 
 * @param T The type of data returned on success
 */
internal sealed class NetworkResult<out T : Any> {
  /**
   * Represents a successful network operation.
   * 
   * @param data The data returned by the network operation (can be null)
   * @param headers HTTP headers from the response
   */
  data class Success<T : Any>(var data : T? = null, var headers: Headers) : NetworkResult<T>()
  
  /**
   * Represents an HTTP-level failure (4xx, 5xx status codes).
   * 
   * @param message Human-readable error message
   * @param errorCode HTTP status code
   */
  data class HttpFailure(val message : String, val errorCode : Int) : NetworkResult<Nothing>()
  
  /**
   * Represents a network-level failure (timeout, connection issues).
   * 
   * @param errorMessage Optional error message
   * @param errorCode Error code (0 for network-level issues)
   */
  data class Failure(var errorMessage : String?, val errorCode: Int) : NetworkResult<Nothing>()
}

/**
 * Sealed class representing the raw network response.
 * 
 * This class wraps the actual HTTP response from Ktor and provides
 * a consistent interface for handling both successful and failed responses.
 * 
 * @param T The type of data expected in the response
 */
internal sealed class NetworkResponse<out T : Any> {
  /**
   * Represents a successful HTTP response.
   * 
   * @param response The raw HttpResponse from Ktor
   */
  data class Success<T : Any>(val response : HttpResponse) : NetworkResponse<T>()
  
  /**
   * Represents a failed HTTP response or network error.
   * 
   * @param exception The exception that occurred
   * @param errorCode HTTP status code or 0 for network errors
   * @param errorBody Optional error response body
   */
  data class Failure(val exception: Exception, val errorCode : Int, val errorBody: String? = null) : NetworkResponse<Nothing>()
}


