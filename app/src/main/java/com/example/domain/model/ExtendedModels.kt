package com.example.domain.model

enum class ProviderCategory(val label: String) {
    WEATHER("Weather"),
    AQI("Air Quality"),
    RADAR("Radar"),
    SATELLITE("Satellite"),
    LIGHTNING("Lightning"),
    FLOOD("Floods"),
    EARTHQUAKE("Earthquakes"),
    TSUNAMI("Tsunami"),
    CYCLONE("Cyclones"),
    WILDFIRE("Wildfires"),
    VOLCANO("Volcanoes"),
    LANDSLIDE("Landslides"),
    MARINE("Marine"),
    TIDE("Tides"),
    AVIATION("Aviation METAR/TAF"),
    ASTRONOMY("Astronomy"),
    SPACE_WEATHER("Space Weather"),
    EMERGENCY("Emergency Numbers")
}

data class ProviderRegistryEntry(
    val id: String,
    val name: String,
    val category: ProviderCategory,
    val baseUrl: String,
    val authRequirement: String,
    val supportedFeatures: String,
    val supportedCountries: String,
    val rateLimit: String,
    val licenseAttribution: String,
    val availabilityStatus: String,
    val isLiveAvailable: Boolean
)

data class ModelForecastEntry(
    val modelName: String,
    val modelCode: String,
    val temperatureC: Double?,
    val feelsLikeC: Double?,
    val rainProbabilityPercent: Int?,
    val precipitationMm: Double?,
    val windSpeedKmh: Double?,
    val pressureHpa: Double?,
    val cloudCoverPercent: Int?,
    val humidityPercent: Int?
)

data class ModelComparisonResult(
    val models: List<ModelForecastEntry>,
    val agreementLabel: String,
    val confidenceLabel: String,
    val confidenceExplanation: String,
    val ensembleMinTempC: Double?,
    val ensembleMedianTempC: Double?,
    val ensembleMaxTempC: Double?,
    val ensembleSpreadC: Double?,
    val fetchedEpochMs: Long
)

data class MarineWeatherData(
    val source: String,
    val isMarineArea: Boolean,
    val waveHeightM: Double?,
    val waveDirectionDeg: Int?,
    val wavePeriodSec: Double?,
    val swellHeightM: Double?,
    val seaSurfaceTempC: Double?,
    val windSpeedKmh: Double?,
    val windGustKmh: Double?,
    val pressureHpa: Double?,
    val tideStatusNote: String,
    val hourlyWaveHeights: List<Pair<String, Double>>,
    val fetchedEpochMs: Long
)

data class AviationAirportPreset(
    val iata: String,
    val icao: String,
    val name: String,
    val countryCode: String
)

data class AviationMetarData(
    val icao: String,
    val iata: String?,
    val airportName: String,
    val rawMetar: String,
    val rawTaf: String?,
    val observationTimeIso: String,
    val windDirDeg: Int?,
    val windSpeedKt: Int?,
    val visibilityStr: String,
    val ceilingStr: String,
    val tempC: Double?,
    val dewpointC: Double?,
    val altimeterHpa: Double?,
    val flightCategory: String?,
    val weatherString: String,
    val source: String,
    val fetchedEpochMs: Long
)

data class SpaceWeatherData(
    val kpIndex: Double?,
    val geomagneticStatus: String,
    val solarWindSpeedKms: Double?,
    val auroraPossibility: String,
    val observationTimeIso: String,
    val source: String,
    val fetchedEpochMs: Long
)

data class EmergencyNumberEntry(
    val countryCode: String,
    val countryName: String,
    val organization: String,
    val service: String,
    val number: String,
    val category: String,
    val source: String,
    val verifiedDate: String
)

data class MetricGlossaryEntry(
    val key: String,
    val titleEn: String,
    val explanationEn: String,
    val titleBn: String,
    val explanationBn: String
)
