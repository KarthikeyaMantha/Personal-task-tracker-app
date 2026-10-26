package com.personal.daymark.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.personal.daymark.data.local.entity.TaskEntity
import com.personal.daymark.data.local.entity.TaskWithSubtasksAndCategory
import kotlinx.coroutines.flow.Flow

/**
 * Aggregated count of completed tasks for a specific date (format: "YYYY-MM-DD").
 */
data class DailyCompletionCount(
    @ColumnInfo(name = "day_date") val dayDate: String,
    @ColumnInfo(name = "completed_count") val count: Int
)

@Dao
interface TaskDao {

    @Transaction
    @Query("SELECT * FROM tasks ORDER BY is_completed ASC, priority DESC, due_date ASC, created_at DESC")
    fun getAllTasksWithDetails(): Flow<List<TaskWithSubtasksAndCategory>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    fun getTaskWithDetailsById(id: Long): Flow<TaskWithSubtasksAndCategory?>

    @Transaction
    @Query("""
        SELECT * FROM tasks 
        WHERE (due_date >= :startOfDay AND due_date <= :endOfDay) 
           OR (is_completed = 0 AND due_date < :startOfDay)
        ORDER BY is_completed ASC, priority DESC, due_date ASC
    """)
    fun getTasksForToday(startOfDay: Long, endOfDay: Long): Flow<List<TaskWithSubtasksAndCategory>>

    @Transaction
    @Query("""
        SELECT * FROM tasks 
        WHERE due_date >= :startDate AND due_date <= :endDate 
        ORDER BY is_completed ASC, due_date ASC, due_time ASC, priority DESC
    """)
    fun getTasksInDateRange(startDate: Long, endDate: Long): Flow<List<TaskWithSubtasksAndCategory>>

    @Query("SELECT * FROM tasks WHERE due_date >= :startDate AND due_date <= :endDate")
    suspend fun getTasksInDateRangeSync(startDate: Long, endDate: Long): List<TaskEntity>

    @Transaction
    @Query("SELECT * FROM tasks WHERE category_id = :categoryId ORDER BY is_completed ASC, priority DESC, due_date ASC")
    fun getTasksByCategory(categoryId: Long): Flow<List<TaskWithSubtasksAndCategory>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE is_completed = :isCompleted ORDER BY priority DESC, due_date ASC")
    fun getTasksByCompletion(isCompleted: Boolean): Flow<List<TaskWithSubtasksAndCategory>>

    @Transaction
    @Query("""
        SELECT * FROM tasks 
        WHERE title LIKE '%' || :query || '%' 
           OR description LIKE '%' || :query || '%'
        ORDER BY is_completed ASC, priority DESC, due_date ASC
    """)
    fun searchTasks(query: String): Flow<List<TaskWithSubtasksAndCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("""
        UPDATE tasks 
        SET is_completed = :isCompleted, 
            completion_timestamp = :completionTimestamp,
            updated_at = :updatedAt
        WHERE id = :id
    """)
    suspend fun setTaskCompleted(
        id: Long,
        isCompleted: Boolean,
        completionTimestamp: Long?,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE is_completed = 0 AND due_date IS NOT NULL")
    suspend fun getPendingTasksWithDueDate(): List<TaskEntity>

    @Query("UPDATE tasks SET category_id = NULL WHERE category_id = :categoryId")
    suspend fun clearCategoryFromTasks(categoryId: Long)

    @Query("SELECT * FROM tasks WHERE series_id = :seriesId ORDER BY due_date ASC")
    suspend fun getTasksBySeriesId(seriesId: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE series_id = :seriesId AND is_completed = 0")
    suspend fun getActiveTasksBySeriesId(seriesId: String): List<TaskEntity>

    @Query("DELETE FROM tasks WHERE series_id = :seriesId")
    suspend fun deleteTasksBySeriesId(seriesId: String)

    @Query("DELETE FROM tasks WHERE series_id = :seriesId AND is_completed = 0 AND due_date >= :fromDueDate")
    suspend fun deleteFutureTasksBySeriesId(seriesId: String, fromDueDate: Long)

    @Query("""
        UPDATE tasks 
        SET title = :title,
            description = :description,
            priority = :priority,
            category_id = :categoryId,
            due_time = :dueTime,
            recurrence_rule = :recurrenceRule,
            updated_at = :updatedAt
        WHERE series_id = :seriesId AND is_completed = 0
    """)
    suspend fun updateActiveTasksInSeries(
        seriesId: String,
        title: String,
        description: String?,
        priority: Int,
        categoryId: Long?,
        dueTime: Int?,
        recurrenceRule: String?,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("SELECT COUNT(*) FROM tasks WHERE is_completed = 1 AND completion_timestamp >= :startMillis AND completion_timestamp <= :endMillis")
    fun getCompletedTasksCountInRange(startMillis: Long, endMillis: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE is_completed = 0")
    fun getPendingTasksCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE is_completed = 0 AND due_date IS NOT NULL AND due_date < :nowMillis")
    fun getOverdueTasksCount(nowMillis: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks")
    fun getTotalTasksCount(): Flow<Int>

    @Query("SELECT * FROM tasks WHERE is_completed = 1 AND completion_timestamp >= :startMillis AND completion_timestamp <= :endMillis ORDER BY completion_timestamp ASC")
    fun getCompletedTasksInRange(startMillis: Long, endMillis: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY id ASC")
    suspend fun getAllTasksList(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    @Query("""
        SELECT date(COALESCE(completion_timestamp, due_date, updated_at) / 1000, 'unixepoch', 'localtime') AS day_date,
               COUNT(*) AS completed_count
        FROM tasks
        WHERE is_completed = 1
          AND COALESCE(completion_timestamp, due_date, updated_at) >= :startMillis
          AND COALESCE(completion_timestamp, due_date, updated_at) <= :endMillis
        GROUP BY day_date
    """)
    fun getCompletedTaskCountsPerDay(startMillis: Long, endMillis: Long): Flow<List<DailyCompletionCount>>

    @Query("""
        SELECT date(COALESCE(completion_timestamp, due_date, updated_at) / 1000, 'unixepoch', 'localtime') AS day_date,
               COUNT(*) AS completed_count
        FROM tasks
        WHERE is_completed = 1
          AND COALESCE(completion_timestamp, due_date, updated_at) >= :startMillis
          AND COALESCE(completion_timestamp, due_date, updated_at) <= :endMillis
        GROUP BY day_date
    """)
    suspend fun getCompletedTaskCountsPerDaySync(startMillis: Long, endMillis: Long): List<DailyCompletionCount>

    @Query("""
        SELECT date(COALESCE(completion_timestamp, due_date, updated_at) / 1000, 'unixepoch', 'localtime') AS day_date,
               COUNT(*) AS completed_count
        FROM tasks
        WHERE is_completed = 1
        GROUP BY day_date
    """)
    fun getAllCompletedTaskCountsPerDay(): Flow<List<DailyCompletionCount>>

    @Query("""
        SELECT * FROM tasks 
        WHERE is_medicine = 1 
          AND ((due_date >= :startOfDay AND due_date <= :endOfDay) OR (is_completed = 0 AND due_date < :startOfDay))
        ORDER BY is_completed ASC, due_time ASC, title ASC
    """)
    fun getTodayMedicines(startOfDay: Long, endOfDay: Long): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks 
        WHERE is_medicine = 1 
          AND ((due_date >= :startOfDay AND due_date <= :endOfDay) OR (is_completed = 0 AND due_date < :startOfDay))
        ORDER BY is_completed ASC, due_time ASC, title ASC
    """)
    suspend fun getTodayMedicinesSync(startOfDay: Long, endOfDay: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE is_medicine = 1 ORDER BY due_date ASC")
    fun getAllMedicines(): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks 
        WHERE is_medicine = 1 
          AND is_completed = 1 
          AND (completion_timestamp >= :startMillis AND completion_timestamp <= :endMillis)
    """)
    suspend fun getCompletedMedicinesInRange(startMillis: Long, endMillis: Long): List<TaskEntity>

    @Query("""
        SELECT * FROM tasks 
        WHERE is_medicine = 1 
          AND (due_date >= :startMillis AND due_date <= :endMillis)
    """)
    suspend fun getScheduledMedicinesInRange(startMillis: Long, endMillis: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE title = :title AND is_completed = 0 LIMIT 1")
    suspend fun findPendingTaskByTitle(title: String): TaskEntity?
}
