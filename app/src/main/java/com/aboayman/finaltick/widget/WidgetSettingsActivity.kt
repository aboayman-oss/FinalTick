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

        appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
            ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val recycler = findViewById<RecyclerView>(R.id.rvDeadlineList)
        val cancelButton = findViewById<Button>(R.id.btnCancelWidget)

        recycler.layoutManager = LinearLayoutManager(this)

        val prefs = getSharedPreferences("finaltick_prefs", Context.MODE_PRIVATE)
        val saved = prefs.getStringSet("deadlines", null)?.toList()?.sorted() ?: emptyList()
        val items = saved.mapNotNull {
            val parts = it.split("|", limit = 3)
            val createdAt = parts.getOrNull(0)?.toLongOrNull() ?: System.currentTimeMillis()
            val timestamp = parts.getOrNull(1)?.toLongOrNull() ?: return@mapNotNull null
            val title = parts.getOrElse(2) { "(No title)" }
            DeadlineItem(title, timestamp, createdAt)
        }

        recycler.adapter = DeadlineAdapter(items) { selected ->
            // When deadline clicked => save immediately
            val deadlineMillis = selected.timestamp

            WidgetPreferencesManager.saveDeadline(this, appWidgetId, deadlineMillis)
            WidgetPreferencesManager.saveTitle(this, appWidgetId, selected.title)

            val resultIntent = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            setResult(Activity.RESULT_OK, resultIntent)

            CountdownWidget.forceUpdateAll(this)

            Toast.makeText(this, "Widget added with '${selected.title}'", Toast.LENGTH_SHORT).show()
            finish()
        }

        cancelButton.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }
}