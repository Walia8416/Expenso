package com.expenso.app.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenso.app.R
import com.expenso.app.core.domain.model.Expense
import com.expenso.app.core.domain.model.Income
import com.expenso.app.core.domain.model.PaymentMethod
import com.expenso.app.core.domain.model.PaymentStatus
import com.expenso.app.core.ui.components.LottieLoop
import com.expenso.app.core.ui.components.formatDayDdMmm
import com.expenso.app.core.ui.components.formatInr
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

private val incomeColor = Color(0xFF14B886)

@Composable
fun HistoryScreen(
    onOpenExpense: (String) -> Unit,
    vm: HistoryViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TopSummary(
            todayTotalMinor = state.todayTotalMinor,
            todayIncomeMinor = state.todayIncomeMinor,
            pendingCount = state.pendingCount,
        )

        FilterRow(
            filter = state.filter,
            dateRange = state.dateRange,
            onSelect = vm::setFilter,
            onSetRange = vm::setDateRange,
            onClearRange = vm::clearDateRange,
        )

        if (state.groups.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp),
                ) {
                    LottieLoop(
                        res = R.raw.history_empty,
                        modifier = Modifier.size(200.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.history_empty),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "Scan a QR, log cash, or add income to get started.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                for (group in state.groups) {
                    item(key = "header-${group.date}") {
                        DayHeader(group)
                    }
                    items(group.items, key = { it.id }) { item ->
                        when (item) {
                            is HistoryItem.ExpenseItem -> ExpenseRow(
                                expense = item.expense,
                                onClick = { onOpenExpense(item.expense.id) },
                            )
                            is HistoryItem.IncomeItem -> IncomeRow(income = item.income)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayHeader(group: DayGroup) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            group.label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (group.incomeTotalMinor > 0L) {
                Text(
                    "+${formatInr(group.incomeTotalMinor)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = incomeColor,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.size(10.dp))
            }
            if (group.expenseTotalMinor > 0L) {
                Text(
                    formatInr(group.expenseTotalMinor),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterRow(
    filter: HistoryFilter,
    dateRange: DateRange?,
    onSelect: (HistoryFilter) -> Unit,
    onSetRange: (Long, Long) -> Unit,
    onClearRange: () -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = filter == HistoryFilter.ALL,
            onClick = { onSelect(HistoryFilter.ALL) },
            label = { Text("All") },
        )
        FilterChip(
            selected = filter == HistoryFilter.EXPENSES,
            onClick = { onSelect(HistoryFilter.EXPENSES) },
            label = { Text("Expenses") },
        )
        FilterChip(
            selected = filter == HistoryFilter.INCOME,
            onClick = { onSelect(HistoryFilter.INCOME) },
            label = { Text("Income") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = incomeColor.copy(alpha = 0.18f),
                selectedLabelColor = incomeColor,
            ),
        )
        val dateLabel = dateRange?.let {
            "${formatDayDdMmm(it.startMs)} → ${formatDayDdMmm(it.endMs)}"
        } ?: "Date"
        FilterChip(
            selected = dateRange != null,
            onClick = {
                if (dateRange != null) onClearRange() else showPicker = true
            },
            label = { Text(dateLabel) },
        )
    }

    if (showPicker) {
        val zone = ZoneId.systemDefault()
        val initialStart = dateRange?.startMs?.toUtcMidnight(zone)
        val initialEnd = dateRange?.endMs?.toUtcMidnight(zone)
        val pickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = initialStart,
            initialSelectedEndDateMillis = initialEnd,
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    enabled = pickerState.selectedStartDateMillis != null &&
                        pickerState.selectedEndDateMillis != null,
                    onClick = {
                        val startUtc = pickerState.selectedStartDateMillis
                        val endUtc = pickerState.selectedEndDateMillis
                        if (startUtc != null && endUtc != null) {
                            val startDay = utcMillisToLocalDate(startUtc)
                            val endDay = utcMillisToLocalDate(endUtc)
                            val startMs = startDay.atStartOfDay(zone).toInstant().toEpochMilli()
                            val endMs = endDay.plusDays(1).atStartOfDay(zone)
                                .toInstant().toEpochMilli() - 1
                            onSetRange(startMs, endMs)
                        }
                        showPicker = false
                    }
                ) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            },
        ) {
            DateRangePicker(state = pickerState)
        }
    }
}

private fun Long.toUtcMidnight(zone: ZoneId): Long {
    val date = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()
    return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

private fun utcMillisToLocalDate(utcMs: Long): LocalDate =
    Instant.ofEpochMilli(utcMs).atZone(ZoneOffset.UTC).toLocalDate()

@Composable
private fun TopSummary(
    todayTotalMinor: Long,
    todayIncomeMinor: Long,
    pendingCount: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Text(
            "Today",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                formatInr(todayTotalMinor),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f),
            )
            if (todayIncomeMinor > 0L) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Income",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    )
                    Text(
                        "+${formatInr(todayIncomeMinor)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = incomeColor,
                    )
                }
            }
        }
        if (pendingCount > 0) {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.history_pending_pill, pendingCount),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                        RoundedCornerShape(100),
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun ExpenseRow(expense: Expense, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(expense.category.emoji, style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = expense.merchantName
                    ?: expense.payee?.displayName
                    ?: expense.payee?.vpa
                    ?: expense.category.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MethodPill(expense.paymentMethod)
                Spacer(Modifier.size(6.dp))
                Text(
                    text = buildString {
                        append(expense.category.name)
                        if (!expense.note.isNullOrBlank()) append(" \u00b7 ${expense.note}")
                        if (expense.status != PaymentStatus.COMPLETED) {
                            append(" \u00b7 ")
                            append(expense.status.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                formatInr(expense.amountMinor),
                style = MaterialTheme.typography.titleMedium,
                color = statusColor(expense.status),
            )
            if (expense.status == PaymentStatus.PENDING) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(MaterialTheme.colorScheme.tertiary, CircleShape),
                )
            }
        }
    }
}

@Composable
private fun IncomeRow(income: Income) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(incomeColor.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("\u20B9", style = MaterialTheme.typography.titleLarge, color = incomeColor)
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                income.source,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(incomeColor.copy(alpha = 0.18f), RoundedCornerShape(100))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        "Income",
                        style = MaterialTheme.typography.labelSmall,
                        color = incomeColor,
                    )
                }
                if (!income.description.isNullOrBlank()) {
                    Spacer(Modifier.size(6.dp))
                    Text(
                        income.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Text(
            "+${formatInr(income.amountMinor)}",
            style = MaterialTheme.typography.titleMedium,
            color = incomeColor,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun MethodPill(method: PaymentMethod) {
    val (bg, fg) = when (method) {
        PaymentMethod.UPI -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        PaymentMethod.CASH -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        PaymentMethod.CARD -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        PaymentMethod.OTHER -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(100))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            method.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
        )
    }
}

@Composable
private fun statusColor(status: PaymentStatus): Color = when (status) {
    PaymentStatus.COMPLETED -> MaterialTheme.colorScheme.onSurface
    PaymentStatus.PENDING -> MaterialTheme.colorScheme.tertiary
    PaymentStatus.FAILED, PaymentStatus.CANCELLED, PaymentStatus.EXPIRED ->
        MaterialTheme.colorScheme.onSurfaceVariant
}
