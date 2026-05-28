package com.expenso.app.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenso.app.core.data.repository.ExpenseRepository
import com.expenso.app.core.data.repository.IncomeRepository
import com.expenso.app.core.domain.model.Expense
import com.expenso.app.core.domain.model.Income
import com.expenso.app.core.domain.model.PaymentStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HistoryFilter { ALL, EXPENSES, INCOME }

sealed interface HistoryItem {
    val id: String
    val createdAt: Long

    data class ExpenseItem(val expense: Expense) : HistoryItem {
        override val id: String = "e-${expense.id}"
        override val createdAt: Long = expense.createdAt
    }

    data class IncomeItem(val income: Income) : HistoryItem {
        override val id: String = "i-${income.id}"
        override val createdAt: Long = income.createdAt
    }
}

data class HistoryUiState(
    val filter: HistoryFilter = HistoryFilter.ALL,
    val groups: List<DayGroup> = emptyList(),
    val pendingCount: Int = 0,
    val todayTotalMinor: Long = 0L,
    val todayIncomeMinor: Long = 0L,
)

data class DayGroup(
    val date: LocalDate,
    val label: String,
    val expenseTotalMinor: Long,
    val incomeTotalMinor: Long,
    val items: List<HistoryItem>,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
) : ViewModel() {

    private val _filter = MutableStateFlow(HistoryFilter.ALL)

    val state: StateFlow<HistoryUiState> = combine(
        expenseRepository.observeAll(),
        expenseRepository.observePending(),
        incomeRepository.observeAll(),
        _filter,
    ) { expenses, pending, incomes, filter ->
        HistoryUiState(
            filter = filter,
            groups = groupByDay(expenses, incomes, filter),
            pendingCount = pending.size,
            todayTotalMinor = todayTotal(expenses),
            todayIncomeMinor = todayIncome(incomes),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun setFilter(filter: HistoryFilter) {
        _filter.value = filter
    }

    fun softDelete(id: String) {
        viewModelScope.launch {
            expenseRepository.softDelete(id, System.currentTimeMillis())
        }
    }

    fun undoSoftDelete(id: String) {
        viewModelScope.launch {
            expenseRepository.undoSoftDelete(id)
        }
    }

    fun softDeleteIncome(id: String) {
        viewModelScope.launch {
            incomeRepository.softDelete(id, System.currentTimeMillis())
        }
    }

    private fun groupByDay(
        expenses: List<Expense>,
        incomes: List<Income>,
        filter: HistoryFilter,
    ): List<DayGroup> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)

        val filteredExpenses = if (filter == HistoryFilter.INCOME) emptyList() else expenses
        val filteredIncomes = if (filter == HistoryFilter.EXPENSES) emptyList() else incomes

        val items = filteredExpenses.map { HistoryItem.ExpenseItem(it) as HistoryItem } +
            filteredIncomes.map { HistoryItem.IncomeItem(it) as HistoryItem }

        if (items.isEmpty()) return emptyList()

        return items
            .groupBy { Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate() }
            .entries
            .sortedByDescending { it.key }
            .map { (date, dayItems) ->
                val expTotal = dayItems.filterIsInstance<HistoryItem.ExpenseItem>()
                    .filter {
                        it.expense.status == PaymentStatus.COMPLETED ||
                            it.expense.status == PaymentStatus.PENDING
                    }
                    .sumOf { it.expense.amountMinor }
                val incTotal = dayItems.filterIsInstance<HistoryItem.IncomeItem>()
                    .sumOf { it.income.amountMinor }
                DayGroup(
                    date = date,
                    label = dayLabel(date, today),
                    expenseTotalMinor = expTotal,
                    incomeTotalMinor = incTotal,
                    items = dayItems.sortedByDescending { it.createdAt },
                )
            }
    }

    private fun todayTotal(items: List<Expense>): Long {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        return items
            .filter {
                it.status == PaymentStatus.COMPLETED &&
                    Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate() == today
            }
            .sumOf { it.amountMinor }
    }

    private fun todayIncome(items: List<Income>): Long {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        return items
            .filter { Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate() == today }
            .sumOf { it.amountMinor }
    }

    private fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.toString()
    }
}
