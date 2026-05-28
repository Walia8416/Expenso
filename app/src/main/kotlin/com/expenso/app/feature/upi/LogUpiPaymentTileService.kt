package com.expenso.app.feature.upi

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import com.expenso.app.MainActivity

/**
 * Quick Settings tile. When the user taps it we jump directly into Expenso's
 * manual UPI log sheet — the workaround for merchant flows that
 * `setPackage(...)` a specific PSP app and bypass our mediator.
 *
 * We use [TileService.startActivityAndCollapse] so the QS shade collapses
 * before we land on the add sheet. API 34 deprecated the `Intent` overload
 * in favour of `PendingIntent`; we gate on SDK level to stay compatible.
 */
class LogUpiPaymentTileService : TileService() {

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            action = MainActivity.ACTION_LOG_UPI
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pi = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            startActivityAndCollapse(pi)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
