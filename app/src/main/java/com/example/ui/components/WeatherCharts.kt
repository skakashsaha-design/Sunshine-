package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

enum class ChartMetricType(val label: String, val unit: String) {
    TEMPERATURE("Temperature", "°"),
    RAIN_PROB("Rain Prob", "%"),
    PRECIP("Precip", "mm"),
    WIND("Wind", "km/h"),
    AQI("US AQI", "")
}

data class ChartPoint(
    val timeLabel: String,
    val value: Double,
    val secondaryLabel: String = ""
)

@Composable
fun InteractiveWeatherChart(
    points: List<ChartPoint>,
    metric: ChartMetricType,
    lineColor: Color,
    isBarChart: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        Text(
            text = "No ${metric.label.lowercase()} chart data available",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(16.dp)
        )
        return
    }

    var selectedIdx by remember(points, metric) { mutableIntStateOf(0) }
    val safeIdx = selectedIdx.coerceIn(0, points.lastIndex)
    val selected = points[safeIdx]

    val minVal = points.minOf { it.value }
    val maxVal = points.maxOf { it.value }
    val range = (maxVal - minVal).coerceAtLeast(1.0)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${selected.timeLabel} · ${selected.value.roundToInt()}${metric.unit}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = lineColor
            )
            Text(
                text = "Range ${minVal.roundToInt()}–${maxVal.roundToInt()}${metric.unit}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        selectedIdx = (frac * (points.size - 1)).roundToInt().coerceIn(0, points.lastIndex)
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val padTop = 14f
            val padBottom = 14f
            val chartH = (h - padTop - padBottom).coerceAtLeast(10f)
            val stepX = if (points.size > 1) w / (points.size - 1).toFloat() else w

            // Subtle horizontal guide lines
            for (g in 0..2) {
                val gy = padTop + chartH * (g / 2f)
                drawLine(
                    color = Color.Gray.copy(alpha = 0.2f),
                    start = Offset(0f, gy),
                    end = Offset(w, gy),
                    strokeWidth = 1f
                )
            }

            if (isBarChart) {
                val barW = (w / points.size.coerceAtLeast(1) * 0.6f).coerceIn(4f, 24f)
                points.forEachIndexed { idx, pt ->
                    val x = idx * stepX
                    val norm = ((pt.value - minVal) / range).toFloat().coerceIn(0.05f, 1f)
                    val barH = chartH * norm
                    drawRect(
                        color = if (idx == safeIdx) lineColor else lineColor.copy(alpha = 0.55f),
                        topLeft = Offset((x - barW / 2f).coerceIn(0f, w - barW), h - padBottom - barH),
                        size = Size(barW, barH)
                    )
                }
            } else {
                val path = Path()
                val fillPath = Path()
                val coords = points.mapIndexed { idx, pt ->
                    val x = idx * stepX
                    val norm = ((pt.value - minVal) / range).toFloat().coerceIn(0f, 1f)
                    val y = padTop + chartH * (1f - norm)
                    Offset(x, y)
                }

                coords.forEachIndexed { i, p ->
                    if (i == 0) {
                        path.moveTo(p.x, p.y)
                        fillPath.moveTo(p.x, h - padBottom)
                        fillPath.lineTo(p.x, p.y)
                    } else {
                        path.lineTo(p.x, p.y)
                        fillPath.lineTo(p.x, p.y)
                    }
                }
                fillPath.lineTo(coords.last().x, h - padBottom)
                fillPath.close()

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.28f), Color.Transparent))
                )
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                )

                val selPt = coords[safeIdx]
                drawLine(
                    color = lineColor.copy(alpha = 0.45f),
                    start = Offset(selPt.x, padTop),
                    end = Offset(selPt.x, h - padBottom),
                    strokeWidth = 2f
                )
                drawCircle(color = Color.White, radius = 8f, center = selPt)
                drawCircle(color = lineColor, radius = 5f, center = selPt)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        // Non-overlapping evenly spaced X-axis time labels (max 5 labels)
        val labelIndices = if (points.size <= 5) {
            points.indices.toList()
        } else {
            listOf(0, points.size / 4, points.size / 2, (points.size * 3) / 4, points.lastIndex)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labelIndices.forEach { idx ->
                Text(
                    text = points[idx].timeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
