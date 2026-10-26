package com.personal.daymark.ui.habits

import com.personal.daymark.data.local.entity.CategoryEntity
import com.personal.daymark.data.local.entity.HabitEntity
import com.personal.daymark.data.local.entity.HabitLogEntity
import com.personal.daymark.data.local.entity.HabitWithLogs
import com.personal.daymark.data.repository.CategoryRepository
import com.personal.daymark.data.repository.HabitRepository
import com.personal.daymark.ui.screens.habits.HabitsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HabitsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeHabitRepository : HabitRepository {
        val habits = mutableListOf<HabitEntity>()
        val logs = mutableListOf<HabitLogEntity>()
        private val _habitsFlow = MutableStateFlow<List<HabitWithLogs>>(emptyList())

        fun updateFlow() {
            val list = habits.map { h ->
                HabitWithLogs(
                    habit = h,
                    logs = logs.filter { it.habitId == h.id }
                )
            }
            _habitsFlow.value = list
        }

        override fun getActiveHabitsWithLogs(): Flow<List<HabitWithLogs>> = _habitsFlow.asStateFlow()
        override fun getAllHabitsWithLogs(): Flow<List<HabitWithLogs>> = _habitsFlow.asStateFlow()
        override fun getHabitWithLogsById(id: Long): Flow<HabitWithLogs?> = flowOf(null)
        override fun getLogsForHabit(habitId: Long): Flow<List<HabitLogEntity>> = flowOf(emptyList())

        override suspend fun createHabit(
            name: String,
            note: String?,
            frequency: String,
            targetWeekdays: Int,
            colorHex: String,
            categoryId: Long?
        ): Long {
            val newId = (habits.size + 1).toLong()
            val newHabit = HabitEntity(
                id = newId,
                name = name,
                note = note,
                frequency = frequency,
                targetWeekdays = targetWeekdays,
                colorHex = colorHex,
                categoryId = categoryId
            )
            habits.add(newHabit)
            updateFlow()
            return newId
        }

        override suspend fun updateHabit(habit: HabitEntity) {
            val index = habits.indexOfFirst { it.id == habit.id }
            if (index >= 0) {
                habits[index] = habit
                updateFlow()
            }
        }

        override suspend fun deleteHabit(habit: HabitEntity) {
            deleteHabitById(habit.id)
        }

        override suspend fun deleteHabitById(id: Long) {
            habits.removeAll { it.id == id }
            logs.removeAll { it.habitId == id }
            updateFlow()
        }

        override suspend fun setHabitCompleted(habitId: Long, date: Long, isCompleted: Boolean) {
            setHabitCompletedForDate(habitId, LocalDate.ofEpochDay(date), isCompleted)
        }

        override suspend fun setHabitCompletedForDate(habitId: Long, localDate: LocalDate, isCompleted: Boolean) {
            val dateStr = localDate.toString()
            if (isCompleted) {
                if (logs.none { it.habitId == habitId && it.dateString == dateStr }) {
                    logs.add(
                        HabitLogEntity(
                            id = (logs.size + 1).toLong(),
                            habitId = habitId,
                            date = localDate.toEpochDay(),
                            dateString = dateStr,
                            isCompleted = true
                        )
                    )
                }
            } else {
                logs.removeAll { it.habitId == habitId && it.dateString == dateStr }
            }
            updateFlow()
        }

        override suspend fun toggleHabitCompletion(habitId: Long, localDate: LocalDate): Boolean {
            val dateStr = localDate.toString()
            val existing = logs.find { it.habitId == habitId && it.dateString == dateStr }
            return if (existing != null) {
                logs.remove(existing)
                updateFlow()
                false
            } else {
                logs.add(
                    HabitLogEntity(
                        id = (logs.size + 1).toLong(),
                        habitId = habitId,
                        date = localDate.toEpochDay(),
                        dateString = dateStr,
                        isCompleted = true
                    )
                )
                updateFlow()
                true
            }
        }

        override fun getActiveHabitsCount(): Flow<Int> = flowOf(habits.count { !it.isArchived })
        override fun getHabitCompletionsCountInRange(startDateString: String, endDateString: String): Flow<Int> = flowOf(0)
        override fun getAllLogsInRange(startDateString: String, endDateString: String): Flow<List<HabitLogEntity>> = flowOf(emptyList())
    }

    private class FakeCategoryRepository : CategoryRepository {
        override fun getAllCategories(): Flow<List<CategoryEntity>> = flowOf(emptyList())
        override fun getCategoryById(id: Long): Flow<CategoryEntity?> = flowOf(null)
        override suspend fun createCategory(name: String, colorHex: String, iconName: String?, sortOrder: Int): Long = 1L
        override suspend fun updateCategory(category: CategoryEntity) {}
        override suspend fun deleteCategory(category: CategoryEntity) {}
        override suspend fun deleteCategoryById(id: Long) {}
        override suspend fun seedDefaultCategoriesIfEmpty() {}
    }

    private lateinit var fakeHabitRepo: FakeHabitRepository
    private lateinit var fakeCatRepo: FakeCategoryRepository
    private lateinit var viewModel: HabitsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeHabitRepo = FakeHabitRepository()
        fakeCatRepo = FakeCategoryRepository()
        viewModel = HabitsViewModel(fakeHabitRepo, fakeCatRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadingHabits_displaysHabitsWithAccurateCompletionStatus() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        val today = LocalDate.now()
        fakeHabitRepo.habits.add(
            HabitEntity(id = 1L, name = "Morning Jog", frequency = "DAILY", targetWeekdays = 127)
        )
        fakeHabitRepo.updateFlow()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.habits.size)
        assertEquals("Morning Jog", state.habits[0].habit.name)
        assertFalse("Not completed today yet", state.habits[0].isCompletedToday)

        // Toggle today
        viewModel.toggleCompletionToday(1L)
        advanceUntilIdle()

        assertTrue("Should be completed today after toggle", viewModel.uiState.value.habits[0].isCompletedToday)
        assertEquals(1, viewModel.uiState.value.habits[0].currentStreak)
    }

    @Test
    fun toggleCompletionToday_undoRemovesLog() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        fakeHabitRepo.habits.add(
            HabitEntity(id = 1L, name = "Meditation", frequency = "DAILY", targetWeekdays = 127)
        )
        fakeHabitRepo.updateFlow()
        advanceUntilIdle()

        // 1st toggle: complete
        viewModel.toggleCompletionToday(1L)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.habits[0].isCompletedToday)

        // 2nd toggle: undo/uncomplete
        viewModel.toggleCompletionToday(1L)
        advanceUntilIdle()
        assertFalse("Undo must uncheck the habit", viewModel.uiState.value.habits[0].isCompletedToday)
    }

    @Test
    fun habitCrud_createEditAndDeleteFlow() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        // 1. Create habit
        viewModel.saveHabit(
            name = "Deep Reading",
            note = "Read 20 pages",
            frequency = "DAILY",
            targetWeekdays = 127,
            colorHex = "#1B4332",
            categoryId = null
        )
        advanceUntilIdle()

        assertEquals(1, fakeHabitRepo.habits.size)
        assertEquals("Deep Reading", fakeHabitRepo.habits[0].name)
        assertEquals("Read 20 pages", fakeHabitRepo.habits[0].note)

        // 2. Edit habit
        val habit = fakeHabitRepo.habits[0]
        viewModel.saveHabit(
            id = habit.id,
            name = "Deep Reading (Night)",
            note = "Read 30 pages",
            frequency = "DAILY",
            targetWeekdays = 127,
            colorHex = "#40916C",
            categoryId = null
        )
        advanceUntilIdle()

        assertEquals("Deep Reading (Night)", fakeHabitRepo.habits[0].name)
        assertEquals("Read 30 pages", fakeHabitRepo.habits[0].note)

        // 3. Delete habit with confirmation
        viewModel.requestDeleteConfirmation(fakeHabitRepo.habits[0])
        advanceUntilIdle()
        assertEquals("Deep Reading (Night)", viewModel.uiState.value.habitToDelete?.name)

        viewModel.confirmDeleteHabit()
        advanceUntilIdle()

        assertTrue(fakeHabitRepo.habits.isEmpty())
    }
}
