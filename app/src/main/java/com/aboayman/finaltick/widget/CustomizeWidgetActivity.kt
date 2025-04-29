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
        val saveButton = findViewById<Button>(R.id.btnSaveCustomize)

        loadSwitchStates()

        listOf(cbDays, cbHours, cbMinutes, cbSeconds).forEach { setupToggleHaptic(it) }

        saveButton.setOnClickListener {
            WidgetPreferencesManager.saveToggle(this, appWidgetId, "show_days", cbDays.isChecked)
            WidgetPreferencesManager.saveToggle(this, appWidgetId, "show_hours", cbHours.isChecked)
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
    }

    private fun loadSwitchStates() {
        cbDays.isChecked = WidgetPreferencesManager.getToggle(this, appWidgetId, "show_days")
        cbHours.isChecked = WidgetPreferencesManager.getToggle(this, appWidgetId, "show_hours")
        cbMinutes.isChecked = WidgetPreferencesManager.getToggle(this, appWidgetId, "show_minutes")
        cbSeconds.isChecked = WidgetPreferencesManager.getToggle(this, appWidgetId, "show_seconds")
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