package com.expenso.app.feature.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.expenso.app.MainActivity
import com.expenso.app.R

/**
 * 1×1 launcher tile. Tapping it opens Expenso on the Home screen with the
 * amount field focused so the user can start typing immediately.
 */
class QuickLogWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (id in appWidgetIds) {
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                action = MainActivity.ACTION_WIDGET_QUICK_LOG
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pending = PendingIntent.getActivity(
                context,
                id,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val views = RemoteViews(context.packageName, R.layout.quick_log_widget).apply {
                setOnClickPendingIntent(R.id.quick_log_widget_root, pending)
            }
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}
