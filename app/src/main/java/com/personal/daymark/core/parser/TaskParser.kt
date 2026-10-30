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
    private val SPOKEN_PRIORITY_REGEX = Regex(
        """(?i)(?:^|\s)(?:priority\s+(urgent|high|medium|med|low)|(urgent|high\s+priority|important|low\s+priority))(?=\s|$)"""
    )

    private val TIME_12H_REGEX = Regex("""(?i)(?:^|\s)(?:at\s+)?(\d{1,2})(?::(\d{2}))?\s*(am|pm|a\.m\.|p\.m\.|a\.m|p\.m)(?=\s|$|[.,!?])""")
    private val TIME_24H_REGEX = Regex("""(?i)(?:^|\s)(?:at\s+)?([01]?\d|2[0-3]):([0-5]\d)(?=\s|$|[.,!?])""")
    private val TIME_AT_SIMPLE_REGEX = Regex("""(?i)(?:^|\s)at\s+(\d{1,2})(?=\s|$|[.,!?])""")

    private val TIME_OF_DAY_REGEX = Regex(
        """(?i)(?:^|\s)(in\s+the\s+morning|in\s+the\s+afternoon|this\s+afternoon|in\s+the\s+evening|this\s+evening|tonight|at\s+night)(?=\s|$)"""
    )

    private val DATE_RELATIVE_REGEX = Regex(
        """(?i)(?:^|\s)(today|tomorrow|day\s+after\s+tomorrow|next\s+week)(?=\s|$)"""
    )
    private val WEEKDAY_REGEX = Regex(
        """(?i)(?:^|\s)(?:on\s+)?(monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tue|wed|thu|fri|sat|sun)(?=\s|$)"""
    )

    /**
     * Parses input string using offline rules to extract categories, priorities,
     * due dates, and times, returning the extracted metadata and the cleaned task title.
     * Normalized for natural typed and spoken speech recognition inputs.
     */
    fun parse(input: String, existingCategories: List<CategoryEntity> = emptyList()): ParsedTaskResult {
        if (input.isBlank()) {
            return ParsedTaskResult(cleanTitle = "")
        }

        // Normalize voice recognition abbreviations (e.g. "p.m." -> "pm", "a.m." -> "am", "o'clock" -> "")
        var normalized = input
            .replace(Regex("""(?i)\bp\.m\.?(?=\s|$|[.,!?])"""), "pm")
            .replace(Regex("""(?i)\ba\.m\.?(?=\s|$|[.,!?])"""), "am")
            .replace(Regex("""(?i)\b(\d{1,2})\s*p\.m\.?(?=\s|$|[.,!?])"""), "$1 pm")
            .replace(Regex("""(?i)\b(\d{1,2})\s*a\.m\.?(?=\s|$|[.,!?])"""), "$1 am")
            .replace(Regex("""(?i)\b(\d{1,2})\s*pm\b"""), "$1 pm")
            .replace(Regex("""(?i)\b(\d{1,2})\s*am\b"""), "$1 am")
            .replace(Regex("""(?i)\bo'clock\b"""), "")
            .trim()

        var workingTitle = normalized
        val tokensToRemove = mutableListOf<String>()
        val detectedLabels = mutableListOf<String>()

        // 1. Extract Category (#work, #personal)
        var matchedCategory: CategoryEntity? = null
        var detectedCategoryName: String? = null
        val catMatch = CATEGORY_REGEX.find(normalized)
        if (catMatch != null) {
            val tag = catMatch.groupValues[1]
            val fullMatch = catMatch.value.trim()
            tokensToRemove.add(fullMatch)

            matchedCategory = existingCategories.firstOrNull { it.name.equals(tag, ignoreCase = true) }
            detectedCategoryName = matchedCategory?.name ?: tag
            detectedLabels.add("#$detectedCategoryName")
        }

        // 2. Extract Priority (!high, !urgent, or spoken "priority high", "urgent", "important")
        var priority: Int? = null
        val prioMatch = PRIORITY_REGEX.find(normalized)
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
        } else {
            val spokenPrioMatch = SPOKEN_PRIORITY_REGEX.find(normalized)
            if (spokenPrioMatch != null) {
                val fullMatch = spokenPrioMatch.value.trim()
                tokensToRemove.add(fullMatch)

                val tag1 = spokenPrioMatch.groupValues[1].lowercase(Locale.ROOT)
                val tag2 = spokenPrioMatch.groupValues[2].lowercase(Locale.ROOT)
                val combined = tag1.ifBlank { tag2 }

                priority = when {
                    combined.contains("urgent") || combined.contains("important") -> 4
                    combined.contains("high") -> 3
                    combined.contains("medium") || combined.contains("med") -> 2
                    combined.contains("low") -> 1
                    else -> null
                }
            }
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

        // 3. Extract Time (at 6pm, 5:30pm, 17:00, or "at 5", or "in the morning", "this evening")
        var dueTimeMinutes: Int? = null
        val time12Match = TIME_12H_REGEX.find(normalized)
        if (time12Match != null) {
            val fullMatch = time12Match.value.trim()
            tokensToRemove.add(fullMatch)

            val rawHour = time12Match.groupValues[1].toIntOrNull() ?: 0
            val rawMinute = time12Match.groupValues[2].takeIf { it.isNotBlank() }?.toIntOrNull() ?: 0
            val rawAmPm = time12Match.groupValues[3].lowercase(Locale.ROOT)
            val isPm = rawAmPm.contains("p")

            var hour24 = rawHour % 12
            if (isPm) {
                hour24 += 12
            }
            dueTimeMinutes = hour24 * 60 + rawMinute
            val standardAmPm = if (isPm) "PM" else "AM"
            detectedLabels.add(String.format(Locale.US, "%02d:%02d %s", if (rawHour == 0) 12 else rawHour, rawMinute, standardAmPm))
        } else {
            val time24Match = TIME_24H_REGEX.find(normalized)
            if (time24Match != null) {
                val fullMatch = time24Match.value.trim()
                tokensToRemove.add(fullMatch)

                val hour = time24Match.groupValues[1].toIntOrNull() ?: 0
                val minute = time24Match.groupValues[2].toIntOrNull() ?: 0
                dueTimeMinutes = hour * 60 + minute
                detectedLabels.add(String.format(Locale.US, "%02d:%02d", hour, minute))
            } else {
                val timeAtMatch = TIME_AT_SIMPLE_REGEX.find(normalized)
                if (timeAtMatch != null) {
                    val fullMatch = timeAtMatch.value.trim()
                    tokensToRemove.add(fullMatch)

                    val rawHour = timeAtMatch.groupValues[1].toIntOrNull() ?: 9
                    // Smart convention: 1..6 implies PM (13:00 - 18:00); 7..11 implies AM (07:00 - 11:00); 12 implies PM (noon)
                    val hour24 = when (rawHour) {
                        in 1..6 -> rawHour + 12
                        in 7..11 -> rawHour
                        12 -> 12
                        else -> rawHour
                    }
                    dueTimeMinutes = hour24 * 60
                    detectedLabels.add(DateFormatter.formatTime(dueTimeMinutes))
                }
            }
        }

        // 4. Extract Date & Time-of-day phrases
        var dueDateMillis: Long? = null
        val dateRelMatch = DATE_RELATIVE_REGEX.find(normalized)
        if (dateRelMatch != null) {
            val fullMatch = dateRelMatch.value.trim()
            tokensToRemove.add(fullMatch)

            val rel = dateRelMatch.groupValues[1].lowercase(Locale.ROOT)
            val startOfToday = DateFormatter.getStartOfTodayMillis()
            dueDateMillis = when (rel) {
                "today" -> startOfToday
                "tomorrow" -> startOfToday + 86400000L
                "day after tomorrow" -> startOfToday + 2 * 86400000L
                "next week" -> startOfToday + 7 * 86400000L
                else -> null
            }
            if (dueDateMillis != null) {
                detectedLabels.add(rel.replaceFirstChar { it.uppercase() })
            }
        } else {
            val weekdayMatch = WEEKDAY_REGEX.find(normalized)
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

        // 5. Time-of-day contextual phrases ("tonight", "this evening", "in the morning")
        val timeOfDayMatch = TIME_OF_DAY_REGEX.find(normalized)
        if (timeOfDayMatch != null) {
            val fullMatch = timeOfDayMatch.value.trim()
            tokensToRemove.add(fullMatch)

            val phrase = timeOfDayMatch.groupValues[1].lowercase(Locale.ROOT)
            if (dueTimeMinutes == null) {
                dueTimeMinutes = when {
                    phrase.contains("morning") -> 540 // 9:00 AM
                    phrase.contains("afternoon") -> 780 // 1:00 PM
                    phrase.contains("evening") -> 1080 // 6:00 PM
                    phrase.contains("tonight") || phrase.contains("night") -> 1200 // 8:00 PM
                    else -> null
                }
            }
            if (dueDateMillis == null) {
                dueDateMillis = DateFormatter.getStartOfTodayMillis()
            }
        }

        // If time was specified but no date was explicitly set, default to today
        if (dueTimeMinutes != null && dueDateMillis == null) {
            dueDateMillis = DateFormatter.getStartOfTodayMillis()
        }

        // Clean tokens from the working title
        for (token in tokensToRemove) {
            val tokenRegex = Regex("""(?i)(?:^|\s)""" + Regex.escape(token) + """(?=\s|$|[.,!?])""")
            workingTitle = tokenRegex.replace(workingTitle, " ")
        }

        // Clean any leftover speech-to-text time/preposition remnants
        workingTitle = workingTitle
            .replace(Regex("""(?i)\b(at|pm|am|p\.m\.|a\.m\.)\s*$"""), "")
            .replace(Regex("""(?i)^\s*(at|pm|am)\b"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()

        val finalTitle = workingTitle.ifBlank { input.trim() }

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
