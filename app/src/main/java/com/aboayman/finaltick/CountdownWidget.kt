package com.aboayman.finaltick

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class CountdownWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
        startRepeatingUpdate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.aboayman.finaltick.REFRESH_WIDGET") {
            forceUpdateAll(context)
        }
    }

    companion object {
        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val prefs = context.getSharedPreferences("finaltick_prefs", Context.MODE_PRIVATE)
            val deadline = prefs.getLong("deadline_timestamp", -1L)
            val all = prefs.getStringSet("deadlines", null)

            val cbDays = prefs.getBoolean("widget_show_days", true)
            val cbHours = prefs.getBoolean("widget_show_hours", true)
            val cbMinutes = prefs.getBoolean("widget_show_minutes", true)
            val cbSeconds = prefs.getBoolean("widget_show_seconds", true)

            val current = all?.firstOrNull { it.startsWith(deadline.toString()) }
            val title = current?.split("|")?.getOrNull(1) ?: "(No title)"

            val views = RemoteViews(context.packageName, R.layout.widget_countdown)
            views.setTextViewText(R.id.widgetTitle, title)

            val now = System.currentTimeMillis()
            if (deadline > now) {
                var remaining = (deadline - now) / 1000

                var days = 0L
                var hours = 0L
                var minutes = 0L
                var seconds = 0L

                if (cbDays) {
                    days = remaining / 86400
                    remaining %= 86400
                }
                if (cbHours) {
                    hours = remaining / 3600
                    remaining %= 3600
                } else {
                    remaining += hours * 3600
                }
                if (cbMinutes) {
                    minutes = remaining / 60
                    remaining %= 60
                } else {
                    remaining += minutes * 60
                }
                if (cbSeconds) {
                    seconds = remaining
                }

                if (!cbMinutes && cbSeconds) {
                    seconds += minutes * 60
                }
                if (!cbHours && (cbMinutes || cbSeconds)) {
                    val bonus = hours * 3600
                    if (cbMinutes) {
                        minutes += bonus / 60
                        seconds += bonus % 60
                    } else {
                        seconds += bonus
                    }
                }
                if (!cbDays && (cbHours || cbMinutes || cbSeconds)) {
                    val bonus = days * 86400
                    if (cbHours) {
                        hours += bonus / 3600
                        val leftover = bonus % 3600
                        if (cbMinutes) {
                            minutes += leftover / 60
                            seconds += leftover % 60
                        } else {
                            seconds += leftover
                        }
                    } else if (cbMinutes) {
                        minutes += bonus / 60
                        seconds += bonus % 60
                    } else {
                        seconds += bonus
                    }
                }

                val parts = mutableListOf<String>()
                if (cbDays) parts.add(String.format("%02d", days))
                if (cbHours) parts.add(String.format("%02d", hours))
                if (cbMinutes) parts.add(String.format("%02d", minutes))
                if (cbSeconds) parts.add(String.format("%02d", seconds))

                views.setTextViewText(R.id.widgetTimer, parts.joinToString(":"))
            } else {
                views.setTextViewText(R.id.widgetTimer, "00:00:00:00")
            }

            // 🔁 Refresh button logic
            val refreshIntent = Intent(context, CountdownWidget::class.java).apply {
                action = "com.aboayman.finaltick.REFRESH_WIDGET"
            }
            val refreshPending = PendingIntent.getBroadcast(
                context, 0, refreshIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widgetRefreshBtn, refreshPending)

            // 📅 Tap = SelectDeadlineActivity instead of MainActivity
            val pickIntent = Intent(context, SelectDeadlineActivity::class.java)
            val pendingPick = PendingIntent.getActivity(
                context, 0, pickIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widgetRoot, pendingPick)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun startRepeatingUpdate(context: Context) {
            val intent = Intent(context, CountdownWidgetUpdater::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis(),
                1000L,
                pendingIntent
            )
        }

        fun forceUpdateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, CountdownWidget::class.java))
            for (id in ids) {
                updateWidget(context, manager, id)
            }
        }
    }
}