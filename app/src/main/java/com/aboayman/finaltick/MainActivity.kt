package com.aboayman.finaltick

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import com.aboayman.finaltick.databinding.ActivityMainBinding
import com.google.android.material.button.MaterialButton
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
        val prefs = getSharedPreferences("finaltick_prefs", MODE_PRIVATE)

        val savedTheme = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(savedTheme)

        setTheme(R.style.Theme_FinalTick)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        val titleEditText = findViewById<TextInputEditText>(R.id.etTitle)
        val selectDateBtn = findViewById<Button>(R.id.btnSelectDate)
        val countdownBtn = findViewById<Button>(R.id.btnCountdown)
        val calculateBtn = findViewById<Button>(R.id.btnCalculate)
        val btnClearAllDeadlines = findViewById<Button>(R.id.btnClearAllDeadlines)

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

        adapter = DeadlineAdapterModern(
            mutableListOf(),
            onClick = { item ->
                // (you already have this logic to activate)
                val prefs = getSharedPreferences("finaltick_prefs", MODE_PRIVATE)

                prefs.edit()
                    .putLong("deadline_timestamp", item.timestamp)
                    .putString("countdown_title", item.title)
                    .apply()

                showSuccessSnackbar(
                    root = findViewById(android.R.id.content),
                    message = "${item.title} → Activated",
                    onClose = { dismissCurrentSnackbar() }
                )

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
                // (your delete logic)
                val prefs = getSharedPreferences("finaltick_prefs", MODE_PRIVATE)
                val updated = prefs.getStringSet("deadlines", mutableSetOf())!!.toMutableSet()
                val toRemove = updated.find { entry ->
                    val parts = entry.split("|", limit = 3)
                    val createdAt = parts.getOrNull(0)?.toLongOrNull()
                    val timestamp = parts.getOrNull(1)?.toLongOrNull()
                    createdAt == item.createdAt && timestamp == item.timestamp
                }
                if (toRemove != null) {
                    updated.remove(toRemove)
                    prefs.edit().putStringSet("deadlines", updated).apply()
                    refreshList(adapter, updated)
                    showErrorSnackbar(
                        root = findViewById(android.R.id.content),
                        message = "${item.title} → Deleted"
                    ) {
                        updated.add(toRemove)
                        prefs.edit().putStringSet("deadlines", updated).apply()
                        refreshList(adapter, updated)
                    }
                }
            },

            onLongPress = { item, view ->

                val popup = androidx.appcompat.widget.PopupMenu(this, view)
                popup.menuInflater.inflate(R.menu.menu_deadline_options, popup.menu)
                // Force icons to appear
                try {
                    val fields = popup.javaClass.declaredFields
                    for (field in fields) {
                        if (field.name == "mPopup") {
                            field.isAccessible = true
                            val menuPopupHelper = field.get(popup)
                            val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
                            val setForceIcons = classPopupHelper.getMethod(
                                "setForceShowIcon",
                                Boolean::class.javaPrimitiveType
                            )
                            setForceIcons.invoke(menuPopupHelper, true)
                            break
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.title) {
                        "Edit" -> {
                            // (We'll do edit in Step 3 next)
                            showEditDialog(item)
                            true
                        }

                        "Delete" -> {
                            val prefs = getSharedPreferences("finaltick_prefs", MODE_PRIVATE)
                            val updated =
                                prefs.getStringSet("deadlines", mutableSetOf())!!.toMutableSet()
                            val toRemove = updated.find { entry ->
                                val parts = entry.split("|", limit = 3)
                                val createdAt = parts.getOrNull(0)?.toLongOrNull()
                                val timestamp = parts.getOrNull(1)?.toLongOrNull()
                                createdAt == item.createdAt && timestamp == item.timestamp
                            }
                            if (toRemove != null) {
                                updated.remove(toRemove)
                                prefs.edit().putStringSet("deadlines", updated).apply()
                                refreshList(adapter, updated)
                                showErrorSnackbar(
                                    root = findViewById(android.R.id.content),
                                    message = "${item.title} → Deleted"
                                ) {
                                    updated.add(toRemove)
                                    prefs.edit().putStringSet("deadlines", updated).apply()
                                    refreshList(adapter, updated)
                                }
                            }
                            true
                        }

                        else -> false
                    }
                }
                popup.show()
            }
        )

        binding.deadlineRecyclerView.adapter = adapter
        binding.deadlineRecyclerView.layoutManager = LinearLayoutManager(this)

        // ✅ Fresh load from preferences on start
        val fresh = prefs.getStringSet("deadlines", mutableSetOf())!!.toMutableSet()
        refreshList(adapter, fresh)

        btnClearAllDeadlines.setOnClickListener {
            val current = prefs.getStringSet("deadlines", mutableSetOf())!!.toMutableSet()
            prefs.edit().putStringSet("deadlines", mutableSetOf()).apply()
            refreshList(adapter, mutableSetOf())

            showErrorSnackbar(
                root = findViewById(android.R.id.content),
                message = "Cleared all deadlines"
            ) {
                prefs.edit().putStringSet("deadlines", current).apply()
                refreshList(adapter, current)
            }
        }

        selectDateBtn.setOnClickListener {
            val title = titleEditText.text.toString().trim()
            if (title.isBlank()) {
                showWarningSnackbar(
                    root = findViewById(android.R.id.content),
                    message = "Enter a title first.",
                    onClose = { dismissCurrentSnackbar() }
                )

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
                        showWarningSnackbar(
                            root = findViewById(android.R.id.content),
                            message = "Select a future time.",
                            onClose = { dismissCurrentSnackbar() }
                        )
                        return@addOnPositiveButtonClickListener
                    }

                    val current = prefs.getStringSet("deadlines", mutableSetOf())!!.toMutableSet()
                    val createdAt = System.currentTimeMillis()
                    current.add("$createdAt|$finalTimestamp|$title")
                    prefs.edit().putStringSet("deadlines", current).apply()

                    prefs.edit()
                        .putLong("deadline_timestamp", finalTimestamp)
                        .putString("countdown_title", title)
                        .apply()

                    val item = DeadlineItem(title, finalTimestamp, createdAt)

                    showSuccessSnackbar(
                        root = findViewById(android.R.id.content),
                        message = "${item.title} → Saved!",
                        onClose = { dismissCurrentSnackbar() }
                    )
                    refreshList(adapter, current)
                }
            }
        }
    }

    private fun refreshList(adapter: DeadlineAdapterModern, saved: Set<String>) {
        val formatter = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault())

        val deadlines = saved.mapNotNull { entry ->
            val parts = entry.split("|", limit = 3)
            val createdAt = parts.getOrNull(0)?.toLongOrNull() ?: System.currentTimeMillis()
            val timestamp = parts.getOrNull(1)?.toLongOrNull() ?: return@mapNotNull null
            val title = parts.getOrNull(2) ?: "(No title)"
            DeadlineItem(title, timestamp, createdAt)
        }.sortedBy { it.timestamp }

        adapter.updateData(deadlines)
        binding.btnClearAllDeadlines.visibility =
            if (deadlines.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showEditDialog(item: DeadlineItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_deadline, null)

        val editTitle = dialogView.findViewById<TextInputEditText>(R.id.etEditTitle)
        val btnPickDate = dialogView.findViewById<MaterialButton>(R.id.btnPickDate)
        val btnPickTime = dialogView.findViewById<MaterialButton>(R.id.btnPickTime)

        editTitle.setText(item.title)

        var newTimestamp = item.timestamp

        btnPickDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Edit deadline date")
                .setSelection(newTimestamp)
                .build()

            datePicker.show(supportFragmentManager, "editDatePicker")
            datePicker.addOnPositiveButtonClickListener { selectedDateMillis ->
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = selectedDateMillis

                val oldCalendar = Calendar.getInstance()
                oldCalendar.timeInMillis = newTimestamp

                calendar.set(Calendar.HOUR_OF_DAY, oldCalendar.get(Calendar.HOUR_OF_DAY))
                calendar.set(Calendar.MINUTE, oldCalendar.get(Calendar.MINUTE))
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)

                newTimestamp = calendar.timeInMillis
            }
        }

        btnPickTime.setOnClickListener {
            val oldCalendar = Calendar.getInstance()
            oldCalendar.timeInMillis = newTimestamp

            val timePicker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(oldCalendar.get(Calendar.HOUR_OF_DAY))
                .setMinute(oldCalendar.get(Calendar.MINUTE))
                .setTitleText("Edit deadline time")
                .build()

            timePicker.show(supportFragmentManager, "editTimePicker")
            timePicker.addOnPositiveButtonClickListener {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = newTimestamp
                calendar.set(Calendar.HOUR_OF_DAY, timePicker.hour)
                calendar.set(Calendar.MINUTE, timePicker.minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)

                newTimestamp = calendar.timeInMillis
            }
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.show()
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btnSave)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)

        btnSave.setOnClickListener {
            val newTitle = editTitle.text.toString().trim()
            if (newTitle.isNotEmpty() && newTimestamp > System.currentTimeMillis()) {
                val prefs = getSharedPreferences("finaltick_prefs", MODE_PRIVATE)
                val current = prefs.getStringSet("deadlines", mutableSetOf())!!.toMutableSet()

                val oldEntry = current.find { it.startsWith("${item.timestamp}|") }
                if (oldEntry != null) {
                    current.remove(oldEntry)
                }

                current.add("$newTimestamp|$newTitle")
                prefs.edit().putStringSet("deadlines", current).apply()

                showSuccessSnackbar(
                    root = findViewById(android.R.id.content),
                    message = "Deadline updated!",
                    onClose = { dismissCurrentSnackbar() }
                )

                refreshList(adapter, current)
                dialog.dismiss()
            } else {
                showWarningSnackbar(
                    root = findViewById(android.R.id.content),
                    message = "Invalid title or time selected.",
                    onClose = { dismissCurrentSnackbar() }
                )
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
    }

    override fun onPause() {
        super.onPause()
        dismissCurrentSnackbar()
    }
}