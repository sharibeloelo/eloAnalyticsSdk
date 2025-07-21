package com.greenhorn.neuronet.worker.syncWorker

import android.content.Context
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.greenhorn.neuronet.utils.EloSdkLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object EloAnalyticsWorkerUtils {
    private const val ELO_ANALYTICS_WORKER = "ELO_ANALYTICS_WORKER"

    private lateinit var workManager: WorkManager

    private data class WorkInfoObserver(
        val liveData: LiveData<List<WorkInfo>>,
        val observer: Observer<List<WorkInfo>>
    )

    private var workInfoObserver: WorkInfoObserver? = null

    private val constraints by lazy {
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }

    private fun initWorker(context: Context) {
        workManager = WorkManager.getInstance(context)
    }

    suspend fun enqueueAnalyticEventsWorkRequest(context: Context) {
        if (::workManager.isInitialized.not()) {
            initWorker(context)
        }

        withContext(Dispatchers.Main) {
            if (workInfoObserver == null) {
                val liveData = workManager.getWorkInfosForUniqueWorkLiveData(ELO_ANALYTICS_WORKER)
                val observer = Observer<List<WorkInfo>> { workInfos ->
                    try {
                        handleWorkInfos(workInfos)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        EloSdkLogger.e(message = "Error handling work infos: ${e.message}", throwable =  e)
                    }
                }
                workInfoObserver = WorkInfoObserver(liveData, observer)
            }

            addWorkInfoObserver()
        }
    }

    private fun handleWorkInfos(workInfos: List<WorkInfo>) {
        val isWorkRunning = workInfos.any { workInfo ->
            workInfo.state == WorkInfo.State.ENQUEUED || workInfo.state == WorkInfo.State.BLOCKED
        }

        if (!isWorkRunning) {
            val workRequest = OneTimeWorkRequestBuilder<EloAnalyticsSyncWorker>()
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniqueWork(
                ELO_ANALYTICS_WORKER,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                workRequest
            )

            EloSdkLogger.d(
                "Enqueued work request for $ELO_ANALYTICS_WORKER with ID: ${workRequest.id}"
            )
        } else {
            EloSdkLogger.d(
                "Work request for $ELO_ANALYTICS_WORKER is already blocked or enqueued."
            )
        }

        // Clean up observer to prevent memory leaks
        cleanUpObserver()
    }

    @MainThread
    private fun addWorkInfoObserver() {
        workInfoObserver?.liveData?.observeForever(
            workInfoObserver?.observer ?: return
        )
    }

    @MainThread
    private fun cleanUpObserver() {
        workInfoObserver?.let { observer ->
            observer.liveData.removeObserver(observer.observer)
        }
        workInfoObserver = null
    }
}
