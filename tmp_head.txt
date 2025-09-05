package com.aboayman.finaltick

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.aboayman.finaltick.databinding.ActivitySettingsBinding
import java.io.File

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private var userInteracted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("finaltick_prefs", MODE_PRIVATE)

        // Theme Spinner Setup
        val themeOptions = arrayOf("Follow System", "Light Mode", "Dark Mode")
        val adapter =
            android.widget.ArrayAdapter(this, R.layout.spinner_item, themeOptions)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.spinnerTheme.adapter = adapter

        binding.spinnerTheme.setOnTouchListener { v, _ ->
            userInteracted = true
            v.performClick()
            false
        }

        binding.spinnerTheme.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (!userInteracted) return
                when (position) {
                    0 -> setThemeMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    1 -> setThemeMode(AppCompatDelegate.MODE_NIGHT_NO)
                    2 -> setThemeMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Load Haptic Feedback setting
        val hapticEnabled = prefs.getBoolean("haptic_feedback", true)
        binding.switchHaptic.isChecked = hapticEnabled

        // Countdown defaults
        val weights = arrayOf("Light", "Regular", "Medium", "SemiBold", "Bold")
        val weightValues = intArrayOf(300, 400, 500, 600, 700)
        val weightAdapter =
            android.widget.ArrayAdapter(this, R.layout.spinner_item, weights).apply {
                setDropDownViewResource(R.layout.spinner_dropdown_item)
            }
        binding.spinnerTimerWeight.adapter = weightAdapter
        val currentWeight = prefs.getInt("countdown_timer_weight", 600)
        binding.spinnerTimerWeight.setSelection(
            weightValues.indexOf(currentWeight).coerceAtLeast(0)
        )
        binding.spinnerTimerWeight.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    prefs.edit().putInt("countdown_timer_weight", weightValues[position]).apply()
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.switchDynamicTimer.isChecked = prefs.getBoolean("countdown_dynamic_color", false)
        binding.switchDynamicTimer.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("countdown_dynamic_color", checked).apply()
        }

        binding.btnPickTimerColor.setOnClickListener {
            val initial = prefs.getInt(
                "countdown_timer_color",
                ContextCompat.getColor(this, R.color.colorPrimary)
            )
            val dialog = yuku.ambilwarna.AmbilWarnaDialog(
                this, initial, true,
                object : yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener {
                    override fun onOk(dialog: yuku.ambilwarna.AmbilWarnaDialog?, color: Int) {
                        prefs.edit().putInt("countdown_timer_color", color).apply()
                    }

                    override fun onCancel(dialog: yuku.ambilwarna.AmbilWarnaDialog?) {}
                })
            dialog.show()
        }        // 🔥 Handle Reset App
        binding.btnReset.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Confirm Reset")
                .setMessage("Are you sure you want to reset the app? This will erase all saved data.")
                .setPositiveButton("Yes") { _, _ ->
                    // User confirmed Reset
                    prefs.edit().clear().apply()
                    // Clear DataStore settings
                    kotlinx.coroutines.runBlocking {
                        com.aboayman.finaltick.SettingsManager.clearAll(this@SettingsActivity)
                    }
                    // Clear all widget-related SharedPreferences
                    val widgetPrefsDir = File(filesDir.parent, "shared_prefs")
                    widgetPrefsDir.listFiles()?.forEach { file ->
                        if (file.name.startsWith("widget_") && file.name.endsWith(".xml")) {
                            file.delete()
                        }
                    }

                    val widgetManager = AppWidgetManager.getInstance(this)
                    val component = ComponentName(this, CountdownWidget::class.java)
                    val widgetIds = widgetManager.getAppWidgetIds(component)

                    for (appWidgetId in widgetIds) {
                        CountdownWidget.resetWidget(this, appWidgetId)
                    }

                    // Now show success dialog
                    androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Reset Successful")
                        .setMessage("App reset completed. Do you want to restart the app now?")
                        .setPositiveButton("Yes") { _, _ ->
                            finishAffinity()
                        }
                        .setNegativeButton("No") { _, _ ->
                            Toast.makeText(this, "Reset completed!", Toast.LENGTH_SHORT).show()
                        }
                        .show()
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    private fun setThemeMode(mode: Int) {
        val prefs = getSharedPreferences("finaltick_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("theme_mode", mode).apply()

        AppCompatDelegate.setDefaultNightMode(mode)
        recreate()
    }
}
