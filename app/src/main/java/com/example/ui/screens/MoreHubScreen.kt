package com.example.ui.screens

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
    onOpenAqi: () -> Unit,
    onOpenAstronomy: () -> Unit,
    onOpenDisasters: () -> Unit,
    onOpenLocations: () -> Unit,
    onOpenProviders: () -> Unit,
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
                Text(
                    text = "Intelligence & Tools",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            item { MoreHubRow(stringResource(R.string.title_aqi), "US/EU AQI, pollutants & best outdoor hours", Icons.Default.Air, onOpenAqi, "more_aqi") }
            item { MoreHubRow(stringResource(R.string.title_astronomy), "Sun path, golden/blue hour, lunar phase & ephemeris", Icons.Default.NightsStay, onOpenAstronomy, "more_astronomy") }
            item { MoreHubRow(stringResource(R.string.title_disasters), "Earthquakes, tsunamis, floods, wildfires & volcanoes", Icons.Default.Public, onOpenDisasters, "more_disasters") }
            item { MoreHubRow(stringResource(R.string.title_locations), "Global city search, GPS & saved places", Icons.Default.EditLocationAlt, onOpenLocations, "more_locations") }
            item { MoreHubRow(stringResource(R.string.title_providers), "Compare 6+1 providers, health & latency", Icons.AutoMirrored.Filled.CompareArrows, onOpenProviders, "more_providers") }

            // Quick Ambient Sound Studio Card
            item {
                Surface(shape = RoundedCornerShape(18.dp), tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.title_ambient), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(if (isSoundPlaying) "Playing: ${activeSound.label}" else "Off", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                AmbientSoundType.RAIN,
                                AmbientSoundType.THUNDER,
                                AmbientSoundType.WIND,
                                AmbientSoundType.WINTER,
                                AmbientSoundType.CALM
                            ).forEach { sound ->
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

            item { MoreHubRow(stringResource(R.string.title_settings), "Language, themes, units, alerts & optional API keys", Icons.Default.Settings, onOpenSettings, "more_settings") }
            item { MoreHubRow(stringResource(R.string.title_about), "Developer Arko Saha, sources & attributions", Icons.Default.Info, onOpenAbout, "more_about") }
        }
    }
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
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}
