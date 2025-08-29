package com.aboayman.finaltick

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

data class CalculateResult(
    val millisUntil: Long = 0L,
    val timeUntilFormatted: String = "00:00:00",
    val totalSleepHours: Float = 0f,
    val adjustedCourseHours: Float = 0f,
    val speedGainHours: Float = 0f,
    val sleepDiffHoursTotal: Float = 0f, // absolute gain/loss across days
    val sleepDiffPositive: Boolean = true,
    val remainingFormatted: String = "00:00:00"
)

class CalculateViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DeadlineRepository(application)

    private val sleepHours = MutableStateFlow(8f)
    private val courseHours = MutableStateFlow(0f)
    private val playbackSpeed = MutableStateFlow(1f)

    private val ticker = MutableStateFlow(System.currentTimeMillis())

    val result: StateFlow<CalculateResult>

    init {
        // Start ticker
        viewModelScope.launch {
            while (true) {
                ticker.value = System.currentTimeMillis()
                delay(1000)
            }
        }

        result =
            combine(sleepHours, courseHours, playbackSpeed, ticker) { sleep, course, speed, now ->
                val deadline = repository.getActiveDeadlineTimestamp()
                val millisUntil = (deadline - now).coerceAtLeast(0)
                val daysLeft = millisUntil.toFloat() / (1000 * 60 * 60 * 24)

                val totalSleep = sleep * daysLeft
                val adjustedCourse = if (speed > 0f) course / speed else 0f

                val totalSleepMs = (totalSleep * 3600_000).toLong()
                val courseMs = (adjustedCourse * 3600_000).toLong()
                val rem = (millisUntil - totalSleepMs - courseMs).coerceAtLeast(0)

                val sleepDiff = 8f - sleep
                val gainHoursTotal = kotlin.math.abs(sleepDiff * daysLeft)

                CalculateResult(
                    millisUntil = millisUntil,
                    timeUntilFormatted = formatHMS(millisUntil),
                    totalSleepHours = totalSleep,
                    adjustedCourseHours = adjustedCourse,
                    speedGainHours = (course - adjustedCourse).coerceAtLeast(0f),
                    sleepDiffHoursTotal = gainHoursTotal,
                    sleepDiffPositive = sleepDiff > 0f,
                    remainingFormatted = formatHMS(rem)
                )
            }.stateIn(
                viewModelScope,
                kotlinx.coroutines.flow.SharingStarted.Eagerly,
                CalculateResult()
            )
    }

    fun setSleepHours(value: Int) {
        sleepHours.value = value.toFloat()
    }

    fun setCourseHours(value: Float) {
        courseHours.value = value
    }

    fun setPlaybackSpeed(value: Float) {
        playbackSpeed.value = value
    }

    fun getActiveTitle(): String? = repository.getActiveDeadlineTitle()
    fun getActiveTimestamp(): Long = repository.getActiveDeadlineTimestamp()

    private fun formatHMS(ms: Long): String {
        val totalSeconds = (ms.coerceAtLeast(0)) / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }
}

