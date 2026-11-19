package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.domain.calc.GeoUtils
import com.example.domain.model.AirQualityData
import com.example.domain.model.TemperatureUnit
import com.example.domain.model.WeatherAlert
import com.example.domain.model.WeatherBundle
import java.text.SimpleDateFormat
import java.util.*

class CurrentWeatherWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_current)
            views.setOnClickPendingIntent(R.id.widget_root, SkyPulseWidgetUpdater.launchIntent(context))
            mgr.updateAppWidget(id, views)
        }
    }
}

class CompactForecastWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_compact)
            views.setOnClickPendingIntent(R.id.widget_root, SkyPulseWidgetUpdater.launchIntent(context))
            mgr.updateAppWidget(id, views)
        }
    }
}

class HourlyForecastWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_hourly)
            views.setOnClickPendingIntent(R.id.widget_root, SkyPulseWidgetUpdater.launchIntent(context))
            mgr.updateAppWidget(id, views)
        }
    }
}

class AqiWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_aqi)
            views.setOnClickPendingIntent(R.id.widget_root, SkyPulseWidgetUpdater.launchIntent(context))
            mgr.updateAppWidget(id, views)
        }
    }
}

class AlertWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_alert)
            views.setOnClickPendingIntent(R.id.widget_root, SkyPulseWidgetUpdater.launchIntent(context))
            mgr.updateAppWidget(id, views)
        }
    }
}

object SkyPulseWidgetUpdater {

    fun launchIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun updateAllWidgets(
        context: Context,
        weather: WeatherBundle,
        aqi: AirQualityData?,
        alerts: List<WeatherAlert>,
        unit: TemperatureUnit
    ) {
        val mgr = AppWidgetManager.getInstance(context)
        val pi = launchIntent(context)
        val timeFmt = SimpleDateFormat("HH:mm", Locale.US).format(Date(weather.lastUpdatedEpochMs))
        val curTemp = GeoUtils.formatTemperature(weather.current.temperatureC, unit)
        val feels = GeoUtils.formatTemperature(weather.current.feelsLikeC, unit)

        // 1. Current Weather Widget
        val curIds = mgr.getAppWidgetIds(ComponentName(context, CurrentWeatherWidgetProvider::class.java))
        curIds.forEach { id ->
            val v = RemoteViews(context.packageName, R.layout.widget_current)
            v.setTextViewText(R.id.widget_city, weather.location.name)
            v.setTextViewText(R.id.widget_temp, curTemp)
            v.setTextViewText(R.id.widget_condition, weather.current.conditionLabel)
            v.setTextViewText(R.id.widget_meta, "Feels $feels · $timeFmt")
            v.setOnClickPendingIntent(R.id.widget_root, pi)
            mgr.updateAppWidget(id, v)
        }

        // 2. Compact Forecast Widget
        val compactIds = mgr.getAppWidgetIds(ComponentName(context, CompactForecastWidgetProvider::class.java))
        val daysLine = weather.daily.take(3).joinToString("  |  ") { d ->
            "${d.isoDate.takeLast(5)}: ${GeoUtils.formatTemperature(d.tempMaxC, unit, false)}/${GeoUtils.formatTemperature(d.tempMinC, unit, false)}"
        }
        compactIds.forEach { id ->
            val v = RemoteViews(context.packageName, R.layout.widget_compact)
            v.setTextViewText(R.id.widget_city, "${weather.location.name} · 3-Day")
            v.setTextViewText(R.id.widget_temp, "$curTemp · ${weather.current.conditionLabel}")
            v.setTextViewText(R.id.widget_days, daysLine.ifBlank { "No daily data" })
            v.setOnClickPendingIntent(R.id.widget_root, pi)
            mgr.updateAppWidget(id, v)
        }

        // 3. Hourly Forecast Widget
        val hourlyIds = mgr.getAppWidgetIds(ComponentName(context, HourlyForecastWidgetProvider::class.java))
        val hourlyStrip = weather.hourly.take(4).joinToString("  ·  ") { h ->
            val hh = h.isoTime.substringAfter("T", "").take(5)
            val prob = h.precipitationProbabilityPercent?.let { " $it%" } ?: ""
            "$hh ${GeoUtils.formatTemperature(h.temperatureC, unit, false)}$prob"
        }
        hourlyIds.forEach { id ->
            val v = RemoteViews(context.packageName, R.layout.widget_hourly)
            v.setTextViewText(R.id.widget_city, "${weather.location.name} Hourly")
            v.setTextViewText(R.id.widget_hourly_strip, hourlyStrip.ifBlank { "No hourly data" })
            v.setOnClickPendingIntent(R.id.widget_root, pi)
            mgr.updateAppWidget(id, v)
        }

        // 4. AQI Widget
        val aqiIds = mgr.getAppWidgetIds(ComponentName(context, AqiWidgetProvider::class.java))
        aqiIds.forEach { id ->
            val v = RemoteViews(context.packageName, R.layout.widget_aqi)
            v.setTextViewText(R.id.widget_aqi_title, "${weather.location.name} US AQI")
            v.setTextViewText(R.id.widget_aqi_score, aqi?.usAqi?.toString() ?: "--")
            v.setTextViewText(R.id.widget_aqi_detail, aqi?.category?.label ?: "Unavailable")
            v.setOnClickPendingIntent(R.id.widget_root, pi)
            mgr.updateAppWidget(id, v)
        }

        // 5. Alert Widget
        val alertIds = mgr.getAppWidgetIds(ComponentName(context, AlertWidgetProvider::class.java))
        val topAlert = alerts.maxByOrNull { it.severity.rank }
        alertIds.forEach { id ->
            val v = RemoteViews(context.packageName, R.layout.widget_alert)
            v.setTextViewText(
                R.id.widget_alert_header,
                if (topAlert != null) "${topAlert.severity.label} Alert (${weather.location.name})"
                else "Alerts · ${weather.location.name}"
            )
            v.setTextViewText(
                R.id.widget_alert_body,
                topAlert?.title ?: "No active weather or hazard warnings"
            )
            v.setOnClickPendingIntent(R.id.widget_root, pi)
            mgr.updateAppWidget(id, v)
        }
    }
}
