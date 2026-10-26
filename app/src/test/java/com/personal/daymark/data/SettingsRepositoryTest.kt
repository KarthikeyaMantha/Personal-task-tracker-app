package com.personal.daymark.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.personal.daymark.data.datastore.UserPreferences
import com.personal.daymark.data.repository.SettingsRepository
import com.personal.daymark.data.repository.SettingsRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createRepository(scope: CoroutineScope): SettingsRepository {
        val testFile = File(tempFolder.newFolder(), "test_user_preferences.preferences_pb")
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { testFile }
        )
        return SettingsRepositoryImpl(
            dataStore = testDataStore,
            ioDispatcher = Dispatchers.Unconfined
        )
    }

    @Test
    fun defaultPreferencesAreCorrect() = runTest {
        val repository = createRepository(backgroundScope)
        val prefs = repository.userPreferencesFlow.first()
        assertEquals(UserPreferences.THEME_SYSTEM, prefs.themePreference)
        assertEquals(0, prefs.defaultTaskPriority)
        assertEquals(UserPreferences.WEEK_START_MONDAY, prefs.weekStartDay)
        assertEquals(25, prefs.defaultFocusDurationMinutes)
        assertTrue(prefs.hapticFeedbackEnabled)
        assertTrue(prefs.dailySummaryEnabled)
    }

    @Test
    fun updateThemePreferencePersistsValue() = runTest {
        val repository = createRepository(backgroundScope)
        repository.updateThemePreference(UserPreferences.THEME_DARK)
        val prefs = repository.userPreferencesFlow.first()
        assertEquals(UserPreferences.THEME_DARK, prefs.themePreference)
    }

    @Test
    fun updateFocusDurationAndHapticsPersists() = runTest {
        val repository = createRepository(backgroundScope)
        repository.updateDefaultFocusDuration(45)
        repository.updateHapticFeedbackEnabled(false)

        val prefs = repository.userPreferencesFlow.first()
        assertEquals(45, prefs.defaultFocusDurationMinutes)
        assertFalse(prefs.hapticFeedbackEnabled)
    }
}
