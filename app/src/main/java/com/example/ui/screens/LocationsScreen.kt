package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.calc.GeoUtils
import com.example.domain.model.SavedLocation
import com.example.domain.model.SearchLocationResult
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationsScreen(
    savedLocations: List<SavedLocation>,
    searchResults: List<SearchLocationResult>,
    searchHistory: List<SearchLocationResult>,
    isSearching: Boolean,
    onSearchQuery: (String) -> Unit,
    onAddCity: (SearchLocationResult) -> Unit,
    onSelectLocation: (SavedLocation) -> Unit,
    onRequestGps: () -> Unit,
    onRenameLocation: (String, String) -> Unit,
    onMoveLocation: (String, Int) -> Unit,
    onDeleteLocation: (String) -> Unit,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<SavedLocation?>(null) }
    var renameText by remember { mutableStateOf("") }

    val gpsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) {
            onRequestGps()
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_locations)) },
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            onSearchQuery(it)
                        },
                        label = { Text("Search city, country, or lat,lon (e.g. 23.81, 90.41)") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = ""; onSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("city_search_input")
                    )
                }

                item {
                    OutlinedButton(
                        onClick = {
                            gpsLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Detect Current GPS Location")
                    }
                }

                if (isSearching) {
                    item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
                }

                if (searchResults.isNotEmpty()) {
                    item { Text("Search Results", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                    items(searchResults) { res ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    onAddCity(res)
                                    query = ""
                                    onBack()
                                },
                            tonalElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${GeoUtils.countryCodeToFlagEmoji(res.countryCode)} ${res.name}", fontWeight = FontWeight.Bold)
                                    Text(
                                        listOfNotNull(res.region.takeIf { it.isNotBlank() }, res.country.takeIf { it.isNotBlank() }).joinToString(", "),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(Icons.Default.AddCircleOutline, contentDescription = "Add city")
                            }
                        }
                    }
                } else if (query.isBlank() && searchHistory.isNotEmpty()) {
                    item { Text("Recent Searches", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                    items(searchHistory.take(4)) { hist ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onAddCity(hist); onBack() },
                            tonalElevation = 1.dp
                        ) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.History, contentDescription = null)
                                Text("${GeoUtils.countryCodeToFlagEmoji(hist.countryCode)} ${hist.name}, ${hist.country}")
                            }
                        }
                    }
                }

                item {
                    Text("Saved Locations (${savedLocations.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }

                items(savedLocations) { loc ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectLocation(loc); onBack() },
                        color = if (loc.isDefault) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        contentColor = if (loc.isDefault) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${GeoUtils.countryCodeToFlagEmoji(loc.countryCode)} ${loc.name}" + if (loc.isDefault) " (Default)" else "",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${String.format(Locale.US, "%.2f, %.2f", loc.latitude, loc.longitude)} · ${loc.country}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row {
                                IconButton(onClick = { onMoveLocation(loc.id, -1) }) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move up")
                                }
                                IconButton(onClick = {
                                    renameTarget = loc
                                    renameText = loc.name
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Rename")
                                }
                                if (savedLocations.size > 1) {
                                    IconButton(onClick = { onDeleteLocation(loc.id) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename Location") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    label = { Text("Display Name") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    onRenameLocation(target.id, renameText)
                    renameTarget = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
            }
        )
    }
}
