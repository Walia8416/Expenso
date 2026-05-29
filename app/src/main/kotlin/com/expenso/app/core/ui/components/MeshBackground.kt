package com.expenso.app.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.expenso.app.core.ui.theme.GradCoolEnd
import com.expenso.app.core.ui.theme.GradCoolStart
import com.expenso.app.core.ui.theme.GradPrimaryEnd
import com.expenso.app.core.ui.theme.GradPrimaryStart
import com.expenso.app.core.ui.theme.HeroMidnight
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated radial-blob mesh — four glow centers drifting in a slow Lissajous
 * pattern. Sits behind app content in dark mode to create the premium
 * financial-app aesthetic (Revolut / Monzo Plus). Cheap: one Canvas + four
 * radial gradients redrawn per frame at ~60fps on modern devices.
 */
@Composable
fun MeshBackground(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "mesh")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "meshT",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val theta = t * 2f * Math.PI.toFloat()

        drawBlob(
            color = GradPrimaryStart.copy(alpha = 0.35f),
            center = Offset(
                x = w * (0.25f + 0.10f * cos(theta)),
                y = h * (0.22f + 0.08f * sin(theta)),
            ),
            radius = maxOf(w, h) * 0.55f,
        )
        drawBlob(
            color = GradPrimaryEnd.copy(alpha = 0.22f),
            center = Offset(
                x = w * (0.78f + 0.08f * cos(theta + 1.4f)),
                y = h * (0.18f + 0.10f * sin(theta + 1.4f)),
            ),
            radius = maxOf(w, h) * 0.50f,
        )
        drawBlob(
            color = GradCoolStart.copy(alpha = 0.20f),
            center = Offset(
                x = w * (0.20f + 0.08f * cos(theta + 2.6f)),
                y = h * (0.80f + 0.10f * sin(theta + 2.6f)),
            ),
            radius = maxOf(w, h) * 0.55f,
        )
        drawBlob(
            color = GradCoolEnd.copy(alpha = 0.22f),
            center = Offset(
                x = w * (0.82f + 0.10f * cos(theta + 4.1f)),
                y = h * (0.78f + 0.08f * sin(theta + 4.1f)),
            ),
            radius = maxOf(w, h) * 0.45f,
        )
    }
}

private fun DrawScope.drawBlob(
    color: Color,
    center: Offset,
    radius: Float,
) {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(color, HeroMidnight.copy(alpha = 0f)),
            center = center,
            radius = radius,
        ),
        topLeft = Offset.Zero,
        size = Size(size.width, size.height),
    )
}
