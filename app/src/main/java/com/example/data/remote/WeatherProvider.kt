package com.example.data.remote

import com.example.domain.model.ProviderId
import com.example.domain.model.ProviderSnapshot

sealed class ProviderResult {
    data class Success(val snapshot: ProviderSnapshot) : ProviderResult()
    data class MissingApiKey(val providerId: ProviderId, val message: String) : ProviderResult()
    data class Failure(
        val providerId: ProviderId,
        val reason: String,
        val responseTimeMs: Long
    ) : ProviderResult()
}

interface WeatherProvider {
    val providerId: ProviderId
    suspend fun fetchWeather(
        latitude: Double,
        longitude: Double,
        timezoneId: String,
        apiKey: String?
    ): ProviderResult
}

fun String?.isConfiguredApiKey(): Boolean {
    if (this.isNullOrBlank()) return false
    val trimmed = this.trim()
    return !trimmed.startsWith("YOUR_", ignoreCase = true) &&
        !trimmed.startsWith("MY_", ignoreCase = true)
}
