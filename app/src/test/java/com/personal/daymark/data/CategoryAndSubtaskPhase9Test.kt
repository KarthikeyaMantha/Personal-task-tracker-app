package com.personal.daymark.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.personal.daymark.data.local.DaymarkDatabase
import com.personal.daymark.data.local.entity.CategoryEntity
import com.personal.daymark.data.local.entity.SubtaskEntity
import com.personal.daymark.data.local.entity.TaskEntity
import com.personal.daymark.data.repository.CategoryRepository
import com.personal.daymark.data.repository.CategoryRepositoryImpl
import com.personal.daymark.data.repository.TaskRepository
import com.personal.daymark.data.repository.TaskRepositoryImpl
import kotlinx.coroutines.flow.first
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
class CategoryAndSubtaskPhase9Test {

    private lateinit var database: DaymarkDatabase
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var taskRepository: TaskRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, DaymarkDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        categoryRepository = CategoryRepositoryImpl(
            categoryDao = database.categoryDao(),
            taskDao = database.taskDao()
        )
        taskRepository = TaskRepositoryImpl(
            taskDao = database.taskDao(),
            subtaskDao = database.subtaskDao()
        )
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun seedDefaultCategories_insertsFiveDistinctCalmCategoriesWithoutDuplicates() = runBlocking {
        // Initially empty in memory DB
        val initialCategories = categoryRepository.getAllCategories().first()
        assertTrue(initialCategories.isEmpty())

        // 1. Seed
        categoryRepository.seedDefaultCategoriesIfEmpty()
        val seeded = categoryRepository.getAllCategories().first()
        assertEquals(5, seeded.size)

        val names = seeded.map { it.name }.toSet()
        assertTrue(names.contains("Personal"))
        assertTrue(names.contains("Work"))
        assertTrue(names.contains("Study"))
        assertTrue(names.contains("Health"))
        assertTrue(names.contains("Other"))

        // Colors should be distinct calm tones
        val colors = seeded.map { it.colorHex }.toSet()
        assertEquals(5, colors.size)

        // 2. Call again - should be idempotent (no duplicates)
        categoryRepository.seedDefaultCategoriesIfEmpty()
        val afterSecondSeed = categoryRepository.getAllCategories().first()
        assertEquals(5, afterSecondSeed.size)
    }

    @Test
    fun categoryCRUD_canCreateUpdateAndQueryCategory() = runBlocking {
        val catId = categoryRepository.createCategory(
            name = "Side Project",
            colorHex = "#2D6A4F",
            iconName = null,
            sortOrder = 10
        )
        assertTrue(catId > 0)

        val retrieved = categoryRepository.getCategoryById(catId).first()
        assertNotNull(retrieved)
        assertEquals("Side Project", retrieved!!.name)
        assertEquals("#2D6A4F", retrieved.colorHex)

        // Update name and color
        categoryRepository.updateCategory(
            retrieved.copy(name = "Daymark Dev", colorHex = "#52B788")
        )

        val updated = categoryRepository.getCategoryById(catId).first()
        assertNotNull(updated)
        assertEquals("Daymark Dev", updated!!.name)
        assertEquals("#52B788", updated.colorHex)
    }

    @Test
    fun safeCategoryDeletion_setsAssociatedTasksCategoryToNullWithoutDeletingTasks() = runBlocking {
        // 1. Create a category
        val categoryId = categoryRepository.createCategory(
            name = "Client Alpha",
            colorHex = "#40916C"
        )

        // 2. Create 2 tasks assigned to this category
        val task1Id = taskRepository.createTask(
            TaskEntity(title = "Deliver wireframes", categoryId = categoryId)
        )
        val task2Id = taskRepository.createTask(
            TaskEntity(title = "Review project scope", categoryId = categoryId)
        )

        // Verify tasks initially have the category
        val task1Initial = taskRepository.getTaskWithDetailsById(task1Id).first()
        val task2Initial = taskRepository.getTaskWithDetailsById(task2Id).first()
        assertEquals(categoryId, task1Initial?.task?.categoryId)
        assertEquals(categoryId, task2Initial?.task?.categoryId)
        assertEquals("Client Alpha", task1Initial?.category?.name)

        // 3. Delete the category safely
        categoryRepository.deleteCategoryById(categoryId)

        // 4. Verify category is deleted
        val deletedCategory = categoryRepository.getCategoryById(categoryId).first()
        assertNull(deletedCategory)

        // 5. CRITICAL: Verify tasks remain intact but are now uncategorized (categoryId == null)
        val task1After = taskRepository.getTaskWithDetailsById(task1Id).first()
        val task2After = taskRepository.getTaskWithDetailsById(task2Id).first()

        assertNotNull(task1After)
        assertNotNull(task2After)
        assertEquals("Deliver wireframes", task1After!!.task.title)
        assertEquals("Review project scope", task2After!!.task.title)
        assertNull(task1After.task.categoryId)
        assertNull(task2After.task.categoryId)
        assertNull(task1After.category)
        assertNull(task2After.category)
    }

    @Test
    fun subtasks_progressCalculationAndReactiveCompletionUpdates() = runBlocking {
        // 1. Create task with 3 subtasks
        val taskId = taskRepository.createTask(
            TaskEntity(title = "Ship release v1.0"),
            subtaskTitles = listOf("Run linter", "Assemble release build", "Upload APK")
        )

        // 2. Verify initial ratio: 0 completed out of 3 total ("0/3")
        val initialWithDetails = taskRepository.getTaskWithDetailsById(taskId).first()
        assertNotNull(initialWithDetails)
        assertEquals(3, initialWithDetails!!.subtasks.size)
        val initialCompletedCount = initialWithDetails.subtasks.count { it.isCompleted }
        val initialTotalCount = initialWithDetails.subtasks.size
        assertEquals(0, initialCompletedCount)
        assertEquals(3, initialTotalCount)

        // 3. Check off 1 subtask
        val subtask1 = initialWithDetails.subtasks[0]
        taskRepository.setSubtaskCompleted(subtask1.id, true)

        // 4. Verify reactive flow updates to 1/3
        val updatedWithDetails1 = taskRepository.getTaskWithDetailsById(taskId).first()
        assertNotNull(updatedWithDetails1)
        val completedAfterOne = updatedWithDetails1!!.subtasks.count { it.isCompleted }
        assertEquals(1, completedAfterOne)
        assertEquals(3, updatedWithDetails1.subtasks.size)

        // 5. Complete all remaining subtasks
        val subtask2 = updatedWithDetails1.subtasks[1]
        val subtask3 = updatedWithDetails1.subtasks[2]
        taskRepository.setSubtaskCompleted(subtask2.id, true)
        taskRepository.setSubtaskCompleted(subtask3.id, true)

        val updatedAll = taskRepository.getTaskWithDetailsById(taskId).first()
        assertNotNull(updatedAll)
        assertEquals(3, updatedAll!!.subtasks.count { it.isCompleted })
        assertEquals(3, updatedAll.subtasks.size)

        // 6. Uncheck a subtask -> drops back to 2/3
        taskRepository.setSubtaskCompleted(subtask2.id, false)
        val updatedAfterUncheck = taskRepository.getTaskWithDetailsById(taskId).first()
        assertEquals(2, updatedAfterUncheck!!.subtasks.count { it.isCompleted })

        // 7. Delete 1 subtask -> total count drops to 2
        taskRepository.deleteSubtask(subtask3)
        val updatedAfterDelete = taskRepository.getTaskWithDetailsById(taskId).first()
        assertEquals(2, updatedAfterDelete!!.subtasks.size)
    }

    @Test
    fun syncSubtasks_replacesAndPreservesSubtaskStateCorrectly() = runBlocking {
        val taskId = taskRepository.createTask(
            TaskEntity(title = "Prepare workshop"),
            subtaskTitles = listOf("Draft slides", "Print handouts")
        )

        val initial = taskRepository.getTaskWithDetailsById(taskId).first()
        assertEquals(2, initial!!.subtasks.size)

        // Sync with updated list (add 1 new, keep 1, remove 1)
        val newSubtasks = listOf(
            SubtaskEntity(taskId = taskId, title = "Draft slides (Updated)", isCompleted = true, sortOrder = 0),
            SubtaskEntity(taskId = taskId, title = "Reserve conference room", isCompleted = false, sortOrder = 1)
        )
        taskRepository.syncSubtasks(taskId, newSubtasks)

        val synced = taskRepository.getTaskWithDetailsById(taskId).first()
        assertNotNull(synced)
        assertEquals(2, synced!!.subtasks.size)
        val slideSubtask = synced.subtasks.find { it.title.contains("Draft slides") }
        assertNotNull(slideSubtask)
        assertTrue(slideSubtask!!.isCompleted)
        val roomSubtask = synced.subtasks.find { it.title.contains("Reserve conference") }
        assertNotNull(roomSubtask)
        assertFalse(roomSubtask!!.isCompleted)
    }
}
