package com.personal.daymark.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.personal.daymark.data.datastore.UserPreferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException

val Context.daymarkDataStore: DataStore<Preferences> by preferencesDataStore(name = "daymark_settings")

interface SettingsRepository {
    val userPreferencesFlow: Flow<UserPreferences>
    suspend fun updateThemePreference(theme: String)
    suspend fun updateDefaultTaskPriority(priority: Int)
    suspend fun updateWeekStartDay(day: Int)
    suspend fun updateDefaultFocusDuration(minutes: Int)
    suspend fun updateShortBreakDuration(minutes: Int)
    suspend fun updateLongBreakDuration(minutes: Int)
    suspend fun updateSessionsUntilLongBreak(count: Int)
    suspend fun updateHapticFeedbackEnabled(enabled: Boolean)
    suspend fun updateDailySummaryEnabled(enabled: Boolean)
}

class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SettingsRepository {

    private object PreferencesKeys {
        val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
        val DEFAULT_TASK_PRIORITY = intPreferencesKey("default_task_priority")
        val WEEK_START_DAY = intPreferencesKey("week_start_day")
        val DEFAULT_FOCUS_DURATION = intPreferencesKey("default_focus_duration")
        val SHORT_BREAK_DURATION = intPreferencesKey("short_break_duration")
        val LONG_BREAK_DURATION = intPreferencesKey("long_break_duration")
        val SESSIONS_UNTIL_LONG_BREAK = intPreferencesKey("sessions_until_long_break")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val DAILY_SUMMARY = booleanPreferencesKey("daily_summary")
    }

    override val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserPreferences(
                themePreference = preferences[PreferencesKeys.THEME_PREFERENCE] ?: UserPreferences.THEME_SYSTEM,
                defaultTaskPriority = preferences[PreferencesKeys.DEFAULT_TASK_PRIORITY] ?: 0,
                weekStartDay = preferences[PreferencesKeys.WEEK_START_DAY] ?: UserPreferences.WEEK_START_MONDAY,
                defaultFocusDurationMinutes = preferences[PreferencesKeys.DEFAULT_FOCUS_DURATION] ?: 25,
                shortBreakDurationMinutes = preferences[PreferencesKeys.SHORT_BREAK_DURATION] ?: 5,
                longBreakDurationMinutes = preferences[PreferencesKeys.LONG_BREAK_DURATION] ?: 15,
                sessionsUntilLongBreak = preferences[PreferencesKeys.SESSIONS_UNTIL_LONG_BREAK] ?: 4,
                hapticFeedbackEnabled = preferences[PreferencesKeys.HAPTIC_FEEDBACK] ?: true,
                dailySummaryEnabled = preferences[PreferencesKeys.DAILY_SUMMARY] ?: true
            )
        }
        .flowOn(ioDispatcher)

    override suspend fun updateThemePreference(theme: String) {
        withContext(ioDispatcher) {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.THEME_PREFERENCE] = theme
            }
        }
    }

    override suspend fun updateDefaultTaskPriority(priority: Int) {
        withContext(ioDispatcher) {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.DEFAULT_TASK_PRIORITY] = priority
            }
        }
    }

    override suspend fun updateWeekStartDay(day: Int) {
        withContext(ioDispatcher) {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.WEEK_START_DAY] = day
            }
        }
    }

    override suspend fun updateDefaultFocusDuration(minutes: Int) {
        withContext(ioDispatcher) {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.DEFAULT_FOCUS_DURATION] = minutes
            }
        }
    }

    override suspend fun updateShortBreakDuration(minutes: Int) {
        withContext(ioDispatcher) {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.SHORT_BREAK_DURATION] = minutes
            }
        }
    }

    override suspend fun updateLongBreakDuration(minutes: Int) {
        withContext(ioDispatcher) {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.LONG_BREAK_DURATION] = minutes
            }
        }
    }

    override suspend fun updateSessionsUntilLongBreak(count: Int) {
        withContext(ioDispatcher) {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.SESSIONS_UNTIL_LONG_BREAK] = count
            }
        }
    }

    override suspend fun updateHapticFeedbackEnabled(enabled: Boolean) {
        withContext(ioDispatcher) {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.HAPTIC_FEEDBACK] = enabled
            }
        }
    }

    override suspend fun updateDailySummaryEnabled(enabled: Boolean) {
        withContext(ioDispatcher) {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.DAILY_SUMMARY] = enabled
            }
        }
    }
}
