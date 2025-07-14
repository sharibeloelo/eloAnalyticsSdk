package com.greenhorn.neuronet.worker

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.asFlow
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.greenhorn.neuronet.client.ApiClient
import com.greenhorn.neuronet.db.AnalyticsDatabase
import com.greenhorn.neuronet.dispatcher.EventDispatcher
import com.greenhorn.neuronet.log.Logger
import com.greenhorn.neuronet.repository.EventRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.net.ssl.HandshakeCompletedEvent

/**
 * A CoroutineWorker that handles the background synchronization of analytics events.
 * It fetches events from the local database, sends them to the backend,
 * and handles success, failure, and retry logic.
 */
//todo: this can be improved: Use the same logic as implemented in Eloelo, thats more performant efficient and reliable.
// Create a single workManager and create tasks to push batches but use liveData observer to to check status of current enqueued tasks,
// if any is enqueued or blocked dont add next one and use these enqueued or blocked to push all the data in a single api
// Also, no need of using retry mechanism, since the task enqueing is so fast we dont need single event push for high priority events
// Make sure to delet only after successful push. and avoid multiple fetching of db data since the data can be very large
class EventSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val SYNC_WORK_NAME = "event_sync_work"
        const val URL = "url"
        private var eventDispatcher: EventDispatcher?= null


        fun enqueueWork(context: Context?, eventDispatcher : EventDispatcher, finalApiEndpoint : String, scope: CoroutineScope, isCompletedEvent : () -> Unit) {
            context ?: throw IllegalStateException("Context should not be null..!")

            this.eventDispatcher = eventDispatcher
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val uploadRequest = OneTimeWorkRequestBuilder<EventSyncWorker>()
                .setConstraints(constraints)
                .setInputData(Data.Builder().putString(URL, finalApiEndpoint).build())
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    10, // Initial delay
                    TimeUnit.SECONDS
                )
                .build()

            // Use enqueueUniqueWork with a policy.
           WorkManager.getInstance(context).enqueueUniqueWork(
                SYNC_WORK_NAME,
                ExistingWorkPolicy.KEEP, // This is the crucial part
                uploadRequest
            )

            val workId = uploadRequest.id
            Logger.i("Work_Id: ${workId}")
            observeWork(workId, context, scope, isCompletedEvent)
        }

        private fun observeWork(workId: UUID, context: Context, scope : CoroutineScope, isCompletedEvent : () -> Unit) {
            scope.launch {
                WorkManager.getInstance(context)
                    .getWorkInfoByIdLiveData(workId)  // Use getWorkInfoByIdLiveData to get the LiveData object
                    .asFlow() // Convert LiveData to Flow
                    .collect { workInfo -> // Collect emissions from the Flow
                        Logger.i("Work status: ${workInfo?.state}")
                        if (workInfo != null) {
                            when (workInfo.state) {
                                WorkInfo.State.SUCCEEDED -> {
                                    Logger.d("Work succeeded!")
                                    isCompletedEvent.invoke()
                                    // Stop collecting if the work is finished
                                }
                                WorkInfo.State.FAILED -> {
                                    Logger.e("Work failed.")
                                    // Stop collecting if the work is finished
                                }
                                // ... handle other states
                                else -> Logger.i("Work status: ${workInfo.state}")
                            }
                        }
                    }

            }
        }
    }

    //todo: not required
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO){
        Logger.d("EventSyncWorker", "Worker started. Attempt: $runAttemptCount")
        try {
            Logger.d("EventUploadWorker running...")
            val url = inputData.getString(URL) // Assume network is available if not specified
            //todo: not required, will be handle by workManager and for logging, handle HttpException
            if (!isNetworkAvailable(applicationContext)) {
                Logger.d("No network connectivity. Retrying later.")
                return@withContext Result.retry()
            }

            return@withContext try {
                Logger.d("Url : $url")
                val eventIsTrigger = eventDispatcher?.triggerEventUpload(url.orEmpty())
                Logger.d("eventIsTrigger : $eventIsTrigger")
                if (eventIsTrigger == true)
                    Result.success()
                else Result.retry()
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