package com.example.data.remote

import com.example.domain.model.RadarFrame
import com.example.domain.model.RadarTimeline
import org.json.JSONObject

class RadarService(private val client: NetworkClient) {

    suspend fun fetchRainViewerTimeline(): Result<RadarTimeline> {
        val url = "https://api.rainviewer.com/public/weather-maps.json"
        return client.getString(url).mapCatching { body ->
            val root = JSONObject(body)
            val host = root.optString("host", "https://tilecache.rainviewer.com")
            val generated = root.optLong("generated", System.currentTimeMillis() / 1000L)
            val radar = root.getJSONObject("radar")
            val frames = mutableListOf<RadarFrame>()

            val past = radar.optJSONArray("past")
            if (past != null) {
                for (i in 0 until past.length()) {
                    val f = past.getJSONObject(i)
                    val path = f.getString("path")
                    frames += RadarFrame(
                        timestampEpochSec = f.getLong("time"),
                        path = path,
                        isNowcast = false,
                        tileUrlTemplate = "$host$path/256/{z}/{x}/{y}/2/1_1.png"
                    )
                }
            }
            val nowcast = radar.optJSONArray("nowcast")
            if (nowcast != null) {
                for (i in 0 until nowcast.length()) {
                    val f = nowcast.getJSONObject(i)
                    val path = f.getString("path")
                    frames += RadarFrame(
                        timestampEpochSec = f.getLong("time"),
                        path = path,
                        isNowcast = true,
                        tileUrlTemplate = "$host$path/256/{z}/{x}/{y}/2/1_1.png"
                    )
                }
            }
            RadarTimeline(host = host, frames = frames, generatedEpochSec = generated)
        }
    }
}
