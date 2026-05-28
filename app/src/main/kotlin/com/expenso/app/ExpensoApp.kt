package com.expenso.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.content.getSystemService
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class ExpensoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService<NotificationManager>() ?: return
        val reconcile = NotificationChannel(
            CHANNEL_RECONCILIATION,
            "Pending payments",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Nudges to confirm whether a UPI payment succeeded."
        }
        nm.createNotificationChannel(reconcile)
    }

    companion object {
        const val CHANNEL_RECONCILIATION = "reconciliation"
    }
}
