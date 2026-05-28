package com.expenso.app.feature.insights

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.expenso.app.core.ui.components.AnimatedRupee

internal fun parseColorHex(hex: String, fallback: Color): Color = runCatching {
    val clean = hex.removePrefix("#")
    val v = if (clean.length == 6) ("FF$clean") else clean
    Color(v.toLong(16))
}.getOrDefault(fallback)

@Composable
fun CategoryDonut(
    slices: List<CategorySlice>,
    totalMinor: Long,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(slices, totalMinor) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(900, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = modifier.size(240.dp),
        contentAlignment = Alignment.Center,
    ) {
        val outline = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 28.dp.toPx()
            val diameter = size.minDimension - stroke
            val topLeft = Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f,
            )
            val arcSize = Size(diameter, diameter)

            drawArc(
                color = outline,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke),
            )
            if (slices.isEmpty() || totalMinor <= 0L) return@Canvas

            var start = -90f
            val totalProgress = progress.value
            for (slice in slices) {
                val fullSweep = slice.fraction * 360f
                val sweep = (fullSweep * totalProgress).coerceAtLeast(0.001f)
                drawArc(
                    color = parseColorHex(slice.category.colorHex, outline),
                    startAngle = start,
                    sweepAngle = (sweep - 2f).coerceAtLeast(0.001f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke),
                )
                start += fullSweep
            }
        }
        Box(contentAlignment = Alignment.Center) {
            AnimatedRupee(
                amountMinor = totalMinor,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "spent",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 40.dp),
            )
        }
    }
}
