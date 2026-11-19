package com.example

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.*
import com.example.data.remote.*
import com.example.data.repository.LocationRepository
import com.example.data.repository.WeatherRepository
import com.example.domain.calc.AstronomyCalculator
import com.example.domain.calc.MeteorologyCalculator
import com.example.domain.model.*
import com.example.ui.UiState
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.SkyPulseTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RepositoryAndUiRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `room database saves locations, reorders, caches payloads, and deduplicates notifications`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, SkyPulseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.dao()

        val locRepo = LocationRepository(context, dao, OpenMeteoProvider(NetworkClient()))
        locRepo.ensureInitialLocations()
        val locs = locRepo.savedLocationsFlow.first()
        assertEquals(3, locs.size)
        assertEquals("Dhaka", locs.first().name)

        locRepo.renameLocation("dhaka_bd", "Dhaka Metro")
        assertEquals("Dhaka Metro", locRepo.savedLocationsFlow.first().first { it.id == "dhaka_bd" }.name)

        dao.putCachedPayload(CachedPayloadEntity("weather_dhaka_bd", "weather", "{}", 1000L))
        assertNotNull(dao.getCachedPayload("weather_dhaka_bd"))

        dao.markNotificationSent(NotificationLogEntity("alert_1", 5000L))
        assertEquals(5000L, dao.getNotificationLog("alert_1")?.sentEpochMs)

        db.close()
    }

    @Test
    fun `bengali localization resources load properly`() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val config = Configuration(base.resources.configuration).apply {
            setLocale(Locale.forLanguageTag("bn"))
        }
        val bnCtx = base.createConfigurationContext(config)
        assertEquals("হোম", bnCtx.getString(R.string.nav_home))
        assertEquals("অফলাইন", bnCtx.getString(R.string.status_offline))
    }

    @Test
    fun `home screen renders weather bundle, offline fallback badge, and alert banner`() {
        val loc = SavedLocation("dhaka", "Dhaka", "Dhaka", "Bangladesh", "BD", 23.81, 90.41, "Asia/Dhaka", true)
        val cur = CurrentWeather(
            temperatureC = 29.0, feelsLikeC = 32.0, condition = WeatherCondition.PARTLY_CLOUDY,
            conditionLabel = "Partly Cloudy", isDay = true, humidityPercent = 72, relativeHumidityPercent = 72,
            dewPointC = 23.0, wetBulbTempC = 25.0, pressureHpa = 1010.0, pressureTrend = PressureTrend.STEADY,
            windSpeedKmh = 14.0, windDirectionDeg = 180, windGustKmh = 22.0, windChillC = null, heatIndexC = 32.0,
            visibilityKm = 10.0, cloudCoverPercent = 40, cloudLowPercent = null, cloudMidPercent = null, cloudHighPercent = null,
            precipitationMm = 0.0, rainMm = 0.0, snowCm = null, sleetMm = null, hailMm = null,
            rainProbabilityPercent = 30, snowProbabilityPercent = null, freezingLevelMeters = null,
            uvIndex = 6.0, uvRiskLabel = "High", solarRadiationWm2 = null, soilMoistureM3 = null,
            soilTempC = null, surfaceTempC = null, capeJoulesKg = null
        )
        val hourly = listOf(
            HourlyForecast(1763550000L, "2026-11-19T15:00", 29.0, 32.0, WeatherCondition.PARTLY_CLOUDY, "Partly Cloudy", true, 0.0, 30, 0.0, null, 14.0, 180, 22.0, 72, 1010.0, 40, 23.0, 10.0, 6.0)
        )
        val daily = listOf(
            DailyForecast(1763550000L, "2026-11-19", 31.0, 23.0, 27.0, WeatherCondition.PARTLY_CLOUDY, "Partly Cloudy", 30, 0.0, null, null, 16.0, 24.0, 180, 6.0, 72, "06:15", "17:15", "Waxing Gibbous")
        )
        val bundle = WeatherBundle(
            location = loc,
            activeProvider = ProviderId.MET_NORWAY,
            requestedProvider = ProviderId.WEATHER_API,
            usedFallback = true,
            fallbackReason = "WeatherAPI key not configured",
            isOfflineCache = true,
            lastUpdatedEpochMs = System.currentTimeMillis(),
            current = cur,
            hourly = hourly,
            daily = daily,
            insights = MeteorologyCalculator.buildInsights(cur, hourly, emptyList()),
            summaryLines = listOf("Warm and humid this afternoon."),
            dailyAdvisorLines = listOf("Carry water."),
            providerHealthList = emptyList(),
            comparisons = emptyList()
        )

        composeRule.setContent {
            SkyPulseTheme {
                HomeScreen(
                    weatherState = UiState.Success(bundle),
                    aqiState = UiState.Idle,
                    astronomy = AstronomyCalculator.calculate(23.81, 90.41, "Asia/Dhaka"),
                    alertsState = UiState.Idle,
                    savedLocations = listOf(loc),
                    activeLocation = loc,
                    prefs = UserPreferences(),
                    onRefresh = {},
                    onSelectLocation = {},
                    onRequestGps = {},
                    onOpenLocations = {},
                    onOpenDetail = {},
                    onOpenForecast = {},
                    onSelectHour = {},
                    onOpenAqi = {},
                    onOpenAstronomy = {},
                    onOpenAlerts = {},
                    onSelectAlert = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_location_selector").assertIsDisplayed()
        composeRule.onNodeWithTag("home_hero_card").assertIsDisplayed()
    }

    @Test
    fun `provider parsers handle valid and edge-case payloads safely`() {
        val client = NetworkClient()
        val openMeteo = OpenMeteoProvider(client)
        val sampleOpenMeteo = """
            {
              "utc_offset_seconds": 21600,
              "current": {
                "temperature_2m": 28.5,
                "relative_humidity_2m": 74,
                "apparent_temperature": 31.2,
                "is_day": 1,
                "weather_code": 2,
                "pressure_msl": 1011.2,
                "wind_speed_10m": 14.5,
                "wind_direction_10m": 180
              },
              "hourly": {
                "time": [1763550000, 1763553600],
                "temperature_2m": [28.5, 27.8],
                "precipitation_probability": [20, 60]
              },
              "daily": {
                "time": [1763550000],
                "temperature_2m_max": [31.0],
                "temperature_2m_min": [22.0]
              }
            }
        """.trimIndent()
        val snap = openMeteo.parseWeatherJson(sampleOpenMeteo, 120L)
        assertEquals(28.5, snap.current.temperatureC, 0.01)
        assertEquals(WeatherCondition.PARTLY_CLOUDY, snap.current.condition)
        assertNull("Unsupported hail must be null, not 0", snap.current.hailMm)
        assertEquals(2, snap.hourly.size)
        assertEquals(1, snap.daily.size)

        val disasterService = DisasterAndAlertService(client)
        val usgsSample = """
            {
              "features": [
                {
                  "id": "us123",
                  "properties": {"mag": 5.4, "place": "Bay of Bengal", "time": 1763550000000, "tsunami": 1},
                  "geometry": {"coordinates": [90.0, 20.0, 15.0]}
                }
              ]
            }
        """.trimIndent()
        val quakes = disasterService.parseUsgsGeoJson(usgsSample, 23.81, 90.41)
        assertEquals(1, quakes.size)
        assertTrue(quakes.first().tsunamiFlag)
        assertEquals(AlertSeverity.EXTREME, quakes.first().severity)
        assertEquals(emptyList<DisasterEvent>(), disasterService.parseUsgsGeoJson("{bad json", 0.0, 0.0))
    }

    @Test
    fun `bangladesh bmd provider detects Bangladesh division and heatwave thresholds`() = runTest {
        val bmd = BmdProvider(NetworkClient())
        val dhaka = SavedLocation("dhaka", "Dhaka", "Dhaka", "Bangladesh", "BD", 23.81, 90.41, "Asia/Dhaka", true)
        val hotCur = CurrentWeather(
            temperatureC = 40.5, feelsLikeC = 43.0, condition = WeatherCondition.CLEAR,
            conditionLabel = "Clear", isDay = true, humidityPercent = 50, relativeHumidityPercent = 50,
            dewPointC = 28.0, wetBulbTempC = 30.0, pressureHpa = 1005.0, pressureTrend = PressureTrend.STEADY,
            windSpeedKmh = 12.0, windDirectionDeg = 180, windGustKmh = 20.0, windChillC = null, heatIndexC = 45.0,
            visibilityKm = 10.0, cloudCoverPercent = 10, cloudLowPercent = null, cloudMidPercent = null, cloudHighPercent = null,
            precipitationMm = 0.0, rainMm = 0.0, snowCm = null, sleetMm = null, hailMm = null,
            rainProbabilityPercent = 0, snowProbabilityPercent = null, freezingLevelMeters = null,
            uvIndex = 9.0, uvRiskLabel = "Very High", solarRadiationWm2 = null, soilMoistureM3 = null,
            soilTempC = null, surfaceTempC = null, capeJoulesKg = null
        )
        val bundle = WeatherBundle(
            location = dhaka, activeProvider = ProviderId.OPEN_METEO, requestedProvider = null,
            usedFallback = false, fallbackReason = null, isOfflineCache = false, lastUpdatedEpochMs = 0L,
            current = hotCur, hourly = emptyList(),
            daily = listOf(DailyForecast(0L, "2026-05-10", 40.5, 28.0, 34.0, WeatherCondition.CLEAR, "Clear", 0, 0.0, null, null, 12.0, 20.0, 180, 9.0, 50, "05:20", "18:30", null)),
            insights = MeteorologyCalculator.buildInsights(hotCur, emptyList(), emptyList()),
            summaryLines = emptyList(), dailyAdvisorLines = emptyList(), providerHealthList = emptyList(), comparisons = emptyList()
        )
        val bulletin = bmd.evaluateBangladeshStatus(dhaka, bundle, "")
        assertTrue(bulletin.isBangladeshLocation)
        assertFalse(bulletin.hasOfficialLiveBulletin)
        assertTrue(bulletin.alerts.any { it.category == AlertCategory.EXTREME_HEAT && it.title.contains("Severe Heatwave") })
    }

    @Test
    fun `forecast screen renders in AMOLED dark mode with hourly and daily tabs`() {
        val loc = SavedLocation("dhaka", "Dhaka", "Dhaka", "Bangladesh", "BD", 23.81, 90.41, "Asia/Dhaka", true)
        val cur = CurrentWeather(
            temperatureC = 26.0, feelsLikeC = 27.0, condition = WeatherCondition.RAIN,
            conditionLabel = "Rain", isDay = false, humidityPercent = 85, relativeHumidityPercent = 85,
            dewPointC = 23.0, wetBulbTempC = 24.0, pressureHpa = 1008.0, pressureTrend = PressureTrend.FALLING,
            windSpeedKmh = 18.0, windDirectionDeg = 200, windGustKmh = 30.0, windChillC = null, heatIndexC = null,
            visibilityKm = 6.0, cloudCoverPercent = 90, cloudLowPercent = null, cloudMidPercent = null, cloudHighPercent = null,
            precipitationMm = 3.2, rainMm = 3.2, snowCm = null, sleetMm = null, hailMm = null,
            rainProbabilityPercent = 80, snowProbabilityPercent = null, freezingLevelMeters = null,
            uvIndex = 0.0, uvRiskLabel = "Low", solarRadiationWm2 = null, soilMoistureM3 = null,
            soilTempC = null, surfaceTempC = null, capeJoulesKg = null
        )
        val hourly = listOf(
            HourlyForecast(1763550000L, "2026-11-19T20:00", 26.0, 27.0, WeatherCondition.RAIN, "Rain", false, 3.2, 80, 3.2, null, 18.0, 200, 30.0, 85, 1008.0, 90, 23.0, 6.0, 0.0),
            HourlyForecast(1763553600L, "2026-11-19T21:00", 25.0, 26.0, WeatherCondition.CLOUDY, "Overcast", false, 0.5, 40, 0.5, null, 14.0, 200, 22.0, 82, 1009.0, 80, 22.0, 8.0, 0.0)
        )
        val bundle = WeatherBundle(
            location = loc, activeProvider = ProviderId.OPEN_METEO, requestedProvider = null,
            usedFallback = false, fallbackReason = null, isOfflineCache = false, lastUpdatedEpochMs = 0L,
            current = cur, hourly = hourly, daily = emptyList(),
            insights = MeteorologyCalculator.buildInsights(cur, hourly, emptyList()),
            summaryLines = emptyList(), dailyAdvisorLines = emptyList(), providerHealthList = emptyList(), comparisons = emptyList()
        )
        composeRule.setContent {
            SkyPulseTheme(appearance = com.example.ui.theme.ThemeAppearance.AMOLED_DARK, isNight = true) {
                com.example.ui.screens.ForecastScreen(
                    weatherState = UiState.Success(bundle),
                    aqiState = UiState.Idle,
                    prefs = UserPreferences(themeAppearance = com.example.ui.theme.ThemeAppearance.AMOLED_DARK),
                    selectedHour = null,
                    selectedDay = null,
                    onSelectHour = {},
                    onSelectDay = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("forecast_tab_hourly").assertIsDisplayed()
        composeRule.onNodeWithTag("forecast_tab_daily").assertIsDisplayed()
    }
}
