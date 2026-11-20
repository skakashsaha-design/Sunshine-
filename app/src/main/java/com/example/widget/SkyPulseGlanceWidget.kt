package com.example.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class GlanceWeatherSnapshot(
    val city: String = "SkyPulse",
    val temperatureDisplay: String = "--°C",
    val conditionLabel: String = "Tap to sync",
    val feelsLikeDisplay: String = "Feels --°",
    val highLowDisplay: String = "",
    val aqiDisplay: String = "AQI --",
    val updatedTime: String = "--:--",
    val sourceLabel: String = "Open-Meteo",
    val isOffline: Boolean = false
)

object GlanceSnapshotStore {
    private const val PREF_NAME = "skypulse_glance_snapshot"

    fun save(context: Context, snap: GlanceWeatherSnapshot) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .putString("city", snap.city)
            .putString("temp", snap.temperatureDisplay)
            .putString("cond", snap.conditionLabel)
            .putString("feels", snap.feelsLikeDisplay)
            .putString("hl", snap.highLowDisplay)
            .putString("aqi", snap.aqiDisplay)
            .putString("updated", snap.updatedTime)
            .putString("source", snap.sourceLabel)
            .putBoolean("offline", snap.isOffline)
            .apply()
    }

    fun read(context: Context): GlanceWeatherSnapshot {
        val p = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return GlanceWeatherSnapshot(
            city = p.getString("city", "SkyPulse") ?: "SkyPulse",
            temperatureDisplay = p.getString("temp", "--°C") ?: "--°C",
            conditionLabel = p.getString("cond", "Tap to sync") ?: "Tap to sync",
            feelsLikeDisplay = p.getString("feels", "Feels --°") ?: "Feels --°",
            highLowDisplay = p.getString("hl", "") ?: "",
            aqiDisplay = p.getString("aqi", "AQI --") ?: "AQI --",
            updatedTime = p.getString("updated", "--:--") ?: "--:--",
            sourceLabel = p.getString("source", "Open-Meteo") ?: "Open-Meteo",
            isOffline = p.getBoolean("offline", false)
        )
    }
}

class SkyPulseGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = withContext(Dispatchers.IO) {
            GlanceSnapshotStore.read(context)
        }
        provideContent {
            GlanceTheme {
                SkyPulseGlanceContent(context = context, snap = snapshot)
            }
        }
    }

    companion object {
        suspend fun refreshAll(context: Context) {
            runCatching {
                val manager = GlanceAppWidgetManager(context)
                if (manager.getGlanceIds(SkyPulseGlanceWidget::class.java).isNotEmpty()) {
                    SkyPulseGlanceWidget().updateAll(context)
                }
            }
        }
    }
}

@Composable
private fun SkyPulseGlanceContent(context: Context, snap: GlanceWeatherSnapshot) {
    val bgColor = ColorProvider(Color(0xFF0E1F38))
    val primaryText = ColorProvider(Color(0xFFF8FAFC))
    val secondaryText = ColorProvider(Color(0xFFCBD5E1))
    val accentText = ColorProvider(Color(0xFF38BDF8))
    val statusColor = ColorProvider(if (snap.isOffline) Color(0xFFFBBF24) else Color(0xFF34D399))

    val launchIntent = Intent(context, MainActivity::class.java)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bgColor)
            .cornerRadius(20.dp)
            .clickable(actionStartActivity(launchIntent))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = snap.city,
                style = TextStyle(color = accentText, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = if (snap.isOffline) "OFFLINE" else snap.updatedTime,
                style = TextStyle(color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Medium),
                maxLines = 1
            )
        }

        Spacer(modifier = GlanceModifier.height(4.dp))

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = snap.temperatureDisplay,
                style = TextStyle(color = primaryText, fontSize = 32.sp, fontWeight = FontWeight.Bold),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.width(10.dp))
            Column {
                Text(
                    text = snap.conditionLabel,
                    style = TextStyle(color = primaryText, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1
                )
                Text(
                    text = snap.feelsLikeDisplay,
                    style = TextStyle(color = secondaryText, fontSize = 11.sp),
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(6.dp))

        val footerText = buildString {
            if (snap.highLowDisplay.isNotBlank()) append("${snap.highLowDisplay} · ")
            append(snap.aqiDisplay)
            append(" · ")
            append(snap.sourceLabel)
        }
        Text(
            text = footerText,
            style = TextStyle(color = secondaryText, fontSize = 11.sp),
            maxLines = 1
        )
    }
}

class SkyPulseGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SkyPulseGlanceWidget()
}
