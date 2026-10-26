package com.personal.daymark.core.backup

import androidx.room.withTransaction
import com.personal.daymark.core.notifications.ReminderScheduler
import com.personal.daymark.data.local.DaymarkDatabase
import com.personal.daymark.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed interface RestoreResult {
    data class Success(
        val categoriesCount: Int,
        val tasksCount: Int,
        val subtasksCount: Int,
        val habitsCount: Int,
        val habitLogsCount: Int,
        val focusSessionsCount: Int
    ) : RestoreResult

    data class Error(val message: String) : RestoreResult
}

interface BackupManager {
    suspend fun createBackupJson(): String
    suspend fun restoreBackupFromJson(jsonString: String): RestoreResult
    suspend fun exportTasksCsv(): String
    suspend fun exportHabitsCsv(): String
}

class BackupManagerImpl(
    private val database: DaymarkDatabase,
    private val settingsRepository: SettingsRepository,
    private val reminderScheduler: ReminderScheduler,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BackupManager {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun createBackupJson(): String = withContext(ioDispatcher) {
        val categoryDao = database.categoryDao()
        val taskDao = database.taskDao()
        val subtaskDao = database.subtaskDao()
        val habitDao = database.habitDao()
        val focusDao = database.focusDao()

        val categories = categoryDao.getAllCategoriesList().map { it.toDto() }
        val tasks = taskDao.getAllTasksList().map { it.toDto() }
        val subtasks = subtaskDao.getAllSubtasksList().map { it.toDto() }
        val habits = habitDao.getAllHabitsList().map { it.toDto() }
        val habitLogs = habitDao.getAllHabitLogsList().map { it.toDto() }
        val focusSessions = focusDao.getAllFocusSessionsList().map { it.toDto() }

        val prefs = try {
            settingsRepository.userPreferencesFlow.first().toDto()
        } catch (_: Exception) {
            null
        }

        val backupData = DaymarkBackupData(
            schemaVersion = CURRENT_BACKUP_SCHEMA_VERSION,
            exportedAt = System.currentTimeMillis(),
            appVersion = "1.0.0",
            categories = categories,
            tasks = tasks,
            subtasks = subtasks,
            habits = habits,
            habitLogs = habitLogs,
            focusSessions = focusSessions,
            userPreferences = prefs
        )

        json.encodeToString(backupData)
    }

    override suspend fun restoreBackupFromJson(jsonString: String): RestoreResult = withContext(ioDispatcher) {
        if (jsonString.isBlank()) {
            return@withContext RestoreResult.Error("The selected backup file is empty.")
        }

        val backupData: DaymarkBackupData = try {
            json.decodeFromString(DaymarkBackupData.serializer(), jsonString)
        } catch (e: SerializationException) {
            return@withContext RestoreResult.Error("Invalid or corrupted backup file format: ${e.message}")
        } catch (e: IllegalArgumentException) {
            return@withContext RestoreResult.Error("Corrupted backup data: ${e.message}")
        } catch (e: Exception) {
            return@withContext RestoreResult.Error("Could not read backup file: ${e.message}")
        }

        if (backupData.schemaVersion > CURRENT_BACKUP_SCHEMA_VERSION) {
            return@withContext RestoreResult.Error(
                "This backup was created with a newer version of Daymark (Schema v${backupData.schemaVersion}). Please update your app to restore it."
            )
        }

        if (backupData.schemaVersion <= 0) {
            return@withContext RestoreResult.Error("Invalid backup file schema version: ${backupData.schemaVersion}")
        }

        val categoryDao = database.categoryDao()
        val taskDao = database.taskDao()
        val subtaskDao = database.subtaskDao()
        val habitDao = database.habitDao()
        val focusDao = database.focusDao()

        try {
            database.withTransaction {
                // Delete existing records in reverse foreign-key dependency order
                focusDao.deleteAllFocusSessions()
                subtaskDao.deleteAllSubtasks()
                habitDao.deleteAllHabitLogs()
                taskDao.deleteAllTasks()
                habitDao.deleteAllHabits()
                categoryDao.deleteAllCategories()

                // Insert restored entities in forward dependency order
                if (backupData.categories.isNotEmpty()) {
                    categoryDao.insertCategories(backupData.categories.map { it.toEntity() })
                }
                if (backupData.habits.isNotEmpty()) {
                    habitDao.insertHabits(backupData.habits.map { it.toEntity() })
                }
                if (backupData.habitLogs.isNotEmpty()) {
                    habitDao.insertHabitLogs(backupData.habitLogs.map { it.toEntity() })
                }
                if (backupData.tasks.isNotEmpty()) {
                    taskDao.insertTasks(backupData.tasks.map { it.toEntity() })
                }
                if (backupData.subtasks.isNotEmpty()) {
                    subtaskDao.insertSubtasks(backupData.subtasks.map { it.toEntity() })
                }
                if (backupData.focusSessions.isNotEmpty()) {
                    focusDao.insertFocusSessions(backupData.focusSessions.map { it.toEntity() })
                }
            }

            // Restore preferences if present
            backupData.userPreferences?.let { p ->
                settingsRepository.updateThemePreference(p.themePreference)
                settingsRepository.updateDefaultTaskPriority(p.defaultTaskPriority)
                settingsRepository.updateWeekStartDay(p.weekStartDay)
                settingsRepository.updateDefaultFocusDuration(p.defaultFocusDurationMinutes)
                settingsRepository.updateShortBreakDuration(p.shortBreakDurationMinutes)
                settingsRepository.updateLongBreakDuration(p.longBreakDurationMinutes)
                settingsRepository.updateSessionsUntilLongBreak(p.sessionsUntilLongBreak)
                settingsRepository.updateHapticFeedbackEnabled(p.hapticFeedbackEnabled)
                settingsRepository.updateDailySummaryEnabled(p.dailySummaryEnabled)
            }

            // Reschedule pending alarms
            val restoredTasks = taskDao.getAllTasksList()
            for (task in restoredTasks) {
                if (!task.isCompleted && task.dueDate != null) {
                    reminderScheduler.scheduleReminder(task)
                } else {
                    reminderScheduler.cancelReminder(task.id)
                }
            }

            RestoreResult.Success(
                categoriesCount = backupData.categories.size,
                tasksCount = backupData.tasks.size,
                subtasksCount = backupData.subtasks.size,
                habitsCount = backupData.habits.size,
                habitLogsCount = backupData.habitLogs.size,
                focusSessionsCount = backupData.focusSessions.size
            )
        } catch (e: Exception) {
            RestoreResult.Error("Database restore failed: ${e.message}")
        }
    }

    override suspend fun exportTasksCsv(): String = withContext(ioDispatcher) {
        val tasks = database.taskDao().getAllTasksList()
        val categories = database.categoryDao().getAllCategoriesList().associateBy { it.id }
        CsvExportHelper.exportTasksToCsv(tasks, categories)
    }

    override suspend fun exportHabitsCsv(): String = withContext(ioDispatcher) {
        val habits = database.habitDao().getAllHabitsList()
        val categories = database.categoryDao().getAllCategoriesList().associateBy { it.id }
        CsvExportHelper.exportHabitsToCsv(habits, categories)
    }
}
