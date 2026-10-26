package com.personal.daymark.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.personal.daymark.data.local.DaymarkDatabase
import com.personal.daymark.data.local.dao.CategoryDao
import com.personal.daymark.data.local.dao.SubtaskDao
import com.personal.daymark.data.local.dao.TaskDao
import com.personal.daymark.data.local.entity.CategoryEntity
import com.personal.daymark.data.local.entity.SubtaskEntity
import com.personal.daymark.data.local.entity.TaskEntity
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
class TaskDaoTest {

    private lateinit var database: DaymarkDatabase
    private lateinit var taskDao: TaskDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var subtaskDao: SubtaskDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, DaymarkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        taskDao = database.taskDao()
        categoryDao = database.categoryDao()
        subtaskDao = database.subtaskDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveTaskWithCategoryAndSubtasks() = runBlocking {
        // 1. Insert Category
        val categoryId = categoryDao.insertCategory(
            CategoryEntity(name = "Deep Work", colorHex = "#1B4332")
        )

        // 2. Insert Task
        val taskId = taskDao.insertTask(
            TaskEntity(
                title = "Design local database architecture",
                description = "Complete Room entity and DAO definitions",
                categoryId = categoryId,
                priority = 3,
                dueDate = 1730000000000L
            )
        )

        // 3. Insert Subtasks
        subtaskDao.insertSubtask(
            SubtaskEntity(taskId = taskId, title = "Define entities", sortOrder = 0)
        )
        subtaskDao.insertSubtask(
            SubtaskEntity(taskId = taskId, title = "Define DAOs", sortOrder = 1)
        )

        // 4. Retrieve with Details
        val taskWithDetails = taskDao.getTaskWithDetailsById(taskId).first()
        assertNotNull(taskWithDetails)
        assertEquals("Design local database architecture", taskWithDetails!!.task.title)
        assertEquals("Deep Work", taskWithDetails.category?.name)
        assertEquals(2, taskWithDetails.subtasks.size)
        assertEquals("Define entities", taskWithDetails.subtasks[0].title)
    }

    @Test
    fun updateTaskCompletionStatusEmitsUpdatedFlow() = runBlocking {
        val taskId = taskDao.insertTask(
            TaskEntity(title = "Read 20 pages")
        )

        val initial = taskDao.getTaskWithDetailsById(taskId).first()
        assertFalse(initial!!.task.isCompleted)

        val now = System.currentTimeMillis()
        taskDao.setTaskCompleted(taskId, true, now)

        val updated = taskDao.getTaskWithDetailsById(taskId).first()
        assertTrue(updated!!.task.isCompleted)
        assertEquals(now, updated.task.completionTimestamp)
    }

    @Test
    fun deleteTaskRemovesFromDatabaseAndCascadesSubtasks() = runBlocking {
        val taskId = taskDao.insertTask(TaskEntity(title = "Task to delete"))
        subtaskDao.insertSubtask(SubtaskEntity(taskId = taskId, title = "Subtask to cascade"))

        taskDao.deleteTaskById(taskId)

        val task = taskDao.getTaskWithDetailsById(taskId).first()
        assertNull(task)

        val subtasks = subtaskDao.getSubtasksForTask(taskId).first()
        assertTrue(subtasks.isEmpty())
    }

    @Test
    fun searchTasksMatchesTitleAndDescription() = runBlocking {
        taskDao.insertTask(TaskEntity(title = "Draft quarterly roadmap", description = "For engineering team"))
        taskDao.insertTask(TaskEntity(title = "Buy groceries", description = "Milk, apples, bread"))

        val searchResults = taskDao.searchTasks("roadmap").first()
        assertEquals(1, searchResults.size)
        assertEquals("Draft quarterly roadmap", searchResults[0].task.title)

        val searchDescResults = taskDao.searchTasks("apples").first()
        assertEquals(1, searchDescResults.size)
        assertEquals("Buy groceries", searchDescResults[0].task.title)
    }
}
