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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.calc.GeoUtils
import com.example.domain.model.*
import com.example.ui.UiState
import com.example.ui.theme.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TripPlannerScreen(
    weatherState: UiState<WeatherBundle>,
    savedPlans: List<SavedTripPlan>,
    prefs: UserPreferences,
    evaluateDay: (DailyForecast, TripActivityType, EventTimeWindow) -> TripEvaluation?,
    onSavePlan: (String, TripEvaluation) -> Unit,
    onDeletePlan: (String) -> Unit,
    onBack: () -> Unit
) {
    val bundle = (weatherState as? UiState.Success)?.data
    val days = bundle?.daily.orEmpty()

    var selectedDayIndex by remember(days) { mutableIntStateOf(0) }
    var selectedActivity by remember { mutableStateOf(TripActivityType.OUTDOOR_EVENT_HOSTING) }
    var selectedWindow by remember { mutableStateOf(EventTimeWindow.FULL_DAY) }
    var customTitle by remember { mutableStateOf("") }
    var savedToast by remember { mutableStateOf(false) }

    val selectedDay = days.getOrNull(selectedDayIndex)
    val evaluation = remember(selectedDay, selectedActivity, selectedWindow, bundle) {
        if (selectedDay != null) evaluateDay(selectedDay, selectedActivity, selectedWindow) else null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trip & Event Host Planner") },
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
            if (bundle == null || days.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Load weather forecast first to evaluate dates.", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 720.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            tonalElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("1. Pick Date in ${bundle.location.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(days.indices.toList()) { idx ->
                                        val d = days[idx]
                                        val isSel = idx == selectedDayIndex
                                        FilterChip(
                                            selected = isSel,
                                            onClick = { selectedDayIndex = idx; savedToast = false },
                                            label = {
                                                Text(
                                                    "${d.isoDate.takeLast(5)} (${GeoUtils.formatTemperature(d.tempMaxC, prefs.temperatureUnit, false)})"
                                                )
                                            },
                                            modifier = Modifier.testTag("trip_day_chip_$idx")
                                        )
                                    }
                                }

                                Text("2. Activity / Hosting Type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TripActivityType.entries.forEach { act ->
                                        FilterChip(
                                            selected = selectedActivity == act,
                                            onClick = { selectedActivity = act; savedToast = false },
                                            label = { Text(act.label) }
                                        )
                                    }
                                }

                                Text("3. Event / Travel Window", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    EventTimeWindow.entries.forEach { w ->
                                        FilterChip(
                                            selected = selectedWindow == w,
                                            onClick = { selectedWindow = w; savedToast = false },
                                            label = { Text(w.label) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (evaluation != null) {
                        val verdictColor = when (evaluation.verdict) {
                            HostVerdict.IDEAL_GO -> SeverityLow
                            HostVerdict.FAIR_WITH_BACKUP -> SkyCyan
                            HostVerdict.CAUTION_PREPARE -> SeverityModerate
                            HostVerdict.HIGH_RISK_RESCHEDULE -> SeverityExtreme
                        }

                        item {
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = verdictColor.copy(alpha = 0.16f),
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("trip_verdict_card")
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = evaluation.verdict.badge,
                                                style = MaterialTheme.typography.headlineMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = verdictColor
                                            )
                                            Text(
                                                text = "${evaluation.isoDate} · ${evaluation.timeWindow.label}",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = verdictColor,
                                            contentColor = Color.White
                                        ) {
                                            Text(
                                                text = "${evaluation.score}/100",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                            )
                                        }
                                    }

                                    Text(evaluation.verdictDetail, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)

                                    LinearProgressIndicator(
                                        progress = { evaluation.score / 100f },
                                        color = verdictColor,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )

                                    Text(
                                        text = "Best Safe Window: ${evaluation.bestWindowLabel}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Temp ${evaluation.tempSummary} · Peak Rain ${evaluation.peakRainProbPercent}% (${String.format(java.util.Locale.US, "%.1f", evaluation.totalRainMm)} mm) · Gusts ${evaluation.peakWindGustKmh.roundToInt()} km/h",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    HorizontalDivider()
                                    Text("Risk Factors", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    evaluation.riskFactors.forEach { risk ->
                                        Text("• $risk", style = MaterialTheme.typography.bodyMedium)
                                    }

                                    HorizontalDivider()
                                    Text("Host & Organizer Checklist", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    evaluation.hostChecklist.forEach { tip ->
                                        Text("✓ $tip", style = MaterialTheme.typography.bodyMedium)
                                    }

                                    OutlinedTextField(
                                        value = customTitle,
                                        onValueChange = { customTitle = it },
                                        label = { Text("Event / Trip Title (optional)") },
                                        placeholder = { Text("e.g. Garden Wedding, Field Trip") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Button(
                                        onClick = {
                                            onSavePlan(customTitle, evaluation)
                                            customTitle = ""
                                            savedToast = true
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("trip_save_button")
                                    ) {
                                        Icon(Icons.Default.BookmarkAdd, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(if (savedToast) "Saved to My Plans" else "Save Day Plan")
                                    }
                                }
                            }
                        }
                    }

                    if (savedPlans.isNotEmpty()) {
                        item {
                            Text(
                                text = "Saved Trip & Event Plans (${savedPlans.size})",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(savedPlans, key = { it.id }) { plan ->
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
                                        Text(plan.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "${plan.locationName} · ${plan.isoDate} (${plan.timeWindow.label})",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${plan.lastVerdict.badge} · Score ${plan.lastScore}/100",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = { onDeletePlan(plan.id) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete plan")
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
