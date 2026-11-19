package com.example.worker

import android.content.Context
import androidx.work.*
import com.example.SkyPulseApp
import com.example.widget.SkyPulseWidgetUpdater
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class WeatherSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? SkyPulseApp ?: return Result.failure()
        val container = app.container
        return try {
            container.locationRepository.ensureInitialLocations()
            val prefs = container.preferencesManager.preferencesFlow.first()
            val locations = container.locationRepository.savedLocationsFlow.first()
            val activeLoc = locations.firstOrNull { it.isDefault } ?: locations.firstOrNull()
                ?: return Result.success()

            val weather = container.weatherRepository.getWeather(activeLoc, prefs, forceRefresh = true).getOrNull()
            val aqi = container.weatherRepository.getAirQuality(activeLoc, forceRefresh = true).getOrNull()
            val alertsBundle = container.disasterAlertRepository.loadAlertsAndDisasters(
                activeLoc,
                weather,
                prefs.personalKeys.bmdCustomFeedUrl
            )

            if (weather != null) {
                container.notificationHelper.evaluateAndDispatchAlerts(
                    settings = prefs.notificationSettings,
                    weather = weather,
                    aqi = aqi,
                    alerts = alertsBundle.alerts,
                    disasters = alertsBundle.disasters,
                    unit = prefs.temperatureUnit
                )
                SkyPulseWidgetUpdater.updateAllWidgets(
                    context = applicationContext,
                    weather = weather,
                    aqi = aqi,
                    alerts = alertsBundle.alerts,
                    unit = prefs.temperatureUnit
                )
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "skypulse_periodic_sync"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()
            val request = PeriodicWorkRequestBuilder<WeatherSyncWorker>(3, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
