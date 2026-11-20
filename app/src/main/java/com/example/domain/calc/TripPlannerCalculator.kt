package com.example.domain.calc

import com.example.domain.model.*
import java.util.Locale
import kotlin.math.roundToInt

object TripPlannerCalculator {

    fun evaluateDay(
        bundle: WeatherBundle,
        day: DailyForecast,
        activityType: TripActivityType,
        timeWindow: EventTimeWindow,
        aqi: AirQualityData?,
        alerts: List<WeatherAlert>,
        unit: TemperatureUnit
    ): TripEvaluation {
        val dayHours = bundle.hourly.filter { it.isoTime.startsWith(day.isoDate) }
        val windowHours = dayHours.filter { hour ->
            val h = hour.isoTime.substringAfter("T", "").take(2).toIntOrNull() ?: 12
            h in timeWindow.startHour..timeWindow.endHour
        }.ifEmpty { dayHours }

        val peakRainProb = windowHours.mapNotNull { it.precipitationProbabilityPercent }.maxOrNull()
            ?: day.rainProbabilityPercent ?: 0
        val totalRainMm = if (windowHours.isNotEmpty()) {
            windowHours.mapNotNull { it.precipitationMm }.sum()
        } else {
            day.rainAmountMm ?: 0.0
        }
        val peakWindGust = windowHours.mapNotNull { it.windGustKmh ?: it.windSpeedKmh }.maxOrNull()
            ?: day.windGustMaxKmh ?: day.windMaxKmh ?: bundle.current.windGustKmh ?: 0.0
        val maxUv = windowHours.mapNotNull { it.uvIndex }.maxOrNull() ?: day.uvIndexMax
        val avgHumidity = windowHours.mapNotNull { it.humidityPercent }.average().takeIf { !it.isNaN() }?.roundToInt()
            ?: day.humidityAvgPercent

        val dayAqi = if (day.isoDate == bundle.daily.firstOrNull()?.isoDate) {
            aqi?.usAqi
        } else {
            val matchingHourAqis = aqi?.hourlyAqi
                ?.filter { it.isoTime.startsWith(day.isoDate) }
                ?.mapNotNull { it.usAqi }
            matchingHourAqis?.takeIf { it.isNotEmpty() }?.average()?.roundToInt() ?: aqi?.usAqi
        }

        val hasThunder = windowHours.any { it.condition == WeatherCondition.THUNDERSTORM } ||
            day.condition == WeatherCondition.THUNDERSTORM
        val hasHeavyRain = windowHours.any { it.condition == WeatherCondition.HEAVY_RAIN } ||
            day.condition == WeatherCondition.HEAVY_RAIN

        val activeSevereAlerts = alerts.filter { it.severity.rank >= AlertSeverity.MODERATE.rank }

        var score = 95
        val risks = mutableListOf<String>()
        val checklist = mutableListOf<String>()

        // Rain penalty
        when {
            hasThunder || hasHeavyRain -> {
                score -= 42
                risks += "Thunderstorm / heavy downpour signature in forecast window"
            }
            peakRainProb >= 70 || totalRainMm >= activityType.rainLimitMm * 2.5 -> {
                score -= 34
                risks += "High precipitation risk ($peakRainProb% chance, ~${String.format(Locale.US, "%.1f", totalRainMm)} mm)"
            }
            peakRainProb >= 40 || totalRainMm >= activityType.rainLimitMm -> {
                score -= 18
                risks += "Scattered showers possible ($peakRainProb% peak chance)"
            }
        }

        // Wind / canopy / travel safety penalty
        when {
            peakWindGust >= activityType.windLimitKmh * 1.45 -> {
                score -= 28
                risks += "Strong wind gusts up to ${peakWindGust.roundToInt()} km/h (unsafe for open tents/decor)"
            }
            peakWindGust >= activityType.windLimitKmh -> {
                score -= 14
                risks += "Breezy gusts (~${peakWindGust.roundToInt()} km/h) — anchor tents, banners & tripods"
            }
        }

        // Thermal comfort penalty
        when {
            day.tempMaxC >= 39.0 -> {
                score -= 22
                risks += "Extreme daytime heat (${GeoUtils.formatTemperature(day.tempMaxC, unit)})"
            }
            day.tempMaxC >= 35.0 && (avgHumidity ?: 60) >= 65 -> {
                score -= 14
                risks += "High heat & muggy humidity (${avgHumidity ?: 65}% RH)"
            }
            day.tempMinC <= 5.0 -> {
                score -= 15
                risks += "Cold temperatures down to ${GeoUtils.formatTemperature(day.tempMinC, unit)}"
            }
        }

        // UV penalty for outdoor events
        if ((maxUv ?: 0.0) >= 8.0 && timeWindow != EventTimeWindow.EVENING) {
            score -= 8
            risks += "Very high UV index (${String.format(Locale.US, "%.1f", maxUv)})"
        }

        // AQI penalty
        if (dayAqi != null) {
            when {
                dayAqi >= 200 -> {
                    score -= 24
                    risks += "Very unhealthy air quality (US AQI $dayAqi)"
                }
                dayAqi >= 151 -> {
                    score -= 15
                    risks += "Unhealthy air quality (US AQI $dayAqi)"
                }
                dayAqi >= 101 -> {
                    score -= 7
                    risks += "Moderate-sensitive AQI ($dayAqi)"
                }
            }
        }

        // Active official/BMD/BWOT alerts
        if (activeSevereAlerts.isNotEmpty() && day.isoDate == bundle.daily.firstOrNull()?.isoDate) {
            score -= 20
            risks += "Active regional warning: ${activeSevereAlerts.first().title}"
        }

        score = score.coerceIn(5, 99)

        val verdict = when {
            score >= 78 && !hasThunder -> HostVerdict.IDEAL_GO
            score >= 58 -> HostVerdict.FAIR_WITH_BACKUP
            score >= 38 -> HostVerdict.CAUTION_PREPARE
            else -> HostVerdict.HIGH_RISK_RESCHEDULE
        }

        val verdictDetail = when (verdict) {
            HostVerdict.IDEAL_GO ->
                "Yes — hosting ${activityType.label.lowercase()} on ${day.isoDate} looks favorable ($score/100)."
            HostVerdict.FAIR_WITH_BACKUP ->
                "Mostly fine with light preparation — keep shade or rain cover ready ($score/100)."
            HostVerdict.CAUTION_PREPARE ->
                "Weather friction detected — strongly recommend covered venue or shifting timing ($score/100)."
            HostVerdict.HIGH_RISK_RESCHEDULE ->
                "Not ideal to host outdoors on ${day.isoDate} — move indoors or reschedule ($score/100)."
        }

        // Compute best 3-hour block on this date
        val bestWindowLabel = computeBestWindow(windowHours, day)

        // Build actionable checklist
        if (peakRainProb >= 35 || totalRainMm >= 0.5) {
            checklist += "Arrange waterproof canopy / indoor backup & umbrellas ($peakRainProb% rain chance)."
        }
        if (peakWindGust >= 28.0) {
            checklist += "Secure tents, lighting rigs & signage for ${peakWindGust.roundToInt()} km/h gusts."
        }
        if (day.tempMaxC >= 32.0) {
            checklist += "Provide chilled hydration stations & shaded seating for guests."
        }
        if ((maxUv ?: 0.0) >= 6.0) {
            checklist += "Peak UV ${String.format(Locale.US, "%.1f", maxUv)} — schedule photos/ceremony outside 11:00–14:30."
        }
        if ((dayAqi ?: 0) >= 120) {
            checklist += "Keep N95 masks available for sensitive guests (US AQI $dayAqi)."
        }
        if (checklist.isEmpty()) {
            checklist += "Comfortable conditions expected — standard event prep & golden-hour lighting setup sufficient."
        }

        val tempSummary = "${GeoUtils.formatTemperature(day.tempMaxC, unit, false)} / ${GeoUtils.formatTemperature(day.tempMinC, unit, false)}"

        return TripEvaluation(
            locationName = bundle.location.name,
            isoDate = day.isoDate,
            activityType = activityType,
            timeWindow = timeWindow,
            score = score,
            verdict = verdict,
            verdictDetail = verdictDetail,
            bestWindowLabel = bestWindowLabel,
            tempSummary = tempSummary,
            peakRainProbPercent = peakRainProb,
            totalRainMm = totalRainMm,
            peakWindGustKmh = peakWindGust,
            maxUvIndex = maxUv,
            aqiEstimate = dayAqi,
            riskFactors = if (risks.isEmpty()) listOf("No significant weather risks detected") else risks,
            hostChecklist = checklist,
            hourlyWindowSlots = windowHours
        )
    }

    private fun computeBestWindow(hours: List<HourlyForecast>, day: DailyForecast): String {
        if (hours.size < 3) {
            return day.sunriseIso?.let { "Morning after $it or late afternoon" } ?: "Mid-morning / late afternoon"
        }
        var bestScore = Int.MIN_VALUE
        var bestStart = hours.first().isoTime
        var bestEnd = hours.last().isoTime
        for (i in 0..(hours.size - 3)) {
            val slice = hours.subList(i, i + 3)
            val avgRain = slice.mapNotNull { it.precipitationProbabilityPercent }.average().takeIf { !it.isNaN() } ?: 0.0
            val avgWind = slice.mapNotNull { it.windGustKmh ?: it.windSpeedKmh }.average().takeIf { !it.isNaN() } ?: 10.0
            val stormPenalty = if (slice.any { it.condition == WeatherCondition.THUNDERSTORM }) 60 else 0
            val slotScore = (100 - avgRain - avgWind * 0.7 - stormPenalty).roundToInt()
            if (slotScore > bestScore) {
                bestScore = slotScore
                bestStart = slice.first().isoTime.substringAfter("T", "").take(5)
                bestEnd = slice.last().isoTime.substringAfter("T", "").take(5)
            }
        }
        return "$bestStart – $bestEnd (lowest rain & wind)"
    }
}
