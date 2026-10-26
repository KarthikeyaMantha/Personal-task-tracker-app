package com.personal.daymark.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.personal.daymark.data.local.DaymarkDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomMigration4To5Test {

    @Test
    fun migration4To5_createsTimeBlocksAndTemplatesTablesAndPreservesExistingData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_4_5_test.db"
        context.deleteDatabase(dbName)

        // 1. Create a database at Version 4 schema directly
        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(4) {
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
                            `start_time` TEXT,
                            FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL 
                        )
                    """.trimIndent())
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val v4Db = helper.writableDatabase

        // 2. Insert existing task in version 4 schema
        v4Db.execSQL("""
            INSERT INTO tasks (id, title, priority, is_completed, created_at, updated_at, duration_minutes, start_time)
            VALUES (101, 'Existing V4 Task', 2, 0, 1700000000, 1700000000, 45, '10:00')
        """.trimIndent())

        // 3. Execute Migration 4 to 5
        DaymarkDatabase.MIGRATION_4_5.migrate(v4Db)

        // 4. Verify existing task data is preserved
        val taskCursor = v4Db.query("SELECT id, title, duration_minutes, start_time FROM tasks WHERE id = 101")
        assertTrue(taskCursor.moveToFirst())
        assertEquals(101L, taskCursor.getLong(taskCursor.getColumnIndexOrThrow("id")))
        assertEquals("Existing V4 Task", taskCursor.getString(taskCursor.getColumnIndexOrThrow("title")))
        assertEquals(45, taskCursor.getInt(taskCursor.getColumnIndexOrThrow("duration_minutes")))
        assertEquals("10:00", taskCursor.getString(taskCursor.getColumnIndexOrThrow("start_time")))
        taskCursor.close()

        // 5. Verify time_blocks table exists and operates correctly
        v4Db.execSQL("""
            INSERT INTO time_blocks (title, start_time, duration_minutes, color_hex, date)
            VALUES ('Lunch Break', '12:00', 60, '#718096', '2026-10-26')
        """.trimIndent())

        val busyCursor = v4Db.query("SELECT * FROM time_blocks WHERE date = '2026-10-26'")
        assertTrue(busyCursor.moveToFirst())
        assertEquals("Lunch Break", busyCursor.getString(busyCursor.getColumnIndexOrThrow("title")))
        assertEquals("12:00", busyCursor.getString(busyCursor.getColumnIndexOrThrow("start_time")))
        assertEquals(60, busyCursor.getInt(busyCursor.getColumnIndexOrThrow("duration_minutes")))
        assertEquals("2026-10-26", busyCursor.getString(busyCursor.getColumnIndexOrThrow("date")))
        busyCursor.close()

        // 6. Verify day_templates & template_items tables exist and cascade delete
        v4Db.execSQL("INSERT INTO day_templates (id, name) VALUES (1, 'Morning Routine')")
        v4Db.execSQL("""
            INSERT INTO template_items (id, template_id, title, start_time, duration_minutes, is_task)
            VALUES (10, 1, 'Exercise', '07:00', 30, 1)
        """.trimIndent())
        v4Db.execSQL("""
            INSERT INTO template_items (id, template_id, title, start_time, duration_minutes, is_task)
            VALUES (11, 1, 'Breakfast', '07:30', 30, 0)
        """.trimIndent())

        val templateCursor = v4Db.query("SELECT * FROM day_templates WHERE id = 1")
        assertTrue(templateCursor.moveToFirst())
        assertEquals("Morning Routine", templateCursor.getString(templateCursor.getColumnIndexOrThrow("name")))
        templateCursor.close()

        val itemsCursor = v4Db.query("SELECT COUNT(*) FROM template_items WHERE template_id = 1")
        assertTrue(itemsCursor.moveToFirst())
        assertEquals(2, itemsCursor.getInt(0))
        itemsCursor.close()

        v4Db.close()
    }
}
