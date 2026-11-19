package com.example.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.NotificationLogEntity
import com.example.data.local.SkyPulseDao
import com.example.domain.calc.GeoUtils
import com.example.domain.model.*
import kotlin.math.roundToInt

class NotificationHelper(
    private val context: Context,
    private val dao: SkyPulseDao
) {
    companion object {
        const val CHANNEL_DAILY = "skypulse_daily"
        const val CHANNEL_ALERTS = "skypulse_alerts"
        const val CHANNEL_RAIN = "skypulse_rain"
        const val CHANNEL_AQI = "skypulse_aqi"
        const val CHANNEL_DISASTER = "skypulse_disaster"
    }

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channels = listOf(
            NotificationChannel(CHANNEL_DAILY, context.getString(R.string.channel_daily_name), NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(CHANNEL_ALERTS, context.getString(R.string.channel_alerts_name), NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel(CHANNEL_RAIN, context.getString(R.string.channel_rain_name), NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(CHANNEL_AQI, context.getString(R.string.channel_aqi_name), NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(CHANNEL_DISASTER, context.getString(R.string.channel_disaster_name), NotificationManager.IMPORTANCE_HIGH)
        )
        mgr.createNotificationChannels(channels)
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    suspend fun dispatchQuickViewNotification(
        weather: WeatherBundle,
        aqi: AirQualityData?,
        unit: TemperatureUnit
    ) {
        if (!hasNotificationPermission()) return
        ensureChannels()

        val temp = GeoUtils.formatTemperature(weather.current.temperatureC, unit)
        val feels = GeoUtils.formatTemperature(weather.current.feelsLikeC, unit)
        val nextRain = weather.hourly.take(8).firstOrNull { (it.precipitationProbabilityPercent ?: 0) >= 45 }
        val rainPart = if (nextRain != null) {
            val hhmm = nextRain.isoTime.substringAfter("T", "").take(5)
            "Rain ${nextRain.precipitationProbabilityPercent}% around $hhmm"
        } else {
            "Dry next 8h"
        }
        val aqiPart = aqi?.usAqi?.let { "AQI $it" } ?: "AQI --"
        val title = "${weather.location.name}  $temp · ${weather.current.conditionLabel}"
        val body = "Feels like $feels · $rainPart · $aqiPart"

        postNotification(
            id = 1001,
            channelId = CHANNEL_DAILY,
            title = title,
            body = body,
            bigText = "$body\n${weather.dailyAdvisorLines.firstOrNull().orEmpty()}"
        )
    }

    suspend fun evaluateAndDispatchAlerts(
        settings: NotificationSettings,
        weather: WeatherBundle,
        aqi: AirQualityData?,
        alerts: List<WeatherAlert>,
        disasters: List<DisasterEvent>,
        unit: TemperatureUnit
    ) {
        if (!settings.enabled || !hasNotificationPermission()) return
        ensureChannels()
        val now = System.currentTimeMillis()
        val dedupeWindowMs = 6 * 60 * 60 * 1000L

        if (settings.severeWeatherEnabled) {
            alerts.filter { it.severity.rank >= settings.minSeverityRank }.take(2).forEach { alert ->
                if (!wasRecentlySent("alert_${alert.id}", now, dedupeWindowMs)) {
                    postNotification(
                        id = alert.id.hashCode(),
                        channelId = CHANNEL_ALERTS,
                        title = "${alert.severity.label}: ${alert.title}",
                        body = "${alert.sourceName} · ${alert.affectedArea}",
                        bigText = alert.description
                    )
                    dao.markNotificationSent(NotificationLogEntity("alert_${alert.id}", now))
                }
            }
        }

        if (settings.rainAlertsEnabled) {
            val imminent = weather.hourly.take(3).firstOrNull {
                (it.precipitationProbabilityPercent ?: 0) >= 65 || it.condition == WeatherCondition.THUNDERSTORM
            }
            if (imminent != null) {
                val key = "rain_${weather.location.id}_${imminent.isoTime.take(13)}"
                if (!wasRecentlySent(key, now, dedupeWindowMs)) {
                    val label = if (imminent.condition == WeatherCondition.THUNDERSTORM) "Thunderstorm" else "Rain"
                    postNotification(
                        id = key.hashCode(),
                        channelId = CHANNEL_RAIN,
                        title = "$label approaching ${weather.location.name}",
                        body = "${imminent.precipitationProbabilityPercent ?: 70}% chance at ${imminent.isoTime.substringAfter("T", "").take(5)} · ${GeoUtils.formatTemperature(imminent.temperatureC, unit)}",
                        bigText = null
                    )
                    dao.markNotificationSent(NotificationLogEntity(key, now))
                }
            }
        }

        if (settings.aqiAlertsEnabled && (aqi?.usAqi ?: 0) >= 151) {
            val key = "aqi_${weather.location.id}"
            if (!wasRecentlySent(key, now, dedupeWindowMs)) {
                postNotification(
                    id = key.hashCode(),
                    channelId = CHANNEL_AQI,
                    title = "Unhealthy Air Quality in ${weather.location.name} (AQI ${aqi?.usAqi})",
                    body = aqi?.healthGuidance ?: "Wear a mask outdoors.",
                    bigText = null
                )
                dao.markNotificationSent(NotificationLogEntity(key, now))
            }
        }

        if (settings.disasterAlertsEnabled) {
            val nearby = disasters.firstOrNull {
                (it.distanceKm ?: Double.MAX_VALUE) <= settings.disasterRadiusKm &&
                    it.severity.rank >= settings.minSeverityRank
            }
            if (nearby != null) {
                val key = "disaster_${nearby.id}"
                if (!wasRecentlySent(key, now, dedupeWindowMs * 2)) {
                    postNotification(
                        id = key.hashCode(),
                        channelId = CHANNEL_DISASTER,
                        title = "${nearby.category.label}: ${nearby.title}",
                        body = "${nearby.distanceKm?.roundToInt()} km away · Source: ${nearby.source}",
                        bigText = "${nearby.affectedRegion} · ${nearby.status}"
                    )
                    dao.markNotificationSent(NotificationLogEntity(key, now))
                }
            }
        }
    }

    private suspend fun wasRecentlySent(key: String, now: Long, windowMs: Long): Boolean {
        val log = dao.getNotificationLog(key) ?: return false
        return (now - log.sentEpochMs) < windowMs
    }

    private fun postNotification(
        id: Int,
        channelId: String,
        title: String,
        body: String,
        bigText: String?
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(
                if (channelId == CHANNEL_ALERTS || channelId == CHANNEL_DISASTER) {
                    NotificationCompat.PRIORITY_HIGH
                } else NotificationCompat.PRIORITY_DEFAULT
            )
        if (!bigText.isNullOrBlank()) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
        }
        runCatching {
            NotificationManagerCompat.from(context).notify(id, builder.build())
        }
    }
}
