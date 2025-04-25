package com.aboayman.finaltick

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission

class CountdownWidgetUpdater : BroadcastReceiver() {
    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    override fun onReceive(context: Context, intent: Intent) {
        val widgetManager = AppWidgetManager.getInstance(context)
        val ids = widgetManager.getAppWidgetIds(
            ComponentName(context, CountdownWidget::class.java)
        )
        for (id in ids) {
            CountdownWidget.updateWidget(context, widgetManager, id)
        }

        // 🔥 RESCHEDULE after 1s or 2s (depending if screen ON/OFF)
        CountdownWidget.scheduleNextUpdate(context)
    }
}