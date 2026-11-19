package com.example.data.remote

import com.example.domain.calc.MeteorologyCalculator
import com.example.domain.model.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class MetNorwayProvider(private val client: NetworkClient) : WeatherProvider {

    override val providerId: ProviderId = ProviderId.MET_NORWAY

    override suspend fun fetchWeather(
        latitude: Double,
        longitude: Double,
        timezoneId: String,
        apiKey: String?
    ): ProviderResult {
        val start = System.currentTimeMillis()
        val url = "https://api.met.no/weatherapi/locationforecast/2.0/complete?" +
            "lat=${String.format(Locale.US, "%.4f", latitude)}" +
            "&lon=${String.format(Locale.US, "%.4f", longitude)}"
        val res = client.getString(url)
        val elapsed = System.currentTimeMillis() - start
        return res.fold(
            onSuccess = { body ->
                runCatching { parseMetNorwayJson(body, elapsed) }.fold(
                    onSuccess = { ProviderResult.Success(it) },
                    onFailure = { ProviderResult.Failure(providerId, "Parse error: ${it.message}", elapsed) }
                )
            },
            onFailure = { ProviderResult.Failure(providerId, it.message ?: "Network error", elapsed) }
        )
    }

    fun parseMetNorwayJson(raw: String, elapsedMs: Long): ProviderSnapshot {
        val root = JSONObject(raw)
        val props = root.getJSONObject("properties")
        val timeseries = props.getJSONArray("timeseries")
        if (timeseries.length() == 0) error("Empty timeseries")

        val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val hourlyList = mutableListOf<HourlyForecast>()
        for (i in 0 until minOf(timeseries.length(), 48)) {
            val item = timeseries.getJSONObject(i)
            val timeStr = item.getString("time")
            val epochSec = (isoFmt.parse(timeStr)?.time ?: System.currentTimeMillis()) / 1000L
            val data = item.getJSONObject("data")
            val instant = data.getJSONObject("instant").getJSONObject("details")
            val next1h = data.optJSONObject("next_1_hours") ?: data.optJSONObject("next_6_hours")
            val symbol = next1h?.optJSONObject("summary")?.optString("symbol_code", "") ?: ""
            val precip = next1h?.optJSONObject("details")?.optDoubleOrNull("precipitation_amount")
            val prob = next1h?.optJSONObject("details")?.optDoubleOrNull("probability_of_precipitation")?.toInt()
            val temp = instant.getDouble("air_temperature")
            val rh = instant.optDoubleOrNull("relative_humidity")?.toInt()
            val windMs = instant.optDoubleOrNull("wind_speed")
            val windKmh = windMs?.times(3.6)
            val gustKmh = instant.optDoubleOrNull("wind_speed_of_gust")?.times(3.6)
            val (cond, condLabel) = mapMetSymbol(symbol)

            hourlyList += HourlyForecast(
                epochSeconds = epochSec,
                isoTime = timeStr,
                temperatureC = temp,
                feelsLikeC = rh?.let { MeteorologyCalculator.calculateHeatIndexC(temp, it) }
                    ?: windKmh?.let { MeteorologyCalculator.calculateWindChillC(temp, it) } ?: temp,
                condition = cond,
                conditionLabel = condLabel,
                isDay = !symbol.contains("_night"),
                precipitationMm = precip,
                precipitationProbabilityPercent = prob,
                rainMm = precip,
                snowCm = null,
                windSpeedKmh = windKmh,
                windDirectionDeg = instant.optDoubleOrNull("wind_from_direction")?.toInt(),
                windGustKmh = gustKmh,
                humidityPercent = rh,
                pressureHpa = instant.optDoubleOrNull("air_pressure_at_sea_level"),
                cloudCoverPercent = instant.optDoubleOrNull("cloud_area_fraction")?.toInt(),
                dewPointC = instant.optDoubleOrNull("dew_point_temperature"),
                visibilityKm = null,
                uvIndex = instant.optDoubleOrNull("ultraviolet_index_clear_sky"),
                origin = DataOrigin.FORECAST
            )
        }

        val first = hourlyList.first()
        val firstDetails = timeseries.getJSONObject(0).getJSONObject("data").getJSONObject("instant").getJSONObject("details")
        val temp = first.temperatureC
        val rh = first.humidityPercent

        // Group hourly into daily buckets
        val dailyList = hourlyList.groupBy { it.isoTime.take(10) }.entries.take(7).map { (date, hours) ->
            val maxT = hours.maxOf { it.temperatureC }
            val minT = hours.minOf { it.temperatureC }
            val mid = hours[hours.size / 2]
            DailyForecast(
                epochSeconds = hours.first().epochSeconds,
                isoDate = date,
                tempMaxC = maxT,
                tempMinC = minT,
                tempAvgC = hours.map { it.temperatureC }.average(),
                condition = mid.condition,
                conditionLabel = mid.conditionLabel,
                rainProbabilityPercent = hours.mapNotNull { it.precipitationProbabilityPercent }.maxOrNull(),
                rainAmountMm = hours.mapNotNull { it.precipitationMm }.sum(),
                snowProbabilityPercent = null,
                snowAmountCm = null,
                windMaxKmh = hours.mapNotNull { it.windSpeedKmh }.maxOrNull(),
                windGustMaxKmh = hours.mapNotNull { it.windGustKmh }.maxOrNull(),
                windDirectionDeg = mid.windDirectionDeg,
                uvIndexMax = hours.mapNotNull { it.uvIndex }.maxOrNull(),
                humidityAvgPercent = hours.mapNotNull { it.humidityPercent }.takeIf { it.isNotEmpty() }?.average()?.toInt(),
                sunriseIso = null,
                sunsetIso = null,
                moonPhaseName = null,
                origin = DataOrigin.FORECAST
            )
        }

        val current = CurrentWeather(
            temperatureC = temp,
            feelsLikeC = first.feelsLikeC ?: temp,
            condition = first.condition,
            conditionLabel = first.conditionLabel,
            isDay = first.isDay,
            humidityPercent = rh,
            relativeHumidityPercent = rh,
            dewPointC = first.dewPointC,
            wetBulbTempC = rh?.let { MeteorologyCalculator.calculateWetBulbC(temp, it) },
            pressureHpa = first.pressureHpa,
            pressureTrend = MeteorologyCalculator.determinePressureTrend(hourlyList),
            windSpeedKmh = first.windSpeedKmh,
            windDirectionDeg = first.windDirectionDeg,
            windGustKmh = first.windGustKmh,
            windChillC = first.windSpeedKmh?.let { MeteorologyCalculator.calculateWindChillC(temp, it) },
            heatIndexC = rh?.let { MeteorologyCalculator.calculateHeatIndexC(temp, it) },
            visibilityKm = null,
            cloudCoverPercent = first.cloudCoverPercent,
            cloudLowPercent = firstDetails.optDoubleOrNull("cloud_area_fraction_low")?.toInt(),
            cloudMidPercent = firstDetails.optDoubleOrNull("cloud_area_fraction_medium")?.toInt(),
            cloudHighPercent = firstDetails.optDoubleOrNull("cloud_area_fraction_high")?.toInt(),
            precipitationMm = first.precipitationMm,
            rainMm = first.rainMm,
            snowCm = null,
            sleetMm = null,
            hailMm = null,
            rainProbabilityPercent = first.precipitationProbabilityPercent,
            snowProbabilityPercent = null,
            freezingLevelMeters = null,
            uvIndex = first.uvIndex,
            uvRiskLabel = MeteorologyCalculator.uvRiskLabel(first.uvIndex),
            solarRadiationWm2 = null,
            soilMoistureM3 = null,
            soilTempC = null,
            surfaceTempC = null,
            capeJoulesKg = null,
            origin = DataOrigin.MODEL_ESTIMATE
        )

        return ProviderSnapshot(
            providerId = providerId,
            fetchedEpochMs = System.currentTimeMillis(),
            current = current,
            hourly = hourlyList,
            daily = dailyList,
            responseTimeMs = elapsedMs
        )
    }

    private fun mapMetSymbol(symbol: String): Pair<WeatherCondition, String> = when {
        symbol.contains("thunder") -> WeatherCondition.THUNDERSTORM to "Thunderstorm"
        symbol.contains("heavyrain") -> WeatherCondition.HEAVY_RAIN to "Heavy Rain"
        symbol.contains("rain") -> WeatherCondition.RAIN to "Rain"
        symbol.contains("sleet") -> WeatherCondition.SLEET to "Sleet"
        symbol.contains("snow") -> WeatherCondition.SNOW to "Snow"
        symbol.contains("fog") -> WeatherCondition.FOG to "Fog"
        symbol.contains("cloudy") -> WeatherCondition.CLOUDY to "Cloudy"
        symbol.contains("partlycloudy") || symbol.contains("fair") -> WeatherCondition.PARTLY_CLOUDY to "Partly Cloudy"
        symbol.contains("clearsky") -> WeatherCondition.CLEAR to "Clear Sky"
        else -> WeatherCondition.PARTLY_CLOUDY to "Mixed Sky"
    }
}
