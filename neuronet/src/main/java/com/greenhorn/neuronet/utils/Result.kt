package com.greenhorn.neuronet.utils

import okhttp3.Headers
import retrofit2.Response


internal sealed class Result<out T : Any>
internal data class Success<out T : Any>(val data: T) : Result<T>()
internal data class Failure(val errorResponse: ErrorResponse, val throwable: Throwable? = null) : Result<Nothing>()

internal inline fun <T : Any> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
  if (this is Success) action(data)
  return this
}

internal inline fun <T : Any> Result<T>.onFailure(action: (ErrorResponse) -> Unit) {
  if (this is Failure) action(errorResponse)
}

internal inline fun <T : Any> Result<T>.onFailureWithThrowable(action: (ErrorResponse, Throwable?) -> Unit) {
  if (this is Failure) action(errorResponse, throwable)
}


internal sealed class NetworkResult<out T : Any> {
  data class Success<T : Any>(var data : T? =null, var headers: Headers) : NetworkResult<T>()
  data class HttpFailure(val message : String,  val errorCode : Int) : NetworkResult<Nothing>()
  data class Failure(var errorMessage : String?, val errorCode: Int) : NetworkResult<Nothing>()
}

internal sealed class NetworkResponse<out T : Any> {
  data class Success<T : Any>(val response : Response<T>) : NetworkResponse<T>()
  data class Failure(val exception: Exception, val errorCode : Int, val errorBody: String? = null) : NetworkResponse<Nothing>()
}


