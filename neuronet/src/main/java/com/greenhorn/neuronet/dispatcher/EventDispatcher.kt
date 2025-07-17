package com.greenhorn.neuronet.dispatcher

import com.greenhorn.neuronet.EloAnalyticsEvent
import com.greenhorn.neuronet.client.ApiClient
import com.greenhorn.neuronet.log.Logger
import com.greenhorn.neuronet.model.EloAnalyticsEventDto
//import com.greenhorn.neuronet.model.EventMapper
import com.greenhorn.neuronet.repository.EventRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class EventDispatcher(
    private val eventRepository: EventRepository,
    private val eventApi: ApiClient,
    private val finalApiEndpoint : String,
    private val batchSize: Int = 10,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val pendingEventsMutex = Mutex()
    private val _pendingEventCount = MutableStateFlow(0L)
    private var currentUserId : Long = 0L
    private var guestUserId : Long = 0L
    val pendingEventCount: StateFlow<Long> = _pendingEventCount.asStateFlow()

    init {
        scope.launch {
            _pendingEventCount.value = eventRepository.getEventCount()
        }
    }

    fun getBatchSizeOfEvents() = batchSize

    suspend fun addEvent(event: EloAnalyticsEvent, currentUserId: Long, guestUserId: Long) {
        pendingEventsMutex.withLock {
            this.currentUserId = currentUserId
            this.guestUserId = guestUserId
            eventRepository.insertEvent(event)
            Logger.d("Event : $event")
            _pendingEventCount.value = eventRepository.getEventCount()
            Logger.d("event count : ${eventRepository.getEventCount()}")
        }
    }

    suspend fun sendSingleEvent(event: EloAnalyticsEvent){
        pendingEventsMutex.withLock {
            triggerEventUpload(event)
        }
    }

    private suspend fun triggerEventUpload(event: EloAnalyticsEvent?) {
        pendingEventsMutex.withLock {
            if(event == null) return@withLock
//            val response = eventApi.sendEvent(finalApiEndpoint, EloAnalyticsEventDto(event.eventName,
//                event.timestamp,
//                event.timestamp, event.sessionTimeStamp, event.payload))
//            if (response.isSuccessful) {
//                Logger.d("response single event send: ${response}")
//                println("Successfully uploaded event.${response}")
//            } else {
//                println("Failed to upload event. Will retry later.")
//                //TODO: Since, if api is failed the data will be lost as in this case we are not saving
//                // data to localDB. Its better to save it in local db incase of failure or use in memory like it is and also save in local and db and delet after success.
//            }
        }
    }

    suspend fun triggerEventUpload(url : String) : Boolean {
        return true
//        return pendingEventsMutex.withLock {
//            val eventsToUpload = eventRepository.getUnsyncedEvents(batchSize)
//            Logger.d("Trigger Sync Event : ${eventsToUpload}")
//            if (eventsToUpload.isEmpty()) {
//                Logger.d("No unsynced events to upload.")
//                true // Return true as there was no failure.
//            }else{
//                Logger.d("Attempting to upload ${eventsToUpload.size} events.")
//                val response = eventApi.sendEvents(url, EventMapper.toEventDtos(events = eventsToUpload, currentUserId, guestUserId))
//                if (response.isSuccessful) {
//                    Logger.d("Successfully uploaded and marked events as synced. : ${response.isSuccessful}")
//                    val uploadedEventIds = eventsToUpload.map { it.id }
//                    //TODO: do batching for deleting events large number events deletion can result into crash. Check implementation
//                    // in EloAnalyticsSdk
//
//                    //TODO: WHY ARE WE deleting sync events then marking same ids synced?? if we have already deleted they wont exist,
//                    eventRepository.markEventsAsSynced(uploadedEventIds)
//                    eventRepository.deleteSyncedEvents(uploadedEventIds) // Clean up
//                    Logger.d("Trigger Event_delete : $uploadedEventIds")
////                    _pendingEventCount.value = eventRepository.getEventCount()
//                    true
//                } else {
//                    Logger.d("Failed to upload events. Will retry later.")
//                    false
//                }
//            }
//        }
    }
}