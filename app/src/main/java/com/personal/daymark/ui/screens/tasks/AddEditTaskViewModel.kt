package com.personal.daymark.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.personal.daymark.core.notifications.ReminderScheduler
import com.personal.daymark.core.recurrence.RecurrenceRule
import com.personal.daymark.data.local.entity.CategoryEntity
import com.personal.daymark.data.local.entity.SubtaskEntity
import com.personal.daymark.data.local.entity.TaskEntity
import com.personal.daymark.data.repository.CategoryRepository
import com.personal.daymark.data.repository.SettingsRepository
import com.personal.daymark.data.repository.TaskRepository
import java.util.UUID
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RecurringEditMode {
    DEFAULT,
    THIS_INSTANCE_ONLY,
    ENTIRE_SERIES
}

data class SubtaskItemState(
    val id: Long = 0,
    val title: String,
    val isCompleted: Boolean = false
)

data class AddEditTaskUiState(
    val taskId: Long? = null,
    val title: String = "",
    val description: String = "",
    val categoryId: Long? = null,
    val priority: Int = 0,
    val dueDate: Long? = null,
    val dueTimeMinutes: Int? = null,
    val recurrenceRule: RecurrenceRule? = null,
    val seriesId: String? = null,
    val isTitleError: Boolean = false,
    val isSaving: Boolean = false,
    val showRecurringEditDialog: Boolean = false,
    val subtasks: List<SubtaskItemState> = emptyList(),
    val newSubtaskInput: String = ""
)

sealed interface AddEditTaskEvent {
    data object SaveSuccess : AddEditTaskEvent
    data class ShowError(val message: String) : AddEditTaskEvent
}

class AddEditTaskViewModel(
    private val taskId: Long? = null,
    private val taskRepository: TaskRepository,
    categoryRepository: CategoryRepository,
    private val reminderScheduler: ReminderScheduler? = null,
    private val initialDueDate: Long? = null,
    private val settingsRepository: SettingsRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditTaskUiState(taskId = taskId, dueDate = initialDueDate))
    val uiState: StateFlow<AddEditTaskUiState> = _uiState.asStateFlow()

    private var initialSeriesId: String? = null
    private var initialRecurrenceRule: String? = null

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _eventChannel = Channel<AddEditTaskEvent>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()

    init {
        if (taskId != null && taskId > 0) {
            loadExistingTask(taskId)
        } else if (settingsRepository != null) {
            viewModelScope.launch {
                val prefs = settingsRepository.userPreferencesFlow.firstOrNull()
                if (prefs != null && prefs.defaultTaskPriority > 0) {
                    _uiState.update { it.copy(priority = prefs.defaultTaskPriority) }
                }
            }
        }
    }

    private fun loadExistingTask(id: Long) {
        viewModelScope.launch {
            val taskWithDetails = taskRepository.getTaskWithDetailsById(id).firstOrNull()
            if (taskWithDetails != null) {
                val task = taskWithDetails.task
                initialSeriesId = task.seriesId
                initialRecurrenceRule = task.recurrenceRule
                val parsedRule = RecurrenceRule.fromJson(task.recurrenceRule)
                val subtasksState = taskWithDetails.subtasks.map {
                    SubtaskItemState(id = it.id, title = it.title, isCompleted = it.isCompleted)
                }
                _uiState.update {
                    it.copy(
                        taskId = task.id,
                        title = task.title,
                        description = task.description.orEmpty(),
                        categoryId = task.categoryId,
                        priority = task.priority,
                        dueDate = task.dueDate,
                        dueTimeMinutes = task.dueTime,
                        recurrenceRule = parsedRule,
                        seriesId = task.seriesId,
                        subtasks = subtasksState
                    )
                }
            }
        }
    }

    fun onTitleChanged(newTitle: String) {
        _uiState.update { it.copy(title = newTitle, isTitleError = false) }
    }

    fun onDescriptionChanged(newDesc: String) {
        _uiState.update { it.copy(description = newDesc) }
    }

    fun onPrioritySelected(newPriority: Int) {
        _uiState.update { it.copy(priority = newPriority) }
    }

    fun onCategorySelected(newCategoryId: Long?) {
        _uiState.update { it.copy(categoryId = newCategoryId) }
    }

    fun onDueDateSelected(newDueDate: Long?) {
        _uiState.update { it.copy(dueDate = newDueDate) }
    }

    fun onDueTimeSelected(newDueTimeMinutes: Int?) {
        _uiState.update { it.copy(dueTimeMinutes = newDueTimeMinutes) }
    }

    fun onRecurrenceRuleSelected(rule: RecurrenceRule?) {
        _uiState.update { it.copy(recurrenceRule = rule) }
    }

    fun dismissRecurringEditDialog() {
        _uiState.update { it.copy(showRecurringEditDialog = false) }
    }

    // Subtask actions
    fun onNewSubtaskInputChanged(text: String) {
        _uiState.update { it.copy(newSubtaskInput = text) }
    }

    fun addSubtask() {
        val trimmed = _uiState.value.newSubtaskInput.trim()
        if (trimmed.isNotBlank()) {
            _uiState.update {
                it.copy(
                    subtasks = it.subtasks + SubtaskItemState(title = trimmed),
                    newSubtaskInput = ""
                )
            }
        }
    }

    fun toggleSubtask(index: Int) {
        val currentList = _uiState.value.subtasks.toMutableList()
        if (index in currentList.indices) {
            val item = currentList[index]
            val updated = item.copy(isCompleted = !item.isCompleted)
            currentList[index] = updated
            _uiState.update { it.copy(subtasks = currentList) }

            if (item.id > 0) {
                viewModelScope.launch {
                    taskRepository.setSubtaskCompleted(item.id, updated.isCompleted)
                }
            }
        }
    }

    fun deleteSubtask(index: Int) {
        val currentList = _uiState.value.subtasks.toMutableList()
        if (index in currentList.indices) {
            val item = currentList.removeAt(index)
            _uiState.update { it.copy(subtasks = currentList) }
            if (item.id > 0) {
                viewModelScope.launch {
                    taskRepository.deleteSubtask(
                        SubtaskEntity(
                            id = item.id,
                            taskId = _uiState.value.taskId ?: 0L,
                            title = item.title
                        )
                    )
                }
            }
        }
    }

    /**
     * Fast task creation/editing with recurring support and subtask checklist sync.
     */
    fun saveTask(editMode: RecurringEditMode = RecurringEditMode.DEFAULT) {
        val currentState = _uiState.value
        val trimmedTitle = currentState.title.trim()

        if (trimmedTitle.isBlank()) {
            _uiState.update { it.copy(isTitleError = true) }
            return
        }

        val isExistingRecurring = currentState.taskId != null &&
                currentState.taskId != 0L &&
                !initialSeriesId.isNullOrBlank()

        if (isExistingRecurring && editMode == RecurringEditMode.DEFAULT) {
            _uiState.update { it.copy(showRecurringEditDialog = true) }
            return
        }

        _uiState.update { it.copy(showRecurringEditDialog = false, isSaving = true) }

        viewModelScope.launch {
            val ruleJson = currentState.recurrenceRule?.toJson()

            val savedTaskId = if (editMode == RecurringEditMode.THIS_INSTANCE_ONLY) {
                val taskEntity = TaskEntity(
                    id = currentState.taskId ?: 0L,
                    title = trimmedTitle,
                    description = currentState.description.trim().ifBlank { null },
                    categoryId = currentState.categoryId,
                    priority = currentState.priority,
                    dueDate = currentState.dueDate,
                    dueTime = currentState.dueTimeMinutes,
                    recurrenceRule = null,
                    seriesId = null
                )
                taskRepository.updateTask(taskEntity)
                taskEntity.id
            } else {
                val effectiveSeriesId = when {
                    currentState.taskId != null && currentState.taskId != 0L -> initialSeriesId ?: (if (ruleJson != null) UUID.randomUUID().toString() else null)
                    ruleJson != null -> UUID.randomUUID().toString()
                    else -> null
                }

                val taskEntity = TaskEntity(
                    id = currentState.taskId ?: 0L,
                    title = trimmedTitle,
                    description = currentState.description.trim().ifBlank { null },
                    categoryId = currentState.categoryId,
                    priority = currentState.priority,
                    dueDate = currentState.dueDate,
                    dueTime = currentState.dueTimeMinutes,
                    recurrenceRule = ruleJson,
                    seriesId = effectiveSeriesId
                )

                if (currentState.taskId == null || currentState.taskId == 0L) {
                    taskRepository.createTask(taskEntity)
                } else {
                    taskRepository.updateTask(taskEntity)
                    if (effectiveSeriesId != null && editMode == RecurringEditMode.ENTIRE_SERIES) {
                        taskRepository.updateEntireSeries(effectiveSeriesId, taskEntity)
                    }
                    currentState.taskId
                }
            }

            // Sync subtasks
            val subtaskEntities = currentState.subtasks.mapIndexed { index, item ->
                SubtaskEntity(
                    id = item.id,
                    taskId = savedTaskId,
                    title = item.title,
                    isCompleted = item.isCompleted,
                    sortOrder = index
                )
            }
            taskRepository.syncSubtasks(savedTaskId, subtaskEntities)

            // Reminder schedule/cancel
            val finalTask = TaskEntity(
                id = savedTaskId,
                title = trimmedTitle,
                dueDate = currentState.dueDate,
                dueTime = currentState.dueTimeMinutes
            )
            if (reminderScheduler != null) {
                if (finalTask.dueDate != null) {
                    reminderScheduler.scheduleReminder(finalTask)
                } else {
                    reminderScheduler.cancelReminder(finalTask.id)
                }
            }

            _eventChannel.send(AddEditTaskEvent.SaveSuccess)
        }
    }

    companion object {
        fun provideFactory(
            taskId: Long? = null,
            taskRepository: TaskRepository,
            categoryRepository: CategoryRepository,
            reminderScheduler: ReminderScheduler? = null,
            initialDueDate: Long? = null,
            settingsRepository: SettingsRepository? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AddEditTaskViewModel(
                    taskId = taskId,
                    taskRepository = taskRepository,
                    categoryRepository = categoryRepository,
                    reminderScheduler = reminderScheduler,
                    initialDueDate = initialDueDate,
                    settingsRepository = settingsRepository
                ) as T
            }
        }
    }
}
