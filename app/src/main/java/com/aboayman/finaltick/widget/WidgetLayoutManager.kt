package com.aboayman.finaltick.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.util.Log
import kotlin.math.floor

object WidgetLayoutManager {

    data class LayoutConfig(
        val showTitle: Boolean,
        val showDate: Boolean,
        val showTimer: Boolean,
        val showProgress: Boolean,
        val showPercent: Boolean,
        val showIcon: Boolean,
        val titleSize: Float,
        val dateSize: Float,
        val timerSize: Float,
        val percentSize: Float
    )

    fun getLayoutConfig(sizeKey: String): LayoutConfig {
        return when (sizeKey) {
            "2x1" -> LayoutConfig(
                showTitle = false,
                showDate = false,
                showTimer = true,
                showProgress = true,
                showPercent = false,
                showIcon = false,
                titleSize = 16f,
                dateSize = 10f,
                timerSize = 26f,
                percentSize = 12f
            )

            "3x1" -> LayoutConfig(
                showTitle = false,
                showDate = false,
                showTimer = true,
                showProgress = true,
                showPercent = false,
                showIcon = true,
                titleSize = 22f,
                dateSize = 10f,
                timerSize = 34f,
                percentSize = 12f
            )

            "5x1" -> LayoutConfig(
                showTitle = false,
                showDate = false,
                showTimer = true,
                showProgress = true,
                showPercent = false,
                showIcon = true,
                titleSize = 18f,
                dateSize = 10f,
                timerSize = 40f,
                percentSize = 13f
            )

            "2x2" -> LayoutConfig(
                showTitle = true,
                showDate = true,
                showTimer = true,
                showProgress = true,
                showPercent = true,
                showIcon = false,
                titleSize = 24f,
                dateSize = 16f,
                timerSize = 30f,
                percentSize = 16f
            )

            "3x2" -> LayoutConfig(
                showTitle = true,
                showDate = true,
                showTimer = true,
                showProgress = true,
                showPercent = true,
                showIcon = true,
                titleSize = 22f,
                dateSize = 16f,
                timerSize = 34f,
                percentSize = 16f
            )

            "5x2" -> LayoutConfig(
                showTitle = true,
                showDate = true,
                showTimer = true,
                showProgress = true,
                showPercent = true,
                showIcon = true,
                titleSize = 26f,
                dateSize = 18f,
                timerSize = 50f,
                percentSize = 20f
            )

            else -> LayoutConfig( // fallback
                showTitle = true,
                showDate = true,
                showTimer = true,
                showProgress = true,
                showPercent = true,
                showIcon = true,
                titleSize = 14f,
                dateSize = 12f,
                timerSize = 24f,
                percentSize = 12f
            )
        }
    }

    fun getGridSizeKey(context: Context, appWidgetId: Int): String {
        val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
        val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0).toFloat()
        val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0).toFloat()

        val oneCellWidthDp = 80f
        val oneCellHeightDp = 100f

        val colSpan = floor(widthDp / oneCellWidthDp).toInt().coerceAtLeast(1)
        val rowSpan = floor(heightDp / oneCellHeightDp).toInt().coerceAtLeast(1)

        val sizeKey = "${colSpan}x${rowSpan}"
        Log.d("WidgetSize", "width=$widthDp dp, height=$heightDp dp, sizeKey=$sizeKey")

        return sizeKey
    }

    fun applyVisibilityOverrides(
        context: Context,
        appWidgetId: Int,
        base: LayoutConfig
    ): LayoutConfig {
        return base.copy(
            showTitle = WidgetPreferencesManager.getToggle(
                context,
                appWidgetId,
                "show_title",
                base.showTitle
            ),
            showDate = WidgetPreferencesManager.getToggle(
                context,
                appWidgetId,
                "show_date",
                base.showDate
            ),
            showTimer = WidgetPreferencesManager.getToggle(
                context,
                appWidgetId,
                "show_timer",
                base.showTimer
            ),
            showProgress = WidgetPreferencesManager.getToggle(
                context,
                appWidgetId,
                "show_progress",
                base.showProgress
            ),
            showPercent = WidgetPreferencesManager.getToggle(
                context,
                appWidgetId,
                "show_percentage",
                base.showPercent
            ),
            showIcon = WidgetPreferencesManager.getToggle(
                context,
                appWidgetId,
                "show_icon",
                base.showIcon
            )
        )
    }
}
