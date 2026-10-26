package com.personal.daymark.core.backup

import com.personal.daymark.data.local.entity.CategoryEntity
import com.personal.daymark.data.local.entity.HabitEntity
import com.personal.daymark.data.local.entity.TaskEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object CsvExportHelper {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())

    fun exportTasksToCsv(
        tasks: List<TaskEntity>,
        categoriesMap: Map<Long, CategoryEntity>
    ): String {
        val sb = StringBuilder()
        // Header
        sb.append("ID,Title,Description,Priority,Due Date,Due Time,Category,Completed,Completion Date,Created At,Recurrence\n")

        for (task in tasks) {
            val categoryName = task.categoryId?.let { categoriesMap[it]?.name } ?: "Uncategorized"
            val priorityName = when (task.priority) {
                1 -> "Low"
                2 -> "Medium"
                3 -> "High"
                4 -> "Urgent"
                else -> "None"
            }

            val dueDateStr = task.dueDate?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
            } ?: ""

            val dueTimeStr = task.dueTime?.let {
                val h = it / 60
                val m = it % 60
                String.format(Locale.getDefault(), "%02d:%02d", h, m)
            } ?: ""

            val completedStr = if (task.isCompleted) "Yes" else "No"

            val completionDateStr = task.completionTimestamp?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(dateTimeFormatter)
            } ?: ""

            val createdAtStr = Instant.ofEpochMilli(task.createdAt).atZone(ZoneId.systemDefault()).format(dateTimeFormatter)

            sb.append(escapeCsvField(task.id.toString())).append(",")
            sb.append(escapeCsvField(task.title)).append(",")
            sb.append(escapeCsvField(task.description ?: "")).append(",")
            sb.append(escapeCsvField(priorityName)).append(",")
            sb.append(escapeCsvField(dueDateStr)).append(",")
            sb.append(escapeCsvField(dueTimeStr)).append(",")
            sb.append(escapeCsvField(categoryName)).append(",")
            sb.append(escapeCsvField(completedStr)).append(",")
            sb.append(escapeCsvField(completionDateStr)).append(",")
            sb.append(escapeCsvField(createdAtStr)).append(",")
            sb.append(escapeCsvField(task.recurrenceRule ?: ""))
            sb.append("\n")
        }

        return sb.toString()
    }

    fun exportHabitsToCsv(
        habits: List<HabitEntity>,
        categoriesMap: Map<Long, CategoryEntity>
    ): String {
        val sb = StringBuilder()
        sb.append("ID,Name,Note,Frequency,Schedule,Color,Category,Archived,Created At\n")

        for (habit in habits) {
            val categoryName = habit.categoryId?.let { categoriesMap[it]?.name } ?: "Uncategorized"
            val scheduleStr = formatWeekdaysBitmask(habit.targetWeekdays)
            val archivedStr = if (habit.isArchived) "Yes" else "No"
            val createdAtStr = Instant.ofEpochMilli(habit.createdAt).atZone(ZoneId.systemDefault()).format(dateTimeFormatter)

            sb.append(escapeCsvField(habit.id.toString())).append(",")
            sb.append(escapeCsvField(habit.name)).append(",")
            sb.append(escapeCsvField(habit.note ?: "")).append(",")
            sb.append(escapeCsvField(habit.frequency)).append(",")
            sb.append(escapeCsvField(scheduleStr)).append(",")
            sb.append(escapeCsvField(habit.colorHex)).append(",")
            sb.append(escapeCsvField(categoryName)).append(",")
            sb.append(escapeCsvField(archivedStr)).append(",")
            sb.append(escapeCsvField(createdAtStr))
            sb.append("\n")
        }

        return sb.toString()
    }

    fun escapeCsvField(value: String): String {
        if (!value.contains(",") && !value.contains("\"") && !value.contains("\n") && !value.contains("\r")) {
            return value
        }
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    private fun formatWeekdaysBitmask(bitmask: Int): String {
        if (bitmask == 127) return "Every Day"
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val active = mutableListOf<String>()
        for (i in 0..6) {
            if ((bitmask and (1 shl i)) != 0) {
                active.add(days[i])
            }
        }
        return if (active.isEmpty()) "None" else active.joinToString(", ")
    }
}
