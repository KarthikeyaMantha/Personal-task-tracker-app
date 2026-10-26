package com.personal.daymark.core.timeline

import com.personal.daymark.data.local.entity.TimeBlockEntity

/**
 * Utility for detecting time block conflicts and overlaps on the daily timeline.
 */
object TimeBlockConflictHelper {

    /**
     * Checks whether interval A [startA, startA + durationA] overlaps with interval B [startB, startB + durationB].
     * Two intervals overlap if and only if startA < endB and endA > startB.
     */
    fun isOverlapping(
        startA: Int,
        durationA: Int,
        startB: Int,
        durationB: Int
    ): Boolean {
        val endA = startA + durationA.coerceAtLeast(1)
        val endB = startB + durationB.coerceAtLeast(1)
        return startA < endB && endA > startB
    }

    /**
     * Finds the first busy block that conflicts with the specified target start time and duration.
     * Returns null if no conflict exists.
     */
    fun findConflictingBusyBlock(
        targetStartMinutes: Int,
        targetDurationMinutes: Int,
        busyBlocks: List<TimeBlockEntity>
    ): TimeBlockEntity? {
        return busyBlocks.firstOrNull { block ->
            isOverlapping(
                startA = targetStartMinutes,
                durationA = targetDurationMinutes,
                startB = block.getStartMinutes(),
                durationB = block.durationMinutes
            )
        }
    }

    /**
     * Formats minute offsets from midnight into a user-friendly "HH:mm" string.
     */
    fun formatMinutesToTimeString(totalMinutes: Int): String {
        val h = (totalMinutes / 60).coerceIn(0, 23)
        val m = (totalMinutes % 60).coerceIn(0, 59)
        return String.format(java.util.Locale.US, "%02d:%02d", h, m)
    }
}
