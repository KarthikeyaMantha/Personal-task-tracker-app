package com.personal.daymark.core.notifications

import com.personal.daymark.data.local.entity.HabitEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class HabitReminderSchedulerTest {

    @Test
    fun calculateNextHabitTriggerMillis_returnsNextOccurrenceTodayIfFuture() {
        // Suppose current time is 7:00 AM on Monday
        val nowCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val habit = HabitEntity(
            id = 1L,
            name = "Morning Stretch",
            frequency = "DAILY",
            targetWeekdays = 127,
            reminderTime = 8 * 60 // 8:00 AM
        )

        val trigger = AlarmManagerReminderScheduler.calculateNextHabitTriggerMillis(habit, nowCal.timeInMillis)
        assertNotNull(trigger)

        val triggerCal = Calendar.getInstance().apply { timeInMillis = trigger!! }
        assertEquals(8, triggerCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, triggerCal.get(Calendar.MINUTE))
        assertEquals(nowCal.get(Calendar.DAY_OF_YEAR), triggerCal.get(Calendar.DAY_OF_YEAR))
    }

    @Test
    fun calculateNextHabitTriggerMillis_advancesToTomorrowIfTodayTimePassed() {
        // Current time is 9:00 AM on Monday
        val nowCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val habit = HabitEntity(
            id = 1L,
            name = "Morning Stretch",
            frequency = "DAILY",
            targetWeekdays = 127,
            reminderTime = 8 * 60 // 8:00 AM
        )

        val trigger = AlarmManagerReminderScheduler.calculateNextHabitTriggerMillis(habit, nowCal.timeInMillis)
        assertNotNull(trigger)

        val triggerCal = Calendar.getInstance().apply { timeInMillis = trigger!! }
        assertEquals(8, triggerCal.get(Calendar.HOUR_OF_DAY))
        assertTrue(triggerCal.timeInMillis > nowCal.timeInMillis)
    }

    @Test
    fun calculateNextHabitTriggerMillis_returnsNullWhenNoReminderTime() {
        val habit = HabitEntity(
            id = 1L,
            name = "No Reminder Habit",
            reminderTime = null
        )
        val trigger = AlarmManagerReminderScheduler.calculateNextHabitTriggerMillis(habit)
        assertNull(trigger)
    }
}

