package com.personal.daymark.data.repository

import com.personal.daymark.data.local.dao.HabitDao
import com.personal.daymark.data.local.entity.HabitEntity
import com.personal.daymark.data.local.entity.HabitLogEntity
import com.personal.daymark.data.local.entity.HabitWithLogs
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

interface HabitRepository {
    fun getActiveHabitsWithLogs(): Flow<List<HabitWithLogs>>
    fun getAllHabitsWithLogs(): Flow<List<HabitWithLogs>>
    fun getHabitWithLogsById(id: Long): Flow<HabitWithLogs?>
    fun getLogsForHabit(habitId: Long): Flow<List<HabitLogEntity>>
    suspend fun createHabit(
        name: String,
        note: String? = null,
        frequency: String = "DAILY",
        targetWeekdays: Int = 127,
        colorHex: String = "#1B4332",
        categoryId: Long? = null
    ): Long = 0L

    suspend fun createHabit(
        name: String,
        note: String? = null,
        frequency: String = "DAILY",
        targetWeekdays: Int = 127,
        colorHex: String = "#1B4332",
        categoryId: Long? = null,
        healthMetricType: String? = null,
        targetValue: Int? = null,
        unit: String? = null
    ): Long = createHabit(name, note, frequency, targetWeekdays, colorHex, categoryId)
    suspend fun logHealthMetric(
        habitId: Long,
        value: Float,
        note: String? = null,
        localDate: java.time.LocalDate = java.time.LocalDate.now()
    ): HabitLogEntity? = null
    suspend fun incrementWater(
        habitId: Long,
        localDate: java.time.LocalDate = java.time.LocalDate.now()
    ): Float = 0f
    suspend fun updateHabit(habit: HabitEntity)
    suspend fun deleteHabit(habit: HabitEntity)
    suspend fun deleteHabitById(id: Long)
    suspend fun setHabitCompleted(habitId: Long, date: Long, isCompleted: Boolean)
    suspend fun setHabitCompletedForDate(habitId: Long, localDate: java.time.LocalDate, isCompleted: Boolean)
    suspend fun toggleHabitCompletion(habitId: Long, localDate: java.time.LocalDate = java.time.LocalDate.now()): Boolean
    fun getActiveHabitsCount(): Flow<Int>
    fun getHabitCompletionsCountInRange(startDateString: String, endDateString: String): Flow<Int>
    fun getAllLogsInRange(startDateString: String, endDateString: String): Flow<List<HabitLogEntity>>
}

class HabitRepositoryImpl(
    private val habitDao: HabitDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : HabitRepository {

    override fun getActiveHabitsWithLogs(): Flow<List<HabitWithLogs>> {
        return habitDao.getActiveHabitsWithLogs().flowOn(ioDispatcher)
    }

    override fun getAllHabitsWithLogs(): Flow<List<HabitWithLogs>> {
        return habitDao.getAllHabitsWithLogs().flowOn(ioDispatcher)
    }

    override fun getHabitWithLogsById(id: Long): Flow<HabitWithLogs?> {
        return habitDao.getHabitWithLogsById(id).flowOn(ioDispatcher)
    }

    override fun getLogsForHabit(habitId: Long): Flow<List<HabitLogEntity>> {
        return habitDao.getLogsForHabit(habitId).flowOn(ioDispatcher)
    }

    override suspend fun createHabit(
        name: String,
        note: String?,
        frequency: String,
        targetWeekdays: Int,
        colorHex: String,
        categoryId: Long?
    ): Long = createHabit(name, note, frequency, targetWeekdays, colorHex, categoryId, null, null, null)

    override suspend fun createHabit(
        name: String,
        note: String?,
        frequency: String,
        targetWeekdays: Int,
        colorHex: String,
        categoryId: Long?,
        healthMetricType: String?,
        targetValue: Int?,
        unit: String?
    ): Long = withContext(ioDispatcher) {
        val habit = HabitEntity(
            name = name,
            note = note,
            frequency = frequency,
            targetWeekdays = targetWeekdays,
            colorHex = colorHex,
            categoryId = categoryId,
            healthMetricType = healthMetricType,
            targetValue = targetValue,
            unit = unit
        )
        habitDao.insertHabit(habit)
    }

    override suspend fun logHealthMetric(
        habitId: Long,
        value: Float,
        note: String?,
        localDate: java.time.LocalDate
    ): HabitLogEntity? = withContext(ioDispatcher) {
        val dateStr = localDate.toString()
        val epochDay = localDate.toEpochDay()
        val existing = habitDao.getLogForDateStringSync(habitId, dateStr)
        val habit = habitDao.getAllHabitsList().find { it.id == habitId }
        val target = habit?.targetValue ?: 1
        val isCompleted = when (habit?.healthMetricType) {
            "mood" -> true
            else -> value >= target
        }
        val log = HabitLogEntity(
            id = existing?.id ?: 0L,
            habitId = habitId,
            date = epochDay,
            dateString = dateStr,
            isCompleted = isCompleted,
            value = value,
            note = note ?: existing?.note,
            loggedAt = System.currentTimeMillis()
        )
        val id = habitDao.insertHabitLog(log)
        log.copy(id = id)
    }

    override suspend fun incrementWater(
        habitId: Long,
        localDate: java.time.LocalDate
    ): Float = withContext(ioDispatcher) {
        val dateStr = localDate.toString()
        val epochDay = localDate.toEpochDay()
        val existing = habitDao.getLogForDateStringSync(habitId, dateStr)
        val currentCount = existing?.value ?: 0f
        val newCount = currentCount + 1f
        val habit = habitDao.getAllHabitsList().find { it.id == habitId }
        val target = habit?.targetValue ?: 8
        val log = HabitLogEntity(
            id = existing?.id ?: 0L,
            habitId = habitId,
            date = epochDay,
            dateString = dateStr,
            isCompleted = newCount >= target,
            value = newCount,
            note = existing?.note,
            loggedAt = System.currentTimeMillis()
        )
        habitDao.insertHabitLog(log)
        newCount
    }

    override suspend fun updateHabit(habit: HabitEntity) = withContext(ioDispatcher) {
        habitDao.updateHabit(habit)
    }

    override suspend fun deleteHabit(habit: HabitEntity) = withContext(ioDispatcher) {
        habitDao.deleteHabit(habit)
    }

    override suspend fun deleteHabitById(id: Long) = withContext(ioDispatcher) {
        habitDao.deleteHabitById(id)
    }

    override suspend fun setHabitCompleted(habitId: Long, date: Long, isCompleted: Boolean) {
        withContext(ioDispatcher) {
            val dateStr = java.time.LocalDate.ofEpochDay(date).toString()
            if (isCompleted) {
                habitDao.insertHabitLog(
                    HabitLogEntity(
                        habitId = habitId,
                        date = date,
                        dateString = dateStr,
                        isCompleted = true
                    )
                )
            } else {
                habitDao.deleteHabitLog(habitId, date)
                habitDao.deleteHabitLogByDateString(habitId, dateStr)
            }
        }
    }

    override suspend fun setHabitCompletedForDate(
        habitId: Long,
        localDate: java.time.LocalDate,
        isCompleted: Boolean
    ) {
        withContext(ioDispatcher) {
            val dateStr = localDate.toString()
            val epochDay = localDate.toEpochDay()
            if (isCompleted) {
                habitDao.insertHabitLog(
                    HabitLogEntity(
                        habitId = habitId,
                        date = epochDay,
                        dateString = dateStr,
                        isCompleted = true
                    )
                )
            } else {
                habitDao.deleteHabitLogByDateString(habitId, dateStr)
                habitDao.deleteHabitLog(habitId, epochDay)
            }
        }
    }

    override suspend fun toggleHabitCompletion(
        habitId: Long,
        localDate: java.time.LocalDate
    ): Boolean = withContext(ioDispatcher) {
        val dateStr = localDate.toString()
        val epochDay = localDate.toEpochDay()
        val existing = habitDao.getLogForDateStringSync(habitId, dateStr)
        if (existing != null) {
            habitDao.deleteHabitLogByDateString(habitId, dateStr)
            habitDao.deleteHabitLog(habitId, epochDay)
            false
        } else {
            habitDao.insertHabitLog(
                HabitLogEntity(
                    habitId = habitId,
                    date = epochDay,
                    dateString = dateStr,
                    isCompleted = true
                )
            )
            true
        }
    }

    override fun getActiveHabitsCount(): Flow<Int> {
        return habitDao.getActiveHabitsCount().flowOn(ioDispatcher)
    }

    override fun getHabitCompletionsCountInRange(startDateString: String, endDateString: String): Flow<Int> {
        return habitDao.getHabitCompletionsCountInRange(startDateString, endDateString).flowOn(ioDispatcher)
    }

    override fun getAllLogsInRange(startDateString: String, endDateString: String): Flow<List<HabitLogEntity>> {
        return habitDao.getAllLogsInRange(startDateString, endDateString).flowOn(ioDispatcher)
    }
}
