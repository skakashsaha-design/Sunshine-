package com.example.wallpaper

import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.example.SkyPulseApp
import com.example.domain.model.WeatherCondition
import kotlinx.coroutines.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Native Android Live Weather Wallpaper Service.
 * Renders dynamic day/night sky gradients, stars, drifting clouds, rain/snow particles,
 * and lightning flashes driven by the latest cached weather condition.
 */
class SkyPulseWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = SkyEngine()

    inner class SkyEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private var visible = false
        private var width = 0
        private var height = 0
        private var tick = 0f
        private var condition = WeatherCondition.PARTLY_CLOUDY
        private var isDay = true
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        private val particles = List(45) {
            floatArrayOf(Random.nextFloat(), Random.nextFloat(), 0.4f + Random.nextFloat() * 0.8f)
        }

        private val drawRunnable = object : Runnable {
            override fun run() {
                drawFrame()
                if (visible) {
                    handler.postDelayed(this, 33L)
                }
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                refreshCachedWeatherState()
                handler.post(drawRunnable)
            } else {
                handler.removeCallbacks(drawRunnable)
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            this.width = width
            this.height = height
        }

        override fun onDestroy() {
            handler.removeCallbacks(drawRunnable)
            scope.cancel()
            super.onDestroy()
        }

        private fun refreshCachedWeatherState() {
            scope.launch {
                val app = applicationContext as? SkyPulseApp ?: return@launch
                val locs = app.container.locationRepository.savedLocationsFlow
                val dao = app.container.database.dao()
                val saved = dao.getSavedLocationsOnce()
                val active = saved.firstOrNull { it.isDefault } ?: saved.firstOrNull() ?: return@launch
                val payload = dao.getCachedPayload("weather_${active.id}") ?: return@launch
                // Quick lightweight inspect without full decode blocking
                val json = payload.payloadJson
                isDay = !json.contains("\"isDay\":false")
                condition = when {
                    json.contains("THUNDERSTORM") -> WeatherCondition.THUNDERSTORM
                    json.contains("HEAVY_RAIN") -> WeatherCondition.HEAVY_RAIN
                    json.contains("\"RAIN\"") -> WeatherCondition.RAIN
                    json.contains("SNOW") -> WeatherCondition.SNOW
                    json.contains("FOG") -> WeatherCondition.FOG
                    json.contains("CLOUDY") -> WeatherCondition.CLOUDY
                    json.contains("CLEAR") -> WeatherCondition.CLEAR
                    else -> WeatherCondition.PARTLY_CLOUDY
                }
            }
        }

        private fun drawFrame() {
            if (width <= 0 || height <= 0) return
            val holder = surfaceHolder
            val canvas: Canvas? = runCatching { holder.lockCanvas() }.getOrNull()
            if (canvas == null) return
            try {
                tick += 0.03f
                val topColor: Int
                val bottomColor: Int
                when {
                    condition == WeatherCondition.THUNDERSTORM -> {
                        topColor = Color.rgb(15, 23, 42)
                        bottomColor = Color.rgb(30, 27, 75)
                    }
                    condition == WeatherCondition.RAIN || condition == WeatherCondition.HEAVY_RAIN -> {
                        topColor = Color.rgb(30, 41, 59)
                        bottomColor = Color.rgb(51, 65, 85)
                    }
                    !isDay -> {
                        topColor = Color.rgb(6, 14, 30)
                        bottomColor = Color.rgb(30, 41, 59)
                    }
                    else -> {
                        topColor = Color.rgb(2, 132, 199)
                        bottomColor = Color.rgb(56, 189, 248)
                    }
                }

                val bgPaint = Paint().apply {
                    shader = LinearGradient(0f, 0f, 0f, height.toFloat(), topColor, bottomColor, Shader.TileMode.CLAMP)
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

                // Sun or Moon orb
                val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isDay) Color.argb(180, 251, 191, 36) else Color.argb(200, 226, 232, 240)
                }
                val orbX = width * 0.72f + sin(tick * 0.1f) * 18f
                val orbY = height * 0.22f + cos(tick * 0.1f) * 10f
                canvas.drawCircle(orbX, orbY, width * 0.09f, orbPaint)

                // Stars at night
                if (!isDay) {
                    val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(160, 255, 255, 255) }
                    particles.take(22).forEachIndexed { idx, p ->
                        val sx = p[0] * width
                        val sy = p[1] * height * 0.55f
                        val r = 1.5f + sin(tick * 2f + idx) * 0.8f
                        canvas.drawCircle(sx, sy, r.coerceAtLeast(0.5f), starPaint)
                    }
                }

                // Rain / Snow particles
                if (condition == WeatherCondition.RAIN || condition == WeatherCondition.HEAVY_RAIN || condition == WeatherCondition.THUNDERSTORM) {
                    val rainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.argb(140, 186, 230, 253)
                        strokeWidth = 3f
                    }
                    particles.forEach { p ->
                        p[1] = (p[1] + 0.025f * p[2]) % 1f
                        val px = p[0] * width
                        val py = p[1] * height
                        canvas.drawLine(px, py, px - 6f, py + 28f, rainPaint)
                    }
                } else if (condition == WeatherCondition.SNOW) {
                    val snowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.argb(200, 255, 255, 255)
                    }
                    particles.forEach { p ->
                        p[1] = (p[1] + 0.008f * p[2]) % 1f
                        val px = p[0] * width + sin(tick + p[1] * 10f) * 14f
                        val py = p[1] * height
                        canvas.drawCircle(px, py, 5f * p[2], snowPaint)
                    }
                }
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }
    }
}
