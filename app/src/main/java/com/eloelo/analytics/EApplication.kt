package com.eloelo.analytics

// In the app's Application class (e.g., com.yourapp.YourApplication.kt)
// In your app's Application class (e.g., com.yourapp.EApplication.kt)

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.work.Configuration
import androidx.work.DelegatingWorkerFactory
import com.greenhorn.neuronet.EloAnalyticsSdk
import com.greenhorn.neuronet.listener.FlushPendingEventTriggerSource
import com.greenhorn.neuronet.utils.EloAnalyticsRuntimeProvider
import com.greenhorn.neuronet.worker.syncWorker.AnalyticsSdkApiData
import com.greenhorn.neuronet.worker.syncWorker.AnalyticsWorkManagerInitializer
import kotlinx.coroutines.launch

private const val TAG = "EloEloApplication"
class EApplication : Application(), Configuration.Provider,
    ActivityLifecycleCallbacks {
    private lateinit var delegatingWorkerFactory: DelegatingWorkerFactory

    private var activityTag: String? = null
    private var taskId: Int? = null

    override fun onCreate() {
        super.onCreate()
        delegatingWorkerFactory = DelegatingWorkerFactory()

        // Add analytics library's worker factory if needed
        delegatingWorkerFactory.addFactory(
            AnalyticsWorkManagerInitializer.createAnalyticsWorkerFactory(
                AnalyticsSdkApiData(
                    baseUrl = "https://central-analytics-producer.eloelo.in/",
                    apiEndPoint = "v2/analytics/send/event/test",
                    isDebug = true,
                    headers = emptyMap(),
                    customOkHttpClient = null,
                    customInterceptor = null
                )
            )
        )
        //central-analytics-producer.eloelo.in/

        val analytics = EloAnalyticsSdk.Builder(this)
            .setRuntimeProvider(object : EloAnalyticsRuntimeProvider {

                override suspend fun isUserLoggedIn(): Boolean {
                    return true
                }

                override fun getHeaders(): Map<String, String> {
                    return emptyMap()
                }

                override fun getAppVersionCode(): String {
                    return "13133123"
                }

                override suspend fun getCurrentUserId(key: String): Long {
                    return 10000
                }

                override suspend fun getGuestUserId(key: String): Long {
                    return 111011L
                }

                override fun isAnalyticsSdkEnabled(): Boolean {
                    return true
                }

            })
            .setConfig {
                batchSize = 2
                endpointUrl = "https://your.server.com/track-events"
                appsFlyerId = "appFlyerId"
            }
            .build()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .setWorkerFactory(delegatingWorkerFactory)
            .build()


    private fun flushPendingEloAnalyticsEvents(
        flushPendingEventTriggerSource: FlushPendingEventTriggerSource,
        activity: Activity
    ) {
        ioScope.launch {
            val isEnableEloAnalytics = eloAnalyticUtils.isEnableEloAnalytics()

            if (isEnableEloAnalytics) {
                eloAnalyticsEventManager.flushPendingEvents(
                    flushPendingEventTriggerSource,
                    activity = activity
                )
            }
        }
    }

    override fun onActivityCreated(p0: Activity, p1: Bundle?) {
        TODO("Not yet implemented")
    }

    override fun onActivityDestroyed(activity: Activity) {
        flushPendingEloAnalyticsEvents(
            flushPendingEventTriggerSource = FlushPendingEventTriggerSource.ACTIVITY_DESTROY,
            activity = activity
        )// flush events for activity destroyed case
    }

    override fun onActivityPaused(activity: Activity) {
        Log.d(TAG, "onActivityPaused: ${activity::class.simpleName}")
    }

    override fun onActivityResumed(activity: Activity) {
        Log.d(TAG, "onActivityResumed: ${activity::class.simpleName}")

        taskId = activity.taskId
        activityTag = activity.localClassName
    }

    override fun onActivitySaveInstanceState(activity: Activity, p1: Bundle) {}

    override fun onActivityStarted(activity: Activity) {
        Log.d(TAG, "onActivityStarted: ${activity::class.simpleName}")
    }

    override fun onActivityStopped(activity: Activity) {
        Log.d(TAG, "onActivityStopped: $activity")
        if (activity.localClassName == activityTag && activity.taskId == taskId) {
            EloAnalyticsSdk.getInstance().trackEvent(
                type = EventType.MOENGAGE,
                eventName = AnalyticsKeyConstants.APP_MINIMISE_OR_EXITED,
                property = emptyMap()
            )
            flushPendingEloAnalyticsEvents(
                flushPendingEventTriggerSource = FlushPendingEventTriggerSource.APP_MINIMIZE,
                activity = activity
            ) // flush events for app minimise case

            socketDisconnection()
        }
    }
}