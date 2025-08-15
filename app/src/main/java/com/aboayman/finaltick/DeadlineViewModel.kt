package com.aboayman.finaltick

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class DeadlineViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DeadlineRepository(application)

    private val _deadlines = MutableLiveData<List<DeadlineItem>>()
    val deadlines: LiveData<List<DeadlineItem>> = _deadlines

    // Variable to hold the last deleted item's data for the undo action
    private var lastDeletedEntry: String? = null

    fun loadDeadlines() {
        val savedSet = repository.getDeadlines()
        val deadlineList = savedSet.mapNotNull { entry ->
            val parts = entry.split("|", limit = 3)
            val createdAt = parts.getOrNull(0)?.toLongOrNull() ?: System.currentTimeMillis()
            val timestamp = parts.getOrNull(1)?.toLongOrNull() ?: return@mapNotNull null
            val title = parts.getOrNull(2) ?: "(No title)"
            DeadlineItem(title, timestamp, createdAt)
        }.sortedBy { it.timestamp }
        _deadlines.value = deadlineList
    }

    fun addDeadline(title: String, timestamp: Long) {
        val createdAt = System.currentTimeMillis()
        repository.addDeadline(createdAt, timestamp, title)
        setActiveDeadline(DeadlineItem(title, timestamp, createdAt))
        loadDeadlines() // Reload the list to reflect the change
    }

    fun deleteDeadline(item: DeadlineItem) {
        // When deleting, save the entry string before it's removed
        lastDeletedEntry = repository.deleteDeadline(item)
        loadDeadlines()
    }

    // New function to handle the undo action
    fun undoDelete() {
        lastDeletedEntry?.let {
            repository.restoreDeadline(it)
            loadDeadlines()
            lastDeletedEntry = null // Clear it after restoring
        }
    }

    fun updateDeadline(oldItem: DeadlineItem, newTimestamp: Long, newTitle: String) {
        repository.updateDeadline(oldItem, newTimestamp, newTitle)
        loadDeadlines()
    }

    fun clearAllDeadlines() {
        repository.clearAll()
        loadDeadlines()
    }

    fun setActiveDeadline(item: DeadlineItem) {
        repository.setActiveDeadline(item)
    }
}