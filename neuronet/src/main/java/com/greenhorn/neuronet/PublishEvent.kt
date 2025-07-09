package com.greenhorn.neuronet

import android.content.Context
import com.greenhorn.neuronet.client.ApiClient
import com.greenhorn.neuronet.constant.Constant
import com.greenhorn.neuronet.db.AnalyticsDatabase
import com.greenhorn.neuronet.dispatcher.EventDispatcher
import com.greenhorn.neuronet.enum.PRIORITY
import com.greenhorn.neuronet.extension.safeLaunch
import com.greenhorn.neuronet.header.MutableHeaderProvider
import com.greenhorn.neuronet.interceptor.HttpInterceptor
import com.greenhorn.neuronet.interceptor.RetryInterceptor
import com.greenhorn.neuronet.listener.PublishEventListener
import com.greenhorn.neuronet.log.Logger
import com.greenhorn.neuronet.repository.EventRepository
import com.greenhorn.neuronet.service.ApiService
import com.greenhorn.neuronet.worker.EventSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

/**
 * The main class for the Analytics SDK.
 * Use the [Builder] to create an instance of this class.
 *
 * @property context The application context.
 * @property repository The repository for data operations.
 * @property workManager The WorkManager instance for scheduling background tasks.
 * @property apiEndpoint The backend URL to send events to.
 */
class PublishEvent(
    context: Context,
    private val finalApiEndpoint: String,
    private val eventDispatcher: EventDispatcher,
    private val appFlyerId: String? = null,
    private val sessionId: String? = null,
    private val scope: CoroutineScope,
    private val headerProvider: MutableHeaderProvider
) : PublishEventListener {
    private val contextRef by lazy { WeakReference(context) }
    private var eventCount = 0

    init {
        scope.launch {
            eventDispatcher.pendingEventCount
                .collectLatest { count ->
                    eventCount++
                    Logger.d("Event_Collect_Count : $eventCount")
                    if (eventCount >= eventDispatcher.getBatchSizeOfEvents()) { // Batch size
                        Logger.d("Event count reached 10 on Android. Enqueuing work.")
                        eventCount = 0
                        /**
                         * Enqueues the EventSyncWorker to run in the background.
                         * It uses a unique work policy to prevent multiple workers from running simultaneously.
                         * Constraints ensure the worker only runs when the network is available.
                         */
                        EventSyncWorker.enqueueWork(
                            contextRef.get(),
                            eventDispatcher = eventDispatcher,
                            finalApiEndpoint = finalApiEndpoint,
                            scope
                        ) {
                            eventCount == 0
                        }
                    }
                }
        }
    }

    /**
     * Tracks a new event.
     * The event is saved to the local database, and a sync job is scheduled if the batch size is reached.
     *
     * @param eventName A descriptive name for the event.
     * @param params A map of key-value pairs for additional event data.
     */
    override suspend fun track(payload: AnalyticsEvent, currentUserId: Long,
                               guestUserId: Long, priority: PRIORITY) {
        scope.safeLaunch({
            when(priority){
                PRIORITY.LOW -> eventDispatcher.addEvent(payload, currentUserId, guestUserId)
                else -> eventDispatcher.sendSingleEvent(event = payload)
            }
        }, {})
    }

    fun setHeaders(headers: Map<String, String>) {
        headerProvider.updateHeaders(headers)
    }

    /**
     * Builder class for constructing an [PublishEvent] instance.
     */
    class Builder(private val context: Context) {
        private var baseUrl: String? = null
        private var apiEndpoint: String? = null
        private var appFlyerId: String? = null
        private var sessionId: String? = null
        private var batchSize: Int = 10
        private var headers: Map<String, String> = emptyMap()
        private var customInterceptor: Interceptor? = null
        private var customOkHttpClient: OkHttpClient? = null
        private var coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        private var isDebug: Boolean = false

        fun baseUrl(baseUrl: String) = apply { this.baseUrl = baseUrl }
        fun apiEndpoint(endpoint: String) = apply { this.apiEndpoint = endpoint }
        fun sessionId(sessionId: String) = apply { this.sessionId = sessionId }
        fun appFlyerId(appFlyerId: String) = apply { this.appFlyerId = appFlyerId }
        fun batchSize(batchSize: Int) = apply { this.batchSize = batchSize }
        fun headers(headers: Map<String, String>) = apply { this.headers = headers }
        fun scope(scope: CoroutineScope) = apply { this.coroutineScope = scope }
        fun interceptor(interceptor: Interceptor) = apply { this.customInterceptor = interceptor }
        fun okHttpClient(okHttpClient: OkHttpClient) = apply { this.customOkHttpClient = okHttpClient }

        fun enableLogs(isDebug: Boolean) = apply {
            this.isDebug = isDebug
            Logger.initialize(enableLogs = isDebug)
        }

        fun build(): PublishEvent {
            // 1. Eagerly validate required parameters
            checkNotNull(baseUrl) { "Base URL must be set." }
            val finalApiEndpoint = requireNotNull(apiEndpoint) { "API endpoint must be set." }

            // 2. Build dependencies in a clean, chained manner
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = if (isDebug) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
            }

            val headerProvider = MutableHeaderProvider(headers)
            // Use a custom OkHttpClient if provided, otherwise build a default one.
            val finalOkHttpClient = customOkHttpClient ?: OkHttpClient.Builder()
                .addInterceptor(customInterceptor ?: HttpInterceptor(headerProvider))
                .addInterceptor(RetryInterceptor())
                .addInterceptor(loggingInterceptor)
                .retryOnConnectionFailure(true)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl!!)
                .client(finalOkHttpClient)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()

            val apiService = retrofit.create(ApiService::class.java)

            // 3. Construct final dependencies for the main class
            val database = AnalyticsDatabase.getInstance(context)
            val eventRepository = EventRepository(database)
            val apiClient = ApiClient(apiService)
            val eventDispatcher = EventDispatcher(eventRepository, apiClient, finalApiEndpoint, batchSize)

            return PublishEvent(
                context = context,
                finalApiEndpoint = finalApiEndpoint,
                eventDispatcher = eventDispatcher,
                appFlyerId = appFlyerId,
                sessionId = sessionId,
                scope = coroutineScope,
                headerProvider = headerProvider
            )
        }
    }

    companion object {
        @Volatile
        private var instance: PublishEvent? = null

        fun init(sdk: PublishEvent) {
            synchronized(this) {
                if (instance == null) {
                    instance = sdk
                } else {
                    throw IllegalStateException("SDK has already been initialized..!")
                }
            }
        }

        /**
         * Gets the singleton instance of the SDK.
         * @throws IllegalStateException if the SDK has not been initialized.
         */
        fun getInstance(): PublishEvent {
            return instance
                ?: throw IllegalStateException("AnalyticsSdk has not been initialized. Call init() first.")
        }
    }
}