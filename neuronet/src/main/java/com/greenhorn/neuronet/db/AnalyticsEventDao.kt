package com.greenhorn.neuronet.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.greenhorn.neuronet.AnalyticsEvent

@Dao
interface AnalyticsEventDao {

    /**
     * Inserts a new event into the database.
     * If there's a conflict, it replaces the existing entry.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: AnalyticsEvent)

    /**
     * Fetches a batch of events from the database.
     * @param limit The maximum number of events to retrieve.
     * @return A list of AnalyticsEvent.
     */
    @Query("SELECT * FROM analytics_events WHERE isSynced = 0 ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getUnsyncedEvents(limit: Int): List<AnalyticsEvent>


    /**
     * Gets the total count of events currently stored in the database.
     * @return The number of events.
     */
    @Query("SELECT COUNT(id) FROM analytics_events")
    suspend fun getEventCount(): Long

    @Query("UPDATE analytics_events SET isSynced = 1 WHERE id IN (:eventIds)")
    suspend fun markEventsAsSynced(eventIds: List<Long>)

    /**
     * Deletes a list of events from the database, identified by their IDs.
     * @param eventIds The list of primary keys of the events to delete.
     */
    @Query("DELETE FROM analytics_events WHERE id IN (:eventIds)")
    suspend fun deleteSyncedEvents(eventIds: List<Long>)

}