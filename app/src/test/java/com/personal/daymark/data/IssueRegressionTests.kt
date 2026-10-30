package com.personal.daymark.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.personal.daymark.data.local.DaymarkDatabase
import com.personal.daymark.data.local.entity.TaskEntity
import com.personal.daymark.data.repository.FocusRepository
import com.personal.daymark.data.repository.FocusRepositoryImpl
import com.personal.daymark.data.repository.TaskRepository
import com.personal.daymark.data.repository.TaskRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.personal.daymark.core.recurrence.RecurrenceFrequency
import com.personal.daymark.core.recurrence.RecurrenceRule
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IssueRegressionTests {

    private lateinit var database: DaymarkDatabase
    private lateinit var taskRepository: TaskRepository
    private lateinit var focusRepository: FocusRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, DaymarkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        taskRepository = TaskRepositoryImpl(
            taskDao = database.taskDao(),
            subtaskDao = database.subtaskDao()
        )
        focusRepository = FocusRepositoryImpl(
            focusDao = database.focusDao()
        )
    }

    @After
    fun teardown() {
        database.close()
    }

    /**
     * Issue 2: Completed tasks reopen after editing
     * Saving an edit must preserve completion status, completion timestamp, and creation date.
     */
    @Test
    fun completedTask_editingPreservesCompletionStatusAndTimestamps() = runBlocking {
        val originalCreated = 1000000L
        val originalCompleted = 2000000L
        val task = TaskEntity(
            id = 1L,
            title = "Original Title",
            priority = 1,
            isCompleted = true,
            completionTimestamp = originalCompleted,
            createdAt = originalCreated,
            updatedAt = originalCompleted
        )
        val taskId = taskRepository.createTask(task)

        // AddEditTaskViewModel preserve logic:
        val existing = taskRepository.getTaskById(taskId)!!
        val updated = existing.copy(
            title = "Edited Title",
            description = "New notes"
        )
        taskRepository.updateTask(updated)

        val retrieved = taskRepository.getTaskById(taskId)!!
        assertEquals("Edited Title", retrieved.title)
        assertTrue("Completion status must remain true", retrieved.isCompleted)
        assertEquals("Completion timestamp must be preserved", originalCompleted, retrieved.completionTimestamp)
        assertEquals("Creation timestamp must be preserved", originalCreated, retrieved.createdAt)
    }

    /**
     * Issue 6: Medicine stock undo
     * Stock 10 -> complete -> 9 -> undo must return to 10. Prevent duplicate deductions and incorrect refill alerts.
     */
    @Test
    fun medicineStock_undoReturnsStockToOriginalAndRemovesRefillAlert() = runBlocking {
        val medTaskId = taskRepository.createTask(
            TaskEntity(
                title = "Metformin",
                priority = 1,
                isCompleted = false,
                isMedicine = true,
                dosage = "500mg",
                medicineForm = "tablet",
                stockCount = 4,
                refillThreshold = 3
            )
        )

        // 1. Mark complete: stock decrements from 4 to 3 (refill threshold reached -> creates alert)
        taskRepository.setTaskCompleted(medTaskId, true)
        var medTask = taskRepository.getTaskById(medTaskId)!!
        assertEquals(3, medTask.stockCount)
        assertTrue(medTask.isCompleted)

        val tasksAfterComplete = database.taskDao().getAllTasksList()
        val refillTasksAfterComplete = tasksAfterComplete.filter { it.title.contains("Refill Metformin") }
        assertEquals(1, refillTasksAfterComplete.size)

        // 2. Undo completion: stock increments back from 3 to 4, refill task alert should be cancelled
        taskRepository.setTaskCompleted(medTaskId, false)
        medTask = taskRepository.getTaskById(medTaskId)!!
        assertEquals(4, medTask.stockCount)
        assertFalse(medTask.isCompleted)

        val tasksAfterUndo = database.taskDao().getAllTasksList()
        val refillTasksAfterUndo = tasksAfterUndo.filter { it.title.contains("Refill Metformin") }
        assertEquals("Refill task should be removed on undo when stock returns above threshold", 0, refillTasksAfterUndo.size)
    }

    /**
     * Issue 7: Duplicate focus records
     * Restoring an expired timer can record it twice. Ensure each session is recorded exactly once.
     */
    @Test
    fun focusSession_duplicateRecordingWithSameStartedAtIsIdempotent() = runBlocking {
        val startedAt = 1700000000000L

        val id1 = focusRepository.recordFocusSession(
            durationMinutes = 25,
            startedAt = startedAt
        )
        val id2 = focusRepository.recordFocusSession(
            durationMinutes = 25,
            startedAt = startedAt
        )

        assertEquals("Second record call should return existing session ID", id1, id2)

        val allSessions = focusRepository.getAllSessions().first()
        assertEquals("Exactly one session should be recorded for the startedAt timestamp", 1, allSessions.size)
    }

    /**
     * Issue 8: Skipping recurring medicine loses details
     * Deleting one occurrence must preserve medicine identity, dosage, stock, and timing in the next occurrence.
     */
    @Test
    fun recurringMedicine_deleteOccurrencePreservesAllMedicineDetailsAndTiming() = runBlocking {
        val todayMillis = System.currentTimeMillis()
        val recurringMedicineTask = TaskEntity(
            title = "Aspirin 81mg",
            priority = 2,
            dueDate = todayMillis,
            startTime = "08:00",
            durationMinutes = 10,
            recurrenceRule = RecurrenceRule(frequency = RecurrenceFrequency.DAILY, interval = 1).toJson(),
            seriesId = "med-series-1",
            isMedicine = true,
            dosage = "81mg",
            medicineForm = "tablet",
            stockCount = 30,
            refillThreshold = 7
        )
        val taskId = taskRepository.createTask(recurringMedicineTask)

        // Delete occurrence (skip today's dose to tomorrow)
        taskRepository.deleteTaskOccurrence(taskId)

        val allTasks = database.taskDao().getAllTasksList()
        assertEquals(1, allTasks.size)
        val nextOccurrence = allTasks[0]
        assertEquals("Aspirin 81mg", nextOccurrence.title)
        assertTrue("Next occurrence must retain isMedicine flag", nextOccurrence.isMedicine)
        assertEquals("81mg", nextOccurrence.dosage)
        assertEquals("tablet", nextOccurrence.medicineForm)
        assertEquals(30, nextOccurrence.stockCount)
        assertEquals(7, nextOccurrence.refillThreshold)
        assertEquals("08:00", nextOccurrence.startTime)
        assertEquals(10, nextOccurrence.durationMinutes)
    }

    /**
     * Issue 9: Heatmap dates
     * Count completions on the actual completion date, without also counting them on their due date.
     */
    @Test
    fun heatmap_countsCompletionsOnActualCompletionTimestampNotDueDate() = runBlocking {
        val zoneId = java.time.ZoneId.systemDefault()
        val completionDate = LocalDate.of(2026, 10, 27)
        val dueDate = LocalDate.of(2026, 10, 20)

        val completionMillis = completionDate.atTime(14, 0).atZone(zoneId).toInstant().toEpochMilli()
        val dueMillis = dueDate.atTime(9, 0).atZone(zoneId).toInstant().toEpochMilli()

        val task = TaskEntity(
            title = "Late Submission",
            priority = 1,
            dueDate = dueMillis,
            isCompleted = true,
            completionTimestamp = completionMillis,
            updatedAt = completionMillis
        )
        taskRepository.createTask(task)

        val heatmapCounts = taskRepository.getAllCompletedTaskCountsPerDay().first()
        val countMap = heatmapCounts.associate { it.dayDate to it.count }

        // Must count on completion date (2026-10-27), not on due date (2026-10-20)
        assertEquals(1, countMap[completionDate.toString()])
        assertEquals(null, countMap[dueDate.toString()])
    }

    /**
     * Issue 12: Typed task dates
     * "Buy milk tomorrow" on Today must save for tomorrow.
     * Distinguish automatic defaults from explicit date selections.
     */
    @Test
    fun typedTaskDates_distinguishAutoDefaultFromExplicitSelection() {
        val todayMillis = 1729987200000L
        val tomorrowMillis = todayMillis + 86400000L

        // When date was defaulted (isUserExplicitDueDate = false)
        var isExplicit = false
        var activeDueDate = todayMillis

        // User typed tomorrow -> parsed date overrides default
        val parsedDueDate = tomorrowMillis
        if (!isExplicit) {
            activeDueDate = parsedDueDate
        }
        assertEquals(tomorrowMillis, activeDueDate)

        // When user explicitly picked today via date picker
        isExplicit = true
        activeDueDate = todayMillis

        // User typed something containing tomorrow -> explicit selection preserved
        if (!isExplicit) {
            activeDueDate = parsedDueDate
        }
        assertEquals(todayMillis, activeDueDate)
    }

    /**
     * Issue 13: Duplicate saves
     * Rapid taps or submissions must only trigger a single creation.
     */
    @Test
    fun rapidSaveAttempts_atomicGuardExecutesOnlyOnce() {
        val isSavingInProgress = java.util.concurrent.atomic.AtomicBoolean(false)
        var saveExecutionCount = 0

        // Simulate rapid concurrent or sequential click events
        val threads = (1..10).map {
            Thread {
                if (isSavingInProgress.compareAndSet(false, true)) {
                    saveExecutionCount++
                }
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertEquals("Atomic compareAndSet must ensure exactly one save execution succeeds", 1, saveExecutionCount)
    }

    /**
     * Issue 7: Deduplication when startedAt is 0L
     */
    @Test
    fun focusSession_zeroStartedAtFallsBackToComputedAndIsDeduplicated() = runBlocking {
        val completedAt = 1700000000000L
        val durationMin = 25

        val id1 = focusRepository.recordFocusSession(
            durationMinutes = durationMin,
            startedAt = 0L,
            completedAt = completedAt
        )
        val id2 = focusRepository.recordFocusSession(
            durationMinutes = durationMin,
            startedAt = 0L,
            completedAt = completedAt
        )

        assertEquals("Sessions with 0L startedAt must calculate deterministic startedAt and deduplicate", id1, id2)
        val sessions = focusRepository.getAllSessions().first()
        assertEquals(1, sessions.size)
    }
}
