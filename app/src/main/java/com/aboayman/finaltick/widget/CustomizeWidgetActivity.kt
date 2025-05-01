package com.aboayman.finaltick.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aboayman.finaltick.CountdownWidget
import com.aboayman.finaltick.R
import com.google.android.material.materialswitch.MaterialSwitch

class CustomizeWidgetActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var cbDays: MaterialSwitch
    private lateinit var cbHours: MaterialSwitch
    private lateinit var cbMinutes: MaterialSwitch
    private lateinit var cbSeconds: MaterialSwitch
    private lateinit var cbTitle: MaterialSwitch
    private lateinit var cbDate: MaterialSwitch
    private lateinit var cbIcon: MaterialSwitch
    private lateinit var cbTimer: MaterialSwitch
    private lateinit var cbProgress: MaterialSwitch
    private lateinit var cbPercentage: MaterialSwitch
    private lateinit var previewController: FakeWidgetPreviewController
    private lateinit var colorPreviewTitle: View
    private lateinit var colorPreviewDate: View
    private lateinit var colorPreviewIcon: View
    private lateinit var colorPreviewTimer: View
    private lateinit var colorPreviewPercentage: View
    private lateinit var btnResetTitleColor: Button
    private lateinit var btnResetDateColor: Button
    private lateinit var btnResetIconColor: Button
    private lateinit var btnResetTimerColor: Button
    private lateinit var btnResetPercentageColor: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_FinalTick)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.customize_widget)

        previewController = FakeWidgetPreviewController(this)

        appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        cbDays = findViewById(R.id.cbDays)
        cbHours = findViewById(R.id.cbHours)
        cbMinutes = findViewById(R.id.cbMinutes)
        cbSeconds = findViewById(R.id.cbSeconds)
        cbTitle = findViewById(R.id.cbTitle)
        cbDate = findViewById(R.id.cbDate)
        cbIcon = findViewById(R.id.cbIcon)
        cbTimer = findViewById(R.id.cbTimer)
        cbProgress = findViewById(R.id.cbProgress)
        cbPercentage = findViewById(R.id.cbPercentage)

        colorPreviewTitle = findViewById(R.id.colorPreviewTitle)
        colorPreviewDate = findViewById(R.id.colorPreviewDate)
        colorPreviewIcon = findViewById(R.id.colorPreviewIcon)
        colorPreviewTimer = findViewById(R.id.colorPreviewTimer)
        colorPreviewPercentage = findViewById(R.id.colorPreviewPercentage)

        btnResetTitleColor = findViewById(R.id.btnResetTitleColor)
        btnResetDateColor = findViewById(R.id.btnResetDateColor)
        btnResetIconColor = findViewById(R.id.btnResetIconColor)
        btnResetTimerColor = findViewById(R.id.btnResetTimerColor)
        btnResetPercentageColor = findViewById(R.id.btnResetPercentageColor)

        val saveButton = findViewById<Button>(R.id.btnSaveCustomize)
        val btnResetAllColors = findViewById<Button>(R.id.btnResetAllColors)
        val btnResetDefaults = findViewById<Button>(R.id.btnResetDefaults)

        loadSwitchStates()
        refreshPreviewVisibility()
        previewController.updateTimerText(
            cbDays.isChecked,
            cbHours.isChecked,
            cbMinutes.isChecked,
            cbSeconds.isChecked
        )

        fun updateColorPreview(view: View, color: Int) {
            view.background.setTint(color)
        }

        updateColorPreview(
            colorPreviewTitle,
            WidgetPreferencesManager.getColor(
                this,
                appWidgetId,
                "color_title",
                getColor(R.color.onSurface)
            )
        )
        updateColorPreview(
            colorPreviewDate,
            WidgetPreferencesManager.getColor(
                this,
                appWidgetId,
                "color_date",
                getColor(R.color.onSurface)
            )
        )
        updateColorPreview(
            colorPreviewIcon,
            WidgetPreferencesManager.getColor(
                this,
                appWidgetId,
                "color_icon",
                getColor(R.color.onSurface)
            )
        )
        updateColorPreview(
            colorPreviewTimer,
            WidgetPreferencesManager.getColor(
                this,
                appWidgetId,
                "color_timer",
                getColor(R.color.onSurface)
            )
        )
        updateColorPreview(
            colorPreviewPercentage,
            WidgetPreferencesManager.getColor(
                this,
                appWidgetId,
                "color_percentage",
                getColor(R.color.onSurface)
            )
        )

        fun openColorPicker(key: String, previewView: View, apply: () -> Unit) {
            val currentColor = WidgetPreferencesManager.getColor(
                this,
                appWidgetId,
                key,
                getColor(R.color.onSurface)
            )

            com.github.dhaval2404.colorpicker.ColorPickerDialog
                .Builder(this)
                .setTitle("Pick a color")
                .setColorShape(com.github.dhaval2404.colorpicker.model.ColorShape.SQAURE)
                .setDefaultColor(currentColor)
                .setColorListener { color, _ ->
                    WidgetPreferencesManager.saveColor(this, appWidgetId, key, color)
                    previewView.background.setTint(color)
                    apply()
                    CountdownWidget.forceUpdateWidget(this, appWidgetId)
                }
                .setNegativeButton("Cancel")
                .show()
        }

        colorPreviewTitle.setOnClickListener {
            openColorPicker("color_title", colorPreviewTitle) {
                previewController.applyColors(
                    titleColor = WidgetPreferencesManager.getColor(
                        this,
                        appWidgetId,
                        "color_title",
                        getColor(R.color.onSurface)
                    ),
                    dateColor = null, timerColor = null, percentColor = null, iconTint = null
                )
            }
        }

        colorPreviewDate.setOnClickListener {
            openColorPicker("color_date", colorPreviewDate) {
                previewController.applyColors(
                    titleColor = null,
                    dateColor = WidgetPreferencesManager.getColor(
                        this,
                        appWidgetId,
                        "color_date",
                        getColor(R.color.onSurface)
                    ),
                    timerColor = null, percentColor = null, iconTint = null
                )
            }
        }

        colorPreviewIcon.setOnClickListener {
            openColorPicker("color_icon", colorPreviewIcon) {
                previewController.applyColors(
                    titleColor = null, dateColor = null, timerColor = null, percentColor = null,
                    iconTint = WidgetPreferencesManager.getColor(
                        this,
                        appWidgetId,
                        "color_icon",
                        getColor(R.color.onSurface)
                    )
                )
            }
        }

        colorPreviewTimer.setOnClickListener {
            openColorPicker("color_timer", colorPreviewTimer) {
                previewController.applyColors(
                    titleColor = null, dateColor = null,
                    timerColor = WidgetPreferencesManager.getColor(
                        this,
                        appWidgetId,
                        "color_timer",
                        getColor(R.color.onSurface)
                    ),
                    percentColor = null, iconTint = null
                )
            }
        }

        colorPreviewPercentage.setOnClickListener {
            openColorPicker("color_percentage", colorPreviewPercentage) {
                previewController.applyColors(
                    titleColor = null, dateColor = null, timerColor = null,
                    percentColor = WidgetPreferencesManager.getColor(
                        this,
                        appWidgetId,
                        "color_percentage",
                        getColor(R.color.onSurface)
                    ),
                    iconTint = null
                )
            }
        }

        btnResetTitleColor.setOnClickListener {
            WidgetPreferencesManager.removeKey(this, appWidgetId, "color_title")
            val defaultColor = getColor(R.color.onSurface)
            updateColorPreview(colorPreviewTitle, defaultColor)
            previewController.applyColors(titleColor = defaultColor, null, null, null, null)
            CountdownWidget.forceUpdateAll(this)
        }

        btnResetDateColor.setOnClickListener {
            WidgetPreferencesManager.removeKey(this, appWidgetId, "color_date")
            val defaultColor = getColor(R.color.onSurface)
            updateColorPreview(colorPreviewDate, defaultColor)
            previewController.applyColors(null, dateColor = defaultColor, null, null, null)
            CountdownWidget.forceUpdateAll(this)
        }

        btnResetIconColor.setOnClickListener {
            WidgetPreferencesManager.removeKey(this, appWidgetId, "color_icon")
            val defaultColor = getColor(R.color.onSurface)
            updateColorPreview(colorPreviewIcon, defaultColor)
            previewController.applyColors(null, null, null, null, iconTint = defaultColor)
            CountdownWidget.forceUpdateAll(this)
        }

        btnResetTimerColor.setOnClickListener {
            WidgetPreferencesManager.removeKey(this, appWidgetId, "color_timer")
            val defaultColor = getColor(R.color.onSurface)
            updateColorPreview(colorPreviewTimer, defaultColor)
            previewController.applyColors(null, null, timerColor = defaultColor, null, null)
            CountdownWidget.forceUpdateAll(this)
        }

        btnResetPercentageColor.setOnClickListener {
            WidgetPreferencesManager.removeKey(this, appWidgetId, "color_percentage")
            val defaultColor = getColor(R.color.onSurface)
            updateColorPreview(colorPreviewPercentage, defaultColor)
            previewController.applyColors(null, null, null, percentColor = defaultColor, null)
            CountdownWidget.forceUpdateAll(this)
        }
        fun setupVisibilityToggleLiveUpdate(switch: MaterialSwitch, key: String) {
            switch.setOnCheckedChangeListener { _, isChecked ->
                WidgetPreferencesManager.saveToggle(this, appWidgetId, key, isChecked)
                refreshPreviewVisibility()
                CountdownWidget.forceUpdateAll(this)
                Toast.makeText(this, "$key updated", Toast.LENGTH_SHORT).show()
            }
        }

        setupVisibilityToggleLiveUpdate(cbTitle, "show_title")
        setupVisibilityToggleLiveUpdate(cbDate, "show_date")
        setupVisibilityToggleLiveUpdate(cbIcon, "show_icon")
        setupVisibilityToggleLiveUpdate(cbTimer, "show_timer")
        setupVisibilityToggleLiveUpdate(cbProgress, "show_progress")
        setupVisibilityToggleLiveUpdate(cbPercentage, "show_percentage")

        setupToggleLiveUpdate(cbDays, "show_days")
        setupToggleLiveUpdate(cbHours, "show_hours")
        setupToggleLiveUpdate(cbMinutes, "show_minutes")
        setupToggleLiveUpdate(cbSeconds, "show_seconds")

        // Still apply haptic feedback if needed
        listOf(
            cbDays, cbHours, cbMinutes, cbSeconds,
            cbTitle, cbDate, cbIcon, cbTimer, cbProgress, cbPercentage
        ).forEach { setupToggleHaptic(it) }

        saveButton.setOnClickListener {
            WidgetPreferencesManager.saveToggle(this, appWidgetId, "show_days", cbDays.isChecked)
            WidgetPreferencesManager.saveToggle(this, appWidgetId, "show_hours", cbHours.isChecked)
            WidgetPreferencesManager.saveToggle(this, appWidgetId, "show_title", cbTitle.isChecked)
            WidgetPreferencesManager.saveToggle(this, appWidgetId, "show_date", cbDate.isChecked)
            WidgetPreferencesManager.saveToggle(this, appWidgetId, "show_icon", cbIcon.isChecked)
            WidgetPreferencesManager.saveToggle(this, appWidgetId, "show_timer", cbTimer.isChecked)
            WidgetPreferencesManager.saveToggle(
                this,
                appWidgetId,
                "show_progress",
                cbProgress.isChecked
            )
            WidgetPreferencesManager.saveToggle(
                this,
                appWidgetId,
                "show_percentage",
                cbPercentage.isChecked
            )
            WidgetPreferencesManager.saveToggle(
                this,
                appWidgetId,
                "show_minutes",
                cbMinutes.isChecked
            )
            WidgetPreferencesManager.saveToggle(
                this,
                appWidgetId,
                "show_seconds",
                cbSeconds.isChecked
            )

            CountdownWidget.forceUpdateAll(this)
            Toast.makeText(this, "Customization saved!", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnResetAllColors.setOnClickListener {
            val colorKeys = arrayOf(
                "color_title",
                "color_date",
                "color_icon",
                "color_timer",
                "color_percentage"
            )
            colorKeys.forEach { key -> WidgetPreferencesManager.removeKey(this, appWidgetId, key) }
            CountdownWidget.forceUpdateAll(this)
            Toast.makeText(this, "All colors reset to defaults", Toast.LENGTH_SHORT).show()
        }

        btnResetDefaults.setOnClickListener {
            val sizeKey = WidgetLayoutManager.getGridSizeKey(this, appWidgetId)
            val defaultConfig = WidgetLayoutManager.getLayoutConfig(sizeKey)

            WidgetPreferencesManager.saveToggle(
                this,
                appWidgetId,
                "show_title",
                defaultConfig.showTitle
            )
            WidgetPreferencesManager.saveToggle(
                this,
                appWidgetId,
                "show_date",
                defaultConfig.showDate
            )
            WidgetPreferencesManager.saveToggle(
                this,
                appWidgetId,
                "show_icon",
                defaultConfig.showIcon
            )
            WidgetPreferencesManager.saveToggle(
                this,
                appWidgetId,
                "show_timer",
                defaultConfig.showTimer
            )
            WidgetPreferencesManager.saveToggle(
                this,
                appWidgetId,
                "show_progress",
                defaultConfig.showProgress
            )
            WidgetPreferencesManager.saveToggle(
                this,
                appWidgetId,
                "show_percentage",
                defaultConfig.showPercent
            )
            WidgetPreferencesManager.saveToggle(this, appWidgetId, "show_days", true)
            WidgetPreferencesManager.saveToggle(this, appWidgetId, "show_hours", true)
            WidgetPreferencesManager.saveToggle(this, appWidgetId, "show_minutes", true)
            WidgetPreferencesManager.saveToggle(this, appWidgetId, "show_seconds", true)

            loadSwitchStates()
            refreshPreviewVisibility()
            previewController.applyColors(
                titleColor = WidgetPreferencesManager.getColor(
                    this,
                    appWidgetId,
                    "color_title",
                    getColor(R.color.onSurface)
                ),
                dateColor = WidgetPreferencesManager.getColor(
                    this,
                    appWidgetId,
                    "color_date",
                    getColor(R.color.onSurface)
                ),
                timerColor = WidgetPreferencesManager.getColor(
                    this,
                    appWidgetId,
                    "color_timer",
                    getColor(R.color.onSurface)
                ),
                percentColor = WidgetPreferencesManager.getColor(
                    this,
                    appWidgetId,
                    "color_percentage",
                    getColor(R.color.onSurface)
                ),
                iconTint = WidgetPreferencesManager.getColor(
                    this,
                    appWidgetId,
                    "color_icon",
                    getColor(R.color.onSurface)
                )
            )
            CountdownWidget.forceUpdateAll(this)
            Toast.makeText(this, "Widget reset to defaults.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshPreviewVisibility() {
        previewController.updateVisibility(
            showTitle = cbTitle.isChecked,
            showDate = cbDate.isChecked,
            showTimer = cbTimer.isChecked,
            showProgress = cbProgress.isChecked,
            showPercent = cbPercentage.isChecked,
            showIcon = cbIcon.isChecked
        )
    }

    private fun loadSwitchStates() {
        val sizeKey = WidgetLayoutManager.getGridSizeKey(this, appWidgetId)
        val defaultConfig = WidgetLayoutManager.getLayoutConfig(sizeKey)

        cbTitle.isChecked = WidgetPreferencesManager.getToggle(
            this,
            appWidgetId,
            "show_title",
            defaultConfig.showTitle
        )
        cbDate.isChecked = WidgetPreferencesManager.getToggle(
            this,
            appWidgetId,
            "show_date",
            defaultConfig.showDate
        )
        cbIcon.isChecked = WidgetPreferencesManager.getToggle(
            this,
            appWidgetId,
            "show_icon",
            defaultConfig.showIcon
        )
        cbTimer.isChecked = WidgetPreferencesManager.getToggle(
            this,
            appWidgetId,
            "show_timer",
            defaultConfig.showTimer
        )
        cbProgress.isChecked = WidgetPreferencesManager.getToggle(
            this,
            appWidgetId,
            "show_progress",
            defaultConfig.showProgress
        )
        cbPercentage.isChecked = WidgetPreferencesManager.getToggle(
            this,
            appWidgetId,
            "show_percentage",
            defaultConfig.showPercent
        )
        cbDays.isChecked = WidgetPreferencesManager.getToggle(this, appWidgetId, "show_days", true)
        cbHours.isChecked =
            WidgetPreferencesManager.getToggle(this, appWidgetId, "show_hours", true)
        cbMinutes.isChecked =
            WidgetPreferencesManager.getToggle(this, appWidgetId, "show_minutes", true)
        cbSeconds.isChecked =
            WidgetPreferencesManager.getToggle(this, appWidgetId, "show_seconds", true)

        CountdownWidget.forceUpdateAll(this)
    }

    private fun setupToggleHaptic(switch: MaterialSwitch) {
        switch.setOnCheckedChangeListener { _, _ ->
            val prefs = getSharedPreferences("finaltick_prefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean("haptic_feedback", true)) {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                } else {
                    switch.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                }
            }
        }
    }
    private fun setupToggleLiveUpdate(switch: MaterialSwitch, key: String) {
        switch.setOnCheckedChangeListener { _, isChecked ->
            WidgetPreferencesManager.saveToggle(this, appWidgetId, key, isChecked)

            previewController.updateTimerText(
                cbDays.isChecked, cbHours.isChecked,
                cbMinutes.isChecked, cbSeconds.isChecked
            )

            CountdownWidget.forceUpdateAll(this)

            Toast.makeText(this, "$key updated", Toast.LENGTH_SHORT).show()
        }
    }
}
