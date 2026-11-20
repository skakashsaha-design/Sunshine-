package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.repository.AlertsAndDisastersBundle
import com.example.domain.model.*
import com.example.notification.alertCategoryVector
import com.example.ui.UiState
import com.example.ui.components.OriginBadge
import com.example.ui.components.SeverityChip
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun AlertsScreen(
    alertsState: UiState<AlertsAndDisastersBundle>,
    onRefresh: () -> Unit,
    onSelectAlert: (WeatherAlert) -> Unit,
    onOpenDisasters: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        if (alertsState is UiState.Idle) onRefresh()
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Alert & BMD Center",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                    }
                }
            }

            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) { Text("Active Alerts") }
                    SegmentedButton(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        modifier = Modifier.testTag("alerts_tab_bmd")
                    ) { Text("Bangladesh BMD") }
                }
            }

            when (alertsState) {
                is UiState.Loading, UiState.Idle -> {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        Text(alertsState.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is UiState.Success -> {
                    val bundle = alertsState.data
                    if (selectedTab == 0) {
                        if (bundle.alerts.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    tonalElevation = 2.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = SeverityLow, modifier = Modifier.size(40.dp))
                                        Text("No Active Severe Weather Alerts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text("Checked official NWS, Bangladesh BMD thresholds, and hydrological flood feeds.", style = MaterialTheme.typography.bodyMedium)
                                        OutlinedButton(onClick = onOpenDisasters) {
                                            Text("Open Global Disaster Center")
                                        }
                                    }
                                }
                            }
                        } else {
                            items(bundle.alerts) { alert ->
                                AlertCard(alert = alert, onClick = { onSelectAlert(alert) })
                            }
                        }
                    } else {
                        val bmd = bundle.bmdBulletin
                        item {
                            BmdHeroBanner(bmd = bmd)
                        }
                        item {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                tonalElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Maritime & Coastal Outlook", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        OriginBadge(bmd.bulletinOrigin)
                                    }
                                    Text(bmd.marineWarning, style = MaterialTheme.typography.bodyMedium)
                                    HorizontalDivider()
                                    Text("Inland River Ports & Squall Outlook", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(bmd.inlandWarning, style = MaterialTheme.typography.bodyMedium)
                                    if (bmd.bwotWatchSummary.isNotBlank()) {
                                        HorizontalDivider()
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("BWOT Convective & Kalboishakhi Watch", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            Surface(
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    "Risk: ${bmd.bwotSquallRisk}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                        Text(bmd.bwotWatchSummary, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    HorizontalDivider()
                                    Text(bmd.statusNote, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    OutlinedButton(onClick = onOpenDisasters, modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Open Worldwide Hazard Monitor")
                                    }
                                }
                            }
                        }
                        items(bmd.alerts) { alert ->
                            AlertCard(alert = alert, onClick = { onSelectAlert(alert) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BmdHeroBanner(bmd: BmdBulletin) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRect(Brush.linearGradient(listOf(Color(0xFF064E3B), Color(0xFF0F766E))))
            drawCircle(Color(0x33EF4444), radius = size.height * 0.45f, center = Offset(size.width * 0.85f, size.height * 0.5f))
        }
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🇧🇩", style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.title_bmd), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
            }
            OriginBadge(bmd.bulletinOrigin)
            Text("Region: ${bmd.stationOrRegion}", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFD1FAE5))
        }
    }
}

@Composable
private fun AlertCard(alert: WeatherAlert, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = alertCategoryVector(alert.category),
                        contentDescription = alert.category.label,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    SeverityChip(alert.severity)
                }
                OriginBadge(alert.origin)
            }
            Text(alert.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${alert.category.label} · ${alert.affectedArea}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(alert.description, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
            Text("Source: ${alert.sourceName} · Expires: ${alert.expiryTimeIso}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertDetailScreen(
    alert: WeatherAlert,
    onBack: () -> Unit,
    onShowOnMap: (Double, Double) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alert Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = alertCategoryVector(alert.category),
                        contentDescription = alert.category.label,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    SeverityChip(alert.severity)
                    OriginBadge(alert.origin)
                }
            }
            item {
                Text(alert.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
            item {
                Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Category: ${alert.category.label}")
                        Text("Urgency: ${alert.urgency} · Certainty: ${alert.certainty}")
                        Text("Source: ${alert.sourceName}")
                        Text("Issuing Authority: ${alert.issuingAuthority}")
                        Text("Issued: ${alert.issueTimeIso}")
                        Text("Effective: ${alert.startTimeIso} → ${alert.expiryTimeIso}")
                        Text("Affected Area: ${alert.affectedArea}")
                        alert.distanceKm?.let { Text("Distance: ${it.roundToInt()} km") }
                    }
                }
            }
            item {
                Text("Description", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(alert.description, style = MaterialTheme.typography.bodyLarge)
            }
            alert.instruction?.takeIf { it.isNotBlank() }?.let { inst ->
                item {
                    Text("Recommended Action", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(inst, style = MaterialTheme.typography.bodyLarge)
                }
            }
            if (alert.latitude != null && alert.longitude != null) {
                item {
                    Button(
                        onClick = { onShowOnMap(alert.latitude, alert.longitude) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Inspect on Map")
                    }
                }
            }
        }
    }
}
