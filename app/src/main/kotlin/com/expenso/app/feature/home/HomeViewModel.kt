package com.expenso.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenso.app.core.data.prefs.ExpensoPrefs
import com.expenso.app.core.data.repository.CategoryRepository
import com.expenso.app.core.data.repository.ExpenseRepository
import com.expenso.app.core.domain.model.Category
import com.expenso.app.core.domain.model.Expense
import com.expenso.app.core.domain.model.PaymentStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val categories: List<Category> = emptyList(),
    val lastUsedCategoryId: String? = null,
    val todayTotalMinor: Long = 0L,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    categoryRepository: CategoryRepository,
    expenseRepository: ExpenseRepository,
    prefs: ExpensoPrefs,
) : ViewModel() {

    val state: StateFlow<HomeUiState> = combine(
        categoryRepository.observeActive(),
        prefs.lastUsedCategoryId,
        expenseRepository.observeAll(),
    ) { cats, last, expenses ->
        HomeUiState(
            categories = cats,
            lastUsedCategoryId = last,
            todayTotalMinor = todayTotal(expenses),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private fun todayTotal(expenses: List<Expense>): Long {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        return expenses
            .filter {
                it.status == PaymentStatus.COMPLETED &&
                    Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate() == today
            }
            .sumOf { it.amountMinor }
    }
}
