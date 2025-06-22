package com.greenhorn.neuronet.interceptor

import okhttp3.Interceptor
import okhttp3.Response

class HttpInterceptor(val headers: Map<String, String>) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val builder = originalRequest.newBuilder()

        headers.forEach { (key, value) ->
            builder.header(key, value)
        }

        return chain.proceed(builder.build())
    }
}