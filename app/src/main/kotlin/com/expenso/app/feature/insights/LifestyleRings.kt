package com.expenso.app.feature.insights

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.expenso.app.core.domain.model.LifestyleGroup
import com.expenso.app.core.ui.components.formatInrCompact

private val lifestyleOrder = listOf(
    LifestyleGroup.ESSENTIAL,
    LifestyleGroup.LIFESTYLE,
    LifestyleGroup.GROWTH,
    LifestyleGroup.OTHER,
)

private fun colorFor(group: LifestyleGroup): Color = when (group) {
    LifestyleGroup.ESSENTIAL -> Color(0xFF2DAE85)
    LifestyleGroup.LIFESTYLE -> Color(0xFFE26A4F)
    LifestyleGroup.GROWTH -> Color(0xFF3C78D8)
    LifestyleGroup.OTHER -> Color(0xFF9A9A93)
}

@Composable
fun LifestyleRings(
    slices: List<LifestyleSlice>,
    modifier: Modifier = Modifier,
) {
    val animated = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        animated.snapTo(0f)
        animated.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }
    val byGroup = slices.associateBy { it.group }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(130.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(130.dp)) {
                val base = size.minDimension
                val strokeWidth = 10.dp.toPx()
                lifestyleOrder.forEachIndexed { index, group ->
                    val ringSize = base - index * (strokeWidth * 2 + 4.dp.toPx())
                    if (ringSize <= 0) return@forEachIndexed
                    val topLeft = Offset(
                        (size.width - ringSize) / 2f,
                        (size.height - ringSize) / 2f,
                    )
                    val arcSize = Size(ringSize, ringSize)
                    drawArc(
                        color = colorFor(group).copy(alpha = 0.15f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth),
                    )
                    val fraction = byGroup[group]?.fraction ?: 0f
                    drawArc(
                        color = colorFor(group),
                        startAngle = -90f,
                        sweepAngle = 360f * fraction * animated.value,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth),
                    )
                }
            }
        }
        Spacer(Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            lifestyleOrder.forEach { group ->
                val slice = byGroup[group]
                LegendRow(
                    color = colorFor(group),
                    label = group.displayName,
                    amount = slice?.totalMinor ?: 0L,
                    pct = slice?.fraction ?: 0f,
                )
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String, amount: Long, pct: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
        Text(
            formatInrCompact(amount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        Spacer(Modifier.size(6.dp))
        Text(
            "${(pct * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
