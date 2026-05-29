package com.expenso.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.expenso.app.core.ui.theme.ExpensoTheme
import com.expenso.app.navigation.ExpensoApp
import com.expenso.app.navigation.WidgetAction
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var widgetActionState: ((WidgetAction) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialAction = widgetActionFromIntent(intent)
        setContent {
            ExpensoTheme {
                var pending by remember { mutableStateOf(initialAction) }
                widgetActionState = { pending = it }
                ExpensoApp(
                    widgetAction = pending,
                    onWidgetActionConsumed = { pending = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        widgetActionFromIntent(intent)?.let { widgetActionState?.invoke(it) }
    }

    /**
     * Routes widget shortcuts, the QS tile, and the GPay-open notification
     * onto in-app entry points. Quick-log entries land on Home with the
     * amount field focused; ACTION_LOG_UPI additionally presets the
     * payment-method picker to UPI for merchant-pinned payment recovery.
     */
    private fun widgetActionFromIntent(intent: Intent?): WidgetAction? = when (intent?.action) {
        ACTION_WIDGET_SCAN -> WidgetAction.ScanAndPay
        ACTION_WIDGET_CASH -> WidgetAction.LogCash
        ACTION_WIDGET_QUICK_LOG -> WidgetAction.QuickLog
        ACTION_OPEN_QUICK_LOG -> WidgetAction.QuickLog
        ACTION_LOG_UPI -> WidgetAction.LogUpi
        else -> null
    }

    companion object {
        const val ACTION_WIDGET_SCAN = "com.expenso.app.ACTION_WIDGET_SCAN"
        const val ACTION_WIDGET_CASH = "com.expenso.app.ACTION_WIDGET_CASH"
        const val ACTION_WIDGET_QUICK_LOG = "com.expenso.app.ACTION_WIDGET_QUICK_LOG"
        const val ACTION_OPEN_QUICK_LOG = "com.expenso.app.ACTION_OPEN_QUICK_LOG"
        const val ACTION_LOG_UPI = "com.expenso.app.ACTION_LOG_UPI"
    }
}
