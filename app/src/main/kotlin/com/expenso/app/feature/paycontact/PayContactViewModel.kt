package com.expenso.app.feature.paycontact

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenso.app.core.data.repository.ContactsRepository
import com.expenso.app.core.data.repository.DeviceContact
import com.expenso.app.core.data.repository.PayeeRepository
import com.expenso.app.core.domain.model.Payee
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PayContactStep { Picker, Capture, Ready }

data class PayContactUiState(
    val step: PayContactStep = PayContactStep.Picker,
    val permissionGranted: Boolean = false,
    val loading: Boolean = false,
    val searchQuery: String = "",
    val contacts: List<DeviceContact> = emptyList(),
    val recentPayees: List<Payee> = emptyList(),
    val selectedContact: DeviceContact? = null,
    val vpaInput: String = "",
    val knownVpas: List<Payee> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class PayContactViewModel @Inject constructor(
    private val contactsRepository: ContactsRepository,
    private val payeeRepository: PayeeRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PayContactUiState())
    val state: StateFlow<PayContactUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun setPermissionGranted(granted: Boolean) {
        _state.update { it.copy(permissionGranted = granted) }
        if (granted) reload()
    }

    fun reload() {
        if (!_state.value.permissionGranted) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching {
                contactsRepository.loadContactsWithPhones(_state.value.searchQuery.ifBlank { null })
            }.onSuccess { list ->
                _state.update { it.copy(loading = false, contacts = list) }
            }.onFailure { t ->
                _state.update { it.copy(loading = false, error = t.message) }
            }
        }
        viewModelScope.launch {
            payeeRepository.observeRecentPeople(10).collect { recents ->
                _state.update { it.copy(recentPayees = recents) }
            }
        }
    }

    fun setSearchQuery(q: String) {
        _state.update { it.copy(searchQuery = q) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(200)
            if (_state.value.permissionGranted) {
                runCatching {
                    contactsRepository.loadContactsWithPhones(q.ifBlank { null })
                }.onSuccess { list ->
                    _state.update { it.copy(contacts = list) }
                }
            }
        }
    }

    fun pickContact(contact: DeviceContact) {
        viewModelScope.launch {
            val known = payeeRepository.findByContact(contact.lookupKey)
            val preferredVpa = known.firstOrNull()?.vpa.orEmpty()
            _state.update {
                it.copy(
                    selectedContact = contact,
                    knownVpas = known,
                    vpaInput = preferredVpa,
                    step = PayContactStep.Capture,
                )
            }
        }
    }

    fun pickRecentPayee(p: Payee) {
        _state.update {
            it.copy(
                selectedContact = DeviceContact(
                    lookupKey = p.contactLookupKey ?: p.id,
                    displayName = p.displayName,
                    phoneNumber = p.phoneNumber.orEmpty(),
                    normalizedPhone = p.phoneNumber.orEmpty(),
                    photoUri = null,
                ),
                vpaInput = p.vpa,
                knownVpas = listOf(p),
                step = PayContactStep.Capture,
            )
        }
    }

    fun setVpa(v: String) = _state.update { it.copy(vpaInput = v.trim()) }

    fun confirmVpa(): Boolean {
        val vpa = _state.value.vpaInput.trim()
        if (!isLikelyVpa(vpa)) {
            _state.update { it.copy(error = "Enter a valid VPA like 9876543210@upi") }
            return false
        }
        val contact = _state.value.selectedContact
        if (contact != null) {
            viewModelScope.launch {
                payeeRepository.upsertContactPayee(
                    vpa = vpa,
                    displayName = contact.displayName,
                    contactLookupKey = contact.lookupKey,
                    phoneNumber = contact.phoneNumber,
                    now = System.currentTimeMillis(),
                )
            }
        }
        _state.update { it.copy(step = PayContactStep.Ready, error = null) }
        return true
    }

    fun resetToPicker() {
        _state.update {
            it.copy(
                step = PayContactStep.Picker,
                selectedContact = null,
                vpaInput = "",
                knownVpas = emptyList(),
                error = null,
            )
        }
    }

    private fun isLikelyVpa(v: String): Boolean {
        if (v.length < 3) return false
        val at = v.indexOf('@')
        return at in 1..(v.length - 2)
    }
}
