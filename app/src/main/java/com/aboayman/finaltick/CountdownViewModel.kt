package com.aboayman.finaltick

import android.app.Application
import android.os.CountDownTimer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData


data class CountdownState(
    val remainingSeconds: Long,
    val progress: Int,
    val finished: Boolean
)

class CountdownViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableLiveData(CountdownState(0L, 0, false))
    val state: LiveData<CountdownState> = _state

    private var timer: CountDownTimer? = null

    fun start(deadline: Long, createdAt: Long) {
        cancel()
        val now = System.currentTimeMillis()
        val millisLeft = (deadline - now).coerceAtLeast(0L)

        if (millisLeft == 0L) {
            _state.postValue(
                CountdownState(
                    remainingSeconds = 0L,
                    progress = 100,
                    finished = true
                )
            )
            return
        }

        timer = object : CountDownTimer(millisLeft, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val remainingSeconds = (millisUntilFinished / 1000L).coerceAtLeast(0L)
                val progress = CountdownFormatter.computeProgress(createdAt, deadline)
                _state.postValue(CountdownState(remainingSeconds, progress, false))
            }

            override fun onFinish() {
                _state.postValue(CountdownState(0L, 100, true))
            }
        }.start()
    }

    fun cancel() {
        timer?.cancel()
        timer = null
    }

    override fun onCleared() {
        super.onCleared()
        cancel()
    }
}