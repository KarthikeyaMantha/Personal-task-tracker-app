package com.personal.daymark.di

import android.content.Context
import com.personal.daymark.data.local.DaymarkDatabase
import com.personal.daymark.data.repository.CategoryRepository
import com.personal.daymark.data.repository.CategoryRepositoryImpl
import com.personal.daymark.data.repository.FocusRepository
import com.personal.daymark.data.repository.FocusRepositoryImpl
import com.personal.daymark.data.repository.HabitRepository
import com.personal.daymark.data.repository.HabitRepositoryImpl
import com.personal.daymark.data.repository.SettingsRepository
import com.personal.daymark.data.repository.SettingsRepositoryImpl
import com.personal.daymark.data.repository.TaskRepository
import com.personal.daymark.data.repository.TaskRepositoryImpl
import com.personal.daymark.data.repository.TemplateRepository
import com.personal.daymark.data.repository.TimeBlockRepository
import com.personal.daymark.data.repository.daymarkDataStore

interface AppContainer {
    val database: DaymarkDatabase
    val taskRepository: TaskRepository
    val categoryRepository: CategoryRepository
    val habitRepository: HabitRepository
    val focusRepository: FocusRepository
    val settingsRepository: SettingsRepository
    val timeBlockRepository: TimeBlockRepository
    val templateRepository: TemplateRepository
    val reminderScheduler: com.personal.daymark.core.notifications.ReminderScheduler
    val focusTimerManager: com.personal.daymark.core.focus.FocusTimerManager
    val backupManager: com.personal.daymark.core.backup.BackupManager
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val database: DaymarkDatabase by lazy {
        DaymarkDatabase.getInstance(context)
    }

    override val taskRepository: TaskRepository by lazy {
        TaskRepositoryImpl(
            taskDao = database.taskDao(),
            subtaskDao = database.subtaskDao(),
            context = context
        )
    }

    override val categoryRepository: CategoryRepository by lazy {
        CategoryRepositoryImpl(
            categoryDao = database.categoryDao(),
            taskDao = database.taskDao()
        )
    }

    override val habitRepository: HabitRepository by lazy {
        HabitRepositoryImpl(
            habitDao = database.habitDao(),
            context = context
        )
    }

    override val focusRepository: FocusRepository by lazy {
        FocusRepositoryImpl(
            focusDao = database.focusDao()
        )
    }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(
            dataStore = context.daymarkDataStore
        )
    }

    override val timeBlockRepository: com.personal.daymark.data.repository.TimeBlockRepository by lazy {
        com.personal.daymark.data.repository.TimeBlockRepositoryImpl(
            timeBlockDao = database.timeBlockDao()
        )
    }

    override val templateRepository: com.personal.daymark.data.repository.TemplateRepository by lazy {
        com.personal.daymark.data.repository.TemplateRepositoryImpl(
            dayTemplateDao = database.dayTemplateDao(),
            taskDao = database.taskDao(),
            timeBlockDao = database.timeBlockDao()
        )
    }

    override val reminderScheduler: com.personal.daymark.core.notifications.ReminderScheduler by lazy {
        com.personal.daymark.core.notifications.AlarmManagerReminderScheduler(
            context = context.applicationContext
        )
    }

    override val focusTimerManager: com.personal.daymark.core.focus.FocusTimerManager by lazy {
        com.personal.daymark.core.focus.FocusTimerManager(
            context = context.applicationContext,
            focusRepository = focusRepository,
            settingsRepository = settingsRepository
        )
    }

    override val backupManager: com.personal.daymark.core.backup.BackupManager by lazy {
        com.personal.daymark.core.backup.BackupManagerImpl(
            database = database,
            settingsRepository = settingsRepository,
            reminderScheduler = reminderScheduler
        )
    }
}
