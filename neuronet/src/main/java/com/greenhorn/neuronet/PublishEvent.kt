package com.greenhorn.neuronet

import android.content.Context
import com.greenhorn.neuronet.client.ApiClient
import com.greenhorn.neuronet.constant.Constant
import com.greenhorn.neuronet.db.AnalyticsDatabase
import com.greenhorn.neuronet.dispatcher.EventDispatcher
import com.greenhorn.neuronet.enum.PRIORITY
import com.greenhorn.neuronet.extension.safeLaunch
import com.greenhorn.neuronet.interceptor.HttpInterceptor
import com.greenhorn.neuronet.interceptor.RetryInterceptor
import com.greenhorn.neuronet.model.Event
import com.greenhorn.neuronet.repository.EventRepository
import com.greenhorn.neuronet.service.ApiService
import com.greenhorn.neuronet.worker.EventSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
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
class PublishEvent private constructor(
    private val context: WeakReference<Context>,
    private val finalApiEndpoint: String,
    private val eventDispatcher: EventDispatcher,
    private val appFlyerId: String? = null,
    private val sessionId: String? = null,
    private val scope: CoroutineScope,
) {

    init {
        scope.launch {
            eventDispatcher.pendingEventCount
                .collect { count ->
                    if (count >= eventDispatcher.getBatchSizeOfEvents()) { // Batch size
                        println("Event count reached 10 on Android. Enqueuing work.")

                        /**
                         * Enqueues the EventSyncWorker to run in the background.
                         * It uses a unique work policy to prevent multiple workers from running simultaneously.
                         * Constraints ensure the worker only runs when the network is available.
                         */
                        EventSyncWorker.enqueueWork(
                            context.get(),
                            eventDispatcher = eventDispatcher,
                            finalApiEndpoint = finalApiEndpoint
                        )
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
    fun track(eventName: String, userId : Long, isUserLogin: Boolean, payload: MutableMap<String, Any>, priority: PRIORITY = PRIORITY.LOW) {
        val eventTs =
            payload[Constant.TIME_STAMP] ?: System.currentTimeMillis()
                .toString()
        payload.put(Constant.APPS_FLYER_ID, appFlyerId.orEmpty())

        scope.safeLaunch({
            val event = Event(
                id = userId,
                eventName = eventName,
                isUserLogin = isUserLogin,
                payload = payload,
                timestamp = eventTs.toString(),
                sessionTimeStamp = sessionId.orEmpty(),
                primaryId = "${userId}_${eventTs}",
                sessionId = "${userId}_${sessionId}"
            )

            when(priority){
                PRIORITY.LOW ->{
                    eventDispatcher.addEvent(event)
                }
                else -> {
                    eventDispatcher.sendSingleEvent(event = event)
                }
            }
        }, {
        })
    }


    /**
     * Builder class for constructing an [PublishEvent] instance.
     */
    class Builder(private val context: Context) {
        private var apiEndpoint: String? = null
        private var appFlyerId: String? = null
        private var sessionId: String? = null
        private var batchSize: Int = 10
        private var headers: Map<String, String> = emptyMap()
        private var interceptor: Interceptor = HttpInterceptor(headers)
        private var loggingInterceptor: HttpLoggingInterceptor? = null
        private var okHttpClient: OkHttpClient? = null

        private var coroutineScope: CoroutineScope =
            CoroutineScope(Dispatchers.IO + SupervisorJob())

        fun setApiEndpoint(endpoint: String) = apply { this.apiEndpoint = endpoint }
        fun setSessionId(sessionId: String) = apply { this.sessionId = sessionId }
        fun setAppFlyerId(appFlyerId: String) = apply { this.appFlyerId = appFlyerId }
        fun setBatchSize(batchSize: Int) = apply { this.batchSize = batchSize }
        fun setHeaders(headers: Map<String, String>) = apply { this.headers = headers }
        fun setScope(scope: CoroutineScope) = apply { this.coroutineScope = scope }
        fun setInterceptor(interceptor: Interceptor) = apply { this.interceptor = interceptor }
        fun setOkHttpClient(okHttpClient: OkHttpClient) = apply { this.okHttpClient = okHttpClient }
        fun enableLogs(isDebug: Boolean) = apply {
            loggingInterceptor = HttpLoggingInterceptor().apply {
                level =
                    if (isDebug) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
            }
        }

        fun build(): PublishEvent {
            val finalApiEndpoint = apiEndpoint
                ?: throw IllegalStateException("API endpoint must be set before building.")

            okHttpClient = OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .addInterceptor(RetryInterceptor())
                .addInterceptor(loggingInterceptor ?: HttpLoggingInterceptor().apply {
                    HttpLoggingInterceptor.Level.NONE
                }
                )
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .client(okHttpClient!!)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val apiService = retrofit.create(ApiService::class.java)

            // Construct dependencies here
            val database = AnalyticsDatabase.getInstance(context)
            val eventRepository = EventRepository(database)
            val apiClient = ApiClient(apiService)
            val eventDispatcher =
                EventDispatcher(eventRepository, apiClient, finalApiEndpoint, batchSize)

            return PublishEvent(
                WeakReference(context),
                finalApiEndpoint = finalApiEndpoint,
                eventDispatcher = eventDispatcher,
                appFlyerId = appFlyerId,
                sessionId = sessionId,
                scope = coroutineScope
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
                    println("SDK: Warning - SDK has already been initialized.")
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