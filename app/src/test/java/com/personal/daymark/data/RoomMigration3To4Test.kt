package com.personal.daymark.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.personal.daymark.data.local.DaymarkDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomMigration3To4Test {

    @Test
    fun migration3To4_addsColumnsAndPreservesExistingData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_test.db"
        context.deleteDatabase(dbName)

        // 1. Create a database at Version 3 schema directly using FrameworkSQLiteOpenHelper
        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(3) {
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
                            FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL 
                        )
                    """.trimIndent())

                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_category_id` ON `tasks` (`category_id`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_due_date` ON `tasks` (`due_date`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_is_completed` ON `tasks` (`is_completed`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_priority` ON `tasks` (`priority`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_series_id` ON `tasks` (`series_id`)")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val v3Db = helper.writableDatabase

        // 2. Insert existing task in version 3 schema
        v3Db.execSQL("""
            INSERT INTO tasks (id, title, description, due_date, due_time, priority, is_completed, created_at, updated_at)
            VALUES (42, 'Existing Important Project', 'Details here', 1700000000000, 540, 3, 0, 1699999000, 1699999000)
        """.trimIndent())

        // 3. Execute Migration 3 to 4
        DaymarkDatabase.MIGRATION_3_4.migrate(v3Db)

        // 4. Verify existing task data is fully preserved and has default column values
        val cursor = v3Db.query("SELECT id, title, description, duration_minutes, start_time FROM tasks WHERE id = 42")
        assertTrue(cursor.moveToFirst())
        assertEquals(42L, cursor.getLong(cursor.getColumnIndexOrThrow("id")))
        assertEquals("Existing Important Project", cursor.getString(cursor.getColumnIndexOrThrow("title")))
        assertEquals("Details here", cursor.getString(cursor.getColumnIndexOrThrow("description")))
        assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("duration_minutes")))
        assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("start_time")))
        cursor.close()

        // 5. Verify new columns can be populated
        v3Db.execSQL("""
            INSERT INTO tasks (id, title, priority, is_completed, created_at, updated_at, duration_minutes, start_time)
            VALUES (43, 'New Time Block Task', 2, 0, 1700000000, 1700000000, 60, '14:30')
        """.trimIndent())

        val cursorNew = v3Db.query("SELECT id, title, duration_minutes, start_time FROM tasks WHERE id = 43")
        assertTrue(cursorNew.moveToFirst())
        assertEquals("New Time Block Task", cursorNew.getString(cursorNew.getColumnIndexOrThrow("title")))
        assertEquals(60, cursorNew.getInt(cursorNew.getColumnIndexOrThrow("duration_minutes")))
        assertEquals("14:30", cursorNew.getString(cursorNew.getColumnIndexOrThrow("start_time")))
        cursorNew.close()

        v3Db.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun roomDatabase_canOpenWithMigration3To4() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, DaymarkDatabase::class.java)
            .addMigrations(DaymarkDatabase.MIGRATION_1_2, DaymarkDatabase.MIGRATION_2_3, DaymarkDatabase.MIGRATION_3_4)
            .build()

        val taskDao = db.taskDao()
        val taskId = taskDao.insertTask(
            com.personal.daymark.data.local.entity.TaskEntity(
                title = "Room Integration Task",
                durationMinutes = 45,
                startTime = "11:15"
            )
        )

        val retrieved = taskDao.getTaskById(taskId)
        assertNotNull(retrieved)
        assertEquals("Room Integration Task", retrieved?.title)
        assertEquals(45, retrieved?.durationMinutes)
        assertEquals("11:15", retrieved?.startTime)
        assertTrue(retrieved!!.isTimelineScheduled)

        db.close()
    }
}
