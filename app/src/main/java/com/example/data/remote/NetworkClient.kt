package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class NetworkClient(cacheDir: File? = null) {

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .apply {
            if (cacheDir != null) {
                cache(Cache(File(cacheDir, "http_cache"), 10L * 1024L * 1024L))
            }
        }
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", "SkyPulseWeather/1.0 (skakashsaha@gmail.com)")
                .header("Accept", "application/json, text/plain, */*")
                .build()
            chain.proceed(req)
        }
        .build()

    fun <T> createRetrofitService(baseUrl: String, serviceClass: Class<T>): T {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(serviceClass)
    }

    val openMeteoWeatherApi: OpenMeteoWeatherApi by lazy {
        createRetrofitService("https://api.open-meteo.com/", OpenMeteoWeatherApi::class.java)
    }

    val openMeteoAirQualityApi: OpenMeteoAirQualityApi by lazy {
        createRetrofitService("https://air-quality-api.open-meteo.com/", OpenMeteoAirQualityApi::class.java)
    }

    val openMeteoFloodApi: OpenMeteoFloodApi by lazy {
        createRetrofitService("https://flood-api.open-meteo.com/", OpenMeteoFloodApi::class.java)
    }

    val openMeteoGeocodingApi: OpenMeteoGeocodingApi by lazy {
        createRetrofitService("https://geocoding-api.open-meteo.com/", OpenMeteoGeocodingApi::class.java)
    }

    suspend fun getString(url: String, headers: Map<String, String> = emptyMap()): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val builder = Request.Builder().url(url).get()
                headers.forEach { (k, v) -> builder.header(k, v) }
                httpClient.newCall(builder.build()).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        throw IOException("HTTP ${resp.code}: ${resp.message}")
                    }
                    resp.body?.string() ?: throw IOException("Empty response body")
                }
            }
        }

    suspend fun postJson(
        url: String,
        jsonBody: String,
        headers: Map<String, String> = emptyMap()
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())
            val builder = Request.Builder().url(url).post(body)
            headers.forEach { (k, v) -> builder.header(k, v) }
            httpClient.newCall(builder.build()).execute().use { resp ->
                if (!resp.isSuccessful) {
                    throw IOException("HTTP ${resp.code}: ${resp.message}")
                }
                resp.body?.string() ?: throw IOException("Empty response body")
            }
        }
    }

    suspend fun getBytes(url: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url(url).get().build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) null else resp.body?.bytes()
            }
        }.getOrNull()
    }
}
