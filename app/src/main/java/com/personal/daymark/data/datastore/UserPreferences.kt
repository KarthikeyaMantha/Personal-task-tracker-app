package com.personal.daymark.data.datastore

data class UserPreferences(
    val themePreference: String = THEME_SYSTEM,
    val defaultTaskPriority: Int = 0,
    val weekStartDay: Int = WEEK_START_MONDAY,
    val defaultFocusDurationMinutes: Int = 25,
    val shortBreakDurationMinutes: Int = 5,
    val longBreakDurationMinutes: Int = 15,
    val sessionsUntilLongBreak: Int = 4,
    val hapticFeedbackEnabled: Boolean = true,
    val dailySummaryEnabled: Boolean = true
) {
    companion object {
        const val THEME_SYSTEM = "SYSTEM"
        const val THEME_LIGHT = "LIGHT"
        const val THEME_DARK = "DARK"

        const val WEEK_START_MONDAY = 1
        const val WEEK_START_SUNDAY = 7
    }
}
