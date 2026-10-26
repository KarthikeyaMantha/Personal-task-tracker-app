package com.personal.daymark.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.personal.daymark.data.local.entity.FocusSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusDao {
    @Query("SELECT * FROM focus_sessions ORDER BY started_at DESC")
    fun getAllSessions(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE task_id = :taskId ORDER BY started_at DESC")
    fun getSessionsForTask(taskId: Long): Flow<List<FocusSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity): Long

    @Update
    suspend fun updateSession(session: FocusSessionEntity)

    @Delete
    suspend fun deleteSession(session: FocusSessionEntity)

    @Query("SELECT SUM(duration_minutes) FROM focus_sessions WHERE started_at >= :startDate AND started_at <= :endDate")
    fun getTotalDurationMinutes(startDate: Long, endDate: Long): Flow<Int?>

    @Query("SELECT SUM(duration_minutes) FROM focus_sessions WHERE started_at >= :startDate AND started_at <= :endDate AND session_type = :sessionType")
    fun getFocusMinutesByType(startDate: Long, endDate: Long, sessionType: String = "FOCUS"): Flow<Int?>

    @Query("SELECT * FROM focus_sessions WHERE started_at >= :startDate AND started_at <= :endDate ORDER BY started_at ASC")
    fun getFocusSessionsInRange(startDate: Long, endDate: Long): Flow<List<FocusSessionEntity>>

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE session_type = :sessionType")
    fun getTotalSessionsCount(sessionType: String = "FOCUS"): Flow<Int>

    @Query("SELECT * FROM focus_sessions ORDER BY id ASC")
    suspend fun getAllFocusSessionsList(): List<FocusSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusSessions(sessions: List<FocusSessionEntity>)

    @Query("DELETE FROM focus_sessions")
    suspend fun deleteAllFocusSessions()
}
