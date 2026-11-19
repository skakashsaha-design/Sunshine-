package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.repository.AlertsAndDisastersBundle
import com.example.domain.model.*
import com.example.ui.UiState
import com.example.ui.components.OriginBadge
import com.example.ui.components.SeverityChip
import com.example.ui.theme.SeverityExtreme
import com.example.ui.theme.SeverityLow
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisasterCenterScreen(
    alertsState: UiState<AlertsAndDisastersBundle>,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    onShowOnMap: (Double, Double) -> Unit
) {
    LaunchedEffect(Unit) {
        if (alertsState is UiState.Idle) onRefresh()
    }

    var selectedFilter by remember { mutableStateOf<DisasterCategory?>(null) }
    var minQuakeMag by remember { mutableFloatStateOf(2.5f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_disasters)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
            when (alertsState) {
                is UiState.Loading, UiState.Idle -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(alertsState.message)
                    }
                }
                is UiState.Success -> {
                    val data = alertsState.data
                    val filtered = data.disasters.filter { ev ->
                        val catMatch = selectedFilter == null || ev.category == selectedFilter
                        val magMatch = if (ev.category == DisasterCategory.EARTHQUAKE) {
                            val mag = ev.magnitudeOrIntensity?.removePrefix("M")?.toFloatOrNull() ?: 0f
                            mag >= minQuakeMag
                        } else true
                        catMatch && magMatch
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 720.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Tsunami Banner
                        item {
                            val ts = data.tsunamiStatus
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = if (ts.activeWarning) SeverityExtreme.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Tsunami Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        OriginBadge(DataOrigin.GLOBAL_OFFICIAL)
                                    }
                                    Text(ts.statusLabel, fontWeight = FontWeight.SemiBold, color = if (ts.activeWarning) SeverityExtreme else SeverityLow)
                                    Text("Region: ${ts.region} · Source: ${ts.source}", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        // River Flood Outlook Card
                        data.floodOutlook?.let { flood ->
                            item {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    tonalElevation = 2.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("River Flood Outlook (GloFAS)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            OriginBadge(flood.origin)
                                        }
                                        Text("Risk Level: ${flood.riskLevel}", fontWeight = FontWeight.SemiBold)
                                        Text(flood.summary, style = MaterialTheme.typography.bodyMedium)
                                        if (flood.meanDischargeHistoricalM3s != null) {
                                            Text(
                                                "Historical Mean: ${String.format(Locale.US, "%.1f", flood.meanDischargeHistoricalM3s)} m³/s",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Category Filter Chips
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    FilterChip(
                                        selected = selectedFilter == null,
                                        onClick = { selectedFilter = null },
                                        label = { Text("All (${data.disasters.size})") }
                                    )
                                }
                                items(DisasterCategory.entries) { cat ->
                                    val count = data.disasters.count { it.category == cat }
                                    if (count > 0) {
                                        FilterChip(
                                            selected = selectedFilter == cat,
                                            onClick = { selectedFilter = if (selectedFilter == cat) null else cat },
                                            label = { Text("${cat.label} ($count)") }
                                        )
                                    }
                                }
                            }
                        }

                        // Earthquake Magnitude Filter Slider
                        if (selectedFilter == null || selectedFilter == DisasterCategory.EARTHQUAKE) {
                            item {
                                Column {
                                    Text(
                                        text = "Earthquake Minimum Magnitude: M${String.format(Locale.US, "%.1f", minQuakeMag)}",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Slider(
                                        value = minQuakeMag,
                                        onValueChange = { minQuakeMag = it },
                                        valueRange = 2.5f..7.0f
                                    )
                                }
                            }
                        }

                        items(filtered) { ev ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { onShowOnMap(ev.latitude, ev.longitude) },
                                tonalElevation = 2.dp
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        SeverityChip(ev.severity)
                                        OriginBadge(ev.origin)
                                    }
                                    Text(ev.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = buildString {
                                            append(ev.category.label)
                                            ev.magnitudeOrIntensity?.let { append(" · $it") }
                                            ev.depthKm?.let { append(" · Depth ${it.roundToInt()} km") }
                                            if (ev.tsunamiFlag) append(" · TSUNAMI FLAG")
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Coords: ${String.format(Locale.US, "%.2f, %.2f", ev.latitude, ev.longitude)} · ${ev.distanceKm?.roundToInt() ?: "--"} km away",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("${ev.source} · ${ev.eventTimeIso}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Tap for Map →", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
