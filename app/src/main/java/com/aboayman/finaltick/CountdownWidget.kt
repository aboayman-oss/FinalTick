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
import android.os.Bundle
import android.os.PowerManager
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.aboayman.finaltick.widget.WidgetEditSettingsActivity
import com.aboayman.finaltick.widget.WidgetLayoutManager
import com.aboayman.finaltick.widget.WidgetLayoutManager.applyVisibilityOverrides
import com.aboayman.finaltick.widget.WidgetPreferencesManager
import com.aboayman.finaltick.widget.WidgetPreferencesManager.TimeDisplayStyle
import com.aboayman.finaltick.widget.WidgetSettingsActivity


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

                Thread {
                    for (drawableRes in frameDrawables) {
                        views.setImageViewResource(R.id.widgetRefreshBtn, drawableRes)
                        widgetManager.updateAppWidget(widgetId, views)
                        Thread.sleep(20)
                    }

                    views.setImageViewResource(
                        R.id.widgetRefreshBtn,
                        R.drawable.refresh_cycle_normal
                    )
                    widgetManager.updateAppWidget(widgetId, views)
                }.start()

                updateWidget(context, widgetManager, widgetId)
            }
        }
    }
    companion object {
        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val deadline = WidgetPreferencesManager.getDeadline(context, appWidgetId)
            if (deadline == -1L) return

            val title = WidgetPreferencesManager.getTitle(context, appWidgetId)
            val views = RemoteViews(context.packageName, R.layout.widget_countdown)
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)

            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            val maxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minWidth)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
            val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minHeight)

            val widthDp = ((minWidth + maxWidth) / 2f)
            val heightDp = ((minHeight + maxHeight) / 2f)

            // --- Date and progress ---
            val now = System.currentTimeMillis()
            val originalCreatedAt = WidgetPreferencesManager.getCreatedAt(context, appWidgetId)
            val elapsed = now - originalCreatedAt

            var timerText = "00:00:00:00"
            var percentText = "0%"
            val dateText =
                android.text.format.DateFormat.format("EEE, MMM d · h:mm a", deadline).toString()

            if (deadline > now) {
                var remaining = (deadline - now) / 1000
                val totalDuration = (deadline - originalCreatedAt).coerceAtLeast(1L)
                val progress = ((elapsed.coerceAtLeast(0L) * 100) / totalDuration).coerceIn(0, 100)
                percentText = "$progress%"

                views.setProgressBar(R.id.widgetProgressBar, 100, progress.toInt(), false)

                val showDays = WidgetPreferencesManager.getToggle(context, appWidgetId, "show_days")
                val showHours =
                    WidgetPreferencesManager.getToggle(context, appWidgetId, "show_hours")
                val showMinutes =
                    WidgetPreferencesManager.getToggle(context, appWidgetId, "show_minutes")
                val showSeconds =
                    WidgetPreferencesManager.getToggle(context, appWidgetId, "show_seconds")

                var days = 0L
                var hours = 0L
                var minutes = 0L
                var seconds = 0L

                val rawDays = remaining / (24 * 3600)
                val rawHours = (remaining % (24 * 3600)) / 3600
                val rawMinutes = (remaining % 3600) / 60
                val rawSeconds = remaining % 60

                if (showDays) {
                    days = rawDays; remaining -= days * 86400
                }
                if (showHours) {
                    hours = remaining / 3600; remaining -= hours * 3600
                } else remaining += hours * 3600
                if (showMinutes) {
                    minutes = remaining / 60; remaining -= minutes * 60
                } else remaining += minutes * 60
                if (showSeconds) {
                    seconds = remaining
                }

                if (!showMinutes && showSeconds) seconds += minutes * 60
                if (!showHours && (showMinutes || showSeconds)) {
                    val bonus = hours * 3600
                    if (showMinutes) {
                        minutes += bonus / 60
                        seconds += bonus % 60
                    } else seconds += bonus
                }
                if (!showDays && (showHours || showMinutes || showSeconds)) {
                    val bonus = days * 86400
                    if (showHours) {
                        hours += bonus / 3600
                        val leftover = bonus % 3600
                        if (showMinutes) {
                            minutes += leftover / 60
                            seconds += leftover % 60
                        } else {
                            seconds += leftover
                        }
                    } else if (showMinutes) {
                        minutes += bonus / 60
                        seconds += bonus % 60
                    } else seconds += bonus
                }

                val parts = mutableListOf<String>()
                if (showDays) {
                    parts.add(days.toString())
                    parts.add(hours.toString().padStart(2, '0'))
                    parts.add(minutes.toString().padStart(2, '0'))
                    parts.add(seconds.toString().padStart(2, '0'))
                } else if (showHours) {
                    parts.add(hours.toString())
                    parts.add(minutes.toString().padStart(2, '0'))
                    parts.add(seconds.toString().padStart(2, '0'))
                } else if (showMinutes) {
                    parts.add(minutes.toString())
                    parts.add(seconds.toString().padStart(2, '0'))
                } else if (showSeconds) {
                    parts.add(seconds.toString())
                }

                val style = WidgetPreferencesManager.getTimeDisplayStyle(context, appWidgetId)
                timerText = formatTimerText(
                    context,
                    appWidgetId,
                    style,
                    days,
                    hours,
                    minutes,
                    seconds,
                    progress.toInt()
                )
            }

            // === Smart adaptive layout with real content ===
            val defaultConfig = WidgetLayoutManager.getAdaptiveLayoutConfig(
                widthDp = widthDp,
                heightDp = heightDp,
                titleText = title,
                timerText = timerText,
                percentText = percentText,
                dateText = dateText
            )
            val layoutConfig = applyVisibilityOverrides(context, appWidgetId, defaultConfig)

            // === Set static UI ===
            views.setTextViewText(R.id.widgetTitle, title)
            views.setTextViewText(R.id.widgetDate, dateText)
            views.setTextViewText(R.id.widgetTimer, timerText)
            views.setTextViewText(R.id.widgetProgressPercent, percentText)

            // --- Colors ---
            val titleColor = WidgetPreferencesManager.getColor(
                context,
                appWidgetId,
                "color_title",
                context.getColor(R.color.onSurface)
            )
            val dateColor = WidgetPreferencesManager.getColor(
                context,
                appWidgetId,
                "color_date",
                context.getColor(R.color.onSurface)
            )
            val iconColor = WidgetPreferencesManager.getColor(
                context,
                appWidgetId,
                "color_icon",
                context.getColor(R.color.onSurface)
            )
            val timerColor = WidgetPreferencesManager.getColor(
                context,
                appWidgetId,
                "color_timer",
                context.getColor(R.color.onSurface)
            )
            val percentColor = WidgetPreferencesManager.getColor(
                context,
                appWidgetId,
                "color_percentage",
                context.getColor(R.color.onSurface)
            )

            views.setTextColor(R.id.widgetTitle, titleColor)
            views.setTextColor(R.id.widgetDate, dateColor)
            views.setTextColor(R.id.widgetProgressPercent, percentColor)
            views.setTextColor(R.id.widgetTimer, timerColor)
            views.setInt(R.id.widgetRefreshBtn, "setColorFilter", iconColor)

            // --- Visibility ---
            views.setViewVisibility(
                R.id.widgetTitle,
                if (layoutConfig.showTitle) View.VISIBLE else View.GONE
            )
            views.setViewVisibility(
                R.id.widgetDate,
                if (layoutConfig.showDate) View.VISIBLE else View.GONE
            )
            views.setViewVisibility(
                R.id.widgetTimer,
                if (layoutConfig.showTimer) View.VISIBLE else View.GONE
            )
            views.setViewVisibility(
                R.id.widgetProgressBar,
                if (layoutConfig.showProgress) View.VISIBLE else View.GONE
            )
            views.setViewVisibility(
                R.id.widgetProgressPercent,
                if (layoutConfig.showPercent) View.VISIBLE else View.GONE
            )
            views.setViewVisibility(
                R.id.widgetRefreshBtn,
                if (layoutConfig.showIcon) View.VISIBLE else View.GONE
            )

            // --- Text Sizes ---
            views.setTextViewTextSize(
                R.id.widgetTitle,
                TypedValue.COMPLEX_UNIT_SP,
                layoutConfig.titleSize
            )
            views.setTextViewTextSize(
                R.id.widgetDate,
                TypedValue.COMPLEX_UNIT_SP,
                layoutConfig.dateSize
            )
            views.setTextViewTextSize(
                R.id.widgetTimer,
                TypedValue.COMPLEX_UNIT_SP,
                layoutConfig.timerSize
            )
            views.setTextViewTextSize(
                R.id.widgetProgressPercent,
                TypedValue.COMPLEX_UNIT_SP,
                layoutConfig.percentSize
            )

            // --- Error styling ---
            if (deadline <= now) {
                views.setTextColor(R.id.widgetTimer, context.getColor(R.color.colorDanger))
            }

            // --- Interactions ---
            val refreshIntent = Intent(context, CountdownWidget::class.java).apply {
                action = "com.aboayman.finaltick.REFRESH_WIDGET"
            }
            val refreshPending = PendingIntent.getBroadcast(
                context, 0, refreshIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widgetRefreshBtn, refreshPending)

            val editIntent = Intent(context, WidgetEditSettingsActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val editPendingIntent = PendingIntent.getActivity(
                context, appWidgetId, editIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widgetRoot, editPendingIntent)

            // --- Apply to system ---
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

            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + delay,
                    pendingIntent
                )
            } else {
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
        fun forceUpdateWidget(context: Context, appWidgetId: Int) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            updateWidget(context, appWidgetManager, appWidgetId)
        }
        fun resetWidget(context: Context, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_countdown)

            // Show fallback content
            views.setTextViewText(R.id.widgetTitle, "No deadline")
            views.setTextViewText(R.id.widgetTimer, "00:00:00:00")

            val configIntent = Intent(context, WidgetSettingsActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }

            val configPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                configIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            views.setOnClickPendingIntent(R.id.widgetRoot, configPendingIntent)

            AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views)
        }

        private fun formatTimerText(
            context: Context,
            appWidgetId: Int,
            style: TimeDisplayStyle,
            days: Long,
            hours: Long,
            minutes: Long,
            seconds: Long,
            progress: Int
        ): String {
            val showDays = WidgetPreferencesManager.getToggle(context, appWidgetId, "show_days")
            val showHours = WidgetPreferencesManager.getToggle(context, appWidgetId, "show_hours")
            val showMinutes =
                WidgetPreferencesManager.getToggle(context, appWidgetId, "show_minutes")
            val showSeconds =
                WidgetPreferencesManager.getToggle(context, appWidgetId, "show_seconds")

            return when (style) {
                TimeDisplayStyle.COLON -> {
                    val parts = mutableListOf<String>()
                    if (showDays) parts.add(days.toString())
                    if (showHours) parts.add(hours.toString().padStart(2, '0'))
                    if (showMinutes) parts.add(minutes.toString().padStart(2, '0'))
                    if (showSeconds) parts.add(seconds.toString().padStart(2, '0'))
                    parts.joinToString(":")
                }

                TimeDisplayStyle.LETTER -> {
                    buildString {
                        if (showDays) append("${days}d ")
                        if (showHours) append("${hours}h ")
                        if (showMinutes) append("${minutes}m ")
                        if (showSeconds) append("${seconds}s")
                    }.trim()
                }

                TimeDisplayStyle.NATURAL_LANGUAGE -> {
                    val parts = mutableListOf<String>()
                    if (showDays && days > 0) parts.add("$days ${if (days == 1L) "day" else "days"}")
                    if (showHours && hours > 0) parts.add("$hours ${if (hours == 1L) "hour" else "hours"}")
                    if (showMinutes && minutes > 0) parts.add("$minutes ${if (minutes == 1L) "minute" else "minutes"}")
                    if (showSeconds && seconds > 0) parts.add("$seconds ${if (seconds == 1L) "second" else "seconds"}")
                    parts.take(2).joinToString(", ") + " remaining"
                }

                TimeDisplayStyle.VERBOSE_SINGLE -> {
                    when {
                        showDays -> "$days ${if (days == 1L) "day" else "days"} remaining"
                        showHours -> "$hours ${if (hours == 1L) "hour" else "hours"} remaining"
                        showMinutes -> "$minutes ${if (minutes == 1L) "minute" else "minutes"} remaining"
                        else -> "$seconds ${if (seconds == 1L) "second" else "seconds"} remaining"
                    }
                }

                TimeDisplayStyle.COUNTDOWN_WORDS -> {
                    when {
                        showDays -> "Only $days ${if (days == 1L) "day" else "days"} left!"
                        showHours -> "Only $hours ${if (hours == 1L) "hour" else "hours"} left!"
                        showMinutes -> "Only $minutes ${if (minutes == 1L) "minute" else "minutes"} left!"
                        else -> "Only $seconds ${if (seconds == 1L) "second" else "seconds"} left!"
                    }
                }

                TimeDisplayStyle.MINIMAL_PROGRESS -> {
                    "Progress: $progress%"
                }
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateWidget(context, appWidgetManager, appWidgetId) // 👈 trigger smart layout logic
    }
}