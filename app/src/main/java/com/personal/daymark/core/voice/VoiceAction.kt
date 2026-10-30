package com.personal.daymark.core.voice

import com.personal.daymark.core.recurrence.RecurrenceRule

/**
 * Structured action parsed from user speech input.
 */
sealed class VoiceAction {

    /**
     * Create a new task with extracted due dates, times, priorities, or categories.
     */
    data class CreateTask(
        val title: String,
        val dueDate: Long? = null,
        val dueTimeMinutes: Int? = null,
        val recurrenceRule: RecurrenceRule? = null,
        val priority: Int = 0,
        val categoryId: Long? = null,
        val isMedicine: Boolean = false,
        val explanation: String = ""
    ) : VoiceAction()

    /**
     * Mark an existing task as completed by search/query title.
     */
    data class CompleteTask(
        val taskQuery: String,
        val explanation: String = ""
    ) : VoiceAction()

    /**
     * Log increment progress for a metric habit (e.g. water, sleep).
     */
    data class LogHabitProgress(
        val habitQuery: String,
        val amount: Int = 1,
        val unit: String? = null,
        val explanation: String = ""
    ) : VoiceAction()

    /**
     * Toggle today's completion for a standard habit (e.g. meditation, workout).
     */
    data class ToggleHabit(
        val habitQuery: String,
        val explanation: String = ""
    ) : VoiceAction()

    /**
     * Create a new habit with specified frequency and routine.
     */
    data class CreateHabit(
        val name: String,
        val frequency: String = "DAILY",
        val targetWeekdays: Int = 127,
        val routineTimeOfDay: String = "ANYTIME",
        val explanation: String = ""
    ) : VoiceAction()

    /**
     * When speech cannot be reliably mapped to an action.
     */
    data class Unknown(
        val rawText: String,
        val suggestion: String? = null
    ) : VoiceAction()
}

