package com.personal.daymark.ui.today

import com.personal.daymark.core.common.DateFormatter
import com.personal.daymark.data.local.entity.TaskEntity
import com.personal.daymark.data.local.entity.TaskWithSubtasksAndCategory
import com.personal.daymark.data.repository.TaskRepository
import com.personal.daymark.ui.screens.today.TodayViewModel
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeTaskRepository : TaskRepository {
        val taskItems = MutableStateFlow<List<TaskWithSubtasksAndCategory>>(emptyList())

        override fun getAllTasksWithDetails(): Flow<List<TaskWithSubtasksAndCategory>> = taskItems.asStateFlow()
        override fun getTaskWithDetailsById(id: Long): Flow<TaskWithSubtasksAndCategory?> = flowOf(null)
        override fun getTasksForToday(startOfDay: Long, endOfDay: Long): Flow<List<TaskWithSubtasksAndCategory>> = taskItems.asStateFlow()
        override fun getTasksInDateRange(startDate: Long, endDate: Long): Flow<List<TaskWithSubtasksAndCategory>> = taskItems.asStateFlow()
        override fun getTasksByCategory(categoryId: Long): Flow<List<TaskWithSubtasksAndCategory>> = taskItems.asStateFlow()
        override fun getTasksByCompletion(isCompleted: Boolean): Flow<List<TaskWithSubtasksAndCategory>> = taskItems.asStateFlow()
        override fun searchTasks(query: String): Flow<List<TaskWithSubtasksAndCategory>> = taskItems.asStateFlow()

        override suspend fun createTask(task: TaskEntity, subtaskTitles: List<String>): Long {
            val id = (taskItems.value.size + 1).toLong()
            taskItems.value += TaskWithSubtasksAndCategory(task = task.copy(id = id))
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
        override suspend fun deleteTaskById(id: Long) {}
        override suspend fun setTaskCompleted(id: Long, isCompleted: Boolean) {
            val now = if (isCompleted) System.currentTimeMillis() else null
            taskItems.value = taskItems.value.map {
                if (it.task.id == id) {
                    it.copy(task = it.task.copy(isCompleted = isCompleted, completionTimestamp = now))
                } else it
            }
        }
        override suspend fun createSubtask(taskId: Long, title: String): Long = 1L
        override suspend fun setSubtaskCompleted(subtaskId: Long, isCompleted: Boolean) {}
        override suspend fun deleteSubtask(subtask: com.personal.daymark.data.local.entity.SubtaskEntity) {}
        override suspend fun getTaskById(id: Long): TaskEntity? = taskItems.value.find { it.task.id == id }?.task
        override suspend fun getPendingTasksWithDueDate(): List<TaskEntity> = taskItems.value.map { it.task }.filter { !it.isCompleted && it.dueDate != null }
        override suspend fun completeTaskAndGenerateNext(taskId: Long): TaskEntity? {
            val now = System.currentTimeMillis()
            taskItems.value = taskItems.value.map {
                if (it.task.id == taskId) {
                    it.copy(task = it.task.copy(isCompleted = true, completionTimestamp = now))
                } else it
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
    private lateinit var viewModel: TodayViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeTaskRepo = FakeTaskRepository()
        viewModel = TodayViewModel(fakeTaskRepo)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun progressRatioCalculatesCorrectlyWhenTasksCompleted() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val startOfToday = DateFormatter.getStartOfTodayMillis()
        val todayMidday = startOfToday + 3600000L

        // Task 1: Due today, not completed
        val id1 = fakeTaskRepo.createTask(TaskEntity(title = "Task 1", dueDate = todayMidday))
        // Task 2: Due today, completed today
        val id2 = fakeTaskRepo.createTask(TaskEntity(title = "Task 2", dueDate = todayMidday))
        fakeTaskRepo.setTaskCompleted(id2, true)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.todayTasks.size)
        assertEquals(1, state.completedTodayCount)
        assertEquals(2, state.totalTodayAndOverdueCount)
        assertEquals(0.5f, state.progressRatio, 0.01f)
    }

    @Test
    fun overdueTasksAreIdentifiedAndRemainInList() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val yesterday = DateFormatter.getStartOfTodayMillis() - 86400000L

        fakeTaskRepo.createTask(TaskEntity(title = "Overdue task", dueDate = yesterday, isCompleted = false))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.overdueTasks.size)
        assertEquals("Overdue task", state.overdueTasks[0].task.title)
    }

    @Test
    fun emptyStateHasZeroProgressRatio() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.todayTasks.size)
        assertEquals(0, state.overdueTasks.size)
        assertEquals(0f, state.progressRatio, 0.01f)
    }

    @Test
    fun deferTask_updatesDueDateAndRemovesFromToday() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val startOfToday = DateFormatter.getStartOfTodayMillis()
        val taskId = fakeTaskRepo.createTask(
            TaskEntity(
                title = "Task for Today",
                dueDate = startOfToday,
                isCompleted = false
            )
        )
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.todayTasks.size)

        val taskItem = viewModel.uiState.value.todayTasks.first()
        val tomorrowMillis = startOfToday + 86400000L

        viewModel.deferTask(
            taskItem = taskItem,
            targetDueDate = tomorrowMillis,
            confirmationMessage = "Deferred to Tomorrow"
        )
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.todayTasks.size)
    }
}
