package com.personal.daymark.core.voice

import com.personal.daymark.core.notifications.ReminderScheduler
import com.personal.daymark.data.local.entity.TaskEntity
import com.personal.daymark.data.repository.HabitRepository
import com.personal.daymark.data.repository.TaskRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate

sealed class VoiceExecutionResult {
    data class Success(val message: String) : VoiceExecutionResult()
    data class NotFound(val message: String) : VoiceExecutionResult()
    data class Error(val message: String) : VoiceExecutionResult()
}

/**
 * Dispatches confirmed VoiceActions to the Room database and background schedulers.
 */
class VoiceActionExecutor(
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository,
    private val reminderScheduler: ReminderScheduler? = null
) {

    suspend fun execute(action: VoiceAction): VoiceExecutionResult {
        return try {
            when (action) {
                is VoiceAction.CreateTask -> {
                    val newTask = TaskEntity(
                        title = action.title,
                        dueDate = action.dueDate,
                        dueTime = action.dueTimeMinutes,
                        priority = action.priority,
                        categoryId = action.categoryId,
                        recurrenceRule = action.recurrenceRule?.toJson(),
                        isMedicine = action.isMedicine
                    )
                    val newId = taskRepository.createTask(newTask)
                    if (action.dueDate != null && reminderScheduler != null) {
                        reminderScheduler.scheduleReminder(newTask.copy(id = newId))
                    }
                    VoiceExecutionResult.Success("Added task '${action.title}'")
                }

                is VoiceAction.CompleteTask -> {
                    val allTasks = taskRepository.getAllTasksWithDetails().first()
                    val pendingTasks = allTasks.filter { !it.task.isCompleted }
                    val match = pendingTasks.firstOrNull {
                        it.task.title.contains(action.taskQuery, ignoreCase = true)
                    } ?: allTasks.firstOrNull {
                        it.task.title.contains(action.taskQuery, ignoreCase = true)
                    }

                    if (match != null) {
                        if (match.task.recurrenceRule != null) {
                            taskRepository.completeTaskAndGenerateNext(match.task.id)
                        } else {
                            taskRepository.setTaskCompleted(match.task.id, true)
                        }
                        VoiceExecutionResult.Success("Completed '${match.task.title}'")
                    } else {
                        VoiceExecutionResult.NotFound("No task found matching '${action.taskQuery}'")
                    }
                }

                is VoiceAction.LogHabitProgress -> {
                    val allHabits = habitRepository.getAllHabitsWithLogs().first()
                    val match = allHabits.firstOrNull {
                        it.habit.healthMetricType.equals(action.habitQuery, ignoreCase = true) ||
                        it.habit.name.contains(action.habitQuery, ignoreCase = true)
                    }

                    if (match != null) {
                        if (match.habit.healthMetricType == "water") {
                            repeat(action.amount) {
                                habitRepository.incrementWater(match.habit.id, LocalDate.now())
                            }
                            VoiceExecutionResult.Success("Added ${action.amount} ${action.unit ?: "glasses"} of water")
                        } else {
                            habitRepository.logHealthMetric(
                                habitId = match.habit.id,
                                value = action.amount.toFloat(),
                                note = null,
                                localDate = LocalDate.now()
                            )
                            VoiceExecutionResult.Success("Logged ${action.amount} ${action.unit.orEmpty()} for ${match.habit.name}")
                        }
                    } else {
                        VoiceExecutionResult.NotFound("No habit found for '${action.habitQuery}'")
                    }
                }

                is VoiceAction.ToggleHabit -> {
                    val allHabits = habitRepository.getAllHabitsWithLogs().first()
                    val match = allHabits.firstOrNull {
                        it.habit.name.contains(action.habitQuery, ignoreCase = true)
                    }

                    if (match != null) {
                        habitRepository.toggleHabitCompletion(match.habit.id, LocalDate.now())
                        VoiceExecutionResult.Success("Updated habit '${match.habit.name}'")
                    } else {
                        VoiceExecutionResult.NotFound("No habit found matching '${action.habitQuery}'")
                    }
                }

                is VoiceAction.CreateHabit -> {
                    habitRepository.createHabit(
                        name = action.name,
                        note = null,
                        frequency = action.frequency,
                        targetWeekdays = action.targetWeekdays,
                        colorHex = "#FF385C",
                        categoryId = null,
                        healthMetricType = null,
                        targetValue = null,
                        unit = null,
                        reminderTime = null,
                        targetDurationMinutes = null,
                        routineTimeOfDay = action.routineTimeOfDay,
                        isFrozen = false
                    )
                    VoiceExecutionResult.Success("Created habit '${action.name}'")
                }

                is VoiceAction.Unknown -> {
                    VoiceExecutionResult.Error(action.suggestion ?: "Could not understand command")
                }
            }
        } catch (e: Exception) {
            VoiceExecutionResult.Error("Error executing action: ${e.localizedMessage}")
        }
    }
}
