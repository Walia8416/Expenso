package com.expenso.app.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

@Composable
fun AnimatedRupee(
    amountMinor: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.displayMedium,
    color: Color = LocalContentColor.current,
    durationMs: Int = 700,
    /** When true, renders with `formatInrCompact` (e.g. ₹1.2L / ₹3.4Cr). */
    compact: Boolean = false,
) {
    val animated = remember { Animatable(0f) }
    LaunchedEffect(amountMinor) {
        animated.animateTo(
            targetValue = amountMinor.toFloat(),
            animationSpec = tween(durationMs, easing = FastOutSlowInEasing),
        )
    }
    val rendered = animated.value.toLong()
    Text(
        text = if (compact) formatInrCompact(rendered) else formatInr(rendered),
        style = style,
        color = color,
        modifier = modifier,
        maxLines = 1,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
    )
}
