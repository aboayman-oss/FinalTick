package com.aboayman.finaltick

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.slider.Slider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CalculateActivity : AppCompatActivity() {

    private lateinit var handler: Handler
    private lateinit var countdownRunnable: Runnable

    private lateinit var valueTimeUntil: TextView
    private lateinit var valueRemaining: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_FinalTick)
        setContentView(R.layout.activity_calculate)

        setupHeader()

        // Bind views
        valueTimeUntil = findViewById(R.id.valueTimeUntil)
        valueRemaining = findViewById(R.id.valueRemaining)

        val sleepSlider = findViewById<Slider>(R.id.sleepSlider)
        val sleepLabel = findViewById<TextView>(R.id.sleepLabel)
        val etCourseHours = findViewById<EditText>(R.id.etCourseHours)
        val speedSlider = findViewById<Slider>(R.id.speedSlider)
        val speedLabel = findViewById<TextView>(R.id.speedLabel)

        var lastSleep = sleepSlider.value.toInt()
        var lastSpeed = speedSlider.value.toInt()

        val valueSleep = findViewById<TextView>(R.id.valueSleep)
        val valueCourse = findViewById<TextView>(R.id.valueCourse)
        val valueSleepGain = findViewById<TextView>(R.id.valueSleepGain)
        val valueSpeedGain = findViewById<TextView>(R.id.valueSpeedGain)

        val cardSleepGain = findViewById<MaterialCardView>(R.id.cardSleepGain)
        val cardSpeedGain = findViewById<MaterialCardView>(R.id.cardSpeedGain)
        val iconSleepGain = findViewById<MaterialCardView>(R.id.iconSleepGain)
        val iconSpeedGain = findViewById<MaterialCardView>(R.id.iconSpeedGain)
        val labelSleepGain = findViewById<TextView>(R.id.labelSleepGain)
        val labelSpeedGain = findViewById<TextView>(R.id.labelSpeedGain)

        etCourseHours.setText("0")

        fun formatHMS(ms: Long): String {
            val safeMs = ms.coerceAtLeast(0)
            val totalSeconds = safeMs / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }

        fun refreshCountdowns() {
            val prefs = getSharedPreferences("finaltick_prefs", Context.MODE_PRIVATE)
            val deadline = prefs.getLong("deadline_timestamp", -1L)
            if (deadline == -1L) return

            val now = System.currentTimeMillis()
            val millisUntil = (deadline - now).coerceAtLeast(0)
            valueTimeUntil.text = formatHMS(millisUntil)

            val sleepHours = sleepSlider.value.toInt()
            val courseHours = etCourseHours.text.toString().toFloatOrNull() ?: 0f
            val playbackSpeed = speedSlider.value

            val daysLeft = millisUntil.toFloat() / (1000 * 60 * 60 * 24)
            val totalSleepTime = sleepHours * daysLeft
            val adjustedCourseTime = courseHours / playbackSpeed

            val totalSleepMs = (totalSleepTime * 3600 * 1000).toLong()
            val courseMs = (adjustedCourseTime * 3600 * 1000).toLong()
            val rem = (millisUntil - totalSleepMs - courseMs).coerceAtLeast(0)

            valueRemaining.text = formatHMS(rem)
            val remainingColor = ContextCompat.getColor(
                this,
                if (rem > 0) R.color.onSuccess else R.color.colorDanger
            )
            valueRemaining.setTextColor(remainingColor)
        }

        fun calculateStatic() {
            val sleepHours = sleepSlider.value.toInt()
            val courseHours = etCourseHours.text.toString().toFloatOrNull() ?: 0f
            val playbackSpeed = speedSlider.value

            val prefs = getSharedPreferences("finaltick_prefs", Context.MODE_PRIVATE)
            val deadline = prefs.getLong("deadline_timestamp", -1L)
            val now = System.currentTimeMillis()
            val millisUntil = (deadline - now).coerceAtLeast(0)

            val daysLeft = millisUntil.toFloat() / (1000 * 60 * 60 * 24)
            val totalSleepTime = sleepHours * daysLeft
            val adjustedCourseTime = courseHours / playbackSpeed

            valueSleep.text = "%.0f hours".format(totalSleepTime)
            valueCourse.text = "%.1f hours".format(adjustedCourseTime)

            val sleepDiff = 8f - sleepHours
            if (sleepDiff == 0f) {
                cardSleepGain.visibility = View.GONE
                iconSleepGain.visibility = View.GONE
            } else {
                cardSleepGain.visibility = View.VISIBLE
                iconSleepGain.visibility = View.VISIBLE

                val gain = sleepDiff > 0f
                val gainHours = kotlin.math.abs(sleepDiff * daysLeft)
                valueSleepGain.text = (if (gain) "+" else "-") + "%.1f hours".format(gainHours)
                labelSleepGain.text = if (gain) "Time Gained by Sleeping Less" else "Time Lost by Oversleeping"

                val bgColor = ContextCompat.getColor(
                    this,
                    if (gain) R.color.colorSuccess else R.color.colorDanger
                )
                cardSleepGain.setCardBackgroundColor(bgColor)

                val fgColor = ContextCompat.getColor(this, R.color.onSuccess)
                labelSleepGain.setTextColor(fgColor)
                valueSleepGain.setTextColor(fgColor)
            }

            if (playbackSpeed <= 1f) {
                cardSpeedGain.visibility = View.GONE
                iconSpeedGain.visibility = View.GONE
            } else {
                cardSpeedGain.visibility = View.VISIBLE
                iconSpeedGain.visibility = View.VISIBLE

                val speedGain = courseHours - adjustedCourseTime
                valueSpeedGain.text = "+%.1f hours".format(speedGain)

                val green = ContextCompat.getColor(this, R.color.colorSuccess)
                val white = ContextCompat.getColor(this, R.color.onSuccess)

                cardSpeedGain.setCardBackgroundColor(green)
                labelSpeedGain.setTextColor(white)
                valueSpeedGain.setTextColor(white)
            }
        }

        handler = Handler(Looper.getMainLooper())
        countdownRunnable = object : Runnable {
            override fun run() {
                refreshCountdowns()
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(countdownRunnable)

        sleepSlider.addOnChangeListener { slider, value, fromUser ->
            val newValue = value.toInt()
            if (fromUser && newValue != lastSleep) {
                slider.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                lastSleep = newValue
            }
            sleepLabel.text = "Selected: $newValue hours"
            calculateStatic()
        }
        var lastSpeedStep = (speedSlider.value * 2).toInt() / 2f

        speedSlider.addOnChangeListener { slider, value, fromUser ->
            val newValue = (value * 2).toInt() / 2f
            if (fromUser && newValue != lastSpeedStep) {
                lastSpeedStep = newValue

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                    vibrator.vibrate(android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_HEAVY_CLICK))
                } else {
                    slider.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                }
            }
            speedLabel.text = "Selected: ${value}x"
            calculateStatic()
        }

        etCourseHours.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = calculateStatic()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        sleepLabel.text = "Selected: ${sleepSlider.value.toInt()} hours"
        speedLabel.text = "Selected: ${speedSlider.value}x"
        calculateStatic()
        refreshCountdowns()

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_calculate

        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> {
                    if (this::class != MainActivity::class) {
                        startActivity(Intent(this, MainActivity::class.java))
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                        finish()
                    }
                    true
                }
                R.id.nav_countdown -> {
                    if (this::class != CountdownActivity::class) {
                        startActivity(Intent(this, CountdownActivity::class.java))
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                        finish()
                    }
                    true
                }
                R.id.nav_calculate -> {
                    if (this::class != CalculateActivity::class) {
                        startActivity(Intent(this, CalculateActivity::class.java))
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                        finish()
                    }
                    true
                }
                else -> false
            }
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
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

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(countdownRunnable)
    }
}