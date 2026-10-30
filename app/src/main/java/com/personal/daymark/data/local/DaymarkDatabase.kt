package com.personal.daymark.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.personal.daymark.data.local.dao.CategoryDao
import com.personal.daymark.data.local.dao.DayTemplateDao
import com.personal.daymark.data.local.dao.FocusDao
import com.personal.daymark.data.local.dao.HabitDao
import com.personal.daymark.data.local.dao.SubtaskDao
import com.personal.daymark.data.local.dao.TaskDao
import com.personal.daymark.data.local.dao.TimeBlockDao
import com.personal.daymark.data.local.entity.CategoryEntity
import com.personal.daymark.data.local.entity.DayTemplateEntity
import com.personal.daymark.data.local.entity.FocusSessionEntity
import com.personal.daymark.data.local.entity.HabitEntity
import com.personal.daymark.data.local.entity.HabitLogEntity
import com.personal.daymark.data.local.entity.SubtaskEntity
import com.personal.daymark.data.local.entity.TaskEntity
import com.personal.daymark.data.local.entity.TemplateItemEntity
import com.personal.daymark.data.local.entity.TimeBlockEntity

@Database(
    entities = [
        CategoryEntity::class,
        TaskEntity::class,
        SubtaskEntity::class,
        HabitEntity::class,
        HabitLogEntity::class,
        FocusSessionEntity::class,
        TimeBlockEntity::class,
        DayTemplateEntity::class,
        TemplateItemEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class DaymarkDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun taskDao(): TaskDao
    abstract fun subtaskDao(): SubtaskDao
    abstract fun habitDao(): HabitDao
    abstract fun focusDao(): FocusDao
    abstract fun timeBlockDao(): TimeBlockDao
    abstract fun dayTemplateDao(): DayTemplateDao

    companion object {
        const val DATABASE_NAME = "daymark.db"

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN series_id TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_series_id ON tasks(series_id)")
            }
        }

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // 1. Add note column to habits
                db.execSQL("ALTER TABLE habits ADD COLUMN note TEXT")

                // 2. Add date_string column and indices to habit_logs
                db.execSQL("ALTER TABLE habit_logs ADD COLUMN date_string TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_habit_logs_habit_id_date_string ON habit_logs(habit_id, date_string)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_habit_logs_date_string ON habit_logs(date_string)")

                // 3. Add planned_duration_minutes and session_type to focus_sessions
                db.execSQL("ALTER TABLE focus_sessions ADD COLUMN planned_duration_minutes INTEGER NOT NULL DEFAULT 25")
                db.execSQL("ALTER TABLE focus_sessions ADD COLUMN session_type TEXT NOT NULL DEFAULT 'FOCUS'")
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN duration_minutes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tasks ADD COLUMN start_time TEXT")
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS time_blocks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        start_time TEXT NOT NULL,
                        duration_minutes INTEGER NOT NULL,
                        color_hex TEXT,
                        date TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_time_blocks_date ON time_blocks(date)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS day_templates (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS template_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        template_id INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        start_time TEXT NOT NULL,
                        duration_minutes INTEGER NOT NULL,
                        is_task INTEGER NOT NULL,
                        FOREIGN KEY(template_id) REFERENCES day_templates(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_template_items_template_id ON template_items(template_id)")
            }
        }

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // 1. Extend tasks table with medicine tracking columns
                db.execSQL("ALTER TABLE tasks ADD COLUMN is_medicine INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tasks ADD COLUMN dosage TEXT")
                db.execSQL("ALTER TABLE tasks ADD COLUMN medicine_form TEXT")
                db.execSQL("ALTER TABLE tasks ADD COLUMN stock_count INTEGER")
                db.execSQL("ALTER TABLE tasks ADD COLUMN refill_threshold INTEGER")

                // 2. Extend habits table with health metric columns
                db.execSQL("ALTER TABLE habits ADD COLUMN health_metric_type TEXT")
                db.execSQL("ALTER TABLE habits ADD COLUMN target_value INTEGER")
                db.execSQL("ALTER TABLE habits ADD COLUMN unit TEXT")

                // 3. Extend habit_logs table with value and note columns
                db.execSQL("ALTER TABLE habit_logs ADD COLUMN value REAL")
                db.execSQL("ALTER TABLE habit_logs ADD COLUMN note TEXT")
            }
        }

        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits ADD COLUMN reminder_time INTEGER")
                db.execSQL("ALTER TABLE habits ADD COLUMN target_duration_minutes INTEGER")
            }
        }

        val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits ADD COLUMN routine_time_of_day TEXT NOT NULL DEFAULT 'ANYTIME'")
                db.execSQL("ALTER TABLE habits ADD COLUMN is_frozen INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE habit_logs ADD COLUMN is_frozen INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile
        private var INSTANCE: DaymarkDatabase? = null

        fun getInstance(context: Context): DaymarkDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(appContext: Context): DaymarkDatabase {
            return Room.databaseBuilder(
                appContext,
                DaymarkDatabase::class.java,
                DATABASE_NAME
            )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        super.onCreate(db)
                        val now = System.currentTimeMillis()
                        db.execSQL("INSERT OR IGNORE INTO categories (name, color_hex, sort_order, created_at) VALUES ('Personal', '#40916C', 0, $now)")
                        db.execSQL("INSERT OR IGNORE INTO categories (name, color_hex, sort_order, created_at) VALUES ('Work', '#1B4332', 1, $now)")
                        db.execSQL("INSERT OR IGNORE INTO categories (name, color_hex, sort_order, created_at) VALUES ('Study', '#4A6FA5', 2, $now)")
                        db.execSQL("INSERT OR IGNORE INTO categories (name, color_hex, sort_order, created_at) VALUES ('Health', '#D97706', 3, $now)")
                        db.execSQL("INSERT OR IGNORE INTO categories (name, color_hex, sort_order, created_at) VALUES ('Other', '#6B7280', 4, $now)")
                    }
                })
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                .build()
        }
    }
}
