package com.eloelo.analytics

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.greenhorn.neuronet.EloAnalyticsSdk
import com.greenhorn.neuronet.utils.AnalyticsSdkUtilProvider

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsSdkUtilProvider.updateSessionTimeStampAndCache(System.currentTimeMillis().toString())

        EloAnalyticsSdk.getInstance().trackEvent(
            name = "TEST_EVENT_ON_RESUME",
            eventData = mutableMapOf(
                "checking1" to "data1",
                "checking2" to "data2"
            )
        )
    }

    override fun onPause() {
        super.onPause()

        EloAnalyticsSdk.getInstance().trackEvent(
            name = "TEST_EVENT_ON_PAUSE",
            eventData = mutableMapOf(
                "checking1" to "data1",
                "checking2" to "data2"
            )
        )
    }
}