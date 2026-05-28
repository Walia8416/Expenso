package com.expenso.app.feature.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expenso.app.core.ui.components.AnimatedRupee
import com.expenso.app.core.ui.components.formatInrCompact

private val income = Color(0xFF14B886)
private val spent = Color(0xFFE26A4F)
private val net = Color(0xFF6A4FE2)

@Composable
fun BalanceCard(
    incomeMinor: Long,
    spentMinor: Long,
    netMinor: Long,
    savingsRatePct: Float,
    incomeDeltaPct: Float,
    spentDeltaPct: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .padding(20.dp),
    ) {
        Text(
            "Balance this period",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BalanceTile(
                label = "Income",
                value = incomeMinor,
                deltaPct = incomeDeltaPct,
                accent = income,
                modifier = Modifier.weight(1f),
            )
            BalanceTile(
                label = "Spent",
                value = spentMinor,
                deltaPct = spentDeltaPct,
                deltaInvert = true,
                accent = spent,
                modifier = Modifier.weight(1f),
            )
            BalanceTile(
                label = "Net",
                value = netMinor,
                isNet = true,
                accent = net,
                modifier = Modifier.weight(1f),
            )
        }

        if (incomeMinor > 0L || spentMinor > 0L) {
            Spacer(Modifier.height(16.dp))
            SavingsRateBar(
                incomeMinor = incomeMinor,
                spentMinor = spentMinor,
                savingsRatePct = savingsRatePct,
            )
        }
    }
}

@Composable
private fun BalanceTile(
    label: String,
    value: Long,
    deltaPct: Float = 0f,
    deltaInvert: Boolean = false,
    isNet: Boolean = false,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(accent.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        if (isNet) {
            val color = if (value >= 0) income else spent
            AnimatedRupee(
                amountMinor = kotlin.math.abs(value),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = color,
                compact = true,
            )
            Text(
                if (value >= 0) "in the green" else "overspent",
                style = MaterialTheme.typography.labelSmall,
                color = color,
            )
        } else {
            AnimatedRupee(
                amountMinor = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                compact = true,
            )
            if (deltaPct != 0f) {
                val goodDirection = if (deltaInvert) deltaPct < 0 else deltaPct > 0
                val arrow = if (deltaPct > 0f) "\u25B2" else "\u25BC"
                val color = if (goodDirection) income else spent
                Text(
                    "$arrow ${"%.0f".format(kotlin.math.abs(deltaPct))}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                )
            }
        }
    }
}

@Composable
private fun SavingsRateBar(
    incomeMinor: Long,
    spentMinor: Long,
    savingsRatePct: Float,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (savingsRatePct >= 0) "Saving ${"%.0f".format(savingsRatePct)}% of income"
                else "Over budget by ${"%.0f".format(-savingsRatePct)}%",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                formatInrCompact(incomeMinor - spentMinor),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (savingsRatePct >= 0) income else spent,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(6.dp))
        val cap = if (incomeMinor <= 0L) 1f
        else (spentMinor.toFloat() / incomeMinor.toFloat()).coerceIn(0f, 1f)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(100)),
        ) {
            Box(
                modifier = Modifier
                    .weight(cap.coerceAtLeast(0.001f))
                    .fillMaxWidth()
                    .background(spent, RoundedCornerShape(100)),
            )
            if (cap < 1f) {
                Box(
                    modifier = Modifier
                        .weight((1f - cap))
                        .fillMaxWidth()
                        .background(income.copy(alpha = 0.35f), RoundedCornerShape(100)),
                )
            }
        }
    }
}
