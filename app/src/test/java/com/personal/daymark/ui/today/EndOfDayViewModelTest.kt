package com.personal.daymark.ui.today

import com.personal.daymark.core.common.DateFormatter
import com.personal.daymark.data.local.entity.TaskEntity
import com.personal.daymark.data.local.entity.TaskWithSubtasksAndCategory
import com.personal.daymark.data.repository.TaskRepository
import com.personal.daymark.ui.screens.today.EndOfDayTaskAction
import com.personal.daymark.ui.screens.today.EndOfDayViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EndOfDayViewModelTest {

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
        override suspend fun deleteTaskOccurrence(taskId: Long): TaskEntity? = null
        override suspend fun deleteTaskSeries(seriesId: String): List<Long> = emptyList()
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

    private lateinit var fakeRepository: FakeTaskRepository
    private lateinit var viewModel: EndOfDayViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeTaskRepository()
        viewModel = EndOfDayViewModel(taskRepository = fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun incompleteTasksOnOrBeforeTodayAreIncluded() = runTest(testDispatcher) {
        val collectJob = launch { viewModel.uiState.collect() }

        val today = DateFormatter.getStartOfTodayMillis() + 1000L
        val yesterday = DateFormatter.getStartOfTodayMillis() - 86400000L
        val nextWeek = DateFormatter.getStartOfTodayMillis() + 7 * 86400000L

        fakeRepository.taskItems.value = listOf(
            TaskWithSubtasksAndCategory(task = TaskEntity(id = 1, title = "Task Today", dueDate = today, isCompleted = false)),
            TaskWithSubtasksAndCategory(task = TaskEntity(id = 2, title = "Task Overdue", dueDate = yesterday, isCompleted = false)),
            TaskWithSubtasksAndCategory(task = TaskEntity(id = 3, title = "Completed Today", dueDate = today, isCompleted = true)),
            TaskWithSubtasksAndCategory(task = TaskEntity(id = 4, title = "Future Task", dueDate = nextWeek, isCompleted = false))
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.incompleteTasks.size)
        assertTrue(state.incompleteTasks.any { it.task.id == 1L })
        assertTrue(state.incompleteTasks.any { it.task.id == 2L })

        // Check that default action is MOVE_TO_TOMORROW
        assertEquals(EndOfDayTaskAction.MOVE_TO_TOMORROW, state.taskActions[1L])
        assertEquals(EndOfDayTaskAction.MOVE_TO_TOMORROW, state.taskActions[2L])

        collectJob.cancel()
    }

    @Test
    fun individualActionAndBulkActionUpdateState() = runTest(testDispatcher) {
        val collectJob = launch { viewModel.uiState.collect() }

        val today = DateFormatter.getStartOfTodayMillis() + 1000L
        fakeRepository.taskItems.value = listOf(
            TaskWithSubtasksAndCategory(task = TaskEntity(id = 1, title = "Task 1", dueDate = today, isCompleted = false)),
            TaskWithSubtasksAndCategory(task = TaskEntity(id = 2, title = "Task 2", dueDate = today, isCompleted = false))
        )
        advanceUntilIdle()

        // Set Task 1 to MOVE_TO_INBOX
        viewModel.setTaskAction(1L, EndOfDayTaskAction.MOVE_TO_INBOX)
        advanceUntilIdle()
        assertEquals(EndOfDayTaskAction.MOVE_TO_INBOX, viewModel.uiState.value.taskActions[1L])
        assertEquals(EndOfDayTaskAction.MOVE_TO_TOMORROW, viewModel.uiState.value.taskActions[2L])

        // Bulk action to MARK_COMPLETE
        viewModel.applyBulkAction(EndOfDayTaskAction.MARK_COMPLETE)
        advanceUntilIdle()
        assertEquals(EndOfDayTaskAction.MARK_COMPLETE, viewModel.uiState.value.taskActions[1L])
        assertEquals(EndOfDayTaskAction.MARK_COMPLETE, viewModel.uiState.value.taskActions[2L])

        collectJob.cancel()
    }

    @Test
    fun confirmShutdownUpdatesDatabaseCorrectly() = runTest(testDispatcher) {
        val collectJob = launch { viewModel.uiState.collect() }

        val today = DateFormatter.getStartOfTodayMillis() + 1000L
        fakeRepository.taskItems.value = listOf(
            TaskWithSubtasksAndCategory(task = TaskEntity(id = 1, title = "Task Tomorrow", dueDate = today, isCompleted = false)),
            TaskWithSubtasksAndCategory(task = TaskEntity(id = 2, title = "Task Inbox", dueDate = today, dueTime = 600, isCompleted = false)),
            TaskWithSubtasksAndCategory(task = TaskEntity(id = 3, title = "Task Complete", dueDate = today, isCompleted = false))
        )
        advanceUntilIdle()

        viewModel.setTaskAction(1L, EndOfDayTaskAction.MOVE_TO_TOMORROW)
        viewModel.setTaskAction(2L, EndOfDayTaskAction.MOVE_TO_INBOX)
        viewModel.setTaskAction(3L, EndOfDayTaskAction.MARK_COMPLETE)
        advanceUntilIdle()

        var onFinishedCalled = false
        viewModel.confirmShutdown {
            onFinishedCalled = true
        }
        advanceUntilIdle()

        assertTrue(onFinishedCalled)
        assertTrue(viewModel.uiState.value.isComplete)
        assertEquals("Day closed. Have a great evening!", viewModel.uiState.value.summaryMessage)

        // Verify task 1 moved to tomorrow
        val task1 = fakeRepository.taskItems.value.first { it.task.id == 1L }.task
        val tomorrowStart = DateFormatter.getStartOfTodayMillis() + 24 * 60 * 60 * 1000L
        assertEquals(tomorrowStart, task1.dueDate)

        // Verify task 2 moved to inbox (dueDate = null, dueTime = null)
        val task2 = fakeRepository.taskItems.value.first { it.task.id == 2L }.task
        assertNull(task2.dueDate)
        assertNull(task2.dueTime)

        // Verify task 3 marked complete
        val task3 = fakeRepository.taskItems.value.first { it.task.id == 3L }.task
        assertTrue(task3.isCompleted)

        collectJob.cancel()
    }
}
