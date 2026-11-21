package com.example.domain.model

enum class MoonPhaseType(val label: String) {
    NEW_MOON("New Moon"),
    WAXING_CRESCENT("Waxing Crescent"),
    FIRST_QUARTER("First Quarter"),
    WAXING_GIBBOUS("Waxing Gibbous"),
    FULL_MOON("Full Moon"),
    WANING_GIBBOUS("Waning Gibbous"),
    LAST_QUARTER("Last Quarter"),
    WANING_CRESCENT("Waning Crescent")
}

data class SunData(
    val sunriseTime: String,
    val sunsetTime: String,
    val solarNoonTime: String,
    val civilDawnTime: String,
    val civilDuskTime: String,
    val goldenHourMorning: String,
    val goldenHourEvening: String,
    val blueHourMorning: String,
    val blueHourEvening: String,
    val dayLengthFormatted: String,
    val dayLengthMinutes: Int,
    val remainingDaylightFormatted: String,
    val sunProgressFraction: Float,
    val sunAltitudeDeg: Double,
    val sunAzimuthDeg: Double,
    val isDaylight: Boolean,
    val isTwilight: Boolean
)

data class MoonData(
    val phaseType: MoonPhaseType,
    val phaseFraction: Double,
    val illuminationPercent: Double,
    val moonAgeDays: Double,
    val moonriseTime: String,
    val moonsetTime: String,
    val moonAltitudeDeg: Double,
    val moonAzimuthDeg: Double,
    val moonDistanceKm: Int,
    val nextFullMoonDate: String,
    val nextNewMoonDate: String
)

data class StarConstellationInfo(
    val name: String,
    val bestVisibility: String,
    val direction: String,
    val description: String
)

data class AstronomyEventInfo(
    val title: String,
    val dateStr: String,
    val description: String
)

data class AstronomySnapshot(
    val sun: SunData,
    val moon: MoonData,
    val constellations: List<StarConstellationInfo>,
    val events: List<AstronomyEventInfo>,
    val calculatedAtEpochMs: Long,
    val origin: DataOrigin = DataOrigin.CALCULATED
)
