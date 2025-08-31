package com.aboayman.finaltick

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log

class CountdownWidgetUpdater : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val widgetManager = AppWidgetManager.getInstance(context)
        val ids = widgetManager.getAppWidgetIds(
            ComponentName(context, CountdownWidget::class.java)
        )
        for (id in ids) {
            val options = widgetManager.getAppWidgetOptions(id)
            val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            val maxW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minW)
            val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
            val maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minH)
            val wDp = ((minW + maxW) / 2f)
            val hDp = ((minH + maxH) / 2f)
            CountdownWidget.updateWidget(context, widgetManager, id, wDp, hDp)
        }

        // Reschedule next update; handles API/permission differences internally
        try {
            CountdownWidget.scheduleNextUpdate(context)
        } catch (e: SecurityException) {
            Log.e("CountdownWidgetUpdater", "Unable to schedule next update", e)
        }
    }
}

