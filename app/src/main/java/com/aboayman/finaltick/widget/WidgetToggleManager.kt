package com.aboayman.finaltick.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.appcompat.app.AppCompatActivity
import com.aboayman.finaltick.CountdownWidget
import com.aboayman.finaltick.R
import com.aboayman.finaltick.widget.WidgetPreferencesManager.TimeDisplayStyle
import com.google.android.material.materialswitch.MaterialSwitch

class WidgetToggleManager(
    private val activity: AppCompatActivity,
    private val appWidgetId: Int,
    private val previewController: FakeWidgetPreviewController
) {
    private val context: Context get() = activity

    private val toggles = listOf(
        "show_title" to R.id.cbTitle,
        "show_date" to R.id.cbDate,
        "show_icon" to R.id.cbIcon,
        "show_timer" to R.id.cbTimer,
        "show_progress" to R.id.cbProgress,
        "show_percentage" to R.id.cbPercentage,
        "show_days" to R.id.cbDays,
        "show_hours" to R.id.cbHours,
        "show_minutes" to R.id.cbMinutes,
        "show_seconds" to R.id.cbSeconds
    )

    fun init() {
        toggles.forEach { (key, id) ->
            val switch = activity.findViewById<MaterialSwitch>(id)
            val isChecked = WidgetPreferencesManager.getToggle(
                context,
                appWidgetId,
                key,
                getDefaultState(key)
            )
            switch.isChecked = isChecked
            setupHaptic(switch)
            switch.setOnCheckedChangeListener { _, isCheckedNow ->
                WidgetPreferencesManager.saveToggle(context, appWidgetId, key, isCheckedNow)
                CountdownWidget.forceUpdateAll(context)
                refreshPreview()
                if (isTimerToggle(key)) {
                    val style = WidgetPreferencesManager.getTimeDisplayStyle(context, appWidgetId)
                    previewController.updateTimerText(
                        getSwitch("show_days").isChecked,
                        getSwitch("show_hours").isChecked,
                        getSwitch("show_minutes").isChecked,
                        getSwitch("show_seconds").isChecked,
                        style
                    )
                }
            }
        }
        refreshPreview()
    }

    fun saveAll() {
        toggles.forEach { (key, id) ->
            val switch = activity.findViewById<MaterialSwitch>(id)
            WidgetPreferencesManager.saveToggle(context, appWidgetId, key, switch.isChecked)
        }
    }

    fun resetToDefaultConfig() {
        val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val maxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minWidth)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minHeight)

        val widthDp = ((minWidth + maxWidth) / 2f)
        val heightDp = ((minHeight + maxHeight) / 2f)


        val config = WidgetLayoutManager.getAdaptiveLayoutConfig(widthDp, heightDp)

        val defaultStates = mapOf(
            "show_title" to config.showTitle,
            "show_date" to config.showDate,
            "show_icon" to config.showIcon,
            "show_timer" to config.showTimer,
            "show_progress" to config.showProgress,
            "show_percentage" to config.showPercent,
            "show_days" to true,
            "show_hours" to true,
            "show_minutes" to true,
            "show_seconds" to true
        )

        defaultStates.forEach { (key, defaultValue) ->
            WidgetPreferencesManager.saveToggle(context, appWidgetId, key, defaultValue)
            getSwitch(key).isChecked = defaultValue
        }

        val style = WidgetPreferencesManager.getTimeDisplayStyle(context, appWidgetId)
        previewController.updateTimerText(
            getSwitch("show_days").isChecked,
            getSwitch("show_hours").isChecked,
            getSwitch("show_minutes").isChecked,
            getSwitch("show_seconds").isChecked,
            style
        )
        refreshPreview()
        CountdownWidget.forceUpdateAll(context)
    }

    private fun getSwitch(key: String): MaterialSwitch {
        val id = toggles.first { it.first == key }.second
        return activity.findViewById(id)
    }

    private fun setupHaptic(switch: MaterialSwitch) {
        switch.setOnCheckedChangeListener { _, _ ->
            val prefs = context.getSharedPreferences("finaltick_prefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean("haptic_feedback", true)) {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(
                        VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                    )
                } else {
                    switch.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                }
            }
        }
    }

    private fun refreshPreview() {
        previewController.updateVisibility(
            showTitle = getSwitch("show_title").isChecked,
            showDate = getSwitch("show_date").isChecked,
            showTimer = getSwitch("show_timer").isChecked,
            showProgress = getSwitch("show_progress").isChecked,
            showPercent = getSwitch("show_percentage").isChecked,
            showIcon = getSwitch("show_icon").isChecked
        )
    }

    private fun getDefaultState(key: String): Boolean {
        val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val maxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minWidth)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minHeight)

        val widthDp = ((minWidth + maxWidth) / 2f)
        val heightDp = ((minHeight + maxHeight) / 2f)

        val config = WidgetLayoutManager.getAdaptiveLayoutConfig(widthDp, heightDp)

        return when (key) {
            "show_title" -> config.showTitle
            "show_date" -> config.showDate
            "show_icon" -> config.showIcon
            "show_timer" -> config.showTimer
            "show_progress" -> config.showProgress
            "show_percentage" -> config.showPercent
            else -> true
        }
    }

    private fun isTimerToggle(key: String): Boolean {
        return key in listOf("show_days", "show_hours", "show_minutes", "show_seconds")
    }
    fun applyTimeStyleConstraints(style: TimeDisplayStyle) {
        val keys = listOf("show_days", "show_hours", "show_minutes", "show_seconds")
        val switches = keys.map { getSwitch(it) }

        when (style) {
            TimeDisplayStyle.MINIMAL_PROGRESS -> {
                switches.forEach {
                    it.setOnCheckedChangeListener(null)
                    it.isChecked = false
                    it.isEnabled = false
                }
            }

            TimeDisplayStyle.VERBOSE_SINGLE, TimeDisplayStyle.COUNTDOWN_WORDS -> {
                switches.forEach { current ->
                    current.isEnabled = true
                    current.setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) {
                            switches.filter { it != current }.forEach {
                                it.setOnCheckedChangeListener(null)
                                it.isChecked = false
                                applyTimeStyleConstraints(style) // re-attach listeners
                            }
                        }
                    }
                }
            }

            else -> {
                switches.forEach {
                    it.setOnCheckedChangeListener(null)
                    it.isEnabled = true
                    // Restore listeners to update prefs/preview if needed
                    val key = keys[switches.indexOf(it)]
                    it.setOnCheckedChangeListener { _, isCheckedNow ->
                        WidgetPreferencesManager.saveToggle(context, appWidgetId, key, isCheckedNow)
                        CountdownWidget.forceUpdateAll(context)
                        refreshPreview()
                        if (isTimerToggle(key)) {
                            val style =
                                WidgetPreferencesManager.getTimeDisplayStyle(context, appWidgetId)
                            previewController.updateTimerText(
                                getSwitch("show_days").isChecked,
                                getSwitch("show_hours").isChecked,
                                getSwitch("show_minutes").isChecked,
                                getSwitch("show_seconds").isChecked,
                                style
                            )
                        }
                    }
                }
            }
        }
    }
}