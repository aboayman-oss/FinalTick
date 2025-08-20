package com.aboayman.finaltick

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.aboayman.finaltick.databinding.ActivityCalculateBinding
import com.aboayman.finaltick.databinding.ItemSummaryCardBinding
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CalculateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalculateBinding
    private lateinit var handler: Handler
    private lateinit var countdownRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_FinalTick)
        binding = ActivityCalculateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupHeader()
        setupListeners()
        setupNavigation()
        setupTimer()

        // Initial UI state setup
        binding.etCourseHours.setText("0")
        binding.sleepLabel.text = "Selected: ${binding.sleepSlider.value.toInt()} hours"
        binding.speedLabel.text = "Selected: ${binding.speedSlider.value}x"
        populateCardLabelsAndIcons()
        calculateStatic()
        refreshCountdowns()
    }

    private fun populateCardLabelsAndIcons() {
        binding.cardTimeUntilDeadline.cardLabel.text = "Time Until Deadline"
        binding.cardTimeUntilDeadline.cardIcon.setImageResource(R.drawable.timer)

        binding.cardSleep.cardLabel.text = "Total Sleep Time"
        binding.cardSleep.cardIcon.setImageResource(R.drawable.sleep)

        binding.cardCourse.cardLabel.text = "Total Course Time"
        binding.cardCourse.cardIcon.setImageResource(R.drawable.course)

        binding.cardSpeedGain.cardLabel.text = "Time Saved by Speed"
        binding.cardSpeedGain.cardIcon.setImageResource(R.drawable.fast)

        binding.cardSleepGain.cardIcon.setImageResource(R.drawable.insights)
    }

    private fun setupListeners() {
        var lastSleep = binding.sleepSlider.value.toInt()
        binding.sleepSlider.addOnChangeListener { slider, value, fromUser ->
            val newValue = value.toInt()
            if (fromUser && newValue != lastSleep) {
                triggerHapticFeedback(slider, android.view.HapticFeedbackConstants.CLOCK_TICK)
                lastSleep = newValue
            }
            binding.sleepLabel.text = "Selected: $newValue hours"
            calculateStatic()
        }

        var lastSpeedStep = (binding.speedSlider.value * 2).toInt() / 2f
        binding.speedSlider.addOnChangeListener { slider, value, fromUser ->
            val newValue = (value * 2).toInt() / 2f
            if (fromUser && newValue != lastSpeedStep) {
                lastSpeedStep = newValue
                triggerHapticFeedback(slider, android.view.HapticFeedbackConstants.LONG_PRESS)
            }
            binding.speedLabel.text = "Selected: ${value}x"
            calculateStatic()
        }

        binding.etCourseHours.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = calculateStatic()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun calculateStatic() {
        val sleepHours = binding.sleepSlider.value
        val courseHours = binding.etCourseHours.text.toString().toFloatOrNull() ?: 0f
        val playbackSpeed = binding.speedSlider.value

        val prefs = getSharedPreferences("finaltick_prefs", Context.MODE_PRIVATE)
        val deadline = prefs.getLong("deadline_timestamp", -1L)
        val now = System.currentTimeMillis()
        val millisUntil = (deadline - now).coerceAtLeast(0)

        val daysLeft = millisUntil.toFloat() / (1000 * 60 * 60 * 24)
        val totalSleepTime = sleepHours * daysLeft
        val adjustedCourseTime = if (playbackSpeed > 0) courseHours / playbackSpeed else 0f

        binding.cardSleep.cardValue.text = "%.0f hours".format(totalSleepTime)
        binding.cardCourse.cardValue.text = "%.1f hours".format(adjustedCourseTime)

        val sleepDiff = 8f - sleepHours

        // Update color of main sleep card
        val sleepColor = ContextCompat.getColor(
            this,
            if (sleepDiff > 0f) R.color.colorSuccess else if (sleepDiff < 0f) R.color.colorDanger else R.color.colorSurfaceContainer
        )
        binding.cardSleep.root.setCardBackgroundColor(sleepColor)

        // Update color of main course card
        val courseColor = ContextCompat.getColor(
            this,
            if (adjustedCourseTime < courseHours && courseHours > 0) R.color.colorSuccess else R.color.colorSurfaceContainer
        )
        binding.cardCourse.root.setCardBackgroundColor(courseColor)

        val sleepTextColor = ContextCompat.getColor(
            this,
            if (sleepColor != ContextCompat.getColor(
                    this,
                    R.color.colorSurfaceContainer
                )
            ) R.color.onSuccess else R.color.onSurface
        )
        binding.cardSleep.cardValue.setTextColor(sleepTextColor)
        binding.cardSleep.cardLabel.setTextColor(sleepTextColor)
        binding.cardSleep.cardIcon.setColorFilter(sleepTextColor)

        val courseTextColor = ContextCompat.getColor(
            this,
            if (courseColor != ContextCompat.getColor(
                    this,
                    R.color.colorSurfaceContainer
                )
            ) R.color.onSuccess else R.color.onSurface
        )
        binding.cardCourse.cardValue.setTextColor(courseTextColor)
        binding.cardCourse.cardLabel.setTextColor(courseTextColor)
        binding.cardCourse.cardIcon.setColorFilter(courseTextColor)

        // Logic for sleep gain/loss card
        val gainHours = kotlin.math.abs(sleepDiff * daysLeft)
        if (sleepDiff == 0f) {
            binding.cardSleepGain.root.visibility = View.GONE
        } else {
            binding.cardSleepGain.root.visibility = View.VISIBLE
            val gain = sleepDiff > 0f
            binding.cardSleepGain.cardValue.text =
                (if (gain) "+" else "-") + "%.1f hours".format(gainHours)
            binding.cardSleepGain.cardLabel.text =
                if (gain) "Time Gained by Sleeping Less" else "Time Lost by Oversleeping"
            updateCardAppearance(binding.cardSleepGain, gain)
        }

        // Logic for speed gain card
        if (playbackSpeed <= 1f || courseHours == 0f) {
            binding.cardSpeedGain.root.visibility = View.GONE
        } else {
            binding.cardSpeedGain.root.visibility = View.VISIBLE
            val speedGain = courseHours - adjustedCourseTime
            binding.cardSpeedGain.cardValue.text = "+%.1f hours".format(speedGain)
            updateCardAppearance(binding.cardSpeedGain, true)
        }
    }

    private fun refreshCountdowns() {
        val prefs = getSharedPreferences("finaltick_prefs", Context.MODE_PRIVATE)
        val deadline = prefs.getLong("deadline_timestamp", -1L)
        if (deadline == -1L) return

        val now = System.currentTimeMillis()
        val millisUntil = (deadline - now).coerceAtLeast(0)
        binding.cardTimeUntilDeadline.cardValue.text = formatHMS(millisUntil)

        val sleepHours = binding.sleepSlider.value
        val courseHours = binding.etCourseHours.text.toString().toFloatOrNull() ?: 0f
        val playbackSpeed = binding.speedSlider.value

        val daysLeft = millisUntil.toFloat() / (1000 * 60 * 60 * 24)
        val totalSleepTime = sleepHours * daysLeft
        val adjustedCourseTime = if (playbackSpeed > 0) courseHours / playbackSpeed else 0f

        val totalSleepMs = (totalSleepTime * 3600 * 1000).toLong()
        val courseMs = (adjustedCourseTime * 3600 * 1000).toLong()
        val rem = (millisUntil - totalSleepMs - courseMs).coerceAtLeast(0)

        binding.valueRemaining.text = formatHMS(rem)
    }

    private fun setupTimer() {
        handler = Handler(Looper.getMainLooper())
        countdownRunnable = object : Runnable {
            override fun run() {
                refreshCountdowns()
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(countdownRunnable)
    }

    private fun setupNavigation() {
        binding.bottomNav.selectedItemId = R.id.nav_calculate
        binding.bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> {
                    navigateTo(MainActivity::class.java)
                    true
                }
                R.id.nav_countdown -> {
                    navigateTo(CountdownActivity::class.java)
                    true
                }

                R.id.nav_calculate -> true
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

    private fun <T> navigateTo(activity: Class<T>) {
        if (this::class != activity::class) {
            startActivity(Intent(this, activity))
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            finish()
        }
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
        binding.header.tvHeaderTitle.text = title
        binding.header.tvHeaderDateTime.text = dateFormatted
    }

    private fun updateCardAppearance(cardBinding: ItemSummaryCardBinding, isPositive: Boolean) {
        val context = cardBinding.root.context
        val bgColor = ContextCompat.getColor(
            context,
            if (isPositive) R.color.colorSuccess else R.color.colorDanger
        )
        val fgColor =
            ContextCompat.getColor(context, if (isPositive) R.color.onSuccess else R.color.onDanger)

        (cardBinding.root as MaterialCardView).setCardBackgroundColor(bgColor)
        cardBinding.cardLabel.setTextColor(fgColor)
        cardBinding.cardValue.setTextColor(fgColor)
        cardBinding.cardIcon.setColorFilter(fgColor)
    }

    private fun formatHMS(ms: Long): String {
        val totalSeconds = ms.coerceAtLeast(0) / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun triggerHapticFeedback(view: View, feedbackType: Int) {
        val prefs = getSharedPreferences("finaltick_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("haptic_feedback", true)) {
            view.performHapticFeedback(feedbackType)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(countdownRunnable)
    }
}