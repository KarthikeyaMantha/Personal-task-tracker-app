package com.personal.daymark.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habits",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("category_id"),
        Index("is_archived")
    ]
)
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val note: String? = null,
    val frequency: String = "DAILY", // "DAILY", "WEEKLY"
    @ColumnInfo(name = "target_weekdays")
    val targetWeekdays: Int = 127, // bitmask for days of week (127 = 1111111 for all 7 days)
    @ColumnInfo(name = "color_hex")
    val colorHex: String = "#1B4332",
    @ColumnInfo(name = "category_id")
    val categoryId: Long? = null,
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "health_metric_type")
    val healthMetricType: String? = null, // "water", "sleep", "mood", or null
    @ColumnInfo(name = "target_value")
    val targetValue: Int? = null, // e.g., 8 glasses, 8 hours
    @ColumnInfo(name = "unit")
    val unit: String? = null, // e.g., "glasses", "hours"
    @ColumnInfo(name = "reminder_time")
    val reminderTime: Int? = null, // minutes from midnight (0..1439), e.g. 480 for 08:00 AM
    @ColumnInfo(name = "target_duration_minutes")
    val targetDurationMinutes: Int? = null, // e.g., 15, 25, 30 min focus duration
    @ColumnInfo(name = "routine_time_of_day", defaultValue = "ANYTIME")
    val routineTimeOfDay: String = "ANYTIME", // "MORNING", "AFTERNOON", "EVENING", "ANYTIME"
    @ColumnInfo(name = "is_frozen", defaultValue = "0")
    val isFrozen: Boolean = false // Overall pause / freeze
)

fun HabitEntity.parsedColor(): androidx.compose.ui.graphics.Color {
    return try {
        androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(colorHex))
    } catch (_: Exception) {
        androidx.compose.ui.graphics.Color(0xFF1B4332)
    }
}
