package com.expenso.app.feature.pay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenso.app.core.data.prefs.ExpensoPrefs
import com.expenso.app.core.data.repository.ExpenseRepository
import com.expenso.app.core.data.repository.PayeeRepository
import com.expenso.app.core.domain.model.ExpenseSource
import com.expenso.app.core.domain.model.InstalledUpiApp
import com.expenso.app.core.domain.upi.UpiAppDiscovery
import com.expenso.app.core.domain.upi.UpiIntentBuilder
import com.expenso.app.core.domain.upi.UpiPaymentRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import android.net.Uri
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

data class PayUiState(
    val vpaInput: String = "",
    val payeeNameInput: String = "",
    val amountRupeesInput: String = "",
    val noteInput: String = "",
    val selectedCategoryId: String? = null,
    val installedUpiApps: List<InstalledUpiApp> = emptyList(),
    val chosenUpiPackage: String? = null,
    val isSignedQr: Boolean = false,
    val amountWasPrefilled: Boolean = false,
    val isVpaEditable: Boolean = false,
    val duplicateWarning: Boolean = false,
    val source: ExpenseSource = ExpenseSource.MANUAL,
    val upiUriHint: String? = null,
)

sealed interface PayEvent {
    data class LaunchUpi(
        val intentUri: String,
        val targetPackage: String?,
        val expenseId: String,
        val qrSource: UpiIntentBuilder.QrSource? = null,
    ) : PayEvent

    data class Error(val message: String) : PayEvent
}

@HiltViewModel
class PayViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val payeeRepository: PayeeRepository,
    private val prefs: ExpensoPrefs,
    private val upiDiscovery: UpiAppDiscovery,
) : ViewModel() {

    private val _state = MutableStateFlow(PayUiState())
    val state: StateFlow<PayUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<PayEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<PayEvent> = _events.asSharedFlow()

    private var originalRequest: UpiPaymentRequest? = null

    fun bindRequest(req: UpiPaymentRequest, defaultCategoryId: String?) {
        originalRequest = req
        viewModelScope.launch {
            val installed = upiDiscovery.installedApps()
            val defaultPkg = prefs.defaultUpiPackage.first()
            val resolvedPkg = defaultPkg?.takeIf { pkg -> installed.any { it.packageName == pkg } }
                ?: installed.firstOrNull()?.packageName

            val isManual = req.payeeVpa.isBlank()
            _state.update {
                it.copy(
                    vpaInput = req.payeeVpa,
                    payeeNameInput = req.payeeName.orEmpty(),
                    amountRupeesInput = req.amountRupees?.stripTrailingZeros()?.toPlainString().orEmpty(),
                    noteInput = req.transactionNote.orEmpty(),
                    selectedCategoryId = defaultCategoryId,
                    installedUpiApps = installed,
                    chosenUpiPackage = resolvedPkg,
                    isSignedQr = req.isSigned,
                    amountWasPrefilled = req.amountRupees != null,
                    isVpaEditable = isManual,
                    source = if (isManual) ExpenseSource.MANUAL else ExpenseSource.QR_SCAN,
                )
            }
        }
    }

    fun setVpa(value: String) = _state.update { it.copy(vpaInput = value.trim()) }
    fun setPayeeName(value: String) = _state.update { it.copy(payeeNameInput = value) }
    fun setAmount(value: String) {
        val sanitized = value.filter { it.isDigit() || it == '.' }
        _state.update { it.copy(amountRupeesInput = sanitized) }
        checkDuplicate()
    }
    fun setNote(value: String) = _state.update { it.copy(noteInput = value.take(50)) }
    fun selectCategory(id: String) = _state.update { it.copy(selectedCategoryId = id) }
    fun chooseUpiApp(pkg: String) = _state.update { it.copy(chosenUpiPackage = pkg) }

    private fun checkDuplicate() {
        val st = _state.value
        val rupees = st.amountRupeesInput.toBigDecimalOrNull() ?: return
        if (rupees.signum() <= 0) return
        val amountMinor = rupees.multiply(BigDecimal(100)).toLong()
        val vpa = st.vpaInput.ifBlank { return }
        viewModelScope.launch {
            val payee = payeeRepository.findByVpa(vpa) ?: return@launch
            val since = System.currentTimeMillis() - 5 * 60 * 1000L
            val n = expenseRepository.countPotentialDuplicates(payee.id, amountMinor, since)
            _state.update { it.copy(duplicateWarning = n > 0) }
        }
    }

    fun onPayClicked() {
        val st = _state.value
        val amountRupees = st.amountRupeesInput.toBigDecimalOrNull()
        if (amountRupees == null || amountRupees.signum() <= 0) {
            viewModelScope.launch { _events.emit(PayEvent.Error("Enter a valid amount")) }
            return
        }
        if (amountRupees > BigDecimal("100000")) {
            viewModelScope.launch { _events.emit(PayEvent.Error("Amount exceeds UPI per-txn cap of ₹1,00,000")) }
            return
        }
        val vpa = st.vpaInput.ifBlank {
            viewModelScope.launch { _events.emit(PayEvent.Error("Enter a VPA or phone@upi")) }
            return
        }
        val categoryId = st.selectedCategoryId ?: run {
            viewModelScope.launch { _events.emit(PayEvent.Error("Pick a category")) }
            return
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val amountMinor = amountRupees.multiply(BigDecimal(100)).toLong()
            val payee = payeeRepository.upsertForPayment(
                vpa = vpa,
                parsedName = st.payeeNameInput.ifBlank { originalRequest?.payeeName },
                merchantCode = originalRequest?.merchantCode,
                now = now,
            )

            // Internal session id for correlating with local expense row. Kept
            // separate from the on-wire `tr` value, which must be alphanumeric.
            val sessionId = UUID.randomUUID().toString()
            val expenseId = expenseRepository.createPendingExpense(
                amountMinor = amountMinor,
                categoryId = categoryId,
                payeeId = payee.id,
                note = st.noteInput,
                source = st.source,
                intentSessionId = sessionId,
                now = now,
            )

            val original = originalRequest
            val signedRaw = original?.sourceRawUri?.takeIf { st.isSignedQr }
            val uri = if (signedRaw != null) {
                // Signed merchant QR. Never decode and rebuild — that breaks the
                // merchant signature (Uri.Builder re-encodes `+`/%20, hex case,
                // reserved chars). Launch the source bytes verbatim. If the
                // original QR had no `am` (variable-amount sticker) and the
                // user entered one, append it as a raw string suffix so signed
                // bytes stay intact.
                if (original.amountRupees == null) {
                    Uri.parse(
                        UpiIntentBuilder.appendAmountToRawUri(
                            sourceRawUri = signedRaw,
                            amountRupees = amountRupees,
                            currency = original.currency,
                        )
                    )
                } else {
                    Uri.parse(signedRaw)
                }
            } else if (shouldLaunchOriginalUri(st, original, amountRupees)) {
                Uri.parse(original!!.sourceRawUri)
            } else {
                val preservedTr = original?.transactionRef
                    ?.filter { it.isLetterOrDigit() }
                    ?.takeIf { it.isNotBlank() && it.length <= 35 }
                UpiIntentBuilder.buildUri(
                    payeeVpa = vpa,
                    payeeName = st.payeeNameInput.ifBlank { original?.payeeName },
                    amountRupees = amountRupees,
                    note = st.noteInput,
                    transactionRef = preservedTr ?: UpiIntentBuilder.generateTxnRef(),
                    extraParams = original?.rawParams?.filterKeys {
                        it !in setOf("pa", "pn", "am", "cu", "tn", "tr")
                    } ?: emptyMap(),
                )
            }

            expenseRepository.insertPaymentIntent(
                id = sessionId,
                upiUri = uri.toString(),
                targetPackage = st.chosenUpiPackage,
                launchedAt = now,
            )

            prefs.setLastUsedCategoryId(categoryId)
            st.chosenUpiPackage?.let { prefs.setDefaultUpiPackage(it) }

            if (!original?.sourceRawUri.isNullOrBlank()) {
                val source = redactForLog(original?.sourceRawUri.orEmpty())
                val outgoing = redactForLog(uri.toString())
                val bytesEqual = original?.sourceRawUri == uri.toString()
                Timber.d(
                    "UPI source=%s outgoing=%s signed=%b bytes_equal=%b",
                    source,
                    outgoing,
                    original?.isSigned == true,
                    bytesEqual,
                )
            }
            Timber.d("Launching UPI: $uri target=${st.chosenUpiPackage}")
            val qrSource = when (st.source) {
                ExpenseSource.QR_SCAN -> UpiIntentBuilder.QrSource.LIVE_CAMERA
                else -> null
            }
            _events.emit(
                PayEvent.LaunchUpi(
                    intentUri = uri.toString(),
                    targetPackage = st.chosenUpiPackage,
                    expenseId = expenseId,
                    qrSource = qrSource,
                )
            )
        }
    }

    private fun shouldLaunchOriginalUri(
        state: PayUiState,
        original: UpiPaymentRequest?,
        amountRupees: BigDecimal,
    ): Boolean {
        if (original == null || original.isSigned) return false
        val source = original.sourceRawUri ?: return false
        if (source.isBlank()) return false
        val sameVpa = state.vpaInput == original.payeeVpa
        val sameName = state.payeeNameInput == original.payeeName.orEmpty()
        val sameAmount = original.amountRupees?.compareTo(amountRupees) == 0
        val sameNote = state.noteInput == original.transactionNote.orEmpty()
        return sameVpa && sameName && sameAmount && sameNote
    }

    private fun redactForLog(raw: String): String {
        val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return raw
        val pa = uri.getQueryParameter("pa").orEmpty()
        val tr = uri.getQueryParameter("tr").orEmpty()
        return "upi://${uri.host.orEmpty()}?pa=${maskMiddle(pa)}&tr=${maskMiddle(tr)}"
    }

    private fun maskMiddle(value: String): String {
        if (value.length <= 4) return "****"
        return value.take(2) + "****" + value.takeLast(2)
    }
}
