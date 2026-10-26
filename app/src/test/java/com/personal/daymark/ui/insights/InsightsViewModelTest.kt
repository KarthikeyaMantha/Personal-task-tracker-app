package com.personal.daymark.ui.insights

import com.personal.daymark.data.local.entity.FocusSessionEntity
import com.personal.daymark.data.local.entity.HabitEntity
import com.personal.daymark.data.local.entity.HabitLogEntity
import com.personal.daymark.data.local.entity.HabitWithLogs
import com.personal.daymark.data.local.entity.SubtaskEntity
import com.personal.daymark.data.local.entity.TaskEntity
import com.personal.daymark.data.local.entity.TaskWithSubtasksAndCategory
import com.personal.daymark.data.repository.FocusRepository
import com.personal.daymark.data.repository.HabitRepository
import com.personal.daymark.data.repository.TaskRepository
import com.personal.daymark.ui.screens.insights.InsightsUiState
import com.personal.daymark.ui.screens.insights.InsightsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeTaskRepository : TaskRepository {
        val completedInRangeCount = MutableStateFlow(0)
        val pendingCount = MutableStateFlow(0)
        val overdueCount = MutableStateFlow(0)
        val totalCount = MutableStateFlow(0)
        val completedInRangeList = MutableStateFlow<List<TaskEntity>>(emptyList())

        override fun getAllTasksWithDetails(): Flow<List<TaskWithSubtasksAndCategory>> = flowOf(emptyList())
        override fun getTaskWithDetailsById(id: Long): Flow<TaskWithSubtasksAndCategory?> = flowOf(null)
        override fun getTasksForToday(startOfDay: Long, endOfDay: Long): Flow<List<TaskWithSubtasksAndCategory>> = flowOf(emptyList())
        override fun getTasksInDateRange(startDate: Long, endDate: Long): Flow<List<TaskWithSubtasksAndCategory>> = flowOf(emptyList())
        override fun getTasksByCategory(categoryId: Long): Flow<List<TaskWithSubtasksAndCategory>> = flowOf(emptyList())
        override fun getTasksByCompletion(isCompleted: Boolean): Flow<List<TaskWithSubtasksAndCategory>> = flowOf(emptyList())
        override fun searchTasks(query: String): Flow<List<TaskWithSubtasksAndCategory>> = flowOf(emptyList())
        override suspend fun createTask(task: TaskEntity, subtaskTitles: List<String>): Long = 1L
        override suspend fun updateTask(task: TaskEntity) {}
        override suspend fun deleteTask(task: TaskEntity) {}
        override suspend fun deleteTaskById(id: Long) {}
        override suspend fun setTaskCompleted(id: Long, isCompleted: Boolean) {}
        override suspend fun createSubtask(taskId: Long, title: String): Long = 1L
        override suspend fun setSubtaskCompleted(subtaskId: Long, isCompleted: Boolean) {}
        override suspend fun updateSubtask(subtask: SubtaskEntity) {}
        override suspend fun deleteSubtask(subtask: SubtaskEntity) {}
        override fun getSubtasksForTask(taskId: Long): Flow<List<SubtaskEntity>> = flowOf(emptyList())
        override suspend fun syncSubtasks(taskId: Long, subtasks: List<SubtaskEntity>) {}
        override suspend fun getTaskById(id: Long): TaskEntity? = null
        override suspend fun getPendingTasksWithDueDate(): List<TaskEntity> = emptyList()
        override suspend fun completeTaskAndGenerateNext(taskId: Long): TaskEntity? = null
        override suspend fun deleteTaskSeries(seriesId: String): List<Long> = emptyList()
        override suspend fun deleteTaskOccurrence(taskId: Long): TaskEntity? = null
        override suspend fun updateEntireSeries(seriesId: String, updatedTask: TaskEntity) {}
        override suspend fun getTasksBySeriesId(seriesId: String): List<TaskEntity> = emptyList()
        override fun getCompletedTasksCountInRange(startMillis: Long, endMillis: Long): Flow<Int> = completedInRangeCount.asStateFlow()
        override fun getPendingTasksCount(): Flow<Int> = pendingCount.asStateFlow()
        override fun getOverdueTasksCount(nowMillis: Long): Flow<Int> = overdueCount.asStateFlow()
        override fun getTotalTasksCount(): Flow<Int> = totalCount.asStateFlow()
        override fun getCompletedTasksInRange(startMillis: Long, endMillis: Long): Flow<List<TaskEntity>> = completedInRangeList.asStateFlow()
    }

    private class FakeFocusRepository : FocusRepository {
        val focusMinutesFlow = MutableStateFlow<Int?>(0)
        val totalSessionsFlow = MutableStateFlow(0)
        val sessionsInRangeFlow = MutableStateFlow<List<FocusSessionEntity>>(emptyList())

        override fun getAllSessions(): Flow<List<FocusSessionEntity>> = flowOf(emptyList())
        override fun getSessionsForTask(taskId: Long): Flow<List<FocusSessionEntity>> = flowOf(emptyList())
        override fun getTotalDurationMinutes(startDate: Long, endDate: Long): Flow<Int?> = focusMinutesFlow.asStateFlow()
        override fun getFocusMinutesByType(startDate: Long, endDate: Long, sessionType: String): Flow<Int?> = focusMinutesFlow.asStateFlow()
        override fun getFocusSessionsInRange(startDate: Long, endDate: Long): Flow<List<FocusSessionEntity>> = sessionsInRangeFlow.asStateFlow()
        override fun getTotalSessionsCount(sessionType: String): Flow<Int> = totalSessionsFlow.asStateFlow()
        override suspend fun recordFocusSession(
            taskId: Long?,
            durationMinutes: Int,
            plannedDurationMinutes: Int,
            sessionType: String,
            startedAt: Long,
            completedAt: Long?,
            notes: String?
        ): Long = 1L
        override suspend fun deleteSession(session: FocusSessionEntity) {}
    }

    private class FakeHabitRepository : HabitRepository {
        val activeCountFlow = MutableStateFlow(0)
        val habitsWithLogsFlow = MutableStateFlow<List<HabitWithLogs>>(emptyList())

        override fun getActiveHabitsWithLogs(): Flow<List<HabitWithLogs>> = habitsWithLogsFlow.asStateFlow()
        override fun getAllHabitsWithLogs(): Flow<List<HabitWithLogs>> = habitsWithLogsFlow.asStateFlow()
        override fun getHabitWithLogsById(id: Long): Flow<HabitWithLogs?> = flowOf(null)
        override fun getLogsForHabit(habitId: Long): Flow<List<HabitLogEntity>> = flowOf(emptyList())
        override suspend fun createHabit(name: String, note: String?, frequency: String, targetWeekdays: Int, colorHex: String, categoryId: Long?): Long = 1L
        override suspend fun updateHabit(habit: HabitEntity) {}
        override suspend fun deleteHabit(habit: HabitEntity) {}
        override suspend fun deleteHabitById(id: Long) {}
        override suspend fun setHabitCompleted(habitId: Long, date: Long, isCompleted: Boolean) {}
        override suspend fun setHabitCompletedForDate(habitId: Long, localDate: LocalDate, isCompleted: Boolean) {}
        override suspend fun toggleHabitCompletion(habitId: Long, localDate: LocalDate): Boolean = true
        override fun getActiveHabitsCount(): Flow<Int> = activeCountFlow.asStateFlow()
        override fun getHabitCompletionsCountInRange(startDateString: String, endDateString: String): Flow<Int> = flowOf(0)
        override fun getAllLogsInRange(startDateString: String, endDateString: String): Flow<List<HabitLogEntity>> = flowOf(emptyList())
    }

    private lateinit var fakeTaskRepo: FakeTaskRepository
    private lateinit var fakeFocusRepo: FakeFocusRepository
    private lateinit var fakeHabitRepo: FakeHabitRepository
    private lateinit var viewModel: InsightsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeTaskRepo = FakeTaskRepository()
        fakeFocusRepo = FakeFocusRepository()
        fakeHabitRepo = FakeHabitRepository()
        viewModel = InsightsViewModel(fakeTaskRepo, fakeFocusRepo, fakeHabitRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_emptyRepositories_emitsSuccessWithHasAnyDataFalse() = runTest {
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state is InsightsUiState.Success)
        val summary = (state as InsightsUiState.Success).summary
        assertFalse(summary.hasAnyData)
        assertEquals(0, summary.tasksCompletedThisWeek)
        assertEquals(0, summary.focusMinutesThisWeek)
        assertEquals(0, summary.activeHabitsCount)
    }

    @Test
    fun populatedData_emitsAccurateAggregations() = runTest {
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val now = System.currentTimeMillis()
        val mockTasks = (1..5).map {
            TaskEntity(id = it.toLong(), title = "Task $it", isCompleted = true, completionTimestamp = now)
        }
        fakeTaskRepo.completedInRangeList.value = mockTasks
        fakeTaskRepo.pendingCount.value = 5
        fakeTaskRepo.overdueCount.value = 1
        fakeTaskRepo.totalCount.value = 20

        val mockSessions = listOf(
            FocusSessionEntity(id = 1L, durationMinutes = 120, sessionType = "FOCUS", startedAt = now),
            FocusSessionEntity(id = 2L, durationMinutes = 120, sessionType = "FOCUS", startedAt = now)
        )
        fakeFocusRepo.sessionsInRangeFlow.value = mockSessions
        fakeFocusRepo.totalSessionsFlow.value = 2

        fakeHabitRepo.activeCountFlow.value = 3

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is InsightsUiState.Success)
        val summary = (state as InsightsUiState.Success).summary

        assertTrue(summary.hasAnyData)
        assertEquals(5, summary.tasksCompletedThisWeek)
        assertEquals(5, summary.totalOutstandingTasks)
        assertEquals(1, summary.totalOverdueTasks)
        assertEquals(240, summary.focusMinutesThisWeek)
        assertEquals(2, summary.totalFocusSessions)
        assertEquals(3, summary.activeHabitsCount)
    }
}
