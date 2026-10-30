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
class RoomMigration6To7Test {

    @Test
    fun migration6To7_addsReminderTimeAndTargetDurationMinutesToHabitsTable() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_6_7_test.db"
        context.deleteDatabase(dbName)

        // 1. Create a database at Version 6 schema
        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(6) {
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
                            `unit` TEXT
                        )
                    """.trimIndent())
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val v6Db = FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase

        // Insert habit into v6 schema
        v6Db.execSQL("""
            INSERT INTO `habits` (`name`, `note`, `frequency`, `target_weekdays`, `color_hex`, `is_archived`, `created_at`)
            VALUES ('Morning Run', 'Keep fit', 'DAILY', 127, '#1B4332', 0, 1000000)
        """.trimIndent())
        v6Db.close()

        // 2. Run Migration 6 to 7
        val v7Config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(7) {
                override fun onCreate(db: SupportSQLiteDatabase) {}
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    if (oldVersion == 6 && newVersion == 7) {
                        DaymarkDatabase.MIGRATION_6_7.migrate(db)
                    }
                }
            })
            .build()

        val v7Db = FrameworkSQLiteOpenHelperFactory().create(v7Config).writableDatabase

        // 3. Verify columns exist and data preserved
        val cursor = v7Db.query("SELECT id, name, reminder_time, target_duration_minutes FROM habits")
        assertTrue(cursor.moveToFirst())
        assertEquals("Morning Run", cursor.getString(1))
        assertTrue(cursor.isNull(2)) // reminder_time is null initially
        assertTrue(cursor.isNull(3)) // target_duration_minutes is null initially
        cursor.close()

        // 4. Update with new fields
        v7Db.execSQL("UPDATE habits SET reminder_time = 480, target_duration_minutes = 30 WHERE name = 'Morning Run'")
        val updatedCursor = v7Db.query("SELECT reminder_time, target_duration_minutes FROM habits WHERE name = 'Morning Run'")
        assertTrue(updatedCursor.moveToFirst())
        assertEquals(480, updatedCursor.getInt(0))
        assertEquals(30, updatedCursor.getInt(1))
        updatedCursor.close()

        v7Db.close()
    }
}

