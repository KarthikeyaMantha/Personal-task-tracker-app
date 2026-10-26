package com.personal.daymark.ui.tasks

import com.personal.daymark.core.common.DateFormatter
import com.personal.daymark.data.local.entity.CategoryEntity
import com.personal.daymark.data.local.entity.TaskEntity
import com.personal.daymark.data.local.entity.TaskWithSubtasksAndCategory
import com.personal.daymark.data.repository.CategoryRepository
import com.personal.daymark.data.repository.TaskRepository
import com.personal.daymark.ui.screens.tasks.TaskListViewModel
import com.personal.daymark.ui.screens.tasks.TaskSortOption
import com.personal.daymark.ui.screens.tasks.TaskViewFilter
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

@OptIn(ExperimentalCoroutinesApi::class)
class TaskListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeCategoryRepository : CategoryRepository {
        private val categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
        override fun getAllCategories(): Flow<List<CategoryEntity>> = categories.asStateFlow()
        override fun getCategoryById(id: Long): Flow<CategoryEntity?> = flowOf(categories.value.find { it.id == id })
        override suspend fun createCategory(name: String, colorHex: String, iconName: String?, sortOrder: Int): Long {
            val id = (categories.value.size + 1).toLong()
            categories.value += CategoryEntity(id = id, name = name, colorHex = colorHex)
            return id
        }
        override suspend fun updateCategory(category: CategoryEntity) {}
        override suspend fun deleteCategory(category: CategoryEntity) {}
        override suspend fun deleteCategoryById(id: Long) {}
        override suspend fun seedDefaultCategoriesIfEmpty() {}
    }

    private class FakeTaskRepository : TaskRepository {
        val taskItems = MutableStateFlow<List<TaskWithSubtasksAndCategory>>(emptyList())
        var lastCompletedId: Long? = null
        var lastCompletedState: Boolean? = null

        override fun getAllTasksWithDetails(): Flow<List<TaskWithSubtasksAndCategory>> = taskItems.asStateFlow()
        override fun getTaskWithDetailsById(id: Long): Flow<TaskWithSubtasksAndCategory?> =
            flowOf(taskItems.value.find { it.task.id == id })
        override fun getTasksForToday(startOfDay: Long, endOfDay: Long): Flow<List<TaskWithSubtasksAndCategory>> = taskItems.asStateFlow()
        override fun getTasksInDateRange(startDate: Long, endDate: Long): Flow<List<TaskWithSubtasksAndCategory>> = taskItems.asStateFlow()
        override fun getTasksByCategory(categoryId: Long): Flow<List<TaskWithSubtasksAndCategory>> = taskItems.asStateFlow()
        override fun getTasksByCompletion(isCompleted: Boolean): Flow<List<TaskWithSubtasksAndCategory>> = taskItems.asStateFlow()
        override fun searchTasks(query: String): Flow<List<TaskWithSubtasksAndCategory>> = taskItems.asStateFlow()

        override suspend fun createTask(task: TaskEntity, subtaskTitles: List<String>): Long {
            val id = if (task.id > 0) task.id else (taskItems.value.size + 1).toLong()
            val newTask = task.copy(id = id)
            taskItems.value += TaskWithSubtasksAndCategory(task = newTask)
            return id
        }

        override suspend fun updateTask(task: TaskEntity) {
            taskItems.value = taskItems.value.map {
                if (it.task.id == task.id) it.copy(task = task) else it
            }
        }

        override suspend fun deleteTask(task: TaskEntity) {
            taskItems.value = taskItems.value.filterNot { it.task.id == task.id }
        }

        override suspend fun deleteTaskById(id: Long) {
            taskItems.value = taskItems.value.filterNot { it.task.id == id }
        }

        override suspend fun setTaskCompleted(id: Long, isCompleted: Boolean) {
            lastCompletedId = id
            lastCompletedState = isCompleted
            taskItems.value = taskItems.value.map {
                if (it.task.id == id) it.copy(task = it.task.copy(isCompleted = isCompleted)) else it
            }
        }

        override suspend fun createSubtask(taskId: Long, title: String): Long = 1L
        override suspend fun setSubtaskCompleted(subtaskId: Long, isCompleted: Boolean) {}
        override suspend fun deleteSubtask(subtask: com.personal.daymark.data.local.entity.SubtaskEntity) {}
        override suspend fun getTaskById(id: Long): TaskEntity? = taskItems.value.find { it.task.id == id }?.task
        override suspend fun getPendingTasksWithDueDate(): List<TaskEntity> = taskItems.value.map { it.task }.filter { !it.isCompleted && it.dueDate != null }
        override suspend fun completeTaskAndGenerateNext(taskId: Long): TaskEntity? {
            lastCompletedId = taskId
            lastCompletedState = true
            taskItems.value = taskItems.value.map {
                if (it.task.id == taskId) it.copy(task = it.task.copy(isCompleted = true)) else it
            }
            return null
        }
        override suspend fun deleteTaskSeries(seriesId: String): List<Long> = emptyList()
        override suspend fun deleteTaskOccurrence(taskId: Long): TaskEntity? = null
        override suspend fun updateEntireSeries(seriesId: String, updatedTask: TaskEntity) {}
        override suspend fun getTasksBySeriesId(seriesId: String): List<TaskEntity> = emptyList()
        override fun getSubtasksForTask(taskId: Long): Flow<List<com.personal.daymark.data.local.entity.SubtaskEntity>> = flowOf(emptyList())
        override suspend fun updateSubtask(subtask: com.personal.daymark.data.local.entity.SubtaskEntity) {}
        override suspend fun syncSubtasks(taskId: Long, subtasks: List<com.personal.daymark.data.local.entity.SubtaskEntity>) {}
        override fun getCompletedTasksCountInRange(startMillis: Long, endMillis: Long): Flow<Int> = flowOf(0)
        override fun getPendingTasksCount(): Flow<Int> = flowOf(0)
        override fun getOverdueTasksCount(nowMillis: Long): Flow<Int> = flowOf(0)
        override fun getTotalTasksCount(): Flow<Int> = flowOf(0)
        override fun getCompletedTasksInRange(startMillis: Long, endMillis: Long): Flow<List<TaskEntity>> = flowOf(emptyList())
    }

    private lateinit var fakeTaskRepo: FakeTaskRepository
    private lateinit var fakeCategoryRepo: FakeCategoryRepository
    private lateinit var viewModel: TaskListViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeTaskRepo = FakeTaskRepository()
        fakeCategoryRepo = FakeCategoryRepository()
        viewModel = TaskListViewModel(fakeTaskRepo, fakeCategoryRepo)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialTasksAreLoadedFromRepository() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        fakeTaskRepo.createTask(TaskEntity(title = "Morning Meditation"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.tasks.size)
        assertEquals("Morning Meditation", state.tasks[0].task.title)
    }

    @Test
    fun toggleTaskCompletionUpdatesState() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val taskId = fakeTaskRepo.createTask(TaskEntity(title = "Exercise"))
        advanceUntilIdle()

        val taskItem = viewModel.uiState.value.tasks[0]
        viewModel.toggleTaskCompletion(taskItem)
        advanceUntilIdle()

        assertEquals(taskId, fakeTaskRepo.lastCompletedId)
        assertTrue(fakeTaskRepo.lastCompletedState == true)
    }

    @Test
    fun deleteTaskAndUndoRestoresTask() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        fakeTaskRepo.createTask(TaskEntity(title = "Task to Undo"))
        advanceUntilIdle()

        val taskItem = viewModel.uiState.value.tasks[0]
        viewModel.deleteTask(taskItem)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.tasks.isEmpty())

        // Trigger undo
        viewModel.undoDelete()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.tasks.size)
        assertEquals("Task to Undo", viewModel.uiState.value.tasks[0].task.title)
    }

    @Test
    fun inboxViewFiltersOutTasksWithDueDate() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val today = DateFormatter.getStartOfTodayMillis()
        // Unscheduled task (Inbox)
        fakeTaskRepo.createTask(TaskEntity(title = "Unscheduled task", dueDate = null))
        // Scheduled task
        fakeTaskRepo.createTask(TaskEntity(title = "Scheduled task", dueDate = today))
        advanceUntilIdle()

        viewModel.onViewFilterSelected(TaskViewFilter.INBOX)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.tasks.size)
        assertEquals("Unscheduled task", state.tasks[0].task.title)
    }

    @Test
    fun overdueViewKeepsOverdueTasksAccessible() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val pastDue = DateFormatter.getStartOfTodayMillis() - 86400000L * 2
        fakeTaskRepo.createTask(TaskEntity(title = "Overdue task", dueDate = pastDue, isCompleted = false))
        fakeTaskRepo.createTask(TaskEntity(title = "Completed past task", dueDate = pastDue, isCompleted = true))
        advanceUntilIdle()

        viewModel.onViewFilterSelected(TaskViewFilter.OVERDUE)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.tasks.size)
        assertEquals("Overdue task", state.tasks[0].task.title)
    }

    @Test
    fun upcomingViewFiltersFutureTasks() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val futureDue = DateFormatter.getEndOfTodayMillis() + 86400000L
        fakeTaskRepo.createTask(TaskEntity(title = "Future task", dueDate = futureDue, isCompleted = false))
        fakeTaskRepo.createTask(TaskEntity(title = "Today task", dueDate = DateFormatter.getStartOfTodayMillis()))
        advanceUntilIdle()

        viewModel.onViewFilterSelected(TaskViewFilter.UPCOMING)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.tasks.size)
        assertEquals("Future task", state.tasks[0].task.title)
    }

    @Test
    fun sortingByPriorityOrdersUrgentFirst() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        fakeTaskRepo.createTask(TaskEntity(title = "Low Priority", priority = 1))
        fakeTaskRepo.createTask(TaskEntity(title = "Urgent Priority", priority = 4))
        fakeTaskRepo.createTask(TaskEntity(title = "Medium Priority", priority = 2))
        advanceUntilIdle()

        viewModel.onSortOptionSelected(TaskSortOption.PRIORITY)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Urgent Priority", state.tasks[0].task.title)
        assertEquals("Medium Priority", state.tasks[1].task.title)
        assertEquals("Low Priority", state.tasks[2].task.title)
    }
}
