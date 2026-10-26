package com.personal.daymark.core.backup

import com.personal.daymark.data.datastore.UserPreferences
import com.personal.daymark.data.local.entity.CategoryEntity
import com.personal.daymark.data.local.entity.FocusSessionEntity
import com.personal.daymark.data.local.entity.HabitEntity
import com.personal.daymark.data.local.entity.HabitLogEntity
import com.personal.daymark.data.local.entity.SubtaskEntity
import com.personal.daymark.data.local.entity.TaskEntity
import kotlinx.serialization.Serializable

const val CURRENT_BACKUP_SCHEMA_VERSION = 1

class IncompatibleSchemaException(message: String) : Exception(message)
class CorruptBackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

@Serializable
data class CategoryBackupDto(
    val id: Long,
    val name: String,
    val colorHex: String,
    val iconName: String? = null,
    val sortOrder: Int = 0,
    val createdAt: Long
)

@Serializable
data class TaskBackupDto(
    val id: Long,
    val title: String,
    val description: String? = null,
    val dueDate: Long? = null,
    val dueTime: Int? = null,
    val priority: Int = 0,
    val categoryId: Long? = null,
    val isCompleted: Boolean = false,
    val completionTimestamp: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val recurrenceRule: String? = null,
    val seriesId: String? = null
)

@Serializable
data class SubtaskBackupDto(
    val id: Long,
    val taskId: Long,
    val title: String,
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long
)

@Serializable
data class HabitBackupDto(
    val id: Long,
    val name: String,
    val note: String? = null,
    val frequency: String = "DAILY",
    val targetWeekdays: Int = 127,
    val colorHex: String = "#1B4332",
    val categoryId: Long? = null,
    val isArchived: Boolean = false,
    val createdAt: Long
)

@Serializable
data class HabitLogBackupDto(
    val id: Long,
    val habitId: Long,
    val date: Long,
    val dateString: String = "",
    val isCompleted: Boolean = true,
    val loggedAt: Long
)

@Serializable
data class FocusSessionBackupDto(
    val id: Long,
    val taskId: Long? = null,
    val durationMinutes: Int,
    val plannedDurationMinutes: Int = 25,
    val sessionType: String = "FOCUS",
    val startedAt: Long,
    val completedAt: Long? = null,
    val notes: String? = null
)

@Serializable
data class UserPreferencesBackupDto(
    val themePreference: String = "SYSTEM",
    val defaultTaskPriority: Int = 0,
    val weekStartDay: Int = 1,
    val defaultFocusDurationMinutes: Int = 25,
    val shortBreakDurationMinutes: Int = 5,
    val longBreakDurationMinutes: Int = 15,
    val sessionsUntilLongBreak: Int = 4,
    val hapticFeedbackEnabled: Boolean = true,
    val dailySummaryEnabled: Boolean = true
)

@Serializable
data class DaymarkBackupData(
    val schemaVersion: Int = CURRENT_BACKUP_SCHEMA_VERSION,
    val exportedAt: Long = System.currentTimeMillis(),
    val appVersion: String = "1.0.0",
    val categories: List<CategoryBackupDto> = emptyList(),
    val tasks: List<TaskBackupDto> = emptyList(),
    val subtasks: List<SubtaskBackupDto> = emptyList(),
    val habits: List<HabitBackupDto> = emptyList(),
    val habitLogs: List<HabitLogBackupDto> = emptyList(),
    val focusSessions: List<FocusSessionBackupDto> = emptyList(),
    val userPreferences: UserPreferencesBackupDto? = null
)

// Mapping extensions from entities to DTOs
fun CategoryEntity.toDto() = CategoryBackupDto(id, name, colorHex, iconName, sortOrder, createdAt)
fun TaskEntity.toDto() = TaskBackupDto(id, title, description, dueDate, dueTime, priority, categoryId, isCompleted, completionTimestamp, createdAt, updatedAt, recurrenceRule, seriesId)
fun SubtaskEntity.toDto() = SubtaskBackupDto(id, taskId, title, isCompleted, sortOrder, createdAt)
fun HabitEntity.toDto() = HabitBackupDto(id, name, note, frequency, targetWeekdays, colorHex, categoryId, isArchived, createdAt)
fun HabitLogEntity.toDto() = HabitLogBackupDto(id, habitId, date, dateString, isCompleted, loggedAt)
fun FocusSessionEntity.toDto() = FocusSessionBackupDto(id, taskId, durationMinutes, plannedDurationMinutes, sessionType, startedAt, completedAt, notes)
fun UserPreferences.toDto() = UserPreferencesBackupDto(
    themePreference = themePreference,
    defaultTaskPriority = defaultTaskPriority,
    weekStartDay = weekStartDay,
    defaultFocusDurationMinutes = defaultFocusDurationMinutes,
    shortBreakDurationMinutes = shortBreakDurationMinutes,
    longBreakDurationMinutes = longBreakDurationMinutes,
    sessionsUntilLongBreak = sessionsUntilLongBreak,
    hapticFeedbackEnabled = hapticFeedbackEnabled,
    dailySummaryEnabled = dailySummaryEnabled
)

// Mapping extensions from DTOs to entities
fun CategoryBackupDto.toEntity() = CategoryEntity(id, name, colorHex, iconName, sortOrder, createdAt)
fun TaskBackupDto.toEntity() = TaskEntity(id, title, description, dueDate, dueTime, priority, categoryId, isCompleted, completionTimestamp, createdAt, updatedAt, recurrenceRule, seriesId)
fun SubtaskBackupDto.toEntity() = SubtaskEntity(id, taskId, title, isCompleted, sortOrder, createdAt)
fun HabitBackupDto.toEntity() = HabitEntity(id, name, note, frequency, targetWeekdays, colorHex, categoryId, isArchived, createdAt)
fun HabitLogBackupDto.toEntity() = HabitLogEntity(id, habitId, date, dateString, isCompleted, loggedAt)
fun FocusSessionBackupDto.toEntity() = FocusSessionEntity(id, taskId, durationMinutes, plannedDurationMinutes, sessionType, startedAt, completedAt, notes)
