package com.personal.daymark.core.habits

import com.personal.daymark.data.local.entity.HabitEntity
import com.personal.daymark.data.local.entity.HabitLogEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class HabitDayStatus {
    COMPLETED,
    MISSED,
    NOT_SCHEDULED,
    NOT_YET_DUE
}

data class HabitDayHistory(
    val date: LocalDate,
    val dateString: String,
    val status: HabitDayStatus,
    val isToday: Boolean
)

/**
 * Pure, reliable calculations for habit consistency, streaks, and calendar day history.
 * Derived 100% strictly from stored HabitLog records without fabricated numbers.
 */
object HabitCalculationHelper {
    private val ISO_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * Checks whether a habit is scheduled to be performed on the given date.
     * targetWeekdays is a 7-bit bitmask:
     * Monday = bit 0 (1), Tuesday = bit 1 (2), Wednesday = bit 2 (4),
     * Thursday = bit 3 (8), Friday = bit 4 (16), Saturday = bit 5 (32), Sunday = bit 6 (64).
     * 127 (1111111 binary) denotes all 7 days.
     */
    fun isScheduledOnDate(habit: HabitEntity, date: LocalDate): Boolean {
        if (habit.frequency.equals("DAILY", ignoreCase = true) && habit.targetWeekdays == 127) {
            return true
        }
        val dayOfWeekValue = date.dayOfWeek.value // 1 (Mon) to 7 (Sun)
        val bit = 1 shl (dayOfWeekValue - 1)
        return (habit.targetWeekdays and bit) != 0
    }

    /**
     * Converts a LocalDate to standard ISO string (yyyy-MM-dd).
     */
    fun toIsoDateString(date: LocalDate): String = date.format(ISO_FORMATTER)

    /**
     * Checks if a habit was completed on a specific date based strictly on stored logs.
     */
    fun isCompletedOnDate(logs: List<HabitLogEntity>, date: LocalDate): Boolean {
        val dateString = toIsoDateString(date)
        val epochDay = date.toEpochDay()
        return logs.any { log ->
            log.isCompleted && (
                (log.dateString.isNotEmpty() && log.dateString == dateString) ||
                (log.date == epochDay)
            )
        }
    }

    /**
     * Calculates completions in the last N days (including today) strictly from logs.
     */
    fun calculateCompletionsInWindow(
        logs: List<HabitLogEntity>,
        today: LocalDate,
        daysCount: Int
    ): Int {
        val startDate = today.minusDays(daysCount.toLong() - 1)
        var count = 0
        var current = startDate
        while (!current.isAfter(today)) {
            if (isCompletedOnDate(logs, current)) {
                count++
            }
            current = current.plusDays(1)
        }
        return count
    }

    /**
     * Calculates the current consecutive run (streak) strictly from real logs.
     * Starts checking from today (if completed today) or yesterday (if today is not completed yet).
     * If today is completed: counts today + previous consecutive scheduled days that were completed.
     * If today is not completed:
     *   - If today is scheduled, today is not missed yet (day is still ongoing), so streak starts checking from yesterday.
     *   - If today is not scheduled, streak starts checking from the most recent scheduled day.
     * Breaks as soon as a past scheduled day is not completed in stored logs.
     * Never invents or inflates streaks!
     */
    fun calculateCurrentStreak(
        habit: HabitEntity,
        logs: List<HabitLogEntity>,
        today: LocalDate
    ): Int {
        val completedToday = isCompletedOnDate(logs, today)
        var streak = 0

        var checkDate = if (completedToday) {
            streak++
            today.minusDays(1)
        } else {
            today.minusDays(1)
        }

        val habitCreationDate = Instant.ofEpochMilli(habit.createdAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        while (!checkDate.isBefore(habitCreationDate)) {
            if (isScheduledOnDate(habit, checkDate)) {
                if (isCompletedOnDate(logs, checkDate)) {
                    streak++
                } else {
                    // Encountered an incomplete scheduled day in the past -> streak ends!
                    break
                }
            }
            // Non-scheduled days (e.g. rest days on weekends) do not break the streak
            checkDate = checkDate.minusDays(1)
        }

        return streak
    }

    /**
     * Calculates the all-time longest consecutive streak of scheduled completions strictly from real logs.
     * Starts from habit creation date up to today.
     */
    fun calculateLongestStreak(
        habit: HabitEntity,
        logs: List<HabitLogEntity>,
        today: LocalDate
    ): Int {
        val habitCreationDate = Instant.ofEpochMilli(habit.createdAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        var maxStreak = 0
        var currentRun = 0
        var date = habitCreationDate

        while (!date.isAfter(today)) {
            if (isScheduledOnDate(habit, date)) {
                if (isCompletedOnDate(logs, date)) {
                    currentRun++
                    if (currentRun > maxStreak) {
                        maxStreak = currentRun
                    }
                } else {
                    currentRun = 0
                }
            }
            date = date.plusDays(1)
        }
        return maxStreak
    }

    /**
     * Calculates the overall habit completion rate for the current week up to today.
     * Evaluates strictly real scheduled habit days that have elapsed.
     * Returns an integer percentage 0-100.
     */
    fun calculateWeeklyHabitCompletionRate(
        habitsWithLogs: List<com.personal.daymark.data.local.entity.HabitWithLogs>,
        today: LocalDate = LocalDate.now()
    ): Int {
        val monday = today.with(java.time.DayOfWeek.MONDAY)
        var scheduledDays = 0
        var completedDays = 0

        var cur = monday
        while (!cur.isAfter(today)) {
            for (hwl in habitsWithLogs) {
                val habit = hwl.habit
                if (isScheduledOnDate(habit, cur)) {
                    val creationDate = Instant.ofEpochMilli(habit.createdAt)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    if (!cur.isBefore(creationDate)) {
                        scheduledDays++
                        if (isCompletedOnDate(hwl.logs, cur)) {
                            completedDays++
                        }
                    }
                }
            }
            cur = cur.plusDays(1)
        }

        if (scheduledDays == 0) return 0
        return ((completedDays.toDouble() / scheduledDays.toDouble()) * 100).toInt()
    }

    /**
     * Generates a calendar-style history for the specified number of days ending at today.
     * Every day is classified as:
     * - COMPLETED: log exists and is completed.
     * - MISSED: past scheduled day without a completion log (NEVER erased or auto-deleted).
     * - NOT_SCHEDULED: not in target weekdays or before habit creation.
     * - NOT_YET_DUE: future date or today before completion.
     */
    fun buildHistoryDays(
        habit: HabitEntity,
        logs: List<HabitLogEntity>,
        today: LocalDate,
        daysCount: Int = 14
    ): List<HabitDayHistory> {
        val habitCreationDate = Instant.ofEpochMilli(habit.createdAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        val startDate = today.minusDays(daysCount.toLong() - 1)
        val history = mutableListOf<HabitDayHistory>()

        var current = startDate
        while (!current.isAfter(today)) {
            val isToday = current == today
            val dateString = toIsoDateString(current)
            val isScheduled = isScheduledOnDate(habit, current)
            val isCompleted = isCompletedOnDate(logs, current)

            val status = when {
                isCompleted -> HabitDayStatus.COMPLETED
                !isScheduled -> HabitDayStatus.NOT_SCHEDULED
                current.isBefore(habitCreationDate) -> HabitDayStatus.NOT_SCHEDULED
                isToday -> HabitDayStatus.NOT_YET_DUE // Today remains "not yet due" until completed
                else -> HabitDayStatus.MISSED // Past scheduled day with no log is visibly MISSED!
            }

            history.add(
                HabitDayHistory(
                    date = current,
                    dateString = dateString,
                    status = status,
                    isToday = isToday
                )
            )
            current = current.plusDays(1)
        }

        return history
    }
}
