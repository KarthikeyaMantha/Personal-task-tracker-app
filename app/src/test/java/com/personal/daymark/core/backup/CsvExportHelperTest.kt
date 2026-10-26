package com.personal.daymark.core.backup

import com.personal.daymark.data.local.entity.CategoryEntity
import com.personal.daymark.data.local.entity.HabitEntity
import com.personal.daymark.data.local.entity.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class CsvExportHelperTest {

    @Test
    fun escapeCsvField_escapesCommasAndQuotesProperly() {
        // Plain text: no change
        assertEquals("Simple text", CsvExportHelper.escapeCsvField("Simple text"))

        // Field with comma: wrapped in quotes
        assertEquals("\"Apples, Bananas\"", CsvExportHelper.escapeCsvField("Apples, Bananas"))

        // Field with quotes: quotes doubled and wrapped
        assertEquals("\"He said \"\"Hello\"\" to me\"", CsvExportHelper.escapeCsvField("He said \"Hello\" to me"))

        // Field with newline: wrapped in quotes
        assertEquals("\"Line 1\nLine 2\"", CsvExportHelper.escapeCsvField("Line 1\nLine 2"))
    }

    @Test
    fun exportTasksToCsv_formatsHeadersAndRecordsAccurately() {
        val category = CategoryEntity(id = 10L, name = "Finance", colorHex = "#2D6A4F")
        val categoriesMap = mapOf(10L to category)

        val taskDate = LocalDate.of(2026, 11, 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val tasks = listOf(
            TaskEntity(
                id = 1L,
                title = "Pay bills, and rent",
                description = "Ensure \"bank account\" is funded",
                priority = 4, // Urgent
                dueDate = taskDate,
                dueTime = 840, // 14:00
                categoryId = 10L,
                isCompleted = false
            ),
            TaskEntity(
                id = 2L,
                title = "Review investments",
                priority = 1, // Low
                isCompleted = true,
                completionTimestamp = taskDate + 3600000L
            )
        )

        val csv = CsvExportHelper.exportTasksToCsv(tasks, categoriesMap)

        val lines = csv.trim().split("\n")
        assertEquals(3, lines.size) // Header + 2 rows

        // Check header
        assertEquals("ID,Title,Description,Priority,Due Date,Due Time,Category,Completed,Completion Date,Created At,Recurrence", lines[0])

        // Check Row 1 (with escaped title, description, and urgency)
        assertTrue(lines[1].contains("\"Pay bills, and rent\""))
        assertTrue(lines[1].contains("\"Ensure \"\"bank account\"\" is funded"))
        assertTrue(lines[1].contains("Urgent"))
        assertTrue(lines[1].contains("Finance"))
        assertTrue(lines[1].contains("2026-11-01"))
        assertTrue(lines[1].contains("14:00"))
        assertTrue(lines[1].contains("No"))

        // Check Row 2 (completed, Low)
        assertTrue(lines[2].contains("Review investments"))
        assertTrue(lines[2].contains("Low"))
        assertTrue(lines[2].contains("Uncategorized"))
        assertTrue(lines[2].contains("Yes"))
    }

    @Test
    fun exportHabitsToCsv_formatsSchedulesAndColorsAccurately() {
        val habits = listOf(
            HabitEntity(
                id = 1L,
                name = "Morning Stretch",
                note = "10 minutes",
                frequency = "DAILY",
                targetWeekdays = 127, // Every Day
                colorHex = "#1B4332",
                isArchived = false
            ),
            HabitEntity(
                id = 2L,
                name = "Weekend Jog",
                frequency = "WEEKLY",
                targetWeekdays = 96, // Sat (32) + Sun (64)
                colorHex = "#2D6A4F",
                isArchived = true
            )
        )

        val csv = CsvExportHelper.exportHabitsToCsv(habits, emptyMap())

        val lines = csv.trim().split("\n")
        assertEquals(3, lines.size)
        assertEquals("ID,Name,Note,Frequency,Schedule,Color,Category,Archived,Created At", lines[0])

        assertTrue(lines[1].contains("Morning Stretch"))
        assertTrue(lines[1].contains("Every Day"))
        assertTrue(lines[1].contains("No"))

        assertTrue(lines[2].contains("Weekend Jog"))
        assertTrue(lines[2].contains("\"Sat, Sun\""))
        assertTrue(lines[2].contains("Yes"))
    }
}
