package com.personal.daymark.core.recurrence

import org.json.JSONArray
import org.json.JSONObject

enum class RecurrenceFrequency {
    DAILY,
    WEEKDAYS,       // Mon - Fri
    WEEKLY,         // Specific weekdays or same day of week
    MONTHLY,        // Same day of month
    YEARLY
}

data class RecurrenceRule(
    val frequency: RecurrenceFrequency,
    val interval: Int = 1,                   // e.g. every 1 day, every 2 weeks
    val daysOfWeek: List<Int> = emptyList(), // 1 = Monday .. 7 = Sunday (java.time.DayOfWeek.value)
    val endAfterOccurrences: Int? = null,
    val endDateMillis: Long? = null
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("frequency", frequency.name)
        json.put("interval", interval)
        val daysArray = JSONArray()
        daysOfWeek.forEach { daysArray.put(it) }
        json.put("daysOfWeek", daysArray)
        if (endAfterOccurrences != null) json.put("endAfterOccurrences", endAfterOccurrences)
        if (endDateMillis != null) json.put("endDateMillis", endDateMillis)
        return json.toString()
    }

    fun toDisplayString(): String {
        return when (frequency) {
            RecurrenceFrequency.DAILY -> if (interval <= 1) "Daily" else "Every $interval days"
            RecurrenceFrequency.WEEKDAYS -> "Every weekday (Mon–Fri)"
            RecurrenceFrequency.WEEKLY -> {
                if (daysOfWeek.isEmpty()) {
                    if (interval <= 1) "Weekly" else "Every $interval weeks"
                } else {
                    val names = daysOfWeek.sorted().map { dayValueToAbbreviation(it) }.joinToString(", ")
                    if (interval <= 1) "Weekly on $names" else "Every $interval weeks on $names"
                }
            }
            RecurrenceFrequency.MONTHLY -> if (interval <= 1) "Monthly" else "Every $interval months"
            RecurrenceFrequency.YEARLY -> if (interval <= 1) "Yearly" else "Every $interval years"
        }
    }

    companion object {
        fun dayValueToAbbreviation(day: Int): String {
            return when (day) {
                1 -> "Mon"
                2 -> "Tue"
                3 -> "Wed"
                4 -> "Thu"
                5 -> "Fri"
                6 -> "Sat"
                7 -> "Sun"
                else -> ""
            }
        }

        fun fromJson(jsonStr: String?): RecurrenceRule? {
            if (jsonStr.isNullOrBlank()) return null
            return try {
                val json = JSONObject(jsonStr)
                val freqName = json.getString("frequency")
                val frequency = RecurrenceFrequency.valueOf(freqName)
                val interval = json.optInt("interval", 1).coerceAtLeast(1)
                val daysArray = json.optJSONArray("daysOfWeek")
                val daysList = mutableListOf<Int>()
                if (daysArray != null) {
                    for (i in 0 until daysArray.length()) {
                        daysList.add(daysArray.getInt(i))
                    }
                }
                val endAfter = if (json.has("endAfterOccurrences")) json.getInt("endAfterOccurrences") else null
                val endDate = if (json.has("endDateMillis")) json.getLong("endDateMillis") else null

                RecurrenceRule(
                    frequency = frequency,
                    interval = interval,
                    daysOfWeek = daysList,
                    endAfterOccurrences = endAfter,
                    endDateMillis = endDate
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
