package com.aboayman.finaltick

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aboayman.finaltick.databinding.ActivityCalculateBinding
import com.aboayman.finaltick.databinding.ItemSummaryCardBinding
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CalculateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalculateBinding
    private val viewModel: CalculateViewModel by viewModels()
    private var animateOnNextUserChange: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_FinalTick)
        binding = ActivityCalculateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbarAndHeaderM3()
        if (viewModel.getActiveTimestamp() == -1L) {
            android.widget.Toast.makeText(
                this,
                getString(R.string.calc_no_deadline),
                android.widget.Toast.LENGTH_LONG
            ).show()
            binding.sleepSlider.isEnabled = false
            binding.speedSlider.isEnabled = false
            binding.etCourseHours.isEnabled = false
        }
        setupListeners()
        setupNavigation()
        observeViewModel()

        // Initial UI state setup handled in observeViewModel()
        populateCardLabelsAndIcons()
    }

    private fun populateCardLabelsAndIcons() {
        binding.cardTimeUntilDeadline.cardLabel.text = getString(R.string.calc_time_until_deadline)
        binding.cardTimeUntilDeadline.cardIcon.setImageResource(R.drawable.timer)

        binding.cardSleep.cardLabel.text = getString(R.string.calc_total_sleep_time)
        binding.cardSleep.cardIcon.setImageResource(R.drawable.sleep)

        binding.cardCourse.cardLabel.text = getString(R.string.calc_total_course_time)
        binding.cardCourse.cardIcon.setImageResource(R.drawable.course)

        binding.cardSpeedGain.cardLabel.text = getString(R.string.calc_time_saved_by_speed)
        binding.cardSpeedGain.cardIcon.setImageResource(R.drawable.fast)

        binding.cardSleepGain.cardIcon.setImageResource(R.drawable.insights)
    }

    private fun setupListeners() {
        var lastSleep = binding.sleepSlider.value.toInt()
        binding.sleepSlider.addOnChangeListener { slider, value, fromUser ->
            val newValue = value.toInt()
            if (fromUser && newValue != lastSleep) {
                Haptics.perform(this, slider, android.view.HapticFeedbackConstants.CLOCK_TICK)
                lastSleep = newValue
                animateOnNextUserChange = true
            }
            binding.sleepLabel.text = getString(R.string.calc_sleep_selected, newValue)
            viewModel.setSleepHours(newValue)
        }

        var lastSpeedStep = (binding.speedSlider.value * 2).toInt() / 2f
        binding.speedSlider.addOnChangeListener { slider, value, fromUser ->
            val newValue = (value * 2).toInt() / 2f
            if (fromUser && newValue != lastSpeedStep) {
                lastSpeedStep = newValue
                Haptics.perform(this, slider, android.view.HapticFeedbackConstants.LONG_PRESS)
                animateOnNextUserChange = true
            }
            binding.speedLabel.text = getString(R.string.calc_speed_selected, newValue)
            viewModel.setPlaybackSpeed(newValue)
        }

        // Quick speed chips
        fun setSpeed(value: Float) {
            val stepped = (value * 2).toInt() / 2f
            if (binding.speedSlider.value != stepped) {
                binding.speedSlider.value = stepped
            }
            binding.speedLabel.text = getString(R.string.calc_speed_selected, stepped)
            viewModel.setPlaybackSpeed(stepped)
            animateOnNextUserChange = true
            // reflect selection in chips
            binding.chipSpeed1.isChecked = stepped == 1.0f
            binding.chipSpeed15.isChecked = stepped == 1.5f
            binding.chipSpeed2.isChecked = stepped == 2.0f
        }

        binding.chipSpeed1.setOnClickListener {
            Haptics.perform(this, it, android.view.HapticFeedbackConstants.LONG_PRESS)
            setSpeed(1.0f)
        }
        binding.chipSpeed15.setOnClickListener {
            Haptics.perform(this, it, android.view.HapticFeedbackConstants.LONG_PRESS)
            setSpeed(1.5f)
        }
        binding.chipSpeed2.setOnClickListener {
            Haptics.perform(this, it, android.view.HapticFeedbackConstants.LONG_PRESS)
            setSpeed(2.0f)
        }

        binding.etCourseHours.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val v = binding.etCourseHours.text.toString().toFloatOrNull() ?: 0f
                viewModel.setCourseHours(v)
                animateOnNextUserChange = true
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupToolbarAndHeaderM3() {
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        // Start with no toolbar title to avoid overlap with header when expanded
        supportActionBar?.title = ""
        val title = viewModel.getActiveTitle()
        val deadline = viewModel.getActiveTimestamp()
        val dateFormatted = if (deadline != 0L && deadline != -1L) {
            val sdf = SimpleDateFormat("MMM dd, yyyy '•' hh:mm a", Locale.getDefault())
            sdf.format(Date(deadline))
        } else {
            getString(R.string.calc_no_date_selected)
        }
        val tvTitle = findViewById<android.widget.TextView>(R.id.tvHeaderDeadlineTitle)
        val tvSubtitle = findViewById<android.widget.TextView>(R.id.tvHeaderDateTime)
        tvTitle?.text = title
            ?: getString(R.string.title_deadlines)
        tvSubtitle?.text = dateFormatted

        // Fade out subtitle and large title as toolbar collapses; hide when collapsed
        val appBar: AppBarLayout? = findViewById(R.id.appBar)
        appBar?.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { layout, verticalOffset ->
            val range = layout.totalScrollRange.takeIf { it != 0 } ?: return@OnOffsetChangedListener
            val collapse = 1f - (range + verticalOffset).toFloat() / range.toFloat()
            val fade = (1f - collapse).coerceIn(0f, 1f)
            val hideThreshold = 0.6f
            val visible = collapse < hideThreshold
            val alpha = if (visible) fade else 0f
            tvSubtitle?.alpha = alpha
            tvTitle?.alpha = alpha
            tvSubtitle?.visibility = if (visible) View.VISIBLE else View.GONE
            tvTitle?.visibility = if (visible) View.VISIBLE else View.GONE
            // Toggle toolbar title only when collapsed enough
            if (visible) {
                supportActionBar?.title = ""
            } else {
                supportActionBar?.title = getString(R.string.calc_title)
            }
        })
    }

    private fun observeViewModel() {
        // Initial UI state setup
        binding.etCourseHours.setText("0")
        binding.sleepLabel.text =
            getString(R.string.calc_sleep_selected, binding.sleepSlider.value.toInt())
        val initialSpeed = ((binding.speedSlider.value * 2).toInt() / 2f)
        binding.speedLabel.text = getString(R.string.calc_speed_selected, initialSpeed)

        // Initialize ViewModel values
        viewModel.setSleepHours(binding.sleepSlider.value.toInt())
        viewModel.setPlaybackSpeed(initialSpeed)
        viewModel.setCourseHours(binding.etCourseHours.text.toString().toFloatOrNull() ?: 0f)
        // preselect matching chip
        binding.chipSpeed1.isChecked = initialSpeed == 1.0f
        binding.chipSpeed15.isChecked = initialSpeed == 1.5f
        binding.chipSpeed2.isChecked = initialSpeed == 2.0f

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.result.collect { res ->
                    // Timers update every second: set directly (no animation)
                    binding.cardTimeUntilDeadline.cardValue.text = res.timeUntilFormatted
                    binding.valueRemaining.text = res.remainingFormatted

                    // Animate only when user changed inputs
                    if (animateOnNextUserChange) {
                        animateHoursValue(binding.cardSleep.cardValue, res.totalSleepHours, 0)
                        animateHoursValue(binding.cardCourse.cardValue, res.adjustedCourseHours, 1)
                    } else {
                        binding.cardSleep.cardValue.text = "%.0f hours".format(res.totalSleepHours)
                        binding.cardCourse.cardValue.text =
                            "%.1f hours".format(res.adjustedCourseHours)
                    }

                    val sleepHoursNow = binding.sleepSlider.value
                    val sleepDiff = 8f - sleepHoursNow
                    val neutralSleep =
                        ContextCompat.getColor(this@CalculateActivity, R.color.colorSurfaceVariant)
                    val sleepColor = ContextCompat.getColor(
                        this@CalculateActivity,
                        if (sleepDiff > 0f) R.color.colorSuccess else if (sleepDiff < 0f) R.color.colorDanger else R.color.colorSurfaceVariant
                    )
                    ViewCompat.setBackgroundTintList(
                        binding.cardSleep.root,
                        ColorStateList.valueOf(sleepColor)
                    )
                    val sleepTextColor = if (sleepDiff > 0f)
                        ContextCompat.getColor(this@CalculateActivity, R.color.onSuccess)
                    else if (sleepDiff < 0f)
                        ContextCompat.getColor(this@CalculateActivity, R.color.onDanger)
                    else
                        ContextCompat.getColor(this@CalculateActivity, R.color.onSurface)
                    binding.cardSleep.cardValue.setTextColor(sleepTextColor)
                    binding.cardSleep.cardLabel.setTextColor(sleepTextColor)
                    binding.cardSleep.cardIcon.setColorFilter(sleepTextColor)

                    val courseHoursNow = binding.etCourseHours.text.toString().toFloatOrNull() ?: 0f
                    val accelerated =
                        res.adjustedCourseHours < courseHoursNow && courseHoursNow > 0f
                    val courseColor = ContextCompat.getColor(
                        this@CalculateActivity,
                        if (accelerated) R.color.colorSuccess else R.color.colorSurfaceVariant
                    )
                    ViewCompat.setBackgroundTintList(
                        binding.cardCourse.root,
                        ColorStateList.valueOf(courseColor)
                    )
                    val courseTextColor = ContextCompat.getColor(
                        this@CalculateActivity,
                        if (accelerated) R.color.onSuccess else R.color.onSurface
                    )
                    binding.cardCourse.cardValue.setTextColor(courseTextColor)
                    binding.cardCourse.cardLabel.setTextColor(courseTextColor)
                    binding.cardCourse.cardIcon.setColorFilter(courseTextColor)

                    if (sleepDiff == 0f) {
                        binding.cardSleepGain.root.visibility = View.GONE
                    } else {
                        binding.cardSleepGain.root.visibility = View.VISIBLE
                        val gain = res.sleepDiffPositive
                        if (animateOnNextUserChange) {
                            animateHoursDelta(
                                binding.cardSleepGain.cardValue,
                                res.sleepDiffHoursTotal,
                                gain
                            )
                        } else {
                            val sign = if (gain) "+" else "-"
                            binding.cardSleepGain.cardValue.text =
                                sign + "%.1f hours".format(res.sleepDiffHoursTotal)
                        }
                        binding.cardSleepGain.cardLabel.text =
                            if (gain) getString(R.string.calc_time_gained_sleep_less) else getString(
                                R.string.calc_time_lost_oversleeping
                            )
                        updateCardAppearance(binding.cardSleepGain, gain)
                    }

                    if (binding.speedSlider.value <= 1f || courseHoursNow == 0f) {
                        binding.cardSpeedGain.root.visibility = View.GONE
                    } else {
                        binding.cardSpeedGain.root.visibility = View.VISIBLE
                        if (animateOnNextUserChange) {
                            animateHoursDelta(
                                binding.cardSpeedGain.cardValue,
                                res.speedGainHours,
                                true
                            )
                        } else {
                            binding.cardSpeedGain.cardValue.text =
                                "+%.1f hours".format(res.speedGainHours)
                        }
                        updateCardAppearance(binding.cardSpeedGain, true)
                    }

                    // Reset animation trigger after processing this update
                    animateOnNextUserChange = false
                }
            }
        }
    }

    // Timer and countdown are handled by ViewModel flows

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
        if (activity != this::class.java) {
            startActivity(Intent(this, activity))
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            finish()
        }
    }

    private fun setupToolbarAndHeader() {
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.calc_title)
        val title = viewModel.getActiveTitle()
        val deadline = viewModel.getActiveTimestamp()
        val dateFormatted = if (deadline != 0L && deadline != -1L) {
            val sdf = SimpleDateFormat("MMM dd, yyyy – hh:mm a", Locale.getDefault())
            sdf.format(Date(deadline))
        } else {
            getString(R.string.calc_no_date_selected)
        }
        findViewById<android.widget.TextView>(R.id.tvHeaderDeadlineTitle)?.text =
            title ?: getString(R.string.title_deadlines)
        findViewById<android.widget.TextView>(R.id.tvHeaderDateTime)?.text = dateFormatted
    }

    private fun updateCardAppearance(cardBinding: ItemSummaryCardBinding, isPositive: Boolean) {
        val context = cardBinding.root.context
        val bgColor = ContextCompat.getColor(
            context,
            if (isPositive) R.color.colorSuccess else R.color.colorDanger
        )
        val fgColor = ContextCompat.getColor(
            context,
            if (isPositive) R.color.onSuccess else R.color.onDanger
        )
        ViewCompat.setBackgroundTintList(cardBinding.root, ColorStateList.valueOf(bgColor))
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
        Haptics.perform(this, view, feedbackType)
    }

    private fun animateTextChange(tv: android.widget.TextView, newText: String) {
        if (tv.text.toString() == newText) return
        val short = resources.getInteger(R.integer.anim_short).toLong()
        val back = resources.getInteger(R.integer.anim_pop_back).toLong()
        val down = resources.getFraction(R.fraction.scale_pop_down, 1, 1)
        val up = resources.getFraction(R.fraction.scale_pop_up, 1, 1)
        tv.animate().alpha(0f).scaleX(down).scaleY(down).setDuration(short).withEndAction {
            tv.text = newText
            tv.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(back).start()
        }.start()
    }

    private fun animateHoursValue(tv: android.widget.TextView, value: Float, decimals: Int) {
        val currentText = tv.text.toString()
        val regex = Regex("([+-]?)(\\d+(?:[.]\\d+)?)\\s+hours")
        val start = regex.find(currentText)?.groupValues?.get(2)?.toFloatOrNull() ?: value
        if (kotlin.math.abs(start - value) < 0.01f) {
            tv.text = String.format(Locale.getDefault(), "%.${decimals}f hours", value)
            return
        }
        val animator = android.animation.ValueAnimator.ofFloat(start, value)
        val medium = resources.getInteger(R.integer.anim_medium).toLong()
        val short = resources.getInteger(R.integer.anim_short).toLong()
        val back = resources.getInteger(R.integer.anim_pop_back).toLong()
        val up = resources.getFraction(R.fraction.scale_pop_up, 1, 1)
        animator.duration = medium
        animator.addUpdateListener {
            val v = it.animatedValue as Float
            tv.text = String.format(Locale.getDefault(), "%.${decimals}f hours", v)
        }
        tv.animate().scaleX(up).scaleY(up).setDuration(short).withEndAction {
            tv.animate().scaleX(1f).scaleY(1f).setDuration(back).start()
        }.start()
        animator.start()
    }

    private fun animateHoursDelta(tv: android.widget.TextView, value: Float, positive: Boolean) {
        val start = 0f
        val end = value
        val animator = android.animation.ValueAnimator.ofFloat(start, end)
        val medium = resources.getInteger(R.integer.anim_medium).toLong()
        val short = resources.getInteger(R.integer.anim_short).toLong()
        val back = resources.getInteger(R.integer.anim_pop_back).toLong()
        val up = resources.getFraction(R.fraction.scale_pop_up, 1, 1)
        val down = resources.getFraction(R.fraction.scale_pop_down, 1, 1)
        animator.duration = medium
        animator.addUpdateListener {
            val v = it.animatedValue as Float
            val sign = if (positive) "+" else "-"
            tv.text = sign + String.format(Locale.getDefault(), "%.1f hours", v)
        }
        tv.animate().scaleX(up).scaleY(up).alpha(0.92f).setDuration(short).withEndAction {
            tv.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(back).start()
        }.start()
        animator.start()
    }


    override fun onDestroy() {
        super.onDestroy()
    }
}
