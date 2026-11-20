package com.example.domain.calc

import com.example.domain.model.PressureUnit
import com.example.domain.model.TemperatureUnit
import com.example.domain.model.WindUnit
import kotlin.math.*

object GeoUtils {

    private const val EARTH_RADIUS_KM = 6371.0

    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_KM * c
    }

    fun compassDirection(deg: Int?): String {
        if (deg == null) return "N/A"
        val dirs = listOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
        val idx = (((deg % 360) + 360) % 360 / 22.5 + 0.5).toInt() % 16
        return dirs[idx]
    }

    fun countryCodeToFlagEmoji(countryCode: String?): String {
        if (countryCode.isNullOrBlank() || countryCode.length != 2) return ""
        val upper = countryCode.uppercase()
        val first = Character.codePointAt(upper, 0) - 0x41 + 0x1F1E6
        val second = Character.codePointAt(upper, 1) - 0x41 + 0x1F1E6
        if (first < 0x1F1E6 || second < 0x1F1E6) return ""
        return String(Character.toChars(first)) + String(Character.toChars(second))
    }

    fun isBangladesh(countryCode: String?, countryName: String?, lat: Double, lon: Double): Boolean {
        if (countryCode.equals("BD", ignoreCase = true)) return true
        if (countryName?.contains("Bangladesh", ignoreCase = true) == true) return true
        return lat in 20.3..26.7 && lon in 88.0..92.7
    }

    fun latLonToTileDouble(lat: Double, lon: Double, zoom: Int): Pair<Double, Double> {
        val n = 2.0.pow(zoom)
        val clampedLat = lat.coerceIn(-85.05112878, 85.05112878)
        val latRad = Math.toRadians(clampedLat)
        val x = (lon + 180.0) / 360.0 * n
        val y = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * n
        return x to y
    }

    fun tileDoubleToLatLon(tileX: Double, tileY: Double, zoom: Int): Pair<Double, Double> {
        val n = 2.0.pow(zoom)
        val lon = tileX / n * 360.0 - 180.0
        val latRad = atan(sinh(PI * (1.0 - 2.0 * tileY / n)))
        val lat = Math.toDegrees(latRad).coerceIn(-85.0, 85.0)
        return lat to lon.coerceIn(-180.0, 180.0)
    }

    fun convertTemperature(celsius: Double, unit: TemperatureUnit): Double = when (unit) {
        TemperatureUnit.CELSIUS -> celsius
        TemperatureUnit.FAHRENHEIT -> celsius * 9.0 / 5.0 + 32.0
        TemperatureUnit.KELVIN -> celsius + 273.15
    }

    fun formatTemperature(celsius: Double?, unit: TemperatureUnit, withUnit: Boolean = true): String {
        if (celsius == null) return "N/A"
        val v = convertTemperature(celsius, unit).roundToInt()
        return if (withUnit) "$v${unit.symbol}" else if (unit == TemperatureUnit.KELVIN) "${v}K" else "$v°"
    }

    fun beaufortScale(kmh: Double?): Pair<Int, String> {
        if (kmh == null) return 0 to "Unknown"
        return when {
            kmh < 1.0 -> 0 to "Calm"
            kmh < 6.0 -> 1 to "Light Air"
            kmh < 12.0 -> 2 to "Light Breeze"
            kmh < 20.0 -> 3 to "Gentle Breeze"
            kmh < 29.0 -> 4 to "Moderate Breeze"
            kmh < 39.0 -> 5 to "Fresh Breeze"
            kmh < 50.0 -> 6 to "Strong Breeze"
            kmh < 62.0 -> 7 to "Near Gale"
            kmh < 75.0 -> 8 to "Gale"
            kmh < 89.0 -> 9 to "Strong Gale"
            kmh < 103.0 -> 10 to "Storm"
            kmh < 118.0 -> 11 to "Violent Storm"
            else -> 12 to "Hurricane Force"
        }
    }

    fun convertWind(kmh: Double, unit: WindUnit): Double = when (unit) {
        WindUnit.KMH -> kmh
        WindUnit.MS -> kmh / 3.6
        WindUnit.MPH -> kmh * 0.621371
        WindUnit.KTS -> kmh * 0.539957
    }

    fun formatWind(kmh: Double?, unit: WindUnit): String {
        if (kmh == null) return "N/A"
        val v = convertWind(kmh, unit)
        return if (unit == WindUnit.MS) {
            String.format(java.util.Locale.US, "%.1f %s", v, unit.symbol)
        } else {
            "${v.roundToInt()} ${unit.symbol}"
        }
    }

    fun convertPressure(hpa: Double, unit: PressureUnit): Double = when (unit) {
        PressureUnit.HPA -> hpa
        PressureUnit.INHG -> hpa * 0.02953
        PressureUnit.MMHG -> hpa * 0.750062
    }

    fun formatPressure(hpa: Double?, unit: PressureUnit): String {
        if (hpa == null) return "N/A"
        val v = convertPressure(hpa, unit)
        return when (unit) {
            PressureUnit.HPA -> "${v.roundToInt()} hPa"
            PressureUnit.INHG -> String.format(java.util.Locale.US, "%.2f inHg", v)
            PressureUnit.MMHG -> "${v.roundToInt()} mmHg"
        }
    }
}
