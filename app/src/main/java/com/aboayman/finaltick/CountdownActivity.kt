package com.aboayman.finaltick

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CountdownActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_FinalTick)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_countdown)

        val prefs = getSharedPreferences("finaltick_prefs", Context.MODE_PRIVATE)
        val deadline = prefs.getLong("deadline_timestamp", -1L)

        if (deadline == -1L) {
            Toast.makeText(this, "No deadline set. Please start over.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupHeader()

        val countdownText = findViewById<TextView>(R.id.countdownText)
        val topTimer = findViewById<TextView>(R.id.tvCountdownTimer)
        val cbDays = findViewById<MaterialSwitch>(R.id.cbDays)
        val cbHours = findViewById<MaterialSwitch>(R.id.cbHours)
        val cbMinutes = findViewById<MaterialSwitch>(R.id.cbMinutes)
        val cbSeconds = findViewById<MaterialSwitch>(R.id.cbSeconds)
        val progressBar = findViewById<LinearProgressIndicator>(R.id.countdownProgressBar)
        val progressPercentText = findViewById<TextView>(R.id.tvCountdownProgressPercent)

        fun saveFormatPrefs() {
            prefs.edit()
                .putBoolean("widget_show_days", cbDays.isChecked)
                .putBoolean("widget_show_hours", cbHours.isChecked)
                .putBoolean("widget_show_minutes", cbMinutes.isChecked)
                .putBoolean("widget_show_seconds", cbSeconds.isChecked)
                .apply()
        }

        listOf(cbDays, cbHours, cbMinutes, cbSeconds).forEach { box ->
            box.setOnCheckedChangeListener { _, _ ->
                saveFormatPrefs()
                if (prefs.getBoolean("haptic_feedback", true)) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        val vibrator =
                            getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                        vibrator.vibrate(android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_HEAVY_CLICK))
                    } else {
                        box.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                    }
                }
            }
        }

        lifecycleScope.launch {
            while (true) {
                val timeLeft = deadline - System.currentTimeMillis()

                if (timeLeft <= 0) {
                    topTimer.text = "00 : 00 : 00 : 00"
                    countdownText.text = "\u23F3 Time's up!"
                    countdownText.setTextColor(
                        ContextCompat.getColor(
                            this@CountdownActivity,
                            R.color.colorDanger
                        )
                    )
                    progressBar.progress = 100
                    progressPercentText.text = "100%"
                    progressBar.setIndicatorColor(
                        ContextCompat.getColor(
                            this@CountdownActivity,
                            R.color.colorDanger
                        )
                    )
                    break
                }

                var remaining = timeLeft / 1000
                var days = 0L
                var hours = 0L
                var minutes = 0L
                var seconds = 0L

                val rawDays = remaining / (24 * 3600)
                val rawHours = (remaining % (24 * 3600)) / 3600
                val rawMinutes = (remaining % 3600) / 60
                val rawSeconds = remaining % 60

                topTimer.text = String.format("%02d : %02d : %02d : %02d", rawDays, rawHours, rawMinutes, rawSeconds)

                if (cbDays.isChecked) {
                    days = remaining / (24 * 3600); remaining %= (24 * 3600)
                }
                if (cbHours.isChecked) {
                    hours = remaining / 3600; remaining %= 3600
                } else {
                    remaining += hours * 3600
                }
                if (cbMinutes.isChecked) {
                    minutes = remaining / 60; remaining %= 60
                } else {
                    remaining += minutes * 60
                }
                if (cbSeconds.isChecked) {
                    seconds = remaining
                }

                if (!cbMinutes.isChecked && cbSeconds.isChecked) {
                    seconds += minutes * 60
                }
                if (!cbHours.isChecked && (cbMinutes.isChecked || cbSeconds.isChecked)) {
                    val bonus = hours * 3600
                    if (cbMinutes.isChecked) {
                        minutes += bonus / 60; seconds += bonus % 60
                    } else {
                        seconds += bonus
                    }
                }
                if (!cbDays.isChecked && (cbHours.isChecked || cbMinutes.isChecked || cbSeconds.isChecked)) {
                    val bonus = days * 86400
                    if (cbHours.isChecked) {
                        hours += bonus / 3600
                        val leftover = bonus % 3600
                        if (cbMinutes.isChecked) {
                            minutes += leftover / 60; seconds += leftover % 60
                        } else {
                            seconds += leftover
                        }
                    } else if (cbMinutes.isChecked) {
                        minutes += bonus / 60
                        seconds += bonus % 60
                    } else {
                        seconds += bonus
                    }
                }

                val parts = mutableListOf<String>()
                if (cbDays.isChecked) parts.add("${days} d")
                if (cbHours.isChecked) parts.add("${hours} h")
                if (cbMinutes.isChecked) parts.add("${minutes} m")
                if (cbSeconds.isChecked) parts.add("${seconds} s")

                countdownText.text = parts.joinToString(" ")

                val createdAt = prefs.getLong("countdown_createdAt", System.currentTimeMillis())
                val totalDuration = deadline - createdAt
                val elapsed = System.currentTimeMillis() - createdAt

                val progress = when {
                    totalDuration <= 0L -> 100
                    elapsed <= 0L -> 0
                    elapsed >= totalDuration -> 100
                    else -> ((elapsed * 100) / totalDuration).toInt()
                }

                progressBar.progress = progress
                progressPercentText.text = "$progress%"

                val colorRes = when (progress) {
                    in 0..24 -> R.color.progressSoftGreen    // 🌱 Very early
                    in 25..49 -> R.color.progressCyanBlue    // 🌊 Quarter to mid
                    in 50..74 -> R.color.progressAmber       // ⚡ Getting late
                    else -> R.color.colorDanger               // 🔥 Critical
                }
                progressBar.setIndicatorColor(
                    ContextCompat.getColor(
                        this@CountdownActivity,
                        colorRes
                    )
                )

                delay(1000)
            }
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_countdown

        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> {
                    startActivity(
                        Intent(
                            this,
                            MainActivity::class.java
                        )
                    ); overridePendingTransition(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left
                    ); finish(); true
                }

                R.id.nav_countdown -> true
                R.id.nav_calculate -> {
                    startActivity(
                        Intent(
                            this,
                            CalculateActivity::class.java
                        )
                    ); overridePendingTransition(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left
                    ); finish(); true
                }
                else -> false
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        })
    }

    private fun setupHeader() {
        val prefs = getSharedPreferences("finaltick_prefs", Context.MODE_PRIVATE)
        val title = prefs.getString("countdown_title", "Untitled Countdown")
        val deadline = prefs.getLong("deadline_timestamp", 0L)

        val dateFormatted = if (deadline != 0L) {
            val sdf = SimpleDateFormat("MMM dd, yyyy – hh:mm a", Locale.getDefault())
            sdf.format(Date(deadline))
        } else {
            "No date selected"
        }

        findViewById<TextView>(R.id.tvHeaderTitle).text = title
        findViewById<TextView>(R.id.tvHeaderDateTime).text = dateFormatted
    }
}