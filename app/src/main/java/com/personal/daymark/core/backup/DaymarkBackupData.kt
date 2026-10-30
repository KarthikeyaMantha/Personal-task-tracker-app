package com.personal.daymark.core.backup

import com.personal.daymark.data.datastore.UserPreferences
import com.personal.daymark.data.local.entity.CategoryEntity
import com.personal.daymark.data.local.entity.DayTemplateEntity
import com.personal.daymark.data.local.entity.FocusSessionEntity
import com.personal.daymark.data.local.entity.HabitEntity
import com.personal.daymark.data.local.entity.HabitLogEntity
import com.personal.daymark.data.local.entity.SubtaskEntity
import com.personal.daymark.data.local.entity.TaskEntity
import com.personal.daymark.data.local.entity.TemplateItemEntity
import com.personal.daymark.data.local.entity.TimeBlockEntity
import kotlinx.serialization.Serializable

const val CURRENT_BACKUP_SCHEMA_VERSION = 1

class IncompatibleSchemaException(message: String) : Exception(message)
class CorruptBackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

@Serializable
data class CategoryBackupDto(
    val id: Long,
    val name: String,
    val colorHex: String = "#1B4332",
    val iconName: String? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = 0L
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
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val recurrenceRule: String? = null,
    val seriesId: String? = null,
    val durationMinutes: Int = 0,
    val startTime: String? = null,
    val isMedicine: Boolean = false,
    val dosage: String? = null,
    val medicineForm: String? = null,
    val stockCount: Int? = null,
    val refillThreshold: Int? = null
)

@Serializable
data class SubtaskBackupDto(
    val id: Long,
    val taskId: Long,
    val title: String,
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = 0L
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
    val createdAt: Long = 0L,
    val healthMetricType: String? = null,
    val targetValue: Int? = null,
    val unit: String? = null,
    val reminderTime: Int? = null,
    val targetDurationMinutes: Int? = null,
    val routineTimeOfDay: String = "ANYTIME",
    val isFrozen: Boolean = false
)

@Serializable
data class HabitLogBackupDto(
    val id: Long,
    val habitId: Long,
    val date: Long,
    val dateString: String = "",
    val isCompleted: Boolean = true,
    val loggedAt: Long = 0L,
    val value: Float? = null,
    val note: String? = null,
    val isFrozen: Boolean = false
)

@Serializable
data class FocusSessionBackupDto(
    val id: Long,
    val taskId: Long? = null,
    val durationMinutes: Int,
    val plannedDurationMinutes: Int = 25,
    val sessionType: String = "FOCUS",
    val startedAt: Long = 0L,
    val completedAt: Long? = null,
    val notes: String? = null
)

@Serializable
data class TimeBlockBackupDto(
    val id: Long = 0,
    val title: String,
    val startTime: String,
    val durationMinutes: Int = 0,
    val colorHex: String? = null,
    val date: String = ""
)

@Serializable
data class DayTemplateBackupDto(
    val id: Long = 0,
    val name: String
)

@Serializable
data class TemplateItemBackupDto(
    val id: Long = 0,
    val templateId: Long,
    val title: String,
    val startTime: String = "",
    val durationMinutes: Int = 0,
    val isTask: Boolean = true
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
    val timeBlocks: List<TimeBlockBackupDto> = emptyList(),
    val dayTemplates: List<DayTemplateBackupDto> = emptyList(),
    val templateItems: List<TemplateItemBackupDto> = emptyList(),
    val userPreferences: UserPreferencesBackupDto? = null
)

// Mapping extensions from entities to DTOs
fun CategoryEntity.toDto() = CategoryBackupDto(id, name, colorHex, iconName, sortOrder, createdAt)
fun TaskEntity.toDto() = TaskBackupDto(
    id = id,
    title = title,
    description = description,
    dueDate = dueDate,
    dueTime = dueTime,
    priority = priority,
    categoryId = categoryId,
    isCompleted = isCompleted,
    completionTimestamp = completionTimestamp,
    createdAt = createdAt,
    updatedAt = updatedAt,
    recurrenceRule = recurrenceRule,
    seriesId = seriesId,
    durationMinutes = durationMinutes,
    startTime = startTime,
    isMedicine = isMedicine,
    dosage = dosage,
    medicineForm = medicineForm,
    stockCount = stockCount,
    refillThreshold = refillThreshold
)
fun SubtaskEntity.toDto() = SubtaskBackupDto(id, taskId, title, isCompleted, sortOrder, createdAt)
fun HabitEntity.toDto() = HabitBackupDto(
    id = id,
    name = name,
    note = note,
    frequency = frequency,
    targetWeekdays = targetWeekdays,
    colorHex = colorHex,
    categoryId = categoryId,
    isArchived = isArchived,
    createdAt = createdAt,
    healthMetricType = healthMetricType,
    targetValue = targetValue,
    unit = unit,
    reminderTime = reminderTime,
    targetDurationMinutes = targetDurationMinutes,
    routineTimeOfDay = routineTimeOfDay,
    isFrozen = isFrozen
)
fun HabitLogEntity.toDto() = HabitLogBackupDto(
    id = id,
    habitId = habitId,
    date = date,
    dateString = dateString,
    isCompleted = isCompleted,
    loggedAt = loggedAt,
    value = value,
    note = note,
    isFrozen = isFrozen
)
fun FocusSessionEntity.toDto() = FocusSessionBackupDto(id, taskId, durationMinutes, plannedDurationMinutes, sessionType, startedAt, completedAt, notes)
fun TimeBlockEntity.toDto() = TimeBlockBackupDto(id, title, startTime, durationMinutes, colorHex, date)
fun DayTemplateEntity.toDto() = DayTemplateBackupDto(id, name)
fun TemplateItemEntity.toDto() = TemplateItemBackupDto(id, templateId, title, startTime, durationMinutes, isTask)

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
fun TaskBackupDto.toEntity() = TaskEntity(
    id = id,
    title = title,
    description = description,
    dueDate = dueDate,
    dueTime = dueTime,
    priority = priority,
    categoryId = categoryId,
    isCompleted = isCompleted,
    completionTimestamp = completionTimestamp,
    createdAt = createdAt,
    updatedAt = updatedAt,
    recurrenceRule = recurrenceRule,
    seriesId = seriesId,
    durationMinutes = durationMinutes,
    startTime = startTime,
    isMedicine = isMedicine,
    dosage = dosage,
    medicineForm = medicineForm,
    stockCount = stockCount,
    refillThreshold = refillThreshold
)
fun SubtaskBackupDto.toEntity() = SubtaskEntity(id, taskId, title, isCompleted, sortOrder, createdAt)
fun HabitBackupDto.toEntity() = HabitEntity(
    id = id,
    name = name,
    note = note,
    frequency = frequency,
    targetWeekdays = targetWeekdays,
    colorHex = colorHex,
    categoryId = categoryId,
    isArchived = isArchived,
    createdAt = createdAt,
    healthMetricType = healthMetricType,
    targetValue = targetValue,
    unit = unit,
    reminderTime = reminderTime,
    targetDurationMinutes = targetDurationMinutes,
    routineTimeOfDay = routineTimeOfDay,
    isFrozen = isFrozen
)
fun HabitLogBackupDto.toEntity() = HabitLogEntity(
    id = id,
    habitId = habitId,
    date = date,
    dateString = dateString,
    isCompleted = isCompleted,
    loggedAt = loggedAt,
    value = value,
    note = note,
    isFrozen = isFrozen
)
fun FocusSessionBackupDto.toEntity() = FocusSessionEntity(id, taskId, durationMinutes, plannedDurationMinutes, sessionType, startedAt, completedAt, notes)
fun TimeBlockBackupDto.toEntity() = TimeBlockEntity(id, title, startTime, durationMinutes, colorHex, date)
fun DayTemplateBackupDto.toEntity() = DayTemplateEntity(id, name)
fun TemplateItemBackupDto.toEntity() = TemplateItemEntity(id, templateId, title, startTime, durationMinutes, isTask)
