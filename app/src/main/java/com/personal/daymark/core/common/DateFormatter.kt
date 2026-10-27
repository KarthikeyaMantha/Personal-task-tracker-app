package com.personal.daymark.core.common

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateFormatter {

    fun formatDueDate(dueDateMillis: Long?, dueTimeMinutes: Int?): String? {
        if (dueDateMillis == null) return null

        val taskCalendar = Calendar.getInstance().apply { timeInMillis = dueDateMillis }
        val todayCalendar = Calendar.getInstance()

        val isSameDay = taskCalendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) &&
                taskCalendar.get(Calendar.DAY_OF_YEAR) == todayCalendar.get(Calendar.DAY_OF_YEAR)

        val tomorrowCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val isTomorrow = taskCalendar.get(Calendar.YEAR) == tomorrowCalendar.get(Calendar.YEAR) &&
                taskCalendar.get(Calendar.DAY_OF_YEAR) == tomorrowCalendar.get(Calendar.DAY_OF_YEAR)

        val yesterdayCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val isYesterday = taskCalendar.get(Calendar.YEAR) == yesterdayCalendar.get(Calendar.YEAR) &&
                taskCalendar.get(Calendar.DAY_OF_YEAR) == yesterdayCalendar.get(Calendar.DAY_OF_YEAR)

        val dateLabel = when {
            isSameDay -> "Today"
            isTomorrow -> "Tomorrow"
            isYesterday -> "Yesterday"
            else -> {
                val format = if (taskCalendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR)) {
                    SimpleDateFormat("MMM d", Locale.getDefault())
                } else {
                    SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                }
                format.format(Date(dueDateMillis))
            }
        }

        return if (dueTimeMinutes != null) {
            val hours = dueTimeMinutes / 60
            val minutes = dueTimeMinutes % 60
            val timeString = String.format(Locale.getDefault(), "%02d:%02d", hours, minutes)
            "$dateLabel, $timeString"
        } else {
            dateLabel
        }
    }

    fun formatRelativeDate(dueDateMillis: Long?): String {
        return formatDueDate(dueDateMillis, null) ?: ""
    }

    fun formatTime(dueTimeMinutes: Int?): String {
        if (dueTimeMinutes == null) return ""
        val hours = dueTimeMinutes / 60
        val minutes = dueTimeMinutes % 60
        val amPm = if (hours >= 12) "PM" else "AM"
        val displayHour = when (val h = hours % 12) {
            0 -> 12
            else -> h
        }
        return String.format(Locale.getDefault(), "%d:%02d %s", displayHour, minutes, amPm)
    }

    fun getStartOfTodayMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun getEndOfTodayMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    fun getDynamicGreeting(currentHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): String {
        return when (currentHour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Good night"
        }
    }

    fun isSameDay(millis1: Long?, millis2: Long = System.currentTimeMillis()): Boolean {
        if (millis1 == null) return false
        val c1 = Calendar.getInstance().apply { timeInMillis = millis1 }
        val c2 = Calendar.getInstance().apply { timeInMillis = millis2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    fun isBeforeToday(millis: Long?): Boolean {
        if (millis == null) return false
        val taskCal = Calendar.getInstance().apply { timeInMillis = millis }
        val todayCal = Calendar.getInstance()
        if (taskCal.get(Calendar.YEAR) < todayCal.get(Calendar.YEAR)) return true
        if (taskCal.get(Calendar.YEAR) > todayCal.get(Calendar.YEAR)) return false
        return taskCal.get(Calendar.DAY_OF_YEAR) < todayCal.get(Calendar.DAY_OF_YEAR)
    }

    fun getFormattedCurrentDate(millis: Long = System.currentTimeMillis()): String {
        val format = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        return format.format(Date(millis))
    }
}
