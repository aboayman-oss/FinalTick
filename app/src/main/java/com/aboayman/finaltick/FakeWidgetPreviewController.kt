package com.aboayman.finaltick.widget

import android.app.Activity
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.aboayman.finaltick.R

class FakeWidgetPreviewController(private val activity: Activity) {

    private val title: TextView = activity.findViewById(R.id.fakeWidgetTitle)
    private val date: TextView = activity.findViewById(R.id.fakeWidgetDate)
    private val timer: TextView = activity.findViewById(R.id.fakeWidgetTimer)
    private val progressBar: ProgressBar = activity.findViewById(R.id.fakeWidgetProgressBar)
    private val percent: TextView = activity.findViewById(R.id.fakeWidgetProgressPercent)
    private val refreshIcon: ImageView = activity.findViewById(R.id.fakeWidgetRefreshBtn)

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

    fun updateTimerText(
        showDays: Boolean,
        showHours: Boolean,
        showMinutes: Boolean,
        showSeconds: Boolean
    ) {
        val parts = mutableListOf<String>()
        if (showDays) parts.add("12")
        if (showHours) parts.add("08")
        if (showMinutes) parts.add("30")
        if (showSeconds) parts.add("25")

        timer.text = parts.joinToString(":")
    }
}