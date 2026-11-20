package com.example.data.remote

import com.example.domain.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.abs

class ExtendedWeatherService(private val client: NetworkClient) {

    val commonAirports: List<AviationAirportPreset> = listOf(
        AviationAirportPreset("DAC", "VGHS", "Hazrat Shahjalal Int'l (Dhaka)", "BD"),
        AviationAirportPreset("CGP", "VGEG", "Shah Amanat Int'l (Chattogram)", "BD"),
        AviationAirportPreset("ZYL", "VGSY", "Osmani Int'l (Sylhet)", "BD"),
        AviationAirportPreset("CXB", "VGCB", "Cox's Bazar Airport", "BD"),
        AviationAirportPreset("DEL", "VIDP", "Indira Gandhi Int'l (New Delhi)", "IN"),
        AviationAirportPreset("BOM", "VABB", "Chhatrapati Shivaji (Mumbai)", "IN"),
        AviationAirportPreset("CCU", "VECC", "Netaji Subhas Chandra Bose (Kolkata)", "IN"),
        AviationAirportPreset("HND", "RJTT", "Tokyo Haneda Airport", "JP"),
        AviationAirportPreset("NRT", "RJAA", "Tokyo Narita Int'l", "JP"),
        AviationAirportPreset("SIN", "WSSS", "Singapore Changi Airport", "SG"),
        AviationAirportPreset("DXB", "OMDB", "Dubai International", "AE"),
        AviationAirportPreset("LHR", "EGLL", "London Heathrow", "GB"),
        AviationAirportPreset("JFK", "KJFK", "New York John F. Kennedy", "US")
    )

    suspend fun fetchModelComparison(lat: Double, lon: Double): Result<ModelComparisonResult> {
        val url = "https://api.open-meteo.com/v1/forecast?" +
            "latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,cloud_cover,pressure_msl,wind_speed_10m" +
            "&hourly=precipitation_probability&forecast_days=1" +
            "&models=ecmwf_ifs025,gfs_seamless,icon_seamless,jma_seamless,gem_seamless"
        val res = client.getString(url)
        return res.mapCatching { body -> parseModelComparisonJson(body) }
    }

    fun parseModelComparisonJson(body: String): ModelComparisonResult {
        val root = JSONObject(body)
        val cur = root.optJSONObject("current") ?: JSONObject()
        val hourly = root.optJSONObject("hourly") ?: JSONObject()

        val modelSpecs = listOf(
            "ECMWF IFS" to "ecmwf_ifs025",
            "NOAA GFS" to "gfs_seamless",
            "DWD ICON" to "icon_seamless",
            "JMA Seamless" to "jma_seamless",
            "CMC GEM" to "gem_seamless"
        )

        val entries = modelSpecs.mapNotNull { (displayName, suffix) ->
            val tKey = "temperature_2m_$suffix"
            if (!cur.has(tKey) && !cur.has("temperature_2m")) return@mapNotNull null
            val temp = cur.optDoubleOrNull(tKey) ?: cur.optDoubleOrNull("temperature_2m")
            val feels = cur.optDoubleOrNull("apparent_temperature_$suffix")
            val hum = cur.optIntOrNull("relative_humidity_2m_$suffix")
            val precip = cur.optDoubleOrNull("precipitation_$suffix")
            val cloud = cur.optIntOrNull("cloud_cover_$suffix")
            val press = cur.optDoubleOrNull("pressure_msl_$suffix")
            val wind = cur.optDoubleOrNull("wind_speed_10m_$suffix")
            val probArr = hourly.optJSONArray("precipitation_probability_$suffix")
            val prob = if (probArr != null && probArr.length() > 0 && !probArr.isNull(0)) {
                probArr.optInt(0)
            } else null

            if (temp == null && wind == null && press == null) return@mapNotNull null
            ModelForecastEntry(
                modelName = displayName,
                modelCode = suffix,
                temperatureC = temp,
                feelsLikeC = feels,
                rainProbabilityPercent = prob,
                precipitationMm = precip,
                windSpeedKmh = wind,
                pressureHpa = press,
                cloudCoverPercent = cloud,
                humidityPercent = hum
            )
        }

        val temps = entries.mapNotNull { it.temperatureC }.sorted()
        val minT = temps.firstOrNull()
        val maxT = temps.lastOrNull()
        val medT = if (temps.isNotEmpty()) temps[temps.size / 2] else null
        val spread = if (minT != null && maxT != null) maxT - minT else null

        val winds = entries.mapNotNull { it.windSpeedKmh }
        val windSpread = if (winds.size >= 2) (winds.max() - winds.min()) else 0.0

        val (agreement, confidence, explain) = when {
            entries.size < 2 -> Triple(
                "Single-model snapshot",
                "Moderate confidence",
                "App-derived indicator: fewer than two global models returned overlapping fields."
            )
            (spread ?: 0.0) <= 1.5 && windSpread <= 6.0 -> Triple(
                "Model agreement: High",
                "High confidence",
                "App-derived indicator (not an official warning): ${entries.size} global models agree within ${String.format(Locale.US, "%.1f", spread ?: 0.0)}°C."
            )
            (spread ?: 0.0) <= 3.5 -> Triple(
                "Model agreement: Moderate",
                "Moderate confidence",
                "App-derived indicator: slight temperature/wind spread (${String.format(Locale.US, "%.1f", spread ?: 0.0)}°C across models)."
            )
            else -> Triple(
                "Model disagreement: High",
                "Low confidence",
                "App-derived indicator: wide ${String.format(Locale.US, "%.1f", spread ?: 0.0)}°C spread indicates higher forecast uncertainty."
            )
        }

        return ModelComparisonResult(
            models = entries,
            agreementLabel = agreement,
            confidenceLabel = confidence,
            confidenceExplanation = explain,
            ensembleMinTempC = minT,
            ensembleMedianTempC = medT,
            ensembleMaxTempC = maxT,
            ensembleSpreadC = spread,
            fetchedEpochMs = System.currentTimeMillis()
        )
    }

    suspend fun fetchMarineWeather(lat: Double, lon: Double, windKmh: Double?, gustKmh: Double?, pressureHpa: Double?): Result<MarineWeatherData> {
        val url = "https://marine-api.open-meteo.com/v1/marine?" +
            "latitude=$lat&longitude=$lon" +
            "&current=wave_height,wave_direction,wave_period,swell_wave_height,sea_surface_temperature" +
            "&hourly=wave_height&forecast_days=1"
        return client.getString(url).mapCatching { body ->
            val root = JSONObject(body)
            val cur = root.optJSONObject("current") ?: JSONObject()
            val hourly = root.optJSONObject("hourly") ?: JSONObject()
            val times = hourly.optJSONArray("time")
            val waves = hourly.optJSONArray("wave_height")
            val waveHeight = cur.optDoubleOrNull("wave_height")
            val waveDir = cur.optIntOrNull("wave_direction")
            val wavePeriod = cur.optDoubleOrNull("wave_period")
            val swell = cur.optDoubleOrNull("swell_wave_height")
            val sst = cur.optDoubleOrNull("sea_surface_temperature")
            val series = buildList {
                if (times != null && waves != null) {
                    val count = minOf(times.length(), waves.length(), 12)
                    for (i in 0 until count) {
                        if (!waves.isNull(i)) {
                            val label = times.optString(i).substringAfter("T", times.optString(i)).take(5)
                            add(label to waves.optDouble(i))
                        }
                    }
                }
            }
            val isMarine = waveHeight != null || swell != null || sst != null
            MarineWeatherData(
                source = "Open-Meteo Marine API",
                isMarineArea = isMarine,
                waveHeightM = waveHeight,
                waveDirectionDeg = waveDir,
                wavePeriodSec = wavePeriod,
                swellHeightM = swell,
                seaSurfaceTempC = sst,
                windSpeedKmh = windKmh,
                windGustKmh = gustKmh,
                pressureHpa = pressureHpa,
                tideStatusNote = "Harmonic Tide Station feed requires external configuration (station token not present — no synthetic tides generated).",
                hourlyWaveHeights = series,
                fetchedEpochMs = System.currentTimeMillis()
            )
        }
    }

    suspend fun fetchAviationWeather(query: String): Result<AviationMetarData> {
        val normalized = resolveIcao(query)
        val enc = URLEncoder.encode(normalized, "UTF-8")
        val metarUrl = "https://aviationweather.gov/api/data/metar?ids=$enc&format=json"
        val tafUrl = "https://aviationweather.gov/api/data/taf?ids=$enc&format=json"

        val metarRes = client.getString(metarUrl)
        val tafBody = client.getString(tafUrl).getOrNull()

        return metarRes.mapCatching { body ->
            val arr = JSONArray(body)
            if (arr.length() == 0) {
                throw IllegalStateException("No METAR observation found for '$normalized'. Try a valid ICAO/IATA code (e.g. DAC, VGHS, DEL, HND, JFK).")
            }
            val m = arr.getJSONObject(0)
            val icao = m.optString("icaoId", normalized)
            val preset = commonAirports.firstOrNull { it.icao.equals(icao, ignoreCase = true) }
            val rawTaf = runCatching {
                val tafArr = JSONArray(tafBody ?: "[]")
                if (tafArr.length() > 0) tafArr.getJSONObject(0).optString("rawTAF") else null
            }.getOrNull()

            val clouds = m.optJSONArray("clouds")
            val ceilingStr = if (clouds != null && clouds.length() > 0) {
                val list = (0 until clouds.length()).mapNotNull { idx ->
                    val c = clouds.optJSONObject(idx) ?: return@mapNotNull null
                    val cover = c.optString("cover")
                    val base = c.optIntOrNull("base")
                    if (cover.isNotBlank()) "${cover}${base?.let { " @ ${it}ft" } ?: ""}" else null
                }
                list.joinToString(", ").ifBlank { "CLR" }
            } else "CLR / Not reported"

            AviationMetarData(
                icao = icao,
                iata = preset?.iata,
                airportName = m.optString("name").ifBlank { preset?.name ?: icao },
                rawMetar = m.optString("rawOb", "Unavailable"),
                rawTaf = rawTaf?.takeIf { it.isNotBlank() },
                observationTimeIso = m.optString("reportTime", ""),
                windDirDeg = m.optIntOrNull("wdir"),
                windSpeedKt = m.optIntOrNull("wspd"),
                visibilityStr = m.optString("visib").takeIf { it.isNotBlank() }?.let { "$it SM" } ?: "N/A",
                ceilingStr = ceilingStr,
                tempC = m.optDoubleOrNull("temp"),
                dewpointC = m.optDoubleOrNull("dewp"),
                altimeterHpa = m.optDoubleOrNull("altim"),
                flightCategory = m.optString("fltCat").takeIf { it.isNotBlank() },
                weatherString = m.optString("wxString").ifBlank { "No significant wx reported" },
                source = "NOAA Aviation Weather Center",
                fetchedEpochMs = System.currentTimeMillis()
            )
        }
    }

    suspend fun fetchSpaceWeather(): Result<SpaceWeatherData> {
        val kpUrl = "https://services.swpc.noaa.gov/products/noaa-planetary-k-index.json"
        val windUrl = "https://services.swpc.noaa.gov/products/summary/solar-wind-speed.json"
        val kpRes = client.getString(kpUrl)
        val windJson = client.getString(windUrl).getOrNull()

        return kpRes.mapCatching { body ->
            val arr = JSONArray(body)
            var latestKp: Double? = null
            var obsTime = ""
            if (arr.length() > 1) {
                val last = arr.optJSONArray(arr.length() - 1)
                if (last != null && last.length() >= 2) {
                    obsTime = last.optString(0, "")
                    latestKp = last.optString(1).toDoubleOrNull()
                }
            }
            val windSpeed = windJson?.let {
                runCatching { JSONObject(it).optString("WindSpeed").toDoubleOrNull() }.getOrNull()
            }
            val status = when {
                latestKp == null -> "Unavailable"
                latestKp >= 7.0 -> "Strong–Severe Geomagnetic Storm (G3+)"
                latestKp >= 5.0 -> "Minor–Moderate Geomagnetic Storm (G1–G2)"
                latestKp >= 4.0 -> "Unsettled / Active"
                else -> "Quiet Geomagnetic Field"
            }
            val aurora = when {
                latestKp == null -> "Check NOAA SWPC"
                latestKp >= 6.0 -> "High- & mid-latitude aurora possible"
                latestKp >= 4.0 -> "High-latitude polar aurora active"
                else -> "Sub-polar high latitudes only"
            }
            SpaceWeatherData(
                kpIndex = latestKp,
                geomagneticStatus = status,
                solarWindSpeedKms = windSpeed,
                auroraPossibility = aurora,
                observationTimeIso = obsTime,
                source = "NOAA SWPC",
                fetchedEpochMs = System.currentTimeMillis()
            )
        }
    }

    private fun resolveIcao(raw: String): String {
        val clean = raw.trim().uppercase(Locale.US)
        if (clean.length == 4 && clean.all { it.isLetterOrDigit() }) return clean
        val match = commonAirports.firstOrNull {
            it.iata.equals(clean, ignoreCase = true) ||
                it.icao.equals(clean, ignoreCase = true) ||
                it.name.contains(clean, ignoreCase = true)
        }
        return match?.icao ?: clean
    }

    private fun JSONObject.optDoubleOrNull(key: String): Double? =
        if (has(key) && !isNull(key)) optDouble(key).takeIf { !it.isNaN() } else null

    private fun JSONObject.optIntOrNull(key: String): Int? =
        if (has(key) && !isNull(key)) optInt(key) else null
}
