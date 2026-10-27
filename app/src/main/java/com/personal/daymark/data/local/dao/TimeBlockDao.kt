package com.personal.daymark.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.personal.daymark.data.local.entity.TimeBlockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeBlockDao {

    @Query("SELECT * FROM time_blocks WHERE date = :date ORDER BY start_time ASC")
    fun getTimeBlocksForDate(date: String): Flow<List<TimeBlockEntity>>

    @Query("SELECT * FROM time_blocks WHERE date = :date ORDER BY start_time ASC")
    suspend fun getTimeBlocksForDateSync(date: String): List<TimeBlockEntity>

    @Query("SELECT * FROM time_blocks ORDER BY date ASC, start_time ASC")
    fun getAllTimeBlocks(): Flow<List<TimeBlockEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(timeBlock: TimeBlockEntity): Long

    @Update
    suspend fun update(timeBlock: TimeBlockEntity)

    @Delete
    suspend fun delete(timeBlock: TimeBlockEntity)

    @Query("SELECT * FROM time_blocks ORDER BY date ASC, start_time ASC")
    suspend fun getAllTimeBlocksList(): List<TimeBlockEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeBlocks(blocks: List<TimeBlockEntity>)

    @Query("DELETE FROM time_blocks")
    suspend fun deleteAllTimeBlocks()

    @Query("DELETE FROM time_blocks WHERE id = :id")
    suspend fun deleteById(id: Long)
}
