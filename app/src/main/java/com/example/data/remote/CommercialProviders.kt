package com.example.data.remote

import com.example.domain.calc.MeteorologyCalculator
import com.example.domain.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class WeatherApiProvider(private val client: NetworkClient) : WeatherProvider {
    override val providerId = ProviderId.WEATHER_API

    override suspend fun fetchWeather(
        latitude: Double,
        longitude: Double,
        timezoneId: String,
        apiKey: String?
    ): ProviderResult {
        if (!apiKey.isConfiguredApiKey()) {
            return ProviderResult.MissingApiKey(providerId, "WeatherAPI key not configured")
        }
        val start = System.currentTimeMillis()
        val q = "${String.format(Locale.US, "%.4f", latitude)},${String.format(Locale.US, "%.4f", longitude)}"
        val url = "https://api.weatherapi.com/v1/forecast.json?key=$apiKey&q=$q&days=7&aqi=yes&alerts=yes"
        val res = client.getString(url)
        val elapsed = System.currentTimeMillis() - start
        return res.fold(
            onSuccess = { body ->
                runCatching { parseWeatherApiJson(body, elapsed) }.fold(
                    onSuccess = { ProviderResult.Success(it) },
                    onFailure = { ProviderResult.Failure(providerId, "Parse error: ${it.message}", elapsed) }
                )
            },
            onFailure = { ProviderResult.Failure(providerId, it.message ?: "WeatherAPI error", elapsed) }
        )
    }

    fun parseWeatherApiJson(raw: String, elapsedMs: Long): ProviderSnapshot {
        val root = JSONObject(raw)
        val cur = root.getJSONObject("current")
        val temp = cur.getDouble("temp_c")
        val rh = cur.optIntOrNull("humidity")
        val condObj = cur.optJSONObject("condition")
        val condText = condObj?.optString("text", "Partly Cloudy") ?: "Partly Cloudy"
        val cond = mapConditionText(condText)
        val windKmh = cur.optDoubleOrNull("wind_kph")

        val forecastDays = root.optJSONObject("forecast")?.optJSONArray("forecastday") ?: JSONArray()
        val hourlyList = mutableListOf<HourlyForecast>()
        val dailyList = mutableListOf<DailyForecast>()

        for (d in 0 until forecastDays.length()) {
            val dayObj = forecastDays.getJSONObject(d)
            val dayInfo = dayObj.getJSONObject("day")
            val astro = dayObj.optJSONObject("astro")
            val dCondText = dayInfo.optJSONObject("condition")?.optString("text", "Clear") ?: "Clear"
            dailyList += DailyForecast(
                epochSeconds = dayObj.optLong("date_epoch", 0L),
                isoDate = dayObj.optString("date", ""),
                tempMaxC = dayInfo.getDouble("maxtemp_c"),
                tempMinC = dayInfo.getDouble("mintemp_c"),
                tempAvgC = dayInfo.optDoubleOrNull("avgtemp_c"),
                condition = mapConditionText(dCondText),
                conditionLabel = dCondText,
                rainProbabilityPercent = dayInfo.optIntOrNull("daily_chance_of_rain"),
                rainAmountMm = dayInfo.optDoubleOrNull("totalprecip_mm"),
                snowProbabilityPercent = dayInfo.optIntOrNull("daily_chance_of_snow"),
                snowAmountCm = dayInfo.optDoubleOrNull("totalsnow_cm"),
                windMaxKmh = dayInfo.optDoubleOrNull("maxwind_kph"),
                windGustMaxKmh = null,
                windDirectionDeg = null,
                uvIndexMax = dayInfo.optDoubleOrNull("uv"),
                humidityAvgPercent = dayInfo.optIntOrNull("avghumidity"),
                sunriseIso = astro?.optString("sunrise"),
                sunsetIso = astro?.optString("sunset"),
                moonPhaseName = astro?.optString("moon_phase"),
                origin = DataOrigin.FORECAST
            )

            val hours = dayObj.optJSONArray("hour") ?: JSONArray()
            for (h in 0 until hours.length()) {
                val hObj = hours.getJSONObject(h)
                val hText = hObj.optJSONObject("condition")?.optString("text", "Clear") ?: "Clear"
                hourlyList += HourlyForecast(
                    epochSeconds = hObj.optLong("time_epoch", 0L),
                    isoTime = hObj.optString("time", "").replace(" ", "T"),
                    temperatureC = hObj.getDouble("temp_c"),
                    feelsLikeC = hObj.optDoubleOrNull("feelslike_c"),
                    condition = mapConditionText(hText),
                    conditionLabel = hText,
                    isDay = hObj.optInt("is_day", 1) == 1,
                    precipitationMm = hObj.optDoubleOrNull("precip_mm"),
                    precipitationProbabilityPercent = hObj.optIntOrNull("chance_of_rain"),
                    rainMm = hObj.optDoubleOrNull("precip_mm"),
                    snowCm = hObj.optDoubleOrNull("snow_cm"),
                    windSpeedKmh = hObj.optDoubleOrNull("wind_kph"),
                    windDirectionDeg = hObj.optIntOrNull("wind_degree"),
                    windGustKmh = hObj.optDoubleOrNull("gust_kph"),
                    humidityPercent = hObj.optIntOrNull("humidity"),
                    pressureHpa = hObj.optDoubleOrNull("pressure_mb"),
                    cloudCoverPercent = hObj.optIntOrNull("cloud"),
                    dewPointC = hObj.optDoubleOrNull("dewpoint_c"),
                    visibilityKm = hObj.optDoubleOrNull("vis_km"),
                    uvIndex = hObj.optDoubleOrNull("uv")
                )
            }
        }

        val current = CurrentWeather(
            temperatureC = temp,
            feelsLikeC = cur.optDoubleOrNull("feelslike_c") ?: temp,
            condition = cond,
            conditionLabel = condText,
            isDay = cur.optInt("is_day", 1) == 1,
            humidityPercent = rh,
            relativeHumidityPercent = rh,
            dewPointC = cur.optDoubleOrNull("dewpoint_c") ?: rh?.let { MeteorologyCalculator.calculateDewPointC(temp, it) },
            wetBulbTempC = rh?.let { MeteorologyCalculator.calculateWetBulbC(temp, it) },
            pressureHpa = cur.optDoubleOrNull("pressure_mb"),
            pressureTrend = MeteorologyCalculator.determinePressureTrend(hourlyList),
            windSpeedKmh = windKmh,
            windDirectionDeg = cur.optIntOrNull("wind_degree"),
            windGustKmh = cur.optDoubleOrNull("gust_kph"),
            windChillC = cur.optDoubleOrNull("windchill_c"),
            heatIndexC = cur.optDoubleOrNull("heatindex_c"),
            visibilityKm = cur.optDoubleOrNull("vis_km"),
            cloudCoverPercent = cur.optIntOrNull("cloud"),
            cloudLowPercent = null,
            cloudMidPercent = null,
            cloudHighPercent = null,
            precipitationMm = cur.optDoubleOrNull("precip_mm"),
            rainMm = cur.optDoubleOrNull("precip_mm"),
            snowCm = null,
            sleetMm = null,
            hailMm = null,
            rainProbabilityPercent = dailyList.firstOrNull()?.rainProbabilityPercent,
            snowProbabilityPercent = dailyList.firstOrNull()?.snowProbabilityPercent,
            freezingLevelMeters = null,
            uvIndex = cur.optDoubleOrNull("uv"),
            uvRiskLabel = MeteorologyCalculator.uvRiskLabel(cur.optDoubleOrNull("uv")),
            solarRadiationWm2 = null,
            soilMoistureM3 = null,
            soilTempC = null,
            surfaceTempC = null,
            capeJoulesKg = null,
            origin = DataOrigin.OBSERVED
        )

        return ProviderSnapshot(providerId, System.currentTimeMillis(), current, hourlyList, dailyList, elapsedMs)
    }
}

class AccuWeatherProvider(private val client: NetworkClient) : WeatherProvider {
    override val providerId = ProviderId.ACCU_WEATHER

    override suspend fun fetchWeather(
        latitude: Double,
        longitude: Double,
        timezoneId: String,
        apiKey: String?
    ): ProviderResult {
        if (!apiKey.isConfiguredApiKey()) {
            return ProviderResult.MissingApiKey(providerId, "AccuWeather API key not configured")
        }
        val start = System.currentTimeMillis()
        val geoUrl = "https://dataservice.accuweather.com/locations/v1/cities/geoposition/search?apikey=$apiKey&q=${String.format(Locale.US, "%.4f,%.4f", latitude, longitude)}"
        val geoRes = client.getString(geoUrl)
        val geoBody = geoRes.getOrElse {
            return ProviderResult.Failure(providerId, it.message ?: "Location lookup failed", System.currentTimeMillis() - start)
        }
        val locationKey = runCatching { JSONObject(geoBody).getString("Key") }.getOrElse {
            return ProviderResult.Failure(providerId, "Invalid AccuWeather location response", System.currentTimeMillis() - start)
        }

        val curUrl = "https://dataservice.accuweather.com/currentconditions/v1/$locationKey?apikey=$apiKey&details=true"
        val curRes = client.getString(curUrl)
        val curBody = curRes.getOrElse {
            return ProviderResult.Failure(providerId, it.message ?: "Current conditions failed", System.currentTimeMillis() - start)
        }

        val hourlyUrl = "https://dataservice.accuweather.com/forecasts/v1/hourly/12hour/$locationKey?apikey=$apiKey&details=true&metric=true"
        val dailyUrl = "https://dataservice.accuweather.com/forecasts/v1/daily/5day/$locationKey?apikey=$apiKey&details=true&metric=true"
        val hourlyBody = client.getString(hourlyUrl).getOrNull()
        val dailyBody = client.getString(dailyUrl).getOrNull()
        val elapsed = System.currentTimeMillis() - start

        return runCatching {
            val curArr = JSONArray(curBody)
            val c = curArr.getJSONObject(0)
            val temp = c.getJSONObject("Temperature").getJSONObject("Metric").getDouble("Value")
            val feels = c.optJSONObject("RealFeelTemperature")?.optJSONObject("Metric")?.optDoubleOrNull("Value") ?: temp
            val rh = c.optIntOrNull("RelativeHumidity")
            val phrase = c.optString("WeatherText", "Partly Cloudy")
            val wind = c.optJSONObject("Wind")?.optJSONObject("Speed")?.optJSONObject("Metric")?.optDoubleOrNull("Value")
            val gust = c.optJSONObject("WindGust")?.optJSONObject("Speed")?.optJSONObject("Metric")?.optDoubleOrNull("Value")
            val pres = c.optJSONObject("Pressure")?.optJSONObject("Metric")?.optDoubleOrNull("Value")
            val vis = c.optJSONObject("Visibility")?.optJSONObject("Metric")?.optDoubleOrNull("Value")
            val uv = c.optDoubleOrNull("UVIndex")

            val hourlyList = mutableListOf<HourlyForecast>()
            if (hourlyBody != null) {
                val hArr = JSONArray(hourlyBody)
                for (i in 0 until hArr.length()) {
                    val h = hArr.getJSONObject(i)
                    val hPhrase = h.optString("IconPhrase", "Clear")
                    hourlyList += HourlyForecast(
                        epochSeconds = h.optLong("EpochDateTime", 0L),
                        isoTime = h.optString("DateTime", ""),
                        temperatureC = h.getJSONObject("Temperature").getDouble("Value"),
                        feelsLikeC = h.optJSONObject("RealFeelTemperature")?.optDoubleOrNull("Value"),
                        condition = mapConditionText(hPhrase),
                        conditionLabel = hPhrase,
                        isDay = h.optBoolean("IsDaylight", true),
                        precipitationMm = h.optJSONObject("TotalLiquid")?.optDoubleOrNull("Value"),
                        precipitationProbabilityPercent = h.optIntOrNull("PrecipitationProbability"),
                        rainMm = h.optJSONObject("Rain")?.optDoubleOrNull("Value"),
                        snowCm = h.optJSONObject("Snow")?.optDoubleOrNull("Value"),
                        windSpeedKmh = h.optJSONObject("Wind")?.optJSONObject("Speed")?.optDoubleOrNull("Value"),
                        windDirectionDeg = h.optJSONObject("Wind")?.optJSONObject("Direction")?.optIntOrNull("Degrees"),
                        windGustKmh = h.optJSONObject("WindGust")?.optJSONObject("Speed")?.optDoubleOrNull("Value"),
                        humidityPercent = h.optIntOrNull("RelativeHumidity"),
                        pressureHpa = null,
                        cloudCoverPercent = h.optIntOrNull("CloudCover"),
                        dewPointC = h.optJSONObject("DewPoint")?.optDoubleOrNull("Value"),
                        visibilityKm = h.optJSONObject("Visibility")?.optDoubleOrNull("Value"),
                        uvIndex = h.optDoubleOrNull("UVIndex")
                    )
                }
            }

            val dailyList = mutableListOf<DailyForecast>()
            if (dailyBody != null) {
                val dArr = JSONObject(dailyBody).optJSONArray("DailyForecasts") ?: JSONArray()
                for (i in 0 until dArr.length()) {
                    val d = dArr.getJSONObject(i)
                    val dayPart = d.optJSONObject("Day")
                    val dPhrase = dayPart?.optString("IconPhrase", "Clear") ?: "Clear"
                    val tMax = d.getJSONObject("Temperature").getJSONObject("Maximum").getDouble("Value")
                    val tMin = d.getJSONObject("Temperature").getJSONObject("Minimum").getDouble("Value")
                    dailyList += DailyForecast(
                        epochSeconds = d.optLong("EpochDate", 0L),
                        isoDate = d.optString("Date", "").take(10),
                        tempMaxC = tMax,
                        tempMinC = tMin,
                        tempAvgC = (tMax + tMin) / 2.0,
                        condition = mapConditionText(dPhrase),
                        conditionLabel = dPhrase,
                        rainProbabilityPercent = dayPart?.optIntOrNull("PrecipitationProbability"),
                        rainAmountMm = dayPart?.optJSONObject("TotalLiquid")?.optDoubleOrNull("Value"),
                        snowProbabilityPercent = null,
                        snowAmountCm = null,
                        windMaxKmh = dayPart?.optJSONObject("Wind")?.optJSONObject("Speed")?.optDoubleOrNull("Value"),
                        windGustMaxKmh = dayPart?.optJSONObject("WindGust")?.optJSONObject("Speed")?.optDoubleOrNull("Value"),
                        windDirectionDeg = dayPart?.optJSONObject("Wind")?.optJSONObject("Direction")?.optIntOrNull("Degrees"),
                        uvIndexMax = null,
                        humidityAvgPercent = null,
                        sunriseIso = null,
                        sunsetIso = null,
                        moonPhaseName = null
                    )
                }
            }

            val current = CurrentWeather(
                temperatureC = temp,
                feelsLikeC = feels,
                condition = mapConditionText(phrase),
                conditionLabel = phrase,
                isDay = c.optBoolean("IsDayTime", true),
                humidityPercent = rh,
                relativeHumidityPercent = rh,
                dewPointC = c.optJSONObject("DewPoint")?.optJSONObject("Metric")?.optDoubleOrNull("Value"),
                wetBulbTempC = c.optJSONObject("WetBulbTemperature")?.optJSONObject("Metric")?.optDoubleOrNull("Value"),
                pressureHpa = pres,
                pressureTrend = MeteorologyCalculator.determinePressureTrend(hourlyList),
                windSpeedKmh = wind,
                windDirectionDeg = c.optJSONObject("Wind")?.optJSONObject("Direction")?.optIntOrNull("Degrees"),
                windGustKmh = gust,
                windChillC = wind?.let { MeteorologyCalculator.calculateWindChillC(temp, it) },
                heatIndexC = rh?.let { MeteorologyCalculator.calculateHeatIndexC(temp, it) },
                visibilityKm = vis,
                cloudCoverPercent = c.optIntOrNull("CloudCover"),
                cloudLowPercent = null,
                cloudMidPercent = null,
                cloudHighPercent = null,
                precipitationMm = c.optJSONObject("Precip1hr")?.optJSONObject("Metric")?.optDoubleOrNull("Value"),
                rainMm = null,
                snowCm = null,
                sleetMm = null,
                hailMm = null,
                rainProbabilityPercent = hourlyList.firstOrNull()?.precipitationProbabilityPercent,
                snowProbabilityPercent = null,
                freezingLevelMeters = null,
                uvIndex = uv,
                uvRiskLabel = MeteorologyCalculator.uvRiskLabel(uv),
                solarRadiationWm2 = null,
                soilMoistureM3 = null,
                soilTempC = null,
                surfaceTempC = null,
                capeJoulesKg = null,
                origin = DataOrigin.OBSERVED
            )
            ProviderSnapshot(providerId, System.currentTimeMillis(), current, hourlyList, dailyList, elapsed)
        }.fold(
            onSuccess = { ProviderResult.Success(it) },
            onFailure = { ProviderResult.Failure(providerId, "Parse error: ${it.message}", elapsed) }
        )
    }
}

class TomorrowIoProvider(private val client: NetworkClient) : WeatherProvider {
    override val providerId = ProviderId.TOMORROW_IO

    override suspend fun fetchWeather(
        latitude: Double,
        longitude: Double,
        timezoneId: String,
        apiKey: String?
    ): ProviderResult {
        if (!apiKey.isConfiguredApiKey()) {
            return ProviderResult.MissingApiKey(providerId, "Tomorrow.io API key not configured")
        }
        val start = System.currentTimeMillis()
        val loc = "${String.format(Locale.US, "%.4f,%.4f", latitude, longitude)}"
        val url = "https://api.tomorrow.io/v4/weather/forecast?location=$loc&units=metric&apikey=$apiKey"
        val res = client.getString(url)
        val elapsed = System.currentTimeMillis() - start
        return res.fold(
            onSuccess = { body ->
                runCatching {
                    val timelines = JSONObject(body).getJSONObject("timelines")
                    val hourlyArr = timelines.optJSONArray("hourly") ?: JSONArray()
                    val dailyArr = timelines.optJSONArray("daily") ?: JSONArray()

                    val hourlyList = mutableListOf<HourlyForecast>()
                    for (i in 0 until minOf(hourlyArr.length(), 48)) {
                        val h = hourlyArr.getJSONObject(i)
                        val v = h.getJSONObject("values")
                        val code = v.optInt("weatherCode", 1000)
                        val (cond, label) = mapTomorrowCode(code)
                        hourlyList += HourlyForecast(
                            epochSeconds = System.currentTimeMillis() / 1000L + i * 3600L,
                            isoTime = h.optString("time", ""),
                            temperatureC = v.getDouble("temperature"),
                            feelsLikeC = v.optDoubleOrNull("temperatureApparent"),
                            condition = cond,
                            conditionLabel = label,
                            isDay = true,
                            precipitationMm = v.optDoubleOrNull("rainIntensity"),
                            precipitationProbabilityPercent = v.optIntOrNull("precipitationProbability"),
                            rainMm = v.optDoubleOrNull("rainIntensity"),
                            snowCm = v.optDoubleOrNull("snowIntensity"),
                            windSpeedKmh = v.optDoubleOrNull("windSpeed")?.times(3.6),
                            windDirectionDeg = v.optIntOrNull("windDirection"),
                            windGustKmh = v.optDoubleOrNull("windGust")?.times(3.6),
                            humidityPercent = v.optIntOrNull("humidity"),
                            pressureHpa = v.optDoubleOrNull("pressureSurfaceLevel"),
                            cloudCoverPercent = v.optIntOrNull("cloudCover"),
                            dewPointC = v.optDoubleOrNull("dewPoint"),
                            visibilityKm = v.optDoubleOrNull("visibility"),
                            uvIndex = v.optDoubleOrNull("uvIndex")
                        )
                    }

                    val dailyList = mutableListOf<DailyForecast>()
                    for (i in 0 until dailyArr.length()) {
                        val d = dailyArr.getJSONObject(i)
                        val v = d.getJSONObject("values")
                        val code = v.optInt("weatherCodeMax", 1000)
                        val (cond, label) = mapTomorrowCode(code)
                        dailyList += DailyForecast(
                            epochSeconds = System.currentTimeMillis() / 1000L + i * 86400L,
                            isoDate = d.optString("time", "").take(10),
                            tempMaxC = v.optDouble("temperatureMax", 0.0),
                            tempMinC = v.optDouble("temperatureMin", 0.0),
                            tempAvgC = v.optDoubleOrNull("temperatureAvg"),
                            condition = cond,
                            conditionLabel = label,
                            rainProbabilityPercent = v.optIntOrNull("precipitationProbabilityMax"),
                            rainAmountMm = v.optDoubleOrNull("rainAccumulationSum"),
                            snowProbabilityPercent = null,
                            snowAmountCm = v.optDoubleOrNull("snowAccumulationSum"),
                            windMaxKmh = v.optDoubleOrNull("windSpeedMax")?.times(3.6),
                            windGustMaxKmh = v.optDoubleOrNull("windGustMax")?.times(3.6),
                            windDirectionDeg = null,
                            uvIndexMax = v.optDoubleOrNull("uvIndexMax"),
                            humidityAvgPercent = v.optIntOrNull("humidityAvg"),
                            sunriseIso = v.optString("sunriseTime", "").takeIf { it.isNotBlank() },
                            sunsetIso = v.optString("sunsetTime", "").takeIf { it.isNotBlank() },
                            moonPhaseName = null
                        )
                    }

                    val first = hourlyList.first()
                    val current = CurrentWeather(
                        temperatureC = first.temperatureC,
                        feelsLikeC = first.feelsLikeC ?: first.temperatureC,
                        condition = first.condition,
                        conditionLabel = first.conditionLabel,
                        isDay = true,
                        humidityPercent = first.humidityPercent,
                        relativeHumidityPercent = first.humidityPercent,
                        dewPointC = first.dewPointC,
                        wetBulbTempC = first.humidityPercent?.let { MeteorologyCalculator.calculateWetBulbC(first.temperatureC, it) },
                        pressureHpa = first.pressureHpa,
                        pressureTrend = MeteorologyCalculator.determinePressureTrend(hourlyList),
                        windSpeedKmh = first.windSpeedKmh,
                        windDirectionDeg = first.windDirectionDeg,
                        windGustKmh = first.windGustKmh,
                        windChillC = first.windSpeedKmh?.let { MeteorologyCalculator.calculateWindChillC(first.temperatureC, it) },
                        heatIndexC = first.humidityPercent?.let { MeteorologyCalculator.calculateHeatIndexC(first.temperatureC, it) },
                        visibilityKm = first.visibilityKm,
                        cloudCoverPercent = first.cloudCoverPercent,
                        cloudLowPercent = null,
                        cloudMidPercent = null,
                        cloudHighPercent = null,
                        precipitationMm = first.precipitationMm,
                        rainMm = first.rainMm,
                        snowCm = first.snowCm,
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
                        origin = DataOrigin.FORECAST
                    )
                    ProviderSnapshot(providerId, System.currentTimeMillis(), current, hourlyList, dailyList, elapsed)
                }.fold(
                    onSuccess = { ProviderResult.Success(it) },
                    onFailure = { ProviderResult.Failure(providerId, "Parse error: ${it.message}", elapsed) }
                )
            },
            onFailure = { ProviderResult.Failure(providerId, it.message ?: "Tomorrow.io error", elapsed) }
        )
    }

    private fun mapTomorrowCode(code: Int): Pair<WeatherCondition, String> = when (code) {
        1000 -> WeatherCondition.CLEAR to "Clear"
        1100, 1101 -> WeatherCondition.PARTLY_CLOUDY to "Partly Cloudy"
        1001, 1102 -> WeatherCondition.CLOUDY to "Cloudy"
        2000, 2100 -> WeatherCondition.FOG to "Fog"
        4000 -> WeatherCondition.DRIZZLE to "Drizzle"
        4001, 4200 -> WeatherCondition.RAIN to "Rain"
        4201 -> WeatherCondition.HEAVY_RAIN to "Heavy Rain"
        5000, 5001, 5100, 5101 -> WeatherCondition.SNOW to "Snow"
        6000, 6001, 7000 -> WeatherCondition.SLEET to "Sleet / Ice"
        8000 -> WeatherCondition.THUNDERSTORM to "Thunderstorm"
        else -> WeatherCondition.PARTLY_CLOUDY to "Mixed"
    }
}

class WeatherbitProvider(private val client: NetworkClient) : WeatherProvider {
    override val providerId = ProviderId.WEATHERBIT

    override suspend fun fetchWeather(
        latitude: Double,
        longitude: Double,
        timezoneId: String,
        apiKey: String?
    ): ProviderResult {
        if (!apiKey.isConfiguredApiKey()) {
            return ProviderResult.MissingApiKey(providerId, "Weatherbit API key not configured")
        }
        val start = System.currentTimeMillis()
        val latStr = String.format(Locale.US, "%.4f", latitude)
        val lonStr = String.format(Locale.US, "%.4f", longitude)
        val curRes = client.getString("https://api.weatherbit.io/v2.0/current?lat=$latStr&lon=$lonStr&key=$apiKey")
        val dailyRes = client.getString("https://api.weatherbit.io/v2.0/forecast/daily?lat=$latStr&lon=$lonStr&days=7&key=$apiKey")
        val elapsed = System.currentTimeMillis() - start

        val curBody = curRes.getOrElse {
            return ProviderResult.Failure(providerId, it.message ?: "Weatherbit error", elapsed)
        }
        return runCatching {
            val c = JSONObject(curBody).getJSONArray("data").getJSONObject(0)
            val temp = c.getDouble("temp")
            val rh = c.optIntOrNull("rh")
            val desc = c.optJSONObject("weather")?.optString("description", "Clear") ?: "Clear"
            val windMs = c.optDoubleOrNull("wind_spd")
            val windKmh = windMs?.times(3.6)

            val dailyList = mutableListOf<DailyForecast>()
            dailyRes.getOrNull()?.let { db ->
                val dArr = JSONObject(db).optJSONArray("data") ?: JSONArray()
                for (i in 0 until dArr.length()) {
                    val d = dArr.getJSONObject(i)
                    val dDesc = d.optJSONObject("weather")?.optString("description", "Clear") ?: "Clear"
                    dailyList += DailyForecast(
                        epochSeconds = d.optLong("ts", 0L),
                        isoDate = d.optString("valid_date", ""),
                        tempMaxC = d.getDouble("max_temp"),
                        tempMinC = d.getDouble("min_temp"),
                        tempAvgC = d.optDoubleOrNull("temp"),
                        condition = mapConditionText(dDesc),
                        conditionLabel = dDesc,
                        rainProbabilityPercent = d.optIntOrNull("pop"),
                        rainAmountMm = d.optDoubleOrNull("precip"),
                        snowProbabilityPercent = null,
                        snowAmountCm = d.optDoubleOrNull("snow"),
                        windMaxKmh = d.optDoubleOrNull("wind_spd")?.times(3.6),
                        windGustMaxKmh = d.optDoubleOrNull("wind_gust_spd")?.times(3.6),
                        windDirectionDeg = d.optIntOrNull("wind_dir"),
                        uvIndexMax = d.optDoubleOrNull("uv"),
                        humidityAvgPercent = d.optIntOrNull("rh"),
                        sunriseIso = null,
                        sunsetIso = null,
                        moonPhaseName = null
                    )
                }
            }

            val current = CurrentWeather(
                temperatureC = temp,
                feelsLikeC = c.optDoubleOrNull("app_temp") ?: temp,
                condition = mapConditionText(desc),
                conditionLabel = desc,
                isDay = c.optString("pod", "d") == "d",
                humidityPercent = rh,
                relativeHumidityPercent = rh,
                dewPointC = c.optDoubleOrNull("dewpt"),
                wetBulbTempC = rh?.let { MeteorologyCalculator.calculateWetBulbC(temp, it) },
                pressureHpa = c.optDoubleOrNull("slp") ?: c.optDoubleOrNull("pres"),
                pressureTrend = null,
                windSpeedKmh = windKmh,
                windDirectionDeg = c.optIntOrNull("wind_dir"),
                windGustKmh = c.optDoubleOrNull("gust")?.times(3.6),
                windChillC = windKmh?.let { MeteorologyCalculator.calculateWindChillC(temp, it) },
                heatIndexC = rh?.let { MeteorologyCalculator.calculateHeatIndexC(temp, it) },
                visibilityKm = c.optDoubleOrNull("vis"),
                cloudCoverPercent = c.optIntOrNull("clouds"),
                cloudLowPercent = null,
                cloudMidPercent = null,
                cloudHighPercent = null,
                precipitationMm = c.optDoubleOrNull("precip"),
                rainMm = c.optDoubleOrNull("precip"),
                snowCm = c.optDoubleOrNull("snow"),
                sleetMm = null,
                hailMm = null,
                rainProbabilityPercent = dailyList.firstOrNull()?.rainProbabilityPercent,
                snowProbabilityPercent = null,
                freezingLevelMeters = null,
                uvIndex = c.optDoubleOrNull("uv"),
                uvRiskLabel = MeteorologyCalculator.uvRiskLabel(c.optDoubleOrNull("uv")),
                solarRadiationWm2 = c.optDoubleOrNull("solar_rad"),
                soilMoistureM3 = null,
                soilTempC = null,
                surfaceTempC = null,
                capeJoulesKg = null,
                origin = DataOrigin.OBSERVED
            )
            ProviderSnapshot(providerId, System.currentTimeMillis(), current, emptyList(), dailyList, elapsed)
        }.fold(
            onSuccess = { ProviderResult.Success(it) },
            onFailure = { ProviderResult.Failure(providerId, "Parse error: ${it.message}", elapsed) }
        )
    }
}

class GoogleWeatherProvider(private val client: NetworkClient) : WeatherProvider {
    override val providerId = ProviderId.GOOGLE_WEATHER

    override suspend fun fetchWeather(
        latitude: Double,
        longitude: Double,
        timezoneId: String,
        apiKey: String?
    ): ProviderResult {
        if (!apiKey.isConfiguredApiKey()) {
            return ProviderResult.MissingApiKey(
                providerId,
                "Google Maps Weather API key not configured (no fabricated data)"
            )
        }
        val start = System.currentTimeMillis()
        val latStr = String.format(Locale.US, "%.4f", latitude)
        val lonStr = String.format(Locale.US, "%.4f", longitude)
        val url = "https://weather.googleapis.com/v1/currentConditions:lookup?key=$apiKey&location.latitude=$latStr&location.longitude=$lonStr"
        val res = client.getString(url)
        val elapsed = System.currentTimeMillis() - start
        return res.fold(
            onSuccess = { body ->
                runCatching {
                    val root = JSONObject(body)
                    val temp = root.getJSONObject("temperature").getDouble("degrees")
                    val feels = root.optJSONObject("feelsLikeTemperature")?.optDoubleOrNull("degrees") ?: temp
                    val rh = root.optIntOrNull("relativeHumidity")
                    val desc = root.optJSONObject("weatherCondition")?.optJSONObject("description")?.optString("text", "Clear") ?: "Clear"
                    val windKmh = root.optJSONObject("wind")?.optJSONObject("speed")?.optDoubleOrNull("value")
                    val gustKmh = root.optJSONObject("wind")?.optJSONObject("gust")?.optDoubleOrNull("value")
                    val pres = root.optJSONObject("airPressure")?.optDoubleOrNull("meanSeaLevelMillibars")
                    val vis = root.optJSONObject("visibility")?.optDoubleOrNull("distance")
                    val uv = root.optDoubleOrNull("uvIndex")

                    val current = CurrentWeather(
                        temperatureC = temp,
                        feelsLikeC = feels,
                        condition = mapConditionText(desc),
                        conditionLabel = desc,
                        isDay = root.optBoolean("isDaytime", true),
                        humidityPercent = rh,
                        relativeHumidityPercent = rh,
                        dewPointC = root.optJSONObject("dewPoint")?.optDoubleOrNull("degrees"),
                        wetBulbTempC = rh?.let { MeteorologyCalculator.calculateWetBulbC(temp, it) },
                        pressureHpa = pres,
                        pressureTrend = null,
                        windSpeedKmh = windKmh,
                        windDirectionDeg = root.optJSONObject("wind")?.optJSONObject("direction")?.optIntOrNull("degrees"),
                        windGustKmh = gustKmh,
                        windChillC = windKmh?.let { MeteorologyCalculator.calculateWindChillC(temp, it) },
                        heatIndexC = rh?.let { MeteorologyCalculator.calculateHeatIndexC(temp, it) },
                        visibilityKm = vis,
                        cloudCoverPercent = root.optIntOrNull("cloudCover"),
                        cloudLowPercent = null,
                        cloudMidPercent = null,
                        cloudHighPercent = null,
                        precipitationMm = root.optJSONObject("precipitation")?.optJSONObject("qpf")?.optDoubleOrNull("quantity"),
                        rainMm = null,
                        snowCm = null,
                        sleetMm = null,
                        hailMm = null,
                        rainProbabilityPercent = root.optJSONObject("precipitation")?.optJSONObject("probability")?.optIntOrNull("percent"),
                        snowProbabilityPercent = null,
                        freezingLevelMeters = null,
                        uvIndex = uv,
                        uvRiskLabel = MeteorologyCalculator.uvRiskLabel(uv),
                        solarRadiationWm2 = null,
                        soilMoistureM3 = null,
                        soilTempC = null,
                        surfaceTempC = null,
                        capeJoulesKg = null,
                        origin = DataOrigin.OBSERVED
                    )
                    ProviderSnapshot(providerId, System.currentTimeMillis(), current, emptyList(), emptyList(), elapsed)
                }.fold(
                    onSuccess = { ProviderResult.Success(it) },
                    onFailure = { ProviderResult.Failure(providerId, "Parse error: ${it.message}", elapsed) }
                )
            },
            onFailure = { ProviderResult.Failure(providerId, it.message ?: "Google Weather API error", elapsed) }
        )
    }
}

fun mapConditionText(text: String): WeatherCondition {
    val t = text.lowercase()
    return when {
        t.contains("thunder") || t.contains("storm") -> WeatherCondition.THUNDERSTORM
        t.contains("heavy rain") || t.contains("torrential") -> WeatherCondition.HEAVY_RAIN
        t.contains("drizzle") -> WeatherCondition.DRIZZLE
        t.contains("rain") || t.contains("shower") -> WeatherCondition.RAIN
        t.contains("sleet") || t.contains("freezing") || t.contains("ice") -> WeatherCondition.SLEET
        t.contains("snow") || t.contains("blizzard") -> WeatherCondition.SNOW
        t.contains("fog") || t.contains("mist") || t.contains("haze") -> WeatherCondition.FOG
        t.contains("overcast") || t.contains("cloudy") -> WeatherCondition.CLOUDY
        t.contains("partly") || t.contains("scattered") || t.contains("fair") -> WeatherCondition.PARTLY_CLOUDY
        t.contains("wind") -> WeatherCondition.WINDY
        else -> WeatherCondition.CLEAR
    }
}
