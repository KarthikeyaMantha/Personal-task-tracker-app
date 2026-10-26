package com.personal.daymark.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.personal.daymark.core.habits.HabitCalculationHelper
import com.personal.daymark.data.repository.FocusRepository
import com.personal.daymark.data.repository.HabitRepository
import com.personal.daymark.data.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class InsightsViewModel(
    private val taskRepository: TaskRepository,
    private val focusRepository: FocusRepository,
    private val habitRepository: HabitRepository
) : ViewModel() {

    val uiState: StateFlow<InsightsUiState> = buildInsightsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = InsightsUiState.Loading
        )

    private fun buildInsightsFlow() = combine(
        buildTasksMetricsFlow(),
        buildFocusMetricsFlow(),
        buildHabitMetricsFlow()
    ) { tasksMetrics, focusMetrics, habitMetrics ->
        val hasAnyData = tasksMetrics.hasData || focusMetrics.hasData || habitMetrics.hasData

        val summary = InsightsSummary(
            tasksCompletedToday = tasksMetrics.completedToday,
            tasksCompletedThisWeek = tasksMetrics.completedThisWeek,
            totalOutstandingTasks = tasksMetrics.outstanding,
            totalOverdueTasks = tasksMetrics.overdue,
            dailyTasksChart = tasksMetrics.dailyChart,

            focusMinutesToday = focusMetrics.minutesToday,
            focusMinutesThisWeek = focusMetrics.minutesThisWeek,
            focusMinutesThisMonth = focusMetrics.minutesThisMonth,
            totalFocusSessions = focusMetrics.totalSessions,
            dailyFocusChart = focusMetrics.dailyChart,

            activeHabitsCount = habitMetrics.activeCount,
            weeklyHabitCompletionRate = habitMetrics.weeklyCompletionRate,
            bestStreakHabitName = habitMetrics.bestHabitName,
            bestStreakCount = habitMetrics.bestStreak,

            hasAnyData = hasAnyData
        )
        InsightsUiState.Success(summary)
    }

    private data class TasksMetrics(
        val completedToday: Int,
        val completedThisWeek: Int,
        val outstanding: Int,
        val overdue: Int,
        val dailyChart: List<DailyMetric>,
        val hasData: Boolean
    )

    private fun buildTasksMetricsFlow() = combine(
        taskRepository.getCompletedTasksInRange(getStartOfPastNDays(7), getEndOfToday()),
        taskRepository.getPendingTasksCount(),
        taskRepository.getOverdueTasksCount(System.currentTimeMillis()),
        taskRepository.getTotalTasksCount()
    ) { pastCompletedTasks, outstanding, overdue, totalTasks ->
        val today = LocalDate.now()
        val startOfToday = getStartOfToday()
        val endOfToday = getEndOfToday()
        val startOfWeek = getStartOfWeek()
        val now = System.currentTimeMillis()

        val completedToday = pastCompletedTasks.count { task ->
            val time = task.completionTimestamp ?: task.updatedAt
            time in startOfToday..endOfToday
        }

        val completedThisWeek = pastCompletedTasks.count { task ->
            val time = task.completionTimestamp ?: task.updatedAt
            time in startOfWeek..now
        }

        val dailyChart = (6 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val startMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = date.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val count = pastCompletedTasks.count { task ->
                val time = task.completionTimestamp ?: task.updatedAt
                time in startMillis..endMillis
            }
            DailyMetric(
                dayLabel = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                date = date,
                value = count.toFloat(),
                displayValue = count.toString(),
                isToday = daysAgo == 0
            )
        }

        TasksMetrics(
            completedToday = completedToday,
            completedThisWeek = completedThisWeek,
            outstanding = outstanding,
            overdue = overdue,
            dailyChart = dailyChart,
            hasData = totalTasks > 0 || completedThisWeek > 0
        )
    }

    private data class FocusMetrics(
        val minutesToday: Int,
        val minutesThisWeek: Int,
        val minutesThisMonth: Int,
        val totalSessions: Int,
        val dailyChart: List<DailyMetric>,
        val hasData: Boolean
    )

    private fun buildFocusMetricsFlow() = combine(
        focusRepository.getFocusSessionsInRange(getStartOfPastNDays(31), getEndOfToday()),
        focusRepository.getTotalSessionsCount("FOCUS")
    ) { pastSessions, totalSessions ->
        val today = LocalDate.now()
        val startOfToday = getStartOfToday()
        val endOfToday = getEndOfToday()
        val startOfWeek = getStartOfWeek()
        val startOfMonth = getStartOfMonth()
        val now = System.currentTimeMillis()

        val focusSessions = pastSessions.filter { it.sessionType == "FOCUS" }

        val minToday = focusSessions.filter { it.startedAt in startOfToday..endOfToday }
            .sumOf { it.durationMinutes }

        val minWeek = focusSessions.filter { it.startedAt in startOfWeek..now }
            .sumOf { it.durationMinutes }

        val minMonth = focusSessions.filter { it.startedAt in startOfMonth..now }
            .sumOf { it.durationMinutes }

        val dailyChart = (6 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val startMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = date.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val minutes = focusSessions.filter { it.startedAt in startMillis..endMillis }
                .sumOf { it.durationMinutes }

            val display = if (minutes >= 60) {
                val h = minutes / 60
                val m = minutes % 60
                if (m > 0) "${h}h ${m}m" else "${h}h"
            } else if (minutes > 0) {
                "${minutes}m"
            } else {
                "0m"
            }

            DailyMetric(
                dayLabel = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                date = date,
                value = minutes.toFloat(),
                displayValue = display,
                isToday = daysAgo == 0
            )
        }

        FocusMetrics(
            minutesToday = minToday,
            minutesThisWeek = minWeek,
            minutesThisMonth = minMonth,
            totalSessions = totalSessions,
            dailyChart = dailyChart,
            hasData = totalSessions > 0 || minWeek > 0
        )
    }

    private data class HabitMetrics(
        val activeCount: Int,
        val weeklyCompletionRate: Int,
        val bestHabitName: String?,
        val bestStreak: Int,
        val hasData: Boolean
    )

    private fun buildHabitMetricsFlow() = combine(
        habitRepository.getActiveHabitsCount(),
        habitRepository.getActiveHabitsWithLogs()
    ) { activeCount, activeWithLogs ->
        val today = LocalDate.now()
        val weeklyRate = HabitCalculationHelper.calculateWeeklyHabitCompletionRate(activeWithLogs, today)

        var bestHabit: String? = null
        var maxStreak = 0

        for (hwl in activeWithLogs) {
            val streak = HabitCalculationHelper.calculateCurrentStreak(hwl.habit, hwl.logs, today)
            if (streak > maxStreak) {
                maxStreak = streak
                bestHabit = hwl.habit.name
            }
        }

        HabitMetrics(
            activeCount = activeCount,
            weeklyCompletionRate = weeklyRate,
            bestHabitName = bestHabit,
            bestStreak = maxStreak,
            hasData = activeCount > 0 && activeWithLogs.any { it.logs.isNotEmpty() }
        )
    }

    companion object {
        fun getStartOfToday(): Long {
            return LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        fun getEndOfToday(): Long {
            return LocalDate.now().atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        fun getStartOfWeek(): Long {
            return LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        fun getStartOfMonth(): Long {
            return LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        fun getStartOfPastNDays(days: Int): Long {
            return LocalDate.now().minusDays(days.toLong() - 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        fun provideFactory(
            taskRepository: TaskRepository,
            focusRepository: FocusRepository,
            habitRepository: HabitRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return InsightsViewModel(
                    taskRepository = taskRepository,
                    focusRepository = focusRepository,
                    habitRepository = habitRepository
                ) as T
            }
        }
    }
}
