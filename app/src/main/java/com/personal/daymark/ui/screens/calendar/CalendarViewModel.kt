package com.personal.daymark.ui.screens.calendar

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.personal.daymark.core.notifications.ReminderScheduler
import com.personal.daymark.data.local.entity.TaskWithSubtasksAndCategory
import com.personal.daymark.data.local.entity.parsedColor
import com.personal.daymark.data.repository.CategoryRepository
import com.personal.daymark.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.personal.daymark.data.datastore.UserPreferences
import com.personal.daymark.data.repository.SettingsRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class CalendarViewMode(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly")
}

data class CalendarDay(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
    val taskDots: List<Color> = emptyList(),
    val taskCount: Int = 0
)

data class CalendarUiState(
    val viewMode: CalendarViewMode = CalendarViewMode.MONTHLY,
    val selectedDate: LocalDate = LocalDate.now(),
    val displayedMonth: YearMonth = YearMonth.now(),
    val tasksForSelectedDate: List<TaskWithSubtasksAndCategory> = emptyList(),
    val overdueTasks: List<TaskWithSubtasksAndCategory> = emptyList(),
    val monthGridDays: List<CalendarDay> = emptyList(),
    val weekDays: List<CalendarDay> = emptyList(),
    val isQuickAddOpen: Boolean = false,
    val quickAddDateMillis: Long? = null,
    val weekStartDay: Int = UserPreferences.WEEK_START_MONDAY
)

private data class CalendarControls(
    val viewMode: CalendarViewMode,
    val selectedDate: LocalDate,
    val displayedMonth: YearMonth,
    val isQuickAddOpen: Boolean,
    val weekStartDay: Int
)

class CalendarViewModel(
    private val taskRepository: TaskRepository,
    private val reminderScheduler: ReminderScheduler? = null,
    private val settingsRepository: SettingsRepository? = null
) : ViewModel() {

    private val _viewMode = MutableStateFlow(CalendarViewMode.MONTHLY)
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _displayedMonth = MutableStateFlow(YearMonth.now())
    private val _isQuickAddOpen = MutableStateFlow(false)

    private val weekStartFlow = settingsRepository?.userPreferencesFlow?.map { it.weekStartDay }
        ?: flowOf(UserPreferences.WEEK_START_MONDAY)

    private val calendarControlsFlow = combine(
        _viewMode,
        _selectedDate,
        _displayedMonth,
        _isQuickAddOpen,
        weekStartFlow
    ) { viewMode, selectedDate, displayedMonth, isQuickAddOpen, weekStartDay ->
        CalendarControls(viewMode, selectedDate, displayedMonth, isQuickAddOpen, weekStartDay)
    }

    val uiState: StateFlow<CalendarUiState> = combine(
        taskRepository.getAllTasksWithDetails(),
        calendarControlsFlow
    ) { allTasks, controls ->
        val viewMode = controls.viewMode
        val selectedDate = controls.selectedDate
        val displayedMonth = controls.displayedMonth
        val isQuickAddOpen = controls.isQuickAddOpen
        val weekStartDay = controls.weekStartDay

        val today = LocalDate.now()
        val zoneId = ZoneId.systemDefault()

        // Group tasks by LocalDate
        val tasksByDate = mutableMapOf<LocalDate, MutableList<TaskWithSubtasksAndCategory>>()
        val overdue = mutableListOf<TaskWithSubtasksAndCategory>()

        allTasks.forEach { taskItem ->
            val dueDate = taskItem.task.dueDate
            if (dueDate != null) {
                val localDate = Instant.ofEpochMilli(dueDate).atZone(zoneId).toLocalDate()
                tasksByDate.getOrPut(localDate) { mutableListOf() }.add(taskItem)

                if (!taskItem.task.isCompleted && localDate.isBefore(today)) {
                    overdue.add(taskItem)
                }
            }
        }

        // Sort tasks for selected date (uncompleted first, by time, then priority)
        val selectedDateTasks = tasksByDate[selectedDate]?.sortedWith(
            compareBy<TaskWithSubtasksAndCategory> { it.task.isCompleted }
                .thenBy { it.task.dueTime ?: Int.MAX_VALUE }
                .thenByDescending { it.task.priority }
        ) ?: emptyList()

        // Build month grid days
        val monthGrid = buildMonthGrid(displayedMonth, selectedDate, today, tasksByDate, weekStartDay)

        // Build 7-day week strip for selectedDate's current week
        val weekStrip = buildWeekStrip(selectedDate, today, tasksByDate, weekStartDay)

        val quickAddMillis = selectedDate.atStartOfDay(zoneId).toInstant().toEpochMilli()

        CalendarUiState(
            viewMode = viewMode,
            selectedDate = selectedDate,
            displayedMonth = displayedMonth,
            tasksForSelectedDate = selectedDateTasks,
            overdueTasks = overdue.sortedBy { it.task.dueDate },
            monthGridDays = monthGrid,
            weekDays = weekStrip,
            isQuickAddOpen = isQuickAddOpen,
            quickAddDateMillis = quickAddMillis,
            weekStartDay = weekStartDay
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CalendarUiState()
    )

    fun selectViewMode(mode: CalendarViewMode) {
        _viewMode.value = mode
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        val newMonth = YearMonth.from(date)
        if (newMonth != _displayedMonth.value) {
            _displayedMonth.value = newMonth
        }
    }

    fun navigatePrevious() {
        when (_viewMode.value) {
            CalendarViewMode.DAILY -> {
                val newDate = _selectedDate.value.minusDays(1)
                selectDate(newDate)
            }
            CalendarViewMode.WEEKLY -> {
                val newDate = _selectedDate.value.minusWeeks(1)
                selectDate(newDate)
            }
            CalendarViewMode.MONTHLY -> {
                _displayedMonth.value = _displayedMonth.value.minusMonths(1)
            }
        }
    }

    fun navigateNext() {
        when (_viewMode.value) {
            CalendarViewMode.DAILY -> {
                val newDate = _selectedDate.value.plusDays(1)
                selectDate(newDate)
            }
            CalendarViewMode.WEEKLY -> {
                val newDate = _selectedDate.value.plusWeeks(1)
                selectDate(newDate)
            }
            CalendarViewMode.MONTHLY -> {
                _displayedMonth.value = _displayedMonth.value.plusMonths(1)
            }
        }
    }

    fun jumpToToday() {
        val today = LocalDate.now()
        _selectedDate.value = today
        _displayedMonth.value = YearMonth.now()
    }

    fun openQuickAdd(date: LocalDate? = null) {
        if (date != null) {
            selectDate(date)
        }
        _isQuickAddOpen.value = true
    }

    fun closeQuickAdd() {
        _isQuickAddOpen.value = false
    }

    fun toggleTaskCompletion(taskItem: TaskWithSubtasksAndCategory) {
        viewModelScope.launch {
            val newStatus = !taskItem.task.isCompleted
            taskRepository.setTaskCompleted(taskItem.task.id, newStatus)
        }
    }

    fun deleteTask(taskItem: TaskWithSubtasksAndCategory) {
        viewModelScope.launch {
            reminderScheduler?.cancelReminder(taskItem.task.id)
            taskRepository.deleteTask(taskItem.task)
        }
    }

    companion object {
        fun provideFactory(
            taskRepository: TaskRepository,
            reminderScheduler: ReminderScheduler? = null,
            settingsRepository: SettingsRepository? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CalendarViewModel(taskRepository, reminderScheduler, settingsRepository) as T
            }
        }

        private fun buildMonthGrid(
            displayedMonth: YearMonth,
            selectedDate: LocalDate,
            today: LocalDate,
            tasksByDate: Map<LocalDate, List<TaskWithSubtasksAndCategory>>,
            weekStartDay: Int
        ): List<CalendarDay> {
            val days = mutableListOf<CalendarDay>()

            val firstOfMonth = displayedMonth.atDay(1)
            val dayOfWeekValue = firstOfMonth.dayOfWeek.value // 1 (Mon) to 7 (Sun)
            val leadDays = if (weekStartDay == UserPreferences.WEEK_START_SUNDAY) {
                dayOfWeekValue % 7 // Sunday -> 0, Monday -> 1, ..., Saturday -> 6
            } else {
                dayOfWeekValue - 1 // Monday -> 0, ..., Sunday -> 6
            }

            // Lead days from previous month
            val prevMonth = displayedMonth.minusMonths(1)
            val prevMonthLength = prevMonth.lengthOfMonth()
            for (i in leadDays downTo 1) {
                val date = prevMonth.atDay(prevMonthLength - i + 1)
                days.add(createCalendarDay(date, false, selectedDate, today, tasksByDate))
            }

            // Days of current month
            for (day in 1..displayedMonth.lengthOfMonth()) {
                val date = displayedMonth.atDay(day)
                days.add(createCalendarDay(date, true, selectedDate, today, tasksByDate))
            }

            // Trailing days from next month to complete 5 or 6 weeks (multiples of 7)
            val nextMonth = displayedMonth.plusMonths(1)
            var trailingDay = 1
            while (days.size % 7 != 0 || days.size < 35) {
                val date = nextMonth.atDay(trailingDay++)
                days.add(createCalendarDay(date, false, selectedDate, today, tasksByDate))
            }

            return days
        }

        private fun buildWeekStrip(
            selectedDate: LocalDate,
            today: LocalDate,
            tasksByDate: Map<LocalDate, List<TaskWithSubtasksAndCategory>>,
            weekStartDay: Int
        ): List<CalendarDay> {
            val startDayOffset = if (weekStartDay == UserPreferences.WEEK_START_SUNDAY) {
                selectedDate.dayOfWeek.value % 7
            } else {
                selectedDate.dayOfWeek.value - 1
            }
            val startOfWeek = selectedDate.minusDays(startDayOffset.toLong())
            return (0..6).map { i ->
                val date = startOfWeek.plusDays(i.toLong())
                createCalendarDay(date, true, selectedDate, today, tasksByDate)
            }
        }

        private fun createCalendarDay(
            date: LocalDate,
            isCurrentMonth: Boolean,
            selectedDate: LocalDate,
            today: LocalDate,
            tasksByDate: Map<LocalDate, List<TaskWithSubtasksAndCategory>>
        ): CalendarDay {
            val tasksOnDay = tasksByDate[date].orEmpty()
            val dots = tasksOnDay
                .mapNotNull { it.category?.parsedColor() }
                .distinct()
                .take(3)
                .ifEmpty {
                    if (tasksOnDay.isNotEmpty()) listOf(Color(0xFF40916C)) else emptyList()
                }

            return CalendarDay(
                date = date,
                isCurrentMonth = isCurrentMonth,
                isToday = date == today,
                isSelected = date == selectedDate,
                taskDots = dots,
                taskCount = tasksOnDay.size
            )
        }
    }
}
