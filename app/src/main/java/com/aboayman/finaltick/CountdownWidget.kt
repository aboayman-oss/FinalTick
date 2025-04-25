package com.aboayman.finaltick

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission

class CountdownWidget : AppWidgetProvider() {

    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startRepeatingUpdate(context)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == "com.aboayman.finaltick.REFRESH_WIDGET") {
            val widgetManager = AppWidgetManager.getInstance(context)
            val widgetIds =
                widgetManager.getAppWidgetIds(ComponentName(context, CountdownWidget::class.java))

            for (widgetId in widgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_countdown)

                // List of frame drawables
                val frameDrawables = listOf(
                    R.drawable.refresh_cycle_10,
                    R.drawable.refresh_cycle_20,
                    R.drawable.refresh_cycle_30,
                    R.drawable.refresh_cycle_40,
                    R.drawable.refresh_cycle_50,
                    R.drawable.refresh_cycle_60,
                    R.drawable.refresh_cycle_70,
                    R.drawable.refresh_cycle_80,
                    R.drawable.refresh_cycle_90,
                    R.drawable.refresh_cycle_100,
                    R.drawable.refresh_cycle_110,
                    R.drawable.refresh_cycle_120,
                    R.drawable.refresh_cycle_130,
                    R.drawable.refresh_cycle_140,
                    R.drawable.refresh_cycle_150,
                    R.drawable.refresh_cycle_160,
                    R.drawable.refresh_cycle_170,
                    R.drawable.refresh_cycle_180,
                    R.drawable.refresh_cycle_190,
                    R.drawable.refresh_cycle_200,
                    R.drawable.refresh_cycle_210,
                    R.drawable.refresh_cycle_220,
                    R.drawable.refresh_cycle_230,
                    R.drawable.refresh_cycle_240,
                    R.drawable.refresh_cycle_250,
                    R.drawable.refresh_cycle_260,
                    R.drawable.refresh_cycle_270,
                    R.drawable.refresh_cycle_280,
                    R.drawable.refresh_cycle_290,
                    R.drawable.refresh_cycle_300,
                    R.drawable.refresh_cycle_310,
                    R.drawable.refresh_cycle_320,
                    R.drawable.refresh_cycle_330,
                    R.drawable.refresh_cycle_340,
                    R.drawable.refresh_cycle_350
                )


                // ⏳ Animate frames one by one
                Thread {
                    for (drawableRes in frameDrawables) {
                        views.setImageViewResource(R.id.widgetRefreshBtn, drawableRes)
                        widgetManager.updateAppWidget(widgetId, views)
                        Thread.sleep(20) // ~40 milliseconds per frame
                    }

                    // After spin finished, set normal icon
                    views.setImageViewResource(
                        R.id.widgetRefreshBtn,
                        R.drawable.refresh_cycle_normal
                    )
                    widgetManager.updateAppWidget(widgetId, views)

                }.start()

                // Also update the main widget content (timer) after refresh
                updateWidget(context, widgetManager, widgetId)
            }
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
                            seconds += leftover
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

                // 🔥 simple fake fade effect (re-draw)
                views.setViewVisibility(R.id.widgetTimer, View.INVISIBLE)
                views.setViewVisibility(R.id.widgetTimer, View.VISIBLE)

            } else {
                views.setTextViewText(R.id.widgetTimer, "00:00:00:00")
                views.setTextColor(R.id.widgetTimer, context.getColor(R.color.colorDanger))
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


            // 📅 Tap = SelectDeadlineActivity
            val pickIntent = Intent(context, SelectDeadlineActivity::class.java)
            val pendingPick = PendingIntent.getActivity(
                context, 0, pickIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widgetRoot, pendingPick)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        @RequiresApi(Build.VERSION_CODES.S)
        @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
        fun startRepeatingUpdate(context: Context) {
            scheduleNextUpdate(context)
        }

        @RequiresApi(Build.VERSION_CODES.S)
        @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
        fun scheduleNextUpdate(context: Context) {
            val intent = Intent(context, CountdownWidgetUpdater::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val delay = if (isScreenOn(context)) 1000L else 2000L

            // 🔥 FIX: Check if exact alarms allowed
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + delay,
                    pendingIntent
                )
            } else {
                // 👇 Optional: You can fallback to non-exact alarm here if you want (less accurate)
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + delay,
                    pendingIntent
                )
            }
        }

        fun isScreenOn(context: Context): Boolean {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return powerManager.isInteractive
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