package com.aboayman.finaltick.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aboayman.finaltick.CountdownWidget
import com.aboayman.finaltick.DeadlineAdapter
import com.aboayman.finaltick.DeadlineItem
import com.aboayman.finaltick.R

class WidgetSettingsActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_FinalTick)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget_settings)

        if (!loadAppWidgetId()) return

        setupRecycler()
        setupCancelButton()
    }

    private fun loadAppWidgetId(): Boolean {
        appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return false
        }
        return true
    }

    private fun setupRecycler() {
        val recycler = findViewById<RecyclerView>(R.id.rvDeadlineList)
        recycler.layoutManager = LinearLayoutManager(this)

        val items = loadDeadlineItems()
        recycler.adapter = DeadlineAdapter(items) { selected ->
            WidgetPreferencesManager.saveDeadline(this, appWidgetId, selected.timestamp)
            WidgetPreferencesManager.saveTitle(this, appWidgetId, selected.title)
            WidgetPreferencesManager.saveCreatedAt(this, appWidgetId, selected.createdAt)

            val resultIntent = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            setResult(Activity.RESULT_OK, resultIntent)

            CountdownWidget.forceUpdateAll(this)

            Toast.makeText(this, "Widget added with '${selected.title}'", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadDeadlineItems(): List<DeadlineItem> {
        val prefs = getSharedPreferences("finaltick_prefs", Context.MODE_PRIVATE)
        val saved = prefs.getStringSet("deadlines", null)?.toList()?.sorted() ?: emptyList()

        return saved.mapNotNull {
            val parts = it.split("|", limit = 3)
            val createdAt = parts.getOrNull(0)?.toLongOrNull() ?: System.currentTimeMillis()
            val timestamp = parts.getOrNull(1)?.toLongOrNull() ?: return@mapNotNull null
            val title = parts.getOrElse(2) { "(No title)" }
            DeadlineItem(title, timestamp, createdAt)
        }
    }

    private fun setupCancelButton() {
        findViewById<Button>(R.id.btnCancelWidget).setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }
}