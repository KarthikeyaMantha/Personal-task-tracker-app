package com.personal.daymark.ui.calendar

import com.personal.daymark.data.local.entity.CategoryEntity
import com.personal.daymark.data.local.entity.TaskEntity
import com.personal.daymark.data.local.entity.TaskWithSubtasksAndCategory
import com.personal.daymark.data.repository.TaskRepository
import com.personal.daymark.ui.screens.calendar.CalendarViewMode
import com.personal.daymark.ui.screens.calendar.CalendarViewModel
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

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
    private lateinit var viewModel: CalendarViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeTaskRepo = FakeTaskRepository()
        viewModel = CalendarViewModel(fakeTaskRepo)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialViewModeIsMonthly() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(CalendarViewMode.MONTHLY, state.viewMode)
        assertEquals(LocalDate.now(), state.selectedDate)
        assertEquals(YearMonth.now(), state.displayedMonth)
        assertTrue(state.monthGridDays.isNotEmpty())
        assertEquals(0, state.monthGridDays.size % 7) // Always full weeks
    }

    @Test
    fun viewModeSwitching_updatesUiState() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.selectViewMode(CalendarViewMode.WEEKLY)
        advanceUntilIdle()
        assertEquals(CalendarViewMode.WEEKLY, viewModel.uiState.value.viewMode)
        assertEquals(7, viewModel.uiState.value.weekDays.size)

        viewModel.selectViewMode(CalendarViewMode.DAILY)
        advanceUntilIdle()
        assertEquals(CalendarViewMode.DAILY, viewModel.uiState.value.viewMode)
    }

    @Test
    fun selectingDate_filtersTasksForSelectedDate() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val zoneId = ZoneId.systemDefault()
        val targetDate = LocalDate.now().plusDays(2)
        val targetMillis = targetDate.atStartOfDay(zoneId).toInstant().toEpochMilli()

        val otherDate = LocalDate.now().plusDays(5)
        val otherMillis = otherDate.atStartOfDay(zoneId).toInstant().toEpochMilli()

        val workCategory = CategoryEntity(id = 1L, name = "Work", colorHex = "#1B4332")

        fakeTaskRepo.taskItems.value = listOf(
            TaskWithSubtasksAndCategory(
                task = TaskEntity(id = 10L, title = "Quarterly Review", dueDate = targetMillis, categoryId = 1L),
                category = workCategory
            ),
            TaskWithSubtasksAndCategory(
                task = TaskEntity(id = 20L, title = "Doctor Visit", dueDate = otherMillis)
            )
        )
        advanceUntilIdle()

        viewModel.selectDate(targetDate)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(targetDate, state.selectedDate)
        assertEquals(1, state.tasksForSelectedDate.size)
        assertEquals("Quarterly Review", state.tasksForSelectedDate[0].task.title)
    }

    @Test
    fun quickAddPreFillsSelectedDateTimestamp() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val futureDate = LocalDate.now().plusDays(3)
        val expectedMillis = futureDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        viewModel.openQuickAdd(futureDate)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isQuickAddOpen)
        assertEquals(expectedMillis, state.quickAddDateMillis)

        viewModel.closeQuickAdd()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isQuickAddOpen)
    }

    @Test
    fun overdueTasks_identifiedCorrectly() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val pastDate = LocalDate.now().minusDays(3)
        val pastMillis = pastDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val todayMillis = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        fakeTaskRepo.taskItems.value = listOf(
            TaskWithSubtasksAndCategory(
                task = TaskEntity(id = 1L, title = "Unfinished invoice", dueDate = pastMillis, isCompleted = false)
            ),
            TaskWithSubtasksAndCategory(
                task = TaskEntity(id = 2L, title = "Finished past task", dueDate = pastMillis, isCompleted = true)
            ),
            TaskWithSubtasksAndCategory(
                task = TaskEntity(id = 3L, title = "Today's task", dueDate = todayMillis, isCompleted = false)
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.overdueTasks.size)
        assertEquals("Unfinished invoice", state.overdueTasks[0].task.title)
    }

    @Test
    fun navigationNextAndPrevious_movesDateOrMonthCorrectly() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        // In Monthly: moves month
        val currentMonth = YearMonth.now()
        viewModel.navigateNext()
        advanceUntilIdle()
        assertEquals(currentMonth.plusMonths(1), viewModel.uiState.value.displayedMonth)

        viewModel.navigatePrevious()
        advanceUntilIdle()
        assertEquals(currentMonth, viewModel.uiState.value.displayedMonth)

        // In Daily: moves day
        viewModel.selectViewMode(CalendarViewMode.DAILY)
        advanceUntilIdle()
        val today = LocalDate.now()
        viewModel.navigateNext()
        advanceUntilIdle()
        assertEquals(today.plusDays(1), viewModel.uiState.value.selectedDate)

        viewModel.jumpToToday()
        advanceUntilIdle()
        assertEquals(today, viewModel.uiState.value.selectedDate)
    }

    @Test
    fun weekStartDay_sundayPreference_adjustsFirstDayOfWeekStrip() = runTest {
        val fakeSettings = object : com.personal.daymark.data.repository.SettingsRepository {
            val prefFlow = kotlinx.coroutines.flow.MutableStateFlow(
                com.personal.daymark.data.datastore.UserPreferences(weekStartDay = com.personal.daymark.data.datastore.UserPreferences.WEEK_START_SUNDAY)
            )
            override val userPreferencesFlow = prefFlow
            override suspend fun updateThemePreference(theme: String) {}
            override suspend fun updateDefaultTaskPriority(priority: Int) {}
            override suspend fun updateWeekStartDay(day: Int) {}
            override suspend fun updateDefaultFocusDuration(minutes: Int) {}
            override suspend fun updateShortBreakDuration(minutes: Int) {}
            override suspend fun updateLongBreakDuration(minutes: Int) {}
            override suspend fun updateSessionsUntilLongBreak(count: Int) {}
            override suspend fun updateHapticFeedbackEnabled(enabled: Boolean) {}
            override suspend fun updateDailySummaryEnabled(enabled: Boolean) {}
        }

        val vm = CalendarViewModel(fakeTaskRepo, settingsRepository = fakeSettings)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect()
        }
        advanceUntilIdle()

        assertEquals(com.personal.daymark.data.datastore.UserPreferences.WEEK_START_SUNDAY, vm.uiState.value.weekStartDay)
        // With Sunday start, the first day of week strip is a Sunday
        val firstDay = vm.uiState.value.weekDays.first()
        assertEquals(java.time.DayOfWeek.SUNDAY, firstDay.date.dayOfWeek)
    }
}
