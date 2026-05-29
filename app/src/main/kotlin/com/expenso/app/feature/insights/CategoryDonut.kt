package com.expenso.app.feature.insights

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.expenso.app.core.ui.components.AnimatedRupee
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    // Each slice gets its own Animatable so they grow in a staggered cascade
    // (60ms per slice) — looks more alive than a single global progress.
    val sliceProgress = remember(slices) {
        List(slices.size) { Animatable(0f) }
    }
    LaunchedEffect(slices, totalMinor) {
        sliceProgress.forEachIndexed { i, anim ->
            launch {
                delay(60L * i)
                anim.snapTo(0f)
                anim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
                )
            }
        }
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
            slices.forEachIndexed { i, slice ->
                val fullSweep = slice.fraction * 360f
                val animated = (sliceProgress.getOrNull(i)?.value ?: 1f)
                // 4° gap between slices so each segment reads as a distinct
                // pill rather than a continuous ring.
                val gap = if (slices.size > 1) 4f else 0f
                val sweep = ((fullSweep - gap) * animated).coerceAtLeast(0.001f)
                drawArc(
                    color = parseColorHex(slice.category.colorHex, outline),
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                start += fullSweep
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AnimatedRupee(
                amountMinor = totalMinor,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "spent",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
