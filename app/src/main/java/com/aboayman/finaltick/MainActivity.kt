package com.aboayman.finaltick

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import com.aboayman.finaltick.databinding.ActivityMainBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: DeadlineAdapterModern

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        setTheme(R.style.Theme_FinalTick)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val titleEditText = findViewById<TextInputEditText>(R.id.etTitle)
        val selectDateBtn = findViewById<Button>(R.id.btnSelectDate)
        val countdownBtn = findViewById<Button>(R.id.btnCountdown)
        val calculateBtn = findViewById<Button>(R.id.btnCalculate)
        val btnClearAllDeadlines = findViewById<Button>(R.id.btnClearAllDeadlines)

        // Show the date button only when a title is entered
        selectDateBtn.visibility = View.GONE
        countdownBtn.visibility = View.GONE
        calculateBtn.visibility = View.GONE

        titleEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                selectDateBtn.visibility = if (!s.isNullOrBlank()) View.VISIBLE else View.GONE
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        titleEditText.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                titleEditText.clearFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                true
            } else false
        }

        val prefs = getSharedPreferences("finaltick_prefs", Context.MODE_PRIVATE)
        val saved = prefs.getStringSet("deadlines", mutableSetOf())!!.toMutableSet()

        adapter = DeadlineAdapterModern(
            mutableListOf(),
            onClick = { item ->
                prefs.edit()
                    .putLong("deadline_timestamp", item.timestamp)
                    .putString("countdown_title", item.title)
                    .apply()

                Toast.makeText(this, "Activated: ${item.title}", Toast.LENGTH_SHORT).show()

                binding.btnCountdown.visibility = View.VISIBLE
                binding.btnCalculate.visibility = View.VISIBLE

                binding.btnCountdown.setOnClickListener {
                    startActivity(Intent(this, CountdownActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                }

                binding.btnCalculate.setOnClickListener {
                    startActivity(Intent(this, CalculateActivity::class.java))
                }
            },
            onDelete = { item ->
                val updated = prefs.getStringSet("deadlines", mutableSetOf())!!.toMutableSet()
                val toRemove = updated.find { it.startsWith("${item.timestamp}|") }
                if (toRemove != null) {
                    updated.remove(toRemove)
                    prefs.edit().putStringSet("deadlines", updated).apply()
                    refreshList(adapter, updated)
                    Toast.makeText(this, "Deleted: ${item.title}", Toast.LENGTH_SHORT).show()
                }
            }
        )

        binding.deadlineRecyclerView.adapter = adapter
        binding.deadlineRecyclerView.layoutManager = LinearLayoutManager(this)
        refreshList(adapter, saved)

        binding.btnClearAllDeadlines.setOnClickListener {
            saved.clear()
            prefs.edit().putStringSet("deadlines", saved).apply()
            refreshList(adapter, saved)
        }

        refreshList(adapter, saved) // ✅ Already called earlier

        selectDateBtn.setOnClickListener {
            val title = titleEditText.text.toString().trim()
            if (title.isBlank()) {
                Toast.makeText(this, "Please enter a title first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select deadline date")
                .build()

            datePicker.show(supportFragmentManager, "datePicker")

            datePicker.addOnPositiveButtonClickListener { selectedDateMillis ->
                val timePicker = MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_12H)
                    .setHour(12)
                    .setMinute(0)
                    .setTitleText("Select time")
                    .build()

                timePicker.show(supportFragmentManager, "timePicker")

                timePicker.addOnPositiveButtonClickListener {
                    val hour = timePicker.hour
                    val minute = timePicker.minute

                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = selectedDateMillis
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)

                    val finalTimestamp = calendar.timeInMillis
                    if (finalTimestamp <= System.currentTimeMillis()) {
                        Toast.makeText(this, "⚠️ Please select a future time.", Toast.LENGTH_LONG).show()
                        return@addOnPositiveButtonClickListener
                    }

                    saved.add("$finalTimestamp|$title")
                    prefs.edit().putStringSet("deadlines", saved).apply()
                    prefs.edit()
                        .putLong("deadline_timestamp", finalTimestamp)
                        .putString("countdown_title", title) // ✅ Save title for widget
                        .apply()

                    Toast.makeText(this, "Deadline saved!", Toast.LENGTH_SHORT).show()
                    refreshList(adapter, saved)
                }
            }
        }
    }

    private fun refreshList(adapter: DeadlineAdapterModern, saved: Set<String>) {
        val formatter = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault())

        val deadlines = saved.mapNotNull { entry ->
            val parts = entry.split("|", limit = 2)
            val timestamp = parts.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
            val title = parts.getOrNull(1) ?: "(No title)"
            DeadlineItem(title, timestamp)
        }.sortedBy { it.timestamp }

        adapter.updateData(deadlines)
        binding.btnClearAllDeadlines.visibility =
            if (deadlines.isEmpty()) View.GONE else View.VISIBLE
    }

}