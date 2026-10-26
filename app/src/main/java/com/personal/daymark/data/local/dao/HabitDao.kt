package com.personal.daymark.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.personal.daymark.data.local.entity.HabitEntity
import com.personal.daymark.data.local.entity.HabitLogEntity
import com.personal.daymark.data.local.entity.HabitWithLogs
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Transaction
    @Query("SELECT * FROM habits WHERE is_archived = 0 ORDER BY created_at ASC")
    fun getActiveHabitsWithLogs(): Flow<List<HabitWithLogs>>

    @Transaction
    @Query("SELECT * FROM habits ORDER BY is_archived ASC, created_at ASC")
    fun getAllHabitsWithLogs(): Flow<List<HabitWithLogs>>

    @Transaction
    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    fun getHabitWithLogsById(id: Long): Flow<HabitWithLogs?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabitById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabitLog(log: HabitLogEntity): Long

    @Query("DELETE FROM habit_logs WHERE habit_id = :habitId AND date = :date")
    suspend fun deleteHabitLog(habitId: Long, date: Long)

    @Query("SELECT * FROM habit_logs WHERE habit_id = :habitId ORDER BY date DESC")
    fun getLogsForHabit(habitId: Long): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE habit_id = :habitId AND date = :date LIMIT 1")
    fun getLogForDate(habitId: Long, date: Long): Flow<HabitLogEntity?>

    @Query("DELETE FROM habit_logs WHERE habit_id = :habitId AND date_string = :dateString")
    suspend fun deleteHabitLogByDateString(habitId: Long, dateString: String)

    @Query("SELECT * FROM habit_logs WHERE habit_id = :habitId AND date_string = :dateString LIMIT 1")
    fun getLogForDateString(habitId: Long, dateString: String): Flow<HabitLogEntity?>

    @Query("SELECT * FROM habit_logs WHERE habit_id = :habitId AND date_string = :dateString LIMIT 1")
    suspend fun getLogForDateStringSync(habitId: Long, dateString: String): HabitLogEntity?

    @Query("SELECT * FROM habit_logs WHERE habit_id = :habitId AND date_string >= :startDateString AND date_string <= :endDateString ORDER BY date_string ASC")
    fun getLogsBetweenDateStrings(habitId: Long, startDateString: String, endDateString: String): Flow<List<HabitLogEntity>>

    @Query("SELECT COUNT(*) FROM habits WHERE is_archived = 0")
    fun getActiveHabitsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM habit_logs WHERE date_string >= :startDateString AND date_string <= :endDateString AND is_completed = 1")
    fun getHabitCompletionsCountInRange(startDateString: String, endDateString: String): Flow<Int>

    @Query("SELECT * FROM habit_logs WHERE date_string >= :startDateString AND date_string <= :endDateString AND is_completed = 1 ORDER BY date_string ASC")
    fun getAllLogsInRange(startDateString: String, endDateString: String): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habits ORDER BY id ASC")
    suspend fun getAllHabitsList(): List<HabitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabits(habits: List<HabitEntity>)

    @Query("DELETE FROM habits")
    suspend fun deleteAllHabits()

    @Query("SELECT * FROM habit_logs ORDER BY id ASC")
    suspend fun getAllHabitLogsList(): List<HabitLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabitLogs(logs: List<HabitLogEntity>)

    @Query("DELETE FROM habit_logs")
    suspend fun deleteAllHabitLogs()
}
