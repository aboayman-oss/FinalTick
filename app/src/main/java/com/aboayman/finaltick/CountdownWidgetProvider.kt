package com.aboayman.finaltick

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class CountdownWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val prefs = context.getSharedPreferences("finaltick_prefs", Context.MODE_PRIVATE)
        val deadline = prefs.getLong("deadline_timestamp", -1L)
        val title = prefs.getString("latest_deadline_title", "FinalTick Countdown")

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_countdown)

            // Set the title
            views.setTextViewText(R.id.widgetTitle, title)

            if (deadline > 0 && deadline > System.currentTimeMillis()) {
                val timeLeft = deadline - System.currentTimeMillis()
                val seconds = timeLeft / 1000
                val days = seconds / (24 * 3600)
                val hours = (seconds % (24 * 3600)) / 3600
                val minutes = (seconds % 3600) / 60
                val secs = seconds % 60

                val formatted = String.format("%02d:%02d:%02d:%02d", days, hours, minutes, secs)
                views.setTextViewText(R.id.widgetTimer, formatted)
            } else {
                views.setTextViewText(R.id.widgetTimer, "00:00:00:00")
                views.setTextColor(R.id.widgetTimer, context.getColor(R.color.colorDanger))
            }

            // Make clicking the widget open the main app
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent)

            // Apply the update
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}