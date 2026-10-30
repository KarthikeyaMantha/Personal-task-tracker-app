package com.personal.daymark.core.recurrence

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object RecurrenceHelper {

    /**
     * Calculates the next occurrence date in epoch milliseconds (start of day).
     *
     * @param currentDueDateMillis The current task's due date in epoch milliseconds.
     * @param dueTimeMinutes The current task's due time in minutes from midnight (optional).
     * @param rule The RecurrenceRule defining recurrence patterns.
     * @param fromDateMillis Anchor date millis (defaults to System.currentTimeMillis()).
     *                       Ensures that if the completed task was overdue, the generated next occurrence
     *                       is scheduled for today or a future occurrence.
     * @param zoneId TimeZone to evaluate calendar dates (default ZoneId.systemDefault()).
     * @return Epoch millisecond timestamp of the next occurrence date, or null if end condition is met.
     */
    fun calculateNextOccurrence(
        currentDueDateMillis: Long,
        dueTimeMinutes: Int?,
        rule: RecurrenceRule,
        fromDateMillis: Long = currentDueDateMillis,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long? {
        val currentDate = Instant.ofEpochMilli(currentDueDateMillis).atZone(zoneId).toLocalDate()
        val today = Instant.ofEpochMilli(fromDateMillis).atZone(zoneId).toLocalDate()

        val nextDate: LocalDate = when (rule.frequency) {
            RecurrenceFrequency.DAILY -> {
                val step = rule.interval.coerceAtLeast(1)
                var candidate = currentDate.plusDays(step.toLong())
                while (candidate.isBefore(today)) {
                    candidate = candidate.plusDays(step.toLong())
                }
                candidate
            }

            RecurrenceFrequency.WEEKDAYS -> {
                var candidate = currentDate.plusDays(1)
                while (candidate.dayOfWeek == DayOfWeek.SATURDAY || candidate.dayOfWeek == DayOfWeek.SUNDAY || candidate.isBefore(today)) {
                    candidate = candidate.plusDays(1)
                }
                candidate
            }

            RecurrenceFrequency.WEEKLY -> {
                val intervalWeeks = rule.interval.coerceAtLeast(1)
                val targetDays = if (rule.daysOfWeek.isNotEmpty()) {
                    rule.daysOfWeek.map { DayOfWeek.of(it) }.sorted()
                } else {
                    listOf(currentDate.dayOfWeek)
                }

                // Check for a day in targetDays later in the current week
                val currentDayVal = currentDate.dayOfWeek.value
                val nextDayInSameWeek = targetDays.firstOrNull { it.value > currentDayVal }

                var candidate = if (nextDayInSameWeek != null) {
                    val daysAhead = (nextDayInSameWeek.value - currentDayVal).toLong()
                    currentDate.plusDays(daysAhead)
                } else {
                    // Wrap around to the first target day in the next interval cycle
                    val firstDayVal = targetDays.first().value
                    val daysUntilEndOfWeek = (7 - currentDayVal).toLong()
                    val daysInSkipWeeks = ((intervalWeeks - 1) * 7).toLong()
                    val daysIntoTargetWeek = firstDayVal.toLong()
                    currentDate.plusDays(daysUntilEndOfWeek + daysInSkipWeeks + daysIntoTargetWeek)
                }

                // If overdue, advance full weekly intervals until on or after today
                while (candidate.isBefore(today)) {
                    candidate = candidate.plusWeeks(intervalWeeks.toLong())
                }
                candidate
            }

            RecurrenceFrequency.MONTHLY -> {
                val step = rule.interval.coerceAtLeast(1)
                var candidate = currentDate.plusMonths(step.toLong())
                while (candidate.isBefore(today)) {
                    candidate = candidate.plusMonths(step.toLong())
                }
                candidate
            }

            RecurrenceFrequency.YEARLY -> {
                val step = rule.interval.coerceAtLeast(1)
                var candidate = currentDate.plusYears(step.toLong())
                while (candidate.isBefore(today)) {
                    candidate = candidate.plusYears(step.toLong())
                }
                candidate
            }
        }

        val nextEpochMillis = nextDate.atStartOfDay(zoneId).toInstant().toEpochMilli()

        if (rule.endDateMillis != null && nextEpochMillis > rule.endDateMillis) {
            return null
        }

        return nextEpochMillis
    }
}
