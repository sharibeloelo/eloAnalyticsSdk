package com.eloelo.analytics.dispatcher

import com.eloelo.analytics.AnalyticsEvent
import com.eloelo.analytics.client.ApiClient
import com.eloelo.analytics.model.Event
import com.eloelo.analytics.repository.EventRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class EventDispatcher(
    private val eventRepository: EventRepository,
    private val eventApi: ApiClient,
    private val batchSize: Int = 10,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val pendingEventsMutex = Mutex()
    private val _pendingEventCount = MutableStateFlow(0L)
    val pendingEventCount: StateFlow<Long> = _pendingEventCount.asStateFlow()

    init {
        scope.launch {
            _pendingEventCount.value = eventRepository.getEventCount()
        }

        // Trigger API call when batch size is reached
        _pendingEventCount
            .filter { it >= batchSize }
            .onEach {
                triggerEventUpload()
            }
            .launchIn(scope)
    }

    suspend fun addEvent(event: Event) {
        pendingEventsMutex.withLock {
            eventRepository.insertEvent(event)
            _pendingEventCount.value = eventRepository.getEventCount()
        }
    }

    suspend fun triggerEventUpload() {
        pendingEventsMutex.withLock {
            val eventsToUpload = eventRepository.getUnsyncedEvents(batchSize)
            if (eventsToUpload.isNotEmpty()) {
                println("Attempting to upload ${eventsToUpload.size} events.")
                val success = eventApi.sendEvents(eventsToUpload)
                if (success) {
                    val uploadedEventIds = eventsToUpload.map { it.id }
                    eventRepository.markEventsAsSynced(uploadedEventIds)
                    eventRepository.deleteSyncedEvents(uploadedEventIds) // Clean up
                    _pendingEventCount.value = eventRepository.getEventCount()
                    println("Successfully uploaded and marked events as synced.")
                } else {
                    println("Failed to upload events. Will retry later.")
                }
            } else {
                println("No unsynced events to upload.")
            }
        }
    }
}