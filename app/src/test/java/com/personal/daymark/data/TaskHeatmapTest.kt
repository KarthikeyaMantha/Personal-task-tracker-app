package com.personal.daymark.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.personal.daymark.data.local.DaymarkDatabase
import com.personal.daymark.data.local.entity.TaskEntity
import com.personal.daymark.data.repository.TaskRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TaskHeatmapTest {

    private lateinit var db: DaymarkDatabase
    private lateinit var repository: TaskRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, DaymarkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TaskRepositoryImpl(db.taskDao(), db.subtaskDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun getCompletedTaskCountsPerDay_accuratelyGroupsCompletionsByDay() = runBlocking {
        val zoneId = ZoneId.systemDefault()
        val day1 = LocalDate.of(2026, 10, 20)
        val day2 = LocalDate.of(2026, 10, 21)
        val day3 = LocalDate.of(2026, 10, 22)

        val day1Millis = day1.atTime(10, 30).atZone(zoneId).toInstant().toEpochMilli()
        val day2Millis1 = day2.atTime(9, 0).atZone(zoneId).toInstant().toEpochMilli()
        val day2Millis2 = day2.atTime(14, 15).atZone(zoneId).toInstant().toEpochMilli()
        val day3Millis = day3.atTime(16, 0).atZone(zoneId).toInstant().toEpochMilli()

        // 1 completed on Day 1
        db.taskDao().insertTask(
            TaskEntity(
                title = "Day 1 Task",
                isCompleted = true,
                completionTimestamp = day1Millis,
                dueDate = day1Millis,
                createdAt = day1Millis
            )
        )

        // 2 completed on Day 2
        db.taskDao().insertTask(
            TaskEntity(
                title = "Day 2 Task 1",
                isCompleted = true,
                completionTimestamp = day2Millis1,
                dueDate = day2Millis1,
                createdAt = day2Millis1
            )
        )
        db.taskDao().insertTask(
            TaskEntity(
                title = "Day 2 Task 2",
                isCompleted = true,
                completionTimestamp = day2Millis2,
                dueDate = day2Millis2,
                createdAt = day2Millis2
            )
        )

        // 1 uncompleted task on Day 2 (should NOT be counted in completion heatmap!)
        db.taskDao().insertTask(
            TaskEntity(
                title = "Day 2 Incomplete Task",
                isCompleted = false,
                completionTimestamp = null,
                dueDate = day2Millis1,
                createdAt = day2Millis1
            )
        )

        // 1 completed on Day 3
        db.taskDao().insertTask(
            TaskEntity(
                title = "Day 3 Task",
                isCompleted = true,
                completionTimestamp = day3Millis,
                dueDate = day3Millis,
                createdAt = day3Millis
            )
        )

        val counts = repository.getAllCompletedTaskCountsPerDay().first()
        val countMap = counts.associate { it.dayDate to it.count }

        // Verify counts per day
        assertEquals(1, countMap[day1.toString()])
        assertEquals(2, countMap[day2.toString()])
        assertEquals(1, countMap[day3.toString()])
    }
}
