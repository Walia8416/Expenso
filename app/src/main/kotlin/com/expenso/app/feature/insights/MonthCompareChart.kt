package com.expenso.app.feature.insights

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.expenso.app.core.ui.components.GradientCard
import com.expenso.app.core.ui.components.formatInrCompact
import com.expenso.app.core.ui.theme.GradCoolEnd
import com.expenso.app.core.ui.theme.GradPrimaryEnd
import com.expenso.app.core.ui.theme.GradPrimaryStart

/**
 * Dual-series month-comparison line chart. Smooth bezier-eased curves through
 * each series' points, gradient under-curve fills, dotted gridlines. Animates
 * upward from the baseline on first composition.
 *
 * Retains the original signature for drop-in replacement of the old bar/line
 * chart — `mode` is ignored; the chart is always a line chart now.
 */
@Composable
fun MonthCompareChart(
    buckets: List<CompareBucket>,
    @Suppress("UNUSED_PARAMETER") mode: CompareChartMode,
    leftLabel: String,
    rightLabel: String,
    modifier: Modifier = Modifier,
) {
    if (buckets.isEmpty()) return

    val maxValue = buckets
        .maxOfOrNull { maxOf(it.leftTotalMinor, it.rightTotalMinor) }
        ?.toFloat()
        ?.coerceAtLeast(1f) ?: 1f

    val progress = remember(buckets) { Animatable(0f) }
    LaunchedEffect(buckets) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        )
    }

    GradientCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Month comparison",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            LegendDot(GradPrimaryStart, leftLabel)
            Spacer(Modifier.size(10.dp))
            LegendDot(GradCoolEnd, rightLabel)
        }
        Spacer(Modifier.height(14.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        ) {
            drawChart(
                buckets = buckets,
                maxValue = maxValue,
                progress = progress.value,
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            buckets.forEach {
                Text(
                    it.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        val leftTotal = buckets.sumOf { it.leftTotalMinor }
        val rightTotal = buckets.sumOf { it.rightTotalMinor }
        Text(
            "$leftLabel ${formatInrCompact(leftTotal)}  vs  $rightLabel ${formatInrCompact(rightTotal)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun DrawScope.drawChart(
    buckets: List<CompareBucket>,
    maxValue: Float,
    progress: Float,
) {
    val w = size.width
    val h = size.height
    val padTop = 14f
    val padBottom = 14f
    val chartH = h - padTop - padBottom
    val baselineY = padTop + chartH

    // 4 dotted gridlines (faint).
    val gridColor = Color.White.copy(alpha = 0.08f)
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f), 0f)
    repeat(4) { i ->
        val y = padTop + chartH * (i / 3f)
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = 1f,
            pathEffect = dashEffect,
        )
    }

    val n = buckets.size
    val stepX = if (n > 1) w / (n - 1).toFloat() else 0f

    // Each point's y interpolates from baseline up to its real height — gives
    // the "growing from the floor" feel without needing a PathMeasure trick.
    fun pointsFor(extract: (CompareBucket) -> Long): List<Offset> =
        buckets.mapIndexed { i, b ->
            val targetY = baselineY - (extract(b) / maxValue) * chartH
            val animatedY = baselineY + (targetY - baselineY) * progress
            Offset(i * stepX, animatedY)
        }

    val leftPts = pointsFor { it.leftTotalMinor }
    val rightPts = pointsFor { it.rightTotalMinor }

    drawFilledCurve(rightPts, baselineY, GradCoolEnd)
    drawFilledCurve(leftPts, baselineY, GradPrimaryStart)

    drawSmoothLine(leftPts, GradPrimaryStart, GradPrimaryEnd, w)
    drawSmoothLine(rightPts, GradCoolEnd, GradCoolEnd, w)

    if (progress >= 0.95f) {
        leftPts.lastOrNull()?.let { drawDot(it, GradPrimaryStart) }
        rightPts.lastOrNull()?.let { drawDot(it, GradCoolEnd) }
    }
}

private fun buildBezierPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points.first().x, points.first().y)
    for (i in 1 until points.size) {
        val p0 = points[i - 1]
        val p1 = points[i]
        val midX = (p0.x + p1.x) / 2f
        path.cubicTo(
            x1 = midX, y1 = p0.y,
            x2 = midX, y2 = p1.y,
            x3 = p1.x, y3 = p1.y,
        )
    }
    return path
}

private fun DrawScope.drawSmoothLine(
    points: List<Offset>,
    startColor: Color,
    endColor: Color,
    width: Float,
) {
    if (points.size < 2) return
    drawPath(
        path = buildBezierPath(points),
        brush = Brush.linearGradient(
            colors = listOf(startColor, endColor),
            start = Offset(0f, 0f),
            end = Offset(width, 0f),
        ),
        style = Stroke(width = 5f, cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawFilledCurve(
    points: List<Offset>,
    bottomY: Float,
    color: Color,
) {
    if (points.size < 2) return
    val curve = buildBezierPath(points)
    val fillPath = Path().apply {
        addPath(curve)
        lineTo(points.last().x, bottomY)
        lineTo(points.first().x, bottomY)
        close()
    }
    drawPath(
        path = fillPath,
        brush = Brush.verticalGradient(
            colors = listOf(color.copy(alpha = 0.35f), color.copy(alpha = 0f)),
            startY = 0f,
            endY = bottomY,
        ),
    )
}

private fun DrawScope.drawDot(center: Offset, color: Color) {
    drawCircle(color = Color.White, radius = 7f, center = center)
    drawCircle(color = color, radius = 5f, center = center)
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(100)),
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
