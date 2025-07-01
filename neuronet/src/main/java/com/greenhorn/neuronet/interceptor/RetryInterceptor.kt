package com.greenhorn.neuronet.interceptor

import okhttp3.Interceptor
import okio.IOException
import kotlin.math.pow
import kotlin.random.Random

//todo: we can use workManager only for High priority events also, since it also has retry mechanism and
// its more reliable since even if the app is killed or app crashed the data wont be lost(HIGHLY IMP. for us)
// Also, creating an interceptor is also little complicated which is not required here.
class RetryInterceptor(private val maxRetries: Int = 3) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        var request = chain.request()
        var response: okhttp3.Response? = null
        var exception: IOException? = null
        var tryCount = 0

        while (tryCount < maxRetries && (response == null || !response.isSuccessful)) {
            // Close the previous response body if it exists, to avoid resource leaks
            response?.close()

            try {
                response = chain.proceed(request)
                // Only retry on 5xx server errors or 429
                if (isRetriableError(response.code)) {
                    tryCount++
                    if (tryCount < maxRetries) {
                        // Implement your exponential backoff delay here
                        Thread.sleep(calculateDelay(tryCount))
                        continue // Retry the loop
                    }
                } else {
                    // Don't retry for success or client errors like 400, 401, 403, etc.
                    break
                }
            } catch (e: IOException) {
                exception = e
                tryCount++
            }
        }

        if (response == null && exception != null) {
            throw exception
        }

        return response!!
    }
}

private fun isRetriableError(code: Int): Boolean {
    return when (code) {
        429, 500, 502, 503, 504 -> true
        else -> false
    }
}

private fun calculateDelay(tryCount: Int): Long {
    // --- Configuration Constants ---

    // The base delay for the first retry attempt.
    val INITIAL_DELAY_MS: Long = 1000L // 1 second
    // The maximum delay we're willing to wait.
    val MAX_DELAY_MS: Long = 16000L // 16 seconds
    // The multiplier for the exponential growth. A factor of 2.0 means doubling the delay.
    val BACKOFF_FACTOR: Double = 2.0
    // The percentage of randomness to apply (e.g., 0.2 means +/- 20% jitter).
    val JITTER_FACTOR: Double = 0.2

    // --- Calculation Logic ---

    // 1. Calculate the base exponential delay.
    // The exponent is (tryCount - 1) so the first retry (tryCount=1) has a delay of INITIAL_DELAY_MS * (2^0) = INITIAL_DELAY_MS.
    val exponentialDelay = INITIAL_DELAY_MS * (BACKOFF_FACTOR.pow(tryCount - 1))

    // 2. Calculate the random jitter.
    // This creates a random value in the range of [-jitter, +jitter].
    val jitter = (exponentialDelay * JITTER_FACTOR * (Random.nextDouble() * 2 - 1)).toLong()

    // 3. Add the jitter to the base delay.
    val finalDelay = (exponentialDelay + jitter).toLong()

    // 4. Ensure the final delay does not exceed the maximum allowed delay.
    // The coerceIn function is a clean way to enforce min/max bounds.
    return finalDelay.coerceIn(INITIAL_DELAY_MS, MAX_DELAY_MS)
}