package com.personal.daymark.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.personal.daymark.data.local.DaymarkDatabase
import com.personal.daymark.data.local.entity.TaskEntity
import com.personal.daymark.data.repository.HabitRepository
import com.personal.daymark.data.repository.HabitRepositoryImpl
import com.personal.daymark.data.repository.TaskRepository
import com.personal.daymark.data.repository.TaskRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MedicineAndHealthTrackingTest {

    private lateinit var database: DaymarkDatabase
    private lateinit var taskRepository: TaskRepository
    private lateinit var habitRepository: HabitRepository

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
        habitRepository = HabitRepositoryImpl(
            habitDao = database.habitDao()
        )
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun medicineTask_completionDecrementsStockAndCreatesRefillTaskAtThreshold() = runBlocking {
        // 1. Create a medicine task with stock count = 10, refill threshold = 3
        val medTaskId = taskRepository.createTask(
            TaskEntity(
                title = "Metformin",
                priority = 1,
                isCompleted = false,
                isMedicine = true,
                dosage = "500mg",
                medicineForm = "tablet",
                stockCount = 10,
                refillThreshold = 3
            )
        )

        // 2. Complete 8 doses in succession
        // Simulating repeated completions as doses are taken
        var currentTaskId = medTaskId
        for (i in 1..8) {
            val taskBefore = taskRepository.getTaskById(currentTaskId)
            assertNotNull(taskBefore)
            // Mark task as taken
            taskRepository.setTaskCompleted(currentTaskId, true)

            val taskAfter = taskRepository.getTaskById(currentTaskId)
            assertNotNull(taskAfter)
            assertEquals(10 - i, taskAfter?.stockCount)

            // Re-open for the next dose in simulation
            if (i < 8) {
                taskRepository.updateTask(taskAfter!!.copy(isCompleted = false))
            }
        }

        // After 8 completions, stock should be 10 - 8 = 2, which is <= 3 (refillThreshold)
        val finalMedTask = taskRepository.getTaskById(medTaskId)
        assertNotNull(finalMedTask)
        assertEquals(2, finalMedTask?.stockCount)

        // 3. Verify high-priority Refill task was automatically created
        val refillTask = database.taskDao().findPendingTaskByTitle("Refill Metformin")
        assertNotNull("Automatic refill task should be created when stock drops to threshold", refillTask)
        assertEquals("Refill Metformin", refillTask?.title)
        assertEquals(3, refillTask?.priority) // High priority
        assertNotNull(refillTask?.dueDate)

        // Verify due date is ~3 days from now
        val expectedMinDueDate = LocalDate.now().plusDays(2).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val expectedMaxDueDate = LocalDate.now().plusDays(4).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertTrue(refillTask!!.dueDate!! in expectedMinDueDate..expectedMaxDueDate)
    }

    @Test
    fun healthHabit_waterIncrementAndSleepMoodLogging() = runBlocking {
        val today = LocalDate.now()

        // 1. Create a Water habit with target = 8 glasses
        val waterHabitId = habitRepository.createHabit(
            name = "Daily Water",
            frequency = "DAILY",
            targetWeekdays = 127,
            colorHex = "#38BDF8",
            healthMetricType = "water",
            targetValue = 8,
            unit = "glasses"
        )

        // 2. Tap +1 five times
        for (i in 1..5) {
            habitRepository.incrementWater(waterHabitId, today)
        }

        val habitsWithLogs = habitRepository.getActiveHabitsWithLogs().first()
        val waterHabitWithLogs = habitsWithLogs.firstOrNull { it.habit.id == waterHabitId }
        assertNotNull(waterHabitWithLogs)
        val waterLog = waterHabitWithLogs?.logs?.firstOrNull { it.date == today.toEpochDay() }
        assertNotNull(waterLog)
        assertEquals(5f, waterLog?.value)

        // 3. Create a Sleep habit with target = 8 hours and log 7.5 hours
        val sleepHabitId = habitRepository.createHabit(
            name = "Sleep Duration",
            frequency = "DAILY",
            targetWeekdays = 127,
            colorHex = "#A78BFA",
            healthMetricType = "sleep",
            targetValue = 8,
            unit = "hours"
        )
        habitRepository.logHealthMetric(sleepHabitId, 7.5f, null, today)

        val sleepHabitWithLogs = habitRepository.getActiveHabitsWithLogs().first().first { it.habit.id == sleepHabitId }
        val sleepLog = sleepHabitWithLogs.logs.firstOrNull { it.date == today.toEpochDay() }
        assertNotNull(sleepLog)
        assertEquals(7.5f, sleepLog?.value)

        // 4. Create a Mood habit and log mood 5 (😄) with note
        val moodHabitId = habitRepository.createHabit(
            name = "Mood Tracking",
            frequency = "DAILY",
            targetWeekdays = 127,
            colorHex = "#FBBF24",
            healthMetricType = "mood",
            targetValue = 5,
            unit = "rating"
        )
        habitRepository.logHealthMetric(moodHabitId, 5f, "Feeling energised and calm", today)

        val moodHabitWithLogs = habitRepository.getActiveHabitsWithLogs().first().first { it.habit.id == moodHabitId }
        val moodLog = moodHabitWithLogs.logs.firstOrNull { it.date == today.toEpochDay() }
        assertNotNull(moodLog)
        assertEquals(5f, moodLog?.value)
        assertEquals("Feeling energised and calm", moodLog?.note)
    }

    @Test
    fun medicineAdherence_calculatesCorrectStatistics() = runBlocking {
        val now = System.currentTimeMillis()
        val oneWeekAgo = now - (7 * 24 * 60 * 60 * 1000L)

        // Insert 3 scheduled medicines in the range
        val med1 = taskRepository.createTask(
            TaskEntity(
                title = "Vitamin C",
                priority = 1,
                dueDate = now - 1000,
                isCompleted = false,
                isMedicine = true
            )
        )
        val med2 = taskRepository.createTask(
            TaskEntity(
                title = "Omega 3",
                priority = 1,
                dueDate = now - 2000,
                isCompleted = false,
                isMedicine = true
            )
        )

        // Complete 1 of them
        taskRepository.setTaskCompleted(med1, true)

        val (taken, total) = taskRepository.getMedicineAdherenceStats(oneWeekAgo, System.currentTimeMillis() + 60_000)
        assertEquals(1, taken)
        assertEquals(2, total)
    }
}
