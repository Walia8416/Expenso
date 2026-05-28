package com.expenso.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenso.app.core.data.prefs.ExpensoPrefs
import com.expenso.app.core.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface RootState {
    data object Loading : RootState
    data object NeedsOnboarding : RootState
    data object Ready : RootState
}

@HiltViewModel
class RootViewModel @Inject constructor(
    private val prefs: ExpensoPrefs,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<RootState>(RootState.Loading)
    val state: StateFlow<RootState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            categoryRepository.ensureSeeded()
            prefs.onboardingComplete.collect { done ->
                _state.value = if (done) RootState.Ready else RootState.NeedsOnboarding
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            prefs.setOnboardingComplete(true)
        }
    }
}
