package com.example.domain.model

enum class AqiCategory(val label: String) {
    GOOD("Good"),
    MODERATE("Moderate"),
    UNHEALTHY_SENSITIVE("Unhealthy for Sensitive Groups"),
    UNHEALTHY("Unhealthy"),
    VERY_UNHEALTHY("Very Unhealthy"),
    HAZARDOUS("Hazardous")
}

data class AqiPollutant(
    val code: String,
    val name: String,
    val value: Double,
    val unit: String,
    val subIndexUs: Int?
)

data class HourlyAqi(
    val epochSeconds: Long,
    val isoTime: String,
    val usAqi: Int?,
    val europeanAqi: Int?,
    val pm25: Double?,
    val pm10: Double?,
    val ozone: Double?
)

data class AqiWindow(
    val label: String,
    val timeRange: String,
    val avgAqi: Int
)

data class AirQualityData(
    val usAqi: Int?,
    val europeanAqi: Int?,
    val category: AqiCategory,
    val sourceLabel: String,
    val origin: DataOrigin,
    val pm25: Double?,
    val pm10: Double?,
    val o3: Double?,
    val no2: Double?,
    val so2: Double?,
    val co: Double?,
    val dust: Double?,
    val aerosolOpticalDepth: Double?,
    val pollutants: List<AqiPollutant>,
    val hourlyAqi: List<HourlyAqi>,
    val trendLabel: String,
    val bestOutdoorWindow: AqiWindow?,
    val worstOutdoorWindow: AqiWindow?,
    val healthGuidance: String,
    val fetchedEpochMs: Long
)
