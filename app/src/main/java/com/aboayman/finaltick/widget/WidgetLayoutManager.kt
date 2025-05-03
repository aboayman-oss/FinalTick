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

    data class TextScaleProfile(
        val timerDivisorW: Float,
        val timerDivisorH: Float,
        val timerMin: Float,
        val timerMax: Float,

        val titleDivisorW: Float,
        val titleDivisorH: Float,
        val titleMin: Float,
        val titleMax: Float,

        val dateDivisorW: Float,
        val dateDivisorH: Float,
        val dateMin: Float,
        val dateMax: Float,

        val percentDivisorW: Float,
        val percentDivisorH: Float,
        val percentMin: Float,
        val percentMax: Float
    )

    private fun estimateSingleLineSp(textLength: Int, widthBudgetDp: Float): Float {
        val avgCharWidthPerSp = 0.55f // Empirical constant (adjustable)
        return (widthBudgetDp / (textLength * avgCharWidthPerSp))
    }

    val defaultScaleProfile = TextScaleProfile(
        timerDivisorW = 4.5f, timerDivisorH = 3.5f, timerMin = 18f, timerMax = 60f,
        titleDivisorW = 7.0f, titleDivisorH = 4.5f, titleMin = 12f, titleMax = 36f,
        dateDivisorW = 8.0f, dateDivisorH = 5.0f, dateMin = 10f, dateMax = 28f,
        percentDivisorW = 9.5f, percentDivisorH = 6.0f, percentMin = 10f, percentMax = 22f
    )

    fun getAdaptiveLayoutConfig(
        widthDp: Float,
        heightDp: Float,
        profile: TextScaleProfile = defaultScaleProfile,
        titleText: String = "Title",
        timerText: String = "00:00:00",
        percentText: String = "100%",
        dateText: String = "Wed, Jan 1 · 12:00 PM"
    ): LayoutConfig {
        val verticalBudget = heightDp

        val showTitle = verticalBudget > 200f
        val showDate = verticalBudget > 200f
        val showPercent = verticalBudget > 200f

        val showIcon = widthDp > 200f
        val iconDpWidth = 80f // Estimate: icon + margin
        val availableWidthDp = if (showIcon) widthDp - iconDpWidth else widthDp

        fun scaleSmart(
            widthDiv: Float,
            heightDiv: Float,
            min: Float,
            max: Float,
            textLength: Int,
            widthBudgetPercent: Float
        ): Float {
            val widthPart = widthDp / widthDiv
            val heightPart = heightDp / heightDiv
            val baseSp = ((widthPart * 0.4f) + (heightPart * 0.6f)).coerceIn(min, max)
            val budgetDp = availableWidthDp * widthBudgetPercent
            val maxSpByFit = estimateSingleLineSp(textLength, budgetDp)
            return minOf(baseSp, maxSpByFit).coerceIn(min, max)
        }

        val timerSize = scaleSmart(
            profile.timerDivisorW, profile.timerDivisorH,
            profile.timerMin, profile.timerMax,
            timerText.length, 0.95f
        )

        val titleSize = scaleSmart(
            profile.titleDivisorW, profile.titleDivisorH,
            profile.titleMin, profile.titleMax,
            titleText.length, 0.85f
        )

        val dateSize = scaleSmart(
            profile.dateDivisorW, profile.dateDivisorH,
            profile.dateMin, profile.dateMax,
            dateText.length, 0.85f
        )

        val percentSize = scaleSmart(
            profile.percentDivisorW, profile.percentDivisorH,
            profile.percentMin, profile.percentMax,
            percentText.length, 0.25f
        )

        Log.d("AdaptiveLayout", "widthDp=$widthDp, heightDp=$heightDp")
        Log.d(
            "AdaptiveLayout",
            "timer=$timerSize, title=$titleSize, date=$dateSize, percent=$percentSize"
        )

        return LayoutConfig(
            showTitle = showTitle,
            showDate = showDate,
            showTimer = true,
            showProgress = true,
            showPercent = showPercent,
            showIcon = showIcon,
            titleSize = titleSize,
            dateSize = dateSize,
            timerSize = timerSize,
            percentSize = percentSize
        )
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
