package com.example.worker

import android.content.Context
import androidx.work.*
import com.example.SkyPulseApp
import com.example.domain.calc.AstronomyCalculator
import com.example.widget.SkyPulseWidgetUpdater
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MorningBriefingWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? SkyPulseApp ?: return Result.failure()
        val container = app.container
        return try {
            container.locationRepository.ensureInitialLocations()
            val prefs = container.preferencesManager.preferencesFlow.first()
            if (!prefs.notificationSettings.enabled || !prefs.notificationSettings.dailySummaryEnabled) {
                return Result.success()
            }

            val savedLocations = container.locationRepository.savedLocationsFlow.first()
            val gpsLoc = runCatching { container.locationRepository.fetchCurrentGpsLocation() }.getOrNull()
            val isCurrentGps = gpsLoc != null
            val targetLoc = gpsLoc
                ?: savedLocations.firstOrNull { it.isDefault }
                ?: savedLocations.firstOrNull()
                ?: return Result.success()

            val weather = container.weatherRepository.getWeather(targetLoc, prefs, forceRefresh = true).getOrNull()
                ?: return Result.retry()
            val aqi = container.weatherRepository.getAirQuality(targetLoc, forceRefresh = true).getOrNull()
            val astronomy = AstronomyCalculator.calculate(
                targetLoc.latitude,
                targetLoc.longitude,
                targetLoc.timezoneId
            )
            val alertsBundle = container.disasterAlertRepository.loadAlertsAndDisasters(
                targetLoc,
                weather,
                prefs.personalKeys.bmdCustomFeedUrl
            )

            container.notificationHelper.dispatchMorningBriefingNotification(
                weather = weather,
                aqi = aqi,
                astronomy = astronomy,
                alerts = alertsBundle.alerts,
                disasters = alertsBundle.disasters,
                prefs = prefs,
                isCurrentGpsLocation = isCurrentGps,
                respectDailyDedupe = true
            )

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
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "skypulse_morning_briefing"

        fun scheduleDaily(
            context: Context,
            enabled: Boolean,
            hour: Int,
            minute: Int,
            replaceExisting: Boolean = false
        ) {
            val wm = WorkManager.getInstance(context)
            if (!enabled) {
                wm.cancelUniqueWork(WORK_NAME)
                return
            }
            val delayMs = computeInitialDelayMs(hour, minute)
            val request = PeriodicWorkRequestBuilder<MorningBriefingWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()
            wm.enqueueUniquePeriodicWork(
                WORK_NAME,
                if (replaceExisting) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun computeInitialDelayMs(
            targetHour: Int,
            targetMinute: Int,
            now: Calendar = Calendar.getInstance()
        ): Long {
            val clampedHour = targetHour.coerceIn(0, 23)
            val clampedMinute = targetMinute.coerceIn(0, 59)
            val next = (now.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, clampedHour)
                set(Calendar.MINUTE, clampedMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (!after(now)) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            return (next.timeInMillis - now.timeInMillis).coerceAtLeast(1000L)
        }
    }
}
