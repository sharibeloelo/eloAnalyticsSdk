package com.greenhorn.neuronet

import android.app.Activity
import android.content.Context
import android.util.Log
import com.greenhorn.neuronet.worker.syncWorker.EloAnalyticsWorkerUtils
import com.greenhorn.neuronet.db.AnalyticsDatabase
import com.greenhorn.neuronet.db.repository.EloAnalyticsLocalRepositoryImpl
import com.greenhorn.neuronet.db.usecase.EloAnalyticsLocalEventUseCase
import com.greenhorn.neuronet.db.usecase.EloAnalyticsLocalEventUseCaseImpl
import com.greenhorn.neuronet.extension.safeLaunch
import com.greenhorn.neuronet.listener.EloAnalyticsEventManager
import com.greenhorn.neuronet.listener.FlushPendingEventTriggerSource
import com.greenhorn.neuronet.log.utils.DataStoreConstants
import com.greenhorn.neuronet.model.EloAnalyticsEventDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.ref.WeakReference

/**
 * The main class for the Analytics SDK.
 * Use the [Builder] to create an instance of this class.
 *
 * @property context The application context.
 * @property repository The repository for data operations.
 * @property workManager The WorkManager instance for scheduling background tasks.
 * @property apiEndpoint The backend URL to send events to.
 */

// 1. Config Data Class
class EloAnalyticsConfigBuilder {
    var batchSize: Int = 50
    var endpointUrl: String = ""
    var appsFlyerId: String? = null

    fun build() = EloAnalyticsConfig(batchSize, endpointUrl, appsFlyerId)
}

data class EloAnalyticsConfig(
    val batchSize: Int,
    val endpointUrl: String,
    val appsFlyerId: String?
)


// 2. Runtime Provider Interface
interface EloAnalyticsRuntimeProvider {
    suspend fun isUserLoggedIn(): Boolean
    fun getHeaders(): Map<String, String>
    fun getAppVersionCode(): String
    fun setSessionTimeStamp(timeStamp: String): String?

    suspend fun getCurrentUserId(key: String): Long

    suspend fun getGuestUserId(key: String): Long

    fun isAnalyticsSdkEnabled(): Boolean
}


fun Map<String, Any>.toStringMap(): Map<String, String> {
    return this.mapValues { entry -> entry.value.toString() }
}

// In SDK
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
    fun setSessionTimeStampAndCache(timeStamp: String) {
        val result = dataProvider?.setSessionTimeStamp(timeStamp)
        sessionTimeStamp = result
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


class EloAnalyticsSdk private constructor(
    private val context: Context,
    private val eloAnalyticUtils: AnalyticsSdkUtilProvider,
    private val useCase: EloAnalyticsLocalEventUseCase,
    private val config: EloAnalyticsConfig,
    private val runtimeProvider: EloAnalyticsRuntimeProvider
) : EloAnalyticsEventManager {
    private val contextRef by lazy { WeakReference(context) }

    private val supervisorScope by lazy {
        CoroutineScope(IO + SupervisorJob())
    }

    private val counterLock by lazy { Mutex() }
    @Volatile
    private var eventCounter = 0

    private val DEFAULT_TRIGGER_SIZE = 10


    fun trackEvent(name: String, eventData: MutableMap<String, Any>) {
        if (!runtimeProvider.isAnalyticsSdkEnabled()) return

        val eventTs =
            eventData[EloAnalyticsEventDto.TIME_STAMP] as? String ?: System.currentTimeMillis()
                .toString()
        eventData.put(EloAnalyticsEventDto.APPS_FLYER_ID, config.appsFlyerId.orEmpty())

        supervisorScope.safeLaunch({
            val event = EloAnalyticsEvent(
                eventName = name,
                isUserLogin = runtimeProvider.isUserLoggedIn(),
                eventTimestamp = eventTs,
                sessionTimeStamp = eloAnalyticUtils.getSessionTimeStamp(),
                eventData = eventData.toStringMap()
            )

            Log.d(TAG, "EloTrackEvent: ${event.logEvent()}")
            insertEventAndIncrementCounter(event)
        }, {
            it.printStackTrace()
            Log.e(TAG2, "Got error-> ${it.message}")
            eloAnalyticUtils.recordFirebaseNonFatal(error = it)
        })
    }

    /**
     * Inserts an event into the local repository and increments the event counter.
     *
     * @param event The event to insert.
     */
    private suspend fun insertEventAndIncrementCounter(event: EloAnalyticsEvent) {
        Log.d(TAG2, "Inserting Elo events in DB-> ${event.eventName}")

        val isInserted = useCase.insertEvent(event)
        Log.d(TAG2, "Event <-${event.eventName}-> inserted: $isInserted")

        incrementCounterAndCheckBatch()
    }

    /**
     * Increments the event counter and checks if a batch size limit is reached to enqueue a new work request.
     */
    private suspend fun incrementCounterAndCheckBatch() {
        try {
            counterLock.withLock {
                eventCounter++

                val triggerCountSize =
                    (config.batchSize ?: DEFAULT_TRIGGER_SIZE)
                val isFlushPendingEvents = eventCounter >= triggerCountSize
                Log.d(TAG2,
                    "counter-> $eventCounter | BATCH_SIZE $triggerCountSize: isFlushPendingEvents: $isFlushPendingEvents"
                )

                if (isFlushPendingEvents) {
                    flushPendingEvents(flushPendingEventTriggerSource = FlushPendingEventTriggerSource.EVENT_BATCH_COUNT_COMPLETE)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG2, "Exception in incrementCounterAndCheckBatch: error-> ${e.message}")
            eloAnalyticUtils.recordFirebaseNonFatal(error = e)
        }
    }


//    fun setHeaders(headers: Map<String, String>) {
//        headerProvider.updateHeaders(headers)
//    }

    /**
     * Flush db pending events to server
     * */
    override fun flushPendingEvents(
        flushPendingEventTriggerSource: FlushPendingEventTriggerSource, activity: Activity? //todo: make weakreference
    ) {
        Log.d(
            TAG2,
            "flush pending events on ${flushPendingEventTriggerSource.name} currentActivity: ${activity?.localClassName}"
        )

        supervisorScope.safeLaunch({
            counterLock.withLock {
                Log.d(
                    TAG2,
                    "flush pending events but return if counter is zero and db count also; counter is $eventCounter!"
                )

                if (eventCounter == 0) {
                    val isPendingEvents = useCase.getEventsCount() != 0
                    Log.d(
                        TAG2, "is pending events in db count; $isPendingEvents!"
                    )
                    if (!isPendingEvents) return@safeLaunch
                }

                eventCounter = 0
                Log.d(TAG2, "counter reset to Zero")

                Log.d(TAG2, "Enqueuing new work request")

                contextRef.get()?.let {
                    EloAnalyticsWorkerUtils.enqueueAnalyticEventsWorkRequest(
                        context = it
                    )
                }
            }
        }, {
            it.printStackTrace()
            Log.e(TAG2, "Exception in incrementCounterAndCheckBatch: error-> ${it.message}")
            eloAnalyticUtils.recordFirebaseNonFatal(error = it)
        })
    }


    class Builder(private val context: Context) {
        private var runtimeProvider: EloAnalyticsRuntimeProvider? = null
        private var configBuilder: EloAnalyticsConfigBuilder = EloAnalyticsConfigBuilder()

        fun setConfig(block: EloAnalyticsConfigBuilder.() -> Unit) = apply { configBuilder.apply(block) }
        fun setRuntimeProvider(provider: EloAnalyticsRuntimeProvider) = apply { this.runtimeProvider = provider }

        fun build(): EloAnalyticsSdk {
            val finalConfig = configBuilder.build()
            if (finalConfig.endpointUrl.isBlank()) {
                Log.w("EloAnalyticsSdk", "Warning: endpointUrl not set")
            }

            val dao = AnalyticsDatabase.getInstance(context).eloAnalyticsEventDao()
            val repo = EloAnalyticsLocalRepositoryImpl(dao)
            val useCase = EloAnalyticsLocalEventUseCaseImpl(repo)
            val utils = AnalyticsSdkUtilProvider

            runtimeProvider?.let { utils.initialize(provider = it) }
            val instance = EloAnalyticsSdk(
                context = context,
                eloAnalyticUtils = utils,
                useCase = useCase,
                config = finalConfig,
                runtimeProvider = requireNotNull(runtimeProvider) { "EloAnalyticsRuntimeProvider is required" }
            )

            Companion.instance = instance
            return instance
        }
    }

    /**
     * Builder class for constructing an [EloAnalyticsSdk] instance.
     */
//    class Builder(private val context: Context) {
//        private var baseUrl: String? = null
//        private var apiEndpoint: String? = null
//        private var appFlyerId: String? = null
//        private var sessionId: String? = null
//        private var batchSize: Int = 10
//        private var headers: Map<String, String> = emptyMap()
//        private var customInterceptor: Interceptor? = null
//        private var customOkHttpClient: OkHttpClient? = null
//        private var coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
//        private var isDebug: Boolean = false
//
//        fun baseUrl(baseUrl: String) = apply { this.baseUrl = baseUrl }
//        fun apiEndpoint(endpoint: String) = apply { this.apiEndpoint = endpoint }
//        fun sessionId(sessionId: String) = apply { this.sessionId = sessionId }
//        fun appFlyerId(appFlyerId: String) = apply { this.appFlyerId = appFlyerId }
//        fun batchSize(batchSize: Int) = apply { this.batchSize = batchSize }
//        fun headers(headers: Map<String, String>) = apply { this.headers = headers }
//        fun scope(scope: CoroutineScope) = apply { this.coroutineScope = scope }
//        fun interceptor(interceptor: Interceptor) = apply { this.customInterceptor = interceptor }
//        fun okHttpClient(okHttpClient: OkHttpClient) = apply { this.customOkHttpClient = okHttpClient }
//
//        fun enableLogs(isDebug: Boolean) = apply {
//            this.isDebug = isDebug
//            Logger.initialize(enableLogs = isDebug)
//        }
//
//        fun build(): EloAnalyticsSdk {
//            // 1. Eagerly validate required parameters
//            checkNotNull(baseUrl) { "Base URL must be set." }
//            val finalApiEndpoint = requireNotNull(apiEndpoint) { "API endpoint must be set." }
//
//            // 2. Build dependencies in a clean, chained manner
//            val loggingInterceptor = HttpLoggingInterceptor().apply {
//                level = if (isDebug) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
//            }
//
//            val headerProvider = MutableHeaderProvider(headers)
//            // Use a custom OkHttpClient if provided, otherwise build a default one.
//            val finalOkHttpClient = customOkHttpClient ?: OkHttpClient.Builder()
//                .addInterceptor(customInterceptor ?: HttpInterceptor(headerProvider))
//                .addInterceptor(RetryInterceptor())
//                .addInterceptor(loggingInterceptor)
//                .retryOnConnectionFailure(true)
//                .connectTimeout(30, TimeUnit.SECONDS)
//                .readTimeout(30, TimeUnit.SECONDS)
//                .writeTimeout(30, TimeUnit.SECONDS)
//                .build()
//
//            val retrofit = Retrofit.Builder()
//                .baseUrl(baseUrl!!)
//                .client(finalOkHttpClient)
//                .addConverterFactory(MoshiConverterFactory.create())
//                .build()
//
//            val apiService = retrofit.create(ApiService::class.java)
//
//            // 3. Construct final dependencies for the main class
//            val database = AnalyticsDatabase.getInstance(context)
//            val eventRepository = EventRepository(database)
//            val apiClient = ApiClient(apiService)
//            val eventDispatcher = EventDispatcher(eventRepository, apiClient, finalApiEndpoint, batchSize)
//
//            return EloAnalyticsSdk(
//                context = context,
//                finalApiEndpoint = finalApiEndpoint,
//                eventDispatcher = eventDispatcher,
//                appFlyerId = appFlyerId,
//                sessionId = sessionId,
//                scope = coroutineScope,
//                headerProvider = headerProvider
//            )
//        }
//    }

    companion object Companion {
        private const val TAG = "EloAnalyticsSDK"
        internal const val TAG2 = "EloAnalyticsSDKTEST"
        @Volatile
        private var instance: EloAnalyticsSdk? = null

        fun getInstance(): EloAnalyticsSdk {
            return instance ?: throw IllegalStateException("EloAnalyticsSdk is not initialized")
        }
    }
}