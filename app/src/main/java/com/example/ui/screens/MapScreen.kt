package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.data.remote.NetworkClient
import com.example.data.repository.AlertsAndDisastersBundle
import com.example.domain.calc.GeoUtils
import com.example.domain.model.*
import com.example.ui.UiState
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

private object TileBitmapCache {
    val cache = LruCache<String, Bitmap>(120)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    activeLocation: SavedLocation?,
    focusCoords: Pair<Double, Double>?,
    weatherState: UiState<WeatherBundle>,
    alertsState: UiState<AlertsAndDisastersBundle>,
    radarTimeline: RadarTimeline?,
    pointWeatherPreview: String?,
    networkClient: NetworkClient,
    onLoadRadar: () -> Unit,
    onLoadDisasters: () -> Unit,
    onInspectPoint: (Double, Double) -> Unit,
    onSavePoint: (Double, Double) -> Unit,
    onOpenLocations: () -> Unit,
    lowRamMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        onLoadRadar()
        onLoadDisasters()
    }

    var centerLat by remember { mutableDoubleStateOf(focusCoords?.first ?: activeLocation?.latitude ?: 23.8103) }
    var centerLon by remember { mutableDoubleStateOf(focusCoords?.second ?: activeLocation?.longitude ?: 90.4125) }
    var zoom by remember { mutableIntStateOf(6) }

    LaunchedEffect(focusCoords) {
        focusCoords?.let {
            centerLat = it.first
            centerLon = it.second
        }
    }

    var baseLayer by remember { mutableStateOf(MapBaseLayer.DARK) }
    var weatherLayer by remember { mutableStateOf(MapWeatherLayer.RADAR_RAIN) }
    var showDisasters by remember { mutableStateOf(true) }
    var showLayerSheet by remember { mutableStateOf(false) }

    var radarFrameIndex by remember { mutableIntStateOf(0) }
    var isRadarPlaying by remember { mutableStateOf(false) }
    var tappedMarker by remember { mutableStateOf<DisasterEvent?>(null) }
    var tappedPoint by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    val frames = radarTimeline?.frames.orEmpty()
    LaunchedEffect(frames.size) {
        if (frames.isNotEmpty() && radarFrameIndex >= frames.size) {
            radarFrameIndex = (frames.size - 3).coerceAtLeast(0)
        }
    }

    LaunchedEffect(isRadarPlaying, frames.size) {
        while (isRadarPlaying && frames.isNotEmpty()) {
            delay(750L)
            radarFrameIndex = (radarFrameIndex + 1) % frames.size
        }
    }

    // Tile bitmap state trigger
    var tileRevision by remember { mutableIntStateOf(0) }
    val pendingLoads = remember { mutableSetOf<String>() }

    val disasters = (alertsState as? UiState.Success)?.data?.disasters.orEmpty()
    val currentWeather = (weatherState as? UiState.Success)?.data?.current

    Box(modifier = modifier.fillMaxSize()) {
        // Full-Screen Interactive Tile Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("map_canvas")
                .pointerInput(zoom) {
                    detectTransformGestures { _, pan, zoomChange, _ ->
                        val scaleFactor = 256.0 * 2.0.pow(zoom)
                        val lonDelta = -pan.x / scaleFactor * 360.0
                        val latRad = Math.toRadians(centerLat)
                        val latDelta = pan.y / scaleFactor * 360.0 * cos(latRad)
                        centerLat = (centerLat + latDelta).coerceIn(-82.0, 82.0)
                        centerLon = ((centerLon + lonDelta + 540.0) % 360.0) - 180.0
                        if (zoomChange > 1.25f && zoom < 14) zoom++
                        else if (zoomChange < 0.8f && zoom > 2) zoom--
                    }
                }
                .pointerInput(centerLat, centerLon, zoom, disasters, showDisasters) {
                    detectTapGestures { offset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val (cxTile, cyTile) = GeoUtils.latLonToTileDouble(centerLat, centerLon, zoom)
                        val tapTileX = cxTile + (offset.x - w / 2f) / 256.0
                        val tapTileY = cyTile + (offset.y - h / 2f) / 256.0
                        val (tapLat, tapLon) = GeoUtils.tileDoubleToLatLon(tapTileX, tapTileY, zoom)

                        // Check if user tapped a disaster marker within 28px
                        val hit = if (showDisasters) {
                            disasters.firstOrNull { ev ->
                                val (ex, ey) = GeoUtils.latLonToTileDouble(ev.latitude, ev.longitude, zoom)
                                val sx = w / 2f + ((ex - cxTile) * 256.0).toFloat()
                                val sy = h / 2f + ((ey - cyTile) * 256.0).toFloat()
                                hypot(offset.x - sx, offset.y - sy) <= 34f
                            }
                        } else null

                        if (hit != null) {
                            tappedMarker = hit
                            tappedPoint = null
                        } else {
                            tappedMarker = null
                            tappedPoint = tapLat to tapLon
                            onInspectPoint(tapLat, tapLon)
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val tileCount = 1 shl zoom
            val (cxTile, cyTile) = GeoUtils.latLonToTileDouble(centerLat, centerLon, zoom)
            val minTx = floor(cxTile - w / 512.0).toInt()
            val maxTx = floor(cxTile + w / 512.0).toInt()
            val minTy = floor(cyTile - h / 512.0).toInt().coerceAtLeast(0)
            val maxTy = floor(cyTile + h / 512.0).toInt().coerceAtMost(tileCount - 1)

            // Reference tileRevision so Compose redraws as tiles arrive
            val rev = tileRevision

            drawRect(color = Color(0xFF0A1628))

            for (txRaw in minTx..maxTx) {
                val tx = ((txRaw % tileCount) + tileCount) % tileCount
                for (ty in minTy..maxTy) {
                    val left = (w / 2f + (txRaw - cxTile) * 256.0).roundToInt()
                    val top = (h / 2f + (ty - cyTile) * 256.0).roundToInt()

                    val baseUrl = baseTileUrl(baseLayer, zoom, tx, ty)
                    val bmp = TileBitmapCache.cache.get(baseUrl)
                    if (bmp != null) {
                        drawImage(
                            image = bmp.asImageBitmap(),
                            dstOffset = IntOffset(left, top),
                            dstSize = IntSize(257, 257)
                        )
                    } else if (pendingLoads.add(baseUrl)) {
                        scope.launch {
                            val bytes = networkClient.getBytes(baseUrl)
                            val decoded = bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                            if (decoded != null) {
                                TileBitmapCache.cache.put(baseUrl, decoded)
                                tileRevision++
                            }
                            pendingLoads.remove(baseUrl)
                        }
                    }

                    // Hybrid labels overlay
                    if (baseLayer == MapBaseLayer.HYBRID) {
                        val labelUrl = "https://basemaps.cartocdn.com/light_only_labels/$zoom/$tx/$ty.png"
                        val lBmp = TileBitmapCache.cache.get(labelUrl)
                        if (lBmp != null) {
                            drawImage(
                                image = lBmp.asImageBitmap(),
                                dstOffset = IntOffset(left, top),
                                dstSize = IntSize(257, 257)
                            )
                        } else if (pendingLoads.add(labelUrl)) {
                            scope.launch {
                                val bytes = networkClient.getBytes(labelUrl)
                                val decoded = bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                                if (decoded != null) {
                                    TileBitmapCache.cache.put(labelUrl, decoded)
                                    tileRevision++
                                }
                                pendingLoads.remove(labelUrl)
                            }
                        }
                    }

                    // RainViewer Radar Tile Overlay
                    if ((weatherLayer == MapWeatherLayer.RADAR_RAIN || weatherLayer == MapWeatherLayer.RADAR_SNOW) && frames.isNotEmpty()) {
                        val f = frames[radarFrameIndex.coerceIn(0, frames.lastIndex)]
                        val radarUrl = f.tileUrlTemplate
                            .replace("{z}", zoom.toString())
                            .replace("{x}", tx.toString())
                            .replace("{y}", ty.toString())
                        val rBmp = TileBitmapCache.cache.get(radarUrl)
                        if (rBmp != null) {
                            drawImage(
                                image = rBmp.asImageBitmap(),
                                dstOffset = IntOffset(left, top),
                                dstSize = IntSize(257, 257),
                                alpha = 0.72f
                            )
                        } else if (pendingLoads.add(radarUrl)) {
                            scope.launch {
                                val bytes = networkClient.getBytes(radarUrl)
                                val decoded = bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                                if (decoded != null) {
                                    TileBitmapCache.cache.put(radarUrl, decoded)
                                    tileRevision++
                                }
                                pendingLoads.remove(radarUrl)
                            }
                        }
                    }
                }
            }

            // Atmospheric Telemetry Overlay Field for selected weather layer (non-radar layers)
            if (weatherLayer != MapWeatherLayer.NONE &&
                weatherLayer != MapWeatherLayer.RADAR_RAIN &&
                weatherLayer != MapWeatherLayer.RADAR_SNOW &&
                activeLocation != null &&
                currentWeather != null
            ) {
                val (ax, ay) = GeoUtils.latLonToTileDouble(activeLocation.latitude, activeLocation.longitude, zoom)
                val sx = w / 2f + ((ax - cxTile) * 256.0).toFloat()
                val sy = h / 2f + ((ay - cyTile) * 256.0).toFloat()
                val layerColor = weatherLayerColor(weatherLayer, currentWeather)
                drawCircle(color = layerColor.copy(alpha = 0.24f), radius = 150f, center = Offset(sx, sy))
                drawCircle(color = layerColor.copy(alpha = 0.40f), radius = 85f, center = Offset(sx, sy))
                if (weatherLayer == MapWeatherLayer.WIND || weatherLayer == MapWeatherLayer.WIND_DIRECTION || weatherLayer == MapWeatherLayer.WIND_GUST) {
                    val deg = (currentWeather.windDirectionDeg ?: 0).toDouble()
                    val rad = Math.toRadians(deg)
                    for (gx in -1..1) {
                        for (gy in -1..1) {
                            val px = sx + gx * 90f
                            val py = sy + gy * 90f
                            val ex = px + (sin(rad) * 34f).toFloat()
                            val ey = py - (cos(rad) * 34f).toFloat()
                            drawLine(Color(0xFF38BDF8), Offset(px, py), Offset(ex, ey), strokeWidth = 4f)
                        }
                    }
                }
            }

            // Active location pin
            activeLocation?.let { loc ->
                val (lx, ly) = GeoUtils.latLonToTileDouble(loc.latitude, loc.longitude, zoom)
                val sx = w / 2f + ((lx - cxTile) * 256.0).toFloat()
                val sy = h / 2f + ((ly - cyTile) * 256.0).toFloat()
                drawCircle(Color.White, radius = 11f, center = Offset(sx, sy))
                drawCircle(SkyCobalt, radius = 7f, center = Offset(sx, sy))
            }

            // Tapped custom point pin
            tappedPoint?.let { (tLat, tLon) ->
                val (lx, ly) = GeoUtils.latLonToTileDouble(tLat, tLon, zoom)
                val sx = w / 2f + ((lx - cxTile) * 256.0).toFloat()
                val sy = h / 2f + ((ly - cyTile) * 256.0).toFloat()
                drawCircle(SkyAmber, radius = 12f, center = Offset(sx, sy), style = Stroke(width = 4f))
                drawCircle(Color.White, radius = 5f, center = Offset(sx, sy))
            }

            // Disaster Event Markers
            if (showDisasters) {
                disasters.take(if (lowRamMode) 20 else 60).forEach { ev ->
                    val (ex, ey) = GeoUtils.latLonToTileDouble(ev.latitude, ev.longitude, zoom)
                    val sx = w / 2f + ((ex - cxTile) * 256.0).toFloat()
                    val sy = h / 2f + ((ey - cyTile) * 256.0).toFloat()
                    if (sx in -30f..(w + 30f) && sy in -30f..(h + 30f)) {
                        val mColor = when (ev.severity) {
                            AlertSeverity.EXTREME -> SeverityExtreme
                            AlertSeverity.SEVERE -> SeverityHigh
                            AlertSeverity.MODERATE -> SeverityModerate
                            else -> SeverityAdvisory
                        }
                        drawCircle(mColor.copy(alpha = 0.28f), radius = 18f, center = Offset(sx, sy))
                        drawCircle(mColor, radius = 9f, center = Offset(sx, sy))
                        drawCircle(Color.White, radius = 3.5f, center = Offset(sx, sy))
                    }
                }
            }
        }

        // TOP SEARCH / LOCATION BAR (non-overlapping)
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .widthIn(max = 620.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenLocations)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text(
                            text = activeLocation?.name ?: "Tap to search city",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${baseLayer.label} · ${weatherLayer.label} · Zoom $zoom",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                FilledTonalIconButton(
                    onClick = { showLayerSheet = true },
                    modifier = Modifier.testTag("map_layers_button")
                ) {
                    Icon(Icons.Default.Layers, contentDescription = "Layers")
                }
            }
        }

        // COMPACT SIDE ZOOM & CENTER CONTROLS
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SmallFloatingActionButton(
                onClick = { if (zoom < 14) zoom++ },
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom in")
            }
            SmallFloatingActionButton(
                onClick = { if (zoom > 2) zoom-- },
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom out")
            }
            SmallFloatingActionButton(
                onClick = {
                    activeLocation?.let {
                        centerLat = it.latitude
                        centerLon = it.longitude
                        zoom = 7
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Center map")
            }
        }

        // BOTTOM STACK: Radar Timeline OR Tapped Point / Disaster Card (strictly 1 panel at bottom so zero overlap!)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp)
                .widthIn(max = 640.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (tappedMarker != null) {
                val ev = tappedMarker!!
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 5.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(ev.category.label, style = MaterialTheme.typography.labelLarge, color = SeverityExtreme, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { tappedMarker = null }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss")
                            }
                        }
                        Text(ev.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${ev.source} · ${ev.eventTimeIso} · ${ev.distanceKm?.roundToInt() ?: "--"} km away", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else if (tappedPoint != null) {
                val (pLat, pLon) = tappedPoint!!
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 5.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Point Weather", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            IconButton(onClick = { tappedPoint = null }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss")
                            }
                        }
                        Text(pointWeatherPreview ?: "${String.format(Locale.US, "%.3f, %.3f", pLat, pLon)}", style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onSavePoint(pLat, pLon); tappedPoint = null }) {
                                Text("Save & Use")
                            }
                            OutlinedButton(onClick = {
                                val uri = Uri.parse("https://www.google.com/maps/@?api=1&map_action=pano&viewpoint=$pLat,$pLon")
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                            }) {
                                Icon(Icons.Default.Streetview, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Street View")
                            }
                        }
                    }
                }
            }

            if ((weatherLayer == MapWeatherLayer.RADAR_RAIN || weatherLayer == MapWeatherLayer.RADAR_SNOW) && frames.isNotEmpty()) {
                val curFrame = frames[radarFrameIndex.coerceIn(0, frames.lastIndex)]
                val tsFmt = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(curFrame.timestampEpochSec * 1000L))
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = { isRadarPlaying = !isRadarPlaying },
                                    modifier = Modifier.testTag("radar_play_pause")
                                ) {
                                    Icon(
                                        if (isRadarPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play or pause radar"
                                    )
                                }
                                IconButton(onClick = {
                                    isRadarPlaying = false
                                    radarFrameIndex = (radarFrameIndex - 1 + frames.size) % frames.size
                                }) {
                                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous radar frame")
                                }
                                IconButton(onClick = {
                                    isRadarPlaying = false
                                    radarFrameIndex = (radarFrameIndex + 1) % frames.size
                                }) {
                                    Icon(Icons.Default.SkipNext, contentDescription = "Next radar frame")
                                }
                            }
                            Text(
                                text = "${if (curFrame.isNowcast) "Nowcast" else "Observed"} · $tsFmt",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = radarFrameIndex.toFloat(),
                            onValueChange = {
                                isRadarPlaying = false
                                radarFrameIndex = it.roundToInt().coerceIn(0, frames.lastIndex)
                            },
                            valueRange = 0f..(frames.lastIndex.coerceAtLeast(1)).toFloat()
                        )
                    }
                }
            }
        }
    }

    // Layer Manager Bottom Sheet
    if (showLayerSheet) {
        ModalBottomSheet(onDismissRequest = { showLayerSheet = false }) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text("Base Map Layer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MapBaseLayer.entries.take(3).forEach { layer ->
                            FilterChip(
                                selected = baseLayer == layer,
                                onClick = { baseLayer = layer },
                                label = { Text(layer.label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MapBaseLayer.entries.drop(3).forEach { layer ->
                            FilterChip(
                                selected = baseLayer == layer,
                                onClick = { baseLayer = layer },
                                label = { Text(layer.label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Disaster & Seismic Markers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Switch(checked = showDisasters, onCheckedChange = { showDisasters = it })
                    }
                }

                item {
                    Text("Weather & Radar Layers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                items(MapWeatherLayer.entries) { wLayer ->
                    val selected = weatherLayer == wLayer
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                weatherLayer = wLayer
                                showLayerSheet = false
                            },
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(wLayer.label, style = MaterialTheme.typography.bodyLarge, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                            if (wLayer.unit.isNotEmpty()) {
                                Text(wLayer.unit, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

private fun baseTileUrl(layer: MapBaseLayer, z: Int, x: Int, y: Int): String = when (layer) {
    MapBaseLayer.STANDARD -> "https://tile.openstreetmap.org/$z/$x/$y.png"
    MapBaseLayer.DARK -> "https://basemaps.cartocdn.com/dark_all/$z/$x/$y.png"
    MapBaseLayer.SATELLITE, MapBaseLayer.HYBRID ->
        "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/$z/$y/$x"
    MapBaseLayer.TERRAIN, MapBaseLayer.TOPOGRAPHIC ->
        "https://tile.opentopomap.org/$z/$x/$y.png"
}

private fun weatherLayerColor(layer: MapWeatherLayer, cur: CurrentWeather): Color = when (layer) {
    MapWeatherLayer.TEMPERATURE, MapWeatherLayer.FEELS_LIKE ->
        if (cur.temperatureC >= 30) SeverityHigh else if (cur.temperatureC >= 18) SkyAmber else SkyCyan
    MapWeatherLayer.RAIN, MapWeatherLayer.PRECIPITATION -> SkyCyan
    MapWeatherLayer.SNOW -> Color(0xFFE0F2FE)
    MapWeatherLayer.WIND, MapWeatherLayer.WIND_GUST, MapWeatherLayer.WIND_DIRECTION -> Color(0xFF38BDF8)
    MapWeatherLayer.LIGHTNING, MapWeatherLayer.STORM_CELLS, MapWeatherLayer.CAPE -> SeverityExtreme
    MapWeatherLayer.UV -> Color(0xFFA855F7)
    else -> SkyCobalt
}
