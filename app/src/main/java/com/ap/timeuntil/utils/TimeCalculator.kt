package com.ap.timeuntil.utils

import java.util.concurrent.TimeUnit

object TimeCalculator {

    data class TimeRemaining(
        val years: Long,
        val months: Long,
        val days: Long,
        val hours: Long,
        val minutes: Long,
        val seconds: Long,
        val isPast: Boolean
    )

    fun calculateTimeRemaining(targetTimeMillis: Long): TimeRemaining {
        val currentTimeMillis = System.currentTimeMillis()
        var diff = targetTimeMillis - currentTimeMillis
        val isPast = diff < 0
        diff = kotlin.math.abs(diff)

        val seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
        val hours = TimeUnit.MILLISECONDS.toHours(diff) % 24
        val totalDays = TimeUnit.MILLISECONDS.toDays(diff)

        val years = totalDays / 365
        val remainingDaysAfterYears = totalDays % 365
        val months = remainingDaysAfterYears / 30
        val days = remainingDaysAfterYears % 30

        return TimeRemaining(years, months, days, hours, minutes, seconds, isPast)
    }

    fun formatTimeRemaining(timeRemaining: TimeRemaining, includeSeconds: Boolean = true): String {
        val parts = mutableListOf<String>()

        if (timeRemaining.years > 0) {
            parts.add("${timeRemaining.years} ${if (timeRemaining.years == 1L) "year" else "years"}")
        }
        if (timeRemaining.months > 0) {
            parts.add("${timeRemaining.months} ${if (timeRemaining.months == 1L) "month" else "months"}")
        }
        if (timeRemaining.days > 0) {
            parts.add("${timeRemaining.days} ${if (timeRemaining.days == 1L) "day" else "days"}")
        }
        if (timeRemaining.hours > 0) {
            parts.add("${timeRemaining.hours} ${if (timeRemaining.hours == 1L) "hour" else "hours"}")
        }
        if (timeRemaining.minutes > 0) {
            parts.add("${timeRemaining.minutes} ${if (timeRemaining.minutes == 1L) "minute" else "minutes"}")
        }
        if (includeSeconds && timeRemaining.seconds > 0) {
            parts.add("${timeRemaining.seconds} ${if (timeRemaining.seconds == 1L) "second" else "seconds"}")
        }

        return if (parts.isEmpty()) {
            if (timeRemaining.isPast) "Event has passed" else "Less than a second"
        } else {
            val prefix = if (timeRemaining.isPast) "Passed " else ""
            prefix + parts.joinToString(" ")
        }
    }
}