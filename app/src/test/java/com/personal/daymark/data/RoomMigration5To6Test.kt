package com.personal.daymark.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.personal.daymark.data.local.DaymarkDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomMigration5To6Test {

    @Test
    fun migration5To6_extendsTasksHabitsAndLogsWithHealthFieldsAndPreservesExistingData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_5_6_test.db"
        context.deleteDatabase(dbName)

        // 1. Create a database at Version 5 schema
        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(5) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `categories` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                            `name` TEXT NOT NULL, 
                            `color_hex` TEXT NOT NULL, 
                            `icon_name` TEXT, 
                            `sort_order` INTEGER NOT NULL, 
                            `created_at` INTEGER NOT NULL
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `tasks` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                            `title` TEXT NOT NULL, 
                            `description` TEXT, 
                            `due_date` INTEGER, 
                            `due_time` INTEGER, 
                            `priority` INTEGER NOT NULL, 
                            `category_id` INTEGER, 
                            `is_completed` INTEGER NOT NULL, 
                            `completion_timestamp` INTEGER, 
                            `created_at` INTEGER NOT NULL, 
                            `updated_at` INTEGER NOT NULL, 
                            `recurrence_rule` TEXT, 
                            `series_id` TEXT, 
                            `duration_minutes` INTEGER NOT NULL DEFAULT 0, 
                            `start_time` TEXT
                        )
                    """.trimIndent())

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
                            `created_at` INTEGER NOT NULL
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `habit_logs` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `habit_id` INTEGER NOT NULL,
                            `date` INTEGER NOT NULL,
                            `date_string` TEXT NOT NULL DEFAULT '',
                            `is_completed` INTEGER NOT NULL,
                            `logged_at` INTEGER NOT NULL
                        )
                    """.trimIndent())
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val v5Db = helper.writableDatabase

        // 2. Insert existing data in version 5 schema
        v5Db.execSQL("""
            INSERT INTO tasks (id, title, priority, is_completed, created_at, updated_at, duration_minutes, start_time)
            VALUES (201, 'Existing V5 Task', 1, 0, 1700000000, 1700000000, 30, '09:00')
        """.trimIndent())

        v5Db.execSQL("""
            INSERT INTO habits (id, name, note, frequency, target_weekdays, color_hex, is_archived, created_at)
            VALUES (301, 'Morning Walk', 'Walk 30m', 'DAILY', 127, '#1B4332', 0, 1700000000)
        """.trimIndent())

        v5Db.execSQL("""
            INSERT INTO habit_logs (id, habit_id, date, date_string, is_completed, logged_at)
            VALUES (401, 301, 20750, '2026-10-26', 1, 1700000000)
        """.trimIndent())

        // 3. Execute Migration 5 to 6
        DaymarkDatabase.MIGRATION_5_6.migrate(v5Db)

        // 4. Verify existing task is preserved with default medicine values
        val taskCursor = v5Db.query("SELECT id, title, is_medicine, dosage, stock_count FROM tasks WHERE id = 201")
        assertTrue(taskCursor.moveToFirst())
        assertEquals(201L, taskCursor.getLong(taskCursor.getColumnIndexOrThrow("id")))
        assertEquals("Existing V5 Task", taskCursor.getString(taskCursor.getColumnIndexOrThrow("title")))
        assertEquals(0, taskCursor.getInt(taskCursor.getColumnIndexOrThrow("is_medicine")))
        assertTrue(taskCursor.isNull(taskCursor.getColumnIndexOrThrow("dosage")))
        assertTrue(taskCursor.isNull(taskCursor.getColumnIndexOrThrow("stock_count")))
        taskCursor.close()

        // 5. Verify existing habit is preserved with null health fields
        val habitCursor = v5Db.query("SELECT id, name, health_metric_type, target_value, unit FROM habits WHERE id = 301")
        assertTrue(habitCursor.moveToFirst())
        assertEquals(301L, habitCursor.getLong(habitCursor.getColumnIndexOrThrow("id")))
        assertEquals("Morning Walk", habitCursor.getString(habitCursor.getColumnIndexOrThrow("name")))
        assertTrue(habitCursor.isNull(habitCursor.getColumnIndexOrThrow("health_metric_type")))
        assertTrue(habitCursor.isNull(habitCursor.getColumnIndexOrThrow("target_value")))
        assertTrue(habitCursor.isNull(habitCursor.getColumnIndexOrThrow("unit")))
        habitCursor.close()

        // 6. Verify existing habit log is preserved with null value & note
        val logCursor = v5Db.query("SELECT id, habit_id, is_completed, value, note FROM habit_logs WHERE id = 401")
        assertTrue(logCursor.moveToFirst())
        assertEquals(401L, logCursor.getLong(logCursor.getColumnIndexOrThrow("id")))
        assertEquals(1, logCursor.getInt(logCursor.getColumnIndexOrThrow("is_completed")))
        assertTrue(logCursor.isNull(logCursor.getColumnIndexOrThrow("value")))
        assertTrue(logCursor.isNull(logCursor.getColumnIndexOrThrow("note")))
        logCursor.close()

        // 7. Verify new medicine and health records can be inserted and queried
        v5Db.execSQL("""
            INSERT INTO tasks (title, priority, is_completed, created_at, updated_at, is_medicine, dosage, medicine_form, stock_count, refill_threshold)
            VALUES ('Vitamin D', 2, 0, 1700000000, 1700000000, 1, '1000 IU', 'capsule', 30, 7)
        """.trimIndent())

        val medCursor = v5Db.query("SELECT * FROM tasks WHERE is_medicine = 1")
        assertTrue(medCursor.moveToFirst())
        assertEquals("Vitamin D", medCursor.getString(medCursor.getColumnIndexOrThrow("title")))
        assertEquals("1000 IU", medCursor.getString(medCursor.getColumnIndexOrThrow("dosage")))
        assertEquals("capsule", medCursor.getString(medCursor.getColumnIndexOrThrow("medicine_form")))
        assertEquals(30, medCursor.getInt(medCursor.getColumnIndexOrThrow("stock_count")))
        assertEquals(7, medCursor.getInt(medCursor.getColumnIndexOrThrow("refill_threshold")))
        medCursor.close()

        v5Db.execSQL("""
            INSERT INTO habits (name, frequency, target_weekdays, color_hex, is_archived, created_at, health_metric_type, target_value, unit)
            VALUES ('Hydration', 'DAILY', 127, '#40916C', 0, 1700000000, 'water', 8, 'glasses')
        """.trimIndent())

        val waterHabitCursor = v5Db.query("SELECT * FROM habits WHERE health_metric_type = 'water'")
        assertTrue(waterHabitCursor.moveToFirst())
        assertEquals("Hydration", waterHabitCursor.getString(waterHabitCursor.getColumnIndexOrThrow("name")))
        assertEquals(8, waterHabitCursor.getInt(waterHabitCursor.getColumnIndexOrThrow("target_value")))
        assertEquals("glasses", waterHabitCursor.getString(waterHabitCursor.getColumnIndexOrThrow("unit")))
        waterHabitCursor.close()

        v5Db.close()
    }
}
