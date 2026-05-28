package com.expenso.app.feature.insights

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.expenso.app.core.domain.model.Category
import com.expenso.app.core.domain.model.LifestyleGroup
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val monthShort = DateTimeFormatter.ofPattern("MMM d")
private val dowFmt = DateTimeFormatter.ofPattern("EEE")
private val monthFmt = DateTimeFormatter.ofPattern("MMM")

@Composable
fun LifestyleFilterChips(
    selected: LifestyleGroup?,
    onSelect: (LifestyleGroup?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val all = listOf(null, LifestyleGroup.ESSENTIAL, LifestyleGroup.LIFESTYLE, LifestyleGroup.GROWTH, LifestyleGroup.OTHER)
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        items(all) { g ->
            val label = g?.displayName ?: "All"
            FilterChip(
                selected = selected == g,
                onClick = { onSelect(g) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = lifestyleColor(g).copy(alpha = 0.2f),
                ),
            )
        }
    }
}

@Composable
fun StackedBarChart(
    points: List<StackedDayPoint>,
    categories: List<Category>,
    lifestyleFilter: LifestyleGroup?,
    milestones: List<InsightMilestone> = emptyList(),
    onDayTap: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    period: InsightsPeriod = InsightsPeriod.WEEK,
    expanded: Boolean = false,
) {
    val catMap = remember(categories) { categories.associateBy { it.id } }
    val maxTotal = remember(points) { points.maxOfOrNull { it.totalMinor } ?: 0L }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(points, lifestyleFilter) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(500))
    }

    val chartHeight = if (expanded) 320.dp else 180.dp
    val gap = 4.dp
    val chartWidth = if (expanded && points.size > 12) (points.size * 36).dp else Dp.Unspecified

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (expanded) Modifier.horizontalScroll(rememberScrollState()) else Modifier)
                .height(chartHeight)
        ) {
            Canvas(
                modifier = if (chartWidth == Dp.Unspecified) {
                    Modifier
                        .fillMaxWidth()
                        .height(chartHeight)
                } else {
                    Modifier
                        .width(chartWidth)
                        .height(chartHeight)
                }.pointerInput(points) {
                    detectTapGestures { offset ->
                        if (points.isEmpty()) return@detectTapGestures
                        val barsWidth = size.width.toFloat()
                        val barWidth = barsWidth / points.size
                        val idx = (offset.x / barWidth).toInt().coerceIn(0, points.size - 1)
                        onDayTap(points[idx].date)
                    }
                }
            ) {
                if (points.isEmpty() || maxTotal <= 0L) return@Canvas
                val fullWidth = size.width
                val totalGap = (points.size - 1).coerceAtLeast(0) * gap.toPx()
                val barWidth = ((fullWidth - totalGap) / points.size).coerceAtLeast(1f)
                val maxHeight = size.height
                val milestoneMap = milestones.groupBy { it.date }
                points.forEachIndexed { idx, point ->
                    val x = idx * (barWidth + gap.toPx())
                    val xCenter = x + barWidth / 2f
                    milestoneMap[point.date].orEmpty().forEach { milestone ->
                        drawLine(
                            color = when (milestone.type) {
                                InsightMilestoneType.PEAK -> Color(0xFF7C4DFF)
                                InsightMilestoneType.SPIKE -> Color(0xFFE65100)
                                InsightMilestoneType.BIGGEST_EXPENSE -> Color(0xFFD81B60)
                                InsightMilestoneType.INCOME_DAY -> Color(0xFF2E7D32)
                            },
                            start = Offset(xCenter, 0f),
                            end = Offset(xCenter, maxHeight),
                            alpha = if (expanded) 0.35f else 0.2f,
                            strokeWidth = if (expanded) 2f else 1f,
                        )
                    }
                    val scale = (point.totalMinor.toDouble() / maxTotal.toDouble()).toFloat() * progress.value
                    val height = maxHeight * scale
                    if (point.segments.isEmpty() || point.totalMinor <= 0L) {
                        drawRoundRectSafe(
                            color = Color(0x22000000),
                            topLeft = Offset(x, maxHeight - 4f),
                            size = Size(barWidth, 4f),
                        )
                    } else {
                        var drawn = 0f
                        point.segments.forEach { seg ->
                            val segFrac = seg.amountMinor.toFloat() / point.totalMinor.toFloat()
                            val segH = height * segFrac
                            val color = catMap[seg.categoryId]?.let { parseHex(it.colorHex) } ?: Color(0xFF6A4FE2)
                            drawRoundRectSafe(
                                color = color,
                                topLeft = Offset(x, maxHeight - drawn - segH),
                                size = Size(barWidth, segH),
                            )
                            drawn += segH
                        }
                    }
                }
            }
        }
        if (milestones.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            TrendMilestoneLegend(milestones = milestones)
        }
        Spacer(Modifier.height(6.dp))
        if (points.isNotEmpty() && points.size <= 12) {
            Row(modifier = Modifier.fillMaxWidth()) {
                points.forEach { p ->
                    Text(
                        formatBarLabel(p.date, period),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        val segTotals = remember(points, lifestyleFilter) {
            points.flatMap { it.segments }
                .groupBy { it.categoryId }
                .mapValues { entry -> entry.value.sumOf { it.amountMinor } }
                .toList()
                .sortedByDescending { it.second }
                .take(6)
        }
        segTotals.forEach { (catId, amount) ->
            val cat = catMap[catId] ?: return@forEach
            val color = parseHex(cat.colorHex)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { /* could drill into category */ })
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(color, CircleShape),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    "${cat.emoji} ${cat.name}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    com.expenso.app.core.ui.components.formatInrCompact(amount),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun TrendMilestoneLegend(milestones: List<InsightMilestone>) {
    val kinds = milestones.map { it.type }.distinct()
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        kinds.forEach { type ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            when (type) {
                                InsightMilestoneType.PEAK -> Color(0xFF7C4DFF)
                                InsightMilestoneType.SPIKE -> Color(0xFFE65100)
                                InsightMilestoneType.BIGGEST_EXPENSE -> Color(0xFFD81B60)
                                InsightMilestoneType.INCOME_DAY -> Color(0xFF2E7D32)
                            },
                            CircleShape,
                        ),
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    when (type) {
                        InsightMilestoneType.PEAK -> "Peak"
                        InsightMilestoneType.SPIKE -> "Spike"
                        InsightMilestoneType.BIGGEST_EXPENSE -> "Biggest"
                        InsightMilestoneType.INCOME_DAY -> "Income"
                    },
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

private fun formatBarLabel(date: LocalDate, period: InsightsPeriod): String = when (period) {
    InsightsPeriod.WEEK -> date.format(dowFmt)
    InsightsPeriod.MONTH -> date.format(monthShort)
    InsightsPeriod.YEAR -> date.format(monthFmt)
}

fun lifestyleColor(group: LifestyleGroup?): Color = when (group) {
    LifestyleGroup.ESSENTIAL -> Color(0xFF2DAE85)
    LifestyleGroup.LIFESTYLE -> Color(0xFFE26A4F)
    LifestyleGroup.GROWTH -> Color(0xFF6A4FE2)
    LifestyleGroup.OTHER -> Color(0xFF9A9A93)
    null -> Color(0xFF1D1B22)
}

internal fun parseHex(hex: String): Color = runCatching {
    val h = hex.removePrefix("#")
    val v = if (h.length == 6) "FF$h" else h
    Color(v.toLong(16))
}.getOrDefault(Color(0xFF6A4FE2))

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRoundRectSafe(
    color: Color,
    topLeft: Offset,
    size: Size,
) {
    if (size.width <= 0f || size.height <= 0f) return
    drawRect(color = color, topLeft = topLeft, size = size)
}
