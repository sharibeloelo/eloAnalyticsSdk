package com.greenhorn.neuronet

import android.app.Activity
import android.content.Context
import android.util.Log
import com.greenhorn.neuronet.db.AnalyticsDatabase
import com.greenhorn.neuronet.db.repository.EloAnalyticsLocalRepositoryImpl
import com.greenhorn.neuronet.db.usecase.EloAnalyticsLocalEventUseCase
import com.greenhorn.neuronet.db.usecase.EloAnalyticsLocalEventUseCaseImpl
import com.greenhorn.neuronet.extension.safeLaunch
import com.greenhorn.neuronet.listener.EloAnalyticsEventManager
import com.greenhorn.neuronet.model.EloAnalyticsEvent
import com.greenhorn.neuronet.model.EloAnalyticsEventDto
import com.greenhorn.neuronet.model.mapper.toStringMap
import com.greenhorn.neuronet.utils.AnalyticsSdkUtilProvider
import com.greenhorn.neuronet.utils.EloAnalyticsConfig
import com.greenhorn.neuronet.utils.EloAnalyticsConfigBuilder
import com.greenhorn.neuronet.utils.EloAnalyticsRuntimeProvider
import com.greenhorn.neuronet.utils.FlushPendingEventTriggerSource
import com.greenhorn.neuronet.worker.syncWorker.EloAnalyticsWorkerUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.ref.WeakReference

/**
 * Main Analytics SDK class responsible for tracking, batching, and synchronizing analytics events.
 *
 * This SDK provides functionality to:
 * - Track analytics events with custom data
 * - Batch events locally before sending to server
 * - Handle offline scenarios by storing events in local database
 * - Automatically flush events based on batch size or manual triggers
 *
 * @property context Application context (stored as weak reference to prevent memory leaks)
 * @property eloAnalyticUtils Utility provider for SDK operations
 * @property useCase Use case for local event database operations
 * @property config SDK configuration containing endpoint URLs, batch sizes, etc.
 * @property runtimeProvider Provider for runtime checks (user login status, SDK enabled state)
 */
class EloAnalyticsSdk private constructor(
    private val context: Context,
    private val eloAnalyticUtils: AnalyticsSdkUtilProvider,
    private val useCase: EloAnalyticsLocalEventUseCase,
    private val config: EloAnalyticsConfig,
    private val runtimeProvider: EloAnalyticsRuntimeProvider
) : EloAnalyticsEventManager {

    // Weak reference to prevent memory leaks
    private val contextRef by lazy { WeakReference(context) }

    // Coroutine scope for async operations with IO dispatcher and SupervisorJob for error isolation
    private val supervisorScope by lazy {
        CoroutineScope(IO + SupervisorJob())
    }

    // Thread-safe counter for tracking events before batch flush
    private val counterLock by lazy { Mutex() }

    @Volatile
    private var eventCounter = 0

    companion object {
        private const val TAG = "EloAnalyticsSDK"
        internal const val TAG2 = "EloAnalyticsSDKTEST"
        private const val DEFAULT_TRIGGER_SIZE = 10

        @Volatile
        private var instance: EloAnalyticsSdk? = null

        /**
         * Returns the singleton instance of EloAnalyticsSdk.
         *
         * @return The initialized SDK instance
         * @throws IllegalStateException if SDK is not initialized
         */
        fun getInstance(): EloAnalyticsSdk {
            return instance ?: throw IllegalStateException(
                "EloAnalyticsSdk is not initialized. Please call EloAnalyticsSdk.Builder(context).build() first."
            )
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
        Log.d(TAG, "trackEvent called: $name")

        // Early return if analytics is disabled
        if (!runtimeProvider.isAnalyticsSdkEnabled()) {
            Log.d(TAG, "Analytics SDK is disabled, skipping event: $name")
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
                Log.d(TAG, "Created event: ${event.logEvent()}")
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
            sessionTimeStamp = eloAnalyticUtils.getSessionTimeStamp(),
            eventData = eventData.toStringMap()
        )
    }

    /**
     * Handles errors that occur during event tracking.
     */
    private fun handleTrackingError(throwable: Throwable, eventName: String) {
        throwable.printStackTrace()
        Log.e(TAG2, "Error tracking event '$eventName': ${throwable.message}")
        eloAnalyticUtils.recordFirebaseNonFatal(error = throwable)
    }

    /**
     * Inserts an event into the local repository and increments the event counter.
     *
     * @param event The event to insert into the database
     */
    private suspend fun insertEventAndIncrementCounter(event: EloAnalyticsEvent) {
        Log.d(TAG2, "Inserting event '${event.eventName}' into database")

        val isInserted = useCase.insertEvent(event)
        Log.d(TAG2, "Event '${event.eventName}' insertion result: $isInserted")

        if (isInserted >= 0L) {
            incrementCounterAndCheckBatch()
        } else {
            Log.w(TAG2, "Failed to insert event '${event.eventName}' into database")
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
                val triggerCountSize = config.batchSize ?: DEFAULT_TRIGGER_SIZE

                Log.d(TAG2, "Event counter: $eventCounter, Batch size: $triggerCountSize")

                if (eventCounter >= triggerCountSize) {
                    Log.d(TAG2, "Batch size reached, triggering flush")
                    flushPendingEvents(
                        flushPendingEventTriggerSource = FlushPendingEventTriggerSource.EVENT_BATCH_COUNT_COMPLETE
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG2, "Error in incrementCounterAndCheckBatch: ${e.message}")
            eloAnalyticUtils.recordFirebaseNonFatal(error = e)
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
            Log.d(TAG, "Analytics SDK is disabled, skipping flushing events!")
            return
        }

        Log.d(TAG2, "Flushing pending events triggered by: ${flushPendingEventTriggerSource.name}")
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
                Log.e(TAG2, "Error in flushPendingEvents: ${throwable.message}")
                eloAnalyticUtils.recordFirebaseNonFatal(error = throwable)
            }
        )
    }

    /**
     * Determines if flush should proceed based on counter and database state.
     */
    private suspend fun shouldProceedWithFlush(): Boolean {
        if (eventCounter > 0) {
            Log.d(TAG2, "Counter is $eventCounter, proceeding with flush")
            return true
        }

        val pendingEventsCount = useCase.getEventsCount()
        val hasPendingEvents = pendingEventsCount > 0

        Log.d(TAG2, "Counter is 0, checking database. Pending events: $pendingEventsCount")

        if (!hasPendingEvents) {
            Log.d(TAG2, "No pending events found, skipping flush")
        }

        return hasPendingEvents
    }

    /**
     * Resets the event counter and enqueues background work for syncing events.
     */
    private suspend fun resetCounterAndEnqueueWork() {
        eventCounter = 0
        Log.d(TAG2, "Event counter reset to 0")

        contextRef.get()?.let { context ->
            Log.d(TAG2, "Enqueuing analytics work request")
            EloAnalyticsWorkerUtils.enqueueAnalyticEventsWorkRequest(context)
        } ?: Log.w(TAG2, "Context reference is null, cannot enqueue work request")
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
    class Builder(private val context: Context) {
        private var runtimeProvider: EloAnalyticsRuntimeProvider? = null
        private var configBuilder: EloAnalyticsConfigBuilder = EloAnalyticsConfigBuilder()

        /**
         * Configures the SDK using the provided configuration block.
         *
         * @param block Configuration block for setting up SDK parameters
         * @return Builder instance for method chaining
         */
        fun setConfig(block: EloAnalyticsConfigBuilder.() -> Unit) = apply {
            configBuilder.apply(block)
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

        /**
         * Builds and initializes the EloAnalyticsSdk instance.
         *
         * @return Configured SDK instance
         * @throws IllegalArgumentException if required parameters are missing
         */
        fun build(): EloAnalyticsSdk {
            Log.d(TAG, "Building EloAnalyticsSdk instance")

            val finalConfig = configBuilder.build()
            validateConfiguration(finalConfig)

            val dependencies = createDependencies(finalConfig)

            val instance = EloAnalyticsSdk(
                context = context,
                eloAnalyticUtils = dependencies.utils,
                useCase = dependencies.useCase,
                config = finalConfig,
                runtimeProvider = requireNotNull(runtimeProvider) {
                    "EloAnalyticsRuntimeProvider is required. Call setRuntimeProvider() before build()."
                }
            )

            // Set singleton instance
            Companion.instance = instance
            Log.d(TAG, "EloAnalyticsSdk instance created and initialized successfully")

            return instance
        }

        /**
         * Validates the configuration parameters.
         */
        private fun validateConfiguration(config: EloAnalyticsConfig) {
            if (config.endpointUrl.isBlank()) {
                Log.w(
                    TAG,
                    "Warning: endpointUrl not set. Events may not be synchronized to server."
                )
            }

            config.batchSize?.let { batchSize ->
                if (batchSize <= 0) {
                    throw IllegalArgumentException("Batch size must be greater than 0")
                }
            }
        }

        /**
         * Creates the required dependencies for the SDK.
         */
        private fun createDependencies(config: EloAnalyticsConfig): Dependencies {
            val dao = AnalyticsDatabase.getInstance(context).eloAnalyticsEventDao()
            val repo = EloAnalyticsLocalRepositoryImpl(dao)
            val useCase = EloAnalyticsLocalEventUseCaseImpl(repo)
            val utils = AnalyticsSdkUtilProvider

            runtimeProvider?.let { utils.initialize(provider = it) }

            return Dependencies(utils, useCase)
        }

        /**
         * Data class to hold SDK dependencies.
         */
        private data class Dependencies(
            val utils: AnalyticsSdkUtilProvider,
            val useCase: EloAnalyticsLocalEventUseCase
        )
    }
}