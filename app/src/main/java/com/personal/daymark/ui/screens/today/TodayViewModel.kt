package com.personal.daymark.ui.screens.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.personal.daymark.core.common.DateFormatter
import com.personal.daymark.data.local.entity.TaskWithSubtasksAndCategory
import com.personal.daymark.data.repository.TaskRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodayUiState(
    val greeting: String = "",
    val formattedDate: String = "",
    val todayTasks: List<TaskWithSubtasksAndCategory> = emptyList(),
    val overdueTasks: List<TaskWithSubtasksAndCategory> = emptyList(),
    val completedTodayCount: Int = 0,
    val totalTodayAndOverdueCount: Int = 0,
    val progressRatio: Float = 0f,
    val upcomingRemindersCount: Int = 0,
    val isLoading: Boolean = false
)

sealed interface TodayEvent {
    data class ShowSnackbar(val message: String, val actionLabel: String? = null) : TodayEvent
}

class TodayViewModel(
    private val taskRepository: TaskRepository,
    private val reminderScheduler: com.personal.daymark.core.notifications.ReminderScheduler? = null
) : ViewModel() {

    private val _eventChannel = Channel<TodayEvent>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()

    private var recentlyDeletedTask: TaskWithSubtasksAndCategory? = null

    val uiState: StateFlow<TodayUiState> = combine(
        taskRepository.getAllTasksWithDetails(),
        com.personal.daymark.core.common.DaymarkDayManager.dayEpoch
    ) { allTasks, _ ->
            val now = System.currentTimeMillis()
            val startOfToday = DateFormatter.getStartOfTodayMillis()
            val endOfToday = DateFormatter.getEndOfTodayMillis()

            // Overdue: Not completed, has a due date, and due date is strictly before today
            val overdueTasks = allTasks.filter { item ->
                !item.task.isCompleted &&
                        item.task.dueDate != null &&
                        DateFormatter.isBeforeToday(item.task.dueDate)
            }.sortedBy { it.task.dueDate }

            // Today: Scheduled for today (either complete or incomplete)
            val todayTasks = allTasks.filter { item ->
                item.task.dueDate != null &&
                        (DateFormatter.isSameDay(item.task.dueDate, now) || item.task.dueDate in startOfToday..endOfToday)
            }.sortedWith(
                compareBy<TaskWithSubtasksAndCategory> { it.task.isCompleted }
                    .thenByDescending { it.task.priority }
                    .thenBy { it.task.dueTime ?: Int.MAX_VALUE }
            )

            // Completed today: any task completed today (regardless of original due date)
            val completedToday = allTasks.count { item ->
                item.task.isCompleted &&
                        item.task.completionTimestamp != null &&
                        DateFormatter.isSameDay(item.task.completionTimestamp, now)
            }

            // Total tasks requiring attention today:
            // incomplete today + overdue + tasks already completed today
            val incompleteToday = todayTasks.count { !it.task.isCompleted }
            val incompleteOverdue = overdueTasks.size
            val totalRelevant = incompleteToday + incompleteOverdue + completedToday

            val ratio = if (totalRelevant > 0) {
                completedToday.toFloat() / totalRelevant.toFloat()
            } else {
                0f
            }

            val upcomingReminders = todayTasks.count { item ->
                if (item.task.isCompleted || item.task.dueDate == null) false
                else {
                    val trigger = com.personal.daymark.core.notifications.AlarmManagerReminderScheduler.calculateTriggerTimeMillis(
                        item.task.dueDate,
                        item.task.dueTime
                    )
                    trigger > now
                }
            }

            TodayUiState(
                greeting = DateFormatter.getDynamicGreeting(),
                formattedDate = DateFormatter.getFormattedCurrentDate(),
                todayTasks = todayTasks,
                overdueTasks = overdueTasks,
                completedTodayCount = completedToday,
                totalTodayAndOverdueCount = totalRelevant,
                progressRatio = ratio,
                upcomingRemindersCount = upcomingReminders,
                isLoading = false
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TodayUiState(
                greeting = DateFormatter.getDynamicGreeting(),
                formattedDate = DateFormatter.getFormattedCurrentDate(),
                isLoading = true
            )
        )

    fun toggleTaskCompletion(taskItem: TaskWithSubtasksAndCategory) {
        viewModelScope.launch {
            if (taskItem.task.isCompleted) {
                // Re-open completed task
                taskRepository.setTaskCompleted(taskItem.task.id, false)
                if (taskItem.task.dueDate != null) {
                    reminderScheduler?.scheduleReminder(taskItem.task.copy(isCompleted = false))
                }
            } else {
                // Complete task and generate next occurrence if recurring
                val nextTask = taskRepository.completeTaskAndGenerateNext(taskItem.task.id)
                reminderScheduler?.cancelReminder(taskItem.task.id)
                if (nextTask != null && nextTask.dueDate != null) {
                    reminderScheduler?.scheduleReminder(nextTask)
                }
            }
        }
    }

    fun deleteTask(taskItem: TaskWithSubtasksAndCategory) {
        viewModelScope.launch {
            recentlyDeletedTask = taskItem
            taskRepository.deleteTask(taskItem.task)
            reminderScheduler?.cancelReminder(taskItem.task.id)
            _eventChannel.send(
                TodayEvent.ShowSnackbar(
                    message = "Task \"${taskItem.task.title}\" deleted",
                    actionLabel = "Undo"
                )
            )
        }
    }

    fun deleteTaskOccurrence(taskItem: TaskWithSubtasksAndCategory) {
        viewModelScope.launch {
            recentlyDeletedTask = taskItem
            reminderScheduler?.cancelReminder(taskItem.task.id)
            val nextTask = taskRepository.deleteTaskOccurrence(taskItem.task.id)
            if (nextTask != null && nextTask.dueDate != null) {
                reminderScheduler?.scheduleReminder(nextTask)
            }
            _eventChannel.send(
                TodayEvent.ShowSnackbar(
                    message = "Occurrence deleted",
                    actionLabel = null
                )
            )
        }
    }

    fun deleteTaskSeries(taskItem: TaskWithSubtasksAndCategory) {
        viewModelScope.launch {
            val seriesId = taskItem.task.seriesId
            if (seriesId != null) {
                val deletedIds = taskRepository.deleteTaskSeries(seriesId)
                deletedIds.forEach { reminderScheduler?.cancelReminder(it) }
            } else {
                taskRepository.deleteTask(taskItem.task)
                reminderScheduler?.cancelReminder(taskItem.task.id)
            }
            _eventChannel.send(
                TodayEvent.ShowSnackbar(
                    message = "Recurring series deleted",
                    actionLabel = null
                )
            )
        }
    }

    fun deferTask(
        taskItem: TaskWithSubtasksAndCategory,
        targetDueDate: Long,
        confirmationMessage: String = "Deferred to Tomorrow"
    ) {
        viewModelScope.launch {
            val dueTime = taskItem.task.dueTime ?: (9 * 60)
            val updatedTask = taskItem.task.copy(
                dueDate = targetDueDate,
                dueTime = dueTime
            )
            taskRepository.updateTask(updatedTask)
            if (reminderScheduler != null) {
                reminderScheduler.scheduleReminder(updatedTask)
            }
            _eventChannel.send(TodayEvent.ShowSnackbar(message = confirmationMessage))
        }
    }

    fun undoDelete() {
        val taskToRestore = recentlyDeletedTask ?: return
        viewModelScope.launch {
            val newId = taskRepository.createTask(
                task = taskToRestore.task.copy(id = 0),
                subtaskTitles = taskToRestore.subtasks.map { it.title }
            )
            if (taskToRestore.task.dueDate != null && !taskToRestore.task.isCompleted) {
                reminderScheduler?.scheduleReminder(taskToRestore.task.copy(id = newId))
            }
            recentlyDeletedTask = null
        }
    }

    companion object {
        fun provideFactory(
            taskRepository: TaskRepository,
            reminderScheduler: com.personal.daymark.core.notifications.ReminderScheduler? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TodayViewModel(taskRepository, reminderScheduler) as T
                }
            }
    }
}
