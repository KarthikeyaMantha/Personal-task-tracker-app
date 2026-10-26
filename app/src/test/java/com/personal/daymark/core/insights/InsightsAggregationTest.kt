package com.personal.daymark.core.insights

import com.personal.daymark.core.habits.HabitCalculationHelper
import com.personal.daymark.data.local.entity.FocusSessionEntity
import com.personal.daymark.data.local.entity.HabitEntity
import com.personal.daymark.data.local.entity.HabitLogEntity
import com.personal.daymark.data.local.entity.HabitWithLogs
import com.personal.daymark.data.local.entity.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

class InsightsAggregationTest {

    private val baseDate = LocalDate.of(2026, 10, 26) // Monday

    @Test
    fun habitStreak_consecutiveCompletedScheduledDays_returnsAccurateCount() {
        val habit = HabitEntity(
            id = 1L,
            name = "Morning Journal",
            frequency = "DAILY",
            targetWeekdays = 127, // All 7 days
            createdAt = baseDate.minusDays(10).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )

        val logs = listOf(
            HabitLogEntity(habitId = 1L, date = baseDate.toEpochDay(), dateString = baseDate.toString(), isCompleted = true),
            HabitLogEntity(habitId = 1L, date = baseDate.minusDays(1).toEpochDay(), dateString = baseDate.minusDays(1).toString(), isCompleted = true),
            HabitLogEntity(habitId = 1L, date = baseDate.minusDays(2).toEpochDay(), dateString = baseDate.minusDays(2).toString(), isCompleted = true),
            HabitLogEntity(habitId = 1L, date = baseDate.minusDays(3).toEpochDay(), dateString = baseDate.minusDays(3).toString(), isCompleted = true)
        )

        val streak = HabitCalculationHelper.calculateCurrentStreak(habit, logs, baseDate)
        assertEquals(4, streak)
    }

    @Test
    fun habitStreak_missedScheduledDayInPast_resetsCurrentStreak() {
        val habit = HabitEntity(
            id = 1L,
            name = "Morning Journal",
            frequency = "DAILY",
            targetWeekdays = 127,
            createdAt = baseDate.minusDays(10).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )

        // Completed today and yesterday, but missed 2 days ago
        val logs = listOf(
            HabitLogEntity(habitId = 1L, date = baseDate.toEpochDay(), dateString = baseDate.toString(), isCompleted = true),
            HabitLogEntity(habitId = 1L, date = baseDate.minusDays(1).toEpochDay(), dateString = baseDate.minusDays(1).toString(), isCompleted = true),
            // Day -2 is MISSED (no log)
            HabitLogEntity(habitId = 1L, date = baseDate.minusDays(3).toEpochDay(), dateString = baseDate.minusDays(3).toString(), isCompleted = true),
            HabitLogEntity(habitId = 1L, date = baseDate.minusDays(4).toEpochDay(), dateString = baseDate.minusDays(4).toString(), isCompleted = true)
        )

        val streak = HabitCalculationHelper.calculateCurrentStreak(habit, logs, baseDate)
        assertEquals(2, streak)
    }

    @Test
    fun habitStreak_longestStreak_identifiesHistoricalPeakAccurately() {
        val habit = HabitEntity(
            id = 1L,
            name = "Exercise",
            frequency = "DAILY",
            targetWeekdays = 127,
            createdAt = baseDate.minusDays(10).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )

        // Historical streak of 5 days, followed by a gap, then current streak of 2 days
        val logs = listOf(
            // Past 5-day streak (-9 to -5)
            HabitLogEntity(habitId = 1L, date = baseDate.minusDays(9).toEpochDay(), dateString = baseDate.minusDays(9).toString(), isCompleted = true),
            HabitLogEntity(habitId = 1L, date = baseDate.minusDays(8).toEpochDay(), dateString = baseDate.minusDays(8).toString(), isCompleted = true),
            HabitLogEntity(habitId = 1L, date = baseDate.minusDays(7).toEpochDay(), dateString = baseDate.minusDays(7).toString(), isCompleted = true),
            HabitLogEntity(habitId = 1L, date = baseDate.minusDays(6).toEpochDay(), dateString = baseDate.minusDays(6).toString(), isCompleted = true),
            HabitLogEntity(habitId = 1L, date = baseDate.minusDays(5).toEpochDay(), dateString = baseDate.minusDays(5).toString(), isCompleted = true),
            // Current 2-day streak (-1 to today)
            HabitLogEntity(habitId = 1L, date = baseDate.minusDays(1).toEpochDay(), dateString = baseDate.minusDays(1).toString(), isCompleted = true),
            HabitLogEntity(habitId = 1L, date = baseDate.toEpochDay(), dateString = baseDate.toString(), isCompleted = true)
        )

        val longest = HabitCalculationHelper.calculateLongestStreak(habit, logs, baseDate)
        assertEquals(5, longest)
    }

    @Test
    fun habitWeeklyCompletionRate_calculatesAccuratePercentage() {
        val habit = HabitEntity(
            id = 1L,
            name = "Reading",
            frequency = "DAILY",
            targetWeekdays = 127,
            createdAt = baseDate.minusDays(30).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )

        // Thursday (baseDate = Monday + 3 days)
        val thursday = baseDate.plusDays(3)

        // Mon, Tue, Wed completed; Thu incomplete -> 3 of 4 = 75%
        val logs = listOf(
            HabitLogEntity(habitId = 1L, date = baseDate.toEpochDay(), dateString = baseDate.toString(), isCompleted = true),
            HabitLogEntity(habitId = 1L, date = baseDate.plusDays(1).toEpochDay(), dateString = baseDate.plusDays(1).toString(), isCompleted = true),
            HabitLogEntity(habitId = 1L, date = baseDate.plusDays(2).toEpochDay(), dateString = baseDate.plusDays(2).toString(), isCompleted = true)
        )

        val rate = HabitCalculationHelper.calculateWeeklyHabitCompletionRate(
            listOf(HabitWithLogs(habit, logs)),
            today = thursday
        )

        assertEquals(75, rate)
    }

    @Test
    fun focusAggregation_sumsOnlyFocusSessionsWithinDateRange() {
        val dayStart = baseDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val dayEnd = baseDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1

        val sessions = listOf(
            FocusSessionEntity(id = 1L, durationMinutes = 25, sessionType = "FOCUS", startedAt = dayStart + 1000),
            FocusSessionEntity(id = 2L, durationMinutes = 50, sessionType = "FOCUS", startedAt = dayStart + 5000),
            FocusSessionEntity(id = 3L, durationMinutes = 15, sessionType = "SHORT_BREAK", startedAt = dayStart + 8000), // Break should be excluded
            FocusSessionEntity(id = 4L, durationMinutes = 45, sessionType = "FOCUS", startedAt = dayEnd + 10000) // Outside range
        )

        val totalFocusMinutes = sessions
            .filter { it.sessionType == "FOCUS" && it.startedAt in dayStart..dayEnd }
            .sumOf { it.durationMinutes }

        assertEquals(75, totalFocusMinutes)
    }

    @Test
    fun taskCompletionAggregation_countsCompletedTasksCorrectly() {
        val rangeStart = baseDate.minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val rangeEnd = baseDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val tasks = listOf(
            TaskEntity(id = 1L, title = "Task 1", isCompleted = true, completionTimestamp = rangeStart + 5000),
            TaskEntity(id = 2L, title = "Task 2", isCompleted = true, completionTimestamp = rangeStart + 15000),
            TaskEntity(id = 3L, title = "Task 3", isCompleted = false, completionTimestamp = null), // Incomplete
            TaskEntity(id = 4L, title = "Task 4", isCompleted = true, completionTimestamp = rangeEnd + 5000) // Beyond range
        )

        val completedCount = tasks.count { it.isCompleted && (it.completionTimestamp ?: 0) in rangeStart..rangeEnd }
        assertEquals(2, completedCount)
    }
}
