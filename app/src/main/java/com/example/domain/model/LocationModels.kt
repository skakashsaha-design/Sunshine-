package com.example.domain.model

data class SavedLocation(
    val id: String,
    val name: String,
    val region: String,
    val country: String,
    val countryCode: String,
    val latitude: Double,
    val longitude: Double,
    val timezoneId: String = "UTC",
    val isDefault: Boolean = false,
    val isCurrentGps: Boolean = false,
    val sortOrder: Int = 0
)

data class SearchLocationResult(
    val name: String,
    val region: String,
    val country: String,
    val countryCode: String,
    val latitude: Double,
    val longitude: Double,
    val timezoneId: String
)

enum class MapBaseLayer(val label: String) {
    STANDARD("Standard"),
    SATELLITE("Satellite"),
    DARK("Dark"),
    TERRAIN("Terrain"),
    TOPOGRAPHIC("Topographic"),
    HYBRID("Hybrid")
}

enum class MapWeatherLayer(val label: String, val unit: String) {
    NONE("None", ""),
    RADAR_RAIN("Rain Radar", "dBZ"),
    RADAR_SNOW("Snow Radar", "cm/h"),
    TEMPERATURE("Temperature", "°C"),
    FEELS_LIKE("Feels Like", "°C"),
    RAIN("Rain", "mm"),
    PRECIPITATION("Precipitation", "mm"),
    SNOW("Snow", "cm"),
    WIND("Wind Speed", "km/h"),
    WIND_GUST("Wind Gust", "km/h"),
    WIND_DIRECTION("Wind Vectors", "°"),
    CLOUD_COVER("Cloud Cover", "%"),
    LOW_CLOUDS("Low Clouds", "%"),
    MID_CLOUDS("Mid Clouds", "%"),
    HIGH_CLOUDS("High Clouds", "%"),
    VISIBILITY("Visibility", "km"),
    FOG("Fog", "%"),
    PRESSURE("Pressure", "hPa"),
    HUMIDITY("Humidity", "%"),
    DEW_POINT("Dew Point", "°C"),
    UV("UV Index", "UV"),
    LIGHTNING("Lightning Risk", "idx"),
    STORM_CELLS("Storm Cells", "idx"),
    CAPE("CAPE / Convective", "J/kg")
}

data class RadarFrame(
    val timestampEpochSec: Long,
    val path: String,
    val isNowcast: Boolean,
    val tileUrlTemplate: String
)

data class RadarTimeline(
    val host: String,
    val frames: List<RadarFrame>,
    val generatedEpochSec: Long
)
