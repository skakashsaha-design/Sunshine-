package com.example.data.remote

import com.example.domain.calc.AqiCalculator
import com.example.domain.calc.MeteorologyCalculator
import com.example.domain.model.*
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale

class OpenMeteoProvider(private val client: NetworkClient) : WeatherProvider {

    override val providerId: ProviderId = ProviderId.OPEN_METEO

    override suspend fun fetchWeather(
        latitude: Double,
        longitude: Double,
        timezoneId: String,
        apiKey: String?
    ): ProviderResult {
        val start = System.currentTimeMillis()
        val url = buildString {
            append("https://api.open-meteo.com/v1/forecast?")
            append("latitude=").append(String.format(Locale.US, "%.4f", latitude))
            append("&longitude=").append(String.format(Locale.US, "%.4f", longitude))
            append("&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,rain,showers,snowfall,weather_code,cloud_cover,pressure_msl,surface_pressure,wind_speed_10m,wind_direction_10m,wind_gusts_10m,dew_point_2m,wet_bulb_temperature_2m,visibility,uv_index,shortwave_radiation,soil_temperature_0cm,soil_moisture_0_to_1cm,cape,freezing_level_height")
            append("&hourly=temperature_2m,relative_humidity_2m,dew_point_2m,apparent_temperature,precipitation_probability,precipitation,rain,snowfall,weather_code,pressure_msl,cloud_cover,cloud_cover_low,cloud_cover_mid,cloud_cover_high,visibility,wind_speed_10m,wind_direction_10m,wind_gusts_10m,uv_index,is_day")
            append("&daily=weather_code,temperature_2m_max,temperature_2m_min,temperature_2m_mean,sunrise,sunset,uv_index_max,precipitation_sum,rain_sum,snowfall_sum,precipitation_probability_max,wind_speed_10m_max,wind_gusts_10m_max,wind_direction_10m_dominant,relative_humidity_2m_mean")
            append("&forecast_days=10&forecast_hours=72&timeformat=unixtime&timezone=auto")
        }

        val res = client.getString(url)
        val elapsed = System.currentTimeMillis() - start
        return res.fold(
            onSuccess = { body ->
                runCatching {
                    parseWeatherJson(body, elapsed)
                }.fold(
                    onSuccess = { ProviderResult.Success(it) },
                    onFailure = { ProviderResult.Failure(providerId, "Parse error: ${it.message}", elapsed) }
                )
            },
            onFailure = { ProviderResult.Failure(providerId, it.message ?: "Network error", elapsed) }
        )
    }

    fun parseWeatherJson(raw: String, elapsedMs: Long): ProviderSnapshot {
        val root = JSONObject(raw)
        val cur = root.getJSONObject("current")
        val utcOffsetSec = root.optInt("utc_offset_seconds", 0)

        val temp = cur.getDouble("temperature_2m")
        val rh = cur.optIntOrNull("relative_humidity_2m")
        val feels = cur.optDoubleOrNull("apparent_temperature") ?: temp
        val wmo = cur.optInt("weather_code", 0)
        val isDay = cur.optInt("is_day", 1) == 1
        val windSpeed = cur.optDoubleOrNull("wind_speed_10m")
        val dew = cur.optDoubleOrNull("dew_point_2m") ?: rh?.let { MeteorologyCalculator.calculateDewPointC(temp, it) }
        val wetBulb = cur.optDoubleOrNull("wet_bulb_temperature_2m") ?: rh?.let { MeteorologyCalculator.calculateWetBulbC(temp, it) }
        val visMeters = cur.optDoubleOrNull("visibility")
        val uv = cur.optDoubleOrNull("uv_index")

        val hourlyObj = root.optJSONObject("hourly")
        val hourlyList = mutableListOf<HourlyForecast>()
        if (hourlyObj != null) {
            val times = hourlyObj.getJSONArray("time")
            val temps = hourlyObj.getJSONArray("temperature_2m")
            val feelsArr = hourlyObj.optJSONArray("apparent_temperature")
            val codes = hourlyObj.optJSONArray("weather_code")
            val isDayArr = hourlyObj.optJSONArray("is_day")
            val precipArr = hourlyObj.optJSONArray("precipitation")
            val probArr = hourlyObj.optJSONArray("precipitation_probability")
            val rainArr = hourlyObj.optJSONArray("rain")
            val snowArr = hourlyObj.optJSONArray("snowfall")
            val windArr = hourlyObj.optJSONArray("wind_speed_10m")
            val dirArr = hourlyObj.optJSONArray("wind_direction_10m")
            val gustArr = hourlyObj.optJSONArray("wind_gusts_10m")
            val rhArr = hourlyObj.optJSONArray("relative_humidity_2m")
            val presArr = hourlyObj.optJSONArray("pressure_msl")
            val cloudArr = hourlyObj.optJSONArray("cloud_cover")
            val dewArr = hourlyObj.optJSONArray("dew_point_2m")
            val visArr = hourlyObj.optJSONArray("visibility")
            val uvArr = hourlyObj.optJSONArray("uv_index")

            val count = minOf(times.length(), 72)
            for (i in 0 until count) {
                val ep = times.getLong(i)
                val hCode = codes?.optInt(i, 0) ?: 0
                val (cond, condLabel) = mapWmoCode(hCode)
                hourlyList += HourlyForecast(
                    epochSeconds = ep,
                    isoTime = epochToLocalIso(ep, utcOffsetSec),
                    temperatureC = temps.optDouble(i, temp),
                    feelsLikeC = feelsArr?.optDoubleOrNull(i),
                    condition = cond,
                    conditionLabel = condLabel,
                    isDay = isDayArr?.optInt(i, 1) == 1,
                    precipitationMm = precipArr?.optDoubleOrNull(i),
                    precipitationProbabilityPercent = probArr?.optIntOrNull(i),
                    rainMm = rainArr?.optDoubleOrNull(i),
                    snowCm = snowArr?.optDoubleOrNull(i),
                    windSpeedKmh = windArr?.optDoubleOrNull(i),
                    windDirectionDeg = dirArr?.optIntOrNull(i),
                    windGustKmh = gustArr?.optDoubleOrNull(i),
                    humidityPercent = rhArr?.optIntOrNull(i),
                    pressureHpa = presArr?.optDoubleOrNull(i),
                    cloudCoverPercent = cloudArr?.optIntOrNull(i),
                    dewPointC = dewArr?.optDoubleOrNull(i),
                    visibilityKm = visArr?.optDoubleOrNull(i)?.div(1000.0),
                    uvIndex = uvArr?.optDoubleOrNull(i),
                    origin = DataOrigin.FORECAST
                )
            }
        }

        val dailyObj = root.optJSONObject("daily")
        val dailyList = mutableListOf<DailyForecast>()
        if (dailyObj != null) {
            val times = dailyObj.getJSONArray("time")
            val maxArr = dailyObj.getJSONArray("temperature_2m_max")
            val minArr = dailyObj.getJSONArray("temperature_2m_min")
            val meanArr = dailyObj.optJSONArray("temperature_2m_mean")
            val codeArr = dailyObj.optJSONArray("weather_code")
            val probArr = dailyObj.optJSONArray("precipitation_probability_max")
            val rainArr = dailyObj.optJSONArray("rain_sum")
            val snowArr = dailyObj.optJSONArray("snowfall_sum")
            val windArr = dailyObj.optJSONArray("wind_speed_10m_max")
            val gustArr = dailyObj.optJSONArray("wind_gusts_10m_max")
            val dirArr = dailyObj.optJSONArray("wind_direction_10m_dominant")
            val uvArr = dailyObj.optJSONArray("uv_index_max")
            val rhMeanArr = dailyObj.optJSONArray("relative_humidity_2m_mean")
            val riseArr = dailyObj.optJSONArray("sunrise")
            val setArr = dailyObj.optJSONArray("sunset")

            for (i in 0 until times.length()) {
                val ep = times.getLong(i)
                val dCode = codeArr?.optInt(i, 0) ?: 0
                val (cond, condLabel) = mapWmoCode(dCode)
                dailyList += DailyForecast(
                    epochSeconds = ep,
                    isoDate = epochToLocalDate(ep, utcOffsetSec),
                    tempMaxC = maxArr.optDouble(i, temp),
                    tempMinC = minArr.optDouble(i, temp),
                    tempAvgC = meanArr?.optDoubleOrNull(i),
                    condition = cond,
                    conditionLabel = condLabel,
                    rainProbabilityPercent = probArr?.optIntOrNull(i),
                    rainAmountMm = rainArr?.optDoubleOrNull(i),
                    snowProbabilityPercent = null,
                    snowAmountCm = snowArr?.optDoubleOrNull(i),
                    windMaxKmh = windArr?.optDoubleOrNull(i),
                    windGustMaxKmh = gustArr?.optDoubleOrNull(i),
                    windDirectionDeg = dirArr?.optIntOrNull(i),
                    uvIndexMax = uvArr?.optDoubleOrNull(i),
                    humidityAvgPercent = rhMeanArr?.optIntOrNull(i),
                    sunriseIso = riseArr?.optLongOrNull(i)?.let { epochToLocalTime(it, utcOffsetSec) },
                    sunsetIso = setArr?.optLongOrNull(i)?.let { epochToLocalTime(it, utcOffsetSec) },
                    moonPhaseName = null,
                    origin = DataOrigin.FORECAST
                )
            }
        }

        val cloudLow = hourlyObj?.optJSONArray("cloud_cover_low")?.optIntOrNull(0)
        val cloudMid = hourlyObj?.optJSONArray("cloud_cover_mid")?.optIntOrNull(0)
        val cloudHigh = hourlyObj?.optJSONArray("cloud_cover_high")?.optIntOrNull(0)

        val (cond, condLabel) = mapWmoCode(wmo)
        val current = CurrentWeather(
            temperatureC = temp,
            feelsLikeC = feels,
            condition = cond,
            conditionLabel = condLabel,
            isDay = isDay,
            humidityPercent = rh,
            relativeHumidityPercent = rh,
            dewPointC = dew,
            wetBulbTempC = wetBulb,
            pressureHpa = cur.optDoubleOrNull("pressure_msl") ?: cur.optDoubleOrNull("surface_pressure"),
            pressureTrend = MeteorologyCalculator.determinePressureTrend(hourlyList),
            windSpeedKmh = windSpeed,
            windDirectionDeg = cur.optIntOrNull("wind_direction_10m"),
            windGustKmh = cur.optDoubleOrNull("wind_gusts_10m"),
            windChillC = windSpeed?.let { MeteorologyCalculator.calculateWindChillC(temp, it) },
            heatIndexC = rh?.let { MeteorologyCalculator.calculateHeatIndexC(temp, it) },
            visibilityKm = visMeters?.div(1000.0),
            cloudCoverPercent = cur.optIntOrNull("cloud_cover"),
            cloudLowPercent = cloudLow,
            cloudMidPercent = cloudMid,
            cloudHighPercent = cloudHigh,
            precipitationMm = cur.optDoubleOrNull("precipitation"),
            rainMm = cur.optDoubleOrNull("rain"),
            snowCm = cur.optDoubleOrNull("snowfall"),
            sleetMm = null,
            hailMm = null,
            rainProbabilityPercent = hourlyList.firstOrNull()?.precipitationProbabilityPercent,
            snowProbabilityPercent = null,
            freezingLevelMeters = cur.optDoubleOrNull("freezing_level_height"),
            uvIndex = uv,
            uvRiskLabel = MeteorologyCalculator.uvRiskLabel(uv),
            solarRadiationWm2 = cur.optDoubleOrNull("shortwave_radiation"),
            soilMoistureM3 = cur.optDoubleOrNull("soil_moisture_0_to_1cm"),
            soilTempC = cur.optDoubleOrNull("soil_temperature_0cm"),
            surfaceTempC = cur.optDoubleOrNull("soil_temperature_0cm"),
            capeJoulesKg = cur.optDoubleOrNull("cape"),
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

    suspend fun fetchAirQuality(latitude: Double, longitude: Double): Result<AirQualityData> {
        val url = "https://air-quality-api.open-meteo.com/v1/air-quality?" +
            "latitude=${String.format(Locale.US, "%.4f", latitude)}" +
            "&longitude=${String.format(Locale.US, "%.4f", longitude)}" +
            "&current=us_aqi,european_aqi,pm10,pm2_5,carbon_monoxide,nitrogen_dioxide,sulphur_dioxide,ozone,dust,aerosol_optical_depth" +
            "&hourly=us_aqi,european_aqi,pm10,pm2_5,ozone&forecast_days=2&timeformat=unixtime&timezone=auto"
        return client.getString(url).mapCatching { parseAirQualityJson(it) }
    }

    fun parseAirQualityJson(raw: String): AirQualityData {
        val root = JSONObject(raw)
        val utcOffsetSec = root.optInt("utc_offset_seconds", 0)
        val cur = root.getJSONObject("current")
        val pm25 = cur.optDoubleOrNull("pm2_5")
        val pm10 = cur.optDoubleOrNull("pm10")
        val o3 = cur.optDoubleOrNull("ozone")
        val no2 = cur.optDoubleOrNull("nitrogen_dioxide")
        val so2 = cur.optDoubleOrNull("sulphur_dioxide")
        val co = cur.optDoubleOrNull("carbon_monoxide")
        val dust = cur.optDoubleOrNull("dust")
        val aod = cur.optDoubleOrNull("aerosol_optical_depth")

        val usAqi = cur.optIntOrNull("us_aqi") ?: pm25?.let { AqiCalculator.pm25ToUsAqi(it) }
        val euAqi = cur.optIntOrNull("european_aqi")
        val category = AqiCalculator.categoryFromUsAqi(usAqi)

        val hourlyList = mutableListOf<HourlyAqi>()
        val hObj = root.optJSONObject("hourly")
        if (hObj != null) {
            val times = hObj.getJSONArray("time")
            val usArr = hObj.optJSONArray("us_aqi")
            val euArr = hObj.optJSONArray("european_aqi")
            val pm25Arr = hObj.optJSONArray("pm2_5")
            val pm10Arr = hObj.optJSONArray("pm10")
            val o3Arr = hObj.optJSONArray("ozone")
            for (i in 0 until minOf(times.length(), 48)) {
                val ep = times.getLong(i)
                hourlyList += HourlyAqi(
                    epochSeconds = ep,
                    isoTime = epochToLocalIso(ep, utcOffsetSec),
                    usAqi = usArr?.optIntOrNull(i),
                    europeanAqi = euArr?.optIntOrNull(i),
                    pm25 = pm25Arr?.optDoubleOrNull(i),
                    pm10 = pm10Arr?.optDoubleOrNull(i),
                    ozone = o3Arr?.optDoubleOrNull(i)
                )
            }
        }

        val (bestWindow, worstWindow) = AqiCalculator.analyzeWindows(hourlyList)
        val first6 = hourlyList.take(6).mapNotNull { it.usAqi }
        val trend = if (first6.size >= 4) {
            val diff = first6.last() - first6.first()
            when {
                diff >= 15 -> "Worsening over next 6h"
                diff <= -15 -> "Improving over next 6h"
                else -> "Steady over next 6h"
            }
        } else "Stable"

        return AirQualityData(
            usAqi = usAqi,
            europeanAqi = euAqi,
            category = category,
            sourceLabel = "Open-Meteo CAMS Atmospheric Model",
            origin = DataOrigin.MODEL_ESTIMATE,
            pm25 = pm25,
            pm10 = pm10,
            o3 = o3,
            no2 = no2,
            so2 = so2,
            co = co,
            dust = dust,
            aerosolOpticalDepth = aod,
            pollutants = AqiCalculator.buildPollutants(pm25, pm10, o3, no2, so2, co, dust, aod),
            hourlyAqi = hourlyList,
            trendLabel = trend,
            bestOutdoorWindow = bestWindow,
            worstOutdoorWindow = worstWindow,
            healthGuidance = AqiCalculator.healthGuidance(category),
            fetchedEpochMs = System.currentTimeMillis()
        )
    }

    suspend fun fetchFloodOutlook(latitude: Double, longitude: Double): Result<FloodOutlook> {
        val url = "https://flood-api.open-meteo.com/v1/flood?" +
            "latitude=${String.format(Locale.US, "%.4f", latitude)}" +
            "&longitude=${String.format(Locale.US, "%.4f", longitude)}" +
            "&daily=river_discharge,river_discharge_mean,river_discharge_max&forecast_days=14"
        return client.getString(url).mapCatching { body ->
            val root = JSONObject(body)
            val daily = root.getJSONObject("daily")
            val dates = daily.getJSONArray("time")
            val disArr = daily.optJSONArray("river_discharge")
            val meanArr = daily.optJSONArray("river_discharge_mean")
            val list = mutableListOf<Pair<String, Double>>()
            for (i in 0 until dates.length()) {
                val valD = disArr?.optDoubleOrNull(i)
                if (valD != null) list += dates.getString(i) to valD
            }
            val currentDis = list.firstOrNull()?.second
            val max7d = list.take(7).maxOfOrNull { it.second }
            val histMean = meanArr?.optDoubleOrNull(0)
            val ratio = if (max7d != null && histMean != null && histMean > 0.5) max7d / histMean else 1.0
            val risk = when {
                ratio >= 2.5 || (max7d ?: 0.0) > 5000.0 -> "Elevated Flood Risk"
                ratio >= 1.5 -> "Moderate High-Water Watch"
                else -> "Normal River Flow"
            }
            FloodOutlook(
                riverDischargeM3s = currentDis,
                maxDischarge7dM3s = max7d,
                meanDischargeHistoricalM3s = histMean,
                riskLevel = risk,
                origin = DataOrigin.MODEL_ESTIMATE,
                summary = if (currentDis != null) {
                    "GloFAS hydrological model discharge: ${String.format(Locale.US, "%.1f", currentDis)} m³/s (7-day peak ${String.format(Locale.US, "%.1f", max7d ?: currentDis)} m³/s)."
                } else {
                    "No major river basin gauge grid matched these exact coordinates."
                },
                dailyDischargeForecast = list
            )
        }
    }

    suspend fun searchCities(query: String): Result<List<SearchLocationResult>> {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=12&language=en&format=json"
        return client.getString(url).mapCatching { body ->
            val root = JSONObject(body)
            val arr = root.optJSONArray("results") ?: return@mapCatching emptyList()
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        SearchLocationResult(
                            name = o.optString("name", "Unknown"),
                            region = o.optString("admin1", ""),
                            country = o.optString("country", ""),
                            countryCode = o.optString("country_code", ""),
                            latitude = o.getDouble("latitude"),
                            longitude = o.getDouble("longitude"),
                            timezoneId = o.optString("timezone", "UTC")
                        )
                    )
                }
            }
        }
    }

    companion object {
        fun mapWmoCode(code: Int): Pair<WeatherCondition, String> = when (code) {
            0 -> WeatherCondition.CLEAR to "Clear Sky"
            1, 2 -> WeatherCondition.PARTLY_CLOUDY to "Partly Cloudy"
            3 -> WeatherCondition.CLOUDY to "Overcast"
            45, 48 -> WeatherCondition.FOG to "Fog"
            51, 53, 55, 56, 57 -> WeatherCondition.DRIZZLE to "Drizzle"
            61, 63, 80, 81 -> WeatherCondition.RAIN to "Rain"
            65, 82 -> WeatherCondition.HEAVY_RAIN to "Heavy Rain"
            66, 67 -> WeatherCondition.SLEET to "Freezing Rain"
            71, 73, 75, 77, 85, 86 -> WeatherCondition.SNOW to "Snow"
            95, 96, 99 -> WeatherCondition.THUNDERSTORM to "Thunderstorm"
            else -> WeatherCondition.PARTLY_CLOUDY to "Mixed Sky"
        }
    }
}
