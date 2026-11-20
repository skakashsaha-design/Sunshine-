package com.example.data.local

import android.content.Context
import androidx.room.*
import com.example.domain.model.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "saved_locations")
data class SavedLocationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val region: String,
    val country: String,
    val countryCode: String,
    val latitude: Double,
    val longitude: Double,
    val timezoneId: String,
    val isDefault: Boolean,
    val isCurrentGps: Boolean,
    val sortOrder: Int
) {
    fun toDomain() = SavedLocation(
        id = id,
        name = name,
        region = region,
        country = country,
        countryCode = countryCode,
        latitude = latitude,
        longitude = longitude,
        timezoneId = timezoneId,
        isDefault = isDefault,
        isCurrentGps = isCurrentGps,
        sortOrder = sortOrder
    )

    companion object {
        fun fromDomain(loc: SavedLocation) = SavedLocationEntity(
            id = loc.id,
            name = loc.name,
            region = loc.region,
            country = loc.country,
            countryCode = loc.countryCode,
            latitude = loc.latitude,
            longitude = loc.longitude,
            timezoneId = loc.timezoneId,
            isDefault = loc.isDefault,
            isCurrentGps = loc.isCurrentGps,
            sortOrder = loc.sortOrder
        )
    }
}

@Entity(tableName = "cached_payloads")
data class CachedPayloadEntity(
    @PrimaryKey val cacheKey: String,
    val category: String,
    val payloadJson: String,
    val updatedEpochMs: Long
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val queryKey: String,
    val name: String,
    val region: String,
    val country: String,
    val countryCode: String,
    val latitude: Double,
    val longitude: Double,
    val timezoneId: String,
    val searchedAtEpochMs: Long
)

@Entity(tableName = "notification_logs")
data class NotificationLogEntity(
    @PrimaryKey val alertId: String,
    val sentEpochMs: Long
)

@Entity(tableName = "trip_plans")
data class TripPlanEntity(
    @PrimaryKey val id: String,
    val title: String,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val isoDate: String,
    val activityTypeName: String,
    val timeWindowName: String,
    val lastScore: Int,
    val lastVerdictName: String,
    val createdEpochMs: Long
) {
    fun toDomain(): SavedTripPlan = SavedTripPlan(
        id = id,
        title = title,
        locationName = locationName,
        latitude = latitude,
        longitude = longitude,
        isoDate = isoDate,
        activityType = runCatching { TripActivityType.valueOf(activityTypeName) }.getOrDefault(TripActivityType.OUTDOOR_EVENT_HOSTING),
        timeWindow = runCatching { EventTimeWindow.valueOf(timeWindowName) }.getOrDefault(EventTimeWindow.FULL_DAY),
        lastScore = lastScore,
        lastVerdict = runCatching { HostVerdict.valueOf(lastVerdictName) }.getOrDefault(HostVerdict.FAIR_WITH_BACKUP),
        createdEpochMs = createdEpochMs
    )

    companion object {
        fun fromDomain(p: SavedTripPlan) = TripPlanEntity(
            id = p.id,
            title = p.title,
            locationName = p.locationName,
            latitude = p.latitude,
            longitude = p.longitude,
            isoDate = p.isoDate,
            activityTypeName = p.activityType.name,
            timeWindowName = p.timeWindow.name,
            lastScore = p.lastScore,
            lastVerdictName = p.lastVerdict.name,
            createdEpochMs = p.createdEpochMs
        )
    }
}

@Dao
interface SkyPulseDao {
    @Query("SELECT * FROM saved_locations ORDER BY sortOrder ASC")
    fun observeSavedLocations(): Flow<List<SavedLocationEntity>>

    @Query("SELECT * FROM saved_locations ORDER BY sortOrder ASC")
    suspend fun getSavedLocationsOnce(): List<SavedLocationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocation(entity: SavedLocationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocations(entities: List<SavedLocationEntity>)

    @Query("DELETE FROM saved_locations WHERE id = :id")
    suspend fun deleteLocationById(id: String)

    @Query("UPDATE saved_locations SET isDefault = CASE WHEN id = :id THEN 1 ELSE 0 END")
    suspend fun setDefaultLocation(id: String)

    @Query("UPDATE saved_locations SET name = :newName WHERE id = :id")
    suspend fun renameLocation(id: String, newName: String)

    @Query("SELECT * FROM cached_payloads WHERE cacheKey = :key LIMIT 1")
    suspend fun getCachedPayload(key: String): CachedPayloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putCachedPayload(entity: CachedPayloadEntity)

    @Query("SELECT * FROM search_history ORDER BY searchedAtEpochMs DESC LIMIT 12")
    fun observeSearchHistory(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSearchHistory(entity: SearchHistoryEntity)

    @Query("SELECT * FROM notification_logs WHERE alertId = :alertId LIMIT 1")
    suspend fun getNotificationLog(alertId: String): NotificationLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markNotificationSent(entity: NotificationLogEntity)

    @Query("SELECT * FROM trip_plans ORDER BY isoDate ASC, createdEpochMs DESC")
    fun observeTripPlans(): Flow<List<TripPlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTripPlan(entity: TripPlanEntity)

    @Query("DELETE FROM trip_plans WHERE id = :id")
    suspend fun deleteTripPlan(id: String)
}

@Database(
    entities = [
        SavedLocationEntity::class,
        CachedPayloadEntity::class,
        SearchHistoryEntity::class,
        NotificationLogEntity::class,
        TripPlanEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SkyPulseDatabase : RoomDatabase() {
    abstract fun dao(): SkyPulseDao

    companion object {
        fun create(context: Context): SkyPulseDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                SkyPulseDatabase::class.java,
                "skypulse_cache.db"
            ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }
}
