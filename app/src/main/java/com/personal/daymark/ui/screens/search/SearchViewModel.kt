package com.personal.daymark.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.personal.daymark.core.common.DateFormatter
import com.personal.daymark.core.notifications.ReminderScheduler
import com.personal.daymark.data.local.entity.CategoryEntity
import com.personal.daymark.data.local.entity.TaskWithSubtasksAndCategory
import com.personal.daymark.data.repository.CategoryRepository
import com.personal.daymark.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class TaskCompletionFilter(val label: String) {
    ALL("All Tasks"),
    ACTIVE("Active"),
    COMPLETED("Completed")
}

enum class TaskDateRangeFilter(val label: String) {
    ALL("All Dates"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    OVERDUE("Overdue"),
    UPCOMING("Upcoming")
}

enum class TaskSearchSort(val label: String) {
    DUE_DATE("Due Date"),
    PRIORITY("Priority"),
    TITLE("Alphabetical (A-Z)"),
    CREATED_AT("Date Created")
}

data class SearchFilterState(
    val query: String = "",
    val completionFilter: TaskCompletionFilter = TaskCompletionFilter.ALL,
    val selectedCategoryId: Long? = null,
    val selectedPriority: Int? = null, // null = All, 0=None, 1=Low, 2=Medium, 3=High, 4=Urgent
    val dateRangeFilter: TaskDateRangeFilter = TaskDateRangeFilter.ALL,
    val sortBy: TaskSearchSort = TaskSearchSort.DUE_DATE,
    val isFilterSheetOpen: Boolean = false
) {
    val activeFilterCount: Int
        get() {
            var count = 0
            if (completionFilter != TaskCompletionFilter.ALL) count++
            if (selectedCategoryId != null) count++
            if (selectedPriority != null) count++
            if (dateRangeFilter != TaskDateRangeFilter.ALL) count++
            return count
        }
}

data class SearchUiState(
    val filterState: SearchFilterState = SearchFilterState(),
    val categories: List<CategoryEntity> = emptyList(),
    val searchResults: List<TaskWithSubtasksAndCategory> = emptyList(),
    val totalCount: Int = 0
)

class SearchViewModel(
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository,
    private val reminderScheduler: ReminderScheduler? = null
) : ViewModel() {

    private val _filterState = MutableStateFlow(SearchFilterState())
    val filterState: StateFlow<SearchFilterState> = _filterState.asStateFlow()

    val uiState: StateFlow<SearchUiState> = combine(
        taskRepository.getAllTasksWithDetails(),
        categoryRepository.getAllCategories(),
        _filterState
    ) { allTasks, categories, filters ->

        val today = LocalDate.now()
        val zoneId = ZoneId.systemDefault()
        val startOfToday = DateFormatter.getStartOfTodayMillis()
        val endOfToday = DateFormatter.getEndOfTodayMillis()

        // Week boundaries
        val startOfWeek = today.minusDays((today.dayOfWeek.value - 1).toLong())
            .atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endOfWeek = today.plusDays((7 - today.dayOfWeek.value).toLong())
            .atTime(23, 59, 59, 999_000_000)
            .atZone(zoneId).toInstant().toEpochMilli()

        val trimmedQuery = filters.query.trim().lowercase()

        val filtered = allTasks.filter { item ->
            val task = item.task

            // 1. Keyword query match (Title or Description/Notes)
            val matchesQuery = if (trimmedQuery.isEmpty()) {
                true
            } else {
                task.title.lowercase().contains(trimmedQuery) ||
                        (task.description?.lowercase()?.contains(trimmedQuery) == true)
            }

            // 2. Completion Status filter
            val matchesStatus = when (filters.completionFilter) {
                TaskCompletionFilter.ALL -> true
                TaskCompletionFilter.ACTIVE -> !task.isCompleted
                TaskCompletionFilter.COMPLETED -> task.isCompleted
            }

            // 3. Category filter
            val matchesCategory = if (filters.selectedCategoryId == null) {
                true
            } else {
                task.categoryId == filters.selectedCategoryId
            }

            // 4. Priority filter
            val matchesPriority = if (filters.selectedPriority == null) {
                true
            } else {
                task.priority == filters.selectedPriority
            }

            // 5. Date Range filter
            val dueDate = task.dueDate
            val matchesDateRange = when (filters.dateRangeFilter) {
                TaskDateRangeFilter.ALL -> true
                TaskDateRangeFilter.TODAY -> {
                    dueDate != null && dueDate in startOfToday..endOfToday
                }
                TaskDateRangeFilter.THIS_WEEK -> {
                    dueDate != null && dueDate in startOfWeek..endOfWeek
                }
                TaskDateRangeFilter.OVERDUE -> {
                    !task.isCompleted && dueDate != null && dueDate < startOfToday
                }
                TaskDateRangeFilter.UPCOMING -> {
                    dueDate != null && dueDate > endOfToday
                }
            }

            matchesQuery && matchesStatus && matchesCategory && matchesPriority && matchesDateRange
        }

        // Apply Sorting
        val sorted = when (filters.sortBy) {
            TaskSearchSort.DUE_DATE -> {
                filtered.sortedWith(
                    compareBy<TaskWithSubtasksAndCategory> { it.task.isCompleted }
                        .thenBy { it.task.dueDate ?: Long.MAX_VALUE }
                        .thenBy { it.task.dueTime ?: Int.MAX_VALUE }
                        .thenByDescending { it.task.priority }
                )
            }
            TaskSearchSort.PRIORITY -> {
                filtered.sortedWith(
                    compareBy<TaskWithSubtasksAndCategory> { it.task.isCompleted }
                        .thenByDescending { it.task.priority }
                        .thenBy { it.task.dueDate ?: Long.MAX_VALUE }
                )
            }
            TaskSearchSort.TITLE -> {
                filtered.sortedWith(
                    compareBy<TaskWithSubtasksAndCategory> { it.task.isCompleted }
                        .thenBy { it.task.title.lowercase() }
                )
            }
            TaskSearchSort.CREATED_AT -> {
                filtered.sortedWith(
                    compareBy<TaskWithSubtasksAndCategory> { it.task.isCompleted }
                        .thenByDescending { it.task.createdAt }
                )
            }
        }

        SearchUiState(
            filterState = filters,
            categories = categories,
            searchResults = sorted,
            totalCount = sorted.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SearchUiState()
    )

    fun onQueryChanged(newQuery: String) {
        _filterState.update { it.copy(query = newQuery) }
    }

    fun onCompletionFilterSelected(status: TaskCompletionFilter) {
        _filterState.update { it.copy(completionFilter = status) }
    }

    fun onCategorySelected(categoryId: Long?) {
        _filterState.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun onPrioritySelected(priority: Int?) {
        _filterState.update { it.copy(selectedPriority = priority) }
    }

    fun onDateRangeSelected(range: TaskDateRangeFilter) {
        _filterState.update { it.copy(dateRangeFilter = range) }
    }

    fun onSortBySelected(sort: TaskSearchSort) {
        _filterState.update { it.copy(sortBy = sort) }
    }

    fun resetFilters() {
        _filterState.update {
            SearchFilterState(query = it.query)
        }
    }

    fun clearAll() {
        _filterState.value = SearchFilterState()
    }

    fun openFilterSheet() {
        _filterState.update { it.copy(isFilterSheetOpen = true) }
    }

    fun closeFilterSheet() {
        _filterState.update { it.copy(isFilterSheetOpen = false) }
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
            categoryRepository: CategoryRepository,
            reminderScheduler: ReminderScheduler? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SearchViewModel(taskRepository, categoryRepository, reminderScheduler) as T
            }
        }
    }
}
