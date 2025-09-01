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
        val iconScale: Float,
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
        val avgCharWidthPerSp = 0.52f // conservative to avoid clipping
        return if (textLength <= 0 || widthBudgetDp <= 0f) 8f else (widthBudgetDp / (textLength * avgCharWidthPerSp))
    }

    val defaultScaleProfile = TextScaleProfile(
        timerDivisorW = 4.0f, timerDivisorH = 3.2f, timerMin = 16f, timerMax = 72f,
        titleDivisorW = 6.5f, titleDivisorH = 4.2f, titleMin = 13f, titleMax = 40f,
        dateDivisorW = 7.5f, dateDivisorH = 4.8f, dateMin = 11f, dateMax = 30f,
        percentDivisorW = 8.5f, percentDivisorH = 5.5f, percentMin = 11f, percentMax = 24f
    )

    // Cell size assumptions (align with widget_info.xml intent)
    private const val CELL_W_DP = 80f
    private const val CELL_H_DP = 100f

    private fun estCols(widthDp: Float) =
        kotlin.math.max(1, kotlin.math.floor(widthDp / CELL_W_DP).toInt())

    private fun estRows(heightDp: Float) =
        kotlin.math.max(1, kotlin.math.floor(heightDp / CELL_H_DP).toInt())

    private fun fitTextSp(
        text: String,
        widthBudgetDp: Float,
        heightBudgetDp: Float,
        minSp: Float,
        maxSp: Float,
        lineHeightFactor: Float = 1.22f,
        avgCharWidthPerSp: Float = 0.55f,
        safety: Float = 0.94f
    ): Float {
        val byWidth =
            if (text.isEmpty()) minSp else (widthBudgetDp / (text.length * avgCharWidthPerSp))
        val byHeight = if (heightBudgetDp > 0f) heightBudgetDp / lineHeightFactor else maxSp
        val base = byWidth.coerceAtMost(byHeight)
        return (base * safety).coerceIn(minSp, maxSp)
    }

    fun getAdaptiveLayoutConfig(
        widthDp: Float,
        heightDp: Float,
        profile: TextScaleProfile = defaultScaleProfile,
        titleText: String = "Title",
        timerText: String = "00:00:00",
        percentText: String = "100%",
        dateText: String = "Wed, Jan 1 · 12:00 PM"
    ): LayoutConfig {
        val cols = floor(widthDp / CELL_W_DP).toInt().coerceAtLeast(1)
        val rows = floor(heightDp / CELL_H_DP).toInt().coerceAtLeast(1)

        // Smart Element Visibility based on grid
        val showTitle = rows >= 2
        val showDate = rows >= 2
        val showPercent = rows >= 2

        val showIcon = !(cols == 2 && rows == 1)
        // Adaptive icon scale factor (applied via RemoteViews setScaleX/Y later)
        val iconScale = when {
            cols >= 4 && rows >= 2 -> 1.25f
            cols >= 3 || rows >= 2 -> 1.0f
            else -> 0.85f
        }

        // Do not reserve explicit space for the icon; only show it when cols >= 3
        val sidePaddingDp = 12f
        val availableWidthDp = (widthDp - sidePaddingDp).coerceAtLeast(40f)

        // Vertical height budgets for each text element
        val titleHeightBudget = if (rows >= 2) heightDp * 0.20f else 0f
        val dateHeightBudget = if (rows >= 2) heightDp * 0.18f else 0f
        val timerHeightBudget = if (rows >= 2) heightDp * 0.40f else heightDp * 0.55f
        val percentHeightBudget = if (rows >= 2) heightDp * 0.14f else 0f

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

        // Second-pass fit to ensure no clipping within budgets
        val timerSize2 = fitTextSp(
            timerText,
            widthBudgetDp = availableWidthDp * 0.96f,
            heightBudgetDp = timerHeightBudget,
            minSp = profile.timerMin,
            maxSp = profile.timerMax,
            lineHeightFactor = 1.24f,
            avgCharWidthPerSp = 0.56f,
            safety = 0.94f
        )
        val titleSize2 = fitTextSp(
            titleText,
            widthBudgetDp = availableWidthDp * 0.93f,
            heightBudgetDp = titleHeightBudget,
            minSp = profile.titleMin,
            maxSp = profile.titleMax,
            lineHeightFactor = 1.26f,
            avgCharWidthPerSp = 0.58f,
            safety = 0.94f
        )
        val dateSize2 = fitTextSp(
            dateText,
            widthBudgetDp = availableWidthDp * 0.93f,
            heightBudgetDp = dateHeightBudget,
            minSp = profile.dateMin,
            maxSp = profile.dateMax,
            lineHeightFactor = 1.26f,
            avgCharWidthPerSp = 0.58f,
            safety = 0.94f
        )
        val percentSize2 = fitTextSp(
            percentText,
            widthBudgetDp = availableWidthDp * 0.38f,
            heightBudgetDp = percentHeightBudget,
            minSp = profile.percentMin,
            maxSp = profile.percentMax,
            lineHeightFactor = 1.30f,
            avgCharWidthPerSp = 0.56f,
            safety = 0.94f
        )

        Log.d("AdaptiveLayout", "widthDp=$widthDp, heightDp=$heightDp")
        Log.d(
            "AdaptiveLayout",
            "timer=$timerSize2, title=$titleSize2, date=$dateSize2, percent=$percentSize2, iconScale=$iconScale"
        )

        return LayoutConfig(
            showTitle = showTitle,
            showDate = showDate,
            showTimer = true,
            showProgress = true,
            showPercent = showPercent,
            showIcon = showIcon,
            iconScale = iconScale,
            titleSize = titleSize2,
            dateSize = dateSize2,
            timerSize = timerSize2,
            percentSize = percentSize2
        )
    }

    fun getGridSizeKey(context: Context, appWidgetId: Int): String {
        val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
        val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0).toFloat()
        val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0).toFloat()

        val oneCellWidthDp = CELL_W_DP
        val oneCellHeightDp = CELL_H_DP

        val colSpan = floor(widthDp / oneCellWidthDp).toInt().coerceAtLeast(1)
        val rowSpan = floor(heightDp / oneCellHeightDp).toInt().coerceAtLeast(1)

        val sizeKey = "${colSpan}x${rowSpan}"
        Log.d("WidgetSize", "width=$widthDp dp, height=$heightDp dp, sizeKey=$sizeKey")

        return sizeKey
    }

    suspend fun applyVisibilityOverrides(
        context: Context,
        appWidgetId: Int,
        base: LayoutConfig
    ): LayoutConfig {
        val withPrefs = base.copy(
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
        // Enforce size constraints: hide icon only for 2x1
        val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val maxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minWidth)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minHeight)
        val widthDp = ((minWidth + maxWidth) / 2f)
        val heightDp = ((minHeight + maxHeight) / 2f)
        val cols = estCols(widthDp)
        val rows = estRows(heightDp)
        return withPrefs.copy(
            showIcon = withPrefs.showIcon && !(cols == 2 && rows == 1)
        )
    }
}
