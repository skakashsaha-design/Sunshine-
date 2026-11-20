package com.example.data.remote

import com.example.domain.calc.AqiCalculator
import com.example.domain.calc.MeteorologyCalculator
import com.example.domain.model.*
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.Locale

interface OpenMeteoWeatherApi {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = DEFAULT_CURRENT_VARS,
        @Query("hourly") hourly: String = DEFAULT_HOURLY_VARS,
        @Query("daily") daily: String = DEFAULT_DAILY_VARS,
        @Query("forecast_days") forecastDays: Int = 10,
        @Query("forecast_hours") forecastHours: Int = 72,
        @Query("timeformat") timeFormat: String = "unixtime",
        @Query("timezone") timezone: String = "auto"
    ): OpenMeteoForecastDto

    companion object {
        const val DEFAULT_CURRENT_VARS =
            "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,rain,showers,snowfall,weather_code,cloud_cover,pressure_msl,surface_pressure,wind_speed_10m,wind_direction_10m,wind_gusts_10m,dew_point_2m,wet_bulb_temperature_2m,visibility,uv_index,shortwave_radiation,soil_temperature_0cm,soil_moisture_0_to_1cm,cape,freezing_level_height"
        const val DEFAULT_HOURLY_VARS =
            "temperature_2m,relative_humidity_2m,dew_point_2m,apparent_temperature,precipitation_probability,precipitation,rain,snowfall,weather_code,pressure_msl,cloud_cover,cloud_cover_low,cloud_cover_mid,cloud_cover_high,visibility,wind_speed_10m,wind_direction_10m,wind_gusts_10m,uv_index,is_day"
        const val DEFAULT_DAILY_VARS =
            "weather_code,temperature_2m_max,temperature_2m_min,temperature_2m_mean,sunrise,sunset,uv_index_max,precipitation_sum,rain_sum,snowfall_sum,precipitation_probability_max,wind_speed_10m_max,wind_gusts_10m_max,wind_direction_10m_dominant,relative_humidity_2m_mean"
    }
}

interface OpenMeteoAirQualityApi {
    @GET("v1/air-quality")
    suspend fun getAirQuality(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "us_aqi,european_aqi,pm10,pm2_5,carbon_monoxide,nitrogen_dioxide,sulphur_dioxide,ozone,dust,aerosol_optical_depth",
        @Query("hourly") hourly: String = "us_aqi,european_aqi,pm10,pm2_5,ozone",
        @Query("forecast_days") forecastDays: Int = 2,
        @Query("timeformat") timeFormat: String = "unixtime",
        @Query("timezone") timezone: String = "auto"
    ): OpenMeteoAirQualityDto
}

interface OpenMeteoFloodApi {
    @GET("v1/flood")
    suspend fun getFloodForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("daily") daily: String = "river_discharge,river_discharge_mean,river_discharge_max",
        @Query("forecast_days") forecastDays: Int = 14
    ): OpenMeteoFloodDto
}

interface OpenMeteoGeocodingApi {
    @GET("v1/search")
    suspend fun searchCities(
        @Query("name") name: String,
        @Query("count") count: Int = 12,
        @Query("language") language: String = "en",
        @Query("format") format: String = "json"
    ): OpenMeteoGeocodingDto
}

@JsonClass(generateAdapter = true)
data class OpenMeteoForecastDto(
    @Json(name = "utc_offset_seconds") val utcOffsetSeconds: Int? = 0,
    @Json(name = "current") val current: OpenMeteoCurrentDto? = null,
    @Json(name = "hourly") val hourly: OpenMeteoHourlyDto? = null,
    @Json(name = "daily") val daily: OpenMeteoDailyDto? = null
)

@JsonClass(generateAdapter = true)
data class OpenMeteoCurrentDto(
    @Json(name = "temperature_2m") val temperature2m: Double? = null,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: Int? = null,
    @Json(name = "apparent_temperature") val apparentTemperature: Double? = null,
    @Json(name = "is_day") val isDay: Int? = 1,
    @Json(name = "precipitation") val precipitation: Double? = null,
    @Json(name = "rain") val rain: Double? = null,
    @Json(name = "showers") val showers: Double? = null,
    @Json(name = "snowfall") val snowfall: Double? = null,
    @Json(name = "weather_code") val weatherCode: Int? = 0,
    @Json(name = "cloud_cover") val cloudCover: Int? = null,
    @Json(name = "pressure_msl") val pressureMsl: Double? = null,
    @Json(name = "surface_pressure") val surfacePressure: Double? = null,
    @Json(name = "wind_speed_10m") val windSpeed10m: Double? = null,
    @Json(name = "wind_direction_10m") val windDirection10m: Int? = null,
    @Json(name = "wind_gusts_10m") val windGusts10m: Double? = null,
    @Json(name = "dew_point_2m") val dewPoint2m: Double? = null,
    @Json(name = "wet_bulb_temperature_2m") val wetBulbTemperature2m: Double? = null,
    @Json(name = "visibility") val visibility: Double? = null,
    @Json(name = "uv_index") val uvIndex: Double? = null,
    @Json(name = "shortwave_radiation") val shortwaveRadiation: Double? = null,
    @Json(name = "soil_temperature_0cm") val soilTemperature0cm: Double? = null,
    @Json(name = "soil_moisture_0_to_1cm") val soilMoisture0To1cm: Double? = null,
    @Json(name = "cape") val cape: Double? = null,
    @Json(name = "freezing_level_height") val freezingLevelHeight: Double? = null
)

@JsonClass(generateAdapter = true)
data class OpenMeteoHourlyDto(
    @Json(name = "time") val time: List<Long?>? = null,
    @Json(name = "temperature_2m") val temperature2m: List<Double?>? = null,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: List<Int?>? = null,
    @Json(name = "dew_point_2m") val dewPoint2m: List<Double?>? = null,
    @Json(name = "apparent_temperature") val apparentTemperature: List<Double?>? = null,
    @Json(name = "precipitation_probability") val precipitationProbability: List<Int?>? = null,
    @Json(name = "precipitation") val precipitation: List<Double?>? = null,
    @Json(name = "rain") val rain: List<Double?>? = null,
    @Json(name = "snowfall") val snowfall: List<Double?>? = null,
    @Json(name = "weather_code") val weatherCode: List<Int?>? = null,
    @Json(name = "pressure_msl") val pressureMsl: List<Double?>? = null,
    @Json(name = "cloud_cover") val cloudCover: List<Int?>? = null,
    @Json(name = "cloud_cover_low") val cloudCoverLow: List<Int?>? = null,
    @Json(name = "cloud_cover_mid") val cloudCoverMid: List<Int?>? = null,
    @Json(name = "cloud_cover_high") val cloudCoverHigh: List<Int?>? = null,
    @Json(name = "visibility") val visibility: List<Double?>? = null,
    @Json(name = "wind_speed_10m") val windSpeed10m: List<Double?>? = null,
    @Json(name = "wind_direction_10m") val windDirection10m: List<Int?>? = null,
    @Json(name = "wind_gusts_10m") val windGusts10m: List<Double?>? = null,
    @Json(name = "uv_index") val uvIndex: List<Double?>? = null,
    @Json(name = "is_day") val isDay: List<Int?>? = null
)

@JsonClass(generateAdapter = true)
data class OpenMeteoDailyDto(
    @Json(name = "time") val time: List<Long?>? = null,
    @Json(name = "weather_code") val weatherCode: List<Int?>? = null,
    @Json(name = "temperature_2m_max") val temperature2mMax: List<Double?>? = null,
    @Json(name = "temperature_2m_min") val temperature2mMin: List<Double?>? = null,
    @Json(name = "temperature_2m_mean") val temperature2mMean: List<Double?>? = null,
    @Json(name = "sunrise") val sunrise: List<Long?>? = null,
    @Json(name = "sunset") val sunset: List<Long?>? = null,
    @Json(name = "uv_index_max") val uvIndexMax: List<Double?>? = null,
    @Json(name = "precipitation_sum") val precipitationSum: List<Double?>? = null,
    @Json(name = "rain_sum") val rainSum: List<Double?>? = null,
    @Json(name = "snowfall_sum") val snowfallSum: List<Double?>? = null,
    @Json(name = "precipitation_probability_max") val precipitationProbabilityMax: List<Int?>? = null,
    @Json(name = "wind_speed_10m_max") val windSpeed10mMax: List<Double?>? = null,
    @Json(name = "wind_gusts_10m_max") val windGusts10mMax: List<Double?>? = null,
    @Json(name = "wind_direction_10m_dominant") val windDirection10mDominant: List<Int?>? = null,
    @Json(name = "relative_humidity_2m_mean") val relativeHumidity2mMean: List<Int?>? = null
)

@JsonClass(generateAdapter = true)
data class OpenMeteoAirQualityDto(
    @Json(name = "utc_offset_seconds") val utcOffsetSeconds: Int? = 0,
    @Json(name = "current") val current: OpenMeteoAqiCurrentDto? = null,
    @Json(name = "hourly") val hourly: OpenMeteoAqiHourlyDto? = null
)

@JsonClass(generateAdapter = true)
data class OpenMeteoAqiCurrentDto(
    @Json(name = "us_aqi") val usAqi: Int? = null,
    @Json(name = "european_aqi") val europeanAqi: Int? = null,
    @Json(name = "pm10") val pm10: Double? = null,
    @Json(name = "pm2_5") val pm25: Double? = null,
    @Json(name = "carbon_monoxide") val carbonMonoxide: Double? = null,
    @Json(name = "nitrogen_dioxide") val nitrogenDioxide: Double? = null,
    @Json(name = "sulphur_dioxide") val sulphurDioxide: Double? = null,
    @Json(name = "ozone") val ozone: Double? = null,
    @Json(name = "dust") val dust: Double? = null,
    @Json(name = "aerosol_optical_depth") val aerosolOpticalDepth: Double? = null
)

@JsonClass(generateAdapter = true)
data class OpenMeteoAqiHourlyDto(
    @Json(name = "time") val time: List<Long?>? = null,
    @Json(name = "us_aqi") val usAqi: List<Int?>? = null,
    @Json(name = "european_aqi") val europeanAqi: List<Int?>? = null,
    @Json(name = "pm2_5") val pm25: List<Double?>? = null,
    @Json(name = "pm10") val pm10: List<Double?>? = null,
    @Json(name = "ozone") val ozone: List<Double?>? = null
)

@JsonClass(generateAdapter = true)
data class OpenMeteoFloodDto(
    @Json(name = "daily") val daily: OpenMeteoFloodDailyDto? = null
)

@JsonClass(generateAdapter = true)
data class OpenMeteoFloodDailyDto(
    @Json(name = "time") val time: List<String?>? = null,
    @Json(name = "river_discharge") val riverDischarge: List<Double?>? = null,
    @Json(name = "river_discharge_mean") val riverDischargeMean: List<Double?>? = null
)

@JsonClass(generateAdapter = true)
data class OpenMeteoGeocodingDto(
    @Json(name = "results") val results: List<OpenMeteoGeoItemDto>? = null
)

@JsonClass(generateAdapter = true)
data class OpenMeteoGeoItemDto(
    @Json(name = "name") val name: String? = null,
    @Json(name = "admin1") val admin1: String? = null,
    @Json(name = "country") val country: String? = null,
    @Json(name = "country_code") val countryCode: String? = null,
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null,
    @Json(name = "timezone") val timezone: String? = null
)

fun OpenMeteoForecastDto.toProviderSnapshot(elapsedMs: Long): ProviderSnapshot {
    val cur = requireNotNull(current) { "Open-Meteo response missing current object" }
    val temp = requireNotNull(cur.temperature2m) { "Open-Meteo current temperature missing" }
    val utcOffsetSec = utcOffsetSeconds ?: 0
    val rh = cur.relativeHumidity2m
    val feels = cur.apparentTemperature ?: temp
    val wmo = cur.weatherCode ?: 0
    val isDay = (cur.isDay ?: 1) == 1
    val windSpeed = cur.windSpeed10m
    val dew = cur.dewPoint2m ?: rh?.let { MeteorologyCalculator.calculateDewPointC(temp, it) }
    val wetBulb = cur.wetBulbTemperature2m ?: rh?.let { MeteorologyCalculator.calculateWetBulbC(temp, it) }
    val uv = cur.uvIndex

    val hourlyList = mutableListOf<HourlyForecast>()
    val hDto = hourly
    if (hDto?.time != null) {
        val count = minOf(hDto.time.size, 72)
        for (i in 0 until count) {
            val ep = hDto.time.getOrNull(i) ?: continue
            val hCode = hDto.weatherCode?.getOrNull(i) ?: 0
            val (cond, condLabel) = OpenMeteoProvider.mapWmoCode(hCode)
            hourlyList += HourlyForecast(
                epochSeconds = ep,
                isoTime = epochToLocalIso(ep, utcOffsetSec),
                temperatureC = hDto.temperature2m?.getOrNull(i) ?: temp,
                feelsLikeC = hDto.apparentTemperature?.getOrNull(i),
                condition = cond,
                conditionLabel = condLabel,
                isDay = (hDto.isDay?.getOrNull(i) ?: 1) == 1,
                precipitationMm = hDto.precipitation?.getOrNull(i),
                precipitationProbabilityPercent = hDto.precipitationProbability?.getOrNull(i),
                rainMm = hDto.rain?.getOrNull(i),
                snowCm = hDto.snowfall?.getOrNull(i),
                windSpeedKmh = hDto.windSpeed10m?.getOrNull(i),
                windDirectionDeg = hDto.windDirection10m?.getOrNull(i),
                windGustKmh = hDto.windGusts10m?.getOrNull(i),
                humidityPercent = hDto.relativeHumidity2m?.getOrNull(i),
                pressureHpa = hDto.pressureMsl?.getOrNull(i),
                cloudCoverPercent = hDto.cloudCover?.getOrNull(i),
                dewPointC = hDto.dewPoint2m?.getOrNull(i),
                visibilityKm = hDto.visibility?.getOrNull(i)?.div(1000.0),
                uvIndex = hDto.uvIndex?.getOrNull(i),
                origin = DataOrigin.FORECAST
            )
        }
    }

    val dailyList = mutableListOf<DailyForecast>()
    val dDto = daily
    if (dDto?.time != null) {
        for (i in dDto.time.indices) {
            val ep = dDto.time.getOrNull(i) ?: continue
            val dCode = dDto.weatherCode?.getOrNull(i) ?: 0
            val (cond, condLabel) = OpenMeteoProvider.mapWmoCode(dCode)
            dailyList += DailyForecast(
                epochSeconds = ep,
                isoDate = epochToLocalDate(ep, utcOffsetSec),
                tempMaxC = dDto.temperature2mMax?.getOrNull(i) ?: temp,
                tempMinC = dDto.temperature2mMin?.getOrNull(i) ?: temp,
                tempAvgC = dDto.temperature2mMean?.getOrNull(i),
                condition = cond,
                conditionLabel = condLabel,
                rainProbabilityPercent = dDto.precipitationProbabilityMax?.getOrNull(i),
                rainAmountMm = dDto.rainSum?.getOrNull(i) ?: dDto.precipitationSum?.getOrNull(i),
                snowProbabilityPercent = null,
                snowAmountCm = dDto.snowfallSum?.getOrNull(i),
                windMaxKmh = dDto.windSpeed10mMax?.getOrNull(i),
                windGustMaxKmh = dDto.windGusts10mMax?.getOrNull(i),
                windDirectionDeg = dDto.windDirection10mDominant?.getOrNull(i),
                uvIndexMax = dDto.uvIndexMax?.getOrNull(i),
                humidityAvgPercent = dDto.relativeHumidity2mMean?.getOrNull(i),
                sunriseIso = dDto.sunrise?.getOrNull(i)?.let { epochToLocalTime(it, utcOffsetSec) },
                sunsetIso = dDto.sunset?.getOrNull(i)?.let { epochToLocalTime(it, utcOffsetSec) },
                moonPhaseName = null,
                origin = DataOrigin.FORECAST
            )
        }
    }

    val (cond, condLabel) = OpenMeteoProvider.mapWmoCode(wmo)
    val currentModel = CurrentWeather(
        temperatureC = temp,
        feelsLikeC = feels,
        condition = cond,
        conditionLabel = condLabel,
        isDay = isDay,
        humidityPercent = rh,
        relativeHumidityPercent = rh,
        dewPointC = dew,
        wetBulbTempC = wetBulb,
        pressureHpa = cur.pressureMsl ?: cur.surfacePressure,
        pressureTrend = MeteorologyCalculator.determinePressureTrend(hourlyList),
        windSpeedKmh = windSpeed,
        windDirectionDeg = cur.windDirection10m,
        windGustKmh = cur.windGusts10m,
        windChillC = windSpeed?.let { MeteorologyCalculator.calculateWindChillC(temp, it) },
        heatIndexC = rh?.let { MeteorologyCalculator.calculateHeatIndexC(temp, it) },
        visibilityKm = cur.visibility?.div(1000.0),
        cloudCoverPercent = cur.cloudCover,
        cloudLowPercent = hDto?.cloudCoverLow?.getOrNull(0),
        cloudMidPercent = hDto?.cloudCoverMid?.getOrNull(0),
        cloudHighPercent = hDto?.cloudCoverHigh?.getOrNull(0),
        precipitationMm = cur.precipitation,
        rainMm = cur.rain,
        snowCm = cur.snowfall,
        sleetMm = null,
        hailMm = null,
        rainProbabilityPercent = hourlyList.firstOrNull()?.precipitationProbabilityPercent,
        snowProbabilityPercent = null,
        freezingLevelMeters = cur.freezingLevelHeight,
        uvIndex = uv,
        uvRiskLabel = MeteorologyCalculator.uvRiskLabel(uv),
        solarRadiationWm2 = cur.shortwaveRadiation,
        soilMoistureM3 = cur.soilMoisture0To1cm,
        soilTempC = cur.soilTemperature0cm,
        surfaceTempC = cur.soilTemperature0cm,
        capeJoulesKg = cur.cape,
        origin = DataOrigin.MODEL_ESTIMATE
    )

    return ProviderSnapshot(
        providerId = ProviderId.OPEN_METEO,
        fetchedEpochMs = System.currentTimeMillis(),
        current = currentModel,
        hourly = hourlyList,
        daily = dailyList,
        responseTimeMs = elapsedMs
    )
}

fun OpenMeteoAirQualityDto.toAirQualityData(): AirQualityData {
    val cur = requireNotNull(current) { "Missing AQI current block" }
    val utcOffsetSec = utcOffsetSeconds ?: 0
    val usAqi = cur.usAqi ?: cur.pm25?.let { AqiCalculator.pm25ToUsAqi(it) }
    val category = AqiCalculator.categoryFromUsAqi(usAqi)

    val hourlyList = mutableListOf<HourlyAqi>()
    val h = hourly
    if (h?.time != null) {
        for (i in 0 until minOf(h.time.size, 48)) {
            val ep = h.time.getOrNull(i) ?: continue
            hourlyList += HourlyAqi(
                epochSeconds = ep,
                isoTime = epochToLocalIso(ep, utcOffsetSec),
                usAqi = h.usAqi?.getOrNull(i),
                europeanAqi = h.europeanAqi?.getOrNull(i),
                pm25 = h.pm25?.getOrNull(i),
                pm10 = h.pm10?.getOrNull(i),
                ozone = h.ozone?.getOrNull(i)
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
        europeanAqi = cur.europeanAqi,
        category = category,
        sourceLabel = "Open-Meteo CAMS Atmospheric Model",
        origin = DataOrigin.MODEL_ESTIMATE,
        pm25 = cur.pm25,
        pm10 = cur.pm10,
        o3 = cur.ozone,
        no2 = cur.nitrogenDioxide,
        so2 = cur.sulphurDioxide,
        co = cur.carbonMonoxide,
        dust = cur.dust,
        aerosolOpticalDepth = cur.aerosolOpticalDepth,
        pollutants = AqiCalculator.buildPollutants(
            cur.pm25, cur.pm10, cur.ozone, cur.nitrogenDioxide,
            cur.sulphurDioxide, cur.carbonMonoxide, cur.dust, cur.aerosolOpticalDepth
        ),
        hourlyAqi = hourlyList,
        trendLabel = trend,
        bestOutdoorWindow = bestWindow,
        worstOutdoorWindow = worstWindow,
        healthGuidance = AqiCalculator.healthGuidance(category),
        fetchedEpochMs = System.currentTimeMillis()
    )
}

fun OpenMeteoFloodDto.toFloodOutlook(): FloodOutlook {
    val d = daily
    val dates = d?.time.orEmpty()
    val discharges = d?.riverDischarge.orEmpty()
    val means = d?.riverDischargeMean.orEmpty()
    val list = mutableListOf<Pair<String, Double>>()
    for (i in dates.indices) {
        val date = dates.getOrNull(i) ?: continue
        val dis = discharges.getOrNull(i) ?: continue
        list += date to dis
    }
    val currentDis = list.firstOrNull()?.second
    val max7d = list.take(7).maxOfOrNull { it.second }
    val histMean = means.firstOrNull()
    val ratio = if (max7d != null && histMean != null && histMean > 0.5) max7d / histMean else 1.0
    val risk = when {
        ratio >= 2.5 || (max7d ?: 0.0) > 5000.0 -> "Elevated Flood Risk"
        ratio >= 1.5 -> "Moderate High-Water Watch"
        else -> "Normal River Flow"
    }
    return FloodOutlook(
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

fun OpenMeteoGeocodingDto.toSearchResults(): List<SearchLocationResult> =
    results.orEmpty().mapNotNull { item ->
        val lat = item.latitude ?: return@mapNotNull null
        val lon = item.longitude ?: return@mapNotNull null
        SearchLocationResult(
            name = item.name ?: "Unknown",
            region = item.admin1 ?: "",
            country = item.country ?: "",
            countryCode = item.countryCode ?: "",
            latitude = lat,
            longitude = lon,
            timezoneId = item.timezone ?: "UTC"
        )
    }
