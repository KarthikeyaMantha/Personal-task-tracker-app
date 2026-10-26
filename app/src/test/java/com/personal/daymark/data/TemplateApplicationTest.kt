package com.personal.daymark.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.personal.daymark.data.local.DaymarkDatabase
import com.personal.daymark.data.local.entity.TemplateItemEntity
import com.personal.daymark.data.repository.TemplateRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TemplateApplicationTest {

    private lateinit var db: DaymarkDatabase
    private lateinit var templateRepository: TemplateRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, DaymarkDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        templateRepository = TemplateRepositoryImpl(
            dayTemplateDao = db.dayTemplateDao(),
            taskDao = db.taskDao(),
            timeBlockDao = db.timeBlockDao()
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun applyTemplate_createsTasksAndBusyBlocksWithoutDuplicates() = runBlocking {
        // 1. Create a template with 1 Task and 1 Busy Block
        val items = listOf(
            TemplateItemEntity(
                templateId = 0,
                title = "Morning Standup",
                startTime = "09:00",
                durationMinutes = 30,
                isTask = true
            ),
            TemplateItemEntity(
                templateId = 0,
                title = "Lunch Break",
                startTime = "12:00",
                durationMinutes = 60,
                isTask = false
            )
        )
        val templateId = templateRepository.createTemplate("Daily Routine", items)

        val targetDate = LocalDate.of(2026, 10, 27)

        // 2. Apply template to targetDate for the first time
        val firstResult = templateRepository.applyTemplateToDate(templateId, targetDate)
        assertEquals("Daily Routine", firstResult.templateName)
        assertEquals(1, firstResult.tasksAdded)
        assertEquals(1, firstResult.busyBlocksAdded)
        assertEquals(0, firstResult.skippedDuplicates)

        // Verify task and busy block in database
        val busyBlocks = db.timeBlockDao().getTimeBlocksForDateSync("2026-10-27")
        assertEquals(1, busyBlocks.size)
        assertEquals("Lunch Break", busyBlocks[0].title)
        assertEquals("12:00", busyBlocks[0].startTime)

        // 3. Apply the same template AGAIN to the same date -> should skip all duplicates!
        val secondResult = templateRepository.applyTemplateToDate(templateId, targetDate)
        assertEquals(0, secondResult.tasksAdded)
        assertEquals(0, secondResult.busyBlocksAdded)
        assertEquals(2, secondResult.skippedDuplicates)

        // Verify no duplicate records were inserted
        val busyBlocksAfterSecond = db.timeBlockDao().getTimeBlocksForDateSync("2026-10-27")
        assertEquals(1, busyBlocksAfterSecond.size)

        // 4. Apply to a different date -> should add 1 task and 1 busy block
        val nextDate = LocalDate.of(2026, 10, 28)
        val nextResult = templateRepository.applyTemplateToDate(templateId, nextDate)
        assertEquals(1, nextResult.tasksAdded)
        assertEquals(1, nextResult.busyBlocksAdded)
        assertEquals(0, nextResult.skippedDuplicates)

        val busyBlocksNext = db.timeBlockDao().getTimeBlocksForDateSync("2026-10-28")
        assertEquals(1, busyBlocksNext.size)
    }
}
