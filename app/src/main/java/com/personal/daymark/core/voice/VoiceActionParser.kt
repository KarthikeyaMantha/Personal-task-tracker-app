package com.personal.daymark.core.voice

import com.personal.daymark.core.parser.TaskParser
import com.personal.daymark.core.recurrence.RecurrenceFrequency
import com.personal.daymark.core.recurrence.RecurrenceRule
import com.personal.daymark.data.local.entity.CategoryEntity
import java.util.Locale

/**
 * Parses raw speech transcripts into actionable, structured VoiceAction intents.
 * Operates 100% locally and offline without external network or API dependencies.
 */
object VoiceActionParser {

    private val WATER_REGEX = Regex(
        """(?i)^(?:i\s+)?(?:drank|drink|add|logged|log)\s*(?:(\d+)\s*(?:glasses|cups|bottles)?(?:\s+of)?)?\s*water$"""
    )
    private val WATER_COUNT_REGEX = Regex(
        """(?i)^(\d+)\s*(?:glasses|cups|bottles)?(?:\s+of)?\s*water$"""
    )
    private val SLEEP_REGEX = Regex(
        """(?i)^(?:i\s+)?(?:slept|logged|log|record)\s*(?:(\d+)\s*(?:hours|hrs)?(?:\s+of)?)?\s*sleep$"""
    )

    private val COMPLETE_TASK_EXPLICIT_REGEX = Regex(
        """(?i)^(?:complete|finish|check off)\s+task\s+(.+)$"""
    )
    private val COMPLETE_TASK_AS_DONE_REGEX = Regex(
        """(?i)^(?:mark\s+)?task\s+(.+?)\s+(?:as\s+)?(?:done|completed|finished)$"""
    )

    private val CREATE_HABIT_REGEX = Regex(
        """(?i)^(?:new|create|start|build)\s+habit\s+(.+)$"""
    )

    private val TOGGLE_HABIT_EXPLICIT_REGEX = Regex(
        """(?i)^(?:check off|did|finished|completed)\s+habit\s+(.+)$"""
    )
    private val TOGGLE_HABIT_MARK_REGEX = Regex(
        """(?i)^mark\s+habit\s+(.+?)(?:\s+(?:as\s+)?done)?$"""
    )
    private val TOGGLE_HABIT_SIMPLE_REGEX = Regex(
        """(?i)^(?:check off|did|finished|completed)\s+(.+)$"""
    )

    private val TASK_PREFIX_REGEX = Regex(
        """(?i)^(?:remind\s+me\s+to|add\s+task|create\s+task|new\s+task)\s+"""
    )

    /**
     * Parses the recognized speech transcript into a structured VoiceAction.
     */
    fun parse(
        transcript: String,
        existingCategories: List<CategoryEntity> = emptyList()
    ): VoiceAction {
        val rawClean = transcript.trim().removeSuffix(".")
        if (rawClean.isBlank()) {
            return VoiceAction.Unknown(rawText = transcript, suggestion = "Please speak a task or habit command")
        }

        // 1. Check for Health Metric Logging: Water
        WATER_REGEX.find(rawClean)?.let { match ->
            val countStr = match.groupValues[1]
            val count = countStr.toIntOrNull() ?: 1
            return VoiceAction.LogHabitProgress(
                habitQuery = "water",
                amount = count,
                unit = "glasses",
                explanation = "Log $count ${if (count == 1) "glass" else "glasses"} of water"
            )
        }
        WATER_COUNT_REGEX.find(rawClean)?.let { match ->
            val count = match.groupValues[1].toIntOrNull() ?: 1
            return VoiceAction.LogHabitProgress(
                habitQuery = "water",
                amount = count,
                unit = "glasses",
                explanation = "Log $count ${if (count == 1) "glass" else "glasses"} of water"
            )
        }

        // 2. Check for Health Metric Logging: Sleep
        SLEEP_REGEX.find(rawClean)?.let { match ->
            val hours = match.groupValues[1].toIntOrNull() ?: 8
            return VoiceAction.LogHabitProgress(
                habitQuery = "sleep",
                amount = hours,
                unit = "hours",
                explanation = "Log $hours hours of sleep"
            )
        }

        // 3. Check for Explicit Task Completion: "complete task buy milk", "mark task buy milk as done"
        COMPLETE_TASK_EXPLICIT_REGEX.find(rawClean)?.let { match ->
            val query = match.groupValues[1].trim()
            return VoiceAction.CompleteTask(
                taskQuery = query,
                explanation = "Complete task '$query'"
            )
        }
        COMPLETE_TASK_AS_DONE_REGEX.find(rawClean)?.let { match ->
            val query = match.groupValues[1].trim()
            return VoiceAction.CompleteTask(
                taskQuery = query,
                explanation = "Complete task '$query'"
            )
        }

        // 4. Check for Habit Creation: "new habit read 10 pages", "create habit gym on weekdays"
        CREATE_HABIT_REGEX.find(rawClean)?.let { match ->
            var habitBody = match.groupValues[1].trim()
            var frequency = "DAILY"
            var targetWeekdays = 127

            if (habitBody.contains("on weekdays", ignoreCase = true)) {
                frequency = "SPECIFIC_DAYS"
                targetWeekdays = 31 // Mon-Fri
                habitBody = habitBody.replace(Regex("""(?i)\s+on\s+weekdays"""), "").trim()
            } else if (habitBody.contains("every day", ignoreCase = true) || habitBody.contains("daily", ignoreCase = true)) {
                frequency = "DAILY"
                targetWeekdays = 127
                habitBody = habitBody.replace(Regex("""(?i)\s+(?:every\s+day|daily)"""), "").trim()
            }

            val capitalizedName = habitBody.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            return VoiceAction.CreateHabit(
                name = capitalizedName,
                frequency = frequency,
                targetWeekdays = targetWeekdays,
                explanation = "Create habit '$capitalizedName' (${if (frequency == "DAILY") "Every day" else "Weekdays"})"
            )
        }

        // 5. Check for Habit Check-off: "check off habit meditation", "did meditation", "finished workout"
        TOGGLE_HABIT_EXPLICIT_REGEX.find(rawClean)?.let { match ->
            val query = match.groupValues[1].trim()
            return VoiceAction.ToggleHabit(
                habitQuery = query,
                explanation = "Check off habit '$query'"
            )
        }
        TOGGLE_HABIT_MARK_REGEX.find(rawClean)?.let { match ->
            val query = match.groupValues[1].trim()
            return VoiceAction.ToggleHabit(
                habitQuery = query,
                explanation = "Check off habit '$query'"
            )
        }
        TOGGLE_HABIT_SIMPLE_REGEX.find(rawClean)?.let { match ->
            val query = match.groupValues[1].trim()
            // If the query doesn't look like a standard task sentence (e.g. "did 10 minutes meditation" or "check off workout")
            if (!query.contains("task", ignoreCase = true) && query.length < 35) {
                return VoiceAction.ToggleHabit(
                    habitQuery = query,
                    explanation = "Check off habit '$query'"
                )
            }
        }

        // 6. Default to Task Creation via Daymark's TaskParser (supports dates, times, priorities, #tags)
        val taskCleanInput = TASK_PREFIX_REGEX.replace(rawClean, "").trim()
        val parsed = TaskParser.parse(taskCleanInput, existingCategories)

        val finalTitle = parsed.cleanTitle.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        if (finalTitle.isBlank()) {
            return VoiceAction.Unknown(rawText = transcript, suggestion = "Could not find task title")
        }

        val explanationParts = mutableListOf<String>()
        explanationParts.add("Add task '$finalTitle'")
        if (parsed.dueDate != null) {
            explanationParts.add("due ${com.personal.daymark.core.common.DateFormatter.formatRelativeDate(parsed.dueDate)}")
        }
        if (parsed.dueTimeMinutes != null) {
            explanationParts.add("at ${com.personal.daymark.core.common.DateFormatter.formatTime(parsed.dueTimeMinutes)}")
        }
        if (parsed.priority != null && parsed.priority > 0) {
            explanationParts.add("priority ${when (parsed.priority) { 4 -> "! Urgent"; 3 -> "▲ High"; 2 -> "■ Med"; else -> "▼ Low" }}")
        }

        return VoiceAction.CreateTask(
            title = finalTitle,
            dueDate = parsed.dueDate,
            dueTimeMinutes = parsed.dueTimeMinutes,
            priority = parsed.priority ?: 0,
            categoryId = parsed.matchedCategory?.id,
            explanation = explanationParts.joinToString(" • ")
        )
    }
}

