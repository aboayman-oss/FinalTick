package com.aboayman.finaltick

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.Chip
import com.google.android.material.progressindicator.LinearProgressIndicator

class CountdownActivity : AppCompatActivity() {

    private val PREF_COLOR = "countdown_timer_color"
    private val PREF_WEIGHT = "countdown_timer_weight"
    private val PREF_DYNAMIC_COLOR = "countdown_dynamic_color"
    private val PREF_USE_GLOBAL = "countdown_use_global"
    private val PREF_CUSTOM_WEIGHT = "countdown_custom_weight"
    private val PREF_CUSTOM_COLOR = "countdown_custom_color"
    private val PREF_CUSTOM_DYNAMIC = "countdown_custom_dynamic"

    private val viewModel: CountdownViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_FinalTick)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_countdown)

        val prefs = getSharedPreferences("finaltick_prefs", Context.MODE_PRIVATE)
        val deadline = prefs.getLong("deadline_timestamp", -1L)
        val createdAt = prefs.getLong("countdown_createdAt", System.currentTimeMillis())
        val title = prefs.getString("countdown_title", "Untitled Countdown")

        if (deadline == -1L) {
            Toast.makeText(this, "No deadline set. Please start over.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = title
        setupHeader()

        val expandedTimer = findViewById<TextView>(R.id.tvCountdownTimer)
        val progressBar = findViewById<LinearProgressIndicator>(R.id.countdownProgressBar)
        val progressPercentText = findViewById<TextView>(R.id.tvCountdownProgressPercent)
        findViewById<TextView>(R.id.tvCountdownTitle).text = title

        applyCustomization()

        val chipDays = findViewById<Chip>(R.id.chipDays)
        val chipHours = findViewById<Chip>(R.id.chipHours)
        val chipMinutes = findViewById<Chip>(R.id.chipMinutes)
        val chipSeconds = findViewById<Chip>(R.id.chipSeconds)

        chipDays.isChecked = prefs.getBoolean("widget_show_days", true)
        chipHours.isChecked = prefs.getBoolean("widget_show_hours", true)
        chipMinutes.isChecked = prefs.getBoolean("widget_show_minutes", true)
        chipSeconds.isChecked = prefs.getBoolean("widget_show_seconds", true)

        fun saveFormatPrefs() {
            prefs.edit()
                .putBoolean("widget_show_days", chipDays.isChecked)
                .putBoolean("widget_show_hours", chipHours.isChecked)
                .putBoolean("widget_show_minutes", chipMinutes.isChecked)
                .putBoolean("widget_show_seconds", chipSeconds.isChecked)
                .apply()
        }

        fun doHaptic() = Haptics.perform(this, expandedTimer)

        listOf(chipDays, chipHours, chipMinutes, chipSeconds).forEach { chip ->
            chip.setOnCheckedChangeListener { _, _ ->
                // Ensure at least one unit is always visible
                if (!chipDays.isChecked && !chipHours.isChecked && !chipMinutes.isChecked && !chipSeconds.isChecked) {
                    chipSeconds.isChecked = true
                }
                saveFormatPrefs()
                doHaptic()
                viewModel.state.value?.let {
                    renderState(
                        it,
                        expandedTimer,
                        progressBar,
                        progressPercentText,
                        chipDays.isChecked,
                        chipHours.isChecked,
                        chipMinutes.isChecked,
                        chipSeconds.isChecked
                    )
                }
            }
        }

        viewModel.state.observe(this, Observer { state ->
            renderState(
                state,
                expandedTimer,
                progressBar,
                progressPercentText,
                chipDays.isChecked,
                chipHours.isChecked,
                chipMinutes.isChecked,
                chipSeconds.isChecked
            )
        })

        viewModel.start(deadline, createdAt)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_countdown
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish(); true
                }
                R.id.nav_countdown -> true
                R.id.nav_calculate -> {
                    startActivity(Intent(this, CalculateActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish(); true
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

    private fun renderState(
        state: CountdownState,
        expandedTimer: TextView,
        progressBar: LinearProgressIndicator,
        progressPercentText: TextView,
        showDays: Boolean,
        showHours: Boolean,
        showMinutes: Boolean,
        showSeconds: Boolean
    ) {
        if (state.finished) {
            expandedTimer.text = "00d 00h 00m 00s"
            val danger = ContextCompat.getColor(this, R.color.colorDanger)
            progressBar.progress = 100
            progressPercentText.text = "100%"
            progressBar.setIndicatorColor(danger)
            expandedTimer.setTextColor(danger)
            return
        }

        val parts = CountdownFormatter.breakdown(
            state.remainingSeconds,
            showDays,
            showHours,
            showMinutes,
            showSeconds
        )
        expandedTimer.text = CountdownFormatter.formatPrimaryTimerRich(
            this,
            parts,
            showDays,
            showHours,
            showMinutes,
            showSeconds
        )

        val progress = state.progress

        val prefsAll = getSharedPreferences("finaltick_prefs", Context.MODE_PRIVATE)
        val useGlobal = prefsAll.getBoolean(PREF_USE_GLOBAL, true)
        val dynamic =
            if (useGlobal) prefsAll.getBoolean(PREF_DYNAMIC_COLOR, false) else prefsAll.getBoolean(
                PREF_CUSTOM_DYNAMIC,
                false
            )
        if (dynamic) {
            val textColorRes = when (progress) {
                in 0..24 -> R.color.progressSoftGreen
                in 25..49 -> R.color.progressCyanBlue
                in 50..74 -> R.color.progressAmber
                else -> R.color.colorDanger
            }
            expandedTimer.setTextColor(ContextCompat.getColor(this, textColorRes))
        } else {
            val color = if (useGlobal) prefsAll.getInt(
                PREF_COLOR,
                getColorFromAttrPrimary()
            ) else prefsAll.getInt(PREF_CUSTOM_COLOR, getColorFromAttrPrimary())
            expandedTimer.setTextColor(color)
        }

        progressBar.progress = progress
        progressPercentText.text = "$progress%"
        val barColorRes = when (progress) {
            in 0..24 -> R.color.progressSoftGreen
            in 25..49 -> R.color.progressCyanBlue
            in 50..74 -> R.color.progressAmber
            else -> R.color.colorDanger
        }
        progressBar.setIndicatorColor(ContextCompat.getColor(this, barColorRes))
    }

    private fun setupHeader() {
        val prefs = getSharedPreferences("finaltick_prefs", Context.MODE_PRIVATE)
        val deadline = prefs.getLong("deadline_timestamp", 0L)
        val dateFormatted = if (deadline != 0L) {
            java.text.SimpleDateFormat("MMM dd, yyyy - hh:mm a", java.util.Locale.getDefault())
                .format(java.util.Date(deadline))
        } else {
            "No date selected"
        }
        findViewById<TextView>(R.id.tvHeaderDateTime)?.text = dateFormatted
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.menu_countdown, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_customize -> {
                openCustomizeDialog(); true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openCustomizeDialog() {
        val prefs = getSharedPreferences("finaltick_prefs", Context.MODE_PRIVATE)
        val useGlobalInitial = prefs.getBoolean(PREF_USE_GLOBAL, true)
        val currentWeight = if (useGlobalInitial) prefs.getInt(PREF_WEIGHT, 600) else prefs.getInt(
            PREF_CUSTOM_WEIGHT,
            600
        )
        val initialColor = if (useGlobalInitial) prefs.getInt(
            PREF_COLOR,
            getColorFromAttrPrimary()
        ) else prefs.getInt(PREF_CUSTOM_COLOR, getColorFromAttrPrimary())
        val currentDynamic =
            if (useGlobalInitial) prefs.getBoolean(PREF_DYNAMIC_COLOR, false) else prefs.getBoolean(
                PREF_CUSTOM_DYNAMIC,
                false
            )

        val weights = arrayOf("Light", "Regular", "Medium", "SemiBold", "Bold")
        val weightValues = intArrayOf(300, 400, 500, 600, 700)
        var selectedWeightIndex = weightValues.indexOf(currentWeight).coerceAtLeast(0)
        var pickedColor = initialColor

        val switchUseGlobal =
            com.google.android.material.materialswitch.MaterialSwitch(this).apply {
                text = "Use global defaults"
                isChecked = useGlobalInitial
            }
        val switchDynamic = com.google.android.material.materialswitch.MaterialSwitch(this).apply {
            text = "Dynamic color by progress"
            isChecked = currentDynamic
        }
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 16, 32, 0)
            addView(switchUseGlobal)
            addView(switchDynamic)
        }

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Customize Timer")
            .setView(container)
            .setSingleChoiceItems(weights, selectedWeightIndex) { _, which ->
                selectedWeightIndex = which
            }
            .setPositiveButton("Pick Color") { _, _ ->
                val dialog = yuku.ambilwarna.AmbilWarnaDialog(
                    this,
                    pickedColor,
                    true,
                    object : yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener {
                        override fun onOk(dialog: yuku.ambilwarna.AmbilWarnaDialog?, color: Int) {
                            pickedColor = color
                            saveCustomization(
                                weightValues[selectedWeightIndex],
                                pickedColor,
                                switchDynamic.isChecked,
                                switchUseGlobal.isChecked
                            )
                            applyCustomization()
                        }

                        override fun onCancel(dialog: yuku.ambilwarna.AmbilWarnaDialog?) {
                            saveCustomization(
                                weightValues[selectedWeightIndex],
                                pickedColor,
                                switchDynamic.isChecked,
                                switchUseGlobal.isChecked
                            )
                            applyCustomization()
                        }
                    }
                )
                dialog.show()
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Apply") { _, _ ->
                saveCustomization(
                    weightValues[selectedWeightIndex],
                    pickedColor,
                    switchDynamic.isChecked,
                    switchUseGlobal.isChecked
                )
                applyCustomization()
            }
        builder.show()
    }

    private fun saveCustomization(weight: Int, color: Int, dynamic: Boolean, useGlobal: Boolean) {
        val prefs = getSharedPreferences("finaltick_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean(PREF_USE_GLOBAL, useGlobal)
            if (useGlobal) {
                putInt(PREF_WEIGHT, weight)
                putInt(PREF_COLOR, color)
                putBoolean(PREF_DYNAMIC_COLOR, dynamic)
            } else {
                putInt(PREF_CUSTOM_WEIGHT, weight)
                putInt(PREF_CUSTOM_COLOR, color)
                putBoolean(PREF_CUSTOM_DYNAMIC, dynamic)
            }
        }.apply()
    }

    private fun applyCustomization() {
        val prefs = getSharedPreferences("finaltick_prefs", Context.MODE_PRIVATE)
        val useGlobal = prefs.getBoolean(PREF_USE_GLOBAL, true)
        val weight =
            if (useGlobal) prefs.getInt(PREF_WEIGHT, 600) else prefs.getInt(PREF_CUSTOM_WEIGHT, 600)
        val color =
            if (useGlobal) prefs.getInt(PREF_COLOR, getColorFromAttrPrimary()) else prefs.getInt(
                PREF_CUSTOM_COLOR,
                getColorFromAttrPrimary()
            )
        val dynamic =
            if (useGlobal) prefs.getBoolean(PREF_DYNAMIC_COLOR, false) else prefs.getBoolean(
                PREF_CUSTOM_DYNAMIC,
                false
            )
        val expandedTimer = findViewById<TextView>(R.id.tvCountdownTimer)
        expandedTimer.typeface = getPoppinsByWeight(weight)
        if (!dynamic) {
            expandedTimer.setTextColor(color)
        }
    }

    private fun getPoppinsByWeight(weight: Int): android.graphics.Typeface? {
        val resId = when (weight) {
            300 -> R.font.poppins_light
            400 -> R.font.poppins_regular
            500 -> R.font.poppins_medium
            600 -> R.font.poppins_semibold
            700 -> R.font.poppins_bold
            else -> R.font.poppins_semibold
        }
        return androidx.core.content.res.ResourcesCompat.getFont(this, resId)
    }

    private fun getColorFromAttrPrimary(): Int {
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)
        return typedValue.data
    }
}
