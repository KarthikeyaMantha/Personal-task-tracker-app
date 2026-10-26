package com.personal.daymark.ui.settings

import com.personal.daymark.data.datastore.UserPreferences
import com.personal.daymark.data.repository.SettingsRepository
import com.personal.daymark.ui.screens.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
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
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeSettingsRepository : SettingsRepository {
        val state = MutableStateFlow(UserPreferences())

        override val userPreferencesFlow: Flow<UserPreferences> = state

        override suspend fun updateThemePreference(theme: String) {
            state.value = state.value.copy(themePreference = theme)
        }

        override suspend fun updateDefaultTaskPriority(priority: Int) {
            state.value = state.value.copy(defaultTaskPriority = priority)
        }

        override suspend fun updateWeekStartDay(day: Int) {
            state.value = state.value.copy(weekStartDay = day)
        }

        override suspend fun updateDefaultFocusDuration(minutes: Int) {
            state.value = state.value.copy(defaultFocusDurationMinutes = minutes)
        }

        override suspend fun updateShortBreakDuration(minutes: Int) {
            state.value = state.value.copy(shortBreakDurationMinutes = minutes)
        }

        override suspend fun updateLongBreakDuration(minutes: Int) {
            state.value = state.value.copy(longBreakDurationMinutes = minutes)
        }

        override suspend fun updateSessionsUntilLongBreak(count: Int) {
            state.value = state.value.copy(sessionsUntilLongBreak = count)
        }

        override suspend fun updateHapticFeedbackEnabled(enabled: Boolean) {
            state.value = state.value.copy(hapticFeedbackEnabled = enabled)
        }

        override suspend fun updateDailySummaryEnabled(enabled: Boolean) {
            state.value = state.value.copy(dailySummaryEnabled = enabled)
        }
    }

    private lateinit var fakeRepo: FakeSettingsRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeSettingsRepository()
        viewModel = SettingsViewModel(fakeRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun setThemePreference_updatesRepositoryAndState() = runTest {
        backgroundScope.launch { viewModel.userPreferences.collect {} }
        advanceUntilIdle()

        assertEquals(UserPreferences.THEME_SYSTEM, viewModel.userPreferences.value.themePreference)

        viewModel.setThemePreference(UserPreferences.THEME_DARK)
        advanceUntilIdle()

        assertEquals(UserPreferences.THEME_DARK, viewModel.userPreferences.value.themePreference)
        assertEquals(UserPreferences.THEME_DARK, fakeRepo.state.value.themePreference)

        viewModel.setThemePreference(UserPreferences.THEME_LIGHT)
        advanceUntilIdle()

        assertEquals(UserPreferences.THEME_LIGHT, viewModel.userPreferences.value.themePreference)
    }

    @Test
    fun setDefaultTaskPriority_updatesRepositoryAndState() = runTest {
        backgroundScope.launch { viewModel.userPreferences.collect {} }
        advanceUntilIdle()

        assertEquals(0, viewModel.userPreferences.value.defaultTaskPriority)

        viewModel.setDefaultTaskPriority(3) // High priority
        advanceUntilIdle()

        assertEquals(3, viewModel.userPreferences.value.defaultTaskPriority)
        assertEquals(3, fakeRepo.state.value.defaultTaskPriority)
    }

    @Test
    fun setWeekStartDay_updatesRepositoryAndState() = runTest {
        backgroundScope.launch { viewModel.userPreferences.collect {} }
        advanceUntilIdle()

        assertEquals(UserPreferences.WEEK_START_MONDAY, viewModel.userPreferences.value.weekStartDay)

        viewModel.setWeekStartDay(UserPreferences.WEEK_START_SUNDAY)
        advanceUntilIdle()

        assertEquals(UserPreferences.WEEK_START_SUNDAY, viewModel.userPreferences.value.weekStartDay)
        assertEquals(UserPreferences.WEEK_START_SUNDAY, fakeRepo.state.value.weekStartDay)
    }

    @Test
    fun setFocusAndBreakDurations_updatesRepositoryAndState() = runTest {
        backgroundScope.launch { viewModel.userPreferences.collect {} }
        advanceUntilIdle()

        viewModel.setDefaultFocusDuration(45)
        viewModel.setShortBreakDuration(8)
        viewModel.setLongBreakDuration(25)
        viewModel.setSessionsUntilLongBreak(3)
        advanceUntilIdle()

        assertEquals(45, viewModel.userPreferences.value.defaultFocusDurationMinutes)
        assertEquals(8, viewModel.userPreferences.value.shortBreakDurationMinutes)
        assertEquals(25, viewModel.userPreferences.value.longBreakDurationMinutes)
        assertEquals(3, viewModel.userPreferences.value.sessionsUntilLongBreak)
    }

    @Test
    fun setHapticsAndDailySummary_updatesRepositoryAndState() = runTest {
        backgroundScope.launch { viewModel.userPreferences.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.userPreferences.value.hapticFeedbackEnabled)
        assertTrue(viewModel.userPreferences.value.dailySummaryEnabled)

        viewModel.setHapticFeedbackEnabled(false)
        viewModel.setDailySummaryEnabled(false)
        advanceUntilIdle()

        assertFalse(viewModel.userPreferences.value.hapticFeedbackEnabled)
        assertFalse(viewModel.userPreferences.value.dailySummaryEnabled)
    }
}
