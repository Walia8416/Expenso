package com.expenso.app.feature.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenso.app.core.data.repository.IncomeRepository
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

data class AddIncomeUiState(
    val amountRupeesInput: String = "",
    val sourceInput: String = "",
    val descriptionInput: String = "",
    val noteInput: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val recentSources: List<String> = emptyList(),
)

sealed interface AddIncomeEvent {
    data object Saved : AddIncomeEvent
    data class Error(val message: String) : AddIncomeEvent
}

@HiltViewModel
class AddIncomeViewModel @Inject constructor(
    private val incomeRepository: IncomeRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AddIncomeUiState())
    val state: StateFlow<AddIncomeUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<AddIncomeEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<AddIncomeEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val recents = incomeRepository.recentSources()
            _state.update { it.copy(recentSources = recents) }
        }
    }

    fun setAmount(value: String) {
        val sanitized = value.filter { it.isDigit() || it == '.' }
        _state.update { it.copy(amountRupeesInput = sanitized) }
    }

    fun setSource(value: String) = _state.update { it.copy(sourceInput = value.take(40)) }
    fun pickSource(value: String) = _state.update { it.copy(sourceInput = value) }
    fun setDescription(value: String) = _state.update { it.copy(descriptionInput = value.take(80)) }
    fun setNote(value: String) = _state.update { it.copy(noteInput = value.take(120)) }
    fun setCreatedAt(epochMs: Long) = _state.update { it.copy(createdAt = epochMs) }

    fun save() {
        val st = _state.value
        val amount = st.amountRupeesInput.toBigDecimalOrNull()
        if (amount == null || amount.signum() <= 0) {
            viewModelScope.launch { _events.emit(AddIncomeEvent.Error("Enter a valid amount")) }
            return
        }
        if (st.sourceInput.isBlank()) {
            viewModelScope.launch { _events.emit(AddIncomeEvent.Error("Enter a source (e.g. Salary)")) }
            return
        }
        viewModelScope.launch {
            val minor = amount.multiply(BigDecimal(100)).toLong()
            incomeRepository.add(
                amountMinor = minor,
                source = st.sourceInput,
                description = st.descriptionInput,
                note = st.noteInput,
                createdAt = st.createdAt,
            )
            _events.emit(AddIncomeEvent.Saved)
        }
    }

    fun reset() {
        _state.value = AddIncomeUiState(recentSources = _state.value.recentSources)
    }
}
