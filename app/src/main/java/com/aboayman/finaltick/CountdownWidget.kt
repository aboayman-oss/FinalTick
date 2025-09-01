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
import android.text.SpannableString
import android.text.style.TypefaceSpan
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.aboayman.finaltick.widget.WidgetEditSettingsActivity
import com.aboayman.finaltick.widget.WidgetLayoutManager
import com.aboayman.finaltick.widget.WidgetLayoutManager.applyVisibilityOverrides
import com.aboayman.finaltick.widget.WidgetPreferencesManager
import com.aboayman.finaltick.widget.WidgetPreferencesManager.FontChoice
import com.aboayman.finaltick.widget.WidgetPreferencesManager.TimeDisplayStyle
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch


class CountdownWidget : AppWidgetProvider() {

    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            // Retrieve per-widget size options (dp) once here, pass into updateWidget
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            val maxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minWidth)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
            val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minHeight)
            val widthDp = ((minWidth + maxWidth) / 2f)
            val heightDp = ((minHeight + maxHeight) / 2f)

            updateWidget(context, appWidgetManager, appWidgetId, widthDp, heightDp)
        }
        // Ensure periodic updates across all supported Android versions
        startRepeatingUpdate(context)
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
                    val refreshIntent = Intent(context, CountdownWidget::class.java).apply {
                        action = "com.aboayman.finaltick.REFRESH_WIDGET"
                    }
                    val refreshPending = PendingIntent.getBroadcast(
                        context, 0, refreshIntent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    val editIntent = Intent(context, WidgetEditSettingsActivity::class.java).apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    }
                    val editPending = PendingIntent.getActivity(
                        context, widgetId, editIntent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )

                    for (drawableRes in frameDrawables) {
                        views.setImageViewResource(R.id.widgetRefreshBtn, drawableRes)
                        views.setOnClickPendingIntent(R.id.widgetRefreshBtn, refreshPending)
                        views.setOnClickPendingIntent(R.id.widgetRoot, editPending)
                        views.setOnClickPendingIntent(R.id.background_view, editPending)
                        views.setOnClickPendingIntent(R.id.timerBlock, editPending)
                        widgetManager.updateAppWidget(widgetId, views)
                        Thread.sleep(20)
                    }

                    views.setImageViewResource(
                        R.id.widgetRefreshBtn,
                        R.drawable.refresh_cycle_normal
                    )
                    views.setOnClickPendingIntent(R.id.widgetRefreshBtn, refreshPending)
                    views.setOnClickPendingIntent(R.id.widgetRoot, editPending)
                    views.setOnClickPendingIntent(R.id.background_view, editPending)
                    views.setOnClickPendingIntent(R.id.timerBlock, editPending)
                    widgetManager.updateAppWidget(widgetId, views)
                }.start()

                val opts = widgetManager.getAppWidgetOptions(widgetId)
                val minW = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
                val maxW = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minW)
                val minH = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
                val maxH = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minH)
                val wDp = ((minW + maxW) / 2f)
                val hDp = ((minH + maxH) / 2f)
                updateWidget(context, widgetManager, widgetId, wDp, hDp)
            }
        }
    }
    companion object {
        private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            widthDp: Float,
            heightDp: Float
        ) {
            widgetScope.launch {
            val deadline = WidgetPreferencesManager.getDeadline(context, appWidgetId)
            if (deadline == -1L) {
                resetWidget(context, appWidgetId)
                return@launch
            }

            val title = WidgetPreferencesManager.getTitle(context, appWidgetId)
            val views = RemoteViews(context.packageName, R.layout.widget_countdown)

            // --- Date and progress ---
            val now = System.currentTimeMillis()
            val originalCreatedAt = WidgetPreferencesManager.getCreatedAt(context, appWidgetId)
            val elapsed = now - originalCreatedAt

            var timerText = "00:00:00:00"
            var percentText = "0%"
                var progressInt = 0
            val dateText =
                android.text.format.DateFormat.format("EEE, MMM d · h:mm a", deadline).toString()

            if (deadline > now) {
                var remaining = (deadline - now) / 1000
                val totalDuration = (deadline - originalCreatedAt).coerceAtLeast(1L)
                val progress = ((elapsed.coerceAtLeast(0L) * 100) / totalDuration).coerceIn(0, 100)
                progressInt = progress.toInt()
                percentText = "$progressInt%"

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
                    style,
                    days,
                    hours,
                    minutes,
                    seconds,
                    progress.toInt(),
                    showDays,
                    showHours,
                    showMinutes,
                    showSeconds
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

                // === Fonts ===
                val titleFont = WidgetPreferencesManager.getTitleFont(context, appWidgetId)
                val dateFont = WidgetPreferencesManager.getDateFont(context, appWidgetId)
                val timerFont = WidgetPreferencesManager.getTimerFont(context, appWidgetId)
                val percentFont = WidgetPreferencesManager.getPercentFont(context, appWidgetId)

                // === Set static UI (with fonts via TypefaceSpan) ===
                views.setTextViewText(R.id.widgetTitle, applyFontSpan(title, "title", titleFont))
                views.setTextViewText(R.id.widgetDate, applyFontSpan(dateText, "date", dateFont))
                views.setTextViewText(
                    R.id.widgetTimer,
                    applyFontSpan(timerText, "timer", timerFont)
                )
                views.setTextViewText(
                    R.id.widgetProgressPercent,
                    applyFontSpan(percentText, "percent", percentFont)
                )

            // --- Colors ---
            var titleFallback = MaterialColors.getColor(
                context,
                com.google.android.material.R.attr.colorOnSurface,
                context.getColor(R.color.onSurface)
            )
            val dateFallback = MaterialColors.getColor(
                context,
                com.google.android.material.R.attr.colorOnSurfaceVariant,
                context.getColor(R.color.onSurface)
            )
            val iconFallback = MaterialColors.getColor(
                context,
                com.google.android.material.R.attr.colorOnSurface,
                context.getColor(R.color.onSurface)
            )
            var timerFallback = MaterialColors.getColor(
                context,
                com.google.android.material.R.attr.colorOnSurface,
                context.getColor(R.color.onSurface)
            )
            var percentFallback = MaterialColors.getColor(
                context,
                com.google.android.material.R.attr.colorOnSurface,
                context.getColor(R.color.onSurface)
            )
            val surfaceFallback = MaterialColors.getColor(
                context,
                com.google.android.material.R.attr.colorSurface,
                context.getColor(R.color.colorWidgetBackground)
            )

            val styleForColors = WidgetPreferencesManager.getTimeDisplayStyle(context, appWidgetId)
            if (styleForColors == TimeDisplayStyle.MINIMAL_PROGRESS) {
                val primary = MaterialColors.getColor(
                    context,
                    com.google.android.material.R.attr.colorPrimary,
                    context.getColor(R.color.colorPrimary)
                )
                titleFallback = primary
                timerFallback = primary
                percentFallback = primary
            }

            val titleColor = WidgetPreferencesManager.getColor(
                context,
                appWidgetId,
                "color_title",
                titleFallback
            )
            val dateColor = WidgetPreferencesManager.getColor(
                context,
                appWidgetId,
                "color_date",
                dateFallback
            )
            val iconColor = WidgetPreferencesManager.getColor(
                context,
                appWidgetId,
                "color_icon",
                iconFallback
            )
            val timerColor = WidgetPreferencesManager.getColor(
                context,
                appWidgetId,
                "color_timer",
                timerFallback
            )
            val percentColor = WidgetPreferencesManager.getColor(
                context,
                appWidgetId,
                "color_percentage",
                percentFallback
            )
            val backgroundBase = WidgetPreferencesManager.getColor(
                context,
                appWidgetId,
                "color_background",
                surfaceFallback
            )
            val backgroundAlpha = WidgetPreferencesManager.getColor(
                context,
                appWidgetId,
                "background_alpha",
                0xCC
            ).coerceIn(0, 255)
                // Use RGB tint + view alpha to avoid unexpected blending
                val backgroundTintRgb = (backgroundBase and 0x00FFFFFF) or (0xFF shl 24)

            views.setTextColor(R.id.widgetTitle, titleColor)
            views.setTextColor(R.id.widgetDate, dateColor)
            views.setTextColor(R.id.widgetProgressPercent, percentColor)
            views.setTextColor(R.id.widgetTimer, timerColor)
            views.setInt(R.id.widgetRefreshBtn, "setColorFilter", iconColor)
            // Apply background style: select shape + dynamic color + transparency
            val shape = WidgetPreferencesManager.getShape(context, appWidgetId)
            val bgRes = when (shape) {
                "pill" -> R.drawable.widget_background_pill
                "square" -> R.drawable.widget_background_square
                else -> R.drawable.widget_background_rounded
            }
            views.setImageViewResource(R.id.background_view, bgRes)
                views.setInt(R.id.background_view, "setColorFilter", backgroundTintRgb)
                views.setInt(R.id.background_view, "setImageAlpha", backgroundAlpha)

                // --- Progress style + visibility ---
                val stylePref = WidgetPreferencesManager.getProgressStyle(context, appWidgetId)
                val band = when (progressInt) {
                    in 0..50 -> "blue"
                    in 51..80 -> "yellow"
                    else -> "red"
                }
                val progressBarId = when (stylePref) {
                    "dashed" -> when (band) {
                        "blue" -> R.id.widgetProgressBarDashedBlue
                        "yellow" -> R.id.widgetProgressBarDashedYellow
                        else -> R.id.widgetProgressBarDashedRed
                    }

                    "gradient" -> when (band) {
                        "blue" -> R.id.widgetProgressBarGradientBlue
                        "yellow" -> R.id.widgetProgressBarGradientYellow
                        else -> R.id.widgetProgressBarGradientRed
                    }

                    else -> when (band) { // solid (default)
                        "blue" -> R.id.widgetProgressBar
                        "yellow" -> R.id.widgetProgressBarSolidYellow
                        else -> R.id.widgetProgressBarSolidRed
                    }
                }
                val allProgressBars = intArrayOf(
                    R.id.widgetProgressBar,
                    R.id.widgetProgressBarSolidYellow,
                    R.id.widgetProgressBarSolidRed,
                    R.id.widgetProgressBarDashedBlue,
                    R.id.widgetProgressBarDashedYellow,
                    R.id.widgetProgressBarDashedRed,
                    R.id.widgetProgressBarGradientBlue,
                    R.id.widgetProgressBarGradientYellow,
                    R.id.widgetProgressBarGradientRed
                )

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
                // Progress bar visibility: show only the selected bar if allowed by layout
                if (layoutConfig.showProgress) {
                    for (bid in allProgressBars) views.setViewVisibility(bid, View.GONE)
                    views.setViewVisibility(progressBarId, View.VISIBLE)
                    views.setProgressBar(progressBarId, 100, progressInt, false)
                } else {
                    for (bid in allProgressBars) views.setViewVisibility(bid, View.GONE)
                }
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
            // Make the whole surface interactive for reliability
            views.setOnClickPendingIntent(R.id.background_view, editPendingIntent)
            views.setOnClickPendingIntent(R.id.timerBlock, editPendingIntent)

            // --- Apply to system ---
            appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        @RequiresApi(Build.VERSION_CODES.S)
        @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
        fun startRepeatingUpdate(context: Context) {
            scheduleNextUpdate(context)
        }

        fun scheduleNextUpdate(context: Context) {
            val intent = Intent(context, CountdownWidgetUpdater::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val delay = if (isScreenOn(context)) 1000L else 2000L

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // On Android 12+, exact alarms may require user approval
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + delay,
                        pendingIntent
                    )
                } else {
                    // Fallback to inexact to avoid crashes and still update periodically
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + delay,
                        pendingIntent
                    )
                }
            } else {
                // Pre-Android 12: exact alarms allowed without special permission
                alarmManager.setExactAndAllowWhileIdle(
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
                val options = manager.getAppWidgetOptions(id)
                val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
                val maxW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minW)
                val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
                val maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minH)
                val wDp = ((minW + maxW) / 2f)
                val hDp = ((minH + maxH) / 2f)
                updateWidget(context, manager, id, wDp, hDp)
            }
        }
        fun forceUpdateWidget(context: Context, appWidgetId: Int) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            val maxW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minW)
            val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
            val maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minH)
            val wDp = ((minW + maxW) / 2f)
            val hDp = ((minH + maxH) / 2f)
            updateWidget(context, appWidgetManager, appWidgetId, wDp, hDp)
        }

        // Backward-compatible overload for callers that don't have dp sizes yet
        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            val maxW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minW)
            val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
            val maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minH)
            val wDp = ((minW + maxW) / 2f)
            val hDp = ((minH + maxH) / 2f)
            updateWidget(context, appWidgetManager, appWidgetId, wDp, hDp)
        }
        fun resetWidget(context: Context, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_countdown)

            // Show fallback content
            views.setTextViewText(R.id.widgetTitle, "No deadline")
            views.setTextViewText(R.id.widgetTimer, "00:00:00:00")
            views.setViewVisibility(R.id.widgetTitle, View.VISIBLE)
            views.setViewVisibility(R.id.widgetTimer, View.VISIBLE)

            val configIntent = Intent(context, WidgetEditSettingsActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }

            val configPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                configIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            views.setOnClickPendingIntent(R.id.widgetRoot, configPendingIntent)
            views.setOnClickPendingIntent(R.id.background_view, configPendingIntent)
            views.setOnClickPendingIntent(R.id.timerBlock, configPendingIntent)

            AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views)
        }

        private fun applyFontSpan(text: String, element: String, choice: FontChoice): CharSequence {
            val family = when (choice) {
                FontChoice.MONOSPACE -> "monospace"
                FontChoice.SERIF -> "serif"
                FontChoice.ROBOTO -> if (element == "title" || element == "timer") "sans-serif-medium" else "sans-serif"
                FontChoice.ROBOTO_REGULAR -> "sans-serif"
                FontChoice.ROBOTO_MEDIUM -> "sans-serif-medium"
                FontChoice.ROBOTO_LIGHT -> "sans-serif-light"
                FontChoice.ROBOTO_CONDENSED -> "sans-serif-condensed"
                FontChoice.ROBOTO_BLACK -> "sans-serif-black"
                FontChoice.ROBOTO_THIN -> "sans-serif-thin"
            }
            return SpannableString(text).apply {
                setSpan(TypefaceSpan(family), 0, text.length, 0)
            }
        }

        private fun formatTimerText(
            style: TimeDisplayStyle,
            days: Long,
            hours: Long,
            minutes: Long,
            seconds: Long,
            progress: Int,
            showDays: Boolean,
            showHours: Boolean,
            showMinutes: Boolean,
            showSeconds: Boolean
        ): String {
            return when (style) {
                TimeDisplayStyle.COLON -> {
                    val partsObj = CountdownParts(days, hours, minutes, seconds)
                    CountdownFormatter.formatColon(
                        partsObj,
                        showDays,
                        showHours,
                        showMinutes,
                        showSeconds
                    )
                }

                TimeDisplayStyle.LETTER -> {
                    val partsObj = CountdownParts(days, hours, minutes, seconds)
                    CountdownFormatter.formatLetters(
                        partsObj,
                        showDays,
                        showHours,
                        showMinutes,
                        showSeconds
                    )
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
