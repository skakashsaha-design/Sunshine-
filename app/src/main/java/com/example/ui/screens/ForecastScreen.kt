package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.calc.GeoUtils
import com.example.domain.model.*
import com.example.ui.UiState
import com.example.ui.components.*
import com.example.ui.theme.SkyAmber
import com.example.ui.theme.SkyCobalt
import com.example.ui.theme.SkyCyan
import java.util.Locale

@Composable
fun ForecastScreen(
    weatherState: UiState<WeatherBundle>,
    aqiState: UiState<AirQualityData>,
    prefs: UserPreferences,
    selectedHour: HourlyForecast?,
    selectedDay: DailyForecast?,
    onSelectHour: (HourlyForecast?) -> Unit,
    onSelectDay: (DailyForecast?) -> Unit,
    modifier: Modifier = Modifier
) {
    var tabIndex by remember { mutableIntStateOf(0) }
    var chartMetric by remember { mutableStateOf(ChartMetricType.TEMPERATURE) }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        when (weatherState) {
            is UiState.Loading, UiState.Idle -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(weatherState.message, style = MaterialTheme.typography.bodyLarge)
                }
            }
            is UiState.Success -> {
                val bundle = weatherState.data
                val aqiData = (aqiState as? UiState.Success)?.data

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 720.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "${bundle.location.name} Forecast",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = tabIndex == 0,
                                onClick = { tabIndex = 0 },
                                shape = SegmentedButtonDefaults.itemShape(0, 2),
                                modifier = Modifier.testTag("forecast_tab_hourly")
                            ) {
                                Text("Hourly (${bundle.hourly.size}h)")
                            }
                            SegmentedButton(
                                selected = tabIndex == 1,
                                onClick = { tabIndex = 1 },
                                shape = SegmentedButtonDefaults.itemShape(1, 2),
                                modifier = Modifier.testTag("forecast_tab_daily")
                            ) {
                                Text("Daily (${bundle.daily.size}d)")
                            }
                        }
                    }

                    if (tabIndex == 0) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                tonalElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(ChartMetricType.entries) { m ->
                                            FilterChip(
                                                selected = chartMetric == m,
                                                onClick = { chartMetric = m },
                                                label = { Text(m.label) }
                                            )
                                        }
                                    }

                                    val points: List<ChartPoint> = when (chartMetric) {
                                        ChartMetricType.TEMPERATURE -> bundle.hourly.take(24).map {
                                            ChartPoint(it.isoTime.substringAfter("T", "").take(5), GeoUtils.convertTemperature(it.temperatureC, prefs.temperatureUnit))
                                        }
                                        ChartMetricType.RAIN_PROB -> bundle.hourly.take(24).mapNotNull { h ->
                                            h.precipitationProbabilityPercent?.let { ChartPoint(h.isoTime.substringAfter("T", "").take(5), it.toDouble()) }
                                        }
                                        ChartMetricType.PRECIP -> bundle.hourly.take(24).mapNotNull { h ->
                                            h.precipitationMm?.let { ChartPoint(h.isoTime.substringAfter("T", "").take(5), it) }
                                        }
                                        ChartMetricType.WIND -> bundle.hourly.take(24).mapNotNull { h ->
                                            h.windSpeedKmh?.let { ChartPoint(h.isoTime.substringAfter("T", "").take(5), GeoUtils.convertWind(it, prefs.windUnit)) }
                                        }
                                        ChartMetricType.AQI -> aqiData?.hourlyAqi?.take(24)?.mapNotNull { h ->
                                            h.usAqi?.let { ChartPoint(h.isoTime.substringAfter("T", "").take(5), it.toDouble()) }
                                        }.orEmpty()
                                    }

                                    InteractiveWeatherChart(
                                        points = points,
                                        metric = chartMetric,
                                        lineColor = when (chartMetric) {
                                            ChartMetricType.TEMPERATURE -> SkyAmber
                                            ChartMetricType.RAIN_PROB, ChartMetricType.PRECIP -> SkyCyan
                                            ChartMetricType.WIND -> SkyCobalt
                                            ChartMetricType.AQI -> aqiColor(aqiData?.usAqi ?: 50)
                                        },
                                        isBarChart = chartMetric == ChartMetricType.RAIN_PROB || chartMetric == ChartMetricType.PRECIP
                                    )
                                }
                            }
                        }

                        items(bundle.hourly) { hour ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onSelectHour(hour) },
                                tonalElevation = 1.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1.2f)) {
                                        Text(
                                            text = hour.isoTime.substringAfter("T", hour.isoTime).take(5),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = hour.isoTime.take(10),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    AnimatedWeatherIcon(hour.condition, hour.isDay, AnimationLevel.OFF, 34.dp)
                                    Column(modifier = Modifier.weight(1.3f).padding(horizontal = 8.dp)) {
                                        Text(hour.conditionLabel, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                        Text(
                                            text = "Rain ${hour.precipitationProbabilityPercent?.let { "$it%" } ?: "N/A"} · ${GeoUtils.formatWind(hour.windSpeedKmh, prefs.windUnit)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = GeoUtils.formatTemperature(hour.temperatureC, prefs.temperatureUnit),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        items(bundle.daily) { day ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { onSelectDay(day) },
                                tonalElevation = 2.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1.3f)) {
                                        Text(day.isoDate, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(day.conditionLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    AnimatedWeatherIcon(day.condition, true, AnimationLevel.OFF, 36.dp)
                                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1.1f)) {
                                        Text(
                                            text = "${GeoUtils.formatTemperature(day.tempMaxC, prefs.temperatureUnit, false)} / ${GeoUtils.formatTemperature(day.tempMinC, prefs.temperatureUnit, false)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Rain ${day.rainProbabilityPercent?.let { "$it%" } ?: "N/A"} · ${day.rainAmountMm?.let { "${String.format(Locale.US, "%.1f", it)} mm" } ?: ""}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = SkyCyan
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Hour Detail Modal
    selectedHour?.let { h ->
        AlertDialog(
            onDismissRequest = { onSelectHour(null) },
            title = { Text("Hour Detail · ${h.isoTime.replace("T", " ").take(16)}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OriginBadge(h.origin)
                    Text("Condition: ${h.conditionLabel}")
                    Text("Temperature: ${GeoUtils.formatTemperature(h.temperatureC, prefs.temperatureUnit)}")
                    Text("Feels Like: ${GeoUtils.formatTemperature(h.feelsLikeC, prefs.temperatureUnit)}")
                    Text("Rain Probability: ${h.precipitationProbabilityPercent?.let { "$it%" } ?: "N/A"}")
                    Text("Precipitation: ${h.precipitationMm?.let { "$it mm" } ?: "N/A"}")
                    Text("Wind: ${GeoUtils.formatWind(h.windSpeedKmh, prefs.windUnit)} (${GeoUtils.compassDirection(h.windDirectionDeg)})")
                    Text("Gusts: ${GeoUtils.formatWind(h.windGustKmh, prefs.windUnit)}")
                    Text("Humidity: ${h.humidityPercent?.let { "$it%" } ?: "N/A"} · Dew Point: ${GeoUtils.formatTemperature(h.dewPointC, prefs.temperatureUnit)}")
                    Text("Pressure: ${GeoUtils.formatPressure(h.pressureHpa, prefs.pressureUnit)}")
                    Text("Cloud Cover: ${h.cloudCoverPercent?.let { "$it%" } ?: "N/A"} · UV: ${h.uvIndex?.toString() ?: "N/A"}")
                    Text("Visibility: ${h.visibilityKm?.let { "${String.format(Locale.US, "%.1f", it)} km" } ?: "N/A"}")
                }
            },
            confirmButton = {
                TextButton(onClick = { onSelectHour(null) }) { Text("Close") }
            }
        )
    }

    // Day Detail Modal
    selectedDay?.let { d ->
        AlertDialog(
            onDismissRequest = { onSelectDay(null) },
            title = { Text("Day Detail · ${d.isoDate}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OriginBadge(d.origin)
                    Text("Condition: ${d.conditionLabel}")
                    Text("High / Low / Avg: ${GeoUtils.formatTemperature(d.tempMaxC, prefs.temperatureUnit)} / ${GeoUtils.formatTemperature(d.tempMinC, prefs.temperatureUnit)} / ${GeoUtils.formatTemperature(d.tempAvgC, prefs.temperatureUnit)}")
                    Text("Rain Prob & Sum: ${d.rainProbabilityPercent?.let { "$it%" } ?: "N/A"} · ${d.rainAmountMm?.let { "$it mm" } ?: "N/A"}")
                    Text("Snow Prob & Sum: ${d.snowProbabilityPercent?.let { "$it%" } ?: "N/A"} · ${d.snowAmountCm?.let { "$it cm" } ?: "N/A"}")
                    Text("Max Wind / Gusts: ${GeoUtils.formatWind(d.windMaxKmh, prefs.windUnit)} / ${GeoUtils.formatWind(d.windGustMaxKmh, prefs.windUnit)}")
                    Text("UV Max: ${d.uvIndexMax?.toString() ?: "N/A"} · Humidity Avg: ${d.humidityAvgPercent?.let { "$it%" } ?: "N/A"}")
                    Text("Sunrise / Sunset: ${d.sunriseIso ?: "N/A"} / ${d.sunsetIso ?: "N/A"}")
                    d.moonPhaseName?.let { Text("Moon Phase: $it") }
                }
            },
            confirmButton = {
                TextButton(onClick = { onSelectDay(null) }) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Close")
                }
            }
        )
    }
}
