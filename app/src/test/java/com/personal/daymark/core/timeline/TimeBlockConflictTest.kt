package com.personal.daymark.core.timeline

import com.personal.daymark.data.local.entity.TimeBlockEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeBlockConflictTest {

    @Test
    fun isOverlapping_detectsOverlappingIntervals() {
        // [10:00, 11:00] (600 to 660) and [10:30, 11:30] (630 to 690) -> OVERLAP
        assertTrue(TimeBlockConflictHelper.isOverlapping(600, 60, 630, 60))

        // [10:00, 11:00] (600 to 660) and [09:30, 10:30] (570 to 630) -> OVERLAP
        assertTrue(TimeBlockConflictHelper.isOverlapping(600, 60, 570, 60))

        // [10:00, 11:00] (600 to 660) and [10:15, 10:45] (615 to 645) -> OVERLAP
        assertTrue(TimeBlockConflictHelper.isOverlapping(600, 60, 615, 30))

        // Identical slots -> OVERLAP
        assertTrue(TimeBlockConflictHelper.isOverlapping(600, 60, 600, 60))
    }

    @Test
    fun isOverlapping_allowsAdjacentAndSeparatedIntervals() {
        // [10:00, 11:00] (600 to 660) and [11:00, 12:00] (660 to 720) -> ADJACENT, NO OVERLAP
        assertFalse(TimeBlockConflictHelper.isOverlapping(600, 60, 660, 60))

        // [10:00, 11:00] (600 to 660) and [09:00, 10:00] (540 to 600) -> ADJACENT, NO OVERLAP
        assertFalse(TimeBlockConflictHelper.isOverlapping(600, 60, 540, 60))

        // [10:00, 11:00] (600 to 660) and [13:00, 14:00] (780 to 840) -> COMPLETELY SEPARATE
        assertFalse(TimeBlockConflictHelper.isOverlapping(600, 60, 780, 60))
    }

    @Test
    fun findConflictingBusyBlock_returnsMatchingBusyBlock() {
        val lunchBlock = TimeBlockEntity(
            id = 1,
            title = "Lunch Break",
            startTime = "12:00", // 720
            durationMinutes = 60, // 720 to 780
            date = "2026-10-26"
        )
        val teamMeeting = TimeBlockEntity(
            id = 2,
            title = "Team Standup",
            startTime = "15:00", // 900
            durationMinutes = 30, // 900 to 930
            date = "2026-10-26"
        )

        val busyBlocks = listOf(lunchBlock, teamMeeting)

        // Dragging task to 12:15 (735) for 30m -> conflicts with Lunch
        val conflict1 = TimeBlockConflictHelper.findConflictingBusyBlock(735, 30, busyBlocks)
        assertNotNull(conflict1)
        assertEquals("Lunch Break", conflict1?.title)

        // Dragging task to 15:10 (910) for 15m -> conflicts with Standup
        val conflict2 = TimeBlockConflictHelper.findConflictingBusyBlock(910, 15, busyBlocks)
        assertNotNull(conflict2)
        assertEquals("Team Standup", conflict2?.title)

        // Dragging task to 13:00 (780) for 45m -> adjacent to Lunch, no conflict
        val conflict3 = TimeBlockConflictHelper.findConflictingBusyBlock(780, 45, busyBlocks)
        assertNull(conflict3)

        // Dragging task to 09:00 (540) for 60m -> no conflict
        val conflict4 = TimeBlockConflictHelper.findConflictingBusyBlock(540, 60, busyBlocks)
        assertNull(conflict4)
    }
}
