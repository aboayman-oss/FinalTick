package com.aboayman.finaltick

import android.content.Context
import androidx.core.content.edit

class DeadlineRepository(context: Context) {

    private val prefs = context.getSharedPreferences("finaltick_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val DEADLINES_KEY = "deadlines"
        private const val ACTIVE_TIMESTAMP_KEY = "deadline_timestamp"
        private const val ACTIVE_TITLE_KEY = "countdown_title"
        private const val ACTIVE_CREATED_AT_KEY = "countdown_createdAt"
    }

    fun getDeadlines(): Set<String> {
        return prefs.getStringSet(DEADLINES_KEY, emptySet()) ?: emptySet()
    }

    private fun saveDeadlines(deadlines: Set<String>) {
        prefs.edit { putStringSet(DEADLINES_KEY, deadlines) }
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
        // First, remove the old entry
        val entryToRemove = current.find { entry ->
            val parts = entry.split("|", limit = 3)
            val createdAt = parts.getOrNull(0)?.toLongOrNull()
            createdAt == oldItem.createdAt
        }
        if (entryToRemove != null) {
            current.remove(entryToRemove)
        }
        // Then, add the updated entry
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

    // This function can be added anywhere inside the DeadlineRepository class
    fun restoreDeadline(deadlineEntry: String) {
        val current = getDeadlines().toMutableSet()
        current.add(deadlineEntry)
        saveDeadlines(current)
    }
}