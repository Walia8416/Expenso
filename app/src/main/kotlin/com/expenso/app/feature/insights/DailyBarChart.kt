package com.expenso.app.feature.insights

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DailyBarChart(
    points: List<DailyPoint>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    accentColor: Color = MaterialTheme.colorScheme.tertiary,
    groupByMonth: Boolean = false,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(points) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
    }

    val bucketed = remember(points, groupByMonth) {
        if (!groupByMonth) points.map { it.date to it.totalMinor }
        else points.groupBy { it.date.withDayOfMonth(1) }
            .entries
            .sortedBy { it.key }
            .map { (date, items) -> date to items.sumOf { it.totalMinor } }
    }
    val maxVal = bucketed.maxOfOrNull { it.second }?.coerceAtLeast(1L) ?: 1L
    val today = LocalDate.now()
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)

    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (bucketed.isEmpty()) return@Canvas
                val left = 0f
                val right = size.width
                val top = 0f
                val bottom = size.height - 24.dp.toPx()

                for (i in 0..3) {
                    val y = top + (bottom - top) * (i / 3f)
                    drawLine(
                        color = gridColor,
                        start = Offset(left, y),
                        end = Offset(right, y),
                        strokeWidth = 1.5f,
                    )
                }

                val gap = 4.dp.toPx()
                val usableWidth = right - left
                val barWidth = ((usableWidth - (bucketed.size - 1) * gap) / bucketed.size)
                    .coerceAtLeast(2f)
                val baseY = bottom
                bucketed.forEachIndexed { index, (date, total) ->
                    val fraction = (total.toFloat() / maxVal).coerceIn(0f, 1f) * progress.value
                    val barHeight = (baseY - top) * fraction
                    val x = left + index * (barWidth + gap)
                    val topY = baseY - barHeight
                    val color = when {
                        date == today -> accentColor
                        else -> barColor
                    }
                    val brush = Brush.verticalGradient(
                        colors = listOf(color.copy(alpha = 0.95f), color.copy(alpha = 0.55f)),
                        startY = topY,
                        endY = baseY,
                    )
                    drawRoundRect(
                        brush = brush,
                        topLeft = Offset(x, topY),
                        size = Size(barWidth, barHeight.coerceAtLeast(0f)),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            barWidth.coerceAtMost(10f), barWidth.coerceAtMost(10f)
                        ),
                    )
                }
            }
        }

        if (bucketed.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth()) {
                val fmt = if (groupByMonth) DateTimeFormatter.ofPattern("LLL")
                else DateTimeFormatter.ofPattern("EE")
                val showLabelAt = pickLabelIndices(bucketed.size)
                bucketed.forEachIndexed { index, (date, _) ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (index in showLabelAt) {
                            Text(
                                text = date.format(fmt).take(3),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun pickLabelIndices(size: Int): Set<Int> {
    if (size <= 7) return (0 until size).toSet()
    val step = (size / 6).coerceAtLeast(1)
    return (0 until size step step).toSet() + (size - 1)
}
