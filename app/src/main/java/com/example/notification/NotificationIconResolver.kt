package com.example.notification

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import com.example.R
import com.example.domain.model.AlertCategory
import com.example.domain.model.AlertSeverity
import com.example.domain.model.DisasterCategory
import com.example.domain.model.WeatherCondition

data class NotificationIconSpec(
    @DrawableRes val smallIconRes: Int,
    val accentColorArgb: Int,
    val label: String
)

object NotificationIconResolver {

    fun forWeatherCondition(condition: WeatherCondition): NotificationIconSpec = when (condition) {
        WeatherCondition.CLEAR -> NotificationIconSpec(
            R.drawable.ic_notif_sun_clear,
            0xFFF59E0B.toInt(),
            "Clear sky icon"
        )
        WeatherCondition.PARTLY_CLOUDY -> NotificationIconSpec(
            R.drawable.ic_notif_partly_cloudy,
            0xFF38BDF8.toInt(),
            "Partly cloudy icon"
        )
        WeatherCondition.CLOUDY, WeatherCondition.FOG -> NotificationIconSpec(
            R.drawable.ic_notif_cloudy,
            0xFF64748B.toInt(),
            "Cloudy or fog icon"
        )
        WeatherCondition.DRIZZLE, WeatherCondition.RAIN, WeatherCondition.HEAVY_RAIN -> NotificationIconSpec(
            R.drawable.ic_notif_rain,
            0xFF0284C7.toInt(),
            "Rain weather icon"
        )
        WeatherCondition.THUNDERSTORM -> NotificationIconSpec(
            R.drawable.ic_notif_storm,
            0xFF7C3AED.toInt(),
            "Thunderstorm lightning icon"
        )
        WeatherCondition.SNOW, WeatherCondition.SLEET -> NotificationIconSpec(
            R.drawable.ic_notif_snow,
            0xFF06B6D4.toInt(),
            "Snow or sleet icon"
        )
        WeatherCondition.WINDY -> NotificationIconSpec(
            R.drawable.ic_notif_wind,
            0xFF0D9488.toInt(),
            "High wind icon"
        )
    }

    fun forAlertCategory(category: AlertCategory, severity: AlertSeverity = AlertSeverity.MODERATE): NotificationIconSpec {
        val defaultSevColor = severityAccentColor(severity)
        return when (category) {
            AlertCategory.THUNDERSTORM -> NotificationIconSpec(R.drawable.ic_notif_storm, 0xFF7C3AED.toInt(), "Thunderstorm warning icon")
            AlertCategory.HEAVY_RAIN -> NotificationIconSpec(R.drawable.ic_notif_rain, 0xFF0284C7.toInt(), "Heavy rain warning icon")
            AlertCategory.FLASH_FLOOD, AlertCategory.RIVER_FLOOD, AlertCategory.COASTAL_FLOOD, AlertCategory.STORM_SURGE ->
                NotificationIconSpec(R.drawable.ic_notif_flood, 0xFF2563EB.toInt(), "Flood warning icon")
            AlertCategory.CYCLONE, AlertCategory.HURRICANE, AlertCategory.TYPHOON, AlertCategory.TROPICAL_STORM,
            AlertCategory.TORNADO, AlertCategory.STRONG_WIND ->
                NotificationIconSpec(R.drawable.ic_notif_wind, 0xFFDC2626.toInt(), "Cyclone or high-wind warning icon")
            AlertCategory.EXTREME_HEAT, AlertCategory.DROUGHT ->
                NotificationIconSpec(R.drawable.ic_notif_sun_clear, 0xFFEA580C.toInt(), "Extreme heat warning icon")
            AlertCategory.EXTREME_COLD, AlertCategory.SNOW, AlertCategory.ICE ->
                NotificationIconSpec(R.drawable.ic_notif_snow, 0xFF0891B2.toInt(), "Cold, snow or ice warning icon")
            AlertCategory.FOG -> NotificationIconSpec(R.drawable.ic_notif_cloudy, 0xFF64748B.toInt(), "Dense fog advisory icon")
            AlertCategory.DUST_STORM -> NotificationIconSpec(R.drawable.ic_notif_aqi, 0xFFD97706.toInt(), "Dust storm air hazard icon")
            AlertCategory.WILDFIRE -> NotificationIconSpec(R.drawable.ic_notif_wildfire, 0xFFDC2626.toInt(), "Wildfire hazard icon")
            AlertCategory.VOLCANO -> NotificationIconSpec(R.drawable.ic_notif_volcano, 0xFFB91C1C.toInt(), "Volcanic activity warning icon")
            AlertCategory.EARTHQUAKE -> NotificationIconSpec(R.drawable.ic_notif_earthquake, 0xFFEF4444.toInt(), "Earthquake alert icon")
            AlertCategory.TSUNAMI -> NotificationIconSpec(R.drawable.ic_notif_tsunami, 0xFF1D4ED8.toInt(), "Tsunami warning icon")
            AlertCategory.LANDSLIDE -> NotificationIconSpec(R.drawable.ic_notif_landslide, 0xFFB45309.toInt(), "Landslide hazard icon")
            AlertCategory.SEVERE_WEATHER, AlertCategory.OTHER ->
                NotificationIconSpec(R.drawable.ic_notif_warning_general, defaultSevColor, "Severe hazard warning icon")
        }
    }

    fun forDisasterCategory(category: DisasterCategory, tsunamiFlag: Boolean = false): NotificationIconSpec {
        if (tsunamiFlag || category == DisasterCategory.TSUNAMI) {
            return NotificationIconSpec(R.drawable.ic_notif_tsunami, 0xFF1D4ED8.toInt(), "Tsunami alert icon")
        }
        return when (category) {
            DisasterCategory.EARTHQUAKE -> NotificationIconSpec(R.drawable.ic_notif_earthquake, 0xFFEF4444.toInt(), "Earthquake alert icon")
            DisasterCategory.CYCLONE -> NotificationIconSpec(R.drawable.ic_notif_wind, 0xFFDC2626.toInt(), "Cyclone disaster icon")
            DisasterCategory.FLOOD -> NotificationIconSpec(R.drawable.ic_notif_flood, 0xFF2563EB.toInt(), "Flood disaster icon")
            DisasterCategory.WILDFIRE -> NotificationIconSpec(R.drawable.ic_notif_wildfire, 0xFFEA580C.toInt(), "Wildfire disaster icon")
            DisasterCategory.VOLCANO -> NotificationIconSpec(R.drawable.ic_notif_volcano, 0xFFB91C1C.toInt(), "Volcano eruption icon")
            DisasterCategory.LANDSLIDE -> NotificationIconSpec(R.drawable.ic_notif_landslide, 0xFFB45309.toInt(), "Landslide disaster icon")
            DisasterCategory.SEVERE_STORM -> NotificationIconSpec(R.drawable.ic_notif_storm, 0xFF7C3AED.toInt(), "Severe storm disaster icon")
            DisasterCategory.DROUGHT, DisasterCategory.EXTREME_WEATHER ->
                NotificationIconSpec(R.drawable.ic_notif_sun_clear, 0xFFD97706.toInt(), "Extreme weather hazard icon")
            DisasterCategory.TSUNAMI -> NotificationIconSpec(R.drawable.ic_notif_tsunami, 0xFF1D4ED8.toInt(), "Tsunami alert icon")
            DisasterCategory.OTHER -> NotificationIconSpec(R.drawable.ic_notif_warning_general, 0xFFF59E0B.toInt(), "Global hazard warning icon")
        }
    }

    fun forAqi(usAqi: Int): NotificationIconSpec = NotificationIconSpec(
        smallIconRes = R.drawable.ic_notif_aqi,
        accentColorArgb = when {
            usAqi >= 201 -> 0xFF9333EA.toInt()
            usAqi >= 151 -> 0xFFDC2626.toInt()
            else -> 0xFFF59E0B.toInt()
        },
        label = "Air quality warning icon"
    )

    fun createLargeBadgeBitmap(context: Context, spec: NotificationIconSpec): Bitmap? {
        return runCatching {
            val sizePx = 128
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = spec.accentColorArgb }
            canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, paint)
            val drawable = ContextCompat.getDrawable(context, spec.smallIconRes)?.mutate()
            if (drawable != null) {
                drawable.setTint(android.graphics.Color.WHITE)
                val pad = (sizePx * 0.24f).toInt()
                drawable.setBounds(pad, pad, sizePx - pad, sizePx - pad)
                drawable.draw(canvas)
            }
            bitmap
        }.getOrNull()
    }

    private fun severityAccentColor(severity: AlertSeverity): Int = when (severity) {
        AlertSeverity.EXTREME -> 0xFFDC2626.toInt()
        AlertSeverity.SEVERE -> 0xFFEA580C.toInt()
        AlertSeverity.MODERATE -> 0xFFF59E0B.toInt()
        AlertSeverity.MINOR -> 0xFF0284C7.toInt()
        AlertSeverity.ADVISORY -> 0xFF3B82F6.toInt()
    }
}

fun alertCategoryVector(category: AlertCategory): ImageVector = when (category) {
    AlertCategory.THUNDERSTORM -> Icons.Default.Thunderstorm
    AlertCategory.HEAVY_RAIN -> Icons.Default.WaterDrop
    AlertCategory.FLASH_FLOOD, AlertCategory.RIVER_FLOOD, AlertCategory.COASTAL_FLOOD, AlertCategory.STORM_SURGE ->
        Icons.Default.Flood
    AlertCategory.CYCLONE, AlertCategory.HURRICANE, AlertCategory.TYPHOON, AlertCategory.TROPICAL_STORM ->
        Icons.Default.Cyclone
    AlertCategory.TORNADO, AlertCategory.STRONG_WIND -> Icons.Default.Air
    AlertCategory.EXTREME_HEAT, AlertCategory.DROUGHT -> Icons.Default.WbSunny
    AlertCategory.EXTREME_COLD, AlertCategory.SNOW, AlertCategory.ICE -> Icons.Default.AcUnit
    AlertCategory.FOG, AlertCategory.DUST_STORM -> Icons.Default.Cloud
    AlertCategory.WILDFIRE -> Icons.Default.LocalFireDepartment
    AlertCategory.VOLCANO -> Icons.Default.Volcano
    AlertCategory.EARTHQUAKE -> Icons.Default.Sensors
    AlertCategory.TSUNAMI -> Icons.Default.Tsunami
    AlertCategory.LANDSLIDE -> Icons.Default.Landslide
    AlertCategory.SEVERE_WEATHER, AlertCategory.OTHER -> Icons.Default.WarningAmber
}

fun disasterCategoryVector(category: DisasterCategory, tsunamiFlag: Boolean = false): ImageVector {
    if (tsunamiFlag || category == DisasterCategory.TSUNAMI) return Icons.Default.Tsunami
    return when (category) {
        DisasterCategory.EARTHQUAKE -> Icons.Default.Sensors
        DisasterCategory.CYCLONE -> Icons.Default.Cyclone
        DisasterCategory.FLOOD -> Icons.Default.Flood
        DisasterCategory.WILDFIRE -> Icons.Default.LocalFireDepartment
        DisasterCategory.VOLCANO -> Icons.Default.Volcano
        DisasterCategory.LANDSLIDE -> Icons.Default.Landslide
        DisasterCategory.SEVERE_STORM -> Icons.Default.Thunderstorm
        DisasterCategory.DROUGHT, DisasterCategory.EXTREME_WEATHER -> Icons.Default.WbSunny
        DisasterCategory.TSUNAMI -> Icons.Default.Tsunami
        DisasterCategory.OTHER -> Icons.Default.WarningAmber
    }
}
