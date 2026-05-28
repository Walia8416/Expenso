package com.expenso.app.feature.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.appwidget.action.actionStartActivity
import com.expenso.app.MainActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object ExpensoWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WidgetContent()
        }
    }

    @Composable
    private fun WidgetContent() {
        val primary = Color(0xFF6A4FE2)
        val accent = Color(0xFFE26A4F)
        val surface = Color(0xFFF6F2FF)
        val onSurface = Color(0xFF1D1B22)

        val scanIntent = Intent(androidx.glance.LocalContext.current, MainActivity::class.java).apply {
            action = MainActivity.ACTION_WIDGET_SCAN
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val cashIntent = Intent(androidx.glance.LocalContext.current, MainActivity::class.java).apply {
            action = MainActivity.ACTION_WIDGET_CASH
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(surface)
                .cornerRadius(24.dp)
                .padding(12.dp),
        ) {
            Text(
                "Expenso",
                style = TextStyle(
                    color = ColorProvider(onSurface),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                ),
            )
            Spacer(GlanceModifier.height(6.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth().fillMaxHeight(),
            ) {
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                        .background(primary)
                        .cornerRadius(18.dp)
                        .clickable(actionStartActivity(scanIntent))
                        .padding(12.dp),
                    contentAlignment = Alignment.BottomStart,
                ) {
                    Column {
                        Text(
                            "Scan",
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        Text(
                            "and pay",
                            style = TextStyle(
                                color = ColorProvider(Color.White.copy(alpha = 0.9f)),
                                fontSize = 12.sp,
                            ),
                        )
                    }
                }
                Spacer(GlanceModifier.width(8.dp))
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                        .background(accent)
                        .cornerRadius(18.dp)
                        .clickable(actionStartActivity(cashIntent))
                        .padding(12.dp),
                    contentAlignment = Alignment.BottomStart,
                ) {
                    Column {
                        Text(
                            "Log",
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        Text(
                            "cash / card",
                            style = TextStyle(
                                color = ColorProvider(Color.White.copy(alpha = 0.9f)),
                                fontSize = 12.sp,
                            ),
                        )
                    }
                }
            }
        }
    }
}
