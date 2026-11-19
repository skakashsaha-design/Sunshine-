package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.AnimationLevel
import com.example.domain.model.AstronomySnapshot
import com.example.domain.model.DataOrigin
import com.example.ui.components.OriginBadge
import com.example.ui.theme.SkyAmber
import com.example.ui.theme.SkyCobalt
import com.example.ui.theme.SkyDeepNavy
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AstronomyScreen(
    astronomy: AstronomySnapshot?,
    cityName: String,
    animationLevel: AnimationLevel,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) { onRefresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$cityName · ${stringResource(R.string.title_astronomy)}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (astronomy == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val sun = astronomy.sun
        val moon = astronomy.moon

        val shimmer by if (animationLevel == AnimationLevel.OFF) {
            remember { mutableFloatStateOf(0.5f) }
        } else {
            rememberInfiniteTransition(label = "astro").animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Reverse),
                label = "astro_shimmer"
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // SUN PATH CARD
                item {
                    Surface(shape = RoundedCornerShape(22.dp), tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Solar Trajectory & Twilight", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                OriginBadge(DataOrigin.CALCULATED)
                            }

                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(155.dp)
                            ) {
                                val w = size.width
                                val h = size.height
                                val horizonY = h * 0.78f
                                val arcRadius = minOf(w * 0.42f, h * 0.65f)
                                val cx = w / 2f

                                // Horizon twilight band
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        listOf(
                                            if (sun.isDaylight) Color(0x220284C7) else Color(0x441E1B4B),
                                            if (sun.isTwilight) Color(0x55F97316) else Color.Transparent
                                        )
                                    ),
                                    size = Size(w, horizonY)
                                )

                                // Horizon line
                                drawLine(
                                    color = Color.Gray.copy(alpha = 0.5f),
                                    start = Offset(16f, horizonY),
                                    end = Offset(w - 16f, horizonY),
                                    strokeWidth = 2f
                                )

                                // Sun arc
                                drawArc(
                                    color = SkyAmber.copy(alpha = 0.45f),
                                    startAngle = 180f,
                                    sweepAngle = 180f,
                                    useCenter = false,
                                    topLeft = Offset(cx - arcRadius, horizonY - arcRadius),
                                    size = Size(arcRadius * 2, arcRadius * 2),
                                    style = Stroke(
                                        width = 5f,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
                                    )
                                )

                                // Current Sun position on arc
                                val angleRad = PI * (1.0 - sun.sunProgressFraction.toDouble())
                                val sunX = cx + (arcRadius * cos(angleRad)).toFloat()
                                val sunY = horizonY - (arcRadius * sin(angleRad)).toFloat()
                                if (sun.isDaylight) {
                                    drawCircle(SkyAmber.copy(alpha = 0.25f + 0.12f * shimmer), radius = 26f, center = Offset(sunX, sunY))
                                    drawCircle(SkyAmber, radius = 14f, center = Offset(sunX, sunY))
                                } else {
                                    drawCircle(Color(0xFF94A3B8), radius = 10f, center = Offset(cx - arcRadius, horizonY))
                                }
                            }

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                AstroStat("Sunrise", sun.sunriseTime)
                                AstroStat("Solar Noon", sun.solarNoonTime)
                                AstroStat("Sunset", sun.sunsetTime)
                            }
                            HorizontalDivider()
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                AstroStat("Civil Dawn", sun.civilDawnTime)
                                AstroStat("Day Length", sun.dayLengthFormatted)
                                AstroStat("Civil Dusk", sun.civilDuskTime)
                            }
                            HorizontalDivider()
                            Text("Golden Hour: ${sun.goldenHourMorning}  |  ${sun.goldenHourEvening}", style = MaterialTheme.typography.bodyMedium)
                            Text("Blue Hour: ${sun.blueHourMorning}  |  ${sun.blueHourEvening}", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Sun Altitude: ${String.format(Locale.US, "%.1f°", sun.sunAltitudeDeg)} · Azimuth: ${String.format(Locale.US, "%.1f°", sun.sunAzimuthDeg)} · Daylight left: ${sun.remainingDaylightFormatted}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // MOON PHASE & EPHEMERIS CARD
                item {
                    Surface(shape = RoundedCornerShape(22.dp), tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Lunar Phase & Position", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                OriginBadge(DataOrigin.CALCULATED)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(18.dp)
                            ) {
                                // Custom Moon Phase Canvas Visualization
                                Canvas(modifier = Modifier.size(92.dp)) {
                                    val r = size.minDimension / 2.2f
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    drawCircle(SkyDeepNavy, radius = r + 4f, center = center)
                                    drawCircle(Color(0xFF1E293B), radius = r, center = center)
                                    val illum = (moon.illuminationPercent / 100.0).toFloat().coerceIn(0f, 1f)
                                    val isWaxing = moon.phaseFraction <= 0.5
                                    if (illum > 0.02f) {
                                        drawArc(
                                            color = Color(0xFFF1F5F9),
                                            startAngle = if (isWaxing) -90f else 90f,
                                            sweepAngle = 180f,
                                            useCenter = true,
                                            topLeft = Offset(center.x - r, center.y - r),
                                            size = Size(r * 2f, r * 2f)
                                        )
                                        val innerWidth = r * kotlin.math.abs(1f - 2f * illum)
                                        val innerColor = if (illum >= 0.5f) Color(0xFFF1F5F9) else Color(0xFF1E293B)
                                        drawOval(
                                            color = innerColor,
                                            topLeft = Offset(center.x - innerWidth, center.y - r),
                                            size = Size(innerWidth * 2f, r * 2f)
                                        )
                                    }
                                    drawCircle(Color(0x5594A3B8), radius = r, center = center, style = Stroke(width = 2f, cap = StrokeCap.Round))
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(moon.phaseType.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text("Illumination: ${String.format(Locale.US, "%.1f", moon.illuminationPercent)}%", style = MaterialTheme.typography.bodyMedium)
                                    Text("Moon Age: ${String.format(Locale.US, "%.1f", moon.moonAgeDays)} days", style = MaterialTheme.typography.bodyMedium)
                                    Text("Distance: ${moon.moonDistanceKm} km", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            HorizontalDivider()
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                AstroStat("Moonrise", moon.moonriseTime)
                                AstroStat("Moonset", moon.moonsetTime)
                                AstroStat("Altitude", "${String.format(Locale.US, "%.1f°", moon.moonAltitudeDeg)}")
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                AstroStat("Azimuth", "${String.format(Locale.US, "%.1f°", moon.moonAzimuthDeg)}")
                                AstroStat("Next Full", moon.nextFullMoonDate)
                                AstroStat("Next New", moon.nextNewMoonDate)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AstroStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}
