package com.expenso.app.feature.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenso.app.core.data.prefs.ExpensoPrefs
import com.expenso.app.core.data.repository.ExpenseRepository
import com.expenso.app.core.domain.model.PaymentMethod
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LogExpenseUiState(
    val amountRupeesInput: String = "",
    val merchantNameInput: String = "",
    val noteInput: String = "",
    val selectedCategoryId: String? = null,
    val method: PaymentMethod = PaymentMethod.UPI,
    val createdAt: Long = System.currentTimeMillis(),
)

sealed interface LogExpenseEvent {
    data object Saved : LogExpenseEvent
    data class Error(val message: String) : LogExpenseEvent
}

@HiltViewModel
class LogExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val prefs: ExpensoPrefs,
) : ViewModel() {

    private val _state = MutableStateFlow(LogExpenseUiState())
    val state: StateFlow<LogExpenseUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<LogExpenseEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<LogExpenseEvent> = _events.asSharedFlow()

    fun initializeDefault(defaultCategoryId: String?) {
        _state.update {
            if (it.selectedCategoryId == null) it.copy(selectedCategoryId = defaultCategoryId) else it
        }
    }

    fun setAmount(value: String) {
        val sanitized = value.filter { it.isDigit() || it == '.' }
        _state.update { it.copy(amountRupeesInput = sanitized) }
    }

    fun setMerchant(value: String) = _state.update { it.copy(merchantNameInput = value.take(60)) }
    fun setNote(value: String) = _state.update { it.copy(noteInput = value.take(80)) }
    fun selectCategory(id: String) = _state.update { it.copy(selectedCategoryId = id) }
    fun setMethod(m: PaymentMethod) = _state.update { it.copy(method = m) }
    fun setCreatedAt(epochMs: Long) = _state.update { it.copy(createdAt = epochMs) }

    fun save() {
        val st = _state.value
        val amount = st.amountRupeesInput.toBigDecimalOrNull()
        if (amount == null || amount.signum() <= 0) {
            viewModelScope.launch { _events.emit(LogExpenseEvent.Error("Enter a valid amount")) }
            return
        }
        val categoryId = st.selectedCategoryId
        if (categoryId == null) {
            viewModelScope.launch { _events.emit(LogExpenseEvent.Error("Pick a category")) }
            return
        }
        viewModelScope.launch {
            val minor = amount.multiply(BigDecimal(100)).toLong()
            expenseRepository.createCompletedExpense(
                amountMinor = minor,
                categoryId = categoryId,
                paymentMethod = st.method,
                merchantName = st.merchantNameInput,
                note = st.noteInput,
                createdAt = st.createdAt,
            )
            prefs.setLastUsedCategoryId(categoryId)
            _events.emit(LogExpenseEvent.Saved)
        }
    }

    fun reset() {
        _state.value = LogExpenseUiState()
    }
}
