package com.example.data.remote

import com.example.domain.calc.GeoUtils
import com.example.domain.model.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Bangladesh Meteorological Department (BMD) & BWOT (Bangladesh Weather Observation Team) Companion Adapter.
 * Connects to an authorized BMD/BWOT JSON feed if configured, and otherwise provides transparent,
 * strictly-labeled MODEL_ESTIMATE / CALCULATED Bangladesh-specific warnings (cyclone, Kalboishakhi
 * convective squall, lightning, heavy monsoon rain, heatwave, cold wave, marine/inland).
 */
class BmdProvider(private val client: NetworkClient) {

    suspend fun evaluateBangladeshStatus(
        location: SavedLocation,
        weather: WeatherBundle?,
        customBmdFeedUrl: String
    ): BmdBulletin {
        val isBd = GeoUtils.isBangladesh(
            location.countryCode,
            location.country,
            location.latitude,
            location.longitude
        )
        if (!isBd) {
            return BmdBulletin(
                isBangladeshLocation = false,
                hasOfficialLiveBulletin = false,
                stationOrRegion = location.name,
                bulletinOrigin = DataOrigin.CALCULATED,
                marineWarning = "Not applicable outside Bangladesh maritime & inland zone.",
                inlandWarning = "Select a Bangladesh location (e.g., Dhaka, Chattogram, Cox's Bazar, Sylhet) for BMD & BWOT Watch.",
                alerts = emptyList(),
                statusNote = "Outside Bangladesh territory",
                lastCheckedEpochMs = System.currentTimeMillis(),
                bwotWatchSummary = "BWOT Watch focuses on Bangladesh divisions & Bay of Bengal.",
                bwotSquallRisk = "N/A"
            )
        }

        val region = mapBangladeshDivision(location.latitude, location.longitude, location.name)
        val nowIso = SimpleDateFormat("yyyy-MM-dd HH:mm 'BST'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Dhaka")
        }.format(Date())

        // Attempt authorized custom BMD/BWOT JSON feed if configured by user/organization
        if (customBmdFeedUrl.isNotBlank() && customBmdFeedUrl.startsWith("http")) {
            val res = client.getString(customBmdFeedUrl.trim())
            if (res.isSuccess) {
                val parsed = runCatching {
                    parseAuthorizedBmdJson(res.getOrThrow(), region, nowIso, location)
                }.getOrNull()
                if (parsed != null) return parsed
            }
        }

        val alerts = mutableListOf<WeatherAlert>()
        var marineStatus = "Bay of Bengal (Model Estimate): Normal sea state; no cyclone-force wind signature detected."
        var inlandStatus = "Inland River Ports & Divisions ($region): No severe storm threshold exceeded."
        var bwotSquallRisk = "Low"
        var bwotSummary = "BWOT Convective Watch ($region): Stable boundary layer; monitor routine monsoon or diurnal showers."

        if (weather != null) {
            val cur = weather.current
            val next24 = weather.hourly.take(24)
            val maxTemp = weather.daily.firstOrNull()?.tempMaxC ?: cur.temperatureC
            val minTemp = weather.daily.firstOrNull()?.tempMinC ?: cur.temperatureC
            val maxGust = next24.mapNotNull { it.windGustKmh ?: it.windSpeedKmh }.maxOrNull() ?: cur.windSpeedKmh ?: 0.0
            val rain24h = next24.mapNotNull { it.precipitationMm }.sum()
            val hasThunder = next24.any { it.condition == WeatherCondition.THUNDERSTORM }
            val cape = cur.capeJoulesKg ?: 0.0

            // BWOT Convective / Kalboishakhi & Lightning Analysis
            when {
                maxGust >= 89.0 -> {
                    bwotSquallRisk = "Extreme (Cyclonic)"
                    bwotSummary = "BWOT Storm Watch: Cyclonic wind field (~${maxGust.toInt()} km/h gusts) near $region."
                }
                hasThunder || cape >= 1200.0 || maxGust >= 52.0 -> {
                    bwotSquallRisk = if (cape >= 1800.0 || maxGust >= 65.0) "High (Kalboishakhi / Squall)" else "Moderate (Convective)"
                    bwotSummary = "BWOT Kalboishakhi / Convective Watch ($region): CAPE ${cape.toInt()} J/kg, peak gusts ~${maxGust.toInt()} km/h. Elevated lightning & squall risk."
                    alerts += buildBdAlert(
                        id = "bwot_squall_${location.id}",
                        title = "BWOT Convective / Kalboishakhi Watch ($region)",
                        category = AlertCategory.THUNDERSTORM,
                        severity = if (maxGust >= 65.0 || cape >= 1800.0) AlertSeverity.SEVERE else AlertSeverity.MODERATE,
                        origin = DataOrigin.MODEL_ESTIMATE,
                        nowIso = nowIso,
                        loc = location,
                        sourceName = "BWOT & BMD Atmospheric Watch",
                        authority = "Model-derived BWOT Convective Criteria (verify bmd.gov.bd / BWOT)",
                        desc = bwotSummary
                    )
                }
                rain24h >= 30.0 -> {
                    bwotSquallRisk = "Rain / Monsoon Band"
                    bwotSummary = "BWOT Monsoon Moisture Watch ($region): ~${rain24h.toInt()} mm rainfall modeled over 24h."
                }
            }

            if (maxGust >= 89.0) {
                marineStatus = "Bay of Bengal Cyclone-Force Wind Signature (~${maxGust.toInt()} km/h model estimate). Verify official BMD Signal at bmd.gov.bd."
                alerts += buildBdAlert(
                    id = "bd_cyclone_${location.id}",
                    title = "Cyclone-Strength Wind Risk ($region)",
                    category = AlertCategory.CYCLONE,
                    severity = AlertSeverity.EXTREME,
                    origin = DataOrigin.MODEL_ESTIMATE,
                    nowIso = nowIso,
                    loc = location,
                    sourceName = "BMD & BWOT Threshold Engine",
                    authority = "Derived using BMD Maritime Thresholds (verify bmd.gov.bd)",
                    desc = "Forecast gusts reach ${maxGust.toInt()} km/h. Monitor Bangladesh Meteorological Department (bmd.gov.bd) official Maritime Signal."
                )
            } else if (maxGust >= 52.0 || hasThunder) {
                inlandStatus = "Kalboishakhi / Squall Wind & Thunderstorm Risk (~${maxGust.toInt()} km/h gusts)."
                alerts += buildBdAlert(
                    id = "bd_storm_${location.id}",
                    title = "Inland Squall & Thunderstorm Watch ($region)",
                    category = AlertCategory.THUNDERSTORM,
                    severity = AlertSeverity.MODERATE,
                    origin = DataOrigin.MODEL_ESTIMATE,
                    nowIso = nowIso,
                    loc = location,
                    sourceName = "Bangladesh Weather Threshold Engine",
                    authority = "Derived using BMD Thresholds (verify bmd.gov.bd)",
                    desc = "Convective thunderstorm / squall winds up to ${maxGust.toInt()} km/h modeled over $region."
                )
            }

            if (rain24h >= 44.0) {
                alerts += buildBdAlert(
                    id = "bd_heavy_rain_${location.id}",
                    title = "Heavy to Very Heavy Rainfall Watch ($region)",
                    category = AlertCategory.HEAVY_RAIN,
                    severity = if (rain24h >= 89.0) AlertSeverity.SEVERE else AlertSeverity.MODERATE,
                    origin = DataOrigin.MODEL_ESTIMATE,
                    nowIso = nowIso,
                    loc = location,
                    sourceName = "BMD & BWOT Rainfall Threshold Engine",
                    authority = "Derived using BMD Thresholds (>=44mm/24h)",
                    desc = "24-hour accumulated rainfall modeled at ${rain24h.toInt()} mm (BMD heavy rain threshold >= 44 mm)."
                )
            }

            if (maxTemp >= 36.0) {
                val tier = when {
                    maxTemp >= 42.0 -> "Very Severe Heatwave"
                    maxTemp >= 40.0 -> "Severe Heatwave"
                    maxTemp >= 38.0 -> "Moderate Heatwave"
                    else -> "Mild Heatwave"
                }
                alerts += buildBdAlert(
                    id = "bd_heatwave_${location.id}",
                    title = "$tier ($region)",
                    category = AlertCategory.EXTREME_HEAT,
                    severity = if (maxTemp >= 40.0) AlertSeverity.SEVERE else AlertSeverity.MODERATE,
                    origin = DataOrigin.CALCULATED,
                    nowIso = nowIso,
                    loc = location,
                    sourceName = "BMD Heatwave Threshold Engine",
                    authority = "Derived using BMD Heatwave Tiers (>=36°C)",
                    desc = "Peak daytime temperature reaches ${maxTemp.toInt()}°C, matching BMD $tier classification (>=36°C)."
                )
            }

            if (minTemp <= 10.0) {
                val tier = when {
                    minTemp <= 4.0 -> "Severe Cold Wave"
                    minTemp <= 6.0 -> "Moderate Cold Wave"
                    else -> "Mild Cold Wave"
                }
                alerts += buildBdAlert(
                    id = "bd_coldwave_${location.id}",
                    title = "$tier ($region)",
                    category = AlertCategory.EXTREME_COLD,
                    severity = if (minTemp <= 6.0) AlertSeverity.SEVERE else AlertSeverity.MODERATE,
                    origin = DataOrigin.CALCULATED,
                    nowIso = nowIso,
                    loc = location,
                    sourceName = "BMD Cold Wave Threshold Engine",
                    authority = "Derived using BMD Cold Wave Tiers (<=10°C)",
                    desc = "Minimum temperature drops to ${minTemp.toInt()}°C, matching BMD $tier criteria (<=10°C)."
                )
            }
        }

        return BmdBulletin(
            isBangladeshLocation = true,
            hasOfficialLiveBulletin = false,
            stationOrRegion = region,
            bulletinOrigin = DataOrigin.MODEL_ESTIMATE,
            marineWarning = marineStatus,
            inlandWarning = inlandStatus,
            alerts = alerts.distinctBy { it.id },
            statusNote = "Showing transparent BMD & BWOT Model/Calculated advisories. Connect an authorized BMD/BWOT JSON feed in Settings for official live bulletins.",
            lastCheckedEpochMs = System.currentTimeMillis(),
            bwotWatchSummary = bwotSummary,
            bwotSquallRisk = bwotSquallRisk
        )
    }

    private fun parseAuthorizedBmdJson(
        raw: String,
        region: String,
        nowIso: String,
        location: SavedLocation
    ): BmdBulletin {
        val root = JSONObject(raw)
        val marine = root.optString("marine_warning", "No active marine signal")
        val inland = root.optString("inland_warning", "No active inland river port warning")
        val bwotSum = root.optString("bwot_summary", "Authorized feed active for $region")
        val arr = root.optJSONArray("alerts")
        val list = mutableListOf<WeatherAlert>()
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list += WeatherAlert(
                    id = o.optString("id", "bmd_$i"),
                    title = o.optString("title", "BMD Official Bulletin"),
                    category = AlertCategory.SEVERE_WEATHER,
                    severity = AlertSeverity.SEVERE,
                    urgency = o.optString("urgency", "Expected"),
                    certainty = o.optString("certainty", "Observed"),
                    sourceName = "Bangladesh Meteorological Department (BMD)",
                    issuingAuthority = "BMD Storm Warning Center, Dhaka",
                    origin = DataOrigin.OFFICIAL_BMD,
                    issueTimeIso = o.optString("issued", nowIso),
                    startTimeIso = o.optString("start", nowIso),
                    expiryTimeIso = o.optString("expiry", "Until further notice"),
                    affectedArea = o.optString("area", region),
                    distanceKm = 0.0,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    description = o.optString("description", "")
                )
            }
        }
        return BmdBulletin(
            isBangladeshLocation = true,
            hasOfficialLiveBulletin = true,
            stationOrRegion = region,
            bulletinOrigin = DataOrigin.OFFICIAL_BMD,
            marineWarning = marine,
            inlandWarning = inland,
            alerts = list,
            statusNote = "Live authorized BMD/BWOT feed active",
            lastCheckedEpochMs = System.currentTimeMillis(),
            bwotWatchSummary = bwotSum,
            bwotSquallRisk = root.optString("squall_risk", "Live Feed")
        )
    }

    private fun buildBdAlert(
        id: String,
        title: String,
        category: AlertCategory,
        severity: AlertSeverity,
        origin: DataOrigin,
        nowIso: String,
        loc: SavedLocation,
        sourceName: String,
        authority: String,
        desc: String
    ) = WeatherAlert(
        id = id,
        title = title,
        category = category,
        severity = severity,
        urgency = "Expected",
        certainty = "Likely",
        sourceName = sourceName,
        issuingAuthority = authority,
        origin = origin,
        issueTimeIso = nowIso,
        startTimeIso = nowIso,
        expiryTimeIso = "+24h",
        affectedArea = loc.name,
        distanceKm = 0.0,
        latitude = loc.latitude,
        longitude = loc.longitude,
        description = desc
    )

    private fun mapBangladeshDivision(lat: Double, lon: Double, fallback: String): String = when {
        lat < 22.7 && lon > 91.2 -> "Chattogram & Cox's Bazar Coastal Zone"
        lat < 23.0 && lon <= 90.2 -> "Khulna & Sundarbans Coastal Zone"
        lat in 22.3..23.2 && lon in 90.0..91.1 -> "Barishal Riverine Zone"
        lat > 24.4 && lon > 91.2 -> "Sylhet Haor & Basin"
        lat > 24.6 && lon < 89.6 -> "Rangpur & Northern Basin"
        lat in 23.9..24.8 && lon in 88.2..90.0 -> "Rajshahi & Padma Basin"
        else -> "$fallback / Dhaka Central"
    }
}
