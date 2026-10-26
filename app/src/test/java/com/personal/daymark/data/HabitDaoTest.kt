package com.personal.daymark.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.personal.daymark.data.local.DaymarkDatabase
import com.personal.daymark.data.local.dao.HabitDao
import com.personal.daymark.data.local.entity.HabitEntity
import com.personal.daymark.data.local.entity.HabitLogEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HabitDaoTest {

    private lateinit var database: DaymarkDatabase
    private lateinit var habitDao: HabitDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, DaymarkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        habitDao = database.habitDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertHabitAndLogCompletion() = runBlocking {
        val habitId = habitDao.insertHabit(
            HabitEntity(name = "Drink 2L Water", frequency = "DAILY", targetWeekdays = 127)
        )

        habitDao.insertHabitLog(
            HabitLogEntity(habitId = habitId, date = 20000L, isCompleted = true)
        )

        val habitsWithLogs = habitDao.getActiveHabitsWithLogs().first()
        assertEquals(1, habitsWithLogs.size)
        assertEquals("Drink 2L Water", habitsWithLogs[0].habit.name)
        assertEquals(1, habitsWithLogs[0].logs.size)
        assertEquals(20000L, habitsWithLogs[0].logs[0].date)
    }

    @Test
    fun deleteHabitLogUpdatesFlow() = runBlocking {
        val habitId = habitDao.insertHabit(HabitEntity(name = "Morning Journal"))
        habitDao.insertHabitLog(HabitLogEntity(habitId = habitId, date = 20001L))

        var logs = habitDao.getLogsForHabit(habitId).first()
        assertEquals(1, logs.size)

        habitDao.deleteHabitLog(habitId, 20001L)

        logs = habitDao.getLogsForHabit(habitId).first()
        assertTrue(logs.isEmpty())
    }
}
