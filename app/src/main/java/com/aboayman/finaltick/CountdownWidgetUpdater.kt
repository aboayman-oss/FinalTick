package com.aboayman.finaltick

import android.app.AlarmManager
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class CountdownWidgetUpdater : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val widgetManager = AppWidgetManager.getInstance(context)
        val ids = widgetManager.getAppWidgetIds(
            ComponentName(context, CountdownWidget::class.java)
        )
        for (id in ids) {
            CountdownWidget.updateWidget(context, widgetManager, id)
        }

        // 🔥 RESCHEDULE only if allowed + on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (alarmManager.canScheduleExactAlarms()) {
                try {
                    CountdownWidget.scheduleNextUpdate(context)
                } catch (e: SecurityException) {
                    Log.e("CountdownWidgetUpdater", "Exact alarm permission not granted", e)
                }
            } else {
                Log.w("CountdownWidgetUpdater", "Exact alarms not permitted for this app")
            }
        }
    }
}
