package com.personal.daymark.core.recurrence

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.personal.daymark.data.local.DaymarkDatabase
import com.personal.daymark.data.local.dao.SubtaskDao
import com.personal.daymark.data.local.dao.TaskDao
import com.personal.daymark.data.local.entity.TaskEntity
import com.personal.daymark.data.repository.TaskRepository
import com.personal.daymark.data.repository.TaskRepositoryImpl
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecurrenceEngineTest {

    private lateinit var database: DaymarkDatabase
    private lateinit var taskDao: TaskDao
    private lateinit var subtaskDao: SubtaskDao
    private lateinit var repository: TaskRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, DaymarkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        taskDao = database.taskDao()
        subtaskDao = database.subtaskDao()
        repository = TaskRepositoryImpl(taskDao, subtaskDao, Dispatchers.Unconfined)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun testRecurrenceRuleSerializationAndDisplay() {
        val dailyRule = RecurrenceRule(frequency = RecurrenceFrequency.DAILY)
        val dailyJson = dailyRule.toJson()
        val parsedDaily = RecurrenceRule.fromJson(dailyJson)
        assertNotNull(parsedDaily)
        assertEquals(RecurrenceFrequency.DAILY, parsedDaily!!.frequency)
        assertEquals(1, parsedDaily.interval)
        assertEquals("Daily", parsedDaily.toDisplayString())

        val weeklyRule = RecurrenceRule(
            frequency = RecurrenceFrequency.WEEKLY,
            interval = 1,
            daysOfWeek = listOf(1, 3, 5) // Mon, Wed, Fri
        )
        val weeklyJson = weeklyRule.toJson()
        val parsedWeekly = RecurrenceRule.fromJson(weeklyJson)
        assertNotNull(parsedWeekly)
        assertEquals(listOf(1, 3, 5), parsedWeekly!!.daysOfWeek)
        assertEquals("Weekly on Mon, Wed, Fri", parsedWeekly.toDisplayString())

        val weekdaysRule = RecurrenceRule(frequency = RecurrenceFrequency.WEEKDAYS)
        assertEquals("Every weekday (Mon–Fri)", weekdaysRule.toDisplayString())

        val monthlyRule = RecurrenceRule(frequency = RecurrenceFrequency.MONTHLY)
        assertEquals("Monthly", monthlyRule.toDisplayString())
    }

    @Test
    fun testDailyRecurrenceCalculation() {
        val zoneId = ZoneId.of("UTC")
        val baseDate = LocalDate.of(2026, 10, 26) // Monday
        val baseMillis = baseDate.atStartOfDay(zoneId).toInstant().toEpochMilli()

        val rule = RecurrenceRule(frequency = RecurrenceFrequency.DAILY, interval = 1)
        val nextMillis = RecurrenceHelper.calculateNextOccurrence(
            currentDueDateMillis = baseMillis,
            dueTimeMinutes = 540,
            rule = rule,
            fromDateMillis = baseMillis,
            zoneId = zoneId
        )

        assertNotNull(nextMillis)
        val nextDate = Instant.ofEpochMilli(nextMillis!!).atZone(zoneId).toLocalDate()
        assertEquals(LocalDate.of(2026, 10, 27), nextDate)
    }

    @Test
    fun testWeekdayRecurrenceCalculation_advancesFridayToMonday() {
        val zoneId = ZoneId.of("UTC")
        val friday = LocalDate.of(2026, 10, 30) // Friday
        val fridayMillis = friday.atStartOfDay(zoneId).toInstant().toEpochMilli()

        val rule = RecurrenceRule(frequency = RecurrenceFrequency.WEEKDAYS)
        val nextMillis = RecurrenceHelper.calculateNextOccurrence(
            currentDueDateMillis = fridayMillis,
            dueTimeMinutes = null,
            rule = rule,
            fromDateMillis = fridayMillis,
            zoneId = zoneId
        )

        assertNotNull(nextMillis)
        val nextDate = Instant.ofEpochMilli(nextMillis!!).atZone(zoneId).toLocalDate()
        assertEquals(LocalDate.of(2026, 11, 2), nextDate) // Monday Nov 2
        assertEquals(DayOfWeek.MONDAY, nextDate.dayOfWeek)
    }

    @Test
    fun testWeeklyRecurrenceCalculation_cyclesThroughSelectedDays() {
        val zoneId = ZoneId.of("UTC")
        // 2026-10-26 is Monday
        val monday = LocalDate.of(2026, 10, 26)
        val mondayMillis = monday.atStartOfDay(zoneId).toInstant().toEpochMilli()

        val rule = RecurrenceRule(
            frequency = RecurrenceFrequency.WEEKLY,
            interval = 1,
            daysOfWeek = listOf(1, 3, 5) // Mon, Wed, Fri
        )

        // Monday -> Next should be Wednesday Oct 28
        val wedMillis = RecurrenceHelper.calculateNextOccurrence(
            currentDueDateMillis = mondayMillis,
            dueTimeMinutes = null,
            rule = rule,
            fromDateMillis = mondayMillis,
            zoneId = zoneId
        )
        assertNotNull(wedMillis)
        val wedDate = Instant.ofEpochMilli(wedMillis!!).atZone(zoneId).toLocalDate()
        assertEquals(LocalDate.of(2026, 10, 28), wedDate)

        // Friday Oct 30 -> Next should wrap to next Monday Nov 2
        val friMillis = LocalDate.of(2026, 10, 30).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val nextMonMillis = RecurrenceHelper.calculateNextOccurrence(
            currentDueDateMillis = friMillis,
            dueTimeMinutes = null,
            rule = rule,
            fromDateMillis = friMillis,
            zoneId = zoneId
        )
        assertNotNull(nextMonMillis)
        val nextMonDate = Instant.ofEpochMilli(nextMonMillis!!).atZone(zoneId).toLocalDate()
        assertEquals(LocalDate.of(2026, 11, 2), nextMonDate)
    }

    @Test
    fun testMonthEndBoundaryRecurrence() {
        val zoneId = ZoneId.of("UTC")
        val jan31 = LocalDate.of(2026, 1, 31)
        val jan31Millis = jan31.atStartOfDay(zoneId).toInstant().toEpochMilli()

        val rule = RecurrenceRule(frequency = RecurrenceFrequency.MONTHLY, interval = 1)
        val nextMillis = RecurrenceHelper.calculateNextOccurrence(
            currentDueDateMillis = jan31Millis,
            dueTimeMinutes = null,
            rule = rule,
            fromDateMillis = jan31Millis,
            zoneId = zoneId
        )

        assertNotNull(nextMillis)
        val nextDate = Instant.ofEpochMilli(nextMillis!!).atZone(zoneId).toLocalDate()
        assertEquals(LocalDate.of(2026, 2, 28), nextDate) // Clamped to Feb 28 in 2026 (non-leap)
    }

    @Test
    fun testDailyRecurringTaskCompletion_generatesTomorrowInstance() = runBlocking {
        val today = LocalDate.of(2026, 10, 26)
        val todayMillis = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val rule = RecurrenceRule(frequency = RecurrenceFrequency.DAILY)
        val seriesId = UUID.randomUUID().toString()

        val initialTask = TaskEntity(
            title = "Morning Run",
            dueDate = todayMillis,
            dueTime = 420, // 7:00 AM
            priority = 2,
            recurrenceRule = rule.toJson(),
            seriesId = seriesId,
            isCompleted = false
        )
        val taskId = repository.createTask(initialTask)

        // Verify task inserted
        val storedTask = repository.getTaskById(taskId)
        assertNotNull(storedTask)
        assertFalse(storedTask!!.isCompleted)

        // Complete the recurring task
        val nextTask = repository.completeTaskAndGenerateNext(taskId)

        // Verify completion of original task
        val updatedInitialTask = repository.getTaskById(taskId)
        assertNotNull(updatedInitialTask)
        assertTrue(updatedInitialTask!!.isCompleted)
        assertNotNull(updatedInitialTask.completionTimestamp)

        // Verify exactly one new task appeared for tomorrow
        assertNotNull(nextTask)
        assertEquals("Morning Run", nextTask!!.title)
        assertEquals(seriesId, nextTask.seriesId)
        assertFalse(nextTask.isCompleted)
        assertEquals(420, nextTask.dueTime)

        val nextTaskDate = Instant.ofEpochMilli(nextTask.dueDate!!)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        assertEquals(today.plusDays(1), nextTaskDate)

        // Verify database holds exactly 2 tasks (1 completed, 1 active)
        val tasksInSeries = repository.getTasksBySeriesId(seriesId)
        assertEquals(2, tasksInSeries.size)
        assertEquals(1, tasksInSeries.count { it.isCompleted })
        assertEquals(1, tasksInSeries.count { !it.isCompleted })
    }

    @Test
    fun testDuplicatePrevention_idempotentCompletion() = runBlocking {
        val todayMillis = System.currentTimeMillis()
        val rule = RecurrenceRule(frequency = RecurrenceFrequency.DAILY)
        val seriesId = UUID.randomUUID().toString()

        val task = TaskEntity(
            title = "Daily Journaling",
            dueDate = todayMillis,
            recurrenceRule = rule.toJson(),
            seriesId = seriesId,
            isCompleted = false
        )
        val taskId = repository.createTask(task)

        // First completion generates next occurrence
        val firstResult = repository.completeTaskAndGenerateNext(taskId)
        assertNotNull(firstResult)

        // Second completion trigger (e.g. rapid duplicate click or repeat notification action)
        val secondResult = repository.completeTaskAndGenerateNext(taskId)
        assertNull("Duplicate trigger must not spawn extra instances", secondResult)

        // Verify series count remains exactly 2 (1 completed, 1 active)
        val allTasks = repository.getTasksBySeriesId(seriesId)
        assertEquals(2, allTasks.size)
        assertEquals(1, allTasks.count { !it.isCompleted })
    }

    @Test
    fun testDeleteRecurringSeries_removesAllInstancesAndReturnsIds() = runBlocking {
        val rule = RecurrenceRule(frequency = RecurrenceFrequency.DAILY)
        val seriesId = UUID.randomUUID().toString()

        val task1 = TaskEntity(
            title = "Drink Water",
            dueDate = System.currentTimeMillis(),
            recurrenceRule = rule.toJson(),
            seriesId = seriesId,
            isCompleted = false
        )
        val id1 = repository.createTask(task1)

        // Complete task1 to spawn task2
        val task2 = repository.completeTaskAndGenerateNext(id1)
        assertNotNull(task2)

        val initialSeries = repository.getTasksBySeriesId(seriesId)
        assertEquals(2, initialSeries.size)

        // Delete entire series
        val deletedIds = repository.deleteTaskSeries(seriesId)
        assertEquals(2, deletedIds.size)
        assertTrue(deletedIds.contains(id1))
        assertTrue(deletedIds.contains(task2!!.id))

        // Verify 0 tasks remain for this series in DB
        val remainingSeries = repository.getTasksBySeriesId(seriesId)
        assertTrue(remainingSeries.isEmpty())
    }

    @Test
    fun testDeleteTaskOccurrence_preservesSeriesContinuation() = runBlocking {
        val today = LocalDate.of(2026, 10, 26)
        val todayMillis = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val rule = RecurrenceRule(frequency = RecurrenceFrequency.DAILY)
        val seriesId = UUID.randomUUID().toString()

        val task = TaskEntity(
            title = "Evening Meditation",
            dueDate = todayMillis,
            recurrenceRule = rule.toJson(),
            seriesId = seriesId,
            isCompleted = false
        )
        val taskId = repository.createTask(task)

        // User skips / deletes today's occurrence only
        val nextOccurrence = repository.deleteTaskOccurrence(taskId)

        // Verify today's task was deleted
        assertNull(repository.getTaskById(taskId))

        // Verify the series advanced to tomorrow's occurrence
        assertNotNull(nextOccurrence)
        assertEquals(seriesId, nextOccurrence!!.seriesId)
        assertFalse(nextOccurrence.isCompleted)

        val nextDate = Instant.ofEpochMilli(nextOccurrence.dueDate!!)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        assertEquals(today.plusDays(1), nextDate)
    }
}
