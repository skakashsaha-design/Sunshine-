package com.example.domain.calc

import com.example.domain.model.*
import kotlin.math.roundToInt

object AqiCalculator {

    private data class Breakpoint(val cLow: Double, val cHigh: Double, val iLow: Int, val iHigh: Int)

    private val pm25Breakpoints = listOf(
        Breakpoint(0.0, 9.0, 0, 50),
        Breakpoint(9.1, 35.4, 51, 100),
        Breakpoint(35.5, 55.4, 101, 150),
        Breakpoint(55.5, 125.4, 151, 200),
        Breakpoint(125.5, 225.4, 201, 300),
        Breakpoint(225.5, 500.4, 301, 500)
    )

    private val pm10Breakpoints = listOf(
        Breakpoint(0.0, 54.0, 0, 50),
        Breakpoint(55.0, 154.0, 51, 100),
        Breakpoint(155.0, 254.0, 101, 150),
        Breakpoint(255.0, 354.0, 151, 200),
        Breakpoint(355.0, 424.0, 201, 300),
        Breakpoint(425.0, 604.0, 301, 500)
    )

    fun pm25ToUsAqi(pm25: Double): Int = computeSubIndex(pm25, pm25Breakpoints)

    fun pm10ToUsAqi(pm10: Double): Int = computeSubIndex(pm10, pm10Breakpoints)

    private fun computeSubIndex(concentration: Double, table: List<Breakpoint>): Int {
        val c = concentration.coerceAtLeast(0.0)
        for (bp in table) {
            if (c <= bp.cHigh) {
                val ratio = (c - bp.cLow) / (bp.cHigh - bp.cLow).coerceAtLeast(0.001)
                return (ratio * (bp.iHigh - bp.iLow) + bp.iLow).roundToInt()
            }
        }
        return 500
    }

    fun categoryFromUsAqi(aqi: Int?): AqiCategory = when {
        aqi == null -> AqiCategory.MODERATE
        aqi <= 50 -> AqiCategory.GOOD
        aqi <= 100 -> AqiCategory.MODERATE
        aqi <= 150 -> AqiCategory.UNHEALTHY_SENSITIVE
        aqi <= 200 -> AqiCategory.UNHEALTHY
        aqi <= 300 -> AqiCategory.VERY_UNHEALTHY
        else -> AqiCategory.HAZARDOUS
    }

    fun healthGuidance(category: AqiCategory): String = when (category) {
        AqiCategory.GOOD -> "Air quality is satisfactory and poses little or no health risk."
        AqiCategory.MODERATE -> "Acceptable air quality; unusually sensitive individuals should monitor symptoms."
        AqiCategory.UNHEALTHY_SENSITIVE -> "Sensitive groups (children, elderly, asthma) should reduce prolonged outdoor exertion."
        AqiCategory.UNHEALTHY -> "Everyone may begin to experience health effects; wear a mask for extended outdoor time."
        AqiCategory.VERY_UNHEALTHY -> "Health alert: avoid prolonged outdoor exertion and keep indoor air filtered."
        AqiCategory.HAZARDOUS -> "Emergency conditions: remain indoors with windows sealed and air purification active."
    }

    fun analyzeWindows(hourly: List<HourlyAqi>): Pair<AqiWindow?, AqiWindow?> {
        val next24 = hourly.take(24).filter { it.usAqi != null }
        if (next24.size < 3) return null to null
        var bestIdx = 0
        var worstIdx = 0
        var bestAvg = Int.MAX_VALUE
        var worstAvg = Int.MIN_VALUE
        for (i in 0..next24.size - 3) {
            val avg = (next24[i].usAqi!! + next24[i + 1].usAqi!! + next24[i + 2].usAqi!!) / 3
            if (avg < bestAvg) {
                bestAvg = avg
                bestIdx = i
            }
            if (avg > worstAvg) {
                worstAvg = avg
                worstIdx = i
            }
        }
        val bestStart = next24[bestIdx].isoTime.substringAfter("T", "").take(5)
        val bestEnd = next24[bestIdx + 2].isoTime.substringAfter("T", "").take(5)
        val worstStart = next24[worstIdx].isoTime.substringAfter("T", "").take(5)
        val worstEnd = next24[worstIdx + 2].isoTime.substringAfter("T", "").take(5)

        return AqiWindow("Best Time Outdoors", "$bestStart – $bestEnd", bestAvg) to
            AqiWindow("Worst Time Outdoors", "$worstStart – $worstEnd", worstAvg)
    }

    fun buildPollutants(
        pm25: Double?,
        pm10: Double?,
        o3: Double?,
        no2: Double?,
        so2: Double?,
        co: Double?,
        dust: Double?,
        aod: Double?
    ): List<AqiPollutant> = buildList {
        pm25?.let { add(AqiPollutant("PM2.5", "Fine Particulate Matter", it, "µg/m³", pm25ToUsAqi(it))) }
        pm10?.let { add(AqiPollutant("PM10", "Coarse Particulate Matter", it, "µg/m³", pm10ToUsAqi(it))) }
        o3?.let { add(AqiPollutant("O₃", "Ozone", it, "µg/m³", null)) }
        no2?.let { add(AqiPollutant("NO₂", "Nitrogen Dioxide", it, "µg/m³", null)) }
        so2?.let { add(AqiPollutant("SO₂", "Sulfur Dioxide", it, "µg/m³", null)) }
        co?.let { add(AqiPollutant("CO", "Carbon Monoxide", it, "µg/m³", null)) }
        dust?.let { add(AqiPollutant("Dust", "Mineral Dust", it, "µg/m³", null)) }
        aod?.let { add(AqiPollutant("AOD", "Aerosol Optical Depth", it, "τ", null)) }
    }
}
