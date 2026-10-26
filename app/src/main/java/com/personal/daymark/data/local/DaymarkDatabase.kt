package com.personal.daymark.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.personal.daymark.data.local.dao.CategoryDao
import com.personal.daymark.data.local.dao.FocusDao
import com.personal.daymark.data.local.dao.HabitDao
import com.personal.daymark.data.local.dao.SubtaskDao
import com.personal.daymark.data.local.dao.TaskDao
import com.personal.daymark.data.local.entity.CategoryEntity
import com.personal.daymark.data.local.entity.FocusSessionEntity
import com.personal.daymark.data.local.entity.HabitEntity
import com.personal.daymark.data.local.entity.HabitLogEntity
import com.personal.daymark.data.local.entity.SubtaskEntity
import com.personal.daymark.data.local.entity.TaskEntity

@Database(
    entities = [
        CategoryEntity::class,
        TaskEntity::class,
        SubtaskEntity::class,
        HabitEntity::class,
        HabitLogEntity::class,
        FocusSessionEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class DaymarkDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun taskDao(): TaskDao
    abstract fun subtaskDao(): SubtaskDao
    abstract fun habitDao(): HabitDao
    abstract fun focusDao(): FocusDao

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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
