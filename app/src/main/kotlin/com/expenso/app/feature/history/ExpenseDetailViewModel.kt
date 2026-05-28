package com.expenso.app.feature.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenso.app.core.data.repository.CategoryRepository
import com.expenso.app.core.data.repository.ExpenseRepository
import com.expenso.app.core.domain.model.Category
import com.expenso.app.core.domain.model.Expense
import com.expenso.app.core.domain.model.PaymentMethod
import com.expenso.app.core.domain.model.PaymentStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExpenseDetailUiState(
    val expense: Expense? = null,
    val categories: List<Category> = emptyList(),
    val editMode: Boolean = false,
    val amountRupeesInput: String = "",
    val noteInput: String = "",
    val merchantInput: String = "",
    val selectedCategoryId: String? = null,
    val selectedMethod: PaymentMethod = PaymentMethod.UPI,
    val deleted: Boolean = false,
)

@HiltViewModel
class ExpenseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private val expenseId: String = savedStateHandle.get<String>("expenseId").orEmpty()

    private val _state = MutableStateFlow(ExpenseDetailUiState())
    val state: StateFlow<ExpenseDetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val expense = expenseRepository.findById(expenseId) ?: return@launch
            _state.update {
                it.copy(
                    expense = expense,
                    selectedCategoryId = expense.category.id,
                    selectedMethod = expense.paymentMethod,
                    amountRupeesInput = "%.2f".format(expense.amountMinor / 100.0),
                    noteInput = expense.note.orEmpty(),
                    merchantInput = expense.merchantName.orEmpty(),
                )
            }
        }
        viewModelScope.launch {
            categoryRepository.observeActive().collect { list ->
                _state.update { it.copy(categories = list) }
            }
        }
    }

    fun setEditMode(on: Boolean) = _state.update { it.copy(editMode = on) }

    fun setAmount(v: String) = _state.update {
        it.copy(amountRupeesInput = v.filter { c -> c.isDigit() || c == '.' })
    }
    fun setNote(v: String) = _state.update { it.copy(noteInput = v.take(80)) }
    fun setCategory(id: String) = _state.update { it.copy(selectedCategoryId = id) }
    fun setMerchant(v: String) = _state.update { it.copy(merchantInput = v.take(60)) }
    fun setMethod(m: PaymentMethod) = _state.update { it.copy(selectedMethod = m) }

    fun save() {
        val cur = _state.value.expense ?: return
        val catId = _state.value.selectedCategoryId ?: cur.category.id
        val rupees = _state.value.amountRupeesInput.toBigDecimalOrNull() ?: return
        val updated = cur.copy(
            amountMinor = rupees.multiply(BigDecimal(100)).toLong(),
            note = _state.value.noteInput.ifBlank { null },
            merchantName = _state.value.merchantInput.ifBlank { null },
            paymentMethod = _state.value.selectedMethod,
            category = _state.value.categories.firstOrNull { it.id == catId } ?: cur.category,
        )
        viewModelScope.launch {
            expenseRepository.updateExpense(updated)
            _state.update { it.copy(editMode = false, expense = updated) }
        }
    }

    fun markStatus(status: PaymentStatus) {
        val cur = _state.value.expense ?: return
        viewModelScope.launch {
            val now = if (status == PaymentStatus.COMPLETED) System.currentTimeMillis() else null
            expenseRepository.markStatus(cur.id, status, now)
            _state.update { it.copy(expense = cur.copy(status = status, completedAt = now)) }
        }
    }

    fun delete() {
        val cur = _state.value.expense ?: return
        viewModelScope.launch {
            expenseRepository.softDelete(cur.id, System.currentTimeMillis())
            _state.update { it.copy(deleted = true) }
        }
    }
}
