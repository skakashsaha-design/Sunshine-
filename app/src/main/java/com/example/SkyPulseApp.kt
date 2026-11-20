package com.example

import android.app.Application
import com.example.di.AppContainer
import com.example.worker.MorningBriefingWorker
import com.example.worker.WeatherSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SkyPulseApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notificationHelper.ensureChannels()
        runCatching { WeatherSyncWorker.schedule(this) }
        appScope.launch {
            runCatching {
                val ns = container.preferencesManager.preferencesFlow.first().notificationSettings
                MorningBriefingWorker.scheduleDaily(
                    context = this@SkyPulseApp,
                    enabled = ns.enabled && ns.dailySummaryEnabled,
                    hour = ns.dailyHour,
                    minute = ns.dailyMinute,
                    replaceExisting = false
                )
            }
        }
    }
}
