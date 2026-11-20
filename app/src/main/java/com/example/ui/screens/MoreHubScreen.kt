package com.example.ui.screens

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.AmbientSoundType
import com.example.domain.model.UserPreferences
import com.example.wallpaper.SkyPulseWallpaperService

@Composable
fun MoreHubScreen(
    prefs: UserPreferences,
    isSoundPlaying: Boolean,
    activeSound: AmbientSoundType,
    onOpenTripPlanner: () -> Unit,
    onOpenModels: () -> Unit,
    onOpenAqi: () -> Unit,
    onOpenAstronomy: () -> Unit,
    onOpenMarine: () -> Unit,
    onOpenAviation: () -> Unit,
    onOpenSpaceWeather: () -> Unit,
    onOpenDisasters: () -> Unit,
    onOpenEmergencyNumbers: () -> Unit,
    onOpenLocations: () -> Unit,
    onOpenProviders: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onToggleSound: (AmbientSoundType) -> Unit,
    onSendTestNotification: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Weather Intelligence",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Meteorology, Planners, Models, Aviation, Marine & Emergency Center",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item { HubCategoryLabel("Planning & Forecasting") }
            item { MoreHubRow("Trip & Event Host Planner", "Evaluate specific days to host events or travel safely", Icons.Default.EventAvailable, onOpenTripPlanner, "more_trip_planner") }
            item { MoreHubRow("Model Comparison & Confidence", "Compare ECMWF, GFS, ICON, JMA, GEM & ensemble spread", Icons.Default.Insights, onOpenModels, "more_models") }
            item { MoreHubRow(stringResource(R.string.title_aqi), "US/EU AQI, pollutants & best outdoor hours", Icons.Default.Air, onOpenAqi, "more_aqi") }
            item { MoreHubRow(stringResource(R.string.title_astronomy), "Sun path, golden/blue hour, lunar phase & twilight", Icons.Default.NightsStay, onOpenAstronomy, "more_astronomy") }

            item { HubCategoryLabel("Specialized & Official Systems") }
            item { MoreHubRow("Marine & Tide Status", "Waves, swell height/period, SST & tide station status", Icons.Default.Waves, onOpenMarine, "more_marine") }
            item { MoreHubRow("Aviation Weather (METAR/TAF)", "Search global airports by IATA/ICAO (DAC, VGHS, DEL, HND)", Icons.Default.FlightTakeoff, onOpenAviation, "more_aviation") }
            item { MoreHubRow("Space Weather (NOAA SWPC)", "Planetary Kp index, geomagnetic storm & aurora status", Icons.Default.Flare, onOpenSpaceWeather, "more_space_weather") }
            item { MoreHubRow(stringResource(R.string.title_disasters), "Earthquakes, tsunamis, GloFAS floods, wildfires & volcanoes", Icons.Default.Public, onOpenDisasters, "more_disasters") }
            item { MoreHubRow("Emergency & Useful Numbers", "Verified national emergency lines worldwide with country flags", Icons.Default.LocalHospital, onOpenEmergencyNumbers, "more_emergency") }

            item { HubCategoryLabel("Places, Data & App Controls") }
            item { MoreHubRow(stringResource(R.string.title_locations), "Global city search, GPS & saved places", Icons.Default.EditLocationAlt, onOpenLocations, "more_locations") }
            item { MoreHubRow("Data Sources & Provider Health", "Live 7-provider comparison + 22-source registry status", Icons.AutoMirrored.Filled.CompareArrows, onOpenProviders, "more_providers") }
            item { MoreHubRow("Permission Manager", "Location, notification & network permissions control", Icons.Default.Security, onOpenPermissions, "more_permissions") }

            // Quick Ambient Sound Studio Card
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.title_ambient), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(if (isSoundPlaying) "Playing: ${activeSound.label}" else "Off", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(
                                listOf(
                                    AmbientSoundType.RAIN,
                                    AmbientSoundType.THUNDER,
                                    AmbientSoundType.WIND,
                                    AmbientSoundType.WINTER,
                                    AmbientSoundType.CALM
                                )
                            ) { sound ->
                                FilterChip(
                                    selected = isSoundPlaying && activeSound == sound,
                                    onClick = { onToggleSound(sound) },
                                    label = { Text(sound.label, maxLines = 1) }
                                )
                            }
                        }
                    }
                }
            }

            // Live Weather Wallpaper & Quick-View Notification Actions
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                                putExtra(
                                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                                    ComponentName(context, SkyPulseWallpaperService::class.java)
                                )
                            }
                            runCatching { context.startActivity(intent) }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Wallpaper, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Live Wallpaper")
                    }
                    OutlinedButton(
                        onClick = onSendTestNotification,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Quick View")
                    }
                }
            }

            item { MoreHubRow(stringResource(R.string.title_settings), "Fonts, themes, backgrounds, 1 GB RAM mode, units & alerts", Icons.Default.Settings, onOpenSettings, "more_settings") }
            item { MoreHubRow(stringResource(R.string.title_about), "Developer Arko Saha, sources & attributions", Icons.Default.Info, onOpenAbout, "more_about") }
        }
    }
}

@Composable
private fun HubCategoryLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 6.dp, start = 4.dp)
    )
}

@Composable
private fun MoreHubRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag(tag),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
