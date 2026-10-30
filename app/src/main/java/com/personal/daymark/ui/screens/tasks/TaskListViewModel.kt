package com.personal.daymark.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.personal.daymark.core.common.DateFormatter
import com.personal.daymark.data.local.entity.CategoryEntity
import com.personal.daymark.data.local.entity.TaskWithSubtasksAndCategory
import com.personal.daymark.data.repository.CategoryRepository
import com.personal.daymark.data.repository.TaskRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TaskViewFilter(val label: String) {
    TODAY("Today"),
    ALL("All"),
    INBOX("Inbox"),
    UPCOMING("Upcoming"),
    OVERDUE("Overdue"),
    COMPLETED("Completed")
}

enum class TaskSortOption(val label: String) {
    DEFAULT("Default"),
    DUE_DATE("Due Date"),
    PRIORITY("Priority"),
    RECENTLY_ADDED("Recently Added"),
    ALPHABETICAL("Alphabetical")
}

data class FilterCriteria(
    val categoryId: Long? = null,
    val viewFilter: TaskViewFilter = TaskViewFilter.TODAY,
    val sortOption: TaskSortOption = TaskSortOption.DEFAULT,
    val searchQuery: String = ""
)

data class TaskListUiState(
    val tasks: List<TaskWithSubtasksAndCategory> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val selectedCategoryId: Long? = null,
    val selectedViewFilter: TaskViewFilter = TaskViewFilter.TODAY,
    val selectedSortOption: TaskSortOption = TaskSortOption.DEFAULT,
    val searchQuery: String = "",
    val inboxCount: Int = 0,
    val todayCount: Int = 0,
    val upcomingCount: Int = 0,
    val overdueCount: Int = 0,
    val completedCount: Int = 0,
    val isLoading: Boolean = false
)

sealed interface TaskListEvent {
    data class ShowSnackbar(val message: String, val actionLabel: String? = null) : TaskListEvent
}

class TaskListViewModel(
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository,
    private val reminderScheduler: com.personal.daymark.core.notifications.ReminderScheduler? = null,
    initialFilter: TaskViewFilter = TaskViewFilter.TODAY
) : ViewModel() {

    private val _filterCriteria = MutableStateFlow(FilterCriteria(viewFilter = initialFilter))

    private val _eventChannel = Channel<TaskListEvent>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()

    private var recentlyDeletedTask: TaskWithSubtasksAndCategory? = null

    val uiState: StateFlow<TaskListUiState> = combine(
        taskRepository.getAllTasksWithDetails(),
        categoryRepository.getAllCategories(),
        _filterCriteria
    ) { allTasks, categories, criteria ->
        val startOfToday = DateFormatter.getStartOfTodayMillis()
        val endOfToday = DateFormatter.getEndOfTodayMillis()

        // Badge counters across entire database
        val inboxCount = allTasks.count { !it.task.isCompleted && it.task.dueDate == null }
        val todayCount = allTasks.count { it.task.dueDate != null && it.task.dueDate in startOfToday..endOfToday }
        val upcomingCount = allTasks.count { !it.task.isCompleted && it.task.dueDate != null && it.task.dueDate > endOfToday }
        val overdueCount = allTasks.count { !it.task.isCompleted && it.task.dueDate != null && it.task.dueDate < startOfToday }
        val completedCount = allTasks.count { it.task.isCompleted }

        // 1. View Filter
        val viewFiltered = when (criteria.viewFilter) {
            TaskViewFilter.ALL -> allTasks
            TaskViewFilter.INBOX -> allTasks.filter { !it.task.isCompleted && it.task.dueDate == null }
            TaskViewFilter.TODAY -> allTasks.filter { it.task.dueDate != null && it.task.dueDate in startOfToday..endOfToday }
            TaskViewFilter.UPCOMING -> allTasks.filter { !it.task.isCompleted && it.task.dueDate != null && it.task.dueDate > endOfToday }
            TaskViewFilter.OVERDUE -> allTasks.filter { !it.task.isCompleted && it.task.dueDate != null && it.task.dueDate < startOfToday }
            TaskViewFilter.COMPLETED -> allTasks.filter { it.task.isCompleted }
        }

        // 2. Category Filter & Search Query
        val secondaryFiltered = viewFiltered.filter { item ->
            val matchesCategory = criteria.categoryId == null || item.task.categoryId == criteria.categoryId
            val matchesQuery = criteria.searchQuery.isBlank() ||
                    item.task.title.contains(criteria.searchQuery, ignoreCase = true) ||
                    (item.task.description?.contains(criteria.searchQuery, ignoreCase = true) == true)
            matchesCategory && matchesQuery
        }

        // 3. Sorting
        val sortedTasks = when (criteria.sortOption) {
            TaskSortOption.DEFAULT -> secondaryFiltered.sortedWith(
                compareBy<TaskWithSubtasksAndCategory> { it.task.isCompleted }
                    .thenByDescending { it.task.priority }
                    .thenBy { it.task.dueDate ?: Long.MAX_VALUE }
                    .thenByDescending { it.task.createdAt }
            )
            TaskSortOption.DUE_DATE -> secondaryFiltered.sortedWith(
                compareBy<TaskWithSubtasksAndCategory> { it.task.dueDate ?: Long.MAX_VALUE }
                    .thenBy { it.task.dueTime ?: Int.MAX_VALUE }
                    .thenByDescending { it.task.priority }
            )
            TaskSortOption.PRIORITY -> secondaryFiltered.sortedWith(
                compareByDescending<TaskWithSubtasksAndCategory> { it.task.priority }
                    .thenBy { it.task.dueDate ?: Long.MAX_VALUE }
            )
            TaskSortOption.RECENTLY_ADDED -> secondaryFiltered.sortedByDescending { it.task.createdAt }
            TaskSortOption.ALPHABETICAL -> secondaryFiltered.sortedBy { it.task.title.lowercase() }
        }

        TaskListUiState(
            tasks = sortedTasks,
            categories = categories,
            selectedCategoryId = criteria.categoryId,
            selectedViewFilter = criteria.viewFilter,
            selectedSortOption = criteria.sortOption,
            searchQuery = criteria.searchQuery,
            inboxCount = inboxCount,
            todayCount = todayCount,
            upcomingCount = upcomingCount,
            overdueCount = overdueCount,
            completedCount = completedCount,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TaskListUiState(isLoading = true)
    )

    fun onViewFilterSelected(filter: TaskViewFilter) {
        _filterCriteria.update { it.copy(viewFilter = filter) }
    }

    fun onSortOptionSelected(option: TaskSortOption) {
        _filterCriteria.update { it.copy(sortOption = option) }
    }

    fun onCategorySelected(categoryId: Long?) {
        _filterCriteria.update { it.copy(categoryId = categoryId) }
    }

    fun onSearchQueryChanged(query: String) {
        _filterCriteria.update { it.copy(searchQuery = query) }
    }

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
                TaskListEvent.ShowSnackbar(
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
                TaskListEvent.ShowSnackbar(
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
                TaskListEvent.ShowSnackbar(
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
            _eventChannel.send(TaskListEvent.ShowSnackbar(message = confirmationMessage))
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
            categoryRepository: CategoryRepository,
            reminderScheduler: com.personal.daymark.core.notifications.ReminderScheduler? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TaskListViewModel(taskRepository, categoryRepository, reminderScheduler) as T
            }
        }
    }
}
