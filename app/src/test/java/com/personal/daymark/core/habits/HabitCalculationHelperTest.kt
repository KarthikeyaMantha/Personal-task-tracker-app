package com.personal.daymark.core.habits

import com.personal.daymark.data.local.entity.HabitEntity
import com.personal.daymark.data.local.entity.HabitLogEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HabitCalculationHelperTest {

    private val baseDate = LocalDate.of(2026, 10, 26) // Monday

    @Test
    fun isScheduledOnDate_daily_returnsTrueForAllDays() {
        val dailyHabit = HabitEntity(
            id = 1L,
            name = "Morning Hydration",
            frequency = "DAILY",
            targetWeekdays = 127
        )

        for (offset in 0..6) {
            val date = baseDate.plusDays(offset.toLong())
            assertTrue("Should be scheduled on $date", HabitCalculationHelper.isScheduledOnDate(dailyHabit, date))
        }
    }

    @Test
    fun isScheduledOnDate_specificWeekdays_filtersAccurately() {
        // Monday (1) + Wednesday (4) + Friday (16) = bitmask 21
        val mwfHabit = HabitEntity(
            id = 2L,
            name = "Gym Workout",
            frequency = "SPECIFIC_DAYS",
            targetWeekdays = 1 or 4 or 16
        )

        val monday = LocalDate.of(2026, 10, 26)
        val tuesday = LocalDate.of(2026, 10, 27)
        val wednesday = LocalDate.of(2026, 10, 28)
        val thursday = LocalDate.of(2026, 10, 29)
        val friday = LocalDate.of(2026, 10, 30)
        val saturday = LocalDate.of(2026, 10, 31)
        val sunday = LocalDate.of(2026, 11, 1)

        assertTrue("Monday should be scheduled", HabitCalculationHelper.isScheduledOnDate(mwfHabit, monday))
        assertFalse("Tuesday should NOT be scheduled", HabitCalculationHelper.isScheduledOnDate(mwfHabit, tuesday))
        assertTrue("Wednesday should be scheduled", HabitCalculationHelper.isScheduledOnDate(mwfHabit, wednesday))
        assertFalse("Thursday should NOT be scheduled", HabitCalculationHelper.isScheduledOnDate(mwfHabit, thursday))
        assertTrue("Friday should be scheduled", HabitCalculationHelper.isScheduledOnDate(mwfHabit, friday))
        assertFalse("Saturday should NOT be scheduled", HabitCalculationHelper.isScheduledOnDate(mwfHabit, saturday))
        assertFalse("Sunday should NOT be scheduled", HabitCalculationHelper.isScheduledOnDate(mwfHabit, sunday))
    }

    @Test
    fun isCompletedOnDate_matchesIsoDateStringAndEpochDay() {
        val date = LocalDate.of(2026, 10, 26)
        val isoStr = "2026-10-26"
        val epochDay = date.toEpochDay()

        val logs = listOf(
            HabitLogEntity(id = 1L, habitId = 1L, date = epochDay, dateString = isoStr, isCompleted = true)
        )

        assertTrue(HabitCalculationHelper.isCompletedOnDate(logs, date))
        assertFalse(HabitCalculationHelper.isCompletedOnDate(logs, date.plusDays(1)))
    }

    @Test
    fun preservationOfMissedDays_pastScheduledDaysWithoutLogsRemainMissed() {
        val today = LocalDate.of(2026, 10, 26) // Monday
        // Created 5 days ago (Wednesday Oct 21)
        val createdMillis = today.minusDays(5).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val dailyHabit = HabitEntity(
            id = 1L,
            name = "Reading",
            frequency = "DAILY",
            targetWeekdays = 127,
            createdAt = createdMillis
        )

        // Only completed yesterday (Oct 25) and 3 days ago (Oct 23)
        // Days Oct 22 and Oct 24 were skipped (missed)
        val logs = listOf(
            HabitLogEntity(
                id = 1L,
                habitId = 1L,
                date = today.minusDays(1).toEpochDay(),
                dateString = today.minusDays(1).toString(),
                isCompleted = true
            ),
            HabitLogEntity(
                id = 2L,
                habitId = 1L,
                date = today.minusDays(3).toEpochDay(),
                dateString = today.minusDays(3).toString(),
                isCompleted = true
            )
        )

        val history = HabitCalculationHelper.buildHistoryDays(dailyHabit, logs, today, daysCount = 6)

        val oct22 = history.find { it.date == today.minusDays(4) }
        val oct23 = history.find { it.date == today.minusDays(3) }
        val oct24 = history.find { it.date == today.minusDays(2) }
        val oct25 = history.find { it.date == today.minusDays(1) }
        val oct26 = history.find { it.date == today }

        assertEquals("Oct 22 was skipped -> must remain visibly MISSED", HabitDayStatus.MISSED, oct22?.status)
        assertEquals("Oct 23 was logged -> COMPLETED", HabitDayStatus.COMPLETED, oct23?.status)
        assertEquals("Oct 24 was skipped -> must remain visibly MISSED", HabitDayStatus.MISSED, oct24?.status)
        assertEquals("Oct 25 was logged -> COMPLETED", HabitDayStatus.COMPLETED, oct25?.status)
        assertEquals("Today Oct 26 not yet completed -> NOT_YET_DUE", HabitDayStatus.NOT_YET_DUE, oct26?.status)
    }

    @Test
    fun streakCalculation_consecutiveRunFromRealLogs() {
        val today = LocalDate.of(2026, 10, 26)
        val createdMillis = today.minusDays(10).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

        val dailyHabit = HabitEntity(
            id = 1L,
            name = "Meditation",
            frequency = "DAILY",
            targetWeekdays = 127,
            createdAt = createdMillis
        )

        // Completed 3 consecutive days: today, yesterday (d-1), and day before (d-2)
        // Day d-3 was missed
        val logs = listOf(
            HabitLogEntity(id = 1L, habitId = 1L, date = today.toEpochDay(), dateString = today.toString(), isCompleted = true),
            HabitLogEntity(id = 2L, habitId = 1L, date = today.minusDays(1).toEpochDay(), dateString = today.minusDays(1).toString(), isCompleted = true),
            HabitLogEntity(id = 3L, habitId = 1L, date = today.minusDays(2).toEpochDay(), dateString = today.minusDays(2).toString(), isCompleted = true),
            // Older log on d-5
            HabitLogEntity(id = 4L, habitId = 1L, date = today.minusDays(5).toEpochDay(), dateString = today.minusDays(5).toString(), isCompleted = true)
        )

        val streak = HabitCalculationHelper.calculateCurrentStreak(dailyHabit, logs, today)
        assertEquals("Streak should be exactly 3 from real logs", 3, streak)
    }

    @Test
    fun streakCalculation_restDaysDoNotBreakStreakForWeekdayHabits() {
        // Weekday habit: Mon - Fri only (bitmask = 1 + 2 + 4 + 8 + 16 = 31)
        val monday = LocalDate.of(2026, 10, 26) // Monday
        val createdMillis = monday.minusDays(14).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

        val weekdayHabit = HabitEntity(
            id = 1L,
            name = "Deep Work Routine",
            frequency = "SPECIFIC_DAYS",
            targetWeekdays = 31,
            createdAt = createdMillis
        )

        // Completed Friday Oct 23, Thursday Oct 22, Wednesday Oct 21
        // Weekend Oct 24-25 are non-scheduled rest days
        // Today Monday Oct 26 completed
        val logs = listOf(
            HabitLogEntity(id = 1L, habitId = 1L, date = monday.toEpochDay(), dateString = monday.toString(), isCompleted = true),
            HabitLogEntity(id = 2L, habitId = 1L, date = monday.minusDays(3).toEpochDay(), dateString = monday.minusDays(3).toString(), isCompleted = true), // Friday
            HabitLogEntity(id = 3L, habitId = 1L, date = monday.minusDays(4).toEpochDay(), dateString = monday.minusDays(4).toString(), isCompleted = true), // Thursday
            HabitLogEntity(id = 4L, habitId = 1L, date = monday.minusDays(5).toEpochDay(), dateString = monday.minusDays(5).toString(), isCompleted = true)  // Wednesday
        )

        val streak = HabitCalculationHelper.calculateCurrentStreak(weekdayHabit, logs, monday)
        assertEquals("Weekend rest days must not break weekday streak (4 completed scheduled days)", 4, streak)
    }

    @Test
    fun consistencyWindow_calculatesCompletionsInLast7And30Days() {
        val today = LocalDate.of(2026, 10, 26)
        val logs = (0..10).map { offset ->
            val date = today.minusDays(offset.toLong())
            HabitLogEntity(
                id = offset.toLong(),
                habitId = 1L,
                date = date.toEpochDay(),
                dateString = date.toString(),
                isCompleted = true
            )
        }

        val last7 = HabitCalculationHelper.calculateCompletionsInWindow(logs, today, 7)
        val last30 = HabitCalculationHelper.calculateCompletionsInWindow(logs, today, 30)

        assertEquals(7, last7)
        assertEquals(11, last30)
    }
}
