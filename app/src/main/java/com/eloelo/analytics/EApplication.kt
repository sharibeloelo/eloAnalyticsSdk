package com.eloelo.analytics

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.work.Configuration
import androidx.work.DelegatingWorkerFactory
import com.greenhorn.neuronet.EloAnalyticsSdk
import com.greenhorn.neuronet.utils.EloAnalyticsConfig
import com.greenhorn.neuronet.worker.syncWorker.EloAnalyticsWorkerFactory

private const val TAG = "EloEloApplication"

class EApplication : Application(), Configuration.Provider{
    private lateinit var delegatingWorkerFactory: DelegatingWorkerFactory

    override fun onCreate() {
        super.onCreate()
        delegatingWorkerFactory = DelegatingWorkerFactory()

        EloAnalyticsSdk.Builder(this)
            .setConfig(
                config = EloAnalyticsConfig(
                    batchSize = 2,
                    apiUrl = "https://central-analytics-producer.eloelo.in/v2/analytics/send/event/test",
                    isDebug = true,
                    headers = mapOf("version_code" to "1212",
                        "father" to "23"),
                )
            )
            .build()

        // Add analytics library's worker factory using dependency container from SDK
        delegatingWorkerFactory.addFactory(
            EloAnalyticsWorkerFactory()
        )

        // Register lifecycle callbacks for analytics
        registerActivityLifecycleCallbacks(AnalyticsLifecycleTracker())

    }

    /**
     * Activity lifecycle tracker for analytics events.
     * 
     * This class handles the lifecycle events and triggers appropriate
     * analytics actions like flushing events when activities are destroyed
     * or when the app goes to background.
     */
    private inner class AnalyticsLifecycleTracker : Application.ActivityLifecycleCallbacks {
        private var activityTag: String? = null
        private var taskId: Int? = null

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            // Activity created - can track if needed
        }

        override fun onActivityStarted(activity: Activity) {
            // Activity started - can track if needed
        }

        override fun onActivityResumed(activity: Activity) {
            taskId = activity.taskId
            activityTag = activity.localClassName
            
            // Track screen view
            EloAnalyticsSdk.getInstance().trackEvent(
                name = "screen_view",
                userId = 123,
                attributes = mapOf(
                    "screen_name" to activity.localClassName,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }

        override fun onActivityPaused(activity: Activity) {
            // Activity paused - can track if needed
        }

        override fun onActivityStopped(activity: Activity) {
            // Check if app is going to background
            if (activity.localClassName == activityTag && activity.taskId == taskId) {
                // Track app background event
                EloAnalyticsSdk.getInstance().trackEvent(
                    name = "app_background",
                    userId = 133,
                    attributes = mapOf(
                        "timestamp" to System.currentTimeMillis(),
                        "last_screen" to activity.localClassName
                    )
                )
                
                // Flush events when app goes to background
                EloAnalyticsSdk.getInstance().flushEventsOnBackgroundOrKilled()
            }
        }

        override fun onActivityDestroyed(activity: Activity) {
            // Flush events when activity is destroyed
            EloAnalyticsSdk.getInstance().flushEventsOnDestroy()
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            // Save any necessary state
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .setWorkerFactory(delegatingWorkerFactory)
            .build()
}