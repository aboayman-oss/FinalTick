package com.aboayman.finaltick

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    private val progressHandler = Handler(Looper.getMainLooper())
    private val progressRunnable = object : Runnable {
        override fun run() {
            updateVisibleProgress()
            progressHandler.postDelayed(this, 1000)
        }
    }

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
        observeViewModel()
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener { openCreateBottomSheet() }
    }

    // Bottom navigation removed per UI simplification

    private fun setupRecyclerView() {
        adapter = DeadlineAdapterModern(
            onClick = { item ->
                openDeadlineActions(item)
            },
            onMenuClick = { item, _ ->
                showDeadlineOptionsBottomSheet(item)
            },
            onLongPress = { item, _ ->
                showDeadlineOptionsBottomSheet(item)
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
                .setTitleText(getString(R.string.picker_select_deadline_date))
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
                .setTitleText(getString(R.string.picker_select_time))
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
                    getString(R.string.warning_enter_title)
                ) { dismissCurrentSnackbar() }
                return@setOnClickListener
            }
            val dateMillis = selectedDateMillis
            if (dateMillis == null) {
                showWarningSnackbar(
                    binding.root,
                    getString(R.string.warning_pick_date)
                ) { dismissCurrentSnackbar() }
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
                    getString(R.string.warning_future_time)
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

    private fun showDeadlineOptionsBottomSheet(item: DeadlineItem) {
        val dialog = BottomSheetDialog(this)
        val content = layoutInflater.inflate(R.layout.bottom_sheet_deadline_options, null)
        val btnEdit = content.findViewById<MaterialButton>(R.id.btnEdit)
        val btnDelete = content.findViewById<MaterialButton>(R.id.btnDelete)

        btnEdit.setOnClickListener {
            dialog.dismiss()
            showEditDialog(item)
        }
        btnDelete.setOnClickListener {
            dialog.dismiss()
            viewModel.deleteDeadline(item)
            showErrorSnackbar(binding.root, getString(R.string.msg_deleted, item.title)) {
                viewModel.undoDelete()
            }
        }

        dialog.setContentView(content)
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
                showSuccessSnackbar(
                    binding.root,
                    getString(R.string.msg_updated)
                ) { dismissCurrentSnackbar() }
                dialog.dismiss()
            } else {
                showWarningSnackbar(
                    binding.root,
                    getString(R.string.warning_invalid_title_or_time)
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
                    .setTitle(getString(R.string.dialog_clear_all_title))
                    .setMessage(getString(R.string.dialog_clear_all_message))
                    .setPositiveButton(getString(R.string.action_clear)) { dialog, _ ->
                        viewModel.clearAllDeadlines()
                        showErrorSnackbar(
                            binding.root,
                            getString(R.string.msg_cleared_all),
                            onUndo = null
                        )
                        dialog.dismiss()
                    }
                    .setNegativeButton(getString(R.string.action_cancel), null)
                    .show()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        progressHandler.post(progressRunnable)
    }

    override fun onPause() {
        super.onPause()
        // Let Snackbars time out naturally; only stop progress updates.
        progressHandler.removeCallbacks(progressRunnable)
    }

    private fun updateVisibleProgress() {
        val lm = binding.deadlineRecyclerView.layoutManager as? LinearLayoutManager ?: return
        val first = lm.findFirstVisibleItemPosition()
        val last = lm.findLastVisibleItemPosition()
        if (first == RecyclerView.NO_POSITION || last == RecyclerView.NO_POSITION) return
        for (pos in first..last) {
            val holder = binding.deadlineRecyclerView.findViewHolderForAdapterPosition(pos)
            if (holder is DeadlineAdapterModern.ViewHolder) {
                holder.updateProgress()
            }
        }
    }
}
