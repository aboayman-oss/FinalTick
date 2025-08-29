package com.aboayman.finaltick

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData

class DeadlineViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DeadlineRepository(application)

    val deadlines: LiveData<List<DeadlineItem>> = repository.deadlinesLiveData

    // Variable to hold the last deleted item's data for the undo action
    private var lastDeletedEntry: String? = null

    fun addDeadline(title: String, timestamp: Long) {
        val createdAt = System.currentTimeMillis()
        repository.addDeadline(createdAt, timestamp, title)
        setActiveDeadline(DeadlineItem(title, timestamp, createdAt))
    }

    fun deleteDeadline(item: DeadlineItem) {
        // When deleting, save the entry string before it's removed
        lastDeletedEntry = repository.deleteDeadline(item)
    }

    fun undoDelete() {
        lastDeletedEntry?.let {
            repository.restoreDeadline(it)
            lastDeletedEntry = null // Clear it after restoring
        }
    }

    fun updateDeadline(oldItem: DeadlineItem, newTimestamp: Long, newTitle: String) {
        repository.updateDeadline(oldItem, newTimestamp, newTitle)
    }

    fun clearAllDeadlines() {
        repository.clearAll()
    }

    fun setActiveDeadline(item: DeadlineItem) {
        repository.setActiveDeadline(item)
    }
}
