package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.EmergencyNumbersDataset
import com.example.data.remote.ExtendedWeatherService
import com.example.domain.calc.GeoUtils
import com.example.domain.model.*
import com.example.ui.UiState
import com.example.ui.components.OriginBadge
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelComparisonScreen(
    modelState: UiState<ModelComparisonResult>,
    cityName: String,
    prefs: UserPreferences,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) {
        if (modelState is UiState.Idle) onRefresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$cityName · Model & Ensemble") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh models")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            when (modelState) {
                is UiState.Loading, UiState.Idle -> {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(10.dp))
                        Text("Comparing ECMWF, GFS, ICON, JMA & GEM models...")
                    }
                }
                is UiState.Error -> {
                    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(modelState.message, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onRefresh) { Text("Retry") }
                    }
                }
                is UiState.Success -> {
                    val data = modelState.data
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().widthIn(max = 720.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(data.agreementLabel, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                        OriginBadge(DataOrigin.CALCULATED)
                                    }
                                    Text("Forecast Confidence: ${data.confidenceLabel}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text(data.confidenceExplanation, style = MaterialTheme.typography.bodyMedium)
                                    if (data.ensembleMedianTempC != null && data.ensembleMinTempC != null && data.ensembleMaxTempC != null) {
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                        Text(
                                            text = "Multi-Model Temperature Ensemble: ${GeoUtils.formatTemperature(data.ensembleMedianTempC, prefs.temperatureUnit)} (Possible range: ${GeoUtils.formatTemperature(data.ensembleMinTempC, prefs.temperatureUnit, false)}–${GeoUtils.formatTemperature(data.ensembleMaxTempC, prefs.temperatureUnit)} · Spread ${String.format(Locale.US, "%.1f", data.ensembleSpreadC ?: 0.0)}°C)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "Wider spread indicates higher forecast uncertainty across global atmospheric models.",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                        }

                        items(data.models) { m ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                tonalElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(m.modelName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(
                                            GeoUtils.formatTemperature(m.temperatureC, prefs.temperatureUnit),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Text(
                                        "Feels ${GeoUtils.formatTemperature(m.feelsLikeC, prefs.temperatureUnit)} · Rain ${m.rainProbabilityPercent?.let { "$it%" } ?: "N/A"} (${m.precipitationMm?.let { "$it mm" } ?: "--"})",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        "Wind ${GeoUtils.formatWind(m.windSpeedKmh, prefs.windUnit)} · Pressure ${GeoUtils.formatPressure(m.pressureHpa, prefs.pressureUnit)} · Cloud ${m.cloudCoverPercent ?: "--"}% · RH ${m.humidityPercent ?: "--"}%",
                                        style = MaterialTheme.typography.labelMedium,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarineWeatherScreen(
    marineState: UiState<MarineWeatherData>,
    cityName: String,
    prefs: UserPreferences,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) {
        if (marineState is UiState.Idle) onRefresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$cityName · Marine & Tide") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh marine")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            when (marineState) {
                is UiState.Loading, UiState.Idle -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                is UiState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { Text(marineState.message) }
                is UiState.Success -> {
                    val m = marineState.data
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().widthIn(max = 720.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                tonalElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Ocean & Coastal State", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                        OriginBadge(DataOrigin.MODEL_ESTIMATE)
                                    }
                                    if (!m.isMarineArea) {
                                        Text(
                                            "Selected coordinates are inland — marine wave height & sea surface temperature are unavailable for inland points. Select a coastal city (e.g. Chattogram, Cox's Bazar, Tokyo) for ocean wave telemetry.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Text("Wave Height: ${m.waveHeightM?.let { "$it m" } ?: "N/A"} · Swell: ${m.swellHeightM?.let { "$it m" } ?: "N/A"}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text("Wave Period: ${m.wavePeriodSec?.let { "$it s" } ?: "N/A"} · Direction: ${GeoUtils.compassDirection(m.waveDirectionDeg)} (${m.waveDirectionDeg ?: "--"}°)", style = MaterialTheme.typography.bodyMedium)
                                        Text("Sea Surface Temp: ${GeoUtils.formatTemperature(m.seaSurfaceTempC, prefs.temperatureUnit)}", style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Text(
                                        "Wind: ${GeoUtils.formatWind(m.windSpeedKmh, prefs.windUnit)} · Gust: ${GeoUtils.formatWind(m.windGustKmh, prefs.windUnit)} · Pressure: ${GeoUtils.formatPressure(m.pressureHpa, prefs.pressureUnit)}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text("Source: ${m.source}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Tide Forecast Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(m.tideStatusNote, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        if (m.hourlyWaveHeights.isNotEmpty()) {
                            item {
                                Text("Upcoming Wave Heights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            items(m.hourlyWaveHeights) { (time, meters) ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(time, fontWeight = FontWeight.SemiBold)
                                        Text("${String.format(Locale.US, "%.2f", meters)} m", fontWeight = FontWeight.Bold, color = SkyCyan)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AviationWeatherScreen(
    aviationState: UiState<AviationMetarData>,
    service: ExtendedWeatherService,
    onSearchAirport: (String) -> Unit,
    onBack: () -> Unit
) {
    var airportQuery by remember { mutableStateOf("DAC") }
    LaunchedEffect(Unit) {
        if (aviationState is UiState.Idle) onSearchAirport("DAC")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aviation Weather (METAR / TAF)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = airportQuery,
                        onValueChange = { airportQuery = it },
                        label = { Text("Airport IATA / ICAO (e.g. DAC, VGHS, DEL, HND, JFK)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("aviation_search_input")
                    )
                    Button(onClick = { onSearchAirport(airportQuery) }) {
                        Text("Fetch")
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(service.commonAirports.take(10)) { preset ->
                        FilterChip(
                            selected = airportQuery.equals(preset.iata, true) || airportQuery.equals(preset.icao, true),
                            onClick = {
                                airportQuery = preset.iata
                                onSearchAirport(preset.icao)
                            },
                            label = { Text("${GeoUtils.countryCodeToFlagEmoji(preset.countryCode)} ${preset.iata}/${preset.icao}") }
                        )
                    }
                }
            }

            item {
                when (aviationState) {
                    is UiState.Loading, UiState.Idle -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    is UiState.Error -> Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(aviationState.message, modifier = Modifier.padding(14.dp))
                    }
                    is UiState.Success -> {
                        val m = aviationState.data
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            tonalElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("${m.icao}${m.iata?.let { " ($it)" } ?: ""}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                    m.flightCategory?.let { cat ->
                                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)) {
                                            Text(cat, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Text(m.airportName, style = MaterialTheme.typography.titleMedium)
                                Text("Observation: ${m.observationTimeIso} · Source: ${m.source}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                HorizontalDivider()
                                Text("Wind: ${m.windDirDeg ?: "--"}° @ ${m.windSpeedKt ?: "--"} kt", fontWeight = FontWeight.SemiBold)
                                Text("Visibility: ${m.visibilityStr} · Ceiling: ${m.ceilingStr}")
                                Text("Temp / Dew Point: ${m.tempC ?: "--"}°C / ${m.dewpointC ?: "--"}°C · QNH: ${m.altimeterHpa ?: "--"} hPa")
                                Text("Conditions: ${m.weatherString}")
                                HorizontalDivider()
                                Text("RAW METAR", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                Text(m.rawMetar, style = MaterialTheme.typography.bodyMedium)
                                m.rawTaf?.let { taf ->
                                    Text("RAW TAF", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                    Text(taf, style = MaterialTheme.typography.bodyMedium)
                                }
                                Text(
                                    "Informational weather reference only — never use for operational flight briefing or dispatch.",
                                    style = MaterialTheme.typography.labelSmall,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceWeatherScreen(
    spaceState: UiState<SpaceWeatherData>,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) {
        if (spaceState is UiState.Idle) onRefresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Space Weather (NOAA SWPC)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh space weather")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            when (spaceState) {
                is UiState.Loading, UiState.Idle -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                is UiState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { Text(spaceState.message) }
                is UiState.Success -> {
                    val s = spaceState.data
                    Column(
                        modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            tonalElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Geomagnetic Kp Index", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    OriginBadge(DataOrigin.GLOBAL_OFFICIAL)
                                }
                                Text(
                                    text = s.kpIndex?.let { String.format(Locale.US, "Kp %.1f", it) } ?: "N/A",
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if ((s.kpIndex ?: 0.0) >= 5.0) SeverityHigh else SeverityLow
                                )
                                Text(s.geomagneticStatus, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("Solar Wind Speed: ${s.solarWindSpeedKms?.let { "${it.toInt()} km/s" } ?: "N/A"}")
                                Text("Aurora Visibility: ${s.auroraPossibility}")
                                Text(
                                    "Distinct from tropospheric weather · Source: ${s.source} · Observed ${s.observationTimeIso}",
                                    style = MaterialTheme.typography.labelSmall,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyNumbersScreen(
    activeCountryCode: String?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val countries = remember { EmergencyNumbersDataset.countries() }
    var selectedCode by remember(activeCountryCode) {
        mutableStateOf(
            activeCountryCode?.uppercase()?.takeIf { c -> countries.any { it.first == c } } ?: "BD"
        )
    }
    val entries = remember(selectedCode) { EmergencyNumbersDataset.forCountry(selectedCode) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Emergency & Useful Numbers") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "Select Country (flag & regional directory)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(countries) { (code, name) ->
                        FilterChip(
                            selected = selectedCode == code,
                            onClick = { selectedCode = code },
                            label = { Text("${GeoUtils.countryCodeToFlagEmoji(code)} $name") },
                            modifier = Modifier.testTag("emergency_country_$code")
                        )
                    }
                }
            }

            items(entries) { e ->
                val flag = GeoUtils.countryCodeToFlagEmoji(e.countryCode)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("$flag ${e.service}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${e.organization} · ${e.category}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Source: ${e.source} · Verified ${e.verifiedDate}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.width(10.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${e.number}"))
                                runCatching { context.startActivity(intent) }
                            }
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Call ${e.number}", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(e.number, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
