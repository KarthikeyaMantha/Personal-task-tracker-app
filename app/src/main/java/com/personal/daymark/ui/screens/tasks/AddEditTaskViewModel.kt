package com.personal.daymark.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.personal.daymark.core.notifications.ReminderScheduler
import com.personal.daymark.core.parser.ParsedTaskResult
import com.personal.daymark.core.parser.TaskParser
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
    val newSubtaskInput: String = "",
    val parsedResult: ParsedTaskResult? = null,
    val durationMinutes: Int = 0,
    val startTime: String? = null,
    val isMedicine: Boolean = false,
    val dosage: String = "",
    val medicineForm: String = "",
    val stockCount: Int? = null,
    val refillThreshold: Int? = null
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

    private var initialTask: TaskEntity? = null
    private var initialSeriesId: String? = null
    private var initialRecurrenceRule: String? = null
    private var isUserExplicitDueDate: Boolean = (taskId != null && taskId > 0)

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
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
                initialTask = task
                initialSeriesId = task.seriesId
                initialRecurrenceRule = task.recurrenceRule
                isUserExplicitDueDate = true
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
                        subtasks = subtasksState,
                        durationMinutes = task.durationMinutes,
                        startTime = task.startTime,
                        isMedicine = task.isMedicine,
                        dosage = task.dosage.orEmpty(),
                        medicineForm = task.medicineForm.orEmpty(),
                        stockCount = task.stockCount,
                        refillThreshold = task.refillThreshold
                    )
                }
            }
        }
    }

    fun onTitleChanged(newTitle: String) {
        val parsed = TaskParser.parse(newTitle, categories.value)
        _uiState.update { current ->
            val updatedDueDate = if (!isUserExplicitDueDate) {
                parsed?.dueDate ?: initialDueDate
            } else {
                current.dueDate
            }
            val updatedDueTime = if (!isUserExplicitDueDate && parsed?.dueTimeMinutes != null) {
                parsed.dueTimeMinutes
            } else {
                current.dueTimeMinutes
            }
            current.copy(
                title = newTitle,
                isTitleError = false,
                parsedResult = parsed,
                dueDate = updatedDueDate,
                dueTimeMinutes = updatedDueTime
            )
        }
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
        isUserExplicitDueDate = true
        _uiState.update { it.copy(dueDate = newDueDate) }
    }

    fun onDueTimeSelected(newDueTimeMinutes: Int?) {
        val calculatedStartTime = newDueTimeMinutes?.let {
            String.format(java.util.Locale.US, "%02d:%02d", it / 60, it % 60)
        }
        _uiState.update {
            it.copy(
                dueTimeMinutes = newDueTimeMinutes,
                startTime = calculatedStartTime,
                durationMinutes = if (it.durationMinutes == 0 && newDueTimeMinutes != null) 30 else it.durationMinutes
            )
        }
    }

    fun onDurationSelected(duration: Int) {
        _uiState.update { it.copy(durationMinutes = duration) }
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

    // Medicine fields
    fun onIsMedicineToggled(isMedicine: Boolean) {
        _uiState.update { it.copy(isMedicine = isMedicine) }
    }

    fun onDosageChanged(dosage: String) {
        _uiState.update { it.copy(dosage = dosage) }
    }

    fun onMedicineFormChanged(form: String) {
        _uiState.update { it.copy(medicineForm = form) }
    }

    fun onStockCountChanged(count: Int?) {
        _uiState.update { it.copy(stockCount = count) }
    }

    fun onRefillThresholdChanged(threshold: Int?) {
        _uiState.update { it.copy(refillThreshold = threshold) }
    }

    /**
     * Fast task creation/editing with recurring support and subtask checklist sync.
     */
    fun saveTask(editMode: RecurringEditMode = RecurringEditMode.DEFAULT) {
        if (_uiState.value.isSaving) return

        val currentState = _uiState.value
        val rawTitle = currentState.title.trim()

        if (rawTitle.isBlank()) {
            _uiState.update { it.copy(isTitleError = true) }
            return
        }

        val parsed = currentState.parsedResult
        val effectiveTitle = if (parsed != null && parsed.hasDetections) {
            parsed.cleanTitle.ifBlank { rawTitle }
        } else {
            rawTitle
        }

        val effectiveCategoryId = currentState.categoryId
            ?: parsed?.matchedCategory?.id
            ?: categories.value.firstOrNull { it.name.equals(parsed?.detectedCategoryName, ignoreCase = true) }?.id
        val effectivePriority = if (currentState.priority == 0) (parsed?.priority ?: 0) else currentState.priority
        val effectiveDueDate = if (isUserExplicitDueDate) {
            currentState.dueDate ?: parsed?.dueDate
        } else {
            parsed?.dueDate ?: currentState.dueDate
        }
        val effectiveDueTime = currentState.dueTimeMinutes ?: parsed?.dueTimeMinutes

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

            val effectiveStartTime = currentState.startTime ?: effectiveDueTime?.let {
                String.format(java.util.Locale.US, "%02d:%02d", it / 60, it % 60)
            }
            val effectiveDuration = if (currentState.durationMinutes > 0) {
                currentState.durationMinutes
            } else if (effectiveStartTime != null) {
                30
            } else {
                0
            }

            val isMed = currentState.isMedicine
            val dosageVal = currentState.dosage.trim().ifBlank { null }
            val formVal = currentState.medicineForm.trim().ifBlank { null }
            val stockVal = currentState.stockCount
            val thresholdVal = currentState.refillThreshold

            val isCompleted = initialTask?.isCompleted ?: false
            val completionTimestamp = initialTask?.completionTimestamp
            val createdAt = initialTask?.createdAt ?: System.currentTimeMillis()

            val savedTaskId = if (editMode == RecurringEditMode.THIS_INSTANCE_ONLY) {
                val taskEntity = TaskEntity(
                    id = currentState.taskId ?: 0L,
                    title = effectiveTitle,
                    description = currentState.description.trim().ifBlank { null },
                    categoryId = effectiveCategoryId,
                    priority = effectivePriority,
                    dueDate = effectiveDueDate,
                    dueTime = effectiveDueTime,
                    recurrenceRule = null,
                    seriesId = null,
                    durationMinutes = effectiveDuration,
                    startTime = effectiveStartTime,
                    isMedicine = isMed,
                    dosage = dosageVal,
                    medicineForm = formVal,
                    stockCount = stockVal,
                    refillThreshold = thresholdVal,
                    isCompleted = isCompleted,
                    completionTimestamp = completionTimestamp,
                    createdAt = createdAt
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
                    title = effectiveTitle,
                    description = currentState.description.trim().ifBlank { null },
                    categoryId = effectiveCategoryId,
                    priority = effectivePriority,
                    dueDate = effectiveDueDate,
                    dueTime = effectiveDueTime,
                    recurrenceRule = ruleJson,
                    seriesId = effectiveSeriesId,
                    durationMinutes = effectiveDuration,
                    startTime = effectiveStartTime,
                    isMedicine = isMed,
                    dosage = dosageVal,
                    medicineForm = formVal,
                    stockCount = stockVal,
                    refillThreshold = thresholdVal,
                    isCompleted = isCompleted,
                    completionTimestamp = completionTimestamp,
                    createdAt = createdAt
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
                title = effectiveTitle,
                dueDate = effectiveDueDate,
                dueTime = effectiveDueTime,
                isCompleted = isCompleted,
                isMedicine = isMed,
                dosage = dosageVal
            )
            if (reminderScheduler != null) {
                if (!isCompleted && finalTask.dueDate != null) {
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
