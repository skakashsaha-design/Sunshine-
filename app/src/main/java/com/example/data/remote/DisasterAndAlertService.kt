package com.example.data.remote

import com.example.domain.calc.GeoUtils
import com.example.domain.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class DisasterAndAlertService(private val client: NetworkClient) {

    private val isoOut = SimpleDateFormat("yyyy-MM-dd HH:mm 'UTC'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    suspend fun fetchUsgsEarthquakes(userLat: Double, userLon: Double): List<DisasterEvent> {
        val url = "https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson&limit=60&minmagnitude=2.5&orderby=time"
        val body = client.getString(url).getOrNull() ?: return emptyList()
        return parseUsgsGeoJson(body, userLat, userLon)
    }

    fun parseUsgsGeoJson(raw: String, userLat: Double, userLon: Double): List<DisasterEvent> {
        return runCatching {
            val features = JSONObject(raw).optJSONArray("features") ?: return emptyList()
            buildList {
                for (i in 0 until features.length()) {
                    val f = features.getJSONObject(i)
                    val props = f.getJSONObject("properties")
                    val coords = f.getJSONObject("geometry").getJSONArray("coordinates")
                    val lon = coords.getDouble(0)
                    val lat = coords.getDouble(1)
                    val depthKm = coords.optDoubleOrNull(2)
                    val mag = props.optDoubleOrNull("mag") ?: 0.0
                    val tsunami = props.optInt("tsunami", 0) == 1
                    val timeMs = props.optLong("time", System.currentTimeMillis())
                    val place = props.optString("place", "Unknown seismic zone")
                    val dist = GeoUtils.haversineKm(userLat, userLon, lat, lon)
                    val sev = when {
                        mag >= 7.0 || tsunami -> AlertSeverity.EXTREME
                        mag >= 6.0 -> AlertSeverity.SEVERE
                        mag >= 4.5 -> AlertSeverity.MODERATE
                        else -> AlertSeverity.MINOR
                    }
                    add(
                        DisasterEvent(
                            id = f.optString("id", "usgs_$i"),
                            title = "M${String.format(Locale.US, "%.1f", mag)} Earthquake — $place",
                            category = if (tsunami) DisasterCategory.TSUNAMI else DisasterCategory.EARTHQUAKE,
                            severity = sev,
                            magnitudeOrIntensity = "M${String.format(Locale.US, "%.1f", mag)}",
                            depthKm = depthKm,
                            tsunamiFlag = tsunami,
                            locationName = place,
                            latitude = lat,
                            longitude = lon,
                            eventTimeIso = isoOut.format(Date(timeMs)),
                            eventEpochMs = timeMs,
                            distanceKm = dist,
                            source = "USGS Earthquake Hazards",
                            origin = DataOrigin.GLOBAL_OFFICIAL,
                            status = if (tsunami) "Tsunami Watch Flagged" else "Observed",
                            affectedRegion = place,
                            url = props.optString("url").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    suspend fun fetchNasaEonet(userLat: Double, userLon: Double): List<DisasterEvent> {
        val url = "https://eonet.gsfc.nasa.gov/api/v3/events?status=open&limit=60"
        val body = client.getString(url).getOrNull() ?: return emptyList()
        return parseNasaEonetJson(body, userLat, userLon)
    }

    fun parseNasaEonetJson(raw: String, userLat: Double, userLon: Double): List<DisasterEvent> {
        return runCatching {
            val events = JSONObject(raw).optJSONArray("events") ?: return emptyList()
            val isoIn = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            buildList {
                for (i in 0 until events.length()) {
                    val ev = events.getJSONObject(i)
                    val geom = ev.optJSONArray("geometry") ?: continue
                    if (geom.length() == 0) continue
                    val latest = geom.getJSONObject(geom.length() - 1)
                    val coords = latest.optJSONArray("coordinates") ?: continue
                    if (coords.length() < 2) continue
                    val lon = coords.optDouble(0, Double.NaN)
                    val lat = coords.optDouble(1, Double.NaN)
                    if (lat.isNaN() || lon.isNaN()) continue

                    val catTitle = ev.optJSONArray("categories")?.optJSONObject(0)?.optString("title", "Other") ?: "Other"
                    val cat = mapEonetCategory(catTitle)
                    val dateStr = latest.optString("date", "")
                    val epoch = runCatching { isoIn.parse(dateStr)?.time }.getOrNull() ?: System.currentTimeMillis()
                    val magVal = latest.optDoubleOrNull("magnitudeValue")
                    val magUnit = latest.optString("magnitudeUnit", "")
                    val magLabel = magVal?.let { "${it.toInt()} $magUnit".trim() }

                    add(
                        DisasterEvent(
                            id = ev.optString("id", "eonet_$i"),
                            title = ev.optString("title", cat.label),
                            category = cat,
                            severity = when (cat) {
                                DisasterCategory.VOLCANO, DisasterCategory.CYCLONE -> AlertSeverity.SEVERE
                                DisasterCategory.WILDFIRE, DisasterCategory.FLOOD, DisasterCategory.LANDSLIDE -> AlertSeverity.MODERATE
                                else -> AlertSeverity.MINOR
                            },
                            magnitudeOrIntensity = magLabel,
                            depthKm = null,
                            tsunamiFlag = false,
                            locationName = ev.optString("title", "Global Event"),
                            latitude = lat,
                            longitude = lon,
                            eventTimeIso = dateStr.ifBlank { isoOut.format(Date(epoch)) },
                            eventEpochMs = epoch,
                            distanceKm = GeoUtils.haversineKm(userLat, userLon, lat, lon),
                            source = "NASA EONET",
                            origin = DataOrigin.GLOBAL_OFFICIAL,
                            status = "Open / Active",
                            affectedRegion = catTitle,
                            url = ev.optString("link").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    suspend fun fetchGdacsEvents(userLat: Double, userLon: Double): List<DisasterEvent> {
        val url = "https://www.gdacs.org/gdacsapi/api/events/geteventlist/SEARCH?alertlevel=Green;Orange;Red&eventlist=EQ;TC;FL;VO;TS;DR"
        val body = client.getString(url).getOrNull() ?: return emptyList()
        return runCatching {
            val features = JSONObject(body).optJSONArray("features") ?: return emptyList()
            buildList {
                for (i in 0 until minOf(features.length(), 40)) {
                    val f = features.getJSONObject(i)
                    val props = f.getJSONObject("properties")
                    val coords = f.optJSONObject("geometry")?.optJSONArray("coordinates") ?: continue
                    val lon = coords.optDouble(0, Double.NaN)
                    val lat = coords.optDouble(1, Double.NaN)
                    if (lat.isNaN() || lon.isNaN()) continue
                    val eventType = props.optString("eventtype", "")
                    val alertLevel = props.optString("alertlevel", "Green")
                    val sev = when (alertLevel.lowercase()) {
                        "red" -> AlertSeverity.EXTREME
                        "orange" -> AlertSeverity.SEVERE
                        else -> AlertSeverity.MODERATE
                    }
                    val cat = when (eventType.uppercase()) {
                        "TC" -> DisasterCategory.CYCLONE
                        "FL" -> DisasterCategory.FLOOD
                        "VO" -> DisasterCategory.VOLCANO
                        "TS" -> DisasterCategory.TSUNAMI
                        "EQ" -> DisasterCategory.EARTHQUAKE
                        "DR" -> DisasterCategory.DROUGHT
                        else -> DisasterCategory.OTHER
                    }
                    add(
                        DisasterEvent(
                            id = "gdacs_${props.optString("eventid", i.toString())}",
                            title = props.optString("name", props.optString("htmldescription", cat.label)),
                            category = cat,
                            severity = sev,
                            magnitudeOrIntensity = props.optString("severity").takeIf { it.isNotBlank() },
                            depthKm = null,
                            tsunamiFlag = cat == DisasterCategory.TSUNAMI,
                            locationName = props.optString("country", "International"),
                            latitude = lat,
                            longitude = lon,
                            eventTimeIso = props.optString("fromdate", ""),
                            eventEpochMs = System.currentTimeMillis(),
                            distanceKm = GeoUtils.haversineKm(userLat, userLon, lat, lon),
                            source = "GDACS (UN/EC)",
                            origin = DataOrigin.GLOBAL_OFFICIAL,
                            status = "Alert Level: $alertLevel",
                            affectedRegion = props.optString("country", "Regional"),
                            url = props.optJSONObject("url")?.optString("report")?.takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    suspend fun fetchNwsAlerts(lat: Double, lon: Double): List<WeatherAlert> {
        val url = "https://api.weather.gov/alerts/active?point=${String.format(Locale.US, "%.4f,%.4f", lat, lon)}"
        val body = client.getString(url).getOrNull() ?: return emptyList()
        return runCatching {
            val features = JSONObject(body).optJSONArray("features") ?: return emptyList()
            buildList {
                for (i in 0 until features.length()) {
                    val p = features.getJSONObject(i).getJSONObject("properties")
                    val event = p.optString("event", "Weather Alert")
                    val sevStr = p.optString("severity", "Moderate")
                    val sev = when (sevStr.lowercase()) {
                        "extreme" -> AlertSeverity.EXTREME
                        "severe" -> AlertSeverity.SEVERE
                        "moderate" -> AlertSeverity.MODERATE
                        "minor" -> AlertSeverity.MINOR
                        else -> AlertSeverity.ADVISORY
                    }
                    add(
                        WeatherAlert(
                            id = p.optString("id", "nws_$i"),
                            title = p.optString("headline", event),
                            category = mapAlertCategory(event),
                            severity = sev,
                            urgency = p.optString("urgency", "Expected"),
                            certainty = p.optString("certainty", "Likely"),
                            sourceName = "NOAA / National Weather Service",
                            issuingAuthority = p.optString("senderName", "NWS"),
                            origin = DataOrigin.GLOBAL_OFFICIAL,
                            issueTimeIso = p.optString("sent", ""),
                            startTimeIso = p.optString("onset", ""),
                            expiryTimeIso = p.optString("expires", ""),
                            affectedArea = p.optString("areaDesc", "Local Zone"),
                            distanceKm = 0.0,
                            latitude = lat,
                            longitude = lon,
                            description = p.optString("description", ""),
                            instruction = p.optString("instruction").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun mapEonetCategory(title: String): DisasterCategory {
        val t = title.lowercase()
        return when {
            t.contains("wildfire") || t.contains("fire") -> DisasterCategory.WILDFIRE
            t.contains("volcano") -> DisasterCategory.VOLCANO
            t.contains("storm") || t.contains("cyclone") || t.contains("hurricane") -> DisasterCategory.SEVERE_STORM
            t.contains("flood") -> DisasterCategory.FLOOD
            t.contains("landslide") -> DisasterCategory.LANDSLIDE
            t.contains("drought") -> DisasterCategory.DROUGHT
            t.contains("dust") || t.contains("haze") -> DisasterCategory.EXTREME_WEATHER
            else -> DisasterCategory.OTHER
        }
    }

    private fun mapAlertCategory(event: String): AlertCategory {
        val e = event.lowercase()
        return when {
            e.contains("tornado") -> AlertCategory.TORNADO
            e.contains("hurricane") -> AlertCategory.HURRICANE
            e.contains("typhoon") -> AlertCategory.TYPHOON
            e.contains("cyclone") -> AlertCategory.CYCLONE
            e.contains("tropical") -> AlertCategory.TROPICAL_STORM
            e.contains("thunderstorm") -> AlertCategory.THUNDERSTORM
            e.contains("flash flood") -> AlertCategory.FLASH_FLOOD
            e.contains("coastal flood") -> AlertCategory.COASTAL_FLOOD
            e.contains("flood") -> AlertCategory.RIVER_FLOOD
            e.contains("storm surge") -> AlertCategory.STORM_SURGE
            e.contains("heat") -> AlertCategory.EXTREME_HEAT
            e.contains("freeze") || e.contains("cold") || e.contains("chill") -> AlertCategory.EXTREME_COLD
            e.contains("snow") || e.contains("blizzard") || e.contains("winter") -> AlertCategory.SNOW
            e.contains("ice") -> AlertCategory.ICE
            e.contains("wind") || e.contains("gale") -> AlertCategory.STRONG_WIND
            e.contains("fog") -> AlertCategory.FOG
            e.contains("dust") -> AlertCategory.DUST_STORM
            e.contains("fire") -> AlertCategory.WILDFIRE
            e.contains("tsunami") -> AlertCategory.TSUNAMI
            else -> AlertCategory.SEVERE_WEATHER
        }
    }
}
