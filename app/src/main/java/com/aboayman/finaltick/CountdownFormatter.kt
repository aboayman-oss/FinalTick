package com.aboayman.finaltick

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan

data class CountdownParts(
    val days: Long,
    val hours: Long,
    val minutes: Long,
    val seconds: Long
)

object CountdownFormatter {

    fun computeRemainingSeconds(
        deadlineMillis: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val diff = (deadlineMillis - nowMillis) / 1000L
        return if (diff < 0L) 0L else diff
    }

    fun computeProgress(
        createdAt: Long,
        deadline: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): Int {
        val total = (deadline - createdAt).coerceAtLeast(1L)
        val elapsed = (nowMillis - createdAt).coerceAtLeast(0L)
        return ((elapsed * 100 / total).toInt()).coerceIn(0, 100)
    }

    fun breakdown(
        remainingSecondsInput: Long,
        showDays: Boolean,
        showHours: Boolean,
        showMinutes: Boolean,
        showSeconds: Boolean
    ): CountdownParts {
        var remaining = remainingSecondsInput

        val rawDays = remaining / 86400
        val rawHours = (remaining % 86400) / 3600
        val rawMinutes = (remaining % 3600) / 60
        val rawSeconds = remaining % 60

        var days = 0L
        var hours = 0L
        var minutes = 0L
        var seconds = 0L

        if (showDays) {
            days = rawDays
            remaining -= days * 86400
        }
        if (showHours) {
            hours = remaining / 3600
            remaining -= hours * 3600
        }
        if (showMinutes) {
            minutes = remaining / 60
            remaining -= minutes * 60
        }
        if (showSeconds) {
            seconds = remaining
        }

        // Reallocate hidden units into the next shown unit down
        if (!showMinutes && showSeconds) seconds += rawMinutes * 60
        if (!showHours && (showMinutes || showSeconds)) {
            val bonus = rawHours * 3600
            if (showMinutes) {
                minutes += bonus / 60
                seconds += bonus % 60
            } else {
                seconds += bonus
            }
        }
        if (!showDays && (showHours || showMinutes || showSeconds)) {
            val bonus = rawDays * 86400
            if (showHours) {
                hours += bonus / 3600
                val leftover = bonus % 3600
                if (showMinutes) {
                    minutes += leftover / 60
                    seconds += leftover % 60
                } else {
                    seconds += leftover
                }
            } else if (showMinutes) {
                minutes += bonus / 60
                seconds += bonus % 60
            } else {
                seconds += bonus
            }
        }

        return CountdownParts(days, hours, minutes, seconds)
    }

    fun formatColon(
        parts: CountdownParts,
        showDays: Boolean,
        showHours: Boolean,
        showMinutes: Boolean,
        showSeconds: Boolean
    ): String {
        val p = mutableListOf<String>()
        if (showDays) p.add(parts.days.toString())
        if (showHours) p.add(parts.hours.toString().padStart(2, '0'))
        if (showMinutes) p.add(parts.minutes.toString().padStart(2, '0'))
        if (showSeconds) p.add(parts.seconds.toString().padStart(2, '0'))
        return p.joinToString(":")
    }

    fun formatLetters(
        parts: CountdownParts,
        showDays: Boolean,
        showHours: Boolean,
        showMinutes: Boolean,
        showSeconds: Boolean
    ): String {
        return buildString {
            if (showDays) append("${parts.days}d ")
            if (showHours) append("${parts.hours}h ")
            if (showMinutes) append("${parts.minutes}m ")
            if (showSeconds) append("${parts.seconds}s")
        }.trim()
    }

    // For the primary timer in the Activity: numbers bold, unit letters lighter/smaller.
    fun formatPrimaryTimerRich(
        context: Context,
        parts: CountdownParts,
        showDays: Boolean,
        showHours: Boolean,
        showMinutes: Boolean,
        showSeconds: Boolean
    ): CharSequence {
        val sb = SpannableStringBuilder()
        fun appendPart(value: Long, unit: String, include: Boolean, last: Boolean = false) {
            if (!include) return
            val startNum = sb.length
            val num = value.toString().padStart(if (unit != "d") 2 else 1, '0')
            sb.append(num)
            sb.setSpan(
                StyleSpan(Typeface.BOLD),
                startNum,
                startNum + num.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            val startUnit = sb.length
            sb.append(unit)
            sb.setSpan(
                RelativeSizeSpan(0.6f),
                startUnit,
                startUnit + unit.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            if (!last) sb.append("  ")
        }

        val included = listOf(showDays, showHours, showMinutes, showSeconds)
        val lastIndex = included.lastIndexOf(true)

        var idx = 0
        appendPart(parts.days, "d", showDays, idx++ == lastIndex)
        appendPart(parts.hours, "h", showHours, idx++ == lastIndex)
        appendPart(parts.minutes, "m", showMinutes, idx++ == lastIndex)
        appendPart(parts.seconds, "s", showSeconds, idx++ == lastIndex)

        return sb
    }
}