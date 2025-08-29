package com.aboayman.finaltick

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class DeadlineRepository(context: Context) {

    private val prefs = context.getSharedPreferences("finaltick_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val DEADLINES_KEY = "deadlines"
        private const val ACTIVE_TIMESTAMP_KEY = "deadline_timestamp"
        private const val ACTIVE_TITLE_KEY = "countdown_title"
        private const val ACTIVE_CREATED_AT_KEY = "countdown_createdAt"
    }

    private val _deadlinesLiveData = MutableLiveData<List<DeadlineItem>>()
    val deadlinesLiveData: LiveData<List<DeadlineItem>> = _deadlinesLiveData

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == DEADLINES_KEY) {
            _deadlinesLiveData.postValue(parseDeadlines(getDeadlines()))
        }
    }

    init {
        _deadlinesLiveData.value = parseDeadlines(getDeadlines())
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun getDeadlines(): Set<String> {
        return prefs.getStringSet(DEADLINES_KEY, emptySet()) ?: emptySet()
    }

    private fun parseDeadlines(savedSet: Set<String>): List<DeadlineItem> {
        return savedSet.mapNotNull { entry ->
            val parts = entry.split("|", limit = 3)
            val createdAt = parts.getOrNull(0)?.toLongOrNull() ?: System.currentTimeMillis()
            val timestamp = parts.getOrNull(1)?.toLongOrNull() ?: return@mapNotNull null
            val title = parts.getOrNull(2) ?: "(No title)"
            DeadlineItem(title, timestamp, createdAt)
        }.sortedBy { it.timestamp }
    }

    private fun saveDeadlines(deadlines: Set<String>) {
        prefs.edit { putStringSet(DEADLINES_KEY, deadlines) }
        // Proactively update LiveData
        _deadlinesLiveData.postValue(parseDeadlines(deadlines))
    }

    fun addDeadline(createdAt: Long, timestamp: Long, title: String) {
        val current = getDeadlines().toMutableSet()
        current.add("$createdAt|$timestamp|$title")
        saveDeadlines(current)
    }

    fun deleteDeadline(itemToDelete: DeadlineItem): String? {
        val current = getDeadlines().toMutableSet()
        val entryToRemove = current.find { entry ->
            val parts = entry.split("|", limit = 3)
            val createdAt = parts.getOrNull(0)?.toLongOrNull()
            createdAt == itemToDelete.createdAt
        }

        if (entryToRemove != null) {
            current.remove(entryToRemove)
            saveDeadlines(current)
            return entryToRemove
        }
        return null
    }

    fun updateDeadline(oldItem: DeadlineItem, newTimestamp: Long, newTitle: String) {
        val current = getDeadlines().toMutableSet()
        val entryToRemove = current.find { entry ->
            val parts = entry.split("|", limit = 3)
            val createdAt = parts.getOrNull(0)?.toLongOrNull()
            createdAt == oldItem.createdAt
        }
        if (entryToRemove != null) {
            current.remove(entryToRemove)
        }
        current.add("${oldItem.createdAt}|$newTimestamp|$newTitle")
        saveDeadlines(current)
    }

    fun clearAll(): Set<String> {
        val current = getDeadlines()
        saveDeadlines(emptySet())
        return current
    }

    fun setActiveDeadline(item: DeadlineItem) {
        prefs.edit {
            putLong(ACTIVE_TIMESTAMP_KEY, item.timestamp)
            putString(ACTIVE_TITLE_KEY, item.title)
            putLong(ACTIVE_CREATED_AT_KEY, item.createdAt)
        }
    }

    fun getActiveDeadlineTimestamp(): Long {
        return prefs.getLong(ACTIVE_TIMESTAMP_KEY, -1L)
    }

    fun getActiveDeadlineTitle(): String? {
        return prefs.getString(ACTIVE_TITLE_KEY, null)
    }

    fun restoreDeadline(deadlineEntry: String) {
        val current = getDeadlines().toMutableSet()
        current.add(deadlineEntry)
        saveDeadlines(current)
    }
}
