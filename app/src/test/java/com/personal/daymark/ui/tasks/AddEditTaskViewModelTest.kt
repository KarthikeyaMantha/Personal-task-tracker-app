package com.personal.daymark.ui.tasks

import com.personal.daymark.data.local.entity.CategoryEntity
import com.personal.daymark.data.local.entity.TaskEntity
import com.personal.daymark.data.local.entity.TaskWithSubtasksAndCategory
import com.personal.daymark.data.repository.CategoryRepository
import com.personal.daymark.data.repository.TaskRepository
import com.personal.daymark.ui.screens.tasks.AddEditTaskEvent
import com.personal.daymark.ui.screens.tasks.AddEditTaskViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddEditTaskViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeCategoryRepository : CategoryRepository {
        override fun getAllCategories(): Flow<List<CategoryEntity>> = flowOf(emptyList())
        override fun getCategoryById(id: Long): Flow<CategoryEntity?> = flowOf(null)
        override suspend fun createCategory(name: String, colorHex: String, iconName: String?, sortOrder: Int): Long = 1L
        override suspend fun updateCategory(category: CategoryEntity) {}
        override suspend fun deleteCategory(category: CategoryEntity) {}
        override suspend fun deleteCategoryById(id: Long) {}
        override suspend fun seedDefaultCategoriesIfEmpty() {}
    }

    private class FakeTaskRepository : TaskRepository {
        val tasks = mutableListOf<TaskEntity>()
        val subtasks = mutableListOf<com.personal.daymark.data.local.entity.SubtaskEntity>()
        var lastInsertedTask: TaskEntity? = null
        var lastUpdatedTask: TaskEntity? = null

        override fun getAllTasksWithDetails(): Flow<List<TaskWithSubtasksAndCategory>> = flowOf(emptyList())
        override fun getTaskWithDetailsById(id: Long): Flow<TaskWithSubtasksAndCategory?> {
            val task = tasks.find { it.id == id }
            val taskSubtasks = subtasks.filter { it.taskId == id }
            return flowOf(task?.let { TaskWithSubtasksAndCategory(task = it, subtasks = taskSubtasks) })
        }
        override fun getTasksForToday(startOfDay: Long, endOfDay: Long): Flow<List<TaskWithSubtasksAndCategory>> = flowOf(emptyList())
        override fun getTasksInDateRange(startDate: Long, endDate: Long): Flow<List<TaskWithSubtasksAndCategory>> = flowOf(emptyList())
        override fun getTasksByCategory(categoryId: Long): Flow<List<TaskWithSubtasksAndCategory>> = flowOf(emptyList())
        override fun getTasksByCompletion(isCompleted: Boolean): Flow<List<TaskWithSubtasksAndCategory>> = flowOf(emptyList())
        override fun searchTasks(query: String): Flow<List<TaskWithSubtasksAndCategory>> = flowOf(emptyList())

        override suspend fun createTask(task: TaskEntity, subtaskTitles: List<String>): Long {
            val id = (tasks.size + 1).toLong()
            val created = task.copy(id = id)
            tasks.add(created)
            lastInsertedTask = created
            return id
        }

        override suspend fun updateTask(task: TaskEntity) {
            lastUpdatedTask = task
            tasks.removeIf { it.id == task.id }
            tasks.add(task)
        }

        override suspend fun deleteTask(task: TaskEntity) {}
        override suspend fun deleteTaskById(id: Long) {}
        override suspend fun setTaskCompleted(id: Long, isCompleted: Boolean) {}
        override suspend fun createSubtask(taskId: Long, title: String): Long = 1L
        override suspend fun setSubtaskCompleted(subtaskId: Long, isCompleted: Boolean) {}
        override suspend fun deleteSubtask(subtask: com.personal.daymark.data.local.entity.SubtaskEntity) {}
        override suspend fun getTaskById(id: Long): TaskEntity? = tasks.find { it.id == id }
        override suspend fun getPendingTasksWithDueDate(): List<TaskEntity> = tasks.filter { !it.isCompleted && it.dueDate != null }
        override suspend fun completeTaskAndGenerateNext(taskId: Long): TaskEntity? = null
        override suspend fun deleteTaskSeries(seriesId: String): List<Long> = emptyList()
        override suspend fun deleteTaskOccurrence(taskId: Long): TaskEntity? = null
        override suspend fun updateEntireSeries(seriesId: String, updatedTask: TaskEntity) {}
        override suspend fun getTasksBySeriesId(seriesId: String): List<TaskEntity> = emptyList()
        override fun getSubtasksForTask(taskId: Long): Flow<List<com.personal.daymark.data.local.entity.SubtaskEntity>> = flowOf(subtasks.filter { it.taskId == taskId })
        override suspend fun updateSubtask(subtask: com.personal.daymark.data.local.entity.SubtaskEntity) {
            subtasks.removeIf { it.id == subtask.id }
            subtasks.add(subtask)
        }
        override suspend fun syncSubtasks(taskId: Long, newSubtasks: List<com.personal.daymark.data.local.entity.SubtaskEntity>) {
            subtasks.removeIf { it.taskId == taskId }
            subtasks.addAll(newSubtasks)
        }
        override fun getCompletedTasksCountInRange(startMillis: Long, endMillis: Long): Flow<Int> = flowOf(0)
        override fun getPendingTasksCount(): Flow<Int> = flowOf(0)
        override fun getOverdueTasksCount(nowMillis: Long): Flow<Int> = flowOf(0)
        override fun getTotalTasksCount(): Flow<Int> = flowOf(0)
        override fun getCompletedTasksInRange(startMillis: Long, endMillis: Long): Flow<List<TaskEntity>> = flowOf(emptyList())
    }

    private lateinit var fakeTaskRepo: FakeTaskRepository
    private lateinit var fakeCategoryRepo: FakeCategoryRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeTaskRepo = FakeTaskRepository()
        fakeCategoryRepo = FakeCategoryRepository()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun saveTaskWithOnlyTitleSucceedsFast() = runTest {
        val viewModel = AddEditTaskViewModel(null, fakeTaskRepo, fakeCategoryRepo)

        viewModel.onTitleChanged("Quick Task")
        viewModel.saveTask()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isTitleError)
        assertEquals("Quick Task", fakeTaskRepo.lastInsertedTask?.title)
        assertEquals(0, fakeTaskRepo.lastInsertedTask?.priority)
    }

    @Test
    fun saveTaskWithBlankTitleShowsError() = runTest {
        val viewModel = AddEditTaskViewModel(null, fakeTaskRepo, fakeCategoryRepo)

        viewModel.onTitleChanged("   ")
        viewModel.saveTask()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isTitleError)
        assertEquals(null, fakeTaskRepo.lastInsertedTask)
    }

    @Test
    fun editExistingTaskLoadsAndUpdatesFields() = runTest {
        val existing = TaskEntity(id = 42L, title = "Original Title", priority = 1)
        fakeTaskRepo.tasks.add(existing)

        val viewModel = AddEditTaskViewModel(42L, fakeTaskRepo, fakeCategoryRepo)
        advanceUntilIdle()

        assertEquals("Original Title", viewModel.uiState.value.title)
        assertEquals(1, viewModel.uiState.value.priority)

        viewModel.onTitleChanged("Updated Title")
        viewModel.onPrioritySelected(3)
        viewModel.saveTask()
        advanceUntilIdle()

        assertEquals("Updated Title", fakeTaskRepo.lastUpdatedTask?.title)
        assertEquals(3, fakeTaskRepo.lastUpdatedTask?.priority)
    }

    @Test
    fun subtaskOperationsInAddEditViewModel() = runTest {
        val viewModel = AddEditTaskViewModel(null, fakeTaskRepo, fakeCategoryRepo)

        // 1. Add subtasks
        viewModel.onNewSubtaskInputChanged("Subtask 1")
        viewModel.addSubtask()
        viewModel.onNewSubtaskInputChanged("Subtask 2")
        viewModel.addSubtask()

        val stateWithSubtasks = viewModel.uiState.value
        assertEquals(2, stateWithSubtasks.subtasks.size)
        assertEquals("Subtask 1", stateWithSubtasks.subtasks[0].title)
        assertFalse(stateWithSubtasks.subtasks[0].isCompleted)
        assertEquals("", stateWithSubtasks.newSubtaskInput)

        // 2. Toggle subtask
        viewModel.toggleSubtask(0)
        assertTrue(viewModel.uiState.value.subtasks[0].isCompleted)

        // 3. Delete subtask
        viewModel.deleteSubtask(0)
        assertEquals(1, viewModel.uiState.value.subtasks.size)
        assertEquals("Subtask 2", viewModel.uiState.value.subtasks[0].title)

        // 4. Save task with subtasks
        viewModel.onTitleChanged("Task with Checklist")
        viewModel.saveTask()
        advanceUntilIdle()

        assertEquals("Task with Checklist", fakeTaskRepo.lastInsertedTask?.title)
    }
}
