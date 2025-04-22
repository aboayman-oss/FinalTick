package com.aboayman.finaltick

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CountdownWidgetUpdater : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val widgetManager = AppWidgetManager.getInstance(context)
        val ids = widgetManager.getAppWidgetIds(
            android.content.ComponentName(context, CountdownWidget::class.java)
        )
        for (id in ids) {
            CountdownWidget.updateWidget(context, widgetManager, id)
        }
    }
}