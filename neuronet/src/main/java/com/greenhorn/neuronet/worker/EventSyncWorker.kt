package com.greenhorn.neuronet.worker

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.greenhorn.neuronet.client.ApiClient
import com.greenhorn.neuronet.db.AnalyticsDatabase
import com.greenhorn.neuronet.dispatcher.EventDispatcher
import com.greenhorn.neuronet.repository.EventRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * A CoroutineWorker that handles the background synchronization of analytics events.
 * It fetches events from the local database, sends them to the backend,
 * and handles success, failure, and retry logic.
 */
class EventSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    // These would typically be injected by a DI framework like Hilt or Koin,
    // but we initialize them manually here for simplicity.
    private val repository: EventRepository by lazy {
        EventRepository(AnalyticsDatabase.getInstance(applicationContext).analyticsEventDao() as AnalyticsDatabase)
    }

    private val eventApi: ApiClient by lazy {
        ApiClient("https://your-backend-api.com") // Replace with your actual base URL
    }
    private val eventDispatcher: EventDispatcher by lazy {
        EventDispatcher(repository, eventApi)
    }

//    private val repository: EventRepository by lazy {
//        val dao = AnalyticsDatabase.getInstance(applicationContext).analyticsEventDao()
//        EventRepository(eventRepository)
//    }

    private val apiClient: ApiClient by lazy {
        // Retrieve the API endpoint from the worker's input data
        val apiEndpoint = inputData.getString(KEY_API_ENDPOINT)
            ?: throw IllegalStateException("API endpoint not provided to EventSyncWorker")
        ApiClient(apiEndpoint)
    }

    companion object {
        const val WORK_NAME = "com.yourapp.analytics.EventSyncWorker"
        const val KEY_API_ENDPOINT = "API_ENDPOINT"
        const val BATCH_SIZE = 10
        const val KEY_HAS_NETWORK = "hasNetwork"


        fun enqueueWork(context: Context, hasNetwork: Boolean = true) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val uploadRequest = OneTimeWorkRequestBuilder<EventSyncWorker>()
                .setConstraints(constraints)
                .addTag(WORK_NAME)
                .setInputData(Data.Builder().putBoolean(KEY_HAS_NETWORK, hasNetwork).build())
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    10, // Initial delay
                    TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueue(uploadRequest)
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO){
        Log.d("EventSyncWorker", "Worker started. Attempt: $runAttemptCount")
        try {
            println("EventUploadWorker running...")
            val networkStatus = inputData.getBoolean(KEY_HAS_NETWORK, true) // Assume network is available if not specified
            if (!networkStatus && !isNetworkAvailable(applicationContext)) {
                println("No network connectivity. Retrying later.")
                return@withContext Result.retry()
            }

            return@withContext try {
                eventDispatcher.triggerEventUpload()
                Result.success()
            } catch (e: Exception) {
                println("Event upload failed: ${e.message}. Retrying.")
                Result.retry() // Implement exponential backoff for retries
            }
        } catch (e: Exception) {
            Log.e("EventSyncWorker", "Error sending events: ${e.message}. Scheduling for retry.")
            // For network errors or other exceptions, tell WorkManager to retry.
            // WorkManager will use an exponential backoff strategy by default.
            Result.retry()
        }
    }

}