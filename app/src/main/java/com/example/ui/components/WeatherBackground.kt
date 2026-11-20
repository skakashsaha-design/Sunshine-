package com.example.ui.components

import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import com.example.domain.model.AnimationLevel
import com.example.domain.model.BackgroundStyle
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
    backgroundStyle: BackgroundStyle = BackgroundStyle.DYNAMIC_SKY,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemScale = remember {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
    }
    val effectiveAnim = if (systemScale == 0f) AnimationLevel.OFF else animationLevel

    val isMinimalDay = (themeAppearance == ThemeAppearance.MINIMAL || themeAppearance == ThemeAppearance.LIGHT) &&
        backgroundStyle == BackgroundStyle.SOLID_CLEAN

    val colors = when {
        themeAppearance == ThemeAppearance.AMOLED_DARK || backgroundStyle == BackgroundStyle.DEEP_SPACE ->
            listOf(Color(0xFF000000), Color(0xFF060C18))
        themeAppearance == ThemeAppearance.AURORA_MIDNIGHT || backgroundStyle == BackgroundStyle.AURORA_GLOW ->
            listOf(Color(0xFF051322), Color(0xFF0F2942), Color(0xFF064E3B))
        themeAppearance == ThemeAppearance.OCEAN_BREEZE ->
            listOf(Color(0xFF041C2C), Color(0xFF0B3B54))
        isMinimalDay ->
            listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surfaceVariant)
        backgroundStyle == BackgroundStyle.RADAR_GRID ->
            listOf(Color(0xFF040D1A), Color(0xFF0A1D36))
        else -> when (condition) {
            WeatherCondition.THUNDERSTORM -> listOf(StormTop, StormBottom)
            WeatherCondition.RAIN, WeatherCondition.HEAVY_RAIN, WeatherCondition.DRIZZLE -> listOf(RainTop, RainBottom)
            WeatherCondition.SNOW, WeatherCondition.SLEET -> listOf(Color(0xFF1E293B), Color(0xFF334155))
            WeatherCondition.FOG -> listOf(Color(0xFF1E293B), Color(0xFF475569))
            WeatherCondition.CLEAR, WeatherCondition.PARTLY_CLOUDY ->
                if (isDay) listOf(Color(0xFF0C4A6E), Color(0xFF0369A1)) else listOf(ClearNightTop, ClearNightBottom)
            else -> if (isDay) listOf(Color(0xFF162D4C), Color(0xFF28466E)) else listOf(ClearNightTop, Color(0xFF1E293B))
        }
    }

    val backdropContentColor = if (isMinimalDay) {
        MaterialTheme.colorScheme.onBackground
    } else {
        Color(0xFFF8FAFC)
    }

    val particles = remember {
        List(28) { Triple(Random.nextFloat(), Random.nextFloat(), 0.5f + Random.nextFloat()) }
    }

    val phase by if (effectiveAnim == AnimationLevel.OFF) {
        remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    } else {
        val durationMs = when (effectiveAnim) {
            AnimationLevel.MINIMAL -> 12500
            AnimationLevel.REDUCED -> 9500
            else -> 5000
        }
        rememberInfiniteTransition(label = "sky_bg").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMs, easing = LinearEasing),
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
        if (effectiveAnim != AnimationLevel.OFF && !isMinimalDay) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                if (backgroundStyle == BackgroundStyle.RADAR_GRID) {
                    val center = Offset(w * 0.5f, h * 0.35f)
                    listOf(0.22f, 0.42f, 0.65f).forEach { r ->
                        drawCircle(
                            color = Color(0x1838BDF8),
                            radius = w * r,
                            center = center,
                            style = Stroke(width = 1.5f)
                        )
                    }
                } else if (backgroundStyle == BackgroundStyle.AURORA_GLOW || themeAppearance == ThemeAppearance.AURORA_MIDNIGHT) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x2834D399), Color.Transparent),
                            center = Offset(w * (0.25f + 0.1f * sin(phase * 6.283f)), h * 0.18f),
                            radius = w * 0.7f
                        ),
                        center = Offset(w * (0.25f + 0.1f * sin(phase * 6.283f)), h * 0.18f),
                        radius = w * 0.7f
                    )
                }

                when {
                    !isDay && (condition == WeatherCondition.CLEAR || condition == WeatherCondition.PARTLY_CLOUDY) -> {
                        val starCount = if (effectiveAnim == AnimationLevel.MINIMAL) 8 else 22
                        particles.take(starCount).forEachIndexed { i, p ->
                            val alpha = (0.25f + 0.45f * (0.5f + 0.5f * sin((phase * 6.283f) + i))).coerceIn(0.1f, 0.75f)
                            drawCircle(
                                color = Color.White.copy(alpha = alpha),
                                radius = 1.5f * p.third,
                                center = Offset(p.first * w, p.second * h * 0.5f)
                            )
                        }
                    }
                    condition == WeatherCondition.RAIN || condition == WeatherCondition.HEAVY_RAIN ||
                        condition == WeatherCondition.DRIZZLE || condition == WeatherCondition.THUNDERSTORM -> {
                        val count = when (effectiveAnim) {
                            AnimationLevel.MINIMAL -> 7
                            AnimationLevel.REDUCED -> 12
                            else -> 26
                        }
                        particles.take(count).forEach { p ->
                            val y = ((p.second + phase * p.third) % 1f) * h
                            val x = p.first * w
                            drawLine(
                                color = Color(0x38BAE6FD),
                                start = Offset(x, y),
                                end = Offset(x - 10f, y + 34f),
                                strokeWidth = 2f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    condition == WeatherCondition.SNOW -> {
                        val count = when (effectiveAnim) {
                            AnimationLevel.MINIMAL -> 7
                            AnimationLevel.REDUCED -> 12
                            else -> 22
                        }
                        particles.take(count).forEach { p ->
                            val y = ((p.second + phase * 0.5f * p.third) % 1f) * h
                            val x = p.first * w + sin(phase * 6.283f + p.second * 6f) * 18f
                            drawCircle(Color(0x44FFFFFF), radius = 4f * p.third, center = Offset(x, y))
                        }
                    }
                }
            }
        }
        CompositionLocalProvider(LocalContentColor provides backdropContentColor) {
            content()
        }
    }
}
