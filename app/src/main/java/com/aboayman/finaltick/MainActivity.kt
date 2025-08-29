package com.aboayman.finaltick

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import com.aboayman.finaltick.databinding.ActivityMainBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.DynamicColors
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: DeadlineAdapterModern
    private val viewModel: DeadlineViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("finaltick_prefs", MODE_PRIVATE)
        val savedTheme = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(savedTheme)
        setTheme(R.style.Theme_FinalTick)
        super.onCreate(savedInstanceState)

        // Apply Material You dynamic color if available
        DynamicColors.applyToActivityIfAvailable(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        setupFab()
        setupRecyclerView()
        setupBottomNav()
        observeViewModel()
        viewModel.loadDeadlines()
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener { openCreateBottomSheet() }
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show()
                    true
                }

                R.id.nav_countdown -> {
                    Toast.makeText(this, "Countdown", Toast.LENGTH_SHORT).show()
                    true
                }

                R.id.nav_calculate -> {
                    Toast.makeText(this, "Insights", Toast.LENGTH_SHORT).show()
                    true
                }

                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = DeadlineAdapterModern(
            onClick = { item ->
                openDeadlineActions(item)
            },
            onMenuClick = { item, view ->
                showDeadlineOptionsMenu(item, view)
            },
            onLongPress = { item, view ->
                showDeadlineOptionsMenu(item, view)
            }
        )

        binding.deadlineRecyclerView.adapter = adapter
        binding.deadlineRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun observeViewModel() {
        viewModel.deadlines.observe(this) { deadlines ->
            adapter.submitList(deadlines)
            binding.emptyState.visibility = if (deadlines.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun openDeadlineActions(item: DeadlineItem) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_deadline_actions, null)
        val tvTitle = view.findViewById<android.widget.TextView>(R.id.tvDeadlineTitle)
        val btnCountdown = view.findViewById<MaterialButton>(R.id.btnStartCountdown)
        val btnCalc = view.findViewById<MaterialButton>(R.id.btnOpenCalculator)

        tvTitle.text = item.title

        btnCountdown.setOnClickListener {
            viewModel.setActiveDeadline(item)
            startActivity(Intent(this, CountdownActivity::class.java))
            dialog.dismiss()
        }
        btnCalc.setOnClickListener {
            viewModel.setActiveDeadline(item)
            startActivity(Intent(this, CalculateActivity::class.java))
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun openCreateBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_create_deadline, null)
        val etTitle = view.findViewById<TextInputEditText>(R.id.etTitle)
        val btnPickDate = view.findViewById<MaterialButton>(R.id.btnPickDate)
        val btnPickTime = view.findViewById<MaterialButton>(R.id.btnPickTime)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnSave)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancel)

        var selectedDateMillis: Long? = null
        var selectedHour = 12
        var selectedMinute = 0

        btnPickDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select deadline date")
                .build()
            datePicker.show(supportFragmentManager, "datePicker")
            datePicker.addOnPositiveButtonClickListener { millis ->
                selectedDateMillis = millis
                btnPickDate.text = datePicker.headerText
            }
        }

        btnPickTime.setOnClickListener {
            val timePicker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setTitleText("Select time")
                .setHour(selectedHour)
                .setMinute(selectedMinute)
                .build()
            timePicker.show(supportFragmentManager, "timePicker")
            timePicker.addOnPositiveButtonClickListener {
                selectedHour = timePicker.hour
                selectedMinute = timePicker.minute
                val ampm = if (selectedHour >= 12) "PM" else "AM"
                val hour12 = ((selectedHour + 11) % 12 + 1)
                btnPickTime.text = String.format("%d:%02d %s", hour12, selectedMinute, ampm)
            }
        }

        btnSave.setOnClickListener {
            val title = etTitle.text?.toString()?.trim().orEmpty()
            if (title.isEmpty()) {
                showWarningSnackbar(
                    binding.root,
                    "Enter a title first."
                ) { dismissCurrentSnackbar() }
                return@setOnClickListener
            }
            val dateMillis = selectedDateMillis
            if (dateMillis == null) {
                showWarningSnackbar(binding.root, "Pick a date.") { dismissCurrentSnackbar() }
                return@setOnClickListener
            }
            val calendar = Calendar.getInstance().apply {
                timeInMillis = dateMillis
                set(Calendar.HOUR_OF_DAY, selectedHour)
                set(Calendar.MINUTE, selectedMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                showWarningSnackbar(
                    binding.root,
                    "Select a future time."
                ) { dismissCurrentSnackbar() }
                return@setOnClickListener
            }
            viewModel.addDeadline(title, calendar.timeInMillis)
            showSuccessSnackbar(binding.root, "$title • Saved!") { dismissCurrentSnackbar() }
            dialog.dismiss()
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun showDeadlineOptionsMenu(item: DeadlineItem, view: View) {
        val popup = androidx.appcompat.widget.PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.menu_deadline_options, popup.menu)
        try {
            val field = popup.javaClass.getDeclaredField("mPopup")
            field.isAccessible = true
            val menuPopupHelper = field.get(popup)
            val setForceIcons = menuPopupHelper.javaClass.getDeclaredMethod(
                "setForceShowIcon",
                Boolean::class.javaPrimitiveType
            )
            setForceIcons.invoke(menuPopupHelper, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit -> {
                    showEditDialog(item)
                    true
                }

                R.id.action_delete -> {
                    viewModel.deleteDeadline(item)
                    showErrorSnackbar(binding.root, "${item.title} • Deleted") {
                        viewModel.undoDelete()
                    }
                    true
                }

                else -> false
            }
        }
        popup.show()
    }

    private fun showEditDialog(item: DeadlineItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_deadline, null)
        val editTitle = dialogView.findViewById<TextInputEditText>(R.id.etEditTitle)
        dialogView.findViewById<MaterialButton>(R.id.btnPickDate)
        dialogView.findViewById<MaterialButton>(R.id.btnPickTime)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btnSave)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)

        editTitle.setText(item.title)
        var newTimestamp = item.timestamp

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnSave.setOnClickListener {
            val newTitle = editTitle.text.toString().trim()
            if (newTitle.isNotEmpty() && newTimestamp > System.currentTimeMillis()) {
                viewModel.updateDeadline(item, newTimestamp, newTitle)
                showSuccessSnackbar(binding.root, "Deadline updated!") { dismissCurrentSnackbar() }
                dialog.dismiss()
            } else {
                showWarningSnackbar(
                    binding.root,
                    "Invalid title or time selected."
                ) { dismissCurrentSnackbar() }
            }
        }

        btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }

            R.id.action_clear_all -> {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Clear all deadlines?")
                    .setMessage("This action cannot be undone.")
                    .setPositiveButton("Clear") { dialog, _ ->
                        viewModel.clearAllDeadlines()
                        showErrorSnackbar(binding.root, "Cleared all deadlines", onUndo = null)
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPause() {
        super.onPause()
        dismissCurrentSnackbar()
    }
}
