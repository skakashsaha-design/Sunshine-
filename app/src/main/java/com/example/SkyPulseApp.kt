package com.example

import android.app.Application
import com.example.di.AppContainer
import com.example.worker.WeatherSyncWorker

class SkyPulseApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notificationHelper.ensureChannels()
        runCatching { WeatherSyncWorker.schedule(this) }
    }
}
