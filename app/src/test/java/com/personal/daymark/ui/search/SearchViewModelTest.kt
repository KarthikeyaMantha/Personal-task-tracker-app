package com.personal.daymark.ui.search

import com.personal.daymark.core.common.DateFormatter
import com.personal.daymark.data.local.entity.CategoryEntity
import com.personal.daymark.data.local.entity.TaskEntity
import com.personal.daymark.data.local.entity.TaskWithSubtasksAndCategory
import com.personal.daymark.data.repository.CategoryRepository
import com.personal.daymark.data.repository.TaskRepository
import com.personal.daymark.ui.screens.search.SearchViewModel
import com.personal.daymark.ui.screens.search.TaskCompletionFilter
import com.personal.daymark.ui.screens.search.TaskDateRangeFilter
import com.personal.daymark.ui.screens.search.TaskSearchSort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeCategoryRepository : CategoryRepository {
        val categories = MutableStateFlow<List<CategoryEntity>>(
            listOf(
                CategoryEntity(id = 1L, name = "Work", colorHex = "#1B4332"),
                CategoryEntity(id = 2L, name = "Personal", colorHex = "#40916C")
            )
        )
        override fun getAllCategories(): Flow<List<CategoryEntity>> = categories.asStateFlow()
        override fun getCategoryById(id: Long): Flow<CategoryEntity?> = flowOf(categories.value.find { it.id == id })
        override suspend fun createCategory(name: String, colorHex: String, iconName: String?, sortOrder: Int): Long = 1L
        override suspend fun updateCategory(category: CategoryEntity) {}
        override suspend fun deleteCategory(category: CategoryEntity) {}
        override suspend fun deleteCategoryById(id: Long) {}
        override suspend fun seedDefaultCategoriesIfEmpty() {}
    }

    private class FakeTaskRepository : TaskRepository {
        val taskItems = MutableStateFlow<List<TaskWithSubtasksAndCategory>>(emptyList())

        override fun getAllTasksWithDetails(): Flow<List<TaskWithSubtasksAndCategory>> = taskItems.asStateFlow()
        override fun getTaskWithDetailsById(id: Long): Flow<TaskWithSubtasksAndCategory?> = flowOf(null)
        override fun getTasksForToday(startOfDay: Long, endOfDay: Long): Flow<List<TaskWithSubtasksAndCategory>> = taskItems.asStateFlow()
        override fun getTasksInDateRange(startDate: Long, endDate: Long): Flow<List<TaskWithSubtasksAndCategory>> = taskItems.asStateFlow()
        override fun getTasksByCategory(categoryId: Long): Flow<List<TaskWithSubtasksAndCategory>> = taskItems.asStateFlow()
        override fun getTasksByCompletion(isCompleted: Boolean): Flow<List<TaskWithSubtasksAndCategory>> = taskItems.asStateFlow()
        override fun searchTasks(query: String): Flow<List<TaskWithSubtasksAndCategory>> = taskItems.asStateFlow()

        override suspend fun createTask(task: TaskEntity, subtaskTitles: List<String>): Long = 1L
        override suspend fun updateTask(task: TaskEntity) {}
        override suspend fun deleteTask(task: TaskEntity) {
            taskItems.value = taskItems.value.filterNot { it.task.id == task.id }
        }
        override suspend fun deleteTaskById(id: Long) {}
        override suspend fun setTaskCompleted(id: Long, isCompleted: Boolean) {
            taskItems.value = taskItems.value.map {
                if (it.task.id == id) it.copy(task = it.task.copy(isCompleted = isCompleted)) else it
            }
        }
        override suspend fun createSubtask(taskId: Long, title: String): Long = 1L
        override suspend fun setSubtaskCompleted(subtaskId: Long, isCompleted: Boolean) {}
        override suspend fun updateSubtask(subtask: com.personal.daymark.data.local.entity.SubtaskEntity) {}
        override suspend fun deleteSubtask(subtask: com.personal.daymark.data.local.entity.SubtaskEntity) {}
        override fun getSubtasksForTask(taskId: Long): Flow<List<com.personal.daymark.data.local.entity.SubtaskEntity>> = flowOf(emptyList())
        override suspend fun syncSubtasks(taskId: Long, subtasks: List<com.personal.daymark.data.local.entity.SubtaskEntity>) {}
        override suspend fun getTaskById(id: Long): TaskEntity? = null
        override suspend fun getPendingTasksWithDueDate(): List<TaskEntity> = emptyList()
        override suspend fun completeTaskAndGenerateNext(taskId: Long): TaskEntity? = null
        override suspend fun deleteTaskSeries(seriesId: String): List<Long> = emptyList()
        override suspend fun deleteTaskOccurrence(taskId: Long): TaskEntity? = null
        override suspend fun updateEntireSeries(seriesId: String, updatedTask: TaskEntity) {}
        override suspend fun getTasksBySeriesId(seriesId: String): List<TaskEntity> = emptyList()
        override fun getCompletedTasksCountInRange(startMillis: Long, endMillis: Long): Flow<Int> = flowOf(0)
        override fun getPendingTasksCount(): Flow<Int> = flowOf(0)
        override fun getOverdueTasksCount(nowMillis: Long): Flow<Int> = flowOf(0)
        override fun getTotalTasksCount(): Flow<Int> = flowOf(0)
        override fun getCompletedTasksInRange(startMillis: Long, endMillis: Long): Flow<List<TaskEntity>> = flowOf(emptyList())
    }

    private lateinit var fakeTaskRepo: FakeTaskRepository
    private lateinit var fakeCategoryRepo: FakeCategoryRepository
    private lateinit var viewModel: SearchViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeTaskRepo = FakeTaskRepository()
        fakeCategoryRepo = FakeCategoryRepository()
        viewModel = SearchViewModel(fakeTaskRepo, fakeCategoryRepo)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun searchByTitle_returnsMatchingTasks() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        fakeTaskRepo.taskItems.value = listOf(
            TaskWithSubtasksAndCategory(task = TaskEntity(id = 1L, title = "Quarterly financial report")),
            TaskWithSubtasksAndCategory(task = TaskEntity(id = 2L, title = "Grocery shopping")),
            TaskWithSubtasksAndCategory(task = TaskEntity(id = 3L, title = "Bug report investigation"))
        )
        advanceUntilIdle()

        viewModel.onQueryChanged("report")
        advanceUntilIdle()

        val results = viewModel.uiState.value.searchResults
        assertEquals(2, results.size)
        assertTrue(results.all { it.task.title.contains("report", ignoreCase = true) })
    }

    @Test
    fun searchByDescriptionNotes_returnsMatchingTasks() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        fakeTaskRepo.taskItems.value = listOf(
            TaskWithSubtasksAndCategory(task = TaskEntity(id = 1L, title = "Client call", description = "Discuss contract terms and NDA requirements")),
            TaskWithSubtasksAndCategory(task = TaskEntity(id = 2L, title = "Weekly sync", description = "Roadmap updates"))
        )
        advanceUntilIdle()

        viewModel.onQueryChanged("contract")
        advanceUntilIdle()

        val results = viewModel.uiState.value.searchResults
        assertEquals(1, results.size)
        assertEquals("Client call", results[0].task.title)
    }

    @Test
    fun searchIncludesCompletedAndHistoricalTasks() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        fakeTaskRepo.taskItems.value = listOf(
            TaskWithSubtasksAndCategory(task = TaskEntity(id = 1L, title = "Archive old tax records", isCompleted = true)),
            TaskWithSubtasksAndCategory(task = TaskEntity(id = 2L, title = "Prepare new tax filings", isCompleted = false))
        )
        advanceUntilIdle()

        viewModel.onQueryChanged("tax")
        advanceUntilIdle()

        val results = viewModel.uiState.value.searchResults
        assertEquals(2, results.size)
        assertTrue(results.any { it.task.isCompleted })
        assertTrue(results.any { !it.task.isCompleted })
    }

    @Test
    fun combinedFilters_categoryPriorityAndStatus() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val workCategory = CategoryEntity(id = 1L, name = "Work", colorHex = "#1B4332")
        val personalCategory = CategoryEntity(id = 2L, name = "Personal", colorHex = "#40916C")

        fakeTaskRepo.taskItems.value = listOf(
            // Target: Work + High priority (3) + Active + matches "design"
            TaskWithSubtasksAndCategory(
                task = TaskEntity(id = 1L, title = "System architecture design", categoryId = 1L, priority = 3, isCompleted = false),
                category = workCategory
            ),
            // Wrong priority (Low = 1)
            TaskWithSubtasksAndCategory(
                task = TaskEntity(id = 2L, title = "Icon design review", categoryId = 1L, priority = 1, isCompleted = false),
                category = workCategory
            ),
            // Wrong category (Personal = 2)
            TaskWithSubtasksAndCategory(
                task = TaskEntity(id = 3L, title = "Garden design plan", categoryId = 2L, priority = 3, isCompleted = false),
                category = personalCategory
            ),
            // Completed
            TaskWithSubtasksAndCategory(
                task = TaskEntity(id = 4L, title = "Database design schema", categoryId = 1L, priority = 3, isCompleted = true),
                category = workCategory
            )
        )
        advanceUntilIdle()

        // Apply combined filters
        viewModel.onQueryChanged("design")
        viewModel.onCategorySelected(1L)
        viewModel.onPrioritySelected(3)
        viewModel.onCompletionFilterSelected(TaskCompletionFilter.ACTIVE)
        advanceUntilIdle()

        val results = viewModel.uiState.value.searchResults
        assertEquals(1, results.size)
        assertEquals("System architecture design", results[0].task.title)
        assertEquals(3, viewModel.uiState.value.filterState.activeFilterCount)
    }

    @Test
    fun dateRangeFilter_overdueAndToday() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val startOfToday = DateFormatter.getStartOfTodayMillis()
        val pastDate = startOfToday - 86400000L * 2
        val futureDate = startOfToday + 86400000L * 5

        fakeTaskRepo.taskItems.value = listOf(
            TaskWithSubtasksAndCategory(task = TaskEntity(id = 1L, title = "Late report", dueDate = pastDate, isCompleted = false)),
            TaskWithSubtasksAndCategory(task = TaskEntity(id = 2L, title = "Today task", dueDate = startOfToday + 3600000L, isCompleted = false)),
            TaskWithSubtasksAndCategory(task = TaskEntity(id = 3L, title = "Future sprint", dueDate = futureDate, isCompleted = false))
        )
        advanceUntilIdle()

        viewModel.onDateRangeSelected(TaskDateRangeFilter.OVERDUE)
        advanceUntilIdle()

        var results = viewModel.uiState.value.searchResults
        assertEquals(1, results.size)
        assertEquals("Late report", results[0].task.title)

        viewModel.onDateRangeSelected(TaskDateRangeFilter.TODAY)
        advanceUntilIdle()

        results = viewModel.uiState.value.searchResults
        assertEquals(1, results.size)
        assertEquals("Today task", results[0].task.title)
    }

    @Test
    fun sortingTasks_byPriorityAndDueDate() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        fakeTaskRepo.taskItems.value = listOf(
            TaskWithSubtasksAndCategory(task = TaskEntity(id = 1L, title = "Low priority task", priority = 1)),
            TaskWithSubtasksAndCategory(task = TaskEntity(id = 2L, title = "Urgent task", priority = 4)),
            TaskWithSubtasksAndCategory(task = TaskEntity(id = 3L, title = "High priority task", priority = 3))
        )
        advanceUntilIdle()

        viewModel.onSortBySelected(TaskSearchSort.PRIORITY)
        advanceUntilIdle()

        val results = viewModel.uiState.value.searchResults
        assertEquals(3, results.size)
        assertEquals("Urgent task", results[0].task.title)
        assertEquals("High priority task", results[1].task.title)
        assertEquals("Low priority task", results[2].task.title)
    }

    @Test
    fun resetFilters_clearsCriteriaPreservingQuery() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.onQueryChanged("project")
        viewModel.onPrioritySelected(3)
        viewModel.onCategorySelected(1L)
        viewModel.onCompletionFilterSelected(TaskCompletionFilter.COMPLETED)
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.filterState.activeFilterCount)

        viewModel.resetFilters()
        advanceUntilIdle()

        assertEquals("project", viewModel.uiState.value.filterState.query)
        assertEquals(0, viewModel.uiState.value.filterState.activeFilterCount)
        assertEquals(TaskCompletionFilter.ALL, viewModel.uiState.value.filterState.completionFilter)
    }
}
