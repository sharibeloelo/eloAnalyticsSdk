package com.eloelo.analytics

// In the app's Application class (e.g., com.yourapp.YourApplication.kt)
// In your app's Application class (e.g., com.yourapp.EApplication.kt)

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager

class EApplication : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()
        WorkManager.initialize(context = this, Configuration.Builder().build())
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()
}