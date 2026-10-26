package com.personal.daymark.ui.screens.insights

import java.time.LocalDate

/**
 * Metric data point for daily bar and trend charts.
 */
data class DailyMetric(
    val dayLabel: String,
    val date: LocalDate,
    val value: Float,
    val displayValue: String,
    val isToday: Boolean = false
)

/**
 * Aggregated summary metrics for Daymark Insights.
 */
data class InsightsSummary(
    // Tasks
    val tasksCompletedToday: Int = 0,
    val tasksCompletedThisWeek: Int = 0,
    val totalOutstandingTasks: Int = 0,
    val totalOverdueTasks: Int = 0,
    val dailyTasksChart: List<DailyMetric> = emptyList(),

    // Focus
    val focusMinutesToday: Int = 0,
    val focusMinutesThisWeek: Int = 0,
    val focusMinutesThisMonth: Int = 0,
    val totalFocusSessions: Int = 0,
    val dailyFocusChart: List<DailyMetric> = emptyList(),

    // Habits
    val activeHabitsCount: Int = 0,
    val weeklyHabitCompletionRate: Int = 0,
    val bestStreakHabitName: String? = null,
    val bestStreakCount: Int = 0,

    // Overall emptiness check
    val hasAnyData: Boolean = false
)

/**
 * UI State for the Insights screen.
 */
sealed interface InsightsUiState {
    data object Loading : InsightsUiState
    data class Success(val summary: InsightsSummary) : InsightsUiState
}

/**
 * Day breakdown for the Weekly Review screen.
 */
data class WeeklyReviewDaySummary(
    val date: LocalDate,
    val dayName: String,
    val formattedDate: String,
    val tasksCompletedCount: Int,
    val focusMinutes: Int,
    val habitsCompletedCount: Int,
    val isToday: Boolean = false
)

/**
 * Aggregated metrics for the past 7 days Weekly Review.
 */
data class WeeklyReviewSummary(
    val weekRangeLabel: String,
    val totalTasksCompleted: Int,
    val totalFocusMinutes: Int,
    val dailyAverageFocusMinutes: Int,
    val habitCompletionRate: Int,
    val topFocusDay: String?,
    val dailyBreakdown: List<WeeklyReviewDaySummary>,
    val hasData: Boolean
)

/**
 * UI State for the Weekly Review screen.
 */
sealed interface WeeklyReviewUiState {
    data object Loading : WeeklyReviewUiState
    data class Success(val summary: WeeklyReviewSummary) : WeeklyReviewUiState
}
