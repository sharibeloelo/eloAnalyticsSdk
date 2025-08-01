package com.greenhorn.neuronet.utils

import com.greenhorn.neuronet.extension.orDefault

data class EloAnalyticsConfig(
    val batchSize: Int = 10,
    val baseUrl: String,
    val endpointUrl: String,
    val isDebug: Boolean = false,
    val appsFlyerId: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val userIdAttributeKeyName: String
)

enum class FlushPendingEventTriggerSource {
    APP_MINIMIZE,
    ACTIVITY_DESTROY,
    EVENT_BATCH_COUNT_COMPLETE
}

object AnalyticsSdkUtilProvider {
    private var apiFinalUrl: String? = null
    private var dataProvider: EloAnalyticsRuntimeProvider? = null
    private var sessionTimeStamp: String? = null
    private var userId: Long = 0L
    private var guestUserId: Long = 0L

    private var userIdAttributeKeyName: String? = null

    internal fun initialize(provider: EloAnalyticsRuntimeProvider) {
        dataProvider = provider
    }

    internal fun getUserIdAttributeKeyName(): String? = userIdAttributeKeyName
    internal fun setUserIdAttributeKeyName(keyName: String?) {
        userIdAttributeKeyName = keyName
    }

    internal fun setApiEndPoint(endPoint: String) {
        apiFinalUrl = endPoint
    }

    internal fun getApiEndPoint(): String {
        return apiFinalUrl.orEmpty()
    }

    // ✅ Set and cache the session timestamp
    internal fun updateSessionTimeStampAndCache(timeStamp: String) {
        sessionTimeStamp = timeStamp
    }

    // ✅ Get from cache or fetch from app
    internal fun getSessionTimeStamp(): String {
        return sessionTimeStamp.orEmpty()
    }

    internal suspend fun isUserLoggedIn(): Boolean? {
        return dataProvider?.isUserLoggedIn()
    }

    internal suspend fun getGuestUserId(): Long {
        if (guestUserId == 0L) {
            guestUserId = dataProvider?.getGuestUserId().orDefault()
        }
        return guestUserId
    }

    internal suspend fun getCurrentUserId(): Long {
        if (userId == 0L) {
            userId = dataProvider?.getCurrentUserId().orDefault()
        }
        return userId
    }

    internal fun recordFirebaseNonFatal(error: Throwable) {
        error.printStackTrace()
    }
}


// 2. Runtime Provider Interface
interface EloAnalyticsRuntimeProvider {
    fun getAppVersionCode(): String
    suspend fun isUserLoggedIn(): Boolean

    suspend fun getCurrentUserId(): Long

    suspend fun getGuestUserId(): Long

    fun isAnalyticsSdkEnabled(): Boolean
}