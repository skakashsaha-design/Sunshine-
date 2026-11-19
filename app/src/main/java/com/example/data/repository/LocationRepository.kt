package com.example.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import com.example.data.local.SavedLocationEntity
import com.example.data.local.SearchHistoryEntity
import com.example.data.local.SkyPulseDao
import com.example.data.remote.OpenMeteoProvider
import com.example.domain.model.SavedLocation
import com.example.domain.model.SearchLocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

class LocationRepository(
    private val context: Context,
    private val dao: SkyPulseDao,
    private val openMeteo: OpenMeteoProvider
) {
    val savedLocationsFlow: Flow<List<SavedLocation>> =
        dao.observeSavedLocations().map { list -> list.map { it.toDomain() } }

    val searchHistoryFlow: Flow<List<SearchLocationResult>> =
        dao.observeSearchHistory().map { list ->
            list.map {
                SearchLocationResult(
                    name = it.name,
                    region = it.region,
                    country = it.country,
                    countryCode = it.countryCode,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    timezoneId = it.timezoneId
                )
            }
        }

    suspend fun ensureInitialLocations() {
        val current = dao.getSavedLocationsOnce()
        if (current.isEmpty()) {
            val defaults = listOf(
                SavedLocation(
                    id = "dhaka_bd",
                    name = "Dhaka",
                    region = "Dhaka Division",
                    country = "Bangladesh",
                    countryCode = "BD",
                    latitude = 23.8103,
                    longitude = 90.4125,
                    timezoneId = "Asia/Dhaka",
                    isDefault = true,
                    sortOrder = 0
                ),
                SavedLocation(
                    id = "chattogram_bd",
                    name = "Chattogram",
                    region = "Chattogram Division",
                    country = "Bangladesh",
                    countryCode = "BD",
                    latitude = 22.3569,
                    longitude = 91.7832,
                    timezoneId = "Asia/Dhaka",
                    isDefault = false,
                    sortOrder = 1
                ),
                SavedLocation(
                    id = "tokyo_jp",
                    name = "Tokyo",
                    region = "Kanto",
                    country = "Japan",
                    countryCode = "JP",
                    latitude = 35.6762,
                    longitude = 139.6503,
                    timezoneId = "Asia/Tokyo",
                    isDefault = false,
                    sortOrder = 2
                )
            )
            dao.upsertLocations(defaults.map { SavedLocationEntity.fromDomain(it) })
        }
    }

    suspend fun searchCities(query: String): List<SearchLocationResult> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()

        // Support direct coordinate input e.g. "23.81, 90.41"
        val coordParts = q.split(",", " ").filter { it.isNotBlank() }
        if (coordParts.size == 2) {
            val lat = coordParts[0].toDoubleOrNull()
            val lon = coordParts[1].toDoubleOrNull()
            if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0) {
                return listOf(
                    SearchLocationResult(
                        name = "Point (${String.format(Locale.US, "%.2f, %.2f", lat, lon)})",
                        region = "Custom Coordinates",
                        country = "",
                        countryCode = "",
                        latitude = lat,
                        longitude = lon,
                        timezoneId = "UTC"
                    )
                )
            }
        }

        val remote = openMeteo.searchCities(q).getOrNull()
        if (!remote.isNullOrEmpty()) return remote

        // Offline reference city fallback if network unavailable
        return offlineFallbackCities.filter {
            it.name.contains(q, ignoreCase = true) || it.country.contains(q, ignoreCase = true)
        }
    }

    suspend fun saveLocation(result: SearchLocationResult, makeDefault: Boolean = true): SavedLocation {
        val existing = dao.getSavedLocationsOnce()
        val id = "${result.name.lowercase().replace(Regex("[^a-z0-9]"), "_")}_${(result.latitude * 100).toInt()}"
        val loc = SavedLocation(
            id = id,
            name = result.name,
            region = result.region,
            country = result.country,
            countryCode = result.countryCode,
            latitude = result.latitude,
            longitude = result.longitude,
            timezoneId = result.timezoneId,
            isDefault = makeDefault,
            sortOrder = existing.size
        )
        dao.upsertLocation(SavedLocationEntity.fromDomain(loc))
        if (makeDefault) dao.setDefaultLocation(id)
        dao.upsertSearchHistory(
            SearchHistoryEntity(
                queryKey = id,
                name = result.name,
                region = result.region,
                country = result.country,
                countryCode = result.countryCode,
                latitude = result.latitude,
                longitude = result.longitude,
                timezoneId = result.timezoneId,
                searchedAtEpochMs = System.currentTimeMillis()
            )
        )
        return loc
    }

    suspend fun renameLocation(id: String, newName: String) {
        if (newName.isNotBlank()) dao.renameLocation(id, newName.trim())
    }

    suspend fun deleteLocation(id: String) {
        val current = dao.getSavedLocationsOnce()
        if (current.size <= 1) return
        dao.deleteLocationById(id)
        val remaining = dao.getSavedLocationsOnce()
        if (remaining.none { it.isDefault } && remaining.isNotEmpty()) {
            dao.setDefaultLocation(remaining.first().id)
        }
    }

    suspend fun setDefaultLocation(id: String) = dao.setDefaultLocation(id)

    suspend fun moveLocation(id: String, direction: Int) {
        val list = dao.getSavedLocationsOnce().toMutableList()
        val idx = list.indexOfFirst { it.id == id }
        if (idx == -1) return
        val target = (idx + direction).coerceIn(0, list.lastIndex)
        if (idx == target) return
        val item = list.removeAt(idx)
        list.add(target, item)
        val reordered = list.mapIndexed { index, entity -> entity.copy(sortOrder = index) }
        dao.upsertLocations(reordered)
    }

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentGpsLocation(): SavedLocation? = withContext(Dispatchers.IO) {
        val fused = runCatching {
            val client = LocationServices.getFusedLocationProviderClient(context)
            suspendCancellableCoroutine<Location?> { cont ->
                val cts = CancellationTokenSource()
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                    .addOnSuccessListener { loc -> cont.resume(loc) }
                    .addOnFailureListener { cont.resume(null) }
                cont.invokeOnCancellation { cts.cancel() }
            }
        }.getOrNull()

        val loc = fused ?: runCatching {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }.getOrNull() ?: return@withContext null

        val geocoded = reverseGeocode(loc.latitude, loc.longitude)
        val gpsSaved = SavedLocation(
            id = "gps_current",
            name = geocoded?.first ?: "Current GPS",
            region = geocoded?.second ?: "",
            country = geocoded?.third ?: "",
            countryCode = geocoded?.fourth ?: "",
            latitude = loc.latitude,
            longitude = loc.longitude,
            timezoneId = java.util.TimeZone.getDefault().id,
            isDefault = true,
            isCurrentGps = true,
            sortOrder = -1
        )
        dao.upsertLocation(SavedLocationEntity.fromDomain(gpsSaved))
        dao.setDefaultLocation("gps_current")
        gpsSaved
    }

    private data class Quad(val first: String, val second: String, val third: String, val fourth: String)

    @Suppress("DEPRECATION")
    private fun reverseGeocode(lat: Double, lon: Double): Quad? = runCatching {
        val geocoder = Geocoder(context, Locale.getDefault())
        val list = geocoder.getFromLocation(lat, lon, 1)
        val addr = list?.firstOrNull() ?: return@runCatching null
        Quad(
            addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "Current Location",
            addr.adminArea ?: "",
            addr.countryName ?: "",
            addr.countryCode ?: ""
        )
    }.getOrNull()

    private val offlineFallbackCities = listOf(
        SearchLocationResult("Dhaka", "Dhaka", "Bangladesh", "BD", 23.8103, 90.4125, "Asia/Dhaka"),
        SearchLocationResult("Chattogram", "Chattogram", "Bangladesh", "BD", 22.3569, 91.7832, "Asia/Dhaka"),
        SearchLocationResult("Cox's Bazar", "Chattogram", "Bangladesh", "BD", 21.4272, 92.0058, "Asia/Dhaka"),
        SearchLocationResult("Sylhet", "Sylhet", "Bangladesh", "BD", 24.8949, 91.8687, "Asia/Dhaka"),
        SearchLocationResult("Rajshahi", "Rajshahi", "Bangladesh", "BD", 24.3745, 88.6042, "Asia/Dhaka"),
        SearchLocationResult("Khulna", "Khulna", "Bangladesh", "BD", 22.8456, 89.5403, "Asia/Dhaka"),
        SearchLocationResult("London", "England", "United Kingdom", "GB", 51.5072, -0.1276, "Europe/London"),
        SearchLocationResult("New York", "New York", "United States", "US", 40.7128, -74.0060, "America/New_York"),
        SearchLocationResult("Tokyo", "Tokyo", "Japan", "JP", 35.6762, 139.6503, "Asia/Tokyo"),
        SearchLocationResult("New Delhi", "Delhi", "India", "IN", 28.6139, 77.2090, "Asia/Kolkata")
    )
}
