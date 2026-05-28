package com.expenso.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenso.app.core.data.prefs.ExpensoPrefs
import com.expenso.app.core.data.repository.CategoryRepository
import com.expenso.app.core.domain.model.Category
import com.expenso.app.core.domain.model.InstalledUpiApp
import com.expenso.app.core.domain.model.LifestyleGroup
import com.expenso.app.core.domain.upi.UpiAppDiscovery
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val installedUpiApps: List<InstalledUpiApp> = emptyList(),
    val defaultUpiPackage: String? = null,
    val biometricEnabled: Boolean = false,
    val upiAssistEnabled: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: ExpensoPrefs,
    private val upiDiscovery: UpiAppDiscovery,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private val _apps = MutableStateFlow<List<InstalledUpiApp>>(emptyList())

    val state: StateFlow<SettingsUiState> = combine(
        _apps,
        prefs.defaultUpiPackage,
        prefs.biometricLockEnabled,
        prefs.upiAssistEnabled,
    ) { apps, def, bio, upiAssist ->
        SettingsUiState(
            installedUpiApps = apps,
            defaultUpiPackage = def,
            biometricEnabled = bio,
            upiAssistEnabled = upiAssist,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    val categories: StateFlow<List<Category>> = categoryRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        refreshApps()
    }

    fun refreshApps() {
        _apps.value = upiDiscovery.installedApps()
    }

    fun setDefaultUpi(pkg: String) {
        viewModelScope.launch { prefs.setDefaultUpiPackage(pkg) }
    }

    fun setBiometric(enabled: Boolean) {
        viewModelScope.launch { prefs.setBiometricLock(enabled) }
    }

    fun setUpiAssist(enabled: Boolean) {
        viewModelScope.launch { prefs.setUpiAssistEnabled(enabled) }
    }

    fun createCategory(
        name: String,
        emoji: String,
        colorHex: String,
        lifestyleGroup: LifestyleGroup,
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            categoryRepository.create(name.trim(), emoji.ifBlank { "\u2022" }, colorHex, lifestyleGroup)
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch { categoryRepository.update(category) }
    }

    fun archiveCategory(id: String) {
        viewModelScope.launch { categoryRepository.archive(id) }
    }

    fun unarchiveCategory(id: String) {
        viewModelScope.launch { categoryRepository.unarchive(id) }
    }

    fun reorderCategories(ids: List<String>) {
        viewModelScope.launch { categoryRepository.reorder(ids) }
    }
}
