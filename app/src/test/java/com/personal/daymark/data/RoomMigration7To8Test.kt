package com.personal.daymark.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.personal.daymark.data.local.DaymarkDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomMigration7To8Test {

    @Test
    fun migration7To8_addsRoutineTimeOfDayAndIsFrozenToHabitsAndIsFrozenToHabitLogs() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_7_8_test.db"
        context.deleteDatabase(dbName)

        // 1. Create a database at Version 7 schema
        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(7) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `habits` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                            `name` TEXT NOT NULL, 
                            `note` TEXT, 
                            `frequency` TEXT NOT NULL, 
                            `target_weekdays` INTEGER NOT NULL, 
                            `color_hex` TEXT NOT NULL, 
                            `category_id` INTEGER, 
                            `is_archived` INTEGER NOT NULL, 
                            `created_at` INTEGER NOT NULL, 
                            `health_metric_type` TEXT, 
                            `target_value` INTEGER, 
                            `unit` TEXT,
                            `reminder_time` INTEGER,
                            `target_duration_minutes` INTEGER
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `habit_logs` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `habit_id` INTEGER NOT NULL,
                            `date` INTEGER NOT NULL,
                            `date_string` TEXT NOT NULL DEFAULT '',
                            `is_completed` INTEGER NOT NULL,
                            `logged_at` INTEGER NOT NULL,
                            `value` REAL,
                            `note` TEXT
                        )
                    """.trimIndent())
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val v7Db = FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase

        // Insert habit and habit_log into v7 schema
        v7Db.execSQL("""
            INSERT INTO `habits` (`name`, `note`, `frequency`, `target_weekdays`, `color_hex`, `is_archived`, `created_at`, `reminder_time`, `target_duration_minutes`)
            VALUES ('Morning Meditation', 'Breathe deeply', 'DAILY', 127, '#1B4332', 0, 1000000, 480, 15)
        """.trimIndent())

        v7Db.execSQL("""
            INSERT INTO `habit_logs` (`habit_id`, `date`, `date_string`, `is_completed`, `logged_at`)
            VALUES (1, 20000, '2024-10-01', 1, 1000000)
        """.trimIndent())

        v7Db.close()

        // 2. Run Migration 7 to 8
        val v8Config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(8) {
                override fun onCreate(db: SupportSQLiteDatabase) {}
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    if (oldVersion == 7 && newVersion == 8) {
                        DaymarkDatabase.MIGRATION_7_8.migrate(db)
                    }
                }
            })
            .build()

        val v8Db = FrameworkSQLiteOpenHelperFactory().create(v8Config).writableDatabase

        // 3. Verify columns exist with defaults
        val habitCursor = v8Db.query("SELECT id, name, routine_time_of_day, is_frozen FROM habits")
        assertTrue(habitCursor.moveToFirst())
        assertEquals("Morning Meditation", habitCursor.getString(1))
        assertEquals("ANYTIME", habitCursor.getString(2))
        assertEquals(0, habitCursor.getInt(3))
        habitCursor.close()

        val logCursor = v8Db.query("SELECT id, habit_id, is_frozen FROM habit_logs")
        assertTrue(logCursor.moveToFirst())
        assertEquals(1L, logCursor.getLong(1))
        assertEquals(0, logCursor.getInt(2))
        logCursor.close()

        // 4. Update with routine and freeze values
        v8Db.execSQL("UPDATE habits SET routine_time_of_day = 'MORNING', is_frozen = 1 WHERE id = 1")
        val updatedHabit = v8Db.query("SELECT routine_time_of_day, is_frozen FROM habits WHERE id = 1")
        assertTrue(updatedHabit.moveToFirst())
        assertEquals("MORNING", updatedHabit.getString(0))
        assertEquals(1, updatedHabit.getInt(1))
        updatedHabit.close()

        v8Db.execSQL("UPDATE habit_logs SET is_frozen = 1 WHERE id = 1")
        val updatedLog = v8Db.query("SELECT is_frozen FROM habit_logs WHERE id = 1")
        assertTrue(updatedLog.moveToFirst())
        assertEquals(1, updatedLog.getInt(0))
        updatedLog.close()

        v8Db.close()
    }
}

