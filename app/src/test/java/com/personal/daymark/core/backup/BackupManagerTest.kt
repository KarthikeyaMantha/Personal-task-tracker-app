package com.personal.daymark.core.backup

import com.personal.daymark.core.notifications.ReminderScheduler
import com.personal.daymark.data.datastore.UserPreferences
import com.personal.daymark.data.local.DaymarkDatabase
import com.personal.daymark.data.local.entity.CategoryEntity
import com.personal.daymark.data.local.entity.FocusSessionEntity
import com.personal.daymark.data.local.entity.HabitEntity
import com.personal.daymark.data.local.entity.HabitLogEntity
import com.personal.daymark.data.local.entity.SubtaskEntity
import com.personal.daymark.data.local.entity.TaskEntity
import com.personal.daymark.data.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider

@RunWith(RobolectricTestRunner::class)
class BackupManagerTest {

    private lateinit var database: DaymarkDatabase
    private lateinit var fakeSettingsRepo: FakeSettingsRepository
    private lateinit var fakeReminderScheduler: FakeReminderScheduler
    private lateinit var backupManager: BackupManager

    private class FakeSettingsRepository : SettingsRepository {
        var userPrefs = UserPreferences(
            themePreference = "DARK",
            defaultTaskPriority = 2,
            weekStartDay = 1,
            defaultFocusDurationMinutes = 30
        )
        override val userPreferencesFlow: Flow<UserPreferences> = MutableStateFlow(userPrefs)

        override suspend fun updateThemePreference(theme: String) { userPrefs = userPrefs.copy(themePreference = theme) }
        override suspend fun updateDefaultTaskPriority(priority: Int) { userPrefs = userPrefs.copy(defaultTaskPriority = priority) }
        override suspend fun updateWeekStartDay(day: Int) { userPrefs = userPrefs.copy(weekStartDay = day) }
        override suspend fun updateDefaultFocusDuration(minutes: Int) { userPrefs = userPrefs.copy(defaultFocusDurationMinutes = minutes) }
        override suspend fun updateShortBreakDuration(minutes: Int) { userPrefs = userPrefs.copy(shortBreakDurationMinutes = minutes) }
        override suspend fun updateLongBreakDuration(minutes: Int) { userPrefs = userPrefs.copy(longBreakDurationMinutes = minutes) }
        override suspend fun updateSessionsUntilLongBreak(count: Int) { userPrefs = userPrefs.copy(sessionsUntilLongBreak = count) }
        override suspend fun updateHapticFeedbackEnabled(enabled: Boolean) { userPrefs = userPrefs.copy(hapticFeedbackEnabled = enabled) }
        override suspend fun updateDailySummaryEnabled(enabled: Boolean) { userPrefs = userPrefs.copy(dailySummaryEnabled = enabled) }
    }

    private class FakeReminderScheduler : ReminderScheduler {
        val scheduledTasks = mutableListOf<TaskEntity>()
        override fun scheduleReminder(task: TaskEntity) { scheduledTasks.add(task) }
        override fun scheduleReminder(taskId: Long, title: String, description: String?, triggerTimeMillis: Long) {}
        override fun scheduleSnooze(taskId: Long, title: String, snoozeTimeMillis: Long) {}
        override fun cancelReminder(taskId: Long) { scheduledTasks.removeAll { it.id == taskId } }
        override fun canScheduleExactAlarms(): Boolean = true
    }

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, DaymarkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        fakeSettingsRepo = FakeSettingsRepository()
        fakeReminderScheduler = FakeReminderScheduler()
        backupManager = BackupManagerImpl(
            database = database,
            settingsRepository = fakeSettingsRepo,
            reminderScheduler = fakeReminderScheduler,
            ioDispatcher = kotlinx.coroutines.Dispatchers.Unconfined
        )
    }

    @Test
    fun roundTripBackupAndRestore_preservesAllData() = runTest {
        // 1. Seed database with categories, tasks, subtasks, habits, logs, and focus sessions
        val categoryId = database.categoryDao().insertCategory(
            CategoryEntity(id = 1L, name = "Deep Work", colorHex = "#1B4332")
        )
        val taskId = database.taskDao().insertTask(
            TaskEntity(
                id = 1L,
                title = "Ship Phase 13",
                description = "Backup & Restore SAF implementation",
                priority = 3,
                categoryId = categoryId,
                dueDate = System.currentTimeMillis() + 86400000L,
                dueTime = 600
            )
        )
        database.subtaskDao().insertSubtask(
            SubtaskEntity(id = 1L, taskId = taskId, title = "Write DTOs", isCompleted = true)
        )
        val habitId = database.habitDao().insertHabit(
            HabitEntity(id = 1L, name = "Daily Meditation", frequency = "DAILY", targetWeekdays = 127)
        )
        database.habitDao().insertHabitLog(
            HabitLogEntity(id = 1L, habitId = habitId, date = 19500L, dateString = "2026-10-26", isCompleted = true)
        )
        database.focusDao().insertSession(
            FocusSessionEntity(id = 1L, taskId = taskId, durationMinutes = 25, sessionType = "FOCUS")
        )

        // 2. Export backup JSON
        val backupJson = backupManager.createBackupJson()
        assertNotNull(backupJson)
        assertTrue(backupJson.contains("\"schemaVersion\": 1"))
        assertTrue(backupJson.contains("Ship Phase 13"))
        assertTrue(backupJson.contains("Daily Meditation"))

        // 3. Clear database to simulate data loss / fresh install
        database.clearAllTables()
        assertEquals(0, database.taskDao().getAllTasksList().size)
        assertEquals(0, database.habitDao().getAllHabitsList().size)

        // 4. Restore from JSON
        val restoreResult = backupManager.restoreBackupFromJson(backupJson)
        assertTrue(restoreResult is RestoreResult.Success)
        val success = restoreResult as RestoreResult.Success
        assertEquals(1, success.categoriesCount)
        assertEquals(1, success.tasksCount)
        assertEquals(1, success.subtasksCount)
        assertEquals(1, success.habitsCount)
        assertEquals(1, success.habitLogsCount)
        assertEquals(1, success.focusSessionsCount)

        // 5. Verify restored database records match original state
        val restoredTasks = database.taskDao().getAllTasksList()
        assertEquals(1, restoredTasks.size)
        assertEquals("Ship Phase 13", restoredTasks[0].title)
        assertEquals(3, restoredTasks[0].priority)

        val restoredSubtasks = database.subtaskDao().getAllSubtasksList()
        assertEquals(1, restoredSubtasks.size)
        assertEquals("Write DTOs", restoredSubtasks[0].title)
        assertTrue(restoredSubtasks[0].isCompleted)

        val restoredHabits = database.habitDao().getAllHabitsList()
        assertEquals(1, restoredHabits.size)
        assertEquals("Daily Meditation", restoredHabits[0].name)

        val restoredLogs = database.habitDao().getAllHabitLogsList()
        assertEquals(1, restoredLogs.size)
        assertEquals("2026-10-26", restoredLogs[0].dateString)

        val restoredFocus = database.focusDao().getAllFocusSessionsList()
        assertEquals(1, restoredFocus.size)
        assertEquals(25, restoredFocus[0].durationMinutes)
    }

    @Test
    fun restoreCorruptedJson_returnsErrorGracefullyWithoutCrashing() = runTest {
        val corruptedJson = "{\"schemaVersion\": 1, \"tasks\": [ { \"id\": broken json ..."
        val result = backupManager.restoreBackupFromJson(corruptedJson)
        assertTrue(result is RestoreResult.Error)
        val error = result as RestoreResult.Error
        assertTrue(error.message.contains("Invalid or corrupted") || error.message.contains("format"))
    }

    @Test
    fun restoreIncompatibleFutureSchema_returnsErrorGracefully() = runTest {
        val futureJson = "{\"schemaVersion\": 99, \"appVersion\": \"2.0.0\", \"tasks\": []}"
        val result = backupManager.restoreBackupFromJson(futureJson)
        assertTrue(result is RestoreResult.Error)
        val error = result as RestoreResult.Error
        assertTrue(error.message.contains("newer version"))
    }

    @Test
    fun restoreEmptyFile_returnsErrorGracefully() = runTest {
        val result = backupManager.restoreBackupFromJson("   ")
        assertTrue(result is RestoreResult.Error)
        val error = result as RestoreResult.Error
        assertTrue(error.message.contains("empty"))
    }
}
