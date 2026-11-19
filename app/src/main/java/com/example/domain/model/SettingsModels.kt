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

enum class AnimationLevel(val label: String) {
    FULL("Full"),
    REDUCED("Reduced"),
    OFF("Off")
}

enum class TemperatureUnit(val symbol: String) {
    CELSIUS("°C"),
    FAHRENHEIT("°F")
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
    val aqiAlertsEnabled: Boolean = true,
    val disasterAlertsEnabled: Boolean = true,
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
    val themeAppearance: ThemeAppearance = ThemeAppearance.SYSTEM,
    val animationLevel: AnimationLevel = AnimationLevel.FULL,
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
