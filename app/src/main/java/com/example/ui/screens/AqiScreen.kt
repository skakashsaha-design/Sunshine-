package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.AirQualityData
import com.example.ui.UiState
import com.example.ui.components.ChartMetricType
import com.example.ui.components.ChartPoint
import com.example.ui.components.InteractiveWeatherChart
import com.example.ui.components.OriginBadge
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AqiScreen(
    aqiState: UiState<AirQualityData>,
    cityName: String,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) {
        if (aqiState is UiState.Idle) onRefresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$cityName · ${stringResource(R.string.title_aqi)}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh AQI")
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
            when (aqiState) {
                is UiState.Loading, UiState.Idle -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(aqiState.message)
                    }
                }
                is UiState.Success -> {
                    val data = aqiState.data
                    val color = aqiColor(data.usAqi ?: 50)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 720.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = color.copy(alpha = 0.16f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("US AQI: ${data.usAqi ?: "N/A"}", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = color)
                                        OriginBadge(data.origin)
                                    }
                                    Text("${data.category.label} · European AQI: ${data.europeanAqi ?: "N/A"}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text(data.healthGuidance, style = MaterialTheme.typography.bodyMedium)
                                    Text("Source: ${data.sourceLabel} (Model / Satellite Estimate)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        // Best & Worst Outdoor Windows
                        if (data.bestOutdoorWindow != null || data.worstOutdoorWindow != null) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    data.bestOutdoorWindow?.let { best ->
                                        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 2.dp, modifier = Modifier.weight(1f)) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(best.label, style = MaterialTheme.typography.labelMedium, color = aqiColor(best.avgAqi), fontWeight = FontWeight.Bold)
                                                Text(best.timeRange, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                Text("Avg AQI ${best.avgAqi}", style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }
                                    }
                                    data.worstOutdoorWindow?.let { worst ->
                                        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 2.dp, modifier = Modifier.weight(1f)) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(worst.label, style = MaterialTheme.typography.labelMedium, color = aqiColor(worst.avgAqi), fontWeight = FontWeight.Bold)
                                                Text(worst.timeRange, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                Text("Avg AQI ${worst.avgAqi}", style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // AQI Trend Chart
                        item {
                            Surface(shape = RoundedCornerShape(18.dp), tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("48h AQI Forecast · ${data.trendLabel}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    InteractiveWeatherChart(
                                        points = data.hourlyAqi.take(24).mapNotNull { h ->
                                            h.usAqi?.let { ChartPoint(h.isoTime.substringAfter("T", "").take(5), it.toDouble()) }
                                        },
                                        metric = ChartMetricType.AQI,
                                        lineColor = color
                                    )
                                }
                            }
                        }

                        item {
                            Text("Pollutant Breakdown", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }

                        items(data.pollutants) { p ->
                            Surface(shape = RoundedCornerShape(14.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("${p.code} — ${p.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        p.subIndexUs?.let { Text("US Sub-Index: $it", style = MaterialTheme.typography.labelMedium, color = aqiColor(it)) }
                                    }
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", p.value)} ${p.unit}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
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
