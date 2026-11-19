package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class NetworkClient {

    val httpClient: OkHttpClient = OkHttpClient.Builder()
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
