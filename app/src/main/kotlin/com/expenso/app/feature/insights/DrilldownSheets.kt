package com.expenso.app.feature.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expenso.app.core.domain.model.Expense
import com.expenso.app.core.ui.components.formatInr
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayBreakdownSheet(
    date: LocalDate,
    period: InsightsPeriod,
    expenses: List<Expense>,
    onDismiss: () -> Unit,
    onOpenExpense: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val zone = ZoneId.systemDefault()

    // When the trend groups by week or month, a tap resolves to the bucket
    // START date — fan out filtering to include every day in that bucket.
    val rangeEnd = when (period) {
        InsightsPeriod.WEEK -> date.plusDays(1)
        InsightsPeriod.MONTH -> date.plusWeeks(1)
        InsightsPeriod.YEAR -> date.plusMonths(1)
    }
    val inRange = expenses.filter {
        val d = Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate()
        !d.isBefore(date) && d.isBefore(rangeEnd)
    }
    val total = inRange.sumOf { it.amountMinor }

    val header = when (period) {
        InsightsPeriod.WEEK -> date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
        InsightsPeriod.MONTH -> {
            val end = rangeEnd.minusDays(1)
            val startFmt = DateTimeFormatter.ofPattern("MMM d")
            if (date.month == end.month) {
                "${date.format(startFmt)} \u2013 ${end.dayOfMonth}"
            } else {
                "${date.format(startFmt)} \u2013 ${end.format(startFmt)}"
            }
        }
        InsightsPeriod.YEAR -> date.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }
    val subtitle = when (period) {
        InsightsPeriod.WEEK -> "transactions"
        InsightsPeriod.MONTH -> "transactions this week"
        InsightsPeriod.YEAR -> "transactions this month"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(header, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "${inRange.size} $subtitle \u2022 ${formatInr(total)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))

            if (inRange.isEmpty()) {
                Text(
                    "Nothing in this period.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(32.dp))
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(480.dp),
                ) {
                    items(inRange.sortedByDescending { it.createdAt }, key = { it.id }) { e ->
                        ExpenseMiniRow(e, onClick = { onOpenExpense(e.id) })
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDrilldownSheet(
    slice: CategorySlice,
    expensesInCategory: List<Expense>,
    onDismiss: () -> Unit,
    onOpenExpense: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val color = parseHex(slice.category.colorHex)
    val days = expensesInCategory.groupBy { millisToDay(it.createdAt) }
    val dayCount = days.size.coerceAtLeast(1)
    val average = slice.totalMinor / dayCount.coerceAtLeast(1)
    val biggest = expensesInCategory.maxByOrNull { it.amountMinor }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(color.copy(alpha = 0.18f), CircleShape)
                        .padding(12.dp),
                ) {
                    Text(slice.category.emoji, style = MaterialTheme.typography.headlineSmall)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        slice.category.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "${slice.count} txn \u2022 ${(slice.fraction * 100).toInt()}% of spend",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(formatInr(slice.totalMinor), style = MaterialTheme.typography.titleLarge)
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MiniStat("Days active", dayCount.toString(), color, Modifier.weight(1f))
                MiniStat(
                    "Avg / active day",
                    com.expenso.app.core.ui.components.formatInrCompact(average),
                    color,
                    Modifier.weight(1f),
                )
                if (biggest != null) {
                    MiniStat(
                        "Biggest",
                        com.expenso.app.core.ui.components.formatInrCompact(biggest.amountMinor),
                        color,
                        Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "All transactions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(420.dp),
            ) {
                items(expensesInCategory.sortedByDescending { it.createdAt }, key = { it.id }) { e ->
                    ExpenseMiniRow(e, onClick = { onOpenExpense(e.id) })
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ExpenseMiniRow(e: Expense, onClick: () -> Unit) {
    val zone = ZoneId.systemDefault()
    val time = Instant.ofEpochMilli(e.createdAt).atZone(zone).toLocalDateTime()
    val timeFmt = DateTimeFormatter.ofPattern("MMM d, h:mm a")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                RoundedCornerShape(14.dp),
            )
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(e.category.emoji, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                e.merchantName ?: e.payee?.displayName ?: e.note ?: e.category.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
            )
            Text(
                time.format(timeFmt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            formatInr(e.amountMinor),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun millisToDay(millis: Long): Long {
    val zone = ZoneId.systemDefault()
    return Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().toEpochDay()
}
