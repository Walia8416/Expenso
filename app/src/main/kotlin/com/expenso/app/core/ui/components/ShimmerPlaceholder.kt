package com.expenso.app.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.expenso.app.core.ui.theme.GlassWhite4
import com.expenso.app.core.ui.theme.GlassWhite8

/**
 * Animated shimmer placeholder used while data loads. A translucent
 * highlight band sweeps diagonally across the surface.
 */
@Composable
fun ShimmerPlaceholder(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val t by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerT",
    )
    val brush = Brush.linearGradient(
        colors = listOf(GlassWhite4, GlassWhite8, GlassWhite4),
        start = Offset(t * 800f, 0f),
        end = Offset(t * 800f + 400f, 400f),
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(GlassWhite4)
            .background(brush),
    )
}
