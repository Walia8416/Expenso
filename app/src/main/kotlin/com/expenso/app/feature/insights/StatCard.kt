package com.expenso.app.feature.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingFlat
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    trendPct: Float? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    gradient: Brush? = null,
    modifier: Modifier = Modifier,
) {
    val onVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val titleColor = if (gradient != null) Color.White.copy(alpha = 0.85f) else onVariant
    val valueColor = if (gradient != null) Color.White else accent
    val subtitleColor = if (gradient != null) Color.White.copy(alpha = 0.75f) else onVariant
    Column(
        modifier = modifier
            .background(
                gradient ?: Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surface,
                    )
                ),
                RoundedCornerShape(20.dp),
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = titleColor,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (trendPct != null) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val absPct = abs(trendPct)
                val isUp = trendPct > 1f
                val isDown = trendPct < -1f
                val color = when {
                    isUp -> MaterialTheme.colorScheme.error
                    isDown -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val icon = when {
                    isUp -> Icons.Rounded.TrendingUp
                    isDown -> Icons.Rounded.TrendingDown
                    else -> Icons.Rounded.TrendingFlat
                }
                Icon(icon, contentDescription = null, tint = color)
                Spacer(Modifier.height(0.dp))
                Text(
                    "  ${"%.0f".format(absPct)}% vs last",
                    style = MaterialTheme.typography.bodySmall,
                    color = color,
                )
            }
        } else if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = subtitleColor,
            )
        }
    }
}
