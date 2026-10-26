package com.personal.daymark.ui.timeline

import com.personal.daymark.data.local.entity.CategoryEntity
import com.personal.daymark.data.local.entity.TaskEntity
import com.personal.daymark.data.local.entity.TaskWithSubtasksAndCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyTimelineTest {

    @Test
    fun getEffectiveStartMinutes_parsesFormattedStringCorrectly() {
        val taskWithStartTime = TaskEntity(
            title = "Morning Deep Work",
            startTime = "09:30",
            durationMinutes = 60,
            dueTime = 600 // 10:00 AM fallback
        )

        // Should prioritize startTime "09:30" (9 * 60 + 30 = 570) over dueTime (600)
        assertEquals(570, taskWithStartTime.getEffectiveStartMinutes())
        assertEquals("09:30", taskWithStartTime.getEffectiveStartTime())
        assertTrue(taskWithStartTime.isTimelineScheduled)
    }

    @Test
    fun getEffectiveStartMinutes_fallsBackToDueTime() {
        val taskWithoutStartTime = TaskEntity(
            title = "Lunch with Team",
            startTime = null,
            durationMinutes = 45,
            dueTime = 13 * 60 + 15 // 13:15
        )

        assertEquals(13 * 60 + 15, taskWithoutStartTime.getEffectiveStartMinutes())
        assertEquals("13:15", taskWithoutStartTime.getEffectiveStartTime())
        assertTrue(taskWithoutStartTime.isTimelineScheduled)
    }

    @Test
    fun isTimelineScheduled_falseWhenDurationZeroOrNoTime() {
        val unscheduledNoTime = TaskEntity(
            title = "Buy groceries",
            startTime = null,
            dueTime = null,
            durationMinutes = 0
        )
        assertFalse(unscheduledNoTime.isTimelineScheduled)
        assertNull(unscheduledNoTime.getEffectiveStartMinutes())

        val unscheduledZeroDuration = TaskEntity(
            title = "Quick check",
            startTime = "15:00",
            durationMinutes = 0
        )
        assertFalse(unscheduledZeroDuration.isTimelineScheduled)
    }

    @Test
    fun timelinePartitioning_separatesScheduledFromUnscheduled() {
        val task1 = TaskWithSubtasksAndCategory(
            task = TaskEntity(id = 1L, title = "Scheduled 1", startTime = "08:00", durationMinutes = 30)
        )
        val task2 = TaskWithSubtasksAndCategory(
            task = TaskEntity(id = 2L, title = "Unscheduled 1", startTime = null, durationMinutes = 0)
        )
        val task3 = TaskWithSubtasksAndCategory(
            task = TaskEntity(id = 3L, title = "Scheduled 2", startTime = "14:00", durationMinutes = 60)
        )

        val list = listOf(task1, task2, task3)

        val scheduled = list.filter { it.task.isTimelineScheduled }
        val unscheduled = list.filter { !it.task.isTimelineScheduled }

        assertEquals(2, scheduled.size)
        assertEquals(1, unscheduled.size)
        assertEquals(1L, scheduled[0].task.id)
        assertEquals(3L, scheduled[1].task.id)
        assertEquals(2L, unscheduled[0].task.id)
    }

    @Test
    fun timeSnapping_snapsToNearest15Minutes() {
        fun snapMinutes(rawMinutes: Int): Int {
            return ((rawMinutes + 7) / 15 * 15).coerceIn(6 * 60, 21 * 60)
        }

        assertEquals(540, snapMinutes(542)) // 09:02 -> 09:00 (540)
        assertEquals(555, snapMinutes(550)) // 09:10 -> 09:15 (555)
        assertEquals(555, snapMinutes(558)) // 09:18 -> 09:15 (555)
        assertEquals(570, snapMinutes(565)) // 09:25 -> 09:30 (570)
    }
}
