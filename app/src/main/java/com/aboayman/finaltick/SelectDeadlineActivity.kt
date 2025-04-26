package com.aboayman.finaltick

import android.content.Context
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SelectDeadlineActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_FinalTick)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_select_deadline)

        val titleText = findViewById<TextView>(R.id.tvDeadlinePickerTitle)
        val recycler = findViewById<RecyclerView>(R.id.rvDeadlineList)

        val prefs = getSharedPreferences("finaltick_prefs", Context.MODE_PRIVATE)
        val saved = prefs.getStringSet("deadlines", null)?.toList()?.sorted() ?: emptyList()

        val items = saved.mapNotNull {
            val parts = it.split("|", limit = 3)
            val createdAt = parts.getOrNull(0)?.toLongOrNull() ?: System.currentTimeMillis()
            val timestamp = parts.getOrNull(1)?.toLongOrNull() ?: return@mapNotNull null
            val title = parts.getOrElse(2) { "(No title)" }
            DeadlineItem(title, timestamp, createdAt)
        }

        if (items.isEmpty()) {
            titleText.text = "No deadlines saved."
            return
        }

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = DeadlineAdapter(items) { selected ->
            prefs.edit()
                .putLong("deadline_timestamp", selected.timestamp)
                .putString("latest_title", selected.title)
                .putString(
                    "widget_deadline",
                    "${selected.createdAt}|${selected.timestamp}|${selected.title}"
                ) // 🔥 add this line
                .apply()

            CountdownWidget.forceUpdateAll(this)
            Toast.makeText(this, "${selected.title} → Selected!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}