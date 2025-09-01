package com.aboayman.finaltick.widget

import android.app.Activity
import android.graphics.Typeface
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.aboayman.finaltick.R
import com.aboayman.finaltick.widget.WidgetPreferencesManager.FontChoice
import com.aboayman.finaltick.widget.WidgetPreferencesManager.TimeDisplayStyle

class FakeWidgetPreviewController(private val activity: Activity) {

    private val title: TextView = activity.findViewById(R.id.fakeWidgetTitle)
    private val date: TextView = activity.findViewById(R.id.fakeWidgetDate)
    private val timer: TextView = activity.findViewById(R.id.fakeWidgetTimer)
    private val progressBar: ProgressBar = activity.findViewById(R.id.fakeWidgetProgressBar)
    private val percent: TextView = activity.findViewById(R.id.fakeWidgetProgressPercent)
    private val refreshIcon: ImageView = activity.findViewById(R.id.fakeWidgetRefreshBtn)
    private val backgroundView: ImageView = activity.findViewById(R.id.fakeBackgroundView)

    fun updateVisibility(
        showTitle: Boolean,
        showDate: Boolean,
        showTimer: Boolean,
        showProgress: Boolean,
        showPercent: Boolean,
        showIcon: Boolean
    ) {
        title.visibility = if (showTitle) View.VISIBLE else View.GONE
        date.visibility = if (showDate) View.VISIBLE else View.GONE
        timer.visibility = if (showTimer) View.VISIBLE else View.GONE
        progressBar.visibility = if (showProgress) View.VISIBLE else View.GONE
        percent.visibility = if (showPercent) View.VISIBLE else View.GONE
        refreshIcon.visibility = if (showIcon) View.VISIBLE else View.GONE
    }

    fun applyColors(
        titleColor: Int?,
        dateColor: Int?,
        timerColor: Int?,
        percentColor: Int?,
        iconTint: Int?
    ) {
        titleColor?.let { title.setTextColor(it) }
        dateColor?.let { date.setTextColor(it) }
        timerColor?.let { timer.setTextColor(it) }
        percentColor?.let { percent.setTextColor(it) }
        iconTint?.let { refreshIcon.setColorFilter(it) }
    }

    fun applyBackground(color: Int?, alpha: Int?) {
        val base = color ?: return
        val a = (alpha ?: 0xCC).coerceIn(0, 255)
        // Apply RGB tint and control transparency via ImageView alpha for correct visual result
        val rgb = (0x00FFFFFF and base) or (0xFF shl 24)
        backgroundView.setColorFilter(rgb)
        backgroundView.imageAlpha = a
    }

    fun applyShape(shape: String) {
        val res = when (shape) {
            "pill" -> R.drawable.widget_background_pill
            "square" -> R.drawable.widget_background_square
            else -> R.drawable.widget_background_rounded
        }
        backgroundView.setImageResource(res)
    }

    fun applyProgressStyle(style: String) {
        val drawableRes = when (style) {
            "dashed" -> R.drawable.widget_progress_dashed_blue
            "gradient" -> R.drawable.widget_progress_gradient_blue
            else -> R.drawable.widget_progress_solid_blue
        }
        progressBar.progressDrawable = activity.getDrawable(drawableRes)
    }

    fun updateTimerText(
        showDays: Boolean,
        showHours: Boolean,
        showMinutes: Boolean,
        showSeconds: Boolean,
        style: TimeDisplayStyle,
        progressPercent: Int = 85
    ) {
        val mockDays = 12L
        val mockHours = 8L
        val mockMinutes = 30L
        val mockSeconds = 25L

        val previewText = when (style) {
            TimeDisplayStyle.COLON -> {
                val parts = mutableListOf<String>()
                if (showDays) parts.add(mockDays.toString())
                if (showHours) parts.add(mockHours.toString().padStart(2, '0'))
                if (showMinutes) parts.add(mockMinutes.toString().padStart(2, '0'))
                if (showSeconds) parts.add(mockSeconds.toString().padStart(2, '0'))
                parts.joinToString(":")
            }

            TimeDisplayStyle.LETTER -> {
                buildString {
                    if (showDays) append("${mockDays}d ")
                    if (showHours) append("${mockHours}h ")
                    if (showMinutes) append("${mockMinutes}m ")
                    if (showSeconds) append("${mockSeconds}s")
                }.trim()
            }

            TimeDisplayStyle.NATURAL_LANGUAGE -> {
                val parts = mutableListOf<String>()
                if (showDays) parts.add("12 days")
                if (showHours) parts.add("8 hours")
                if (showMinutes) parts.add("30 minutes")
                if (showSeconds) parts.add("25 seconds")
                parts.take(2).joinToString(", ") + " remaining"
            }

            TimeDisplayStyle.VERBOSE_SINGLE -> {
                when {
                    showDays -> "12 days remaining"
                    showHours -> "8 hours remaining"
                    showMinutes -> "30 minutes remaining"
                    else -> "25 seconds remaining"
                }
            }

            TimeDisplayStyle.COUNTDOWN_WORDS -> {
                when {
                    showDays -> "Only 12 days left!"
                    showHours -> "Only 8 hours left!"
                    showMinutes -> "Only 30 minutes left!"
                    else -> "Only 25 seconds left!"
                }
            }

            TimeDisplayStyle.MINIMAL_PROGRESS -> {
                "Progress: ${progressPercent}%"
            }
        }

        timer.text = previewText
    }

    fun applyFonts(
        titleFont: FontChoice,
        dateFont: FontChoice,
        timerFont: FontChoice,
        percentFont: FontChoice
    ) {
        title.typeface = toTypeface(titleFont, "title")
        date.typeface = toTypeface(dateFont, "date")
        timer.typeface = toTypeface(timerFont, "timer")
        percent.typeface = toTypeface(percentFont, "percent")
    }

    private fun toTypeface(choice: FontChoice, element: String): android.graphics.Typeface {
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
        return android.graphics.Typeface.create(family, Typeface.NORMAL)
    }
}
