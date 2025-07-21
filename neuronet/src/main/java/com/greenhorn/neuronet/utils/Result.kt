package com.greenhorn.neuronet.utils

import okhttp3.Headers
import retrofit2.Response


sealed class Result<out T : Any>
data class Success<out T : Any>(val data: T) : Result<T>()
data class Failure(val errorResponse: ErrorResponse, val throwable: Throwable? = null) : Result<Nothing>()

class HttpError(val throwable: Throwable, val errorCode: Int = 0)

inline fun <T : Any> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
  if (this is Success) action(data)
  return this
}

inline fun <T : Any> Result<T>.onFailure(action: (ErrorResponse) -> Unit) {
  if (this is Failure) action(errorResponse)
}

inline fun <T : Any> Result<T>.onFailureWithThrowable(action: (ErrorResponse, Throwable?) -> Unit) {
  if (this is Failure) action(errorResponse, throwable)
}


sealed class NetworkResult<out T : Any> {
  data class Success<T : Any>(var data : T? =null, var headers: Headers) : NetworkResult<T>()
  data class HttpFailure(val message : String,  val errorCode : Int) : NetworkResult<Nothing>()
  data class Failure(var errorMessage : String?, val errorCode: Int) : NetworkResult<Nothing>()
}

sealed class NetworkResponse<out T : Any> {
  data class Success<T : Any>(val response : Response<T>) : NetworkResponse<T>()
  data class Failure(val exception: Exception, val errorCode : Int, val errorBody: String? = null) : NetworkResponse<Nothing>()
}


