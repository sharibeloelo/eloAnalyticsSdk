package com.eloelo.analytics

// In the app's Application class (e.g., com.yourapp.YourApplication.kt)
// In your app's Application class (e.g., com.yourapp.EApplication.kt)

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager

class EApplication : Application() {

    override fun onCreate() {
        super.onCreate()
    }
}