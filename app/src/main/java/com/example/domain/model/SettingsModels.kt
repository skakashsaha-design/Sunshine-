package com.example.domain.model

import com.example.ui.theme.ThemeAppearance

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    BENGALI("bn", "বাংলা"),
    HINDI("hi", "हिन्दी"),
    SPANISH("es", "Español"),
    FRENCH("fr", "Français"),
    JAPANESE("ja", "日本語")
}

enum class FontStylePreset(val label: String, val description: String) {
    MODERN_GEOMETRIC("Modern Plus", "Plus Jakarta + Inter"),
    EDITORIAL_TECH("Space Tech", "Space Grotesk + Inter"),
    SLEEK_ROUNDED("Outfit Clean", "Outfit + Plus Jakarta"),
    TELEMETRY_MONO("Telemetry Mono", "JetBrains Mono Focus")
}

enum class BackgroundStyle(val label: String) {
    DYNAMIC_SKY("Dynamic Weather Sky"),
    AURORA_GLOW("Aurora Horizon"),
    DEEP_SPACE("Deep Obsidian"),
    RADAR_GRID("Atmospheric Radar"),
    SOLID_CLEAN("Solid Clean")
}

enum class AnimationLevel(val label: String) {
    FULL("Full"),
    REDUCED("Reduced"),
    MINIMAL("Minimal"),
    OFF("Off")
}

enum class PerformanceMode(val label: String, val subtitle: String) {
    AUTO("Automatic", "Auto-detects device RAM tier (enables 1 GB mode on low-RAM devices)"),
    LOW_RAM_1GB("1 GB Low-RAM", "Minimal animations, lightweight tiles & memory footprint"),
    BALANCED("Balanced", "Moderate transitions & standard map caching"),
    HIGH_PERFORMANCE("High Performance", "Full dynamic sky animations, full charts & high-res layers")
}

enum class TemperatureUnit(val symbol: String) {
    CELSIUS("°C"),
    FAHRENHEIT("°F"),
    KELVIN("K")
}

enum class WindUnit(val symbol: String) {
    KMH("km/h"),
    MS("m/s"),
    MPH("mph"),
    KTS("kts")
}

enum class PressureUnit(val symbol: String) {
    HPA("hPa"),
    INHG("inHg"),
    MMHG("mmHg")
}

enum class AmbientSoundType(val label: String) {
    NONE("Off"),
    RAIN("Rain"),
    THUNDER("Thunder"),
    WIND("Wind"),
    WINTER("Winter Breeze"),
    CALM("Calm Ambience")
}

data class NotificationSettings(
    val enabled: Boolean = true,
    val dailySummaryEnabled: Boolean = true,
    val dailyHour: Int = 7,
    val dailyMinute: Int = 30,
    val rainAlertsEnabled: Boolean = true,
    val severeWeatherEnabled: Boolean = true,
    val bwotAlertsEnabled: Boolean = true,
    val aqiAlertsEnabled: Boolean = true,
    val disasterAlertsEnabled: Boolean = true,
    val worldwideAlertsEnabled: Boolean = true,
    val minSeverityRank: Int = 1,
    val disasterRadiusKm: Int = 500
)

data class PersonalApiKeys(
    val weatherApiKey: String = "",
    val accuWeatherKey: String = "",
    val tomorrowIoKey: String = "",
    val weatherbitKey: String = "",
    val googleWeatherKey: String = "",
    val personalAiEnabled: Boolean = false,
    val personalAiEndpoint: String = "",
    val personalAiKey: String = "",
    val bmdCustomFeedUrl: String = ""
)

data class UserPreferences(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val themeAppearance: ThemeAppearance = ThemeAppearance.DYNAMIC_WEATHER,
    val fontStyle: FontStylePreset = FontStylePreset.MODERN_GEOMETRIC,
    val backgroundStyle: BackgroundStyle = BackgroundStyle.DYNAMIC_SKY,
    val animationLevel: AnimationLevel = AnimationLevel.FULL,
    val performanceMode: PerformanceMode = PerformanceMode.AUTO,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val windUnit: WindUnit = WindUnit.KMH,
    val pressureUnit: PressureUnit = PressureUnit.HPA,
    val autoProvider: Boolean = true,
    val selectedProvider: ProviderId = ProviderId.OPEN_METEO,
    val notificationSettings: NotificationSettings = NotificationSettings(),
    val ambientSound: AmbientSoundType = AmbientSoundType.NONE,
    val ambientVolume: Float = 0.45f,
    val ambientAutoStopMinutes: Int = 30,
    val liveWallpaperStaticFallback: Boolean = false,
    val personalKeys: PersonalApiKeys = PersonalApiKeys()
)
