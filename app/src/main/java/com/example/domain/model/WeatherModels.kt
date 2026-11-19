package com.example.domain.model

enum class WeatherCondition {
    CLEAR,
    PARTLY_CLOUDY,
    CLOUDY,
    FOG,
    DRIZZLE,
    RAIN,
    HEAVY_RAIN,
    SLEET,
    SNOW,
    THUNDERSTORM,
    WINDY
}

enum class DataOrigin {
    OBSERVED,
    FORECAST,
    CALCULATED,
    MODEL_ESTIMATE,
    OFFICIAL_BMD,
    GLOBAL_OFFICIAL
}

enum class ProviderId(val displayName: String, val requiresKey: Boolean) {
    OPEN_METEO("Open-Meteo", false),
    MET_NORWAY("MET Norway", false),
    WEATHER_API("WeatherAPI", true),
    ACCU_WEATHER("AccuWeather", true),
    TOMORROW_IO("Tomorrow.io", true),
    WEATHERBIT("Weatherbit", true),
    GOOGLE_WEATHER("Google Weather", true),
    BMD("Bangladesh BMD", false)
}

data class CurrentWeather(
    val temperatureC: Double,
    val feelsLikeC: Double,
    val condition: WeatherCondition,
    val conditionLabel: String,
    val isDay: Boolean,
    val humidityPercent: Int?,
    val relativeHumidityPercent: Int?,
    val dewPointC: Double?,
    val wetBulbTempC: Double?,
    val pressureHpa: Double?,
    val pressureTrend: PressureTrend?,
    val windSpeedKmh: Double?,
    val windDirectionDeg: Int?,
    val windGustKmh: Double?,
    val windChillC: Double?,
    val heatIndexC: Double?,
    val visibilityKm: Double?,
    val cloudCoverPercent: Int?,
    val cloudLowPercent: Int?,
    val cloudMidPercent: Int?,
    val cloudHighPercent: Int?,
    val precipitationMm: Double?,
    val rainMm: Double?,
    val snowCm: Double?,
    val sleetMm: Double?,
    val hailMm: Double?,
    val rainProbabilityPercent: Int?,
    val snowProbabilityPercent: Int?,
    val freezingLevelMeters: Double?,
    val uvIndex: Double?,
    val uvRiskLabel: String?,
    val solarRadiationWm2: Double?,
    val soilMoistureM3: Double?,
    val soilTempC: Double?,
    val surfaceTempC: Double?,
    val capeJoulesKg: Double?,
    val origin: DataOrigin = DataOrigin.OBSERVED
)

enum class PressureTrend {
    RISING_RAPIDLY,
    RISING,
    STEADY,
    FALLING,
    FALLING_RAPIDLY
}

data class HourlyForecast(
    val epochSeconds: Long,
    val isoTime: String,
    val temperatureC: Double,
    val feelsLikeC: Double?,
    val condition: WeatherCondition,
    val conditionLabel: String,
    val isDay: Boolean,
    val precipitationMm: Double?,
    val precipitationProbabilityPercent: Int?,
    val rainMm: Double?,
    val snowCm: Double?,
    val windSpeedKmh: Double?,
    val windDirectionDeg: Int?,
    val windGustKmh: Double?,
    val humidityPercent: Int?,
    val pressureHpa: Double?,
    val cloudCoverPercent: Int?,
    val dewPointC: Double?,
    val visibilityKm: Double?,
    val uvIndex: Double?,
    val aqiUs: Int? = null,
    val origin: DataOrigin = DataOrigin.FORECAST
)

data class DailyForecast(
    val epochSeconds: Long,
    val isoDate: String,
    val tempMaxC: Double,
    val tempMinC: Double,
    val tempAvgC: Double?,
    val condition: WeatherCondition,
    val conditionLabel: String,
    val rainProbabilityPercent: Int?,
    val rainAmountMm: Double?,
    val snowProbabilityPercent: Int?,
    val snowAmountCm: Double?,
    val windMaxKmh: Double?,
    val windGustMaxKmh: Double?,
    val windDirectionDeg: Int?,
    val uvIndexMax: Double?,
    val humidityAvgPercent: Int?,
    val sunriseIso: String?,
    val sunsetIso: String?,
    val moonPhaseName: String?,
    val origin: DataOrigin = DataOrigin.FORECAST
)

data class WeatherInsights(
    val feelsLikeExplanation: String,
    val dewPointComfort: String,
    val humidityComfort: String,
    val pressureAnalysis: String,
    val rainTrend: String,
    val stormTrend: String,
    val visibilityRisk: String,
    val uvExposureLevel: String,
    val outdoorComfortScore: Int,
    val outdoorComfortLabel: String,
    val weatherConfidencePercent: Int?,
    val cycloneWindRisk: String?,
    val landslideRisk: String?
)

data class ProviderHealth(
    val providerId: ProviderId,
    val isAvailable: Boolean,
    val isFallback: Boolean,
    val lastSuccessEpochMs: Long?,
    val responseTimeMs: Long?,
    val errorMessage: String?
)

data class ProviderSnapshot(
    val providerId: ProviderId,
    val fetchedEpochMs: Long,
    val current: CurrentWeather,
    val hourly: List<HourlyForecast>,
    val daily: List<DailyForecast>,
    val responseTimeMs: Long
)

data class ProviderComparisonEntry(
    val providerId: ProviderId,
    val status: String,
    val fetchedEpochMs: Long?,
    val responseTimeMs: Long?,
    val temperatureC: Double?,
    val feelsLikeC: Double?,
    val rainProbabilityPercent: Int?,
    val precipitationMm: Double?,
    val windSpeedKmh: Double?,
    val humidityPercent: Int?,
    val pressureHpa: Double?,
    val uvIndex: Double?,
    val conditionLabel: String?,
    val error: String?
)

data class WeatherBundle(
    val location: SavedLocation,
    val activeProvider: ProviderId,
    val requestedProvider: ProviderId?,
    val usedFallback: Boolean,
    val fallbackReason: String?,
    val isOfflineCache: Boolean,
    val lastUpdatedEpochMs: Long,
    val current: CurrentWeather,
    val hourly: List<HourlyForecast>,
    val daily: List<DailyForecast>,
    val insights: WeatherInsights,
    val summaryLines: List<String>,
    val dailyAdvisorLines: List<String>,
    val providerHealthList: List<ProviderHealth>,
    val comparisons: List<ProviderComparisonEntry>
)
