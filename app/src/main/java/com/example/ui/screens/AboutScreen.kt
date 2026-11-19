package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_about)) },
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
                    Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("SkyPulse Meteorological Platform", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("Developer: ${stringResource(R.string.about_developer_name)} (Bangladesh)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.about_developer_role), style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(4.dp))
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:skakashsaha@gmail.com"))
                                    runCatching { context.startActivity(intent) }
                                }
                            ) {
                                Icon(Icons.Default.Email, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.about_developer_email))
                            }
                        }
                    }
                }

                item {
                    Surface(shape = RoundedCornerShape(18.dp), tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("Authoritative Data Sources", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Text("• Weather & Hydrological Floods: Open-Meteo, MET Norway Locationforecast 2.0, WeatherAPI, AccuWeather, Tomorrow.io, Weatherbit, Google Maps Platform Weather API")
                            Text("• Air Quality: Open-Meteo CAMS Global Atmospheric Model")
                            Text("• Precipitation Radar: RainViewer Weather Maps")
                            Text("• Seismic & Global Disasters: USGS Earthquake Hazards, NASA EONET v3, UN/EC GDACS, NOAA/NWS Alerts")
                            Text("• Bangladesh Companion: BMD Threshold Engine & Authorized Feed Adapter (bmd.gov.bd)")
                            Text("• Map Base Tiles: OpenStreetMap, CartoDB, Esri World Imagery, OpenTopoMap")
                        }
                    }
                }
            }
        }
    }
}
