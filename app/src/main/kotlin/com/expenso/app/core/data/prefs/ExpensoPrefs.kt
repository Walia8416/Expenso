package com.expenso.app.core.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.prefsStore: DataStore<Preferences> by preferencesDataStore(name = "expenso_prefs")

@Singleton
class ExpensoPrefs @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store = context.prefsStore

    val onboardingComplete: Flow<Boolean> = store.data.map { it[KEY_ONBOARDING_DONE] ?: false }
    val defaultUpiPackage: Flow<String?> = store.data.map { it[KEY_DEFAULT_UPI] }
    val lastUsedCategoryId: Flow<String?> = store.data.map { it[KEY_LAST_CATEGORY] }
    val biometricLockEnabled: Flow<Boolean> = store.data.map { it[KEY_BIOMETRIC] ?: false }
    val upiAssistEnabled: Flow<Boolean> = store.data.map { it[KEY_UPI_ASSIST] ?: false }
    val defaultUpiPromptShown: Flow<Boolean> = store.data.map { it[KEY_DEFAULT_UPI_PROMPT] ?: false }
    val pendingExpenseId: Flow<String?> = store.data.map { it[KEY_PENDING_EXPENSE_ID] }

    suspend fun setOnboardingComplete(done: Boolean) = store.edit {
        it[KEY_ONBOARDING_DONE] = done
    }

    suspend fun setDefaultUpiPackage(pkg: String?) = store.edit {
        if (pkg == null) it.remove(KEY_DEFAULT_UPI) else it[KEY_DEFAULT_UPI] = pkg
    }

    suspend fun setLastUsedCategoryId(id: String) = store.edit {
        it[KEY_LAST_CATEGORY] = id
    }

    suspend fun setBiometricLock(enabled: Boolean) = store.edit {
        it[KEY_BIOMETRIC] = enabled
    }

    suspend fun setUpiAssistEnabled(enabled: Boolean) = store.edit {
        it[KEY_UPI_ASSIST] = enabled
    }

    suspend fun setDefaultUpiPromptShown(shown: Boolean) = store.edit {
        it[KEY_DEFAULT_UPI_PROMPT] = shown
    }

    /**
     * Persists the expense id that is waiting for user confirmation after a
     * UPI hand-off. Survives the mediator activity finishing while the user
     * is in the PSP app, so `ScannerScreen` can prompt next time Expenso is
     * opened.
     */
    suspend fun setPendingExpenseId(id: String?) = store.edit {
        if (id == null) it.remove(KEY_PENDING_EXPENSE_ID) else it[KEY_PENDING_EXPENSE_ID] = id
    }

    private companion object {
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val KEY_DEFAULT_UPI = stringPreferencesKey("default_upi_pkg")
        val KEY_LAST_CATEGORY = stringPreferencesKey("last_used_category")
        val KEY_BIOMETRIC = booleanPreferencesKey("biometric_lock")
        val KEY_UPI_ASSIST = booleanPreferencesKey("upi_assist_enabled")
        val KEY_DEFAULT_UPI_PROMPT = booleanPreferencesKey("default_upi_prompt_shown")
        val KEY_PENDING_EXPENSE_ID = stringPreferencesKey("pending_expense_id")
    }
}
