package com.example.domain.calc

import com.example.domain.model.*
import kotlin.math.*

object MeteorologyCalculator {

    fun calculateDewPointC(tempC: Double, humidityPercent: Int): Double {
        val rh = humidityPercent.coerceIn(1, 100).toDouble()
        val a = 17.625
        val b = 243.04
        val alpha = ln(rh / 100.0) + (a * tempC) / (b + tempC)
        return (b * alpha) / (a - alpha)
    }

    /**
     * Stull (2011) empirical wet-bulb temperature equation (accurate for RH 5-99%, T -20..50C).
     */
    fun calculateWetBulbC(tempC: Double, humidityPercent: Int): Double {
        val rh = humidityPercent.coerceIn(1, 100).toDouble()
        return tempC * atan(0.151977 * sqrt(rh + 8.313659)) +
            atan(tempC + rh) -
            atan(rh - 1.676331) +
            0.00391838 * rh.pow(1.5) * atan(0.023101 * rh) -
            4.686035
    }

    /**
     * Rothfusz Heat Index (valid when T >= 27C and RH >= 40%).
     */
    fun calculateHeatIndexC(tempC: Double, humidityPercent: Int): Double? {
        if (tempC < 26.7 || humidityPercent < 40) return null
        val tF = tempC * 9.0 / 5.0 + 32.0
        val r = humidityPercent.toDouble()
        val hiF = -42.379 + 2.04901523 * tF + 10.14333127 * r -
            0.22475541 * tF * r - 0.00683783 * tF * tF -
            0.05481717 * r * r + 0.00122874 * tF * tF * r +
            0.00085282 * tF * r * r - 0.00000199 * tF * tF * r * r
        return (hiF - 32.0) * 5.0 / 9.0
    }

    /**
     * NWS Wind Chill (valid when T <= 10C and wind > 4.8 km/h).
     */
    fun calculateWindChillC(tempC: Double, windKmh: Double): Double? {
        if (tempC > 10.0 || windKmh <= 4.8) return null
        val vPow = windKmh.pow(0.16)
        return 13.12 + 0.6215 * tempC - 11.37 * vPow + 0.3965 * tempC * vPow
    }

    fun determinePressureTrend(hourly: List<HourlyForecast>): PressureTrend? {
        val pressures = hourly.mapNotNull { it.pressureHpa }
        if (pressures.size < 4) return null
        val delta = pressures[3] - pressures[0]
        return when {
            delta >= 2.5 -> PressureTrend.RISING_RAPIDLY
            delta >= 0.8 -> PressureTrend.RISING
            delta <= -2.5 -> PressureTrend.FALLING_RAPIDLY
            delta <= -0.8 -> PressureTrend.FALLING
            else -> PressureTrend.STEADY
        }
    }

    fun uvRiskLabel(uv: Double?): String? {
        if (uv == null) return null
        return when {
            uv < 3.0 -> "Low"
            uv < 6.0 -> "Moderate"
            uv < 8.0 -> "High"
            uv < 11.0 -> "Very High"
            else -> "Extreme"
        }
    }

    fun buildInsights(
        current: CurrentWeather,
        hourly: List<HourlyForecast>,
        comparisons: List<ProviderComparisonEntry>
    ): WeatherInsights {
        val temp = current.temperatureC
        val feels = current.feelsLikeC
        val rh = current.humidityPercent
        val wind = current.windSpeedKmh
        val dew = current.dewPointC ?: rh?.let { calculateDewPointC(temp, it) }

        val feelsExplanation = when {
            feels - temp >= 2.5 -> "High humidity and low airflow make it feel ${(feels - temp).roundToInt()}°C warmer than air temperature."
            temp - feels >= 2.5 -> "Breeze and wind chill make it feel ${(temp - feels).roundToInt()}°C cooler than air temperature."
            else -> "Feels-like temperature closely matches the actual air temperature."
        }

        val dewComfort = when {
            dew == null -> "Dew point unavailable"
            dew < 10.0 -> "Dry & crisp (${dew.roundToInt()}°C)"
            dew < 16.0 -> "Comfortable (${dew.roundToInt()}°C)"
            dew < 20.0 -> "Slightly muggy (${dew.roundToInt()}°C)"
            dew < 24.0 -> "Humid & sticky (${dew.roundToInt()}°C)"
            else -> "Oppressive moisture (${dew.roundToInt()}°C)"
        }

        val humComfort = when {
            rh == null -> "Humidity unavailable"
            rh < 30 -> "Dry air ($rh%)"
            rh <= 60 -> "Balanced ($rh%)"
            rh <= 80 -> "Humid ($rh%)"
            else -> "Very saturated ($rh%)"
        }

        val trend = current.pressureTrend ?: determinePressureTrend(hourly)
        val pressureAnalysis = when (trend) {
            PressureTrend.RISING_RAPIDLY -> "Rising rapidly — clearing skies or post-frontal ridge."
            PressureTrend.RISING -> "Rising steadily — stabilizing atmospheric conditions."
            PressureTrend.STEADY -> "Steady pressure — minimal short-term synoptic change."
            PressureTrend.FALLING -> "Falling pressure — approaching trough or rain system."
            PressureTrend.FALLING_RAPIDLY -> "Falling rapidly — deepening low-pressure or storm risk."
            null -> "Pressure trend unavailable."
        }

        val next12 = hourly.take(12)
        val maxRainProb = next12.mapNotNull { it.precipitationProbabilityPercent }.maxOrNull()
        val totalRainMm = next12.mapNotNull { it.precipitationMm }.sum()
        val rainTrend = when {
            maxRainProb == null && totalRainMm <= 0.1 -> "Dry outlook in the next 12 hours."
            (maxRainProb ?: 0) >= 70 || totalRainMm >= 10.0 -> "High rain risk (${maxRainProb ?: "--"}% peak, ~${String.format(java.util.Locale.US, "%.1f", totalRainMm)} mm expected)."
            (maxRainProb ?: 0) >= 35 || totalRainMm >= 1.0 -> "Scattered rain possible (${maxRainProb ?: "--"}% peak probability)."
            else -> "Low precipitation likelihood over the next 12 hours."
        }

        val hasStormHour = next12.any { it.condition == WeatherCondition.THUNDERSTORM }
        val maxGust = next12.mapNotNull { it.windGustKmh ?: it.windSpeedKmh }.maxOrNull() ?: wind ?: 0.0
        val stormTrend = when {
            hasStormHour || (current.capeJoulesKg ?: 0.0) > 1200.0 -> "Convective thunderstorm potential detected in upcoming hours."
            maxGust >= 50.0 -> "Strong wind gusts up to ${maxGust.roundToInt()} km/h expected."
            else -> "No convective storm signatures detected."
        }

        val visRisk = when {
            current.visibilityKm == null -> "Visibility not reported"
            current.visibilityKm < 1.0 -> "Dense fog / poor visibility (${String.format(java.util.Locale.US, "%.1f", current.visibilityKm)} km)"
            current.visibilityKm < 5.0 -> "Reduced visibility (${String.format(java.util.Locale.US, "%.1f", current.visibilityKm)} km)"
            else -> "Clear visibility (${String.format(java.util.Locale.US, "%.1f", current.visibilityKm)} km)"
        }

        val uvLabel = current.uvRiskLabel ?: uvRiskLabel(current.uvIndex) ?: "Not reported"

        var comfort = 88
        if (temp > 33 || temp < 8) comfort -= 22 else if (temp > 29 || temp < 14) comfort -= 10
        if ((rh ?: 50) > 82) comfort -= 14
        if ((wind ?: 0.0) > 35.0) comfort -= 15
        if ((maxRainProb ?: 0) > 60) comfort -= 18
        if (current.condition == WeatherCondition.THUNDERSTORM || current.condition == WeatherCondition.HEAVY_RAIN) comfort -= 25
        comfort = comfort.coerceIn(10, 100)
        val comfortLabel = when {
            comfort >= 80 -> "Ideal outdoors"
            comfort >= 60 -> "Fair conditions"
            comfort >= 40 -> "Use caution"
            else -> "Poor outdoor conditions"
        }

        val validTemps = comparisons.mapNotNull { it.temperatureC }
        val confidence = if (validTemps.size >= 2) {
            val spread = (validTemps.max() - validTemps.min()).coerceAtLeast(0.0)
            (100 - (spread * 10).roundToInt()).coerceIn(45, 99)
        } else null

        val cycloneRisk = when {
            maxGust >= 89.0 || (wind ?: 0.0) >= 89.0 -> "Cyclone/Storm-force winds (${maxGust.roundToInt()} km/h) — seek shelter."
            maxGust >= 62.0 || (wind ?: 0.0) >= 62.0 -> "Gale-force / Tropical storm wind threshold (${maxGust.roundToInt()} km/h)."
            else -> null
        }

        val rain24h = hourly.take(24).mapNotNull { it.precipitationMm }.sum()
        val landslideRisk = if (rain24h >= 75.0) {
            "Elevated landslide & slope saturation risk (${rain24h.roundToInt()} mm / 24h model estimate)."
        } else null

        return WeatherInsights(
            feelsLikeExplanation = feelsExplanation,
            dewPointComfort = dewComfort,
            humidityComfort = humComfort,
            pressureAnalysis = pressureAnalysis,
            rainTrend = rainTrend,
            stormTrend = stormTrend,
            visibilityRisk = visRisk,
            uvExposureLevel = uvLabel,
            outdoorComfortScore = comfort,
            outdoorComfortLabel = comfortLabel,
            weatherConfidencePercent = confidence,
            cycloneWindRisk = cycloneRisk,
            landslideRisk = landslideRisk
        )
    }

    fun generateHumanSummary(
        current: CurrentWeather,
        hourly: List<HourlyForecast>,
        aqi: AirQualityData?,
        unit: TemperatureUnit
    ): List<String> {
        val lines = mutableListOf<String>()
        val tempStr = GeoUtils.formatTemperature(current.temperatureC, unit)
        val feelsStr = GeoUtils.formatTemperature(current.feelsLikeC, unit)
        lines += "Currently ${current.conditionLabel.lowercase()} at $tempStr (feels like $feelsStr)."

        val nextRain = hourly.take(12).firstOrNull {
            (it.precipitationProbabilityPercent ?: 0) >= 50 || (it.precipitationMm ?: 0.0) >= 0.5
        }
        if (nextRain != null) {
            val timePart = nextRain.isoTime.substringAfter("T", "").take(5)
            val prob = nextRain.precipitationProbabilityPercent
            lines += if (prob != null) {
                "Rain probability rises to $prob% around $timePart."
            } else {
                "Precipitation expected around $timePart."
            }
        } else if (hourly.isNotEmpty()) {
            lines += "Dry conditions expected over the next 12 hours."
        }

        val peakWind = hourly.take(12).mapNotNull { it.windGustKmh ?: it.windSpeedKmh }.maxOrNull()
        if (peakWind != null && peakWind >= 35.0) {
            lines += "Gusty winds up to ${peakWind.roundToInt()} km/h expected."
        }

        if (aqi?.usAqi != null && aqi.usAqi >= 101) {
            lines += "Air quality is ${aqi.category.label.lowercase()} (US AQI ${aqi.usAqi})."
        }

        return lines
    }

    fun generateDailyAdvisor(
        current: CurrentWeather,
        hourly: List<HourlyForecast>,
        aqi: AirQualityData?,
        alerts: List<WeatherAlert>,
        disasters: List<DisasterEvent>
    ): List<String> {
        val advice = mutableListOf<String>()

        if (alerts.isNotEmpty()) {
            val top = alerts.maxByOrNull { it.severity.rank }!!
            advice += "Alert Priority: ${top.title} (${top.sourceName}). Follow local authority guidance."
        }

        val maxRainProb = hourly.take(12).mapNotNull { it.precipitationProbabilityPercent }.maxOrNull() ?: 0
        if (maxRainProb >= 50 || current.condition == WeatherCondition.RAIN || current.condition == WeatherCondition.HEAVY_RAIN) {
            advice += "Carry waterproof protection — rain probability peaks at $maxRainProb% today."
        }

        if ((current.uvIndex ?: 0.0) >= 6.0) {
            advice += "High UV (${current.uvIndex?.roundToInt()}): wear SPF 30+ sunscreen and sunglasses between 10 AM and 3 PM."
        }

        if ((current.heatIndexC ?: current.temperatureC) >= 35.0) {
            advice += "Heat stress caution: stay hydrated and avoid strenuous midday outdoor exertion."
        }

        val aqiVal = aqi?.usAqi
        if (aqiVal != null) {
            when {
                aqiVal >= 151 -> advice += "Air Quality Alert (AQI $aqiVal): wear a fitted N95 mask outdoors and keep windows closed."
                aqiVal >= 101 -> advice += "Sensitive groups should limit prolonged outdoor exercise (AQI $aqiVal)."
                aqiVal <= 50 -> advice += "Clean air today (AQI $aqiVal) — great window for outdoor activity."
            }
        }

        val nearbyDisaster = disasters.firstOrNull { (it.distanceKm ?: Double.MAX_VALUE) <= 300.0 }
        if (nearbyDisaster != null) {
            advice += "Regional Hazard Watch: ${nearbyDisaster.title} (~${nearbyDisaster.distanceKm?.roundToInt()} km away)."
        }

        if (advice.isEmpty()) {
            advice += "Favorable conditions today. No immediate severe weather or hazard advisories."
        }
        return advice
    }

    fun buildTodayAiAnalysis(
        current: CurrentWeather,
        hourly: List<HourlyForecast>,
        daily: List<DailyForecast>,
        aqi: AirQualityData?,
        unit: TemperatureUnit,
        aiEnhancedSummary: String? = null
    ): TodayAiAnalysis {
        val insights = buildInsights(current, hourly, emptyList())
        val todayHours = hourly.take(24)

        val thermalScore = when {
            current.feelsLikeC in 18.0..27.0 -> 94
            current.feelsLikeC in 14.0..31.0 -> 78
            current.feelsLikeC in 8.0..35.0 -> 58
            else -> 32
        }
        val maxRainProb = todayHours.mapNotNull { it.precipitationProbabilityPercent }.maxOrNull() ?: 0
        val rainScore = (100 - maxRainProb).coerceIn(10, 98)
        val peakGust = todayHours.mapNotNull { it.windGustKmh ?: it.windSpeedKmh }.maxOrNull() ?: current.windSpeedKmh ?: 8.0
        val windScore = (100 - (peakGust * 1.4).roundToInt()).coerceIn(15, 98)
        val aqiVal = aqi?.usAqi ?: 50
        val airScore = (100 - (aqiVal / 3)).coerceIn(10, 98)

        val overallScore = ((thermalScore * 0.30) + (rainScore * 0.30) + (airScore * 0.25) + (windScore * 0.15)).roundToInt().coerceIn(10, 99)

        val phases = listOf(
            summarizePhase("Morning", "06:00–11:59", todayHours, 6..11, unit),
            summarizePhase("Afternoon", "12:00–16:59", todayHours, 12..16, unit),
            summarizePhase("Evening", "17:00–20:59", todayHours, 17..20, unit),
            summarizePhase("Night", "21:00–05:59", todayHours, listOf(21, 22, 23, 0, 1, 2, 3, 4, 5).toSet(), unit)
        )

        val triggers = mutableListOf<String>()
        val rainStart = todayHours.firstOrNull { (it.precipitationProbabilityPercent ?: 0) >= 50 || (it.precipitationMm ?: 0.0) >= 0.4 }
        if (rainStart != null) {
            triggers += "Rain window starts around ${rainStart.isoTime.substringAfter("T", "").take(5)} (${rainStart.precipitationProbabilityPercent ?: "--"}%)"
        } else {
            triggers += "Dry pattern over the next 24h"
        }
        val warmestHour = todayHours.maxByOrNull { it.temperatureC }
        if (warmestHour != null) {
            triggers += "Peak warmth ${GeoUtils.formatTemperature(warmestHour.temperatureC, unit)} around ${warmestHour.isoTime.substringAfter("T", "").take(5)}"
        }
        val gustiestHour = todayHours.maxByOrNull { it.windGustKmh ?: it.windSpeedKmh ?: 0.0 }
        if (gustiestHour != null) {
            val g = gustiestHour.windGustKmh ?: gustiestHour.windSpeedKmh ?: 0.0
            if (g >= 25.0) {
                triggers += "Strongest gusts (${g.roundToInt()} km/h) at ${gustiestHour.isoTime.substringAfter("T", "").take(5)}"
            }
        }
        aqi?.bestOutdoorWindow?.let {
            triggers += "Cleanest air window: ${it.timeRange} (AQI ${it.avgAqi})"
        }

        val wardrobeTips = mutableListOf<String>()
        if (maxRainProb >= 45 || current.condition == WeatherCondition.RAIN || current.condition == WeatherCondition.DRIZZLE) {
            wardrobeTips += "Carry compact umbrella or waterproof shell; allow extra commute braking distance."
        }
        if (current.feelsLikeC >= 30.0) {
            wardrobeTips += "Breathable cotton/linen layers & electrolyte hydration recommended."
        } else if (current.temperatureC <= 16.0) {
            wardrobeTips += "Wear a wind-resistant mid-layer, especially after sunset."
        }
        if ((current.uvIndex ?: 0.0) >= 5.5 || (daily.firstOrNull()?.uvIndexMax ?: 0.0) >= 6.0) {
            wardrobeTips += "Apply SPF 30+ sunscreen & UV sunglasses during midday hours."
        }
        if (aqiVal >= 120) {
            wardrobeTips += "High particulate load (AQI $aqiVal): use N95 mask for outdoor travel."
        }
        if (wardrobeTips.isEmpty()) {
            wardrobeTips += "Light comfortable attire suits today's mild atmospheric profile."
        }

        val capeNote = current.capeJoulesKg?.let { "CAPE ${it.roundToInt()} J/kg" } ?: "CAPE N/A"
        val wetBulbNote = current.wetBulbTempC?.let { "Wet-bulb ${GeoUtils.formatTemperature(it, unit)}" } ?: ""
        val atmosphericSignals = "${insights.pressureAnalysis} · $capeNote${if (wetBulbNote.isNotBlank()) " · $wetBulbNote" else ""}"

        val headline = when {
            overallScore >= 80 -> "Crisp & Favorable Day Ahead"
            overallScore >= 60 -> "Manageable Weather — Watch Peak Windows"
            overallScore >= 40 -> "Mixed Weather Friction Expected"
            else -> "Challenging Weather & Air Conditions"
        }

        val execSummary = aiEnhancedSummary ?: buildString {
            append("Currently ${current.conditionLabel.lowercase()} at ${GeoUtils.formatTemperature(current.temperatureC, unit)} ")
            append("(feels like ${GeoUtils.formatTemperature(current.feelsLikeC, unit)}, ${insights.dewPointComfort.lowercase()}). ")
            if (maxRainProb >= 50) {
                append("Rain peaks near $maxRainProb%; ")
            } else {
                append("Low rain chance ($maxRainProb%); ")
            }
            if (aqi?.usAqi != null) {
                append("air quality is ${aqi.category.label.lowercase()} (US AQI ${aqi.usAqi}).")
            } else {
                append("winds peak around ${peakGust.roundToInt()} km/h.")
            }
        }

        return TodayAiAnalysis(
            headline = headline,
            executiveSummary = execSummary,
            comfortScore = overallScore,
            comfortBreakdown = listOf(
                ComfortBreakdownItem("Thermal", thermalScore),
                ComfortBreakdownItem("Dryness", rainScore),
                ComfortBreakdownItem("Air Quality", airScore),
                ComfortBreakdownItem("Wind Calm", windScore)
            ),
            phases = phases,
            keyTriggers = triggers,
            wardrobeAndHealthTips = wardrobeTips,
            atmosphericSignals = atmosphericSignals,
            isAiEnhanced = aiEnhancedSummary != null
        )
    }

    private fun summarizePhase(
        name: String,
        windowLabel: String,
        hours: List<HourlyForecast>,
        hourRange: Iterable<Int>,
        unit: TemperatureUnit
    ): DayPhaseForecast {
        val allowed = hourRange.toSet()
        val matching = hours.filter { h ->
            val hr = h.isoTime.substringAfter("T", "").take(2).toIntOrNull()
            hr != null && hr in allowed
        }
        if (matching.isEmpty()) {
            return DayPhaseForecast(name, windowLabel, "--", "No data", 0, "Telemetry pending")
        }
        val minT = matching.minOf { it.temperatureC }
        val maxT = matching.maxOf { it.temperatureC }
        val rainPeak = matching.mapNotNull { it.precipitationProbabilityPercent }.maxOrNull() ?: 0
        val rep = matching[matching.size / 2]
        val tempRange = if (minT.roundToInt() == maxT.roundToInt()) {
            GeoUtils.formatTemperature(maxT, unit, false)
        } else {
            "${GeoUtils.formatTemperature(minT, unit, false)}–${GeoUtils.formatTemperature(maxT, unit, false)}"
        }
        return DayPhaseForecast(
            phaseName = name,
            timeWindow = windowLabel,
            tempRangeLabel = tempRange,
            conditionLabel = rep.conditionLabel,
            rainRiskPercent = rainPeak,
            summary = "${rep.conditionLabel} · Rain $rainPeak%"
        )
    }
}
