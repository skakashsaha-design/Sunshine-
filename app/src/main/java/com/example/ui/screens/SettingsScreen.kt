package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.calc.RuntimePerformanceProfile
import com.example.domain.model.*
import com.example.ui.theme.ThemeAppearance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefs: UserPreferences,
    runtimePerformance: RuntimePerformanceProfile,
    onUpdateLanguage: (AppLanguage) -> Unit,
    onUpdateTheme: (ThemeAppearance) -> Unit,
    onUpdateFontStyle: (FontStylePreset) -> Unit,
    onUpdateBackgroundStyle: (BackgroundStyle) -> Unit,
    onUpdateAnimation: (AnimationLevel) -> Unit,
    onUpdatePerformanceMode: (PerformanceMode) -> Unit,
    onUpdateTempUnit: (TemperatureUnit) -> Unit,
    onUpdateWindUnit: (WindUnit) -> Unit,
    onUpdatePressureUnit: (PressureUnit) -> Unit,
    onUpdateProvider: (Boolean, ProviderId) -> Unit,
    onUpdateNotifications: (NotificationSettings) -> Unit,
    onUpdateAmbientVolume: (Float) -> Unit,
    onUpdateAmbientTimer: (Int) -> Unit,
    onUpdateWallpaperStatic: (Boolean) -> Unit,
    onUpdatePersonalKeys: (PersonalApiKeys) -> Unit,
    onSendTestNotification: () -> Unit,
    onSendMorningBriefing: () -> Unit,
    onBack: () -> Unit
) {
    var keysDraft by remember(prefs.personalKeys) { mutableStateOf(prefs.personalKeys) }
    var pendingPreviewAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) pendingPreviewAction?.invoke()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_settings)) },
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Language
                item {
                    SettingsSection("Language") {
                        FlowChipGroup(
                            items = AppLanguage.entries,
                            selected = prefs.language,
                            labelFor = { it.displayName },
                            onSelect = onUpdateLanguage
                        )
                    }
                }

                // Custom Fonts, Themes & Backgrounds
                item {
                    SettingsSection("Custom Typography, Theme & Background") {
                        Text("Font Style", style = MaterialTheme.typography.labelLarge)
                        FlowChipGroup(
                            items = FontStylePreset.entries,
                            selected = prefs.fontStyle,
                            labelFor = { it.label },
                            onSelect = onUpdateFontStyle
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("Theme Appearance", style = MaterialTheme.typography.labelLarge)
                        FlowChipGroup(
                            items = ThemeAppearance.entries,
                            selected = prefs.themeAppearance,
                            labelFor = { it.label },
                            onSelect = onUpdateTheme
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("Atmospheric Background", style = MaterialTheme.typography.labelLarge)
                        FlowChipGroup(
                            items = BackgroundStyle.entries,
                            selected = prefs.backgroundStyle,
                            labelFor = { it.label },
                            onSelect = onUpdateBackgroundStyle
                        )
                    }
                }

                // 1 GB RAM Mode & Animation Optimization
                item {
                    SettingsSection("Performance & 1 GB Low-RAM Optimization") {
                        Text(
                            "Detected Device RAM: ${runtimePerformance.totalRamMb} MB · Active Mode: ${runtimePerformance.effectiveMode.label}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FlowChipGroup(
                            items = PerformanceMode.entries,
                            selected = prefs.performanceMode,
                            labelFor = { it.label },
                            onSelect = onUpdatePerformanceMode
                        )
                        Text(
                            prefs.performanceMode.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("Weather Animation Level", style = MaterialTheme.typography.labelLarge)
                        FlowChipGroup(
                            items = AnimationLevel.entries,
                            selected = prefs.animationLevel,
                            labelFor = { it.label },
                            onSelect = onUpdateAnimation
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Static Live Wallpaper Fallback")
                            Switch(checked = prefs.liveWallpaperStaticFallback, onCheckedChange = onUpdateWallpaperStatic)
                        }
                    }
                }

                // Units
                item {
                    SettingsSection("Units") {
                        Text("Temperature", style = MaterialTheme.typography.labelLarge)
                        FlowChipGroup(TemperatureUnit.entries, prefs.temperatureUnit, { it.symbol }, onUpdateTempUnit)
                        Text("Wind Speed", style = MaterialTheme.typography.labelLarge)
                        FlowChipGroup(WindUnit.entries, prefs.windUnit, { it.symbol }, onUpdateWindUnit)
                        Text("Pressure", style = MaterialTheme.typography.labelLarge)
                        FlowChipGroup(PressureUnit.entries, prefs.pressureUnit, { it.symbol }, onUpdatePressureUnit)
                    }
                }

                // Provider Selection
                item {
                    SettingsSection("Weather Data Provider") {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Automatic Smart Fallback", fontWeight = FontWeight.SemiBold)
                                Text("Uses Open-Meteo → MET Norway → Configured keys", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = prefs.autoProvider,
                                onCheckedChange = { onUpdateProvider(it, prefs.selectedProvider) },
                                modifier = Modifier.testTag("settings_auto_provider")
                            )
                        }
                        if (!prefs.autoProvider) {
                            FlowChipGroup(
                                items = ProviderId.entries.filter { it != ProviderId.BMD },
                                selected = prefs.selectedProvider,
                                labelFor = { it.displayName },
                                onSelect = { onUpdateProvider(false, it) }
                            )
                        }
                    }
                }

                // Notifications, Morning Briefing, BWOT & Worldwide
                item {
                    val ns = prefs.notificationSettings
                    SettingsSection("Notifications, Morning Briefing & Warnings") {
                        ToggleRow("Enable All Notifications", ns.enabled) { onUpdateNotifications(ns.copy(enabled = it)) }
                        ToggleRow("Daily Morning Weather Briefing", ns.dailySummaryEnabled) { onUpdateNotifications(ns.copy(dailySummaryEnabled = it)) }
                        if (ns.enabled && ns.dailySummaryEnabled) {
                            val formattedTime = String.format(java.util.Locale.US, "%02d:%02d", ns.dailyHour, ns.dailyMinute)
                            Text(
                                text = "Morning Briefing Time: $formattedTime (uses live GPS location if permitted, else default city)",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            val presets = listOf(6 to 0, 6 to 30, 7 to 0, 7 to 30, 8 to 0, 9 to 0)
                            FlowChipGroup(
                                items = presets,
                                selected = ns.dailyHour to ns.dailyMinute,
                                labelFor = { (h, m) -> String.format(java.util.Locale.US, "%02d:%02d", h, m) },
                                onSelect = { (h, m) -> onUpdateNotifications(ns.copy(dailyHour = h, dailyMinute = m)) }
                            )
                        }
                        ToggleRow("Rain & Storm Approaching", ns.rainAlertsEnabled) { onUpdateNotifications(ns.copy(rainAlertsEnabled = it)) }
                        ToggleRow("Severe Weather & BMD Warnings", ns.severeWeatherEnabled) { onUpdateNotifications(ns.copy(severeWeatherEnabled = it)) }
                        ToggleRow("BWOT Kalboishakhi & Convective Alerts", ns.bwotAlertsEnabled) { onUpdateNotifications(ns.copy(bwotAlertsEnabled = it)) }
                        ToggleRow("Air Quality Advisories", ns.aqiAlertsEnabled) { onUpdateNotifications(ns.copy(aqiAlertsEnabled = it)) }
                        ToggleRow("Nearby Disasters (within radius)", ns.disasterAlertsEnabled) { onUpdateNotifications(ns.copy(disasterAlertsEnabled = it)) }
                        ToggleRow("Worldwide Critical Hazard Alerts", ns.worldwideAlertsEnabled) { onUpdateNotifications(ns.copy(worldwideAlertsEnabled = it)) }

                        Text("Nearby Disaster Radius: ${ns.disasterRadiusKm} km", style = MaterialTheme.typography.labelLarge)
                        Slider(
                            value = ns.disasterRadiusKm.toFloat(),
                            onValueChange = { onUpdateNotifications(ns.copy(disasterRadiusKm = it.toInt())) },
                            valueRange = 100f..2000f
                        )
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    pendingPreviewAction = onSendMorningBriefing
                                    notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onSendMorningBriefing()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_send_morning_briefing")
                        ) {
                            Text("Send Morning Briefing Now")
                        }
                        OutlinedButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    pendingPreviewAction = onSendTestNotification
                                    notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onSendTestNotification()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Send Quick-View Notification Now")
                        }
                    }
                }

                // Ambient Audio Settings
                item {
                    SettingsSection("Ambient Audio Controls") {
                        Text("Volume: ${(prefs.ambientVolume * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
                        Slider(
                            value = prefs.ambientVolume,
                            onValueChange = onUpdateAmbientVolume,
                            valueRange = 0f..1f
                        )
                        Text("Auto-Stop Timer", style = MaterialTheme.typography.labelLarge)
                        FlowChipGroup(
                            items = listOf(15, 30, 60, 0),
                            selected = prefs.ambientAutoStopMinutes,
                            labelFor = { if (it == 0) "Continuous" else "${it}m" },
                            onSelect = onUpdateAmbientTimer
                        )
                    }
                }

                // Optional Personal AI & Commercial Provider Keys
                item {
                    SettingsSection("Optional API Add-ons & Personal AI") {
                        Text(
                            "No API key is required for core weather, AQI, radar, astronomy, or built-in Today AI Analysis. Enter optional personal keys below only if desired.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        ToggleRow("Enable Optional Personal AI Brief", keysDraft.personalAiEnabled) {
                            keysDraft = keysDraft.copy(personalAiEnabled = it)
                        }
                        if (keysDraft.personalAiEnabled) {
                            OutlinedTextField(
                                value = keysDraft.personalAiEndpoint,
                                onValueChange = { keysDraft = keysDraft.copy(personalAiEndpoint = it) },
                                label = { Text("Personal AI Endpoint URL (optional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = keysDraft.personalAiKey,
                                onValueChange = { keysDraft = keysDraft.copy(personalAiKey = it) },
                                label = { Text("Personal AI API Key (optional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        OutlinedTextField(
                            value = keysDraft.weatherApiKey,
                            onValueChange = { keysDraft = keysDraft.copy(weatherApiKey = it) },
                            label = { Text("WeatherAPI Key (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = keysDraft.accuWeatherKey,
                            onValueChange = { keysDraft = keysDraft.copy(accuWeatherKey = it) },
                            label = { Text("AccuWeather Key (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = keysDraft.tomorrowIoKey,
                            onValueChange = { keysDraft = keysDraft.copy(tomorrowIoKey = it) },
                            label = { Text("Tomorrow.io Key (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = keysDraft.weatherbitKey,
                            onValueChange = { keysDraft = keysDraft.copy(weatherbitKey = it) },
                            label = { Text("Weatherbit Key (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = keysDraft.googleWeatherKey,
                            onValueChange = { keysDraft = keysDraft.copy(googleWeatherKey = it) },
                            label = { Text("Google Maps Weather API Key (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = keysDraft.bmdCustomFeedUrl,
                            onValueChange = { keysDraft = keysDraft.copy(bmdCustomFeedUrl = it) },
                            label = { Text("Authorized BMD Feed URL (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = { onUpdatePersonalKeys(keysDraft) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Optional Keys")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> FlowChipGroup(
    items: List<T>,
    selected: T,
    labelFor: (T) -> String,
    onSelect: (T) -> Unit
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            FilterChip(
                selected = item == selected,
                onClick = { onSelect(item) },
                label = { Text(labelFor(item)) }
            )
        }
    }
}
