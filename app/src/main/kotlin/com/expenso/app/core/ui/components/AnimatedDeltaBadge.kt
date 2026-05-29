package com.expenso.app.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingFlat
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Pill badge showing a percentage delta with directional icon. The badge
 * springs into place on value change and color-codes by sign.
 *
 * @param deltaPercent positive = increase, negative = decrease, ~0 = flat.
 * @param positiveIsGood true for income (up = green); false for spend (up = red).
 */
@Composable
fun AnimatedDeltaBadge(
    deltaPercent: Float,
    modifier: Modifier = Modifier,
    positiveIsGood: Boolean = false,
) {
    val flat = abs(deltaPercent) < 0.5f
    val up = deltaPercent > 0
    val good = flat || (up == positiveIsGood)

    val targetColor = when {
        flat -> MaterialTheme.colorScheme.onSurfaceVariant
        good -> Color(0xFF22C55E)
        else -> Color(0xFFEF4444)
    }
    val color by animateColorAsState(targetColor, label = "deltaColor")
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "deltaScale",
    )

    val icon = when {
        flat -> Icons.Rounded.TrendingFlat
        up -> Icons.Rounded.TrendingUp
        else -> Icons.Rounded.TrendingDown
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .scale(scale)
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(50))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color)
        Spacer(Modifier.width(2.dp))
        Text(
            text = "${if (up && !flat) "+" else ""}${"%.1f".format(deltaPercent)}%",
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}
