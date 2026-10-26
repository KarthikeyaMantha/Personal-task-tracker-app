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
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class WeeklyReviewViewModel(
    private val taskRepository: TaskRepository,
    private val focusRepository: FocusRepository,
    private val habitRepository: HabitRepository
) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

    val uiState: StateFlow<WeeklyReviewUiState> = buildReviewFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WeeklyReviewUiState.Loading
        )

    private fun buildReviewFlow() = combine(
        taskRepository.getCompletedTasksInRange(getStartOfPast7Days(), getEndOfToday()),
        focusRepository.getFocusSessionsInRange(getStartOfPast7Days(), getEndOfToday()),
        habitRepository.getActiveHabitsWithLogs()
    ) { completedTasks, focusSessions, habitsWithLogs ->
        val today = LocalDate.now()
        val startDate = today.minusDays(6)

        val rangeLabel = "${startDate.format(dateFormatter)} – ${today.format(dateFormatter)}"

        var totalTasks = 0
        var totalFocusMin = 0
        var maxFocusDayName: String? = null
        var maxFocusMinutes = 0

        val daysBreakdown = (6 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val startMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = date.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val dayTasks = completedTasks.count { task ->
                val time = task.completionTimestamp ?: task.updatedAt
                time in startMillis..endMillis
            }

            val dayFocus = focusSessions.filter { it.sessionType == "FOCUS" && it.startedAt in startMillis..endMillis }
                .sumOf { it.durationMinutes }

            val dayHabits = habitsWithLogs.count { hwl ->
                HabitCalculationHelper.isCompletedOnDate(hwl.logs, date)
            }

            totalTasks += dayTasks
            totalFocusMin += dayFocus

            if (dayFocus > maxFocusMinutes) {
                maxFocusMinutes = dayFocus
                val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
                val timeFormatted = if (dayFocus >= 60) "${dayFocus / 60}h ${dayFocus % 60}m" else "${dayFocus}m"
                maxFocusDayName = "$dayOfWeek ($timeFormatted)"
            }

            WeeklyReviewDaySummary(
                date = date,
                dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                formattedDate = date.format(dateFormatter),
                tasksCompletedCount = dayTasks,
                focusMinutes = dayFocus,
                habitsCompletedCount = dayHabits,
                isToday = daysAgo == 0
            )
        }

        val dailyAvgFocus = totalFocusMin / 7
        val habitRate = HabitCalculationHelper.calculateWeeklyHabitCompletionRate(habitsWithLogs, today)
        val hasData = totalTasks > 0 || totalFocusMin > 0 || habitsWithLogs.any { it.logs.isNotEmpty() }

        val summary = WeeklyReviewSummary(
            weekRangeLabel = rangeLabel,
            totalTasksCompleted = totalTasks,
            totalFocusMinutes = totalFocusMin,
            dailyAverageFocusMinutes = dailyAvgFocus,
            habitCompletionRate = habitRate,
            topFocusDay = maxFocusDayName,
            dailyBreakdown = daysBreakdown,
            hasData = hasData
        )

        WeeklyReviewUiState.Success(summary)
    }

    companion object {
        fun getStartOfPast7Days(): Long {
            return LocalDate.now().minusDays(6).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        fun getEndOfToday(): Long {
            return LocalDate.now().atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        fun provideFactory(
            taskRepository: TaskRepository,
            focusRepository: FocusRepository,
            habitRepository: HabitRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return WeeklyReviewViewModel(
                    taskRepository = taskRepository,
                    focusRepository = focusRepository,
                    habitRepository = habitRepository
                ) as T
            }
        }
    }
}
