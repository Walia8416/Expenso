package com.expenso.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenso.app.core.data.prefs.ExpensoPrefs
import com.expenso.app.core.domain.model.InstalledUpiApp
import com.expenso.app.core.domain.upi.UpiAppDiscovery
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val page: Int = 0,
    val installedUpiApps: List<InstalledUpiApp> = emptyList(),
    val selectedUpiPackage: String? = null,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: ExpensoPrefs,
    private val upiDiscovery: UpiAppDiscovery,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun refreshInstalledApps() {
        val apps = upiDiscovery.installedApps()
        _state.update { it.copy(installedUpiApps = apps) }
    }

    fun goToPage(page: Int) {
        _state.update { it.copy(page = page) }
    }

    fun selectUpiApp(pkg: String) {
        _state.update { it.copy(selectedUpiPackage = pkg) }
    }

    fun saveDefaultUpiApp() {
        val pkg = _state.value.selectedUpiPackage ?: return
        viewModelScope.launch {
            prefs.setDefaultUpiPackage(pkg)
        }
    }
}
