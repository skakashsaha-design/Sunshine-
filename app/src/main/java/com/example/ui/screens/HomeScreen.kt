package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.repository.AlertsAndDisastersBundle
import com.example.domain.calc.AqiCalculator
import com.example.domain.calc.GeoUtils
import com.example.domain.model.*
import com.example.ui.UiState
import com.example.ui.components.AnimatedWeatherIcon
import com.example.ui.components.OriginBadge
import com.example.ui.components.SeverityChip
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    weatherState: UiState<WeatherBundle>,
    aqiState: UiState<AirQualityData>,
    astronomy: AstronomySnapshot?,
    alertsState: UiState<AlertsAndDisastersBundle>,
    savedLocations: List<SavedLocation>,
    activeLocation: SavedLocation?,
    prefs: UserPreferences,
    onRefresh: () -> Unit,
    onSelectLocation: (SavedLocation) -> Unit,
    onRequestGps: () -> Unit,
    onOpenLocations: () -> Unit,
    onOpenDetail: () -> Unit,
    onOpenForecast: () -> Unit,
    onSelectHour: (HourlyForecast) -> Unit,
    onOpenAqi: () -> Unit,
    onOpenAstronomy: () -> Unit,
    onOpenAlerts: () -> Unit,
    onSelectAlert: (WeatherAlert) -> Unit,
    modifier: Modifier = Modifier
) {
    var locationMenuOpen by remember { mutableStateOf(false) }

    val gpsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) onRequestGps()
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Location & Action Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { locationMenuOpen = true }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                .testTag("home_location_selector"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Location",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            val flag = GeoUtils.countryCodeToFlagEmoji(activeLocation?.countryCode)
                            Text(
                                text = listOfNotNull(flag.takeIf { it.isNotBlank() }, activeLocation?.name ?: "Select City").joinToString(" "),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Switch location"
                            )
                        }

                        DropdownMenu(
                            expanded = locationMenuOpen,
                            onDismissRequest = { locationMenuOpen = false }
                        ) {
                            savedLocations.forEach { loc ->
                                DropdownMenuItem(
                                    text = {
                                        Text("${GeoUtils.countryCodeToFlagEmoji(loc.countryCode)} ${loc.name}")
                                    },
                                    onClick = {
                                        locationMenuOpen = false
                                        onSelectLocation(loc)
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.title_locations)) },
                                leadingIcon = { Icon(Icons.Default.EditLocationAlt, contentDescription = null) },
                                onClick = {
                                    locationMenuOpen = false
                                    onOpenLocations()
                                }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                gpsPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    )
                                )
                            },
                            modifier = Modifier.testTag("home_gps_button")
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = stringResource(R.string.use_gps))
                        }
                        IconButton(
                            onClick = onOpenLocations,
                            modifier = Modifier.testTag("home_search_button")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search locations")
                        }
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.testTag("home_refresh_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                        }
                    }
                }
            }

            when (weatherState) {
                is UiState.Loading, UiState.Idle -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator()
                                Text("Syncing meteorological telemetry...", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Weather Unavailable", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(weatherState.message, style = MaterialTheme.typography.bodyMedium)
                                Button(onClick = onRefresh) { Text("Retry") }
                            }
                        }
                    }
                }
                is UiState.Success -> {
                    val bundle = weatherState.data
                    val cur = bundle.current
                    val today = bundle.daily.firstOrNull()
                    val timeFmt = SimpleDateFormat("HH:mm", Locale.US).format(Date(bundle.lastUpdatedEpochMs))

                    // Status & Provider Transparency Strip
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val statusBg = if (bundle.isOfflineCache) SeverityModerate else SeverityLow
                                Surface(color = statusBg.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                                    Text(
                                        text = if (bundle.isOfflineCache) stringResource(R.string.status_offline) else stringResource(R.string.status_online),
                                        color = statusBg,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    text = "${bundle.activeProvider.displayName} · ${stringResource(R.string.status_last_updated, timeFmt)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (bundle.usedFallback && bundle.fallbackReason != null) {
                                Surface(
                                    color = SeverityModerate.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = stringResource(R.string.status_fallback_used, "${bundle.activeProvider.displayName} (${bundle.fallbackReason})"),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Hero Current Weather Card (Tappable -> Detailed Weather)
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .clickable(onClick = onOpenDetail)
                                .testTag("home_hero_card"),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                            tonalElevation = 3.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    OriginBadge(origin = cur.origin)
                                    Text(
                                        text = GeoUtils.formatTemperature(cur.temperatureC, prefs.temperatureUnit),
                                        style = MaterialTheme.typography.displayLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = cur.conditionLabel,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.feels_like,
                                            GeoUtils.formatTemperature(cur.feelsLikeC, prefs.temperatureUnit)
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (today != null) {
                                        Text(
                                            text = stringResource(
                                                R.string.high_low,
                                                GeoUtils.formatTemperature(today.tempMaxC, prefs.temperatureUnit),
                                                GeoUtils.formatTemperature(today.tempMinC, prefs.temperatureUnit)
                                            ),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Tap for 30+ telemetry parameters →",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }

                                AnimatedWeatherIcon(
                                    condition = cur.condition,
                                    isDay = cur.isDay,
                                    animationLevel = prefs.animationLevel,
                                    size = 94.dp
                                )
                            }
                        }
                    }

                    // Important Alert Banner (if active alerts exist)
                    val topAlert = (alertsState as? UiState.Success)?.data?.alerts?.firstOrNull()
                    if (topAlert != null) {
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onSelectAlert(topAlert) }
                                    .testTag("home_alert_banner"),
                                color = SeverityExtreme.copy(alpha = 0.16f),
                                tonalElevation = 2.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = "Alert", tint = SeverityExtreme)
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            SeverityChip(topAlert.severity)
                                            OriginBadge(topAlert.origin)
                                        }
                                        Text(
                                            text = topAlert.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = topAlert.sourceName,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = "Open alert")
                                }
                            }
                        }
                    }

                    // Hourly Outlook Strip
                    if (bundle.hourly.isNotEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.hourly_outlook),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        TextButton(onClick = onOpenForecast) {
                                            Text(stringResource(R.string.view_all))
                                        }
                                    }
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(bundle.hourly.take(24)) { hour ->
                                            Surface(
                                                modifier = Modifier
                                                    .width(74.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .clickable { onSelectHour(hour) },
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = hour.isoTime.substringAfter("T", "").take(5).ifBlank { "--" },
                                                        style = MaterialTheme.typography.labelMedium
                                                    )
                                                    AnimatedWeatherIcon(
                                                        condition = hour.condition,
                                                        isDay = hour.isDay,
                                                        animationLevel = AnimationLevel.OFF,
                                                        size = 30.dp
                                                    )
                                                    Text(
                                                        text = GeoUtils.formatTemperature(hour.temperatureC, prefs.temperatureUnit, false),
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    val prob = hour.precipitationProbabilityPercent
                                                    Text(
                                                        text = if (prob != null && prob > 0) "$prob%" else "",
                                                        style = MaterialTheme.typography.labelSmall,
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

                    // Today's Summary & Daily Safety Advisor
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text(
                                        text = stringResource(R.string.todays_summary),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                bundle.summaryLines.forEach { line ->
                                    Text(text = "• $line", style = MaterialTheme.typography.bodyMedium)
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Text(
                                    text = stringResource(R.string.daily_advisor),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                bundle.dailyAdvisorLines.take(3).forEach { tip ->
                                    Text(text = "• $tip", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }

                    // Essential 2x2 Quick Metric Cards (Rain, AQI, Wind, Sunrise/Sunset)
                    item {
                        val aqiData = (aqiState as? UiState.Success)?.data
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HomeMetricTile(
                                    title = stringResource(R.string.rain_probability),
                                    value = cur.rainProbabilityPercent?.let { "$it%" } ?: "N/A",
                                    subtitle = cur.precipitationMm?.let { "${String.format(Locale.US, "%.1f", it)} mm now" } ?: "Tap for forecast",
                                    icon = Icons.Default.WaterDrop,
                                    onClick = onOpenForecast,
                                    modifier = Modifier.weight(1f)
                                )
                                HomeMetricTile(
                                    title = stringResource(R.string.air_quality),
                                    value = aqiData?.usAqi?.toString() ?: "N/A",
                                    subtitle = aqiData?.category?.label ?: "Tap for pollutants",
                                    icon = Icons.Default.Air,
                                    accentColor = aqiData?.usAqi?.let { aqiColor(it) },
                                    onClick = onOpenAqi,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HomeMetricTile(
                                    title = stringResource(R.string.wind),
                                    value = GeoUtils.formatWind(cur.windSpeedKmh, prefs.windUnit),
                                    subtitle = "${GeoUtils.compassDirection(cur.windDirectionDeg)} · Gust ${GeoUtils.formatWind(cur.windGustKmh, prefs.windUnit)}",
                                    icon = Icons.Default.Explore,
                                    onClick = onOpenDetail,
                                    modifier = Modifier.weight(1f)
                                )
                                HomeMetricTile(
                                    title = stringResource(R.string.sunrise_sunset),
                                    value = astronomy?.sun?.sunriseTime ?: "N/A",
                                    subtitle = "Set ${astronomy?.sun?.sunsetTime ?: "N/A"} · ${astronomy?.moon?.phaseType?.label ?: ""}",
                                    icon = Icons.Default.WbTwilight,
                                    onClick = onOpenAstronomy,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Selected Useful Metrics Bar + Full Telemetry Link
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable(onClick = onOpenDetail),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = stringResource(R.string.detailed_metrics),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "UV ${cur.uvIndex?.roundToInt() ?: "N/A"} · RH ${cur.humidityPercent?.let { "$it%" } ?: "N/A"} · ${GeoUtils.formatPressure(cur.pressureHpa, prefs.pressureUnit)} · Comfort ${bundle.insights.outdoorComfortScore}/100",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = "Open full telemetry")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeMetricTile(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color? = null
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(icon, contentDescription = title, tint = accentColor ?: MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor ?: MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

fun aqiColor(usAqi: Int): Color = when {
    usAqi <= 50 -> AqiGood
    usAqi <= 100 -> AqiModerate
    usAqi <= 150 -> AqiSensitive
    usAqi <= 200 -> AqiUnhealthy
    usAqi <= 300 -> AqiVeryUnhealthy
    else -> AqiHazardous
}
