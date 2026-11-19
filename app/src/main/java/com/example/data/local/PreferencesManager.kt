package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.*
import com.example.ui.theme.ThemeAppearance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "skypulse_prefs")

class PreferencesManager(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val THEME = stringPreferencesKey("theme")
        val ANIMATION = stringPreferencesKey("animation")
        val TEMP_UNIT = stringPreferencesKey("temp_unit")
        val WIND_UNIT = stringPreferencesKey("wind_unit")
        val PRESSURE_UNIT = stringPreferencesKey("pressure_unit")
        val AUTO_PROVIDER = booleanPreferencesKey("auto_provider")
        val SELECTED_PROVIDER = stringPreferencesKey("selected_provider")

        val NOTIF_ENABLED = booleanPreferencesKey("notif_enabled")
        val NOTIF_DAILY = booleanPreferencesKey("notif_daily")
        val NOTIF_DAILY_HOUR = intPreferencesKey("notif_daily_hour")
        val NOTIF_DAILY_MIN = intPreferencesKey("notif_daily_min")
        val NOTIF_RAIN = booleanPreferencesKey("notif_rain")
        val NOTIF_SEVERE = booleanPreferencesKey("notif_severe")
        val NOTIF_AQI = booleanPreferencesKey("notif_aqi")
        val NOTIF_DISASTER = booleanPreferencesKey("notif_disaster")
        val NOTIF_MIN_SEV = intPreferencesKey("notif_min_sev")
        val NOTIF_RADIUS = intPreferencesKey("notif_radius")

        val AMBIENT_SOUND = stringPreferencesKey("ambient_sound")
        val AMBIENT_VOLUME = floatPreferencesKey("ambient_volume")
        val AMBIENT_TIMER = intPreferencesKey("ambient_timer")
        val WALLPAPER_STATIC = booleanPreferencesKey("wallpaper_static")

        val KEY_WEATHERAPI = stringPreferencesKey("key_weatherapi")
        val KEY_ACCUWEATHER = stringPreferencesKey("key_accuweather")
        val KEY_TOMORROWIO = stringPreferencesKey("key_tomorrowio")
        val KEY_WEATHERBIT = stringPreferencesKey("key_weatherbit")
        val KEY_GOOGLE_WEATHER = stringPreferencesKey("key_google_weather")
        val AI_ENABLED = booleanPreferencesKey("ai_enabled")
        val AI_ENDPOINT = stringPreferencesKey("ai_endpoint")
        val AI_KEY = stringPreferencesKey("ai_key")
        val BMD_FEED_URL = stringPreferencesKey("bmd_feed_url")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { p ->
            UserPreferences(
                language = enumValueOrDefault(p[Keys.LANGUAGE], AppLanguage.ENGLISH),
                themeAppearance = enumValueOrDefault(p[Keys.THEME], ThemeAppearance.SYSTEM),
                animationLevel = enumValueOrDefault(p[Keys.ANIMATION], AnimationLevel.FULL),
                temperatureUnit = enumValueOrDefault(p[Keys.TEMP_UNIT], TemperatureUnit.CELSIUS),
                windUnit = enumValueOrDefault(p[Keys.WIND_UNIT], WindUnit.KMH),
                pressureUnit = enumValueOrDefault(p[Keys.PRESSURE_UNIT], PressureUnit.HPA),
                autoProvider = p[Keys.AUTO_PROVIDER] ?: true,
                selectedProvider = enumValueOrDefault(p[Keys.SELECTED_PROVIDER], ProviderId.OPEN_METEO),
                notificationSettings = NotificationSettings(
                    enabled = p[Keys.NOTIF_ENABLED] ?: true,
                    dailySummaryEnabled = p[Keys.NOTIF_DAILY] ?: true,
                    dailyHour = p[Keys.NOTIF_DAILY_HOUR] ?: 7,
                    dailyMinute = p[Keys.NOTIF_DAILY_MIN] ?: 30,
                    rainAlertsEnabled = p[Keys.NOTIF_RAIN] ?: true,
                    severeWeatherEnabled = p[Keys.NOTIF_SEVERE] ?: true,
                    aqiAlertsEnabled = p[Keys.NOTIF_AQI] ?: true,
                    disasterAlertsEnabled = p[Keys.NOTIF_DISASTER] ?: true,
                    minSeverityRank = p[Keys.NOTIF_MIN_SEV] ?: 1,
                    disasterRadiusKm = p[Keys.NOTIF_RADIUS] ?: 500
                ),
                ambientSound = enumValueOrDefault(p[Keys.AMBIENT_SOUND], AmbientSoundType.NONE),
                ambientVolume = p[Keys.AMBIENT_VOLUME] ?: 0.45f,
                ambientAutoStopMinutes = p[Keys.AMBIENT_TIMER] ?: 30,
                liveWallpaperStaticFallback = p[Keys.WALLPAPER_STATIC] ?: false,
                personalKeys = PersonalApiKeys(
                    weatherApiKey = p[Keys.KEY_WEATHERAPI] ?: "",
                    accuWeatherKey = p[Keys.KEY_ACCUWEATHER] ?: "",
                    tomorrowIoKey = p[Keys.KEY_TOMORROWIO] ?: "",
                    weatherbitKey = p[Keys.KEY_WEATHERBIT] ?: "",
                    googleWeatherKey = p[Keys.KEY_GOOGLE_WEATHER] ?: "",
                    personalAiEnabled = p[Keys.AI_ENABLED] ?: false,
                    personalAiEndpoint = p[Keys.AI_ENDPOINT] ?: "",
                    personalAiKey = p[Keys.AI_KEY] ?: "",
                    bmdCustomFeedUrl = p[Keys.BMD_FEED_URL] ?: ""
                )
            )
        }

    suspend fun updateLanguage(language: AppLanguage) = edit { it[Keys.LANGUAGE] = language.name }
    suspend fun updateTheme(theme: ThemeAppearance) = edit { it[Keys.THEME] = theme.name }
    suspend fun updateAnimationLevel(level: AnimationLevel) = edit { it[Keys.ANIMATION] = level.name }
    suspend fun updateTemperatureUnit(unit: TemperatureUnit) = edit { it[Keys.TEMP_UNIT] = unit.name }
    suspend fun updateWindUnit(unit: WindUnit) = edit { it[Keys.WIND_UNIT] = unit.name }
    suspend fun updatePressureUnit(unit: PressureUnit) = edit { it[Keys.PRESSURE_UNIT] = unit.name }
    suspend fun updateProviderSelection(auto: Boolean, provider: ProviderId) = edit {
        it[Keys.AUTO_PROVIDER] = auto
        it[Keys.SELECTED_PROVIDER] = provider.name
    }

    suspend fun updateNotificationSettings(s: NotificationSettings) = edit {
        it[Keys.NOTIF_ENABLED] = s.enabled
        it[Keys.NOTIF_DAILY] = s.dailySummaryEnabled
        it[Keys.NOTIF_DAILY_HOUR] = s.dailyHour
        it[Keys.NOTIF_DAILY_MIN] = s.dailyMinute
        it[Keys.NOTIF_RAIN] = s.rainAlertsEnabled
        it[Keys.NOTIF_SEVERE] = s.severeWeatherEnabled
        it[Keys.NOTIF_AQI] = s.aqiAlertsEnabled
        it[Keys.NOTIF_DISASTER] = s.disasterAlertsEnabled
        it[Keys.NOTIF_MIN_SEV] = s.minSeverityRank
        it[Keys.NOTIF_RADIUS] = s.disasterRadiusKm
    }

    suspend fun updateAmbientSettings(sound: AmbientSoundType, volume: Float, timerMin: Int) = edit {
        it[Keys.AMBIENT_SOUND] = sound.name
        it[Keys.AMBIENT_VOLUME] = volume
        it[Keys.AMBIENT_TIMER] = timerMin
    }

    suspend fun updateWallpaperMode(staticFallback: Boolean) = edit {
        it[Keys.WALLPAPER_STATIC] = staticFallback
    }

    suspend fun updatePersonalKeys(keys: PersonalApiKeys) = edit {
        it[Keys.KEY_WEATHERAPI] = keys.weatherApiKey
        it[Keys.KEY_ACCUWEATHER] = keys.accuWeatherKey
        it[Keys.KEY_TOMORROWIO] = keys.tomorrowIoKey
        it[Keys.KEY_WEATHERBIT] = keys.weatherbitKey
        it[Keys.KEY_GOOGLE_WEATHER] = keys.googleWeatherKey
        it[Keys.AI_ENABLED] = keys.personalAiEnabled
        it[Keys.AI_ENDPOINT] = keys.personalAiEndpoint
        it[Keys.AI_KEY] = keys.personalAiKey
        it[Keys.BMD_FEED_URL] = keys.bmdCustomFeedUrl
    }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(raw: String?, default: T): T {
        if (raw == null) return default
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(default)
    }
}
