package com.personal.daymark.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.personal.daymark.data.local.entity.SubtaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubtaskDao {
    @Query("SELECT * FROM subtasks WHERE task_id = :taskId ORDER BY sort_order ASC, created_at ASC")
    fun getSubtasksForTask(taskId: Long): Flow<List<SubtaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtask(subtask: SubtaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtasks(subtasks: List<SubtaskEntity>)

    @Update
    suspend fun updateSubtask(subtask: SubtaskEntity)

    @Delete
    suspend fun deleteSubtask(subtask: SubtaskEntity)

    @Query("UPDATE subtasks SET is_completed = :isCompleted WHERE id = :id")
    suspend fun setSubtaskCompleted(id: Long, isCompleted: Boolean)

    @Query("DELETE FROM subtasks WHERE task_id = :taskId")
    suspend fun deleteSubtasksForTask(taskId: Long)

    @Query("SELECT * FROM subtasks ORDER BY id ASC")
    suspend fun getAllSubtasksList(): List<SubtaskEntity>

    @Query("DELETE FROM subtasks")
    suspend fun deleteAllSubtasks()
}
