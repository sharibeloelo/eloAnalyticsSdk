package com.greenhorn.neuronet.utils

import com.greenhorn.neuronet.constant.DataStoreConstants

// 1. Config Data Class
class EloAnalyticsConfigBuilder {
    var batchSize: Int = 50
    var endpointUrl: String = ""
    var appsFlyerId: String? = null

    fun build() = EloAnalyticsConfig(batchSize, endpointUrl, appsFlyerId)
}

data class EloAnalyticsConfig(
    val batchSize: Int?,
    val endpointUrl: String,
    val appsFlyerId: String?
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

    fun initialize(provider: EloAnalyticsRuntimeProvider) {
        dataProvider = provider
    }

    fun setApiEndPoint(endPoint: String) {
        apiFinalUrl = endPoint
    }

    fun getApiEndPoint(): String {
        return apiFinalUrl.orEmpty()
    }

    // ✅ Set and cache the session timestamp
    fun updateSessionTimeStampAndCache(timeStamp: String) {
        sessionTimeStamp = timeStamp
    }

    // ✅ Get from cache or fetch from app
    fun getSessionTimeStamp(): String {
        return sessionTimeStamp.orEmpty()
    }

    suspend fun isUserLoggedIn(): Boolean? {
        return dataProvider?.isUserLoggedIn()
    }

    suspend fun getGuestUserId(): Long {
        if (guestUserId == 0L) {
            guestUserId = dataProvider?.getGuestUserId(DataStoreConstants.GUEST_USER_ID) ?: 0L
        }
        return guestUserId
    }

    suspend fun getCurrentUserId(): Long {
        if (userId == 0L) {
            userId = dataProvider?.getCurrentUserId(DataStoreConstants.USER_ID) ?: 0
        }
        return userId
    }

    fun recordFirebaseNonFatal(error: Throwable) {
        error.printStackTrace()
    }

}


// 2. Runtime Provider Interface
interface EloAnalyticsRuntimeProvider {
    fun getHeaders(): Map<String, String>
    fun getAppVersionCode(): String
    suspend fun isUserLoggedIn(): Boolean

    suspend fun getCurrentUserId(key: String): Long

    suspend fun getGuestUserId(key: String): Long

    fun isAnalyticsSdkEnabled(): Boolean
}