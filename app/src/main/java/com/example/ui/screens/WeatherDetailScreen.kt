package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.calc.GeoUtils
import com.example.domain.calc.MetricGlossary
import com.example.domain.model.*
import com.example.ui.components.OriginBadge
import java.util.Locale
import kotlin.math.roundToInt

private data class TelemetryItem(
    val glossaryKey: String?,
    val label: String,
    val value: String,
    val detail: String,
    val origin: DataOrigin
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherDetailScreen(
    bundle: WeatherBundle,
    prefs: UserPreferences,
    onBack: () -> Unit
) {
    val cur = bundle.current
    val ins = bundle.insights
    val beaufort = GeoUtils.beaufortScale(cur.windSpeedKmh)
    var activeGlossary by remember { mutableStateOf<MetricGlossaryEntry?>(null) }

    val rows = listOf(
        TelemetryItem("temperature", "Temperature", GeoUtils.formatTemperature(cur.temperatureC, prefs.temperatureUnit), cur.conditionLabel, cur.origin),
        TelemetryItem("feels_like", "Feels Like", GeoUtils.formatTemperature(cur.feelsLikeC, prefs.temperatureUnit), ins.feelsLikeExplanation, cur.origin),
        TelemetryItem("feels_like", "Heat Index", GeoUtils.formatTemperature(cur.heatIndexC, prefs.temperatureUnit), "Rothfusz apparent heat", DataOrigin.CALCULATED),
        TelemetryItem("feels_like", "Wind Chill", GeoUtils.formatTemperature(cur.windChillC, prefs.temperatureUnit), "NWS cold wind index", DataOrigin.CALCULATED),
        TelemetryItem("humidity", "Humidity", cur.humidityPercent?.let { "$it%" } ?: "N/A", ins.humidityComfort, cur.origin),
        TelemetryItem("dew_point", "Dew Point", GeoUtils.formatTemperature(cur.dewPointC, prefs.temperatureUnit), ins.dewPointComfort, cur.origin),
        TelemetryItem("wet_bulb", "Wet-Bulb Temperature", GeoUtils.formatTemperature(cur.wetBulbTempC, prefs.temperatureUnit), "Evaporative cooling threshold", DataOrigin.CALCULATED),
        TelemetryItem("pressure", "Pressure", GeoUtils.formatPressure(cur.pressureHpa, prefs.pressureUnit), ins.pressureAnalysis, cur.origin),
        TelemetryItem(
            "wind_direction",
            "Wind Speed & Direction",
            "${GeoUtils.formatWind(cur.windSpeedKmh, prefs.windUnit)} · From ${GeoUtils.compassDirection(cur.windDirectionDeg)} (${cur.windDirectionDeg ?: "--"}°)",
            "Beaufort ${beaufort.first} (${beaufort.second}) · Direction FROM which wind is coming",
            cur.origin
        ),
        TelemetryItem("wind_direction", "Wind Gusts", GeoUtils.formatWind(cur.windGustKmh, prefs.windUnit), ins.cycloneWindRisk ?: ins.stormTrend, cur.origin),
        TelemetryItem("visibility", "Visibility", cur.visibilityKm?.let { "${String.format(Locale.US, "%.1f", it)} km" } ?: "N/A", ins.visibilityRisk, cur.origin),
        TelemetryItem(null, "Cloud Cover (Total)", cur.cloudCoverPercent?.let { "$it%" } ?: "N/A", "Sky opacity", cur.origin),
        TelemetryItem(null, "Cloud Layers (L / M / H)", listOf(cur.cloudLowPercent, cur.cloudMidPercent, cur.cloudHighPercent).joinToString(" / ") { it?.let { v -> "$v%" } ?: "N/A" }, "Low / Mid / High altitude clouds", cur.origin),
        TelemetryItem(null, "Precipitation", cur.precipitationMm?.let { "${String.format(Locale.US, "%.1f", it)} mm" } ?: "N/A", ins.rainTrend, cur.origin),
        TelemetryItem(null, "Rain / Snow / Sleet / Hail", "${cur.rainMm?.let { "${String.format(Locale.US, "%.1f", it)} mm" } ?: "N/A"} / ${cur.snowCm?.let { "${String.format(Locale.US, "%.1f", it)} cm" } ?: "N/A"} / ${cur.sleetMm?.let { "${String.format(Locale.US, "%.1f", it)} mm" } ?: "N/A"} / ${cur.hailMm?.let { "${String.format(Locale.US, "%.1f", it)} mm" } ?: "N/A"}", "Phase-separated precipitation", cur.origin),
        TelemetryItem(null, "Rain / Snow Probability", "${cur.rainProbabilityPercent?.let { "$it%" } ?: "N/A"} / ${cur.snowProbabilityPercent?.let { "$it%" } ?: "N/A"}", "Next hour likelihood", DataOrigin.FORECAST),
        TelemetryItem("uv", "UV Index", cur.uvIndex?.let { "${String.format(Locale.US, "%.1f", it)} (${ins.uvExposureLevel})" } ?: "N/A", "Solar UV exposure", cur.origin),
        TelemetryItem("uv", "Solar Radiation", cur.solarRadiationWm2?.let { "${it.roundToInt()} W/m²" } ?: "N/A", "Shortwave irradiance", cur.origin),
        TelemetryItem(null, "Freezing Level", cur.freezingLevelMeters?.let { "${it.roundToInt()} m" } ?: "N/A", "0°C isotherm altitude", cur.origin),
        TelemetryItem(null, "Surface / Soil Temperature", "${GeoUtils.formatTemperature(cur.surfaceTempC, prefs.temperatureUnit)} / ${GeoUtils.formatTemperature(cur.soilTempC, prefs.temperatureUnit)}", "0 cm ground level", cur.origin),
        TelemetryItem(null, "Soil Moisture", cur.soilMoistureM3?.let { "${String.format(Locale.US, "%.3f", it)} m³/m³" } ?: "N/A", "0–1 cm volumetric moisture", cur.origin),
        TelemetryItem("cape", "CAPE (Convective Energy)", cur.capeJoulesKg?.let { "${it.roundToInt()} J/kg" } ?: "N/A", if ((cur.capeJoulesKg ?: 0.0) > 1000) "Unstable convective atmosphere" else "Normal stability", cur.origin),
        TelemetryItem(null, "Outdoor Comfort Score", "${ins.outdoorComfortScore} / 100", ins.outdoorComfortLabel, DataOrigin.CALCULATED)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${bundle.location.name} · ${stringResource(R.string.detailed_metrics)}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (ins.landslideRisk != null || ins.cycloneWindRisk != null) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                ins.cycloneWindRisk?.let { Text("Wind Hazard: $it", fontWeight = FontWeight.Bold) }
                                ins.landslideRisk?.let { Text("Terrain Risk: $it", fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
                items(rows) { item ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(item.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    val entry = item.glossaryKey?.let { MetricGlossary.find(it) }
                                    if (entry != null) {
                                        IconButton(
                                            onClick = { activeGlossary = entry },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Outlined.HelpOutline,
                                                contentDescription = "What does ${item.label} mean?",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                                OriginBadge(item.origin)
                            }
                            Text(item.value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text(item.detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    activeGlossary?.let { entry ->
        val isBn = prefs.language == AppLanguage.BENGALI
        AlertDialog(
            onDismissRequest = { activeGlossary = null },
            title = { Text(if (isBn) entry.titleBn else entry.titleEn) },
            text = { Text(if (isBn) entry.explanationBn else entry.explanationEn) },
            confirmButton = {
                TextButton(onClick = { activeGlossary = null }) { Text("OK") }
            }
        )
    }
}
