package com.example.notification

import com.example.domain.calc.GeoUtils
import com.example.domain.model.*
import java.util.Locale
import kotlin.math.roundToInt

data class MorningBriefingContent(
    val title: String,
    val compactSummary: String,
    val fullBriefingText: String,
    val iconSpec: NotificationIconSpec
)

object MorningBriefingBuilder {

    fun build(
        weather: WeatherBundle,
        aqi: AirQualityData?,
        astronomy: AstronomySnapshot?,
        alerts: List<WeatherAlert>,
        disasters: List<DisasterEvent>,
        prefs: UserPreferences,
        isCurrentGpsLocation: Boolean
    ): MorningBriefingContent {
        val cur = weather.current
        val today = weather.daily.firstOrNull()
        val tempNow = GeoUtils.formatTemperature(cur.temperatureC, prefs.temperatureUnit)
        val feelsNow = GeoUtils.formatTemperature(cur.feelsLikeC, prefs.temperatureUnit)
        val highLow = if (today != null) {
            "H ${GeoUtils.formatTemperature(today.tempMaxC, prefs.temperatureUnit, false)} / L ${GeoUtils.formatTemperature(today.tempMinC, prefs.temperatureUnit, false)}"
        } else ""

        val next12 = weather.hourly.take(12)
        val maxRainProb = next12.mapNotNull { it.precipitationProbabilityPercent }.maxOrNull()
            ?: today?.rainProbabilityPercent
            ?: cur.rainProbabilityPercent
            ?: 0
        val rainWindow = next12.firstOrNull { (it.precipitationProbabilityPercent ?: 0) >= 45 }
            ?.isoTime?.substringAfter("T", "")?.take(5)

        val windStr = GeoUtils.formatWind(cur.windSpeedKmh, prefs.windUnit)
        val dirStr = GeoUtils.compassDirection(cur.windDirectionDeg)
        val gustStr = GeoUtils.formatWind(cur.windGustKmh, prefs.windUnit)

        val aqiPart = aqi?.usAqi?.let { "US AQI $it (${aqi.category.label})" } ?: "AQI unavailable"
        val uvPeak = today?.uvIndexMax ?: cur.uvIndex
        val uvPart = uvPeak?.let { "Peak UV ${String.format(Locale.US, "%.0f", it)}" }

        val topAlert = alerts.maxByOrNull { it.severity.rank }
        val nearbyDisaster = disasters.firstOrNull { (it.distanceKm ?: Double.MAX_VALUE) <= prefs.notificationSettings.disasterRadiusKm }

        val locBadge = if (isCurrentGpsLocation) "${weather.location.name} (GPS)" else weather.location.name
        val flag = GeoUtils.countryCodeToFlagEmoji(weather.location.countryCode)
        val locPrefix = if (flag.isNotBlank()) "$flag $locBadge" else locBadge

        val title = if (topAlert != null && topAlert.severity.rank >= AlertSeverity.MODERATE.rank) {
            "Morning Briefing · $locPrefix: $tempNow (${topAlert.severity.label} Alert)"
        } else {
            "Morning Briefing · $locPrefix: $tempNow ${cur.conditionLabel}"
        }

        val compactSummary = buildList {
            if (highLow.isNotBlank()) add(highLow)
            add("Feels $feelsNow")
            add("Rain $maxRainProb%")
            if (aqi?.usAqi != null) add("AQI ${aqi.usAqi}")
        }.joinToString(" · ")

        val lines = mutableListOf<String>()
        lines += "Current: $tempNow (${cur.conditionLabel}), feels like $feelsNow${if (highLow.isNotBlank()) " · $highLow" else ""}."
        lines += if (rainWindow != null) {
            "Precipitation: Peaks at $maxRainProb% (earliest expected ~$rainWindow)."
        } else {
            "Precipitation: Low rain chance ($maxRainProb% over next 12h)."
        }
        lines += "Wind & UV: $windStr from $dirStr (gusts $gustStr)${uvPart?.let { " · $it" } ?: ""}."
        lines += "Air Quality: $aqiPart."

        if (astronomy != null) {
            lines += "Sun & Moon: Sunrise ${astronomy.sun.sunriseTime} · Sunset ${astronomy.sun.sunsetTime} · ${astronomy.moon.phaseType.label}."
        }

        if (topAlert != null) {
            lines += "Warning (${topAlert.severity.label}): ${topAlert.title} [${topAlert.sourceName}]."
        } else if (nearbyDisaster != null) {
            lines += "Regional Hazard: ${nearbyDisaster.title} (${nearbyDisaster.distanceKm?.roundToInt() ?: "--"} km away, ${nearbyDisaster.source})."
        }

        val advisor = weather.todayAiAnalysis?.wardrobeAndHealthTips?.firstOrNull()
            ?: weather.dailyAdvisorLines.firstOrNull()
        if (!advisor.isNullOrBlank()) {
            lines += "Advice: $advisor"
        }

        if (weather.isOfflineCache) {
            lines += "Source: ${weather.activeProvider.displayName} (Cached offline data)."
        } else {
            lines += "Source: ${weather.activeProvider.displayName}."
        }

        val iconSpec = when {
            topAlert != null && topAlert.severity.rank >= AlertSeverity.MODERATE.rank ->
                NotificationIconResolver.forAlertCategory(topAlert.category, topAlert.severity)
            nearbyDisaster != null && nearbyDisaster.severity.rank >= AlertSeverity.MODERATE.rank ->
                NotificationIconResolver.forDisasterCategory(nearbyDisaster.category, nearbyDisaster.tsunamiFlag)
            else -> NotificationIconResolver.forWeatherCondition(cur.condition)
        }

        return MorningBriefingContent(
            title = title,
            compactSummary = compactSummary,
            fullBriefingText = lines.joinToString("\n"),
            iconSpec = iconSpec
        )
    }
}
