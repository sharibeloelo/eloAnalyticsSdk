package com.greenhorn.neuronet.db

import android.content.Context
import androidx.lifecycle.ViewModelProvider.NewInstanceFactory.Companion.instance
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.greenhorn.neuronet.AnalyticsEvent
import com.greenhorn.neuronet.EventParamsConverter
import kotlin.jvm.java

/**
 * The Room database that holds the analytics_events table.
 * It follows the singleton pattern to ensure only one instance of the database is created.
 */
@Database(entities = [AnalyticsEvent::class], version = 1, exportSchema = false)
@TypeConverters(
    EventParamsConverter::class
)
abstract class AnalyticsDatabase : RoomDatabase() {

    abstract fun analyticsEventDao(): AnalyticsEventDao

    companion object {
        // Volatile ensures that the instance is always up-to-date and the same for all execution threads.
        @Volatile
        private var INSTANCE: AnalyticsDatabase? = null

        fun getInstance(context: Context): AnalyticsDatabase {
            // Multiple threads can ask for the database instance at the same time,
            // so we use synchronized to ensure only one thread can create it.
            INSTANCE = Room.databaseBuilder(
                context.applicationContext,
                AnalyticsDatabase::class.java,
                "analytics_sdk_database"
            ).build()

            return INSTANCE!!
        }
    }
}