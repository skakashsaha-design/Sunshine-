package com.example

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.*
import com.example.data.remote.*
import com.example.data.repository.LocationRepository
import com.example.domain.calc.*
import com.example.domain.model.*
import com.example.notification.MorningBriefingBuilder
import com.example.notification.NotificationHelper
import com.example.notification.NotificationIconResolver
import com.example.ui.UiState
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.SkyPulseTheme
import com.example.widget.GlanceSnapshotStore
import com.example.widget.GlanceWeatherSnapshot
import com.example.worker.MorningBriefingWorker
import java.util.Calendar
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
    fun `room database saves locations, trip plans, caches payloads, and deduplicates notifications`() = runTest {
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

        val trip = SavedTripPlan(
            id = "trip_1",
            title = "Rooftop Reception",
            locationName = "Dhaka",
            latitude = 23.81,
            longitude = 90.41,
            isoDate = "2026-11-22",
            activityType = TripActivityType.OUTDOOR_EVENT_HOSTING,
            timeWindow = EventTimeWindow.EVENING,
            lastScore = 88,
            lastVerdict = HostVerdict.IDEAL_GO,
            createdEpochMs = 12345L
        )
        dao.upsertTripPlan(TripPlanEntity.fromDomain(trip))
        val savedPlans = dao.observeTripPlans().first()
        assertEquals(1, savedPlans.size)
        assertEquals("Rooftop Reception", savedPlans.first().title)
        dao.deleteTripPlan("trip_1")
        assertTrue(dao.observeTripPlans().first().isEmpty())

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
    fun `home screen renders weather bundle, today ai analysis, and offline badge`() {
        val loc = SavedLocation("dhaka", "Dhaka", "Dhaka", "Bangladesh", "BD", 23.81, 90.41, "Asia/Dhaka", true)
        val cur = sampleCurrentWeather(29.0, 32.0, WeatherCondition.PARTLY_CLOUDY)
        val hourly = listOf(
            HourlyForecast(1763550000L, "2026-11-19T15:00", 29.0, 32.0, WeatherCondition.PARTLY_CLOUDY, "Partly Cloudy", true, 0.0, 30, 0.0, null, 14.0, 180, 22.0, 72, 1010.0, 40, 23.0, 10.0, 6.0)
        )
        val daily = listOf(
            DailyForecast(1763550000L, "2026-11-19", 31.0, 23.0, 27.0, WeatherCondition.PARTLY_CLOUDY, "Partly Cloudy", 30, 0.0, null, null, 16.0, 24.0, 180, 6.0, 72, "06:15", "17:15", "Waxing Gibbous")
        )
        val aiAnalysis = MeteorologyCalculator.buildTodayAiAnalysis(cur, hourly, daily, null, TemperatureUnit.CELSIUS)
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
            comparisons = emptyList(),
            todayAiAnalysis = aiAnalysis
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
                    onSelectAlert = {},
                    onOpenTripPlanner = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_location_selector").assertIsDisplayed()
        composeRule.onNodeWithTag("home_hero_card").assertIsDisplayed()
        composeRule.onNodeWithTag("home_feed_list").performScrollToNode(hasTestTag("home_today_ai_card"))
        composeRule.onNodeWithTag("home_today_ai_card").assertIsDisplayed()
        composeRule.onNodeWithTag("home_feed_list").performScrollToNode(hasTestTag("home_trip_planner_card"))
        composeRule.onNodeWithTag("home_trip_planner_card").assertIsDisplayed()
    }

    @Test
    fun `open meteo retrofit dto maps full telemetry and excludes unsupported hail`() {
        val dto = OpenMeteoForecastDto(
            utcOffsetSeconds = 21600,
            current = OpenMeteoCurrentDto(
                temperature2m = 27.4,
                relativeHumidity2m = 68,
                apparentTemperature = 29.8,
                isDay = 1,
                weatherCode = 2,
                pressureMsl = 1012.5,
                windSpeed10m = 13.0,
                windDirection10m = 135,
                cape = 850.0,
                soilTemperature0cm = 25.0,
                soilMoisture0To1cm = 0.24
            ),
            hourly = OpenMeteoHourlyDto(
                time = listOf(1763550000L, 1763553600L),
                temperature2m = listOf(27.4, 26.2),
                precipitationProbability = listOf(15, 25)
            ),
            daily = OpenMeteoDailyDto(
                time = listOf(1763550000L),
                temperature2mMax = listOf(30.2),
                temperature2mMin = listOf(21.4)
            )
        )
        val snap = dto.toProviderSnapshot(95L)
        assertEquals(ProviderId.OPEN_METEO, snap.providerId)
        assertEquals(27.4, snap.current.temperatureC, 0.01)
        assertEquals(850.0, snap.current.capeJoulesKg ?: 0.0, 0.1)
        assertNull("Unsupported hail must be null", snap.current.hailMm)
        assertEquals(2, snap.hourly.size)
        assertEquals(1, snap.daily.size)
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

        val extService = ExtendedWeatherService(client)
        val modelJson = """
            {
              "current": {
                "temperature_2m_ecmwf_ifs025": 28.1,
                "wind_speed_10m_ecmwf_ifs025": 12.0,
                "temperature_2m_gfs_seamless": 28.6,
                "wind_speed_10m_gfs_seamless": 14.0,
                "temperature_2m_icon_seamless": 28.3,
                "wind_speed_10m_icon_seamless": 13.0
              },
              "hourly": {
                "precipitation_probability_ecmwf_ifs025": [20],
                "precipitation_probability_gfs_seamless": [30]
              }
            }
        """.trimIndent()
        val cmp = extService.parseModelComparisonJson(modelJson)
        assertEquals(3, cmp.models.size)
        assertEquals("Model agreement: High", cmp.agreementLabel)
        assertEquals(0.5, cmp.ensembleSpreadC ?: -1.0, 0.01)

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
    fun `trip planner scores favorable vs thunderstorm dates accurately`() {
        val loc = SavedLocation("dhaka", "Dhaka", "Dhaka", "Bangladesh", "BD", 23.81, 90.41, "Asia/Dhaka", true)
        val cur = sampleCurrentWeather(25.0, 25.0, WeatherCondition.CLEAR)
        val sunnyDay = DailyForecast(1L, "2026-11-22", 27.0, 18.0, 22.0, WeatherCondition.CLEAR, "Clear", 10, 0.0, null, null, 10.0, 16.0, 90, 4.0, 50, "06:15", "17:15", null)
        val stormDay = DailyForecast(2L, "2026-11-23", 38.0, 26.0, 32.0, WeatherCondition.THUNDERSTORM, "Thunderstorm", 90, 35.0, null, null, 50.0, 72.0, 180, 9.0, 88, "06:15", "17:15", null)
        val bundle = WeatherBundle(
            location = loc, activeProvider = ProviderId.OPEN_METEO, requestedProvider = null,
            usedFallback = false, fallbackReason = null, isOfflineCache = false, lastUpdatedEpochMs = 0L,
            current = cur, hourly = emptyList(), daily = listOf(sunnyDay, stormDay),
            insights = MeteorologyCalculator.buildInsights(cur, emptyList(), emptyList()),
            summaryLines = emptyList(), dailyAdvisorLines = emptyList(), providerHealthList = emptyList(), comparisons = emptyList()
        )

        val goodEval = TripPlannerCalculator.evaluateDay(bundle, sunnyDay, TripActivityType.OUTDOOR_EVENT_HOSTING, EventTimeWindow.AFTERNOON, null, emptyList(), TemperatureUnit.CELSIUS)
        assertEquals(HostVerdict.IDEAL_GO, goodEval.verdict)
        assertTrue(goodEval.score >= 80)

        val badEval = TripPlannerCalculator.evaluateDay(bundle, stormDay, TripActivityType.OUTDOOR_EVENT_HOSTING, EventTimeWindow.AFTERNOON, null, emptyList(), TemperatureUnit.CELSIUS)
        assertEquals(HostVerdict.HIGH_RISK_RESCHEDULE, badEval.verdict)
        assertTrue(badEval.score < 40)
    }

    @Test
    fun `emergency dataset includes country flags, kelvin unit, beaufort scale, and glance store`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bdNumbers = EmergencyNumbersDataset.forCountry("BD")
        assertTrue(bdNumbers.any { it.number == "999" })
        assertTrue(GeoUtils.countryCodeToFlagEmoji("BD").isNotEmpty())
        assertTrue(GeoUtils.formatTemperature(27.0, TemperatureUnit.KELVIN).endsWith("K"))
        assertEquals(0 to "Calm", GeoUtils.beaufortScale(0.5))
        assertEquals(12 to "Hurricane Force", GeoUtils.beaufortScale(125.0))
        assertNotNull(MetricGlossary.find("dew_point")?.explanationBn)

        val profile = PerformanceHelper.resolveProfile(context, PerformanceMode.LOW_RAM_1GB, AnimationLevel.FULL)
        assertTrue(profile.isLowRamActive)
        assertEquals(AnimationLevel.MINIMAL, profile.effectiveAnimation)
        assertEquals(6, profile.maxRadarFrames)

        GlanceSnapshotStore.save(
            context,
            GlanceWeatherSnapshot(city = "Dhaka", temperatureDisplay = "29°C", conditionLabel = "Clear", aqiDisplay = "AQI 72")
        )
        val restored = GlanceSnapshotStore.read(context)
        assertEquals("Dhaka", restored.city)
        assertEquals("29°C", restored.temperatureDisplay)
    }

    @Test
    fun `bangladesh bmd provider detects Bangladesh division and heatwave thresholds`() = runTest {
        val bmd = BmdProvider(NetworkClient())
        val dhaka = SavedLocation("dhaka", "Dhaka", "Dhaka", "Bangladesh", "BD", 23.81, 90.41, "Asia/Dhaka", true)
        val hotCur = sampleCurrentWeather(40.5, 43.0, WeatherCondition.CLEAR).copy(heatIndexC = 45.0, uvIndex = 9.0, humidityPercent = 50)
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
        val cur = sampleCurrentWeather(26.0, 27.0, WeatherCondition.RAIN, isDay = false)
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

    @Test
    fun `notification icon resolver assigns distinct icons across weather and disaster categories`() {
        val clearIcon = NotificationIconResolver.forWeatherCondition(WeatherCondition.CLEAR).smallIconRes
        val rainIcon = NotificationIconResolver.forWeatherCondition(WeatherCondition.RAIN).smallIconRes
        val stormIcon = NotificationIconResolver.forWeatherCondition(WeatherCondition.THUNDERSTORM).smallIconRes
        val snowIcon = NotificationIconResolver.forWeatherCondition(WeatherCondition.SNOW).smallIconRes
        val windIcon = NotificationIconResolver.forWeatherCondition(WeatherCondition.WINDY).smallIconRes
        assertEquals(5, setOf(clearIcon, rainIcon, stormIcon, snowIcon, windIcon).size)

        val quakeIcon = NotificationIconResolver.forDisasterCategory(DisasterCategory.EARTHQUAKE).smallIconRes
        val tsunamiIcon = NotificationIconResolver.forDisasterCategory(DisasterCategory.TSUNAMI).smallIconRes
        val floodIcon = NotificationIconResolver.forDisasterCategory(DisasterCategory.FLOOD).smallIconRes
        val fireIcon = NotificationIconResolver.forDisasterCategory(DisasterCategory.WILDFIRE).smallIconRes
        val volcanoIcon = NotificationIconResolver.forDisasterCategory(DisasterCategory.VOLCANO).smallIconRes
        val landslideIcon = NotificationIconResolver.forDisasterCategory(DisasterCategory.LANDSLIDE).smallIconRes
        val cycloneIcon = NotificationIconResolver.forDisasterCategory(DisasterCategory.CYCLONE).smallIconRes
        assertEquals(
            7,
            setOf(quakeIcon, tsunamiIcon, floodIcon, fireIcon, volcanoIcon, landslideIcon, cycloneIcon).size
        )

        val alertFlood = NotificationIconResolver.forAlertCategory(AlertCategory.FLASH_FLOOD).smallIconRes
        val alertHeat = NotificationIconResolver.forAlertCategory(AlertCategory.EXTREME_HEAT).smallIconRes
        val alertDust = NotificationIconResolver.forAlertCategory(AlertCategory.DUST_STORM).smallIconRes
        assertEquals(3, setOf(alertFlood, alertHeat, alertDust).size)
    }

    @Test
    fun `morning briefing builder and scheduler synthesize collected data and compute daily delay`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val loc = SavedLocation("dhaka_gps", "Dhaka", "Dhaka", "Bangladesh", "BD", 23.81, 90.41, "Asia/Dhaka", true, isCurrentGps = true)
        val cur = sampleCurrentWeather(24.0, 26.0, WeatherCondition.CLEAR)
        val hourly = listOf(
            HourlyForecast(1763600000L, "2026-11-20T09:00", 26.0, 27.0, WeatherCondition.PARTLY_CLOUDY, "Partly Cloudy", true, 0.2, 55, 0.2, null, 12.0, 180, 20.0, 68, 1012.0, 30, 20.0, 10.0, 5.0)
        )
        val daily = listOf(
            DailyForecast(1763600000L, "2026-11-20", 30.0, 20.0, 25.0, WeatherCondition.CLEAR, "Clear", 55, 1.0, null, null, 15.0, 24.0, 180, 7.0, 65, "06:16", "17:15", "Waxing Gibbous")
        )
        val bundle = WeatherBundle(
            location = loc, activeProvider = ProviderId.OPEN_METEO, requestedProvider = null,
            usedFallback = false, fallbackReason = null, isOfflineCache = false, lastUpdatedEpochMs = 0L,
            current = cur, hourly = hourly, daily = daily,
            insights = MeteorologyCalculator.buildInsights(cur, hourly, emptyList()),
            summaryLines = listOf("Clear morning"), dailyAdvisorLines = listOf("Carry light sun protection."),
            providerHealthList = emptyList(), comparisons = emptyList(),
            todayAiAnalysis = MeteorologyCalculator.buildTodayAiAnalysis(cur, hourly, daily, null, TemperatureUnit.CELSIUS)
        )
        val astro = AstronomyCalculator.calculate(23.81, 90.41, "Asia/Dhaka")

        val clearBriefing = MorningBriefingBuilder.build(
            weather = bundle, aqi = null, astronomy = astro,
            alerts = emptyList(), disasters = emptyList(),
            prefs = UserPreferences(), isCurrentGpsLocation = true
        )
        assertTrue(clearBriefing.title.contains("Morning Briefing"))
        assertTrue(clearBriefing.title.contains("Dhaka (GPS)"))
        assertTrue(clearBriefing.fullBriefingText.contains("Sunrise"))
        assertEquals(R.drawable.ic_notif_sun_clear, clearBriefing.iconSpec.smallIconRes)

        val floodAlert = WeatherAlert(
            id = "flood_1", title = "River Flood Warning", category = AlertCategory.FLASH_FLOOD,
            severity = AlertSeverity.SEVERE, urgency = "Immediate", certainty = "Likely",
            sourceName = "FFWC", issuingAuthority = "FFWC", origin = DataOrigin.OFFICIAL_BMD,
            issueTimeIso = "2026-11-20T06:00", startTimeIso = "2026-11-20T06:00", expiryTimeIso = "2026-11-20T18:00",
            affectedArea = "Dhaka Basin", distanceKm = 12.0, latitude = 23.81, longitude = 90.41,
            description = "Water levels rising."
        )
        val warnBriefing = MorningBriefingBuilder.build(
            weather = bundle, aqi = null, astronomy = astro,
            alerts = listOf(floodAlert), disasters = emptyList(),
            prefs = UserPreferences(), isCurrentGpsLocation = true
        )
        assertEquals(R.drawable.ic_notif_flood, warnBriefing.iconSpec.smallIconRes)
        assertTrue(warnBriefing.fullBriefingText.contains("River Flood Warning"))

        val beforeTarget = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 20, 6, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val delaySameDay = MorningBriefingWorker.computeInitialDelayMs(7, 30, beforeTarget)
        assertEquals(60 * 60 * 1000L, delaySameDay)

        val afterTarget = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 20, 8, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val delayNextDay = MorningBriefingWorker.computeInitialDelayMs(7, 30, afterTarget)
        assertEquals(23 * 60 * 60 * 1000L, delayNextDay)

        val db = Room.inMemoryDatabaseBuilder(context, SkyPulseDatabase::class.java).allowMainThreadQueries().build()
        val helper = NotificationHelper(context, db.dao())
        // Ensure icon bitmaps render cleanly without crash
        assertNotNull(NotificationIconResolver.createLargeBadgeBitmap(context, warnBriefing.iconSpec))
        val postedFirst = helper.dispatchMorningBriefingNotification(
            weather = bundle, aqi = null, astronomy = astro,
            alerts = emptyList(), disasters = emptyList(),
            prefs = UserPreferences(), isCurrentGpsLocation = true, respectDailyDedupe = true
        )
        // Robolectric grants or denies permission; verify dedupe safety doesn't throw
        if (postedFirst) {
            val postedSecond = helper.dispatchMorningBriefingNotification(
                weather = bundle, aqi = null, astronomy = astro,
                alerts = emptyList(), disasters = emptyList(),
                prefs = UserPreferences(), isCurrentGpsLocation = true, respectDailyDedupe = true
            )
            assertFalse("Scheduled morning briefing should deduplicate same calendar day", postedSecond)
        }
        db.close()
    }

    private fun sampleCurrentWeather(
        tempC: Double,
        feelsC: Double,
        condition: WeatherCondition,
        isDay: Boolean = true
    ) = CurrentWeather(
        temperatureC = tempC, feelsLikeC = feelsC, condition = condition,
        conditionLabel = condition.name.replace('_', ' '), isDay = isDay, humidityPercent = 72, relativeHumidityPercent = 72,
        dewPointC = 23.0, wetBulbTempC = 25.0, pressureHpa = 1010.0, pressureTrend = PressureTrend.STEADY,
        windSpeedKmh = 14.0, windDirectionDeg = 180, windGustKmh = 22.0, windChillC = null, heatIndexC = 32.0,
        visibilityKm = 10.0, cloudCoverPercent = 40, cloudLowPercent = null, cloudMidPercent = null, cloudHighPercent = null,
        precipitationMm = 0.0, rainMm = 0.0, snowCm = null, sleetMm = null, hailMm = null,
        rainProbabilityPercent = 30, snowProbabilityPercent = null, freezingLevelMeters = null,
        uvIndex = 6.0, uvRiskLabel = "High", solarRadiationWm2 = null, soilMoistureM3 = null,
        soilTempC = null, surfaceTempC = null, capeJoulesKg = null
    )
}
