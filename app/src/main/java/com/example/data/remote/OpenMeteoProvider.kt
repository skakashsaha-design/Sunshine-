package com.example.data.remote

import com.example.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OpenMeteoProvider(private val client: NetworkClient) : WeatherProvider {

    override val providerId: ProviderId = ProviderId.OPEN_METEO

    private val forecastAdapter by lazy { client.moshi.adapter(OpenMeteoForecastDto::class.java) }
    private val aqiAdapter by lazy { client.moshi.adapter(OpenMeteoAirQualityDto::class.java) }

    override suspend fun fetchWeather(
        latitude: Double,
        longitude: Double,
        timezoneId: String,
        apiKey: String?
    ): ProviderResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        runCatching {
            val dto = client.openMeteoWeatherApi.getForecast(
                latitude = latitude,
                longitude = longitude
            )
            dto.toProviderSnapshot(System.currentTimeMillis() - start)
        }.fold(
            onSuccess = { ProviderResult.Success(it) },
            onFailure = { err ->
                ProviderResult.Failure(
                    providerId = providerId,
                    reason = err.message ?: "Open-Meteo Retrofit error",
                    responseTimeMs = System.currentTimeMillis() - start
                )
            }
        )
    }

    fun parseWeatherJson(raw: String, elapsedMs: Long): ProviderSnapshot {
        val dto = requireNotNull(forecastAdapter.fromJson(raw)) { "Invalid Open-Meteo JSON" }
        return dto.toProviderSnapshot(elapsedMs)
    }

    suspend fun fetchAirQuality(latitude: Double, longitude: Double): Result<AirQualityData> =
        withContext(Dispatchers.IO) {
            runCatching {
                client.openMeteoAirQualityApi.getAirQuality(latitude, longitude).toAirQualityData()
            }
        }

    fun parseAirQualityJson(raw: String): AirQualityData {
        val dto = requireNotNull(aqiAdapter.fromJson(raw)) { "Invalid Open-Meteo AQI JSON" }
        return dto.toAirQualityData()
    }

    suspend fun fetchFloodOutlook(latitude: Double, longitude: Double): Result<FloodOutlook> =
        withContext(Dispatchers.IO) {
            runCatching {
                client.openMeteoFloodApi.getFloodForecast(latitude, longitude).toFloodOutlook()
            }
        }

    suspend fun searchCities(query: String): Result<List<SearchLocationResult>> =
        withContext(Dispatchers.IO) {
            runCatching {
                client.openMeteoGeocodingApi.searchCities(query.trim()).toSearchResults()
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
