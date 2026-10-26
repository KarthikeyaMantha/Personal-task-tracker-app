package com.personal.daymark.data.repository

import com.personal.daymark.data.local.dao.TimeBlockDao
import com.personal.daymark.data.local.entity.TimeBlockEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

interface TimeBlockRepository {
    fun getTimeBlocksForDate(date: String): Flow<List<TimeBlockEntity>>
    suspend fun getTimeBlocksForDateSync(date: String): List<TimeBlockEntity>
    fun getAllTimeBlocks(): Flow<List<TimeBlockEntity>>
    suspend fun createTimeBlock(timeBlock: TimeBlockEntity): Long
    suspend fun updateTimeBlock(timeBlock: TimeBlockEntity)
    suspend fun deleteTimeBlock(id: Long)
}

class TimeBlockRepositoryImpl(
    private val timeBlockDao: TimeBlockDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : TimeBlockRepository {

    override fun getTimeBlocksForDate(date: String): Flow<List<TimeBlockEntity>> {
        return timeBlockDao.getTimeBlocksForDate(date).flowOn(ioDispatcher)
    }

    override suspend fun getTimeBlocksForDateSync(date: String): List<TimeBlockEntity> =
        withContext(ioDispatcher) {
            timeBlockDao.getTimeBlocksForDateSync(date)
        }

    override fun getAllTimeBlocks(): Flow<List<TimeBlockEntity>> {
        return timeBlockDao.getAllTimeBlocks().flowOn(ioDispatcher)
    }

    override suspend fun createTimeBlock(timeBlock: TimeBlockEntity): Long =
        withContext(ioDispatcher) {
            timeBlockDao.insert(timeBlock)
        }

    override suspend fun updateTimeBlock(timeBlock: TimeBlockEntity) =
        withContext(ioDispatcher) {
            timeBlockDao.update(timeBlock)
        }

    override suspend fun deleteTimeBlock(id: Long) =
        withContext(ioDispatcher) {
            timeBlockDao.deleteById(id)
        }
}
