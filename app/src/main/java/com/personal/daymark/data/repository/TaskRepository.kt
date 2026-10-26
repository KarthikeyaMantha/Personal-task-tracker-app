package com.personal.daymark.data.repository

import com.personal.daymark.data.local.dao.SubtaskDao
import com.personal.daymark.data.local.dao.TaskDao
import com.personal.daymark.data.local.entity.SubtaskEntity
import com.personal.daymark.data.local.entity.TaskEntity
import com.personal.daymark.data.local.entity.TaskWithSubtasksAndCategory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

interface TaskRepository {
    fun getAllTasksWithDetails(): Flow<List<TaskWithSubtasksAndCategory>>
    fun getTaskWithDetailsById(id: Long): Flow<TaskWithSubtasksAndCategory?>
    fun getTasksForToday(startOfDay: Long, endOfDay: Long): Flow<List<TaskWithSubtasksAndCategory>>
    fun getTasksInDateRange(startDate: Long, endDate: Long): Flow<List<TaskWithSubtasksAndCategory>>
    fun getTasksByCategory(categoryId: Long): Flow<List<TaskWithSubtasksAndCategory>>
    fun getTasksByCompletion(isCompleted: Boolean): Flow<List<TaskWithSubtasksAndCategory>>
    fun searchTasks(query: String): Flow<List<TaskWithSubtasksAndCategory>>
    suspend fun createTask(task: TaskEntity, subtaskTitles: List<String> = emptyList()): Long
    suspend fun updateTask(task: TaskEntity)
    suspend fun deleteTask(task: TaskEntity)
    suspend fun deleteTaskById(id: Long)
    suspend fun setTaskCompleted(id: Long, isCompleted: Boolean)
    suspend fun createSubtask(taskId: Long, title: String): Long
    suspend fun setSubtaskCompleted(subtaskId: Long, isCompleted: Boolean)
    suspend fun updateSubtask(subtask: SubtaskEntity)
    suspend fun deleteSubtask(subtask: SubtaskEntity)
    fun getSubtasksForTask(taskId: Long): Flow<List<SubtaskEntity>>
    suspend fun syncSubtasks(taskId: Long, subtasks: List<SubtaskEntity>)
    suspend fun getTaskById(id: Long): TaskEntity?
    suspend fun getPendingTasksWithDueDate(): List<TaskEntity>
    suspend fun completeTaskAndGenerateNext(taskId: Long): TaskEntity?
    suspend fun deleteTaskSeries(seriesId: String): List<Long>
    suspend fun deleteTaskOccurrence(taskId: Long): TaskEntity?
    suspend fun updateEntireSeries(seriesId: String, updatedTask: TaskEntity)
    suspend fun getTasksBySeriesId(seriesId: String): List<TaskEntity>
    fun getCompletedTasksCountInRange(startMillis: Long, endMillis: Long): Flow<Int>
    fun getPendingTasksCount(): Flow<Int>
    fun getOverdueTasksCount(nowMillis: Long = System.currentTimeMillis()): Flow<Int>
    fun getTotalTasksCount(): Flow<Int>
    fun getCompletedTasksInRange(startMillis: Long, endMillis: Long): Flow<List<TaskEntity>>
}

class TaskRepositoryImpl(
    private val taskDao: TaskDao,
    private val subtaskDao: SubtaskDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : TaskRepository {

    override fun getAllTasksWithDetails(): Flow<List<TaskWithSubtasksAndCategory>> {
        return taskDao.getAllTasksWithDetails().flowOn(ioDispatcher)
    }

    override fun getTaskWithDetailsById(id: Long): Flow<TaskWithSubtasksAndCategory?> {
        return taskDao.getTaskWithDetailsById(id).flowOn(ioDispatcher)
    }

    override fun getTasksForToday(startOfDay: Long, endOfDay: Long): Flow<List<TaskWithSubtasksAndCategory>> {
        return taskDao.getTasksForToday(startOfDay, endOfDay).flowOn(ioDispatcher)
    }

    override fun getTasksInDateRange(startDate: Long, endDate: Long): Flow<List<TaskWithSubtasksAndCategory>> {
        return taskDao.getTasksInDateRange(startDate, endDate).flowOn(ioDispatcher)
    }

    override fun getTasksByCategory(categoryId: Long): Flow<List<TaskWithSubtasksAndCategory>> {
        return taskDao.getTasksByCategory(categoryId).flowOn(ioDispatcher)
    }

    override fun getTasksByCompletion(isCompleted: Boolean): Flow<List<TaskWithSubtasksAndCategory>> {
        return taskDao.getTasksByCompletion(isCompleted).flowOn(ioDispatcher)
    }

    override fun searchTasks(query: String): Flow<List<TaskWithSubtasksAndCategory>> {
        return taskDao.searchTasks(query).flowOn(ioDispatcher)
    }

    override suspend fun createTask(task: TaskEntity, subtaskTitles: List<String>): Long = withContext(ioDispatcher) {
        val taskId = taskDao.insertTask(task)
        if (subtaskTitles.isNotEmpty()) {
            val subtasks = subtaskTitles.mapIndexed { index, title ->
                SubtaskEntity(
                    taskId = taskId,
                    title = title,
                    sortOrder = index
                )
            }
            subtaskDao.insertSubtasks(subtasks)
        }
        taskId
    }

    override suspend fun updateTask(task: TaskEntity) = withContext(ioDispatcher) {
        taskDao.updateTask(task.copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun deleteTask(task: TaskEntity) = withContext(ioDispatcher) {
        taskDao.deleteTask(task)
    }

    override suspend fun deleteTaskById(id: Long) = withContext(ioDispatcher) {
        taskDao.deleteTaskById(id)
    }

    override suspend fun setTaskCompleted(id: Long, isCompleted: Boolean) = withContext(ioDispatcher) {
        val timestamp = if (isCompleted) System.currentTimeMillis() else null
        taskDao.setTaskCompleted(id, isCompleted, timestamp)
    }

    override suspend fun createSubtask(taskId: Long, title: String): Long = withContext(ioDispatcher) {
        subtaskDao.insertSubtask(SubtaskEntity(taskId = taskId, title = title))
    }

    override suspend fun setSubtaskCompleted(subtaskId: Long, isCompleted: Boolean) = withContext(ioDispatcher) {
        subtaskDao.setSubtaskCompleted(subtaskId, isCompleted)
    }

    override suspend fun updateSubtask(subtask: SubtaskEntity) = withContext(ioDispatcher) {
        subtaskDao.updateSubtask(subtask)
    }

    override suspend fun deleteSubtask(subtask: SubtaskEntity) = withContext(ioDispatcher) {
        subtaskDao.deleteSubtask(subtask)
    }

    override fun getSubtasksForTask(taskId: Long): Flow<List<SubtaskEntity>> {
        return subtaskDao.getSubtasksForTask(taskId).flowOn(ioDispatcher)
    }

    override suspend fun syncSubtasks(taskId: Long, subtasks: List<SubtaskEntity>) = withContext(ioDispatcher) {
        subtaskDao.deleteSubtasksForTask(taskId)
        if (subtasks.isNotEmpty()) {
            val toInsert = subtasks.mapIndexed { index, subtask ->
                subtask.copy(id = 0, taskId = taskId, sortOrder = index)
            }
            subtaskDao.insertSubtasks(toInsert)
        }
    }

    override suspend fun getTaskById(id: Long): TaskEntity? = withContext(ioDispatcher) {
        taskDao.getTaskById(id)
    }

    override suspend fun getPendingTasksWithDueDate(): List<TaskEntity> = withContext(ioDispatcher) {
        taskDao.getPendingTasksWithDueDate()
    }

    override suspend fun completeTaskAndGenerateNext(taskId: Long): TaskEntity? = withContext(ioDispatcher) {
        val task = taskDao.getTaskById(taskId) ?: return@withContext null
        if (task.isCompleted) return@withContext null

        taskDao.setTaskCompleted(taskId, true, System.currentTimeMillis())

        val ruleStr = task.recurrenceRule ?: return@withContext null
        val rule = com.personal.daymark.core.recurrence.RecurrenceRule.fromJson(ruleStr) ?: return@withContext null

        val seriesId = task.seriesId ?: java.util.UUID.randomUUID().toString()
        if (task.seriesId == null) {
            taskDao.updateTask(task.copy(seriesId = seriesId, isCompleted = true))
        }

        val existingActive = taskDao.getActiveTasksBySeriesId(seriesId)
        if (existingActive.isNotEmpty()) {
            return@withContext null
        }

        val currentDueDate = task.dueDate ?: System.currentTimeMillis()
        val nextDueDate = com.personal.daymark.core.recurrence.RecurrenceHelper.calculateNextOccurrence(
            currentDueDateMillis = currentDueDate,
            dueTimeMinutes = task.dueTime,
            rule = rule
        ) ?: return@withContext null

        val nextTask = TaskEntity(
            title = task.title,
            description = task.description,
            priority = task.priority,
            categoryId = task.categoryId,
            dueDate = nextDueDate,
            dueTime = task.dueTime,
            recurrenceRule = task.recurrenceRule,
            seriesId = seriesId,
            isCompleted = false
        )
        val newId = taskDao.insertTask(nextTask)
        nextTask.copy(id = newId)
    }

    override suspend fun deleteTaskSeries(seriesId: String): List<Long> = withContext(ioDispatcher) {
        val tasks = taskDao.getTasksBySeriesId(seriesId)
        val taskIds = tasks.map { it.id }
        taskDao.deleteTasksBySeriesId(seriesId)
        taskIds
    }

    override suspend fun deleteTaskOccurrence(taskId: Long): TaskEntity? = withContext(ioDispatcher) {
        val task = taskDao.getTaskById(taskId) ?: return@withContext null
        taskDao.deleteTask(task)

        val seriesId = task.seriesId
        val ruleStr = task.recurrenceRule
        if (seriesId != null && ruleStr != null) {
            val rule = com.personal.daymark.core.recurrence.RecurrenceRule.fromJson(ruleStr)
            if (rule != null) {
                val active = taskDao.getActiveTasksBySeriesId(seriesId)
                if (active.isEmpty()) {
                    val currentDueDate = task.dueDate ?: System.currentTimeMillis()
                    val nextDueDate = com.personal.daymark.core.recurrence.RecurrenceHelper.calculateNextOccurrence(
                        currentDueDateMillis = currentDueDate,
                        dueTimeMinutes = task.dueTime,
                        rule = rule
                    )
                    if (nextDueDate != null) {
                        val nextTask = TaskEntity(
                            title = task.title,
                            description = task.description,
                            priority = task.priority,
                            categoryId = task.categoryId,
                            dueDate = nextDueDate,
                            dueTime = task.dueTime,
                            recurrenceRule = task.recurrenceRule,
                            seriesId = seriesId,
                            isCompleted = false
                        )
                        val newId = taskDao.insertTask(nextTask)
                        return@withContext nextTask.copy(id = newId)
                    }
                }
            }
        }
        null
    }

    override suspend fun updateEntireSeries(seriesId: String, updatedTask: TaskEntity) = withContext(ioDispatcher) {
        taskDao.updateActiveTasksInSeries(
            seriesId = seriesId,
            title = updatedTask.title,
            description = updatedTask.description,
            priority = updatedTask.priority,
            categoryId = updatedTask.categoryId,
            dueTime = updatedTask.dueTime,
            recurrenceRule = updatedTask.recurrenceRule
        )
    }

    override suspend fun getTasksBySeriesId(seriesId: String): List<TaskEntity> = withContext(ioDispatcher) {
        taskDao.getTasksBySeriesId(seriesId)
    }

    override fun getCompletedTasksCountInRange(startMillis: Long, endMillis: Long): Flow<Int> {
        return taskDao.getCompletedTasksCountInRange(startMillis, endMillis).flowOn(ioDispatcher)
    }

    override fun getPendingTasksCount(): Flow<Int> {
        return taskDao.getPendingTasksCount().flowOn(ioDispatcher)
    }

    override fun getOverdueTasksCount(nowMillis: Long): Flow<Int> {
        return taskDao.getOverdueTasksCount(nowMillis).flowOn(ioDispatcher)
    }

    override fun getTotalTasksCount(): Flow<Int> {
        return taskDao.getTotalTasksCount().flowOn(ioDispatcher)
    }

    override fun getCompletedTasksInRange(startMillis: Long, endMillis: Long): Flow<List<TaskEntity>> {
        return taskDao.getCompletedTasksInRange(startMillis, endMillis).flowOn(ioDispatcher)
    }
}
