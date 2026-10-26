package com.personal.daymark.ui.screens.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.personal.daymark.core.common.DateFormatter
import com.personal.daymark.core.notifications.ReminderScheduler
import com.personal.daymark.data.local.entity.TaskWithSubtasksAndCategory
import com.personal.daymark.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class EndOfDayTaskAction(val displayName: String) {
    MOVE_TO_TOMORROW("Tomorrow"),
    MOVE_TO_INBOX("Inbox"),
    MARK_COMPLETE("Complete"),
    KEEP("Keep")
}

data class EndOfDayUiState(
    val incompleteTasks: List<TaskWithSubtasksAndCategory> = emptyList(),
    val taskActions: Map<Long, EndOfDayTaskAction> = emptyMap(),
    val isSubmitting: Boolean = false,
    val isComplete: Boolean = false,
    val completedCount: Int = 0,
    val summaryMessage: String? = null
)

class EndOfDayViewModel(
    private val taskRepository: TaskRepository,
    private val reminderScheduler: ReminderScheduler? = null
) : ViewModel() {

    private val _customActions = MutableStateFlow<Map<Long, EndOfDayTaskAction>>(emptyMap())
    private val _isSubmitting = MutableStateFlow(false)
    private val _isComplete = MutableStateFlow(false)
    private val _summaryMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<EndOfDayUiState> = combine(
        taskRepository.getAllTasksWithDetails(),
        _customActions,
        _isSubmitting,
        _isComplete,
        _summaryMessage
    ) { allTasks, customActions, isSubmitting, isComplete, summaryMessage ->
        val endOfToday = DateFormatter.getEndOfTodayMillis()

        // Incomplete tasks scheduled on or before today
        val incomplete = allTasks.filter { item ->
            !item.task.isCompleted &&
                    item.task.dueDate != null &&
                    item.task.dueDate <= endOfToday
        }.sortedWith(
            compareByDescending<TaskWithSubtasksAndCategory> { it.task.priority }
                .thenBy { it.task.dueDate }
                .thenBy { it.task.dueTime ?: Int.MAX_VALUE }
        )

        // Default all unmapped tasks to MOVE_TO_TOMORROW
        val effectiveActions = incomplete.associate { taskItem ->
            taskItem.task.id to (customActions[taskItem.task.id] ?: EndOfDayTaskAction.MOVE_TO_TOMORROW)
        }

        EndOfDayUiState(
            incompleteTasks = incomplete,
            taskActions = effectiveActions,
            isSubmitting = isSubmitting,
            isComplete = isComplete,
            completedCount = incomplete.size,
            summaryMessage = summaryMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EndOfDayUiState()
    )

    fun setTaskAction(taskId: Long, action: EndOfDayTaskAction) {
        _customActions.value = _customActions.value.toMutableMap().apply {
            put(taskId, action)
        }
    }

    fun applyBulkAction(action: EndOfDayTaskAction) {
        val currentTasks = uiState.value.incompleteTasks
        val updated = currentTasks.associate { it.task.id to action }
        _customActions.value = updated
    }

    fun confirmShutdown(onFinished: () -> Unit = {}) {
        val state = uiState.value
        if (state.isSubmitting) return

        viewModelScope.launch {
            _isSubmitting.value = true

            val tomorrowStartMillis = DateFormatter.getStartOfTodayMillis() + 24 * 60 * 60 * 1000L

            for (taskItem in state.incompleteTasks) {
                val action = state.taskActions[taskItem.task.id] ?: EndOfDayTaskAction.MOVE_TO_TOMORROW
                when (action) {
                    EndOfDayTaskAction.MOVE_TO_TOMORROW -> {
                        val updatedTask = taskItem.task.copy(dueDate = tomorrowStartMillis)
                        taskRepository.updateTask(updatedTask)
                        if (updatedTask.dueTime != null) {
                            reminderScheduler?.scheduleReminder(updatedTask)
                        } else {
                            reminderScheduler?.cancelReminder(updatedTask.id)
                        }
                    }
                    EndOfDayTaskAction.MOVE_TO_INBOX -> {
                        val updatedTask = taskItem.task.copy(dueDate = null, dueTime = null)
                        taskRepository.updateTask(updatedTask)
                        reminderScheduler?.cancelReminder(updatedTask.id)
                    }
                    EndOfDayTaskAction.MARK_COMPLETE -> {
                        if (taskItem.task.recurrenceRule != null) {
                            val next = taskRepository.completeTaskAndGenerateNext(taskItem.task.id)
                            reminderScheduler?.cancelReminder(taskItem.task.id)
                            if (next != null && next.dueDate != null) {
                                reminderScheduler?.scheduleReminder(next)
                            }
                        } else {
                            taskRepository.setTaskCompleted(taskItem.task.id, true)
                            reminderScheduler?.cancelReminder(taskItem.task.id)
                        }
                    }
                    EndOfDayTaskAction.KEEP -> {
                        // Keep unchanged on current schedule
                    }
                }
            }

            _isSubmitting.value = false
            _isComplete.value = true
            _summaryMessage.value = "Day closed. Have a great evening!"
            onFinished()
        }
    }

    fun resetState() {
        _customActions.value = emptyMap()
        _isSubmitting.value = false
        _isComplete.value = false
        _summaryMessage.value = null
    }

    companion object {
        fun provideFactory(
            taskRepository: TaskRepository,
            reminderScheduler: ReminderScheduler? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return EndOfDayViewModel(taskRepository, reminderScheduler) as T
            }
        }
    }
}
