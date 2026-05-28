package com.expenso.app.feature.upi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenso.app.core.data.prefs.ExpensoPrefs
import com.expenso.app.core.data.repository.CategoryRepository
import com.expenso.app.core.domain.model.Category
import com.expenso.app.core.domain.upi.UpiParseResult
import com.expenso.app.core.domain.upi.UpiPaymentRequest
import com.expenso.app.core.domain.upi.UpiUriParser
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Minimal VM for [UpiMediatorActivity]. Owns just enough state to render
 * [com.expenso.app.feature.pay.PaySheet]: the parsed request, the category
 * list for the picker, and the remembered "last used category" default.
 *
 * The heavy lifting (building the outbound `upi://pay?...` URI, writing the
 * pending expense and `payment_intent` rows, emitting `PayEvent.LaunchUpi`)
 * stays inside `PayViewModel` so the scanner and mediator paths stay in sync.
 */
data class UpiMediatorUiState(
    val categories: List<Category> = emptyList(),
    val lastUsedCategoryId: String? = null,
    val request: UpiPaymentRequest? = null,
    val parseError: String? = null,
)

@HiltViewModel
class UpiMediatorViewModel @Inject constructor(
    categoryRepository: CategoryRepository,
    private val prefs: ExpensoPrefs,
) : ViewModel() {

    private val _state = MutableStateFlow(UpiMediatorUiState())
    val state: StateFlow<UpiMediatorUiState> = _state.asStateFlow()

    private val categories: StateFlow<List<Category>> = categoryRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            categories.collect { list ->
                _state.update { it.copy(categories = list) }
            }
        }
        viewModelScope.launch {
            prefs.lastUsedCategoryId.collect { id ->
                _state.update { it.copy(lastUsedCategoryId = id) }
            }
        }
    }

    fun parseIncoming(raw: String?) {
        if (raw.isNullOrBlank()) {
            _state.update { it.copy(request = null, parseError = "Empty UPI request") }
            return
        }
        when (val parsed = UpiUriParser.parse(raw)) {
            is UpiParseResult.Success -> {
                _state.update { it.copy(request = parsed.request, parseError = null) }
            }
            is UpiParseResult.Failure -> {
                Timber.w("Mediator rejected UPI request: ${parsed.reason}")
                _state.update { it.copy(request = null, parseError = parsed.reason) }
            }
        }
    }

    /**
     * Fire-and-forget on `viewModelScope` so the write survives the
     * mediator activity calling `finish()` immediately after the PSP hand-off.
     */
    fun rememberPendingExpense(expenseId: String) {
        viewModelScope.launch {
            prefs.setPendingExpenseId(expenseId)
        }
    }
}
