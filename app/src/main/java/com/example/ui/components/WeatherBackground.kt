package com.example.ui.components

import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import com.example.domain.model.AnimationLevel
import com.example.domain.model.WeatherCondition
import com.example.ui.theme.*
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun DynamicWeatherBackground(
    condition: WeatherCondition,
    isDay: Boolean,
    animationLevel: AnimationLevel,
    themeAppearance: ThemeAppearance,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemScale = remember {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
    }
    val effectiveAnim = when {
        systemScale == 0f -> AnimationLevel.OFF
        else -> animationLevel
    }

    val colors = when (themeAppearance) {
        ThemeAppearance.AMOLED_DARK -> listOf(Color(0xFF000000), Color(0xFF070B12))
        ThemeAppearance.MINIMAL -> if (isDay) listOf(Color(0xFFF8FAFC), Color(0xFFE2E8F0)) else listOf(Color(0xFF0F172A), Color(0xFF1E293B))
        else -> when (condition) {
            WeatherCondition.THUNDERSTORM -> listOf(StormTop, StormBottom)
            WeatherCondition.RAIN, WeatherCondition.HEAVY_RAIN, WeatherCondition.DRIZZLE -> listOf(RainTop, RainBottom)
            WeatherCondition.SNOW, WeatherCondition.SLEET -> listOf(SnowTop, SnowBottom)
            WeatherCondition.FOG -> listOf(FogTop, FogBottom)
            WeatherCondition.CLEAR, WeatherCondition.PARTLY_CLOUDY ->
                if (isDay) listOf(ClearDayTop, ClearDayBottom) else listOf(ClearNightTop, ClearNightBottom)
            else -> if (isDay) listOf(Color(0xFF1E3A5F), Color(0xFF475569)) else listOf(ClearNightTop, Color(0xFF1E293B))
        }
    }

    val particles = remember {
        List(32) { Triple(Random.nextFloat(), Random.nextFloat(), 0.5f + Random.nextFloat()) }
    }

    val phase by if (effectiveAnim == AnimationLevel.OFF) {
        remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    } else {
        rememberInfiniteTransition(label = "sky_bg").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(if (effectiveAnim == AnimationLevel.REDUCED) 9000 else 4500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "bg_phase"
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors))
    ) {
        if (effectiveAnim != AnimationLevel.OFF && themeAppearance != ThemeAppearance.MINIMAL) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                when {
                    !isDay && (condition == WeatherCondition.CLEAR || condition == WeatherCondition.PARTLY_CLOUDY) -> {
                        particles.take(24).forEachIndexed { i, p ->
                            val alpha = (0.25f + 0.45f * (0.5f + 0.5f * sin((phase * 6.283f) + i))).coerceIn(0.1f, 0.75f)
                            drawCircle(
                                color = Color.White.copy(alpha = alpha),
                                radius = 1.5f * p.third,
                                center = Offset(p.first * w, p.second * h * 0.5f)
                            )
                        }
                    }
                    condition == WeatherCondition.RAIN || condition == WeatherCondition.HEAVY_RAIN || condition == WeatherCondition.THUNDERSTORM -> {
                        val count = if (effectiveAnim == AnimationLevel.REDUCED) 12 else 28
                        particles.take(count).forEach { p ->
                            val y = ((p.second + phase * p.third) % 1f) * h
                            val x = p.first * w
                            drawLine(
                                color = Color(0x33BAE6FD),
                                start = Offset(x, y),
                                end = Offset(x - 10f, y + 36f),
                                strokeWidth = 2f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    condition == WeatherCondition.SNOW -> {
                        val count = if (effectiveAnim == AnimationLevel.REDUCED) 12 else 24
                        particles.take(count).forEach { p ->
                            val y = ((p.second + phase * 0.5f * p.third) % 1f) * h
                            val x = p.first * w + sin(phase * 6.283f + p.second * 6f) * 18f
                            drawCircle(Color(0x44FFFFFF), radius = 4f * p.third, center = Offset(x, y))
                        }
                    }
                }
            }
        }
        content()
    }
}
