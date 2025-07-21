package com.greenhorn.neuronet.worker.syncWorker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.greenhorn.neuronet.EloAnalyticsDependencyContainer
import com.greenhorn.neuronet.EloAnalyticsSdk
import com.greenhorn.neuronet.model.EloAnalyticsEvent
import com.greenhorn.neuronet.model.mapper.EventMapper
import com.greenhorn.neuronet.utils.AnalyticsSdkUtilProvider
import com.greenhorn.neuronet.utils.EloSdkLogger
import com.greenhorn.neuronet.utils.onFailure
import com.greenhorn.neuronet.utils.onSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class EloAnalyticsSyncWorker(
    context: Context,
    workerParameters: WorkerParameters,
    private val dependencyContainer: EloAnalyticsDependencyContainer
) : CoroutineWorker(context, params = workerParameters) {

    private val analyticsSdkUtilProvider get() = dependencyContainer.analyticsSdkUtilProvider
    private val eloAnalyticsEventUseCase get() = dependencyContainer.remoteEventUseCase
    private val eloAnalyticsLocalEventUseCase get() = dependencyContainer.localEventUseCase

    companion object {
        private const val TAG = "EloAnalyticsSyncWorker"
    }

    override suspend fun doWork(): Result {
        EloSdkLogger.d(
            "ELO ANALYTICS worker started!"
        )

        return withContext(Dispatchers.IO) {
            try {
                val storedPendingEvents = fetchStoredEvents().map { event ->
                    event.copy(
                        eventData = event.eventData.toMutableMap().apply {
                            val id =
                                if (event.isUserLogin) analyticsSdkUtilProvider.getCurrentUserId() else analyticsSdkUtilProvider.getGuestUserId()
                            AnalyticsSdkUtilProvider.getUserIdAttributeKeyName()?.let {
                                this[it] = id.toString()
                            }
                        }
                    )
                }

                if (storedPendingEvents.isEmpty()) {
                    EloSdkLogger.d(
                        "pending events are zero, so finishing worker!"
                    )
                    return@withContext Result.success()
                }

                sendEventsToServer(storedPendingEvents)

                return@withContext Result.success()
            } catch (e: Exception) {
                e.printStackTrace()
                EloSdkLogger.e("exception in $TAG exception: ${e.message}", e)
                analyticsSdkUtilProvider.recordFirebaseNonFatal(error = e)

                Result.failure()
            }
        }
    }

    private suspend fun fetchStoredEvents(): List<EloAnalyticsEvent> {
        return eloAnalyticsLocalEventUseCase.getEvents().also { storedEvents ->
            EloSdkLogger.d("Fetched pending events count: ${storedEvents.size}")
        }
    }

    private suspend fun sendEventsToServer(storedEvents: List<EloAnalyticsEvent>) {
        eloAnalyticsEventUseCase.sendAnalyticEvents(
            EventMapper.toEventDtos(
                events = storedEvents,
                currentUserId = analyticsSdkUtilProvider.getCurrentUserId(),
                guestUserId = analyticsSdkUtilProvider.getGuestUserId()
            )
        )
            .onSuccess {
                EloSdkLogger.d(
                    "Successfully sent ${storedEvents.size} events to server!"
                )
                deleteSentEvents(storedEvents)
            }
            .onFailure {
                EloSdkLogger.e(
                    "Failed to send ${storedEvents.size} events: ${it.message}"
                )
            }
    }

    private suspend fun deleteSentEvents(storedEvents: List<EloAnalyticsEvent>) {
        val deletedCount = eloAnalyticsLocalEventUseCase.deleteEvents(storedEvents.map { it.id })
        EloSdkLogger.d("Deleted $deletedCount events from local DB after sending.")
    }
}

class EloAnalyticsWorkerFactory(
//    private val delegate: WorkerFactory?, todo: check
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return if (workerClassName == EloAnalyticsSyncWorker::class.java.name) {
            EloAnalyticsSyncWorker(
                context = appContext,
                workerParameters = workerParameters,
                dependencyContainer = EloAnalyticsSdk.getDependencyContainer()
            )
        } else {
            // Return null to delegate to the default WorkerFactory
            null //todo: check if needs to return null or
            //    delegate?.createWorker(appContext, workerClassName, workerParameters)

        }
    }
}