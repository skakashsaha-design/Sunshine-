package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ProviderRegistry
import com.example.domain.calc.GeoUtils
import com.example.domain.model.ProviderComparisonEntry
import com.example.domain.model.UserPreferences
import com.example.ui.UiState
import com.example.ui.theme.SeverityLow
import com.example.ui.theme.SeverityModerate
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderComparisonScreen(
    comparisonState: UiState<List<ProviderComparisonEntry>>,
    cityName: String,
    prefs: UserPreferences,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) {
        if (comparisonState is UiState.Idle) onRefresh()
    }

    var tabIndex by remember { mutableIntStateOf(0) }
    val registry = remember(prefs.personalKeys) { ProviderRegistry.catalog(prefs.personalKeys) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$cityName · Data Sources") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Run comparison")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                SegmentedButton(
                    selected = tabIndex == 0,
                    onClick = { tabIndex = 0 },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text("Live Comparison") }
                SegmentedButton(
                    selected = tabIndex == 1,
                    onClick = { tabIndex = 1 },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text("Ecosystem (${registry.size})") }
            }

            if (tabIndex == 1) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().widthIn(max = 720.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(registry) { entry ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            tonalElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(entry.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Surface(
                                        color = (if (entry.isLiveAvailable) SeverityLow else SeverityModerate).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = entry.availabilityStatus,
                                            color = if (entry.isLiveAvailable) SeverityLow else SeverityModerate,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Text("${entry.category.label} · ${entry.supportedCountries}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                Text(entry.supportedFeatures, style = MaterialTheme.typography.bodyMedium)
                                Text("Auth: ${entry.authRequirement} · Attribution: ${entry.licenseAttribution}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            } else {
                when (comparisonState) {
                    is UiState.Loading, UiState.Idle -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(10.dp))
                            Text("Querying all 6+1 weather providers...")
                        }
                    }
                    is UiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(comparisonState.message)
                        }
                    }
                    is UiState.Success -> {
                        val entries = comparisonState.data
                        val fmt = SimpleDateFormat("HH:mm:ss", Locale.US)

                        LazyColumn(
                            modifier = Modifier.fillMaxSize().widthIn(max = 720.dp),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(entries) { e ->
                                val isOnline = e.status == "Online"
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    tonalElevation = 2.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(e.providerId.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Surface(
                                                color = (if (isOnline) SeverityLow else SeverityModerate).copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = e.status + (e.responseTimeMs?.let { " · ${it}ms" } ?: ""),
                                                    color = if (isOnline) SeverityLow else SeverityModerate,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }

                                        if (isOnline) {
                                            Text(
                                                text = "Temp: ${GeoUtils.formatTemperature(e.temperatureC, prefs.temperatureUnit)} (Feels ${GeoUtils.formatTemperature(e.feelsLikeC, prefs.temperatureUnit)}) · ${e.conditionLabel ?: "N/A"}",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "Rain Prob: ${e.rainProbabilityPercent?.let { "$it%" } ?: "N/A"} · Precip: ${e.precipitationMm?.let { "$it mm" } ?: "N/A"}",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "Wind: ${GeoUtils.formatWind(e.windSpeedKmh, prefs.windUnit)} · RH: ${e.humidityPercent?.let { "$it%" } ?: "N/A"} · Pressure: ${GeoUtils.formatPressure(e.pressureHpa, prefs.pressureUnit)} · UV: ${e.uvIndex?.toString() ?: "N/A"}",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            e.fetchedEpochMs?.let {
                                                Text("Timestamp: ${fmt.format(Date(it))}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        } else {
                                            Text(
                                                text = e.error ?: "Provider not configured",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    }
}
