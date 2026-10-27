package com.personal.daymark.data.repository

import com.personal.daymark.data.local.dao.FocusDao
import com.personal.daymark.data.local.entity.FocusSessionEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

interface FocusRepository {
    fun getAllSessions(): Flow<List<FocusSessionEntity>>
    fun getSessionsForTask(taskId: Long): Flow<List<FocusSessionEntity>>
    fun getTotalDurationMinutes(startDate: Long, endDate: Long): Flow<Int?>
    fun getFocusMinutesByType(startDate: Long, endDate: Long, sessionType: String = "FOCUS"): Flow<Int?>
    fun getFocusSessionsInRange(startDate: Long, endDate: Long): Flow<List<FocusSessionEntity>>
    fun getTotalSessionsCount(sessionType: String = "FOCUS"): Flow<Int>
    suspend fun recordFocusSession(
        taskId: Long? = null,
        durationMinutes: Int,
        plannedDurationMinutes: Int = durationMinutes,
        sessionType: String = "FOCUS",
        startedAt: Long,
        completedAt: Long? = null,
        notes: String? = null
    ): Long
    suspend fun deleteSession(session: FocusSessionEntity)
}

class FocusRepositoryImpl(
    private val focusDao: FocusDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : FocusRepository {

    override fun getAllSessions(): Flow<List<FocusSessionEntity>> {
        return focusDao.getAllSessions().flowOn(ioDispatcher)
    }

    override fun getSessionsForTask(taskId: Long): Flow<List<FocusSessionEntity>> {
        return focusDao.getSessionsForTask(taskId).flowOn(ioDispatcher)
    }

    override fun getTotalDurationMinutes(startDate: Long, endDate: Long): Flow<Int?> {
        return focusDao.getTotalDurationMinutes(startDate, endDate).flowOn(ioDispatcher)
    }

    override fun getFocusMinutesByType(startDate: Long, endDate: Long, sessionType: String): Flow<Int?> {
        return focusDao.getFocusMinutesByType(startDate, endDate, sessionType).flowOn(ioDispatcher)
    }

    override fun getFocusSessionsInRange(startDate: Long, endDate: Long): Flow<List<FocusSessionEntity>> {
        return focusDao.getFocusSessionsInRange(startDate, endDate).flowOn(ioDispatcher)
    }

    override fun getTotalSessionsCount(sessionType: String): Flow<Int> {
        return focusDao.getTotalSessionsCount(sessionType).flowOn(ioDispatcher)
    }

    override suspend fun recordFocusSession(
        taskId: Long?,
        durationMinutes: Int,
        plannedDurationMinutes: Int,
        sessionType: String,
        startedAt: Long,
        completedAt: Long?,
        notes: String?
    ): Long = withContext(ioDispatcher) {
        if (startedAt != 0L) {
            val existing = focusDao.getSessionByStartedAt(startedAt)
            if (existing != null) {
                return@withContext existing.id
            }
        }
        val session = FocusSessionEntity(
            taskId = taskId,
            durationMinutes = durationMinutes,
            plannedDurationMinutes = plannedDurationMinutes,
            sessionType = sessionType,
            startedAt = startedAt,
            completedAt = completedAt,
            notes = notes
        )
        focusDao.insertSession(session)
    }

    override suspend fun deleteSession(session: FocusSessionEntity) = withContext(ioDispatcher) {
        focusDao.deleteSession(session)
    }
}
