package com.greenhorn.neuronet.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.greenhorn.neuronet.model.EloAnalyticsEvent

@Dao
internal interface EloAnalyticsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EloAnalyticsEvent): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<EloAnalyticsEvent>): List<Long>

    @Query("SELECT * FROM analytics_sdk_events LIMIT :limit")
    suspend fun getEvents(limit: Int): List<EloAnalyticsEvent>

    @Query("SELECT * FROM analytics_sdk_events")
    suspend fun getEvents(): List<EloAnalyticsEvent>

    @Query("DELETE FROM analytics_sdk_events WHERE id IN (:eventIds)")
    suspend fun deleteEvents(eventIds: List<Long>): Int

    @Query("SELECT COUNT(*) FROM analytics_sdk_events")
    suspend fun getEventsCount(): Int
}