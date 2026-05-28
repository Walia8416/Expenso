package com.expenso.app.feature.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenso.app.core.data.prefs.ExpensoPrefs
import com.expenso.app.core.data.repository.CategoryRepository
import com.expenso.app.core.domain.model.Category
import com.expenso.app.core.domain.model.Expense
import com.expenso.app.core.domain.upi.UpiParseResult
import com.expenso.app.core.domain.upi.UpiPaymentRequest
import com.expenso.app.core.domain.upi.UpiUriParser
import com.expenso.app.core.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

data class ScannerUiState(
    val categories: List<Category> = emptyList(),
    val lastUsedCategoryId: String? = null,
    val pendingRequest: UpiPaymentRequest? = null,
    val pendingExpenseId: String? = null,
    val awaitingConfirmFor: Expense? = null,
    val error: String? = null,
    val scanning: Boolean = true,
    val showAddSheet: Boolean = false,
    val addSheetTabIndex: Int = 0,
    /** Single-screen fast-path logger opened from the scanner's quick FAB. */
    val showQuickLog: Boolean = false,
    /** Shown once, after the first confirmed UPI payment. */
    val showDefaultUpiPrompt: Boolean = false,
)

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val expenseRepository: ExpenseRepository,
    private val prefs: ExpensoPrefs,
) : ViewModel() {

    private val _state = MutableStateFlow(ScannerUiState())
    val state: StateFlow<ScannerUiState> = _state.asStateFlow()

    val categories: StateFlow<List<Category>> = categoryRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            combine(categories, prefs.lastUsedCategoryId) { cats, last ->
                cats to last
            }.collect { (cats, last) ->
                _state.update { it.copy(categories = cats, lastUsedCategoryId = last) }
            }
        }
        // Hydrate a pending expense id that the UpiMediatorActivity may have
        // stashed in prefs while the user was in the PSP app. Lets the
        // ConfirmPaymentSheet fire the next time Expenso is opened.
        viewModelScope.launch {
            prefs.pendingExpenseId.collect { id ->
                if (id != null && _state.value.pendingExpenseId == null) {
                    _state.update { it.copy(pendingExpenseId = id) }
                    checkForPendingExpense()
                }
            }
        }
        checkForPendingExpense()
    }

    fun onQrDetected(raw: String) {
        if (!_state.value.scanning) return
        when (val parsed = UpiUriParser.parse(raw)) {
            is UpiParseResult.Success -> {
                _state.update {
                    it.copy(
                        pendingRequest = parsed.request,
                        scanning = false,
                        error = null,
                    )
                }
            }
            is UpiParseResult.Failure -> {
                Timber.d("Ignored non-UPI QR: ${parsed.reason}")
            }
        }
    }

    /**
     * Invoked when another app hands us a `upi://pay` intent. Acts as a UPI
     * "mediator": we parse the URI, then let the user pick one of their
     * installed bank-backed UPI apps from the PaySheet.
     */
    fun onUpiIntentReceived(raw: String) {
        when (val parsed = UpiUriParser.parse(raw)) {
            is UpiParseResult.Success -> {
                _state.update {
                    it.copy(
                        pendingRequest = parsed.request,
                        scanning = false,
                        showAddSheet = false,
                        error = null,
                    )
                }
            }
            is UpiParseResult.Failure -> {
                Timber.w("Rejected incoming UPI intent: ${parsed.reason}")
                _state.update { it.copy(error = "Couldn't read that UPI request") }
            }
        }
    }

    fun onManualEntry() {
        _state.update {
            it.copy(
                pendingRequest = UpiPaymentRequest(
                    payeeVpa = "",
                    payeeName = null,
                    amountRupees = null,
                    currency = "INR",
                    transactionNote = null,
                    transactionRef = null,
                    merchantCode = null,
                    url = null,
                    isSigned = false,
                    rawParams = emptyMap(),
                ),
                scanning = false,
                showAddSheet = false,
            )
        }
    }

    fun openAddSheet(tabIndex: Int = 0) {
        _state.update { it.copy(showAddSheet = true, addSheetTabIndex = tabIndex, scanning = false) }
    }

    fun selectAddSheetTab(index: Int) {
        _state.update { it.copy(addSheetTabIndex = index) }
    }

    fun dismissAddSheet() {
        _state.update { it.copy(showAddSheet = false, scanning = true) }
    }

    fun openQuickLog() {
        _state.update { it.copy(showQuickLog = true, scanning = false) }
    }

    fun dismissQuickLog() {
        _state.update { it.copy(showQuickLog = false, scanning = true) }
    }

    fun dismissPaySheet() {
        _state.update { it.copy(pendingRequest = null, scanning = true) }
    }

    fun setPendingExpenseId(id: String) {
        _state.update { it.copy(pendingExpenseId = id, pendingRequest = null, scanning = true) }
    }

    /** Called from UI on screen resume. */
    fun checkForPendingExpense() {
        viewModelScope.launch {
            val pendingId = _state.value.pendingExpenseId ?: return@launch
            val expense = expenseRepository.findById(pendingId) ?: return@launch
            if (expense.status == com.expenso.app.core.domain.model.PaymentStatus.PENDING) {
                _state.update { it.copy(awaitingConfirmFor = expense) }
            } else {
                _state.update { it.copy(awaitingConfirmFor = null, pendingExpenseId = null) }
            }
        }
    }

    fun confirmStatus(status: com.expenso.app.core.domain.model.PaymentStatus) {
        val expense = _state.value.awaitingConfirmFor ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            expenseRepository.markStatus(
                expenseId = expense.id,
                status = status,
                completedAt = if (status == com.expenso.app.core.domain.model.PaymentStatus.COMPLETED) now else null,
            )
            prefs.setPendingExpenseId(null)
            val shouldPrompt = status == com.expenso.app.core.domain.model.PaymentStatus.COMPLETED &&
                !prefs.defaultUpiPromptShown.first()
            _state.update {
                it.copy(
                    awaitingConfirmFor = null,
                    pendingExpenseId = null,
                    showDefaultUpiPrompt = shouldPrompt,
                )
            }
        }
    }

    fun dismissConfirm() {
        _state.update { it.copy(awaitingConfirmFor = null) }
    }

    fun dismissDefaultUpiPrompt() {
        viewModelScope.launch {
            prefs.setDefaultUpiPromptShown(true)
            _state.update { it.copy(showDefaultUpiPrompt = false) }
        }
    }
}
