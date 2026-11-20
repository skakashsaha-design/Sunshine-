package com.example.domain.model

enum class AlertSeverity(val rank: Int, val label: String) {
    EXTREME(4, "Extreme"),
    SEVERE(3, "Severe"),
    MODERATE(2, "Moderate"),
    MINOR(1, "Minor"),
    ADVISORY(0, "Advisory")
}

enum class AlertCategory(val label: String) {
    SEVERE_WEATHER("Severe Weather"),
    THUNDERSTORM("Thunderstorm"),
    TORNADO("Tornado"),
    CYCLONE("Cyclone"),
    TROPICAL_STORM("Tropical Storm"),
    HURRICANE("Hurricane"),
    TYPHOON("Typhoon"),
    HEAVY_RAIN("Heavy Rain"),
    FLASH_FLOOD("Flash Flood"),
    RIVER_FLOOD("River Flood"),
    COASTAL_FLOOD("Coastal Flood"),
    STORM_SURGE("Storm Surge"),
    EXTREME_HEAT("Extreme Heat"),
    EXTREME_COLD("Extreme Cold"),
    SNOW("Snow"),
    ICE("Ice"),
    STRONG_WIND("Strong Wind"),
    FOG("Fog"),
    DUST_STORM("Dust Storm"),
    WILDFIRE("Wildfire"),
    VOLCANO("Volcano"),
    EARTHQUAKE("Earthquake"),
    TSUNAMI("Tsunami"),
    LANDSLIDE("Landslide"),
    DROUGHT("Drought"),
    OTHER("Natural Hazard")
}

data class WeatherAlert(
    val id: String,
    val title: String,
    val category: AlertCategory,
    val severity: AlertSeverity,
    val urgency: String,
    val certainty: String,
    val sourceName: String,
    val issuingAuthority: String,
    val origin: DataOrigin,
    val issueTimeIso: String,
    val startTimeIso: String,
    val expiryTimeIso: String,
    val affectedArea: String,
    val distanceKm: Double?,
    val latitude: Double?,
    val longitude: Double?,
    val description: String,
    val instruction: String? = null
)

enum class DisasterCategory(val label: String) {
    EARTHQUAKE("Earthquake"),
    TSUNAMI("Tsunami"),
    CYCLONE("Cyclone / Hurricane / Typhoon"),
    FLOOD("Flood"),
    WILDFIRE("Wildfire"),
    VOLCANO("Volcano"),
    LANDSLIDE("Landslide"),
    SEVERE_STORM("Severe Storm"),
    DROUGHT("Drought"),
    EXTREME_WEATHER("Extreme Weather"),
    OTHER("Other Event")
}

data class DisasterEvent(
    val id: String,
    val title: String,
    val category: DisasterCategory,
    val severity: AlertSeverity,
    val magnitudeOrIntensity: String?,
    val depthKm: Double?,
    val tsunamiFlag: Boolean,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val eventTimeIso: String,
    val eventEpochMs: Long,
    val distanceKm: Double?,
    val source: String,
    val origin: DataOrigin,
    val status: String,
    val affectedRegion: String,
    val url: String?
)

data class FloodOutlook(
    val riverDischargeM3s: Double?,
    val maxDischarge7dM3s: Double?,
    val meanDischargeHistoricalM3s: Double?,
    val riskLevel: String,
    val origin: DataOrigin,
    val summary: String,
    val dailyDischargeForecast: List<Pair<String, Double>>
)

data class TsunamiStatus(
    val activeWarning: Boolean,
    val statusLabel: String,
    val region: String,
    val issueTimeIso: String,
    val expiryTimeIso: String,
    val source: String,
    val latitude: Double?,
    val longitude: Double?
)

data class BmdBulletin(
    val isBangladeshLocation: Boolean,
    val hasOfficialLiveBulletin: Boolean,
    val stationOrRegion: String,
    val bulletinOrigin: DataOrigin,
    val marineWarning: String,
    val inlandWarning: String,
    val alerts: List<WeatherAlert>,
    val statusNote: String,
    val lastCheckedEpochMs: Long,
    val bwotWatchSummary: String = "",
    val bwotSquallRisk: String = "Low"
)
