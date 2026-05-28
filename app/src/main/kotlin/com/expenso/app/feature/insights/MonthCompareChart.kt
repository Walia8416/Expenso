package com.expenso.app.feature.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.expenso.app.core.ui.components.formatInrCompact

@Composable
fun MonthCompareChart(
    buckets: List<CompareBucket>,
    mode: CompareChartMode,
    leftLabel: String,
    rightLabel: String,
    modifier: Modifier = Modifier,
) {
    if (buckets.isEmpty()) return
    val maxV = buckets.maxOfOrNull { maxOf(it.leftTotalMinor, it.rightTotalMinor) }?.toFloat()?.coerceAtLeast(1f) ?: 1f
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            LegendDot(Color(0xFF5E7BFF), leftLabel)
            LegendDot(Color(0xFF22B573), rightLabel)
        }
        Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
            val gap = 14.dp.toPx()
            val bucketW = (size.width - (buckets.size - 1) * gap) / buckets.size
            buckets.forEachIndexed { i, b ->
                val x = i * (bucketW + gap)
                val lh = (b.leftTotalMinor / maxV) * size.height
                val rh = (b.rightTotalMinor / maxV) * size.height
                if (mode == CompareChartMode.PAIRED_BARS) {
                    drawRect(
                        color = Color(0xFF5E7BFF),
                        topLeft = Offset(x, size.height - lh),
                        size = androidx.compose.ui.geometry.Size(bucketW * 0.44f, lh),
                    )
                    drawRect(
                        color = Color(0xFF22B573),
                        topLeft = Offset(x + bucketW * 0.56f, size.height - rh),
                        size = androidx.compose.ui.geometry.Size(bucketW * 0.44f, rh),
                    )
                } else {
                    val cx = x + bucketW / 2f
                    if (i > 0) {
                        val px = (i - 1) * (bucketW + gap) + bucketW / 2f
                        val plh = (buckets[i - 1].leftTotalMinor / maxV) * size.height
                        val prh = (buckets[i - 1].rightTotalMinor / maxV) * size.height
                        drawLine(Color(0xFF5E7BFF), Offset(px, size.height - plh), Offset(cx, size.height - lh), strokeWidth = 4f)
                        drawLine(Color(0xFF22B573), Offset(px, size.height - prh), Offset(cx, size.height - rh), strokeWidth = 4f)
                    }
                    drawCircle(Color(0xFF5E7BFF), radius = 5f, center = Offset(cx, size.height - lh))
                    drawCircle(Color(0xFF22B573), radius = 5f, center = Offset(cx, size.height - rh))
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            buckets.forEach { Text(it.label, style = MaterialTheme.typography.labelSmall) }
        }
        val leftTotal = buckets.sumOf { it.leftTotalMinor }
        val rightTotal = buckets.sumOf { it.rightTotalMinor }
        Text(
            "$leftLabel ${formatInrCompact(leftTotal)}  vs  $rightLabel ${formatInrCompact(rightTotal)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(100)),
        )
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
