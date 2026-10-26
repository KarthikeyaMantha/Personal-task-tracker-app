package com.personal.daymark.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
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
        Index("due_date"),
        Index("is_completed"),
        Index("priority"),
        Index("series_id")
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String? = null,
    @ColumnInfo(name = "due_date")
    val dueDate: Long? = null, // epoch millis representing calendar date
    @ColumnInfo(name = "due_time")
    val dueTime: Int? = null, // minutes from midnight (0..1439)
    val priority: Int = 0, // 0 = None, 1 = Low, 2 = Medium, 3 = High, 4 = Urgent
    @ColumnInfo(name = "category_id")
    val categoryId: Long? = null,
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,
    @ColumnInfo(name = "completion_timestamp")
    val completionTimestamp: Long? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "recurrence_rule")
    val recurrenceRule: String? = null,
    @ColumnInfo(name = "series_id")
    val seriesId: String? = null,
    @ColumnInfo(name = "duration_minutes", defaultValue = "0")
    val durationMinutes: Int = 0,
    @ColumnInfo(name = "start_time")
    val startTime: String? = null, // formatted as "HH:mm", e.g. "09:30"
    @ColumnInfo(name = "is_medicine", defaultValue = "0")
    val isMedicine: Boolean = false,
    @ColumnInfo(name = "dosage")
    val dosage: String? = null, // e.g. "500mg", "1 tablet"
    @ColumnInfo(name = "medicine_form")
    val medicineForm: String? = null, // e.g. "tablet", "capsule", "liquid"
    @ColumnInfo(name = "stock_count")
    val stockCount: Int? = null, // e.g. 24
    @ColumnInfo(name = "refill_threshold")
    val refillThreshold: Int? = null // e.g. 7
) {
    /**
     * Returns effective start time string "HH:mm", falling back to dueTime if startTime is null.
     */
    fun getEffectiveStartTime(): String? {
        if (!startTime.isNullOrBlank()) return startTime
        if (dueTime != null) {
            val h = dueTime / 60
            val m = dueTime % 60
            return String.format(java.util.Locale.US, "%02d:%02d", h, m)
        }
        return null
    }

    /**
     * Returns start time in minutes from midnight (0..1439), or null if unscheduled.
     */
    fun getEffectiveStartMinutes(): Int? {
        if (!startTime.isNullOrBlank()) {
            val parts = startTime.split(":")
            if (parts.size == 2) {
                val h = parts[0].trim().toIntOrNull()
                val m = parts[1].trim().toIntOrNull()
                if (h != null && m != null) {
                    return h * 60 + m
                }
            }
        }
        return dueTime
    }

    /**
     * True if the task has both a valid start time and positive duration for timeline display.
     */
    val isTimelineScheduled: Boolean
        get() = getEffectiveStartMinutes() != null && durationMinutes > 0
}
