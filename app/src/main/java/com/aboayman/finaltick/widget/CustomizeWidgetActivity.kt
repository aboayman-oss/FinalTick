package com.aboayman.finaltick.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
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


    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_FinalTick)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.customize_widget)

        appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
            ?: AppWidgetManager.INVALID_APPWIDGET_ID

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
        val saveButton = findViewById<Button>(R.id.btnSaveCustomize)
        val btnChangeColor = findViewById<Button>(R.id.btnChangeColor)
        val btnResetAllColors = findViewById<Button>(R.id.btnResetAllColors)
        val btnResetDefaults = findViewById<Button>(R.id.btnResetDefaults)

        loadSwitchStates()

        listOf(cbDays, cbHours, cbMinutes, cbSeconds).forEach { setupToggleHaptic(it) }

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
        btnChangeColor.setOnClickListener {
            val items = arrayOf(
                "Title", "Date & Time", "Refresh Icon", "Timer", "Progress %"
            )

            val keys = arrayOf(
                "color_title", "color_date", "color_icon", "color_timer", "color_percentage"
            )

            var selectedIndex = -1

            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Choose Element to Customize")
                .setItems(items) { _, index ->
                    selectedIndex = index
                    val key = keys[index]
                    val currentColor = WidgetPreferencesManager.getColor(
                        this, appWidgetId, key, getColor(R.color.onSurface)
                    )

                    val colorDialog = yuku.ambilwarna.AmbilWarnaDialog(
                        this,
                        currentColor,
                        true,
                        object : yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener {
                            override fun onOk(
                                dialog: yuku.ambilwarna.AmbilWarnaDialog?,
                                color: Int
                            ) {
                                WidgetPreferencesManager.saveColor(
                                    this@CustomizeWidgetActivity,
                                    appWidgetId,
                                    key,
                                    color
                                )
                                CountdownWidget.forceUpdateAll(this@CustomizeWidgetActivity)
                                Toast.makeText(
                                    this@CustomizeWidgetActivity,
                                    "${items[index]} color updated",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            override fun onCancel(dialog: yuku.ambilwarna.AmbilWarnaDialog?) {}
                        }
                    )
                    colorDialog.show()
                }
                .setNeutralButton("Reset Selected to Default") { _, _ ->
                    if (selectedIndex in keys.indices) {
                        WidgetPreferencesManager.removeKey(this, appWidgetId, keys[selectedIndex])
                        CountdownWidget.forceUpdateAll(this)
                        Toast.makeText(
                            this,
                            "Reset ${items[selectedIndex]} color",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(this, "No item selected yet", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)

            builder.show()
        }
        btnResetAllColors.setOnClickListener {
            val colorKeys = arrayOf(
                "color_title", "color_date", "color_icon", "color_timer", "color_percentage"
            )

            colorKeys.forEach { key ->
                WidgetPreferencesManager.removeKey(this, appWidgetId, key)
            }

            // Force widget refresh
            CountdownWidget.forceUpdateAll(this)
            Toast.makeText(this, "All colors reset to defaults", Toast.LENGTH_SHORT).show()
        }
        btnResetDefaults.setOnClickListener {
            val sizeKey = WidgetLayoutManager.getGridSizeKey(this, appWidgetId)
            val defaultConfig = WidgetLayoutManager.getLayoutConfig(sizeKey)

            // Reset visibility toggles to LayoutConfig defaults
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

            // Reset static toggles
            WidgetPreferencesManager.saveToggle(this, appWidgetId, "show_days", true)
            WidgetPreferencesManager.saveToggle(this, appWidgetId, "show_hours", true)
            WidgetPreferencesManager.saveToggle(this, appWidgetId, "show_minutes", true)
            WidgetPreferencesManager.saveToggle(this, appWidgetId, "show_seconds", true)

            // Update UI and widget
            loadSwitchStates()
            CountdownWidget.forceUpdateWidget(this, appWidgetId)
            Toast.makeText(this, "Widget reset to defaults.", Toast.LENGTH_SHORT).show()
        }
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

        // These use static defaults since they aren’t part of LayoutConfig
        cbDays.isChecked = WidgetPreferencesManager.getToggle(this, appWidgetId, "show_days", true)
        cbHours.isChecked =
            WidgetPreferencesManager.getToggle(this, appWidgetId, "show_hours", true)
        cbMinutes.isChecked =
            WidgetPreferencesManager.getToggle(this, appWidgetId, "show_minutes", true)
        cbSeconds.isChecked =
            WidgetPreferencesManager.getToggle(this, appWidgetId, "show_seconds", true)

        CountdownWidget.forceUpdateWidget(this, appWidgetId)
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
}