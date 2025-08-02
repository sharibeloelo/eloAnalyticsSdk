package com.greenhorn.neuronet.utils

import com.greenhorn.neuronet.constant.Constant
import com.greenhorn.neuronet.extension.orDefault

data class EloAnalyticsConfig(
    val batchSize: Int = 10,
    val baseUrl: String,
    val endpointUrl: String,
    val isDebug: Boolean = false,
    val appsFlyerId: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val userIdAttributeKeyName: String,
    val syncBatchSize: Int? = null // Optional sync batch size
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
    private var syncBatchSize: Int = Constant.DEFAULT_SYNC_BATCH_SIZE

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

    /**
     * Sets the sync batch size with validation.
     * 
     * If the provided batch size is less than 1000 or null, it will use the default
     * batch size and log a warning message to the user.
     * 
     * @param batchSize The batch size to set (must be >= 1000)
     */
    internal fun setSyncBatchSize(batchSize: Int?) {
        when {
            batchSize == null -> {
                EloSdkLogger.w("Sync batch size is null, using default value of ${Constant.DEFAULT_SYNC_BATCH_SIZE}")
                syncBatchSize = Constant.DEFAULT_SYNC_BATCH_SIZE
            }
            batchSize < Constant.MIN_SYNC_BATCH_SIZE -> {
                EloSdkLogger.e("❌ ERROR: Sync batch size ($batchSize) cannot be less than ${Constant.MIN_SYNC_BATCH_SIZE}. Using default value of ${Constant.DEFAULT_SYNC_BATCH_SIZE}")
                EloSdkLogger.w("💡 TIP: Set syncBatchSize to at least ${Constant.MIN_SYNC_BATCH_SIZE} for optimal performance")
                syncBatchSize = Constant.DEFAULT_SYNC_BATCH_SIZE
            }
            else -> {
                EloSdkLogger.d("✅ Sync batch size set to $batchSize")
                syncBatchSize = batchSize
            }
        }
    }

    /**
     * Gets the configured sync batch size.
     * 
     * @return The validated sync batch size (always >= 1000)
     */
    internal fun getSyncBatchSize(): Int = syncBatchSize

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