package com.example.data.repository

import com.example.data.local.CachedPayloadEntity
import com.example.data.local.SkyPulseDao
import com.example.data.remote.BmdProvider
import com.example.data.remote.DisasterAndAlertService
import com.example.data.remote.OpenMeteoProvider
import com.example.domain.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class AlertsAndDisastersBundle(
    val alerts: List<WeatherAlert>,
    val disasters: List<DisasterEvent>,
    val floodOutlook: FloodOutlook?,
    val tsunamiStatus: TsunamiStatus,
    val bmdBulletin: BmdBulletin,
    val isOfflineCache: Boolean,
    val fetchedEpochMs: Long
)

class DisasterAlertRepository(
    private val dao: SkyPulseDao,
    private val disasterService: DisasterAndAlertService,
    private val openMeteo: OpenMeteoProvider,
    private val bmdProvider: BmdProvider
) {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val disasterListType = Types.newParameterizedType(List::class.java, DisasterEvent::class.java)
    private val disasterAdapter = moshi.adapter<List<DisasterEvent>>(disasterListType)
    private val alertListType = Types.newParameterizedType(List::class.java, WeatherAlert::class.java)
    private val alertAdapter = moshi.adapter<List<WeatherAlert>>(alertListType)

    suspend fun loadAlertsAndDisasters(
        location: SavedLocation,
        weatherBundle: WeatherBundle?,
        customBmdFeedUrl: String
    ): AlertsAndDisastersBundle = withContext(Dispatchers.IO) {
        coroutineScope {
            val eqDeferred = async { disasterService.fetchUsgsEarthquakes(location.latitude, location.longitude) }
            val eonetDeferred = async { disasterService.fetchNasaEonet(location.latitude, location.longitude) }
            val gdacsDeferred = async { disasterService.fetchGdacsEvents(location.latitude, location.longitude) }
            val nwsDeferred = async { disasterService.fetchNwsAlerts(location.latitude, location.longitude) }
            val floodDeferred = async { openMeteo.fetchFloodOutlook(location.latitude, location.longitude).getOrNull() }
            val bmdDeferred = async { bmdProvider.evaluateBangladeshStatus(location, weatherBundle, customBmdFeedUrl) }

            val eq = eqDeferred.await()
            val eonet = eonetDeferred.await()
            val gdacs = gdacsDeferred.await()
            val nws = nwsDeferred.await()
            val flood = floodDeferred.await()
            val bmd = bmdDeferred.await()

            var mergedDisasters = (eq + gdacs + eonet)
                .distinctBy { it.id }
                .sortedWith(compareByDescending<DisasterEvent> { it.severity.rank }.thenBy { it.distanceKm ?: Double.MAX_VALUE })

            var isOffline = false
            val disasterCacheKey = "disasters_global"
            if (mergedDisasters.isEmpty()) {
                val cached = dao.getCachedPayload(disasterCacheKey)
                if (cached != null) {
                    mergedDisasters = runCatching { disasterAdapter.fromJson(cached.payloadJson) }.getOrNull().orEmpty()
                    if (mergedDisasters.isNotEmpty()) isOffline = true
                }
            } else {
                runCatching {
                    dao.putCachedPayload(CachedPayloadEntity(disasterCacheKey, "disasters", disasterAdapter.toJson(mergedDisasters), System.currentTimeMillis()))
                }
            }

            // Synthesize transparent MODEL_ESTIMATE alerts for severe local weather or flood anomalies
            val derivedAlerts = deriveWeatherAlerts(location, weatherBundle, flood)
            var mergedAlerts = (bmd.alerts + nws + derivedAlerts)
                .distinctBy { it.id }
                .sortedByDescending { it.severity.rank }

            val alertCacheKey = "alerts_${location.id}"
            if (mergedAlerts.isEmpty() && nws.isEmpty() && weatherBundle == null) {
                val cached = dao.getCachedPayload(alertCacheKey)
                if (cached != null) {
                    mergedAlerts = runCatching { alertAdapter.fromJson(cached.payloadJson) }.getOrNull().orEmpty()
                    if (mergedAlerts.isNotEmpty()) isOffline = true
                }
            } else {
                runCatching {
                    dao.putCachedPayload(CachedPayloadEntity(alertCacheKey, "alerts", alertAdapter.toJson(mergedAlerts), System.currentTimeMillis()))
                }
            }

            val tsunamiEvent = mergedDisasters.firstOrNull { it.tsunamiFlag || it.category == DisasterCategory.TSUNAMI }
            val tsunamiStatus = if (tsunamiEvent != null) {
                TsunamiStatus(
                    activeWarning = true,
                    statusLabel = "Tsunami Watch / Flag Active",
                    region = tsunamiEvent.locationName,
                    issueTimeIso = tsunamiEvent.eventTimeIso,
                    expiryTimeIso = "Active — Monitor PTWC/NOAA/USGS",
                    source = tsunamiEvent.source,
                    latitude = tsunamiEvent.latitude,
                    longitude = tsunamiEvent.longitude
                )
            } else {
                TsunamiStatus(
                    activeWarning = false,
                    statusLabel = "No Active Tsunami Warning",
                    region = "Global Pacific / Indian Ocean Basins",
                    issueTimeIso = SimpleDateFormat("yyyy-MM-dd HH:mm 'UTC'", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.format(Date()),
                    expiryTimeIso = "None",
                    source = "USGS / GDACS",
                    latitude = null,
                    longitude = null
                )
            }

            AlertsAndDisastersBundle(
                alerts = mergedAlerts,
                disasters = mergedDisasters,
                floodOutlook = flood,
                tsunamiStatus = tsunamiStatus,
                bmdBulletin = bmd,
                isOfflineCache = isOffline,
                fetchedEpochMs = System.currentTimeMillis()
            )
        }
    }

    private fun deriveWeatherAlerts(
        loc: SavedLocation,
        weather: WeatherBundle?,
        flood: FloodOutlook?
    ): List<WeatherAlert> {
        if (weather == null) return emptyList()
        val list = mutableListOf<WeatherAlert>()
        val nowIso = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        val cur = weather.current
        val maxGust = weather.hourly.take(24).mapNotNull { it.windGustKmh ?: it.windSpeedKmh }.maxOrNull() ?: 0.0
        val rain24 = weather.hourly.take(24).mapNotNull { it.precipitationMm }.sum()

        if (maxGust >= 62.0) {
            list += WeatherAlert(
                id = "derived_wind_${loc.id}",
                title = "Strong Gale / Storm Wind Advisory (${maxGust.toInt()} km/h)",
                category = if (maxGust >= 89.0) AlertCategory.CYCLONE else AlertCategory.STRONG_WIND,
                severity = if (maxGust >= 89.0) AlertSeverity.EXTREME else AlertSeverity.SEVERE,
                urgency = "Expected",
                certainty = "Likely",
                sourceName = "SkyPulse Meteorological Engine",
                issuingAuthority = "Forecast Model Detection",
                origin = DataOrigin.MODEL_ESTIMATE,
                issueTimeIso = nowIso,
                startTimeIso = nowIso,
                expiryTimeIso = "+24h",
                affectedArea = loc.name,
                distanceKm = 0.0,
                latitude = loc.latitude,
                longitude = loc.longitude,
                description = "Modeled peak wind gusts reach ${maxGust.toInt()} km/h over the next 24 hours."
            )
        }
        if (rain24 >= 50.0) {
            list += WeatherAlert(
                id = "derived_rain_${loc.id}",
                title = "Heavy Rainfall & Flash Flood Risk (${rain24.toInt()} mm/24h)",
                category = AlertCategory.HEAVY_RAIN,
                severity = if (rain24 >= 100.0) AlertSeverity.SEVERE else AlertSeverity.MODERATE,
                urgency = "Expected",
                certainty = "Likely",
                sourceName = "SkyPulse Meteorological Engine",
                issuingAuthority = "Forecast Model Detection",
                origin = DataOrigin.MODEL_ESTIMATE,
                issueTimeIso = nowIso,
                startTimeIso = nowIso,
                expiryTimeIso = "+24h",
                affectedArea = loc.name,
                distanceKm = 0.0,
                latitude = loc.latitude,
                longitude = loc.longitude,
                description = "Accumulated rainfall modeled at ${rain24.toInt()} mm in 24 hours."
            )
        }
        if ((cur.visibilityKm ?: 10.0) < 0.8) {
            list += WeatherAlert(
                id = "derived_fog_${loc.id}",
                title = "Dense Fog & Low Visibility (${String.format(Locale.US, "%.1f", cur.visibilityKm)} km)",
                category = AlertCategory.FOG,
                severity = AlertSeverity.MODERATE,
                urgency = "Immediate",
                certainty = "Observed",
                sourceName = "SkyPulse Meteorological Engine",
                issuingAuthority = "Visibility Telemetry",
                origin = DataOrigin.MODEL_ESTIMATE,
                issueTimeIso = nowIso,
                startTimeIso = nowIso,
                expiryTimeIso = "+6h",
                affectedArea = loc.name,
                distanceKm = 0.0,
                latitude = loc.latitude,
                longitude = loc.longitude,
                description = "Surface visibility below 1 km; exercise caution while driving or navigating."
            )
        }
        if (flood != null && flood.riskLevel.contains("Elevated", ignoreCase = true)) {
            list += WeatherAlert(
                id = "derived_flood_${loc.id}",
                title = "Elevated River Flood Outlook",
                category = AlertCategory.RIVER_FLOOD,
                severity = AlertSeverity.SEVERE,
                urgency = "Expected",
                certainty = "Model Estimate",
                sourceName = "Open-Meteo GloFAS Flood Model",
                issuingAuthority = "Hydrological Forecast Model",
                origin = DataOrigin.MODEL_ESTIMATE,
                issueTimeIso = nowIso,
                startTimeIso = nowIso,
                expiryTimeIso = "+7d",
                affectedArea = loc.name,
                distanceKm = 0.0,
                latitude = loc.latitude,
                longitude = loc.longitude,
                description = flood.summary
            )
        }
        return list
    }
}
