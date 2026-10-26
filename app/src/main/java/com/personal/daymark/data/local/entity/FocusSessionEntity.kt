package com.personal.daymark.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "focus_sessions",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("task_id"),
        Index("started_at")
    ]
)
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "task_id")
    val taskId: Long? = null,
    @ColumnInfo(name = "duration_minutes")
    val durationMinutes: Int,
    @ColumnInfo(name = "planned_duration_minutes", defaultValue = "25")
    val plannedDurationMinutes: Int = 25,
    @ColumnInfo(name = "session_type", defaultValue = "FOCUS")
    val sessionType: String = "FOCUS", // "FOCUS", "SHORT_BREAK", "LONG_BREAK"
    @ColumnInfo(name = "started_at")
    val startedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null,
    val notes: String? = null
)
