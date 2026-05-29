package com.expenso.app.feature.upiassist

import android.accessibilityservice.AccessibilityService
import android.app.PendingIntent
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.expenso.app.ExpensoApp
import com.expenso.app.MainActivity
import com.expenso.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

private val android.content.Context.assistStore by preferencesDataStore(name = "expenso_prefs")

class UpiAssistAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var lastPromptAt: Long = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) return
        val pkg = event.packageName?.toString().orEmpty()
        if (pkg.isBlank()) return
        if (pkg !in PSP_PACKAGES) return

        val now = System.currentTimeMillis()
        if (now - lastPromptAt < PROMPT_COOLDOWN_MS) return
        lastPromptAt = now

        scope.launch {
            if (!isAssistEnabled()) return@launch
            showPrompt(pkg)
        }
    }

    override fun onInterrupt() = Unit

    private suspend fun isAssistEnabled(): Boolean {
        return runCatching {
            assistStore.data
                .map { prefs -> prefs[KEY_UPI_ASSIST] ?: false }
                .first()
        }.getOrDefault(false)
    }

    private fun showPrompt(psp: String) {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            action = MainActivity.ACTION_OPEN_QUICK_LOG
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this,
            2001,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, ExpensoApp.CHANNEL_RECONCILIATION)
            .setSmallIcon(R.drawable.ic_log_upi)
            .setContentTitle("Log this UPI payment?")
            .setContentText("Tap to open Quick Log in Expenso")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Detected ${shortName(psp)} in the foreground. Tap to capture the amount in Quick Log.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingOpen)
            .build()
        runCatching {
            NotificationManagerCompat.from(this).notify(7001, notification)
        }.onFailure { Timber.w(it, "UPI assist notify failed") }
    }

    private fun shortName(pkg: String): String =
        pkg.substringAfterLast('.').ifBlank { pkg }

    companion object {
        private const val PROMPT_COOLDOWN_MS = 60_000L
        private val KEY_UPI_ASSIST = booleanPreferencesKey("upi_assist_enabled")
        private val PSP_PACKAGES = setOf(
            "com.google.android.apps.nbu.paisa.user", // GPay
            "com.phonepe.app",                         // PhonePe
            "net.one97.paytm",                         // Paytm
            "in.org.npci.upiapp",                      // BHIM
            "com.amazon.mShop.android.shopping",       // Amazon (Amazon Pay UPI)
        )
    }
}
