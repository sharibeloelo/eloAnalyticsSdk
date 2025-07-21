package com.greenhorn.neuronet

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import com.greenhorn.neuronet.client.ApiClient
import com.greenhorn.neuronet.client.ApiService
import com.greenhorn.neuronet.client.repository.EloAnalyticsRepositoryImpl
import com.greenhorn.neuronet.client.useCase.EloAnalyticsEventUseCase
import com.greenhorn.neuronet.client.useCase.EloAnalyticsEventUseCaseImpl
import com.greenhorn.neuronet.constant.Constant
import com.greenhorn.neuronet.db.AnalyticsDatabase
import com.greenhorn.neuronet.db.repository.EloAnalyticsLocalRepositoryImpl
import com.greenhorn.neuronet.db.usecase.EloAnalyticsLocalEventUseCase
import com.greenhorn.neuronet.db.usecase.EloAnalyticsLocalEventUseCaseImpl
import com.greenhorn.neuronet.extension.safeLaunch
import com.greenhorn.neuronet.header.MutableHeaderProvider
import com.greenhorn.neuronet.interceptor.HttpInterceptor
import com.greenhorn.neuronet.interceptor.RetryInterceptor
import com.greenhorn.neuronet.listener.EloAnalyticsEventManager
import com.greenhorn.neuronet.model.EloAnalyticsEvent
import com.greenhorn.neuronet.model.EloAnalyticsEventDto
import com.greenhorn.neuronet.model.mapper.toStringMap
import com.greenhorn.neuronet.utils.AnalyticsSdkUtilProvider
import com.greenhorn.neuronet.utils.ConnectivityImpl
import com.greenhorn.neuronet.utils.EloAnalyticsConfig
import com.greenhorn.neuronet.utils.EloAnalyticsRuntimeProvider
import com.greenhorn.neuronet.utils.EloSdkLogger
import com.greenhorn.neuronet.utils.FlushPendingEventTriggerSource
import com.greenhorn.neuronet.worker.syncWorker.EloAnalyticsWorkerUtils
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

/**
 * Main Analytics SDK class responsible for tracking, batching, and synchronizing analytics events.
 *
 * This SDK provides functionality to:
 * - Track analytics events with custom data
 * - Batch events locally before sending to server
 * - Handle offline scenarios by storing events in local database
 * - Automatically flush events based on batch size or manual triggers
 *
 * @property contextRef Weak reference to the application context to prevent memory leaks.
 * Used for enqueuing WorkManager jobs safely across the SDK.
 * @property eloAnalyticUtils Utility provider for SDK operations
 * @property useCase Use case for local event database operations
 * @property config SDK configuration containing endpoint URLs, batch sizes, etc.
 * @property runtimeProvider Provider for runtime checks (user login status, SDK enabled state)
 */
@Suppress("KDocUnresolvedReference")
class EloAnalyticsSdk private constructor(
    private val contextRef: WeakReference<Context>,
    private val config: EloAnalyticsConfig,
    private val runtimeProvider: EloAnalyticsRuntimeProvider,
) : EloAnalyticsEventManager {


    // Coroutine scope for async operations with IO dispatcher and SupervisorJob for error isolation
    private val supervisorScope by lazy {
        CoroutineScope(IO + SupervisorJob())
    }

    private lateinit var dependencyContainer: EloAnalyticsDependencyContainer
    fun getDependencyContainer(): EloAnalyticsDependencyContainer = dependencyContainer
    fun setDependencyContainer(dependencies: EloAnalyticsDependencyContainer) {
        dependencyContainer = dependencies
    }


    // Thread-safe counter for tracking events before batch flush
    private val counterLock by lazy { Mutex() }

    @Volatile
    private var eventCounter = 0

    companion object {
        @Volatile
        private var instance: EloAnalyticsSdk? = null

        /**
         * Returns the singleton instance of EloAnalyticsSdk.
         *
         * @return The initialized SDK instance
         * @throws IllegalStateException if SDK is not initialized
         */
        fun getInstance(): EloAnalyticsSdk {
            return instance ?: synchronized(this) {
                instance ?: throw IllegalStateException(
                    "EloAnalyticsSdk is not initialized. Call Builder.build() first."
                )
            }
        }

        /**
         * Checks if the SDK instance is initialized.
         *
         * @return true if initialized, false otherwise
         */
        fun isInitialized(): Boolean = instance != null
    }

    /**
     * Tracks an analytics event with the provided name and data.
     *
     * This method:
     * 1. Validates if analytics is enabled
     * 2. Adds required metadata (timestamp, AppsFlyer ID)
     * 3. Stores event in local database
     * 4. Increments counter and checks for batch flush trigger
     *
     * @param name The event name/identifier
     * @param eventData Mutable map containing event data and properties
     */
    fun trackEvent(name: String, eventData: MutableMap<String, Any>) {
        EloSdkLogger.d("[trackEvent] name=$name")

        // Early return if analytics is disabled
        if (!runtimeProvider.isAnalyticsSdkEnabled()) {
            EloSdkLogger.d("Analytics SDK is disabled, skipping event: $name")
            return
        }

        // Extract or generate timestamp
        val eventTimestamp = eventData[EloAnalyticsEventDto.TIME_STAMP] as? String
            ?: System.currentTimeMillis().toString()

        // Add AppsFlyer ID from config
        eventData[EloAnalyticsEventDto.APPS_FLYER_ID] = config.appsFlyerId.orEmpty()

        supervisorScope.safeLaunch(
            {
                val event = createAnalyticsEvent(name, eventTimestamp, eventData)
                EloSdkLogger.d("Created event: ${event.logEvent()}")
                insertEventAndIncrementCounter(event)
            },
            { throwable ->
                handleTrackingError(throwable, name)
            }
        )
    }

    /**
     * Creates an EloAnalyticsEvent from the provided parameters.
     */
    private suspend fun createAnalyticsEvent(
        name: String,
        timestamp: String,
        eventData: MutableMap<String, Any>
    ): EloAnalyticsEvent {
        return EloAnalyticsEvent(
            eventName = name,
            isUserLogin = runtimeProvider.isUserLoggedIn(),
            eventTimestamp = timestamp,
            sessionTimeStamp = dependencyContainer.analyticsSdkUtilProvider.getSessionTimeStamp(),
            eventData = eventData.toStringMap()
        )
    }

    /**
     * Handles errors that occur during event tracking.
     */
    private fun handleTrackingError(throwable: Throwable, eventName: String) {
        throwable.printStackTrace()
        EloSdkLogger.e("Error tracking event '$eventName': ${throwable.message}", throwable)
        dependencyContainer.analyticsSdkUtilProvider.recordFirebaseNonFatal(error = throwable)
    }

    /**
     * Inserts an event into the local repository and increments the event counter.
     *
     * @param event The event to insert into the database
     */
    private suspend fun insertEventAndIncrementCounter(event: EloAnalyticsEvent) {
        EloSdkLogger.d("Inserting event '${event.eventName}' into database")

        val isInserted = dependencyContainer.localEventUseCase.insertEvent(event)
        EloSdkLogger.d("Event '${event.eventName}' insertion result: $isInserted")

        if (isInserted >= 0L) {
            incrementCounterAndCheckBatch()
        } else {
            EloSdkLogger.w("Failed to insert event '${event.eventName}' into database")
        }
    }

    /**
     * Increments the event counter and checks if batch size limit is reached.
     * If the batch limit is reached, triggers a flush of pending events.
     */
    private suspend fun incrementCounterAndCheckBatch() {
        try {
            counterLock.withLock {
                eventCounter++
                val triggerCountSize = config.batchSize

                EloSdkLogger.d("Event counter: $eventCounter, Batch size: $triggerCountSize")

                if (eventCounter >= triggerCountSize) {
                    EloSdkLogger.d("Batch size reached, triggering flush")
                    flushPendingEvents(
                        flushPendingEventTriggerSource = FlushPendingEventTriggerSource.EVENT_BATCH_COUNT_COMPLETE
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            EloSdkLogger.e("Error in incrementCounterAndCheckBatch: ${e.message}")
            dependencyContainer.analyticsSdkUtilProvider.recordFirebaseNonFatal(error = e)
        }
    }

    /**
     * Flushes pending events from local database to server.
     *
     * This method:
     * 1. Resets the event counter
     * 2. Checks if there are pending events in database
     * 3. Enqueues a background work request to sync events
     *
     * @param flushPendingEventTriggerSource Source that triggered the flush operation
     * @param activity Optional activity reference for context
     */
    override fun flushPendingEvents(
        flushPendingEventTriggerSource: FlushPendingEventTriggerSource,
        activity: Activity?
    ) {
        // Early return if analytics is disabled
        if (!runtimeProvider.isAnalyticsSdkEnabled()) {
            EloSdkLogger.d("Analytics SDK is disabled, skipping flushing events!")
            return
        }

        EloSdkLogger.d("Flushing pending events triggered by: ${flushPendingEventTriggerSource.name}")
        supervisorScope.safeLaunch(
            {
                counterLock.withLock {
                    val shouldFlush = shouldProceedWithFlush()
                    if (!shouldFlush) return@safeLaunch

                    resetCounterAndEnqueueWork()
                }
            },
            { throwable ->
                throwable.printStackTrace()
                EloSdkLogger.e("Error in flushPendingEvents: ${throwable.message}")
                dependencyContainer.analyticsSdkUtilProvider.recordFirebaseNonFatal(error = throwable)
            }
        )
    }

    /**
     * Determines if flush should proceed based on counter and database state.
     */
    private suspend fun shouldProceedWithFlush(): Boolean {
        if (eventCounter > 0) {
            EloSdkLogger.d("Counter is $eventCounter, proceeding with flush")
            return true
        }

        val pendingEventsCount = dependencyContainer.localEventUseCase.getEventsCount()
        val hasPendingEvents = pendingEventsCount > 0

        EloSdkLogger.d("Counter is 0, checking database. Pending events: $pendingEventsCount")

        if (!hasPendingEvents) {
            EloSdkLogger.d("No pending events found, skipping flush")
        }

        return hasPendingEvents
    }

    /**
     * Resets the event counter and enqueues background work for syncing events.
     */
    private suspend fun resetCounterAndEnqueueWork() {
        eventCounter = 0
        EloSdkLogger.d("Event counter reset to 0")

        contextRef.get()?.let { context ->
            EloSdkLogger.d("Enqueuing analytics work request")
            EloAnalyticsWorkerUtils.enqueueAnalyticEventsWorkRequest(context)
        } ?: EloSdkLogger.w("Context reference is null, cannot enqueue work request")
    }

    /**
     * Builder class for constructing an [EloAnalyticsSdk] instance.
     *
     * Example usage:
     * ```
     * val sdk = EloAnalyticsSdk.Builder(context)
     *     .setConfig {
     *         endpointUrl = "https://api.example.com"
     *         batchSize = 20
     *         appsFlyerId = "your-appsflyer-id"
     *     }
     *     .setRuntimeProvider(myRuntimeProvider)
     *     .build()
     * ```
     */
    class Builder(private val application: Application) {
        private var runtimeProvider: EloAnalyticsRuntimeProvider? = null
        private var config: EloAnalyticsConfig? = null

        private val appContext = application.applicationContext

        /**
         * Configures the SDK using the provided configuration block.
         *
         * @param block Configuration block for setting up SDK parameters
         * @return Builder instance for method chaining
         */
        fun setConfig(config: EloAnalyticsConfig) = apply {
            this.config = config
        }


        /**
         * Sets the runtime provider for checking SDK state and user login status.
         *
         * @param provider Runtime provider implementation
         * @return Builder instance for method chaining
         */
        fun setRuntimeProvider(provider: EloAnalyticsRuntimeProvider) = apply {
            this.runtimeProvider = provider
        }

        fun initActivityLifecycleCallback(application: Application) {
            application.registerActivityLifecycleCallbacks(ActivityTracker())
        }

        private inner class ActivityTracker : Application.ActivityLifecycleCallbacks {
            private var activityTag: String? = null
            private var taskId: Int? = null

            private fun flushPendingEloAnalyticsEvents(
                flushPendingEventTriggerSource: FlushPendingEventTriggerSource,
                activity: Activity
            ) {
                getInstance().flushPendingEvents(
                    flushPendingEventTriggerSource,
                    activity = activity
                )
            }

            override fun onActivityDestroyed(activity: Activity) {
                EloSdkLogger.d("Activity destroyed, flushing all pending events!")
                flushPendingEloAnalyticsEvents(
                    flushPendingEventTriggerSource = FlushPendingEventTriggerSource.ACTIVITY_DESTROY,
                    activity = activity
                )// flush events for activity destroyed case
            }

            override fun onActivityResumed(activity: Activity) {
                taskId = activity.taskId
                activityTag = activity.localClassName
            }

            override fun onActivityStopped(activity: Activity) {
                if (activity.localClassName == activityTag && activity.taskId == taskId) {
                    EloSdkLogger.d("App went to background or killed, flushing events!")
                    getInstance().trackEvent(
                        name = Constant.APP_MINIMISE_OR_EXITED,
                        eventData = mutableMapOf()
                    )
                    flushPendingEloAnalyticsEvents(
                        flushPendingEventTriggerSource = FlushPendingEventTriggerSource.APP_MINIMIZE,
                        activity = activity
                    ) // flush events for app minimise case
                }
            }
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityCreated(p0: Activity, p1: Bundle?) {}
            override fun onActivitySaveInstanceState(activity: Activity, p1: Bundle) {}
        }

        /**
         * Builds and initializes the EloAnalyticsSdk instance.
         *
         * @return Configured SDK instance
         * @throws IllegalArgumentException if required parameters are missing
         */
        fun build() {
            if (isInitialized()) {
                EloSdkLogger.w("EloAnalyticsSdk is already initialized. Reinitializing...")
            }

            initActivityLifecycleCallback(application)

            EloSdkLogger.d("Building EloAnalyticsSdk instance!")

            val appContext = application.applicationContext
            val finalConfig =
                requireNotNull(config) { "EloAnalyticsConfig is required. Call setConfig() before build()." }
            validateConfiguration(finalConfig)

            val dependencies = createDependencies(finalConfig)
            val dependencyContainer = EloAnalyticsDependencyContainer(
                localEventUseCase = dependencies.localEventUseCase,
                remoteEventUseCase = dependencies.remoteEventUseCase,
                analyticsSdkUtilProvider = dependencies.analyticsSdkUtilProvider,
                mutableHeaderProvider = dependencies.mutableHeaderProvider
            )

            val instance = EloAnalyticsSdk(
                contextRef = WeakReference(appContext),
                config = finalConfig,
                runtimeProvider = requireNotNull(runtimeProvider) {
                    "EloAnalyticsRuntimeProvider is required. Call setRuntimeProvider() before build()."
                }
            )

            instance.setDependencyContainer(dependencies = dependencyContainer)

            // Set singleton instance
            Companion.instance = instance
            EloSdkLogger.d("EloAnalyticsSdk instance created and initialized successfully")
        }

        /**
         * Validates the configuration parameters.
         */
        private fun validateConfiguration(config: EloAnalyticsConfig) {
            if (config.baseUrl.isBlank()) {
                EloSdkLogger.w(
                    "Base URL must be set"
                )
            }

            if (config.endpointUrl.isBlank()) {
                EloSdkLogger.w(
                    "Warning: endpointUrl not set. Events may not be synchronized to server."
                )
            }

            if (config.batchSize <= 0) {
                throw IllegalArgumentException("Batch size must be greater than 0")
            }
        }

        /**
         * Creates the required dependencies for the SDK.
         */
        private fun createDependencies(config: EloAnalyticsConfig): EloAnalyticsDependencyContainer {
            val dao =
                AnalyticsDatabase.getInstance(appContext).eloAnalyticsEventDao()
            val repo = EloAnalyticsLocalRepositoryImpl(dao)
            val useCase = EloAnalyticsLocalEventUseCaseImpl(repo)
            val utils = AnalyticsSdkUtilProvider

            runtimeProvider?.let { utils.initialize(provider = it) }

            val json = Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
                encodeDefaults = true
                coerceInputValues = true
            }


            val baseUrl = checkNotNull(config.baseUrl) { "Base URL must be set." }
            val finalApiEndpoint =
                requireNotNull(config.endpointUrl) { "API endpoint must be set." }
            AnalyticsSdkUtilProvider.setApiEndPoint(endPoint = baseUrl + finalApiEndpoint)

            EloSdkLogger.init(debug = config.isDebug)
            // 2. Build dependencies in a clean, chained manner
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level =
                    if (config.isDebug) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
            }

            val contentType by lazy { "application/json".toMediaType() }

            val headerProvider = MutableHeaderProvider(config.headers)
            // Use a custom OkHttpClient if provided, otherwise build a default one.
            val finalOkHttpClient = config.customOkHttpClient ?: OkHttpClient.Builder()
                .addInterceptor(
                    config.customInterceptor ?: HttpInterceptor(
                        headerProvider
                    )
                )
                .addInterceptor(RetryInterceptor())
                .addInterceptor(loggingInterceptor)
                .retryOnConnectionFailure(true)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(finalOkHttpClient)
                .addConverterFactory(json.asConverterFactory(contentType))
                .build()

            val apiService = retrofit.create(ApiService::class.java)

            val apiClient = ApiClient(apiClient = apiService)
            val connectivity = ConnectivityImpl(appContext)
            val remoteEventRepository = EloAnalyticsRepositoryImpl(apiClient, connectivity)
            val remoteEventUseCase = EloAnalyticsEventUseCaseImpl(remoteEventRepository)
            return EloAnalyticsDependencyContainer(
                analyticsSdkUtilProvider = utils,
                localEventUseCase = useCase,
                remoteEventUseCase = remoteEventUseCase,
                mutableHeaderProvider = headerProvider
            )
        }
    }

    fun updateHeader(header: Map<String, String>) {
        EloSdkLogger.d("Updating Api headers!")
        dependencyContainer.mutableHeaderProvider.updateHeaders(newHeaders = header)
    }

    /**
     * Data class to hold SDK dependencies.
     */
    data class EloAnalyticsDependencyContainer(
        val analyticsSdkUtilProvider: AnalyticsSdkUtilProvider,
        val localEventUseCase: EloAnalyticsLocalEventUseCase,
        val remoteEventUseCase: EloAnalyticsEventUseCase,
        val mutableHeaderProvider: MutableHeaderProvider
    )
}