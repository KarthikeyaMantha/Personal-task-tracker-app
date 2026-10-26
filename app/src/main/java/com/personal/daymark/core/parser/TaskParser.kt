package com.personal.daymark.core.parser

import com.personal.daymark.core.common.DateFormatter
import com.personal.daymark.data.local.entity.CategoryEntity
import java.util.Calendar
import java.util.Locale

data class ParsedTaskResult(
    val cleanTitle: String,
    val matchedCategory: CategoryEntity? = null,
    val detectedCategoryName: String? = null,
    val priority: Int? = null,
    val dueDate: Long? = null,
    val dueTimeMinutes: Int? = null,
    val detectedTokens: List<String> = emptyList()
) {
    val hasDetections: Boolean
        get() = matchedCategory != null || detectedCategoryName != null || priority != null || dueDate != null || dueTimeMinutes != null

    fun getExplanationChips(): List<String> {
        val list = mutableListOf<String>()
        if (detectedCategoryName != null) {
            list.add("Will set category to #$detectedCategoryName")
        }
        if (priority != null) {
            val prioName = when (priority) {
                4 -> "Urgent"
                3 -> "High"
                2 -> "Medium"
                else -> "Low"
            }
            list.add("Will set priority to $prioName")
        }
        if (dueDate != null) {
            val dateStr = DateFormatter.formatRelativeDate(dueDate)
            list.add("Will set due date to $dateStr")
        }
        if (dueTimeMinutes != null) {
            val timeStr = DateFormatter.formatTime(dueTimeMinutes)
            list.add("Will set reminder at $timeStr")
        }
        return list
    }
}

object TaskParser {

    private val CATEGORY_REGEX = Regex("""(?i)(?:^|\s)#([a-zA-Z0-9_\-]+)""")
    private val PRIORITY_REGEX = Regex("""(?i)(?:^|\s)!(urgent|high|med|medium|low|p1|p2|p3|p4)(?=\s|$)""")
    private val TIME_12H_REGEX = Regex("""(?i)(?:^|\s)(?:at\s+)?(\d{1,2})(?::(\d{2}))?\s*(am|pm)(?=\s|$)""")
    private val TIME_24H_REGEX = Regex("""(?i)(?:^|\s)(?:at\s+)?([01]?\d|2[0-3]):([0-5]\d)(?=\s|$)""")
    private val DATE_RELATIVE_REGEX = Regex("""(?i)(?:^|\s)(today|tomorrow|next\s+week)(?=\s|$)""")
    private val WEEKDAY_REGEX = Regex("""(?i)(?:^|\s)(?:on\s+)?(monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tue|wed|thu|fri|sat|sun)(?=\s|$)""")

    /**
     * Parses input string using offline regex rules to extract categories, priorities,
     * due dates, and times, returning the extracted metadata and the cleaned task title.
     */
    fun parse(input: String, existingCategories: List<CategoryEntity> = emptyList()): ParsedTaskResult {
        if (input.isBlank()) {
            return ParsedTaskResult(cleanTitle = "")
        }

        var workingTitle = input
        val tokensToRemove = mutableListOf<String>()
        val detectedLabels = mutableListOf<String>()

        // 1. Extract Category (#work, #personal)
        var matchedCategory: CategoryEntity? = null
        var detectedCategoryName: String? = null
        val catMatch = CATEGORY_REGEX.find(input)
        if (catMatch != null) {
            val tag = catMatch.groupValues[1]
            val fullMatch = catMatch.value.trim()
            tokensToRemove.add(fullMatch)

            matchedCategory = existingCategories.firstOrNull { it.name.equals(tag, ignoreCase = true) }
            detectedCategoryName = matchedCategory?.name ?: tag
            detectedLabels.add("#$detectedCategoryName")
        }

        // 2. Extract Priority (!high, !medium, !low, !urgent)
        var priority: Int? = null
        val prioMatch = PRIORITY_REGEX.find(input)
        if (prioMatch != null) {
            val prioTag = prioMatch.groupValues[1].lowercase(Locale.ROOT)
            val fullMatch = prioMatch.value.trim()
            tokensToRemove.add(fullMatch)

            priority = when (prioTag) {
                "urgent", "p4" -> 4
                "high", "p3" -> 3
                "medium", "med", "p2" -> 2
                "low", "p1" -> 1
                else -> null
            }
            if (priority != null) {
                val label = when (priority) {
                    4 -> "!urgent"
                    3 -> "!high"
                    2 -> "!medium"
                    else -> "!low"
                }
                detectedLabels.add(label)
            }
        }

        // 3. Extract Time (at 6pm, 5:30pm, 17:00)
        var dueTimeMinutes: Int? = null
        val time12Match = TIME_12H_REGEX.find(input)
        if (time12Match != null) {
            val fullMatch = time12Match.value.trim()
            tokensToRemove.add(fullMatch)

            val rawHour = time12Match.groupValues[1].toIntOrNull() ?: 0
            val rawMinute = time12Match.groupValues[2].takeIf { it.isNotBlank() }?.toIntOrNull() ?: 0
            val amPm = time12Match.groupValues[3].lowercase(Locale.ROOT)

            var hour24 = rawHour % 12
            if (amPm == "pm") {
                hour24 += 12
            }
            dueTimeMinutes = hour24 * 60 + rawMinute
            detectedLabels.add(String.format(Locale.US, "%02d:%02d %s", if (rawHour == 0) 12 else rawHour, rawMinute, amPm.uppercase(Locale.US)))
        } else {
            val time24Match = TIME_24H_REGEX.find(input)
            if (time24Match != null) {
                val fullMatch = time24Match.value.trim()
                tokensToRemove.add(fullMatch)

                val hour = time24Match.groupValues[1].toIntOrNull() ?: 0
                val minute = time24Match.groupValues[2].toIntOrNull() ?: 0
                dueTimeMinutes = hour * 60 + minute
                detectedLabels.add(String.format(Locale.US, "%02d:%02d", hour, minute))
            }
        }

        // 4. Extract Date (today, tomorrow, next week, monday..sunday)
        var dueDateMillis: Long? = null
        val dateRelMatch = DATE_RELATIVE_REGEX.find(input)
        if (dateRelMatch != null) {
            val fullMatch = dateRelMatch.value.trim()
            tokensToRemove.add(fullMatch)

            val rel = dateRelMatch.groupValues[1].lowercase(Locale.ROOT)
            val startOfToday = DateFormatter.getStartOfTodayMillis()
            dueDateMillis = when (rel) {
                "today" -> startOfToday
                "tomorrow" -> startOfToday + 86400000L
                "next week" -> startOfToday + 7 * 86400000L
                else -> null
            }
            if (dueDateMillis != null) {
                detectedLabels.add(rel.replaceFirstChar { it.uppercase() })
            }
        } else {
            val weekdayMatch = WEEKDAY_REGEX.find(input)
            if (weekdayMatch != null) {
                val fullMatch = weekdayMatch.value.trim()
                tokensToRemove.add(fullMatch)

                val weekdayStr = weekdayMatch.groupValues[1].lowercase(Locale.ROOT)
                val targetDayOfWeek = parseWeekday(weekdayStr)
                if (targetDayOfWeek != null) {
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val currentDay = cal.get(Calendar.DAY_OF_WEEK)
                    var daysAhead = (targetDayOfWeek - currentDay + 7) % 7
                    if (daysAhead == 0) {
                        daysAhead = 7 // Default to the next upcoming occurrence
                    }
                    cal.add(Calendar.DAY_OF_YEAR, daysAhead)
                    dueDateMillis = cal.timeInMillis
                    detectedLabels.add(weekdayStr.replaceFirstChar { it.uppercase() })
                }
            }
        }

        // If time was specified but no date was explicitly set, default to today
        if (dueTimeMinutes != null && dueDateMillis == null) {
            dueDateMillis = DateFormatter.getStartOfTodayMillis()
        }

        // Clean tokens from the working title
        for (token in tokensToRemove) {
            val tokenRegex = Regex("""(?i)(?:^|\s)""" + Regex.escape(token) + """(?=\s|$)""")
            workingTitle = tokenRegex.replace(workingTitle, " ")
        }

        val cleaned = workingTitle.replace(Regex("""\s+"""), " ").trim()
        val finalTitle = cleaned.ifBlank { input.trim() }

        return ParsedTaskResult(
            cleanTitle = finalTitle,
            matchedCategory = matchedCategory,
            detectedCategoryName = detectedCategoryName,
            priority = priority,
            dueDate = dueDateMillis,
            dueTimeMinutes = dueTimeMinutes,
            detectedTokens = detectedLabels
        )
    }

    private fun parseWeekday(day: String): Int? {
        return when (day) {
            "monday", "mon" -> Calendar.MONDAY
            "tuesday", "tue" -> Calendar.TUESDAY
            "wednesday", "wed" -> Calendar.WEDNESDAY
            "thursday", "thu" -> Calendar.THURSDAY
            "friday", "fri" -> Calendar.FRIDAY
            "saturday", "sat" -> Calendar.SATURDAY
            "sunday", "sun" -> Calendar.SUNDAY
            else -> null
        }
    }
}
