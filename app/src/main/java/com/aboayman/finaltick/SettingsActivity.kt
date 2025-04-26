package com.aboayman.finaltick

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.aboayman.finaltick.databinding.ActivitySettingsBinding

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
            android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, themeOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTheme.adapter = adapter

        binding.spinnerTheme.setOnTouchListener { _, _ ->
            userInteracted = true
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

        binding.switchHaptic.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("haptic_feedback", isChecked).apply()
        }

        // 🔥 Handle Reset App
        binding.btnReset.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Confirm Reset")
                .setMessage("Are you sure you want to reset the app? This will erase all saved data.")
                .setPositiveButton("Yes") { _, _ ->
                    // User confirmed Reset
                    prefs.edit().clear().apply()

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