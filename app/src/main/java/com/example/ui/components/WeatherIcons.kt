package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.model.*
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SkyPulseLogo(
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f

        // Deep cobalt sky badge circle
        drawCircle(
            color = Color(0xFF0F294A),
            radius = w * 0.48f,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = Color(0xFF38BDF8),
            radius = w * 0.46f,
            center = Offset(cx, cy),
            style = Stroke(width = w * 0.045f)
        )
        // Radar sweep arc
        drawArc(
            color = Color(0xFF34D399),
            startAngle = -55f,
            sweepAngle = 95f,
            useCenter = false,
            topLeft = Offset(w * 0.14f, h * 0.14f),
            size = Size(w * 0.72f, h * 0.72f),
            style = Stroke(width = w * 0.055f, cap = StrokeCap.Round)
        )
        // Amber solar core + crisp white cloud
        drawCircle(
            color = Color(0xFFFBBF24),
            radius = w * 0.16f,
            center = Offset(cx + w * 0.11f, cy - h * 0.12f)
        )
        drawCloud(cx - w * 0.04f, cy + h * 0.09f, w * 0.56f, Color.White)
    }
}

@Composable
fun AnimatedWeatherIcon(
    condition: WeatherCondition,
    isDay: Boolean,
    animationLevel: AnimationLevel = AnimationLevel.FULL,
    size: Dp = 64.dp,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "weather_icon")
    val pulse by if (animationLevel == AnimationLevel.OFF || animationLevel == AnimationLevel.MINIMAL) {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    } else {
        infinite.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = if (animationLevel == AnimationLevel.REDUCED) 4000 else 2400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "icon_phase"
        )
    }

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f

        when (condition) {
            WeatherCondition.CLEAR -> {
                if (isDay) drawSun(cx, cy, w * 0.24f, pulse)
                else drawMoonOrb(cx, cy, w * 0.25f)
            }
            WeatherCondition.PARTLY_CLOUDY -> {
                if (isDay) drawSun(cx + w * 0.14f, cy - h * 0.12f, w * 0.18f, pulse)
                else drawMoonOrb(cx + w * 0.15f, cy - h * 0.12f, w * 0.17f)
                drawCloud(cx - w * 0.05f, cy + h * 0.08f, w * 0.62f, Color.White)
            }
            WeatherCondition.CLOUDY -> {
                drawCloud(cx + w * 0.08f, cy - h * 0.06f, w * 0.50f, Color(0xFFCBD5E1))
                drawCloud(cx - w * 0.04f, cy + h * 0.08f, w * 0.66f, Color.White)
            }
            WeatherCondition.FOG -> {
                drawCloud(cx, cy - h * 0.08f, w * 0.58f, Color(0xFFE2E8F0))
                val drift = sin(pulse * 6.283f) * w * 0.04f
                for (row in 0..2) {
                    val y = cy + h * (0.14f + row * 0.10f)
                    drawLine(
                        color = Color(0xFF94A3B8),
                        start = Offset(w * 0.2f + drift, y),
                        end = Offset(w * 0.8f + drift, y),
                        strokeWidth = w * 0.055f,
                        cap = StrokeCap.Round
                    )
                }
            }
            WeatherCondition.DRIZZLE, WeatherCondition.RAIN -> {
                drawCloud(cx, cy - h * 0.10f, w * 0.64f, Color(0xFFE2E8F0))
                drawRainDrops(w, h, pulse, count = 3, heavy = false)
            }
            WeatherCondition.HEAVY_RAIN -> {
                drawCloud(cx, cy - h * 0.12f, w * 0.68f, Color(0xFF94A3B8))
                drawRainDrops(w, h, pulse, count = 5, heavy = true)
            }
            WeatherCondition.THUNDERSTORM -> {
                drawCloud(cx, cy - h * 0.14f, w * 0.68f, Color(0xFF64748B))
                drawRainDrops(w, h, pulse, count = 3, heavy = false)
                val flashAlpha = if (pulse in 0.72f..0.85f) 1f else 0.75f
                val bolt = Path().apply {
                    moveTo(cx + w * 0.02f, cy + h * 0.02f)
                    lineTo(cx - w * 0.09f, cy + h * 0.20f)
                    lineTo(cx + w * 0.02f, cy + h * 0.20f)
                    lineTo(cx - w * 0.06f, cy + h * 0.38f)
                    lineTo(cx + w * 0.14f, cy + h * 0.15f)
                    lineTo(cx + w * 0.03f, cy + h * 0.15f)
                    close()
                }
                drawPath(bolt, Color(0xFFFBBF24).copy(alpha = flashAlpha))
            }
            WeatherCondition.SNOW -> {
                drawCloud(cx, cy - h * 0.12f, w * 0.64f, Color.White)
                for (i in 0..3) {
                    val x = w * (0.28f + i * 0.15f)
                    val y = cy + h * 0.10f + ((pulse + i * 0.25f) % 1f) * h * 0.25f
                    drawCircle(Color(0xFFE0F2FE), radius = w * 0.04f, center = Offset(x, y))
                }
            }
            WeatherCondition.SLEET -> {
                drawCloud(cx, cy - h * 0.12f, w * 0.64f, Color(0xFFCBD5E1))
                drawRainDrops(w, h, pulse, count = 2, heavy = false)
                val y = cy + h * 0.12f + (pulse * h * 0.24f)
                drawCircle(Color.White, radius = w * 0.045f, center = Offset(cx + w * 0.12f, y))
            }
            WeatherCondition.WINDY -> {
                val shift = sin(pulse * 6.283f) * w * 0.05f
                for (row in 0..2) {
                    val y = cy - h * 0.12f + row * h * 0.14f
                    drawLine(
                        color = Color(0xFF38BDF8),
                        start = Offset(w * 0.15f + shift, y),
                        end = Offset(w * (0.78f - row * 0.08f) + shift, y),
                        strokeWidth = w * 0.06f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawSun(cx: Float, cy: Float, r: Float, pulse: Float) {
    drawCircle(color = Color(0x44FBBF24), radius = r * (1.35f + 0.12f * sin(pulse * 6.283f)), center = Offset(cx, cy))
    drawCircle(color = Color(0xFFFBBF24), radius = r, center = Offset(cx, cy))
    for (i in 0 until 8) {
        val angle = (i * 45.0 + pulse * 45.0) * Math.PI / 180.0
        val start = Offset(cx + (r * 1.25f * cos(angle)).toFloat(), cy + (r * 1.25f * sin(angle)).toFloat())
        val end = Offset(cx + (r * 1.62f * cos(angle)).toFloat(), cy + (r * 1.62f * sin(angle)).toFloat())
        drawLine(Color(0xFFF59E0B), start, end, strokeWidth = r * 0.18f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawMoonOrb(cx: Float, cy: Float, r: Float) {
    drawCircle(color = Color(0x33E2E8F0), radius = r * 1.32f, center = Offset(cx, cy))
    drawCircle(color = Color(0xFFE2E8F0), radius = r, center = Offset(cx, cy))
    drawCircle(color = Color(0xFFCBD5E1), radius = r * 0.22f, center = Offset(cx - r * 0.3f, cy - r * 0.2f))
    drawCircle(color = Color(0xFFCBD5E1), radius = r * 0.15f, center = Offset(cx + r * 0.25f, cy + r * 0.28f))
}

private fun DrawScope.drawCloud(cx: Float, cy: Float, width: Float, color: Color) {
    val h = width * 0.42f
    drawRoundRect(
        color = color,
        topLeft = Offset(cx - width * 0.5f, cy - h * 0.2f),
        size = Size(width, h * 0.72f),
        cornerRadius = CornerRadius(h * 0.36f, h * 0.36f)
    )
    drawCircle(color, radius = width * 0.24f, center = Offset(cx - width * 0.12f, cy - h * 0.25f))
    drawCircle(color, radius = width * 0.19f, center = Offset(cx + width * 0.18f, cy - h * 0.12f))
}

private fun DrawScope.drawRainDrops(w: Float, h: Float, pulse: Float, count: Int, heavy: Boolean) {
    val step = w * 0.46f / count.coerceAtLeast(1)
    for (i in 0 until count) {
        val x = w * 0.28f + i * step
        val offset = ((pulse + i * 0.31f) % 1f) * h * 0.22f
        val y1 = h * 0.56f + offset
        drawLine(
            color = if (heavy) Color(0xFF38BDF8) else Color(0xFF60A5FA),
            start = Offset(x, y1),
            end = Offset(x - w * 0.04f, y1 + h * 0.12f),
            strokeWidth = w * (if (heavy) 0.055f else 0.045f),
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun OriginBadge(origin: DataOrigin, modifier: Modifier = Modifier) {
    val (label, bg, fg) = when (origin) {
        DataOrigin.OFFICIAL_BMD -> Triple("OFFICIAL BMD", Color(0xFF15803D), Color.White)
        DataOrigin.GLOBAL_OFFICIAL -> Triple("GLOBAL OFFICIAL", Color(0xFF1D4ED8), Color.White)
        DataOrigin.OBSERVED -> Triple("OBSERVED", Color(0xFF0F766E), Color.White)
        DataOrigin.FORECAST -> Triple("FORECAST", Color(0xFF475569), Color.White)
        DataOrigin.MODEL_ESTIMATE -> Triple("MODEL ESTIMATE", Color(0xFF7C3AED), Color.White)
        DataOrigin.CALCULATED -> Triple("APP-CALCULATED", Color(0xFFB45309), Color.White)
    }
    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun SeverityChip(severity: AlertSeverity, modifier: Modifier = Modifier) {
    val color = when (severity) {
        AlertSeverity.EXTREME -> SeverityExtreme
        AlertSeverity.SEVERE -> SeverityHigh
        AlertSeverity.MODERATE -> SeverityModerate
        AlertSeverity.MINOR -> SeverityLow
        AlertSeverity.ADVISORY -> SeverityAdvisory
    }
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(4.dp))
        )
        Text(
            text = severity.label.uppercase(),
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
