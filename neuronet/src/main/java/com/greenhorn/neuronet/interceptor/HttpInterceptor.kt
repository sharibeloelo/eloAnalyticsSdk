package com.greenhorn.neuronet.interceptor

import com.greenhorn.neuronet.listener.HeaderProvider
import okhttp3.Interceptor
import okhttp3.Response

internal class HttpInterceptor(private val headerProvider: HeaderProvider) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val builder = originalRequest.newBuilder()

        if(headerProvider.getHeaders().isNotEmpty()) {
            headerProvider.getHeaders().forEach { (key, value) ->
                builder.header(key, value)
            }
        }

        return chain.proceed(builder.build())
    }
}