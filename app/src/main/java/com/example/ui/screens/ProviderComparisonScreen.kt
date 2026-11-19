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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$cityName · ${stringResource(R.string.title_providers)}") },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
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
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 720.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(entries) { e ->
                            val isOnline = e.status == "Online"
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                tonalElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(e.providerId.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Surface(
                                            color = (if (isOnline) SeverityLow else SeverityModerate).copy(alpha = 0.18f),
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
