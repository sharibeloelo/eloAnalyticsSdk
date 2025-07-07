package com.eloelo.analytics

import android.app.Application
import android.provider.Settings
import com.greenhorn.neuronet.PublishEvent

class EApplication : Application() {

    override fun onCreate() {
        super.onCreate()

       val publishEvent = PublishEvent.Builder(this)
            .enableLogs(true)
            .setBaseUrl("https://analytics-producer.eloelo.in/")
            .setApiEndpoint("v2/analytics/send/moe/event")
//            .setAppFlyerId(
//                AppsFlyerLib.getInstance().getAppsFlyerUID(EloEloApplication.getInstance())
//                    .toString()
//            )
            .setSessionId(System.currentTimeMillis().toString())
            .build()

        PublishEvent.init(publishEvent)
    }
}