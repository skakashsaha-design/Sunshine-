package com.example.data.repository

import com.example.BuildConfig
import com.example.data.local.CachedPayloadEntity
import com.example.data.local.SkyPulseDao
import com.example.data.remote.*
import com.example.domain.calc.MeteorologyCalculator
import com.example.domain.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeatherRepository(
    private val dao: SkyPulseDao,
    private val openMeteo: OpenMeteoProvider,
    private val metNorway: MetNorwayProvider,
    private val weatherApi: WeatherApiProvider,
    private val accuWeather: AccuWeatherProvider,
    private val tomorrowIo: TomorrowIoProvider,
    private val weatherbit: WeatherbitProvider,
    private val googleWeather: GoogleWeatherProvider,
    private val personalAiClient: PersonalAiAdvisorClient,
    private val radarService: RadarService
) {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val bundleAdapter = moshi.adapter(WeatherBundle::class.java)
    private val aqiAdapter = moshi.adapter(AirQualityData::class.java)
    private val radarAdapter = moshi.adapter(RadarTimeline::class.java)

    private val providers: Map<ProviderId, WeatherProvider> = mapOf(
        ProviderId.OPEN_METEO to openMeteo,
        ProviderId.MET_NORWAY to metNorway,
        ProviderId.WEATHER_API to weatherApi,
        ProviderId.ACCU_WEATHER to accuWeather,
        ProviderId.TOMORROW_IO to tomorrowIo,
        ProviderId.WEATHERBIT to weatherbit,
        ProviderId.GOOGLE_WEATHER to googleWeather
    )

    fun resolveApiKey(providerId: ProviderId, personal: PersonalApiKeys): String? = when (providerId) {
        ProviderId.WEATHER_API -> personal.weatherApiKey.takeIf { it.isConfiguredApiKey() }
            ?: BuildConfig.WEATHERAPI_KEY.takeIf { it.isConfiguredApiKey() }
        ProviderId.ACCU_WEATHER -> personal.accuWeatherKey.takeIf { it.isConfiguredApiKey() }
            ?: BuildConfig.ACCUWEATHER_KEY.takeIf { it.isConfiguredApiKey() }
        ProviderId.TOMORROW_IO -> personal.tomorrowIoKey.takeIf { it.isConfiguredApiKey() }
            ?: BuildConfig.TOMORROWIO_KEY.takeIf { it.isConfiguredApiKey() }
        ProviderId.WEATHERBIT -> personal.weatherbitKey.takeIf { it.isConfiguredApiKey() }
            ?: BuildConfig.WEATHERBIT_KEY.takeIf { it.isConfiguredApiKey() }
        ProviderId.GOOGLE_WEATHER -> personal.googleWeatherKey.takeIf { it.isConfiguredApiKey() }
            ?: BuildConfig.GOOGLE_WEATHER_API_KEY.takeIf { it.isConfiguredApiKey() }
        else -> null
    }

    suspend fun getWeather(
        location: SavedLocation,
        prefs: UserPreferences,
        forceRefresh: Boolean = false
    ): Result<WeatherBundle> = withContext(Dispatchers.IO) {
        val cacheKey = "weather_${location.id}"
        if (!forceRefresh) {
            val cached = dao.getCachedPayload(cacheKey)
            if (cached != null && System.currentTimeMillis() - cached.updatedEpochMs < 10 * 60 * 1000L) {
                val decoded = runCatching { bundleAdapter.fromJson(cached.payloadJson) }.getOrNull()
                if (decoded != null) return@withContext Result.success(decoded)
            }
        }

        val orderedChain = buildFallbackChain(prefs.autoProvider, prefs.selectedProvider)
        val healthList = mutableListOf<ProviderHealth>()
        var winningSnapshot: ProviderSnapshot? = null
        var fallbackReason: String? = null
        val requested = if (prefs.autoProvider) null else prefs.selectedProvider

        for ((idx, pid) in orderedChain.withIndex()) {
            val provider = providers[pid] ?: continue
            val key = resolveApiKey(pid, prefs.personalKeys)
            when (val res = provider.fetchWeather(location.latitude, location.longitude, location.timezoneId, key)) {
                is ProviderResult.Success -> {
                    healthList += ProviderHealth(
                        providerId = pid,
                        isAvailable = true,
                        isFallback = idx > 0,
                        lastSuccessEpochMs = res.snapshot.fetchedEpochMs,
                        responseTimeMs = res.snapshot.responseTimeMs,
                        errorMessage = null
                    )
                    if (winningSnapshot == null) {
                        winningSnapshot = res.snapshot
                        break
                    }
                }
                is ProviderResult.MissingApiKey -> {
                    healthList += ProviderHealth(
                        providerId = pid,
                        isAvailable = false,
                        isFallback = false,
                        lastSuccessEpochMs = null,
                        responseTimeMs = null,
                        errorMessage = res.message
                    )
                    if (idx == 0 && !prefs.autoProvider) {
                        fallbackReason = "${pid.displayName}: ${res.message}"
                    }
                }
                is ProviderResult.Failure -> {
                    healthList += ProviderHealth(
                        providerId = pid,
                        isAvailable = false,
                        isFallback = false,
                        lastSuccessEpochMs = null,
                        responseTimeMs = res.responseTimeMs,
                        errorMessage = res.reason
                    )
                    if (idx == 0 && fallbackReason == null) {
                        fallbackReason = "${pid.displayName} failed (${res.reason})"
                    }
                }
            }
        }

        if (winningSnapshot == null) {
            // Try offline Room cache regardless of age
            val cached = dao.getCachedPayload(cacheKey)
            val decoded = cached?.let { runCatching { bundleAdapter.fromJson(it.payloadJson) }.getOrNull() }
            if (decoded != null) {
                return@withContext Result.success(
                    decoded.copy(
                        isOfflineCache = true,
                        providerHealthList = healthList
                    )
                )
            }
            return@withContext Result.failure(
                IllegalStateException(fallbackReason ?: "All weather providers unreachable and no offline cache available.")
            )
        }

        // Enrich with AQI if available for summary/advisor
        val aqi = getAirQuality(location, forceRefresh = false).getOrNull()
        val insights = MeteorologyCalculator.buildInsights(winningSnapshot.current, winningSnapshot.hourly, emptyList())
        val summary = MeteorologyCalculator.generateHumanSummary(
            winningSnapshot.current,
            winningSnapshot.hourly,
            aqi,
            prefs.temperatureUnit
        )
        val advisor = MeteorologyCalculator.generateDailyAdvisor(
            winningSnapshot.current,
            winningSnapshot.hourly,
            aqi,
            emptyList(),
            emptyList()
        ).toMutableList()

        // Optional personal AI advisor enhancement (only if user configured personal key)
        if (prefs.personalKeys.personalAiEnabled && prefs.personalKeys.personalAiKey.isConfiguredApiKey()) {
            val contextPrompt = "${location.name}: ${summary.joinToString(" ")} AQI: ${aqi?.usAqi ?: "unknown"}"
            personalAiClient.generateOptionalPersonalBrief(prefs.personalKeys, contextPrompt)?.let { aiText ->
                advisor.add(0, "Personal AI Brief: $aiText")
            }
        }

        val usedFallback = (!prefs.autoProvider && winningSnapshot.providerId != prefs.selectedProvider) ||
            (prefs.autoProvider && fallbackReason != null)

        val bundle = WeatherBundle(
            location = location,
            activeProvider = winningSnapshot.providerId,
            requestedProvider = requested,
            usedFallback = usedFallback,
            fallbackReason = if (usedFallback) fallbackReason ?: "Switched to ${winningSnapshot.providerId.displayName}" else null,
            isOfflineCache = false,
            lastUpdatedEpochMs = winningSnapshot.fetchedEpochMs,
            current = winningSnapshot.current,
            hourly = winningSnapshot.hourly,
            daily = winningSnapshot.daily,
            insights = insights,
            summaryLines = summary,
            dailyAdvisorLines = advisor,
            providerHealthList = healthList,
            comparisons = emptyList()
        )

        runCatching {
            val json = bundleAdapter.toJson(bundle)
            dao.putCachedPayload(CachedPayloadEntity(cacheKey, "weather", json, bundle.lastUpdatedEpochMs))
        }

        Result.success(bundle)
    }

    suspend fun compareAllProviders(
        location: SavedLocation,
        prefs: UserPreferences
    ): List<ProviderComparisonEntry> = coroutineScope {
        val ids = listOf(
            ProviderId.OPEN_METEO,
            ProviderId.MET_NORWAY,
            ProviderId.WEATHER_API,
            ProviderId.ACCU_WEATHER,
            ProviderId.TOMORROW_IO,
            ProviderId.WEATHERBIT,
            ProviderId.GOOGLE_WEATHER
        )
        ids.map { pid ->
            async(Dispatchers.IO) {
                val p = providers[pid]!!
                val key = resolveApiKey(pid, prefs.personalKeys)
                when (val res = p.fetchWeather(location.latitude, location.longitude, location.timezoneId, key)) {
                    is ProviderResult.Success -> {
                        val s = res.snapshot
                        ProviderComparisonEntry(
                            providerId = pid,
                            status = "Online",
                            fetchedEpochMs = s.fetchedEpochMs,
                            responseTimeMs = s.responseTimeMs,
                            temperatureC = s.current.temperatureC,
                            feelsLikeC = s.current.feelsLikeC,
                            rainProbabilityPercent = s.current.rainProbabilityPercent,
                            precipitationMm = s.current.precipitationMm,
                            windSpeedKmh = s.current.windSpeedKmh,
                            humidityPercent = s.current.humidityPercent,
                            pressureHpa = s.current.pressureHpa,
                            uvIndex = s.current.uvIndex,
                            conditionLabel = s.current.conditionLabel,
                            error = null
                        )
                    }
                    is ProviderResult.MissingApiKey -> ProviderComparisonEntry(
                        providerId = pid,
                        status = "Key Required",
                        fetchedEpochMs = null,
                        responseTimeMs = null,
                        temperatureC = null,
                        feelsLikeC = null,
                        rainProbabilityPercent = null,
                        precipitationMm = null,
                        windSpeedKmh = null,
                        humidityPercent = null,
                        pressureHpa = null,
                        uvIndex = null,
                        conditionLabel = null,
                        error = res.message
                    )
                    is ProviderResult.Failure -> ProviderComparisonEntry(
                        providerId = pid,
                        status = "Error",
                        fetchedEpochMs = null,
                        responseTimeMs = res.responseTimeMs,
                        temperatureC = null,
                        feelsLikeC = null,
                        rainProbabilityPercent = null,
                        precipitationMm = null,
                        windSpeedKmh = null,
                        humidityPercent = null,
                        pressureHpa = null,
                        uvIndex = null,
                        conditionLabel = null,
                        error = res.reason
                    )
                }
            }
        }.awaitAll()
    }

    suspend fun getAirQuality(location: SavedLocation, forceRefresh: Boolean = false): Result<AirQualityData> =
        withContext(Dispatchers.IO) {
            val cacheKey = "aqi_${location.id}"
            if (!forceRefresh) {
                val cached = dao.getCachedPayload(cacheKey)
                if (cached != null && System.currentTimeMillis() - cached.updatedEpochMs < 20 * 60 * 1000L) {
                    val decoded = runCatching { aqiAdapter.fromJson(cached.payloadJson) }.getOrNull()
                    if (decoded != null) return@withContext Result.success(decoded)
                }
            }
            val res = openMeteo.fetchAirQuality(location.latitude, location.longitude)
            if (res.isSuccess) {
                val data = res.getOrThrow()
                runCatching {
                    dao.putCachedPayload(CachedPayloadEntity(cacheKey, "aqi", aqiAdapter.toJson(data), data.fetchedEpochMs))
                }
                return@withContext res
            }
            val fallback = dao.getCachedPayload(cacheKey)?.let {
                runCatching { aqiAdapter.fromJson(it.payloadJson) }.getOrNull()
            }
            if (fallback != null) Result.success(fallback) else res
        }

    suspend fun getRadarTimeline(): Result<RadarTimeline> = withContext(Dispatchers.IO) {
        val cacheKey = "radar_timeline"
        val res = radarService.fetchRainViewerTimeline()
        if (res.isSuccess) {
            val data = res.getOrThrow()
            runCatching {
                dao.putCachedPayload(CachedPayloadEntity(cacheKey, "radar", radarAdapter.toJson(data), System.currentTimeMillis()))
            }
            return@withContext res
        }
        val cached = dao.getCachedPayload(cacheKey)?.let {
            runCatching { radarAdapter.fromJson(it.payloadJson) }.getOrNull()
        }
        if (cached != null) Result.success(cached) else res
    }

    private fun buildFallbackChain(auto: Boolean, selected: ProviderId): List<ProviderId> {
        val all = listOf(
            ProviderId.OPEN_METEO,
            ProviderId.MET_NORWAY,
            ProviderId.WEATHER_API,
            ProviderId.ACCU_WEATHER,
            ProviderId.TOMORROW_IO,
            ProviderId.WEATHERBIT,
            ProviderId.GOOGLE_WEATHER
        )
        return if (auto) all else listOf(selected) + all.filter { it != selected }
    }
}
