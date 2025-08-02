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
import com.greenhorn.neuronet.utils.Failure
import com.greenhorn.neuronet.utils.Success
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
        EloSdkLogger.d("ELO ANALYTICS worker started!")

        return withContext(Dispatchers.IO) {
            try {
                // Get total count of pending events
                val totalEventsCount = eloAnalyticsLocalEventUseCase.getEventsCount()
                
                if (totalEventsCount == 0) {
                    EloSdkLogger.d("No pending events found, finishing worker!")
                    return@withContext Result.success()
                }

                EloSdkLogger.d("Total pending events: $totalEventsCount")

                // Get validated batch size from SDK configuration
                val batchSize = analyticsSdkUtilProvider.getSyncBatchSize()
                EloSdkLogger.d("Using configured batch size: $batchSize")

                // Process events in batches using LIMIT only
                val successCount = processEventsInBatches(totalEventsCount, batchSize)
                
                EloSdkLogger.d("Successfully processed $successCount out of $totalEventsCount events")
                
                return@withContext Result.success()
            } catch (e: Exception) {
                e.printStackTrace()
                EloSdkLogger.e("Exception in $TAG: ${e.message}", e)
                analyticsSdkUtilProvider.recordFirebaseNonFatal(error = e)
                Result.failure()
            }
        }
    }

    /**
     * Processes events in batches using LIMIT only for database fetching.
     * 
     * This method:
     * 1. Fetches events from database using LIMIT only
     * 2. Processes each batch independently
     * 3. Deletes successfully sent events
     * 4. Continues until all events are processed
     * 
     * @param totalEventsCount Total number of events to process
     * @param batchSize Number of events to process in each batch
     * @return Number of successfully processed events
     */
    private suspend fun processEventsInBatches(totalEventsCount: Int, batchSize: Int): Int {
        var processedCount = 0
        var currentBatch = 0
        
        while (processedCount < totalEventsCount) {
            currentBatch++
            val currentBatchSize = minOf(batchSize, totalEventsCount - processedCount)
            
            EloSdkLogger.d("Processing batch $currentBatch: $currentBatchSize events")
            
            try {
                val batchSuccess = processBatch(currentBatchSize)
                if (batchSuccess) {
                    processedCount += currentBatchSize
                    EloSdkLogger.d("Batch $currentBatch completed successfully. Progress: $processedCount/$totalEventsCount")
                } else {
                    EloSdkLogger.e("Batch $currentBatch failed, moving to next batch")
                    // Continue with next batch even if current fails
                    processedCount += currentBatchSize
                }
            } catch (e: Exception) {
                EloSdkLogger.e("Exception in batch $currentBatch: ${e.message}")
                // Continue with next batch even if current fails
                processedCount += currentBatchSize
            }
        }
        
        return processedCount
    }

    /**
     * Processes a single batch of events.
     * 
     * @param batchSize Number of events to process in this batch
     * @return true if batch was processed successfully, false otherwise
     */
    private suspend fun processBatch(batchSize: Int): Boolean {
        try {
            // Fetch batch of events using LIMIT only
            val storedEvents = fetchStoredEventsBatch(batchSize)
            
            if (storedEvents.isEmpty()) {
                EloSdkLogger.d("No more events to process")
                return true
            }

            // Enrich events with user data
            val enrichedEvents = storedEvents.map { event ->
                event.copy(
                    eventData = event.eventData.toMutableMap().apply {
                        val id = if (event.isUserLogin) {
                            analyticsSdkUtilProvider.getCurrentUserId()
                        } else {
                            analyticsSdkUtilProvider.getGuestUserId()
                        }
                        AnalyticsSdkUtilProvider.getUserIdAttributeKeyName()?.let {
                            this[it] = id.toString()
                        }
                    }
                )
            }

            // Send batch to server
            val success = sendEventsBatchToServer(enrichedEvents)
            
            if (success) {
                // Delete successfully sent events
                deleteSentEvents(successfullySentEvents = storedEvents)
                return true
            } else {
                EloSdkLogger.e("Failed to send batch to server")
                return false
            }
        } catch (e: Exception) {
            EloSdkLogger.e("Exception in batch processing: ${e.message}")
            return false
        }
    }

    /**
     * Fetches a batch of events from the database using LIMIT only.
     * 
     * This method uses LIMIT for efficient database fetching,
     * avoiding the need to load all events into memory.
     * 
     * @param limit Maximum number of events to fetch
     * @return List of events from the database
     */
    private suspend fun fetchStoredEventsBatch(limit: Int): List<EloAnalyticsEvent> {
        return eloAnalyticsLocalEventUseCase.getEvents(limit).also { storedEvents ->
            EloSdkLogger.d("Fetched batch of ${storedEvents.size} events")
        }
    }

    /**
     * Sends a batch of events to the server.
     * 
     * @param storedEvents List of events to send
     * @return true if events were sent successfully, false otherwise
     */
    private suspend fun sendEventsBatchToServer(storedEvents: List<EloAnalyticsEvent>): Boolean {
        return try {
            eloAnalyticsEventUseCase.sendAnalyticEvents(
                EventMapper.toEventDtos(
                    events = storedEvents,
                    currentUserId = analyticsSdkUtilProvider.getCurrentUserId(),
                    guestUserId = analyticsSdkUtilProvider.getGuestUserId()
                )
            ).let { result ->
                when (result) {
                    is Success -> {
                        EloSdkLogger.d("Successfully sent batch of ${storedEvents.size} events to server!")
                        true
                    }
                    is Failure -> {
                        EloSdkLogger.e("Failed to send batch of ${storedEvents.size} events: ${result.errorResponse.message}")
                        false
                    }
                }
            }
        } catch (e: Exception) {
            EloSdkLogger.e("Exception sending batch to server: ${e.message}")
            false
        }
    }

    /**
     * Deletes successfully sent events from the local database.
     * 
     * @param successfullySentEvents List of events to delete
     */
    private suspend fun deleteSentEvents(successfullySentEvents: List<EloAnalyticsEvent>) {
        try {
            val deletedCount = eloAnalyticsLocalEventUseCase.deleteEvents(successfullySentEvents.map { it.id })
            EloSdkLogger.d("Deleted $deletedCount events from local DB after successful batch send")
        } catch (e: Exception) {
            EloSdkLogger.e("Failed to delete events from local DB: ${e.message}")
        }
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