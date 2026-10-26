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
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class EndOfDayTaskAction(val displayName: String) {
    MOVE_TO_TOMORROW("Tomorrow"),
    MOVE_TO_INBOX("Inbox"),
    MARK_COMPLETE("Complete"),
    KEEP("Keep")
}

data class MigrationDayTarget(
    val date: LocalDate,
    val dayOfWeek: String,
    val label: String,
    val dateMillis: Long
)

sealed class MigrationChoice {
    data class SpecificDate(val dateMillis: Long, val label: String) : MigrationChoice()
    data object Inbox : MigrationChoice()
    data object Completed : MigrationChoice()
    data object KeepToday : MigrationChoice()
}

data class EndOfDayUiState(
    val incompleteTasks: List<TaskWithSubtasksAndCategory> = emptyList(),
    val taskActions: Map<Long, EndOfDayTaskAction> = emptyMap(),
    val taskDestinations: Map<Long, MigrationChoice> = emptyMap(),
    val targetDays: List<MigrationDayTarget> = emptyList(),
    val isSubmitting: Boolean = false,
    val isComplete: Boolean = false,
    val completedCount: Int = 0,
    val summaryMessage: String? = null
)

class EndOfDayViewModel(
    private val taskRepository: TaskRepository,
    private val reminderScheduler: ReminderScheduler? = null
) : ViewModel() {

    private val _customDestinations = MutableStateFlow<Map<Long, MigrationChoice>>(emptyMap())
    private val _isSubmitting = MutableStateFlow(false)
    private val _isComplete = MutableStateFlow(false)
    private val _summaryMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<EndOfDayUiState> = combine(
        taskRepository.getAllTasksWithDetails(),
        _customDestinations,
        _isSubmitting,
        _isComplete,
        _summaryMessage
    ) { allTasks, customDestinations, isSubmitting, isComplete, summaryMessage ->
        val endOfToday = DateFormatter.getEndOfTodayMillis()
        val today = LocalDate.now()
        val zoneId = ZoneId.systemDefault()

        // Build next 4 future days (Tomorrow, Day+2, Day+3, Day+4)
        val targets = (1..4).map { offset ->
            val date = today.plusDays(offset.toLong())
            val label = when (offset) {
                1 -> "Tomorrow"
                else -> date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
            }
            val dayOfWeek = date.format(DateTimeFormatter.ofPattern("EEE"))
            val dateMillis = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
            MigrationDayTarget(
                date = date,
                dayOfWeek = dayOfWeek,
                label = label,
                dateMillis = dateMillis
            )
        }

        val tomorrowMillis = targets.firstOrNull()?.dateMillis
            ?: (DateFormatter.getStartOfTodayMillis() + 86400000L)
        val defaultTomorrowChoice = MigrationChoice.SpecificDate(tomorrowMillis, "Tomorrow")

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

        // Default all unmapped tasks to Tomorrow
        val effectiveDestinations = incomplete.associate { taskItem ->
            taskItem.task.id to (customDestinations[taskItem.task.id] ?: defaultTomorrowChoice)
        }

        // Map backwards to EndOfDayTaskAction for existing UI / testing compatibility
        val legacyActions = effectiveDestinations.mapValues { (_, choice) ->
            when (choice) {
                is MigrationChoice.SpecificDate -> EndOfDayTaskAction.MOVE_TO_TOMORROW
                MigrationChoice.Inbox -> EndOfDayTaskAction.MOVE_TO_INBOX
                MigrationChoice.Completed -> EndOfDayTaskAction.MARK_COMPLETE
                MigrationChoice.KeepToday -> EndOfDayTaskAction.KEEP
            }
        }

        EndOfDayUiState(
            incompleteTasks = incomplete,
            taskActions = legacyActions,
            taskDestinations = effectiveDestinations,
            targetDays = targets,
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

    fun assignTaskToTargetDate(taskId: Long, target: MigrationDayTarget) {
        val choice = MigrationChoice.SpecificDate(target.dateMillis, target.label)
        _customDestinations.value = _customDestinations.value.toMutableMap().apply {
            put(taskId, choice)
        }
    }

    fun assignTaskToInbox(taskId: Long) {
        _customDestinations.value = _customDestinations.value.toMutableMap().apply {
            put(taskId, MigrationChoice.Inbox)
        }
    }

    fun setTaskAction(taskId: Long, action: EndOfDayTaskAction) {
        val today = LocalDate.now()
        val zoneId = ZoneId.systemDefault()
        val tomorrowMillis = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

        val choice = when (action) {
            EndOfDayTaskAction.MOVE_TO_TOMORROW -> MigrationChoice.SpecificDate(tomorrowMillis, "Tomorrow")
            EndOfDayTaskAction.MOVE_TO_INBOX -> MigrationChoice.Inbox
            EndOfDayTaskAction.MARK_COMPLETE -> MigrationChoice.Completed
            EndOfDayTaskAction.KEEP -> MigrationChoice.KeepToday
        }
        _customDestinations.value = _customDestinations.value.toMutableMap().apply {
            put(taskId, choice)
        }
    }

    fun applyBulkAction(action: EndOfDayTaskAction) {
        val currentTasks = uiState.value.incompleteTasks
        val today = LocalDate.now()
        val zoneId = ZoneId.systemDefault()
        val tomorrowMillis = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

        val choice = when (action) {
            EndOfDayTaskAction.MOVE_TO_TOMORROW -> MigrationChoice.SpecificDate(tomorrowMillis, "Tomorrow")
            EndOfDayTaskAction.MOVE_TO_INBOX -> MigrationChoice.Inbox
            EndOfDayTaskAction.MARK_COMPLETE -> MigrationChoice.Completed
            EndOfDayTaskAction.KEEP -> MigrationChoice.KeepToday
        }
        val updated = currentTasks.associate { it.task.id to choice }
        _customDestinations.value = updated
    }

    fun leaveAllInInbox() {
        val currentTasks = uiState.value.incompleteTasks
        val updated = currentTasks.associate { it.task.id to MigrationChoice.Inbox }
        _customDestinations.value = updated
    }

    fun confirmShutdown(onFinished: () -> Unit = {}) {
        val state = uiState.value
        if (state.isSubmitting) return

        viewModelScope.launch {
            _isSubmitting.value = true

            val tomorrowStartMillis = DateFormatter.getStartOfTodayMillis() + 86400000L

            for (taskItem in state.incompleteTasks) {
                val choice = state.taskDestinations[taskItem.task.id]
                    ?: MigrationChoice.SpecificDate(tomorrowStartMillis, "Tomorrow")

                when (choice) {
                    is MigrationChoice.SpecificDate -> {
                        val updatedTask = taskItem.task.copy(
                            dueDate = choice.dateMillis,
                            updatedAt = System.currentTimeMillis()
                        )
                        taskRepository.updateTask(updatedTask)
                        if (updatedTask.dueTime != null) {
                            reminderScheduler?.scheduleReminder(updatedTask)
                        } else {
                            reminderScheduler?.cancelReminder(updatedTask.id)
                        }
                    }
                    MigrationChoice.Inbox -> {
                        // Strip due date and time entirely
                        val updatedTask = taskItem.task.copy(
                            dueDate = null,
                            dueTime = null,
                            startTime = null,
                            durationMinutes = 0,
                            updatedAt = System.currentTimeMillis()
                        )
                        taskRepository.updateTask(updatedTask)
                        reminderScheduler?.cancelReminder(updatedTask.id)
                    }
                    MigrationChoice.Completed -> {
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
                    MigrationChoice.KeepToday -> {
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
        _customDestinations.value = emptyMap()
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
