package com.aboayman.finaltick

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import com.aboayman.finaltick.databinding.ActivityMainBinding
import com.google.android.material.button.MaterialButton
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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupRecyclerView()
        observeViewModel()
        viewModel.loadDeadlines()
    }

    private fun setupUI() {
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.btnSelectDate.visibility = View.GONE
        binding.btnCountdown.visibility = View.GONE
        binding.btnCalculate.visibility = View.GONE

        binding.etTitle.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                binding.btnSelectDate.visibility =
                    if (!s.isNullOrBlank()) View.VISIBLE else View.GONE
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.etTitle.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.etTitle.clearFocus()
                val imm =
                    getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                true
            } else false
        }

        binding.btnSelectDate.setOnClickListener {
            handleDateSelection()
        }

        binding.btnClearAllDeadlines.setOnClickListener {
            viewModel.clearAllDeadlines()
            // CORRECTED: Call showErrorSnackbar with `onUndo = null` since there's no undo here.
            showErrorSnackbar(
                root = binding.root,
                message = "Cleared all deadlines",
                onUndo = null
            )
        }
    }

    private fun setupRecyclerView() {
        adapter = DeadlineAdapterModern(
            onClick = { item ->
                viewModel.setActiveDeadline(item)
                showSuccessSnackbar(
                    binding.root,
                    "${item.title} → Activated"
                ) { dismissCurrentSnackbar() }

                binding.btnCountdown.visibility = View.VISIBLE
                binding.btnCalculate.visibility = View.VISIBLE

                binding.btnCountdown.setOnClickListener {
                    startActivity(Intent(this, CountdownActivity::class.java))
                }
                binding.btnCalculate.setOnClickListener {
                    startActivity(Intent(this, CalculateActivity::class.java))
                }
            },
            onDelete = { item ->
                viewModel.deleteDeadline(item)
                // CORRECTED: The trailing lambda now correctly calls viewModel.undoDelete()
                showErrorSnackbar(binding.root, "${item.title} → Deleted") {
                    viewModel.undoDelete()
                }
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
            binding.btnClearAllDeadlines.visibility =
                if (deadlines.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun handleDateSelection() {
        val title = binding.etTitle.text.toString().trim()
        if (title.isBlank()) {
            showWarningSnackbar(binding.root, "Enter a title first.") { dismissCurrentSnackbar() }
            return
        }

        val datePicker =
            MaterialDatePicker.Builder.datePicker().setTitleText("Select deadline date").build()
        datePicker.show(supportFragmentManager, "datePicker")

        datePicker.addOnPositiveButtonClickListener { selectedDateMillis ->
            val timePicker = MaterialTimePicker.Builder().setTimeFormat(TimeFormat.CLOCK_12H)
                .setTitleText("Select time").build()
            timePicker.show(supportFragmentManager, "timePicker")

            timePicker.addOnPositiveButtonClickListener {
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = selectedDateMillis
                    set(Calendar.HOUR_OF_DAY, timePicker.hour)
                    set(Calendar.MINUTE, timePicker.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                if (calendar.timeInMillis <= System.currentTimeMillis()) {
                    showWarningSnackbar(
                        binding.root,
                        "Select a future time."
                    ) { dismissCurrentSnackbar() }
                    return@addOnPositiveButtonClickListener
                }

                viewModel.addDeadline(title, calendar.timeInMillis)
                showSuccessSnackbar(binding.root, "$title → Saved!") { dismissCurrentSnackbar() }
                binding.etTitle.text?.clear()
            }
        }
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
                    // CORRECTED: This also now correctly calls viewModel.undoDelete()
                    showErrorSnackbar(binding.root, "${item.title} → Deleted") {
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

        // Date and time picker logic for the edit dialog would go here, updating `newTimestamp`

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

    override fun onPause() {
        super.onPause()
        dismissCurrentSnackbar()
    }
}