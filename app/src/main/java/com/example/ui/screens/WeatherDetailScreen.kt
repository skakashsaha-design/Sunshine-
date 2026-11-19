package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.calc.GeoUtils
import com.example.domain.model.*
import com.example.ui.components.OriginBadge
import java.util.Locale
import kotlin.math.roundToInt

private data class TelemetryItem(
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

    val rows = listOf(
        TelemetryItem("Temperature", GeoUtils.formatTemperature(cur.temperatureC, prefs.temperatureUnit), cur.conditionLabel, cur.origin),
        TelemetryItem("Feels Like", GeoUtils.formatTemperature(cur.feelsLikeC, prefs.temperatureUnit), ins.feelsLikeExplanation, cur.origin),
        TelemetryItem("Heat Index", GeoUtils.formatTemperature(cur.heatIndexC, prefs.temperatureUnit), "Rothfusz apparent heat", DataOrigin.CALCULATED),
        TelemetryItem("Wind Chill", GeoUtils.formatTemperature(cur.windChillC, prefs.temperatureUnit), "NWS cold wind index", DataOrigin.CALCULATED),
        TelemetryItem("Humidity", cur.humidityPercent?.let { "$it%" } ?: "N/A", ins.humidityComfort, cur.origin),
        TelemetryItem("Dew Point", GeoUtils.formatTemperature(cur.dewPointC, prefs.temperatureUnit), ins.dewPointComfort, cur.origin),
        TelemetryItem("Wet-Bulb Temperature", GeoUtils.formatTemperature(cur.wetBulbTempC, prefs.temperatureUnit), "Evaporative cooling threshold", DataOrigin.CALCULATED),
        TelemetryItem("Pressure", GeoUtils.formatPressure(cur.pressureHpa, prefs.pressureUnit), ins.pressureAnalysis, cur.origin),
        TelemetryItem("Wind Speed & Direction", "${GeoUtils.formatWind(cur.windSpeedKmh, prefs.windUnit)} (${GeoUtils.compassDirection(cur.windDirectionDeg)} ${cur.windDirectionDeg?.let { "$it°" } ?: ""})", "10m surface wind", cur.origin),
        TelemetryItem("Wind Gusts", GeoUtils.formatWind(cur.windGustKmh, prefs.windUnit), ins.cycloneWindRisk ?: ins.stormTrend, cur.origin),
        TelemetryItem("Visibility", cur.visibilityKm?.let { "${String.format(Locale.US, "%.1f", it)} km" } ?: "N/A", ins.visibilityRisk, cur.origin),
        TelemetryItem("Cloud Cover (Total)", cur.cloudCoverPercent?.let { "$it%" } ?: "N/A", "Sky opacity", cur.origin),
        TelemetryItem("Cloud Layers (L / M / H)", listOf(cur.cloudLowPercent, cur.cloudMidPercent, cur.cloudHighPercent).joinToString(" / ") { it?.let { v -> "$v%" } ?: "N/A" }, "Low / Mid / High altitude clouds", cur.origin),
        TelemetryItem("Precipitation", cur.precipitationMm?.let { "${String.format(Locale.US, "%.1f", it)} mm" } ?: "N/A", ins.rainTrend, cur.origin),
        TelemetryItem("Rain / Snow / Sleet / Hail", "${cur.rainMm?.let { "${String.format(Locale.US, "%.1f", it)} mm" } ?: "N/A"} / ${cur.snowCm?.let { "${String.format(Locale.US, "%.1f", it)} cm" } ?: "N/A"} / ${cur.sleetMm?.let { "${String.format(Locale.US, "%.1f", it)} mm" } ?: "N/A"} / ${cur.hailMm?.let { "${String.format(Locale.US, "%.1f", it)} mm" } ?: "N/A"}", "Phase-separated precipitation", cur.origin),
        TelemetryItem("Rain / Snow Probability", "${cur.rainProbabilityPercent?.let { "$it%" } ?: "N/A"} / ${cur.snowProbabilityPercent?.let { "$it%" } ?: "N/A"}", "Next hour likelihood", DataOrigin.FORECAST),
        TelemetryItem("UV Index", cur.uvIndex?.let { "${String.format(Locale.US, "%.1f", it)} (${ins.uvExposureLevel})" } ?: "N/A", "Solar UV exposure", cur.origin),
        TelemetryItem("Solar Radiation", cur.solarRadiationWm2?.let { "${it.roundToInt()} W/m²" } ?: "N/A", "Shortwave irradiance", cur.origin),
        TelemetryItem("Freezing Level", cur.freezingLevelMeters?.let { "${it.roundToInt()} m" } ?: "N/A", "0°C isotherm altitude", cur.origin),
        TelemetryItem("Surface / Soil Temperature", "${GeoUtils.formatTemperature(cur.surfaceTempC, prefs.temperatureUnit)} / ${GeoUtils.formatTemperature(cur.soilTempC, prefs.temperatureUnit)}", "0 cm ground level", cur.origin),
        TelemetryItem("Soil Moisture", cur.soilMoistureM3?.let { "${String.format(Locale.US, "%.3f", it)} m³/m³" } ?: "N/A", "0–1 cm volumetric moisture", cur.origin),
        TelemetryItem("CAPE (Convective Energy)", cur.capeJoulesKg?.let { "${it.roundToInt()} J/kg" } ?: "N/A", if ((cur.capeJoulesKg ?: 0.0) > 1000) "Unstable convective atmosphere" else "Normal stability", cur.origin),
        TelemetryItem("Outdoor Comfort Score", "${ins.outdoorComfortScore} / 100", ins.outdoorComfortLabel, DataOrigin.CALCULATED)
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
                                Text(item.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
}
