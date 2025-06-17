package com.greenhorn.neuronet

import android.content.Context
import com.greenhorn.neuronet.client.ApiClient
import com.greenhorn.neuronet.constant.Constant
import com.greenhorn.neuronet.db.AnalyticsDatabase
import com.greenhorn.neuronet.dispatcher.EventDispatcher
import com.greenhorn.neuronet.extension.safeLaunch
import com.greenhorn.neuronet.model.Event
import com.greenhorn.neuronet.repository.EventRepository
import com.greenhorn.neuronet.worker.EventSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The main class for the Analytics SDK.
 * Use the [Builder] to create an instance of this class.
 *
 * @property context The application context.
 * @property repository The repository for data operations.
 * @property workManager The WorkManager instance for scheduling background tasks.
 * @property apiEndpoint The backend URL to send events to.
 */
class AnalyticsSdk private constructor(
    private val context: Context,
    private val eventDispatcher: EventDispatcher,
    private val appFlyerId: String ?= null,
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    // A dedicated CoroutineScope for SDK operations
    private val sdkScope by lazy { CoroutineScope(Dispatchers.IO) }

    init {
        scope.launch {
            eventDispatcher.pendingEventCount
                .collect { count ->
                    if (count >= 10) { // Batch size
                        println("Event count reached 10 on Android. Enqueuing work.")

                        /**
                         * Enqueues the EventSyncWorker to run in the background.
                         * It uses a unique work policy to prevent multiple workers from running simultaneously.
                         * Constraints ensure the worker only runs when the network is available.
                         */
                        EventSyncWorker.Companion.enqueueWork(context)
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
    fun track(eventName: String, isUserLogin : Boolean, payload: MutableMap<String, String>) {
        val eventTs =
            payload[Constant.TIME_STAMP] ?: System.currentTimeMillis()
                .toString()
        payload.put(Constant.APPS_FLYER_ID, appFlyerId.orEmpty())

        sdkScope.safeLaunch({
            val event = Event(
                eventName = eventName,
                isUserLogin = isUserLogin,
                payload = payload,
                timestamp = eventTs,
                sessionTimeStamp = ""
            )
            eventDispatcher.addEvent(event)
        }, {
//            eloAnalyticUtils.recordFirebaseNonFatal(error = it)
        })
    }


    /**
     * Builder class for constructing an [AnalyticsSdk] instance.
     */
    class Builder(private val context: Context) {
        private var apiEndpoint: String? = null
        private var appFlyerId: String? = null

        fun setApiEndpoint(endpoint: String): Builder {
            this.apiEndpoint = endpoint
            return this
        }

        fun setAppFlyerId(appFlyerId: String): Builder {
            this.appFlyerId = appFlyerId
            return this
        }

        fun build(): AnalyticsSdk {
            apiEndpoint ?: throw IllegalStateException("API endpoint must be set before building the SDK.")
            val database = AnalyticsDatabase.getInstance(context)
            val eventRepository = EventRepository(database)
            val eventApi = ApiClient(apiEndpoint.orEmpty())
            val eventDispatcher = EventDispatcher(eventRepository, eventApi)
            return AnalyticsSdk(context, eventDispatcher, appFlyerId)
        }
    }
}